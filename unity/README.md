# Non-root Unity IL2CPP integration

The Unity plugin bridge is designed for Android Unity games you can rebuild,
and does not require root. It is not a generic binary APK injector.

## Supported integration path

1. Build the Android ARM64 JEV native library with JEV_WITH_IMGUI=ON.
2. Copy libjevtool.so to Assets/Plugins/Android/arm64-v8a/ in the Unity project.
3. Copy unity/JevUnityOverlay.cs into Assets/Scripts/.
4. Select OpenGL ES 3 (disable Vulkan) in Android Player graphics settings.
5. Attach JevUnityOverlay to the **camera that renders the final game frame**.
6. Build and install the Unity game. Tap the JEV launcher during gameplay.

The component uses GL.IssuePluginEvent for render-thread callbacks. The plugin
initializes lazily and can display live IL2CPP assembly/class information once
the IL2CPP runtime is initialized.

## Known limitations

- This integration is untested in a full Unity game and may need renderer-
  specific changes (especially URP/HDRP and multithreaded rendering).
- The current touch forwarding is basic; games can receive touches beneath
  the menu, and the UI does not yet implement touch ownership.
- Render callback scheduling and shutdown require testing during pause/resume,
  surface recreation, and context loss.
- The menu's live scanner can stall the render thread while enumerating classes.
- Android apps using Vulkan are not supported by this GLES3 overlay.
- Copying the library into a third-party APK does not make it load or render.
  No automated third-party APK patcher, signing pipeline or anti-tamper bypass
  is implemented.
- The floating system overlay works over other apps but cannot read their
  process memory.

For games you cannot rebuild, an APK-specific loader and render integration
would have to be independently researched and tested. A universal no-root
live-inspection solution is not provided by this bridge.
