using System;
using System.Collections;
using System.Runtime.InteropServices;
using UnityEngine;
using UnityEngine.Rendering;

// Attach to a GameObject in a Unity Android project you can rebuild.
// Requires JEV native plugin in Assets/Plugins/Android/arm64-v8a/libjevtool.so.
// Works only with OpenGL ES 3, not Vulkan.
public sealed class JevUnityOverlay : MonoBehaviour
{
    [DllImport("jevtool")] private static extern IntPtr jev_unity_get_render_event_func();
    [DllImport("jevtool")] private static extern void jev_unity_set_screen(int width, int height);
    [DllImport("jevtool")] private static extern void jev_unity_set_touch(float x, float y, bool down);

    private IntPtr callback;
    private bool started;
    private Coroutine frameLoop;

    private void Start()
    {
        if (SystemInfo.graphicsDeviceType != GraphicsDeviceType.OpenGLES3)
        {
            Debug.LogWarning("JEV requires OpenGL ES 3.");
            enabled = false;
            return;
        }
        callback = jev_unity_get_render_event_func();
        if (callback == IntPtr.Zero) { enabled = false; return; }
        jev_unity_set_screen(Screen.width, Screen.height);
        GL.IssuePluginEvent(callback, 1);
        started = true;
        frameLoop = StartCoroutine(RenderAtEndOfFrame());
    }

    private void Update()
    {
        if (!started) return;
        jev_unity_set_screen(Screen.width, Screen.height);
        if (Input.touchCount > 0)
        {
            Touch touch = Input.GetTouch(0);
            jev_unity_set_touch(touch.position.x, Screen.height - touch.position.y,
                touch.phase != TouchPhase.Ended && touch.phase != TouchPhase.Canceled);
        }
        else jev_unity_set_touch(-1f, -1f, false);
    }

    private IEnumerator RenderAtEndOfFrame()
    {
        var wait = new WaitForEndOfFrame();
        while (started)
        {
            yield return wait;
            if (started) GL.IssuePluginEvent(callback, 2);
        }
    }

    private void OnDestroy()
    {
        if (frameLoop != null) StopCoroutine(frameLoop);
        if (started) GL.IssuePluginEvent(callback, 3);
        started = false;
    }
}
