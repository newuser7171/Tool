# JEV embedded overlay integration

The JEV overlay is a **host-driven** OpenGL ES 3 module. It is not an automatic
APK injector and does not hook third-party graphics functions.

## Integration in an Android Unity project you control

1. Build `libjevtool.so` for the same ABI as the host (ARM64).
2. Load it from the host application at startup.
3. After the OpenGL ES context is current on the render thread, call
   `jev_overlay_init(width, height)`.
4. Forward framebuffer resize events to `jev_overlay_resize`.
5. Forward pointer input in framebuffer coordinates with
   `jev_overlay_touch(x, y, pressed)`.
6. At the end of each OpenGL ES frame, before presenting, call
   `jev_overlay_frame(deltaSeconds)`.
7. Before the GL context is destroyed, call `jev_overlay_shutdown()`.

All rendering API calls must run on the same active GL thread. The overlay
creates its own ImGui context, so it must not share a context with another
ImGui integration. Host applications must handle touch ownership and GL state
isolation, and should test context loss, orientation changes and pause/resume.

## Runtime inspection

The overlay's Scan and Explore buttons query `libil2cpp.so` in the **current
process only**. If IL2CPP has not initialized, the inspector reports that it
is unavailable. The scan is synchronous and may briefly block the render
thread for large class sets. This is a read-only inspector; it does not
change gameplay fields.

## Subway Surfers status

No Subway Surfers-specific startup hook, rendering hook, touch dispatcher,
APK patcher or re-signing pipeline is included. The JEV standalone APK is
not an in-game overlay, and copying the library into a game APK is not enough
to make it run. The overlay is presently an integration component, not a
tested Subway Surfers modification.
