# JEV host activity integration

The existing native GLES3 overlay is exercised by the `Test native menu`
activity. To embed JEV into a Unity Android project you can rebuild, register
an Android activity view after Unity's activity has resumed.

The Android view overlay is a separate integration path from the ImGui
OpenGL overlay. It needs no OpenGL frame interception. Its launcher and
panel live in the Android window's view hierarchy.

## Host requirements

- Include the JEV native library for arm64-v8a.
- Include the JEV inspector bridge class.
- Install activity lifecycle callbacks from the host Application.
- Attach the menu to `android.R.id.content` after the host activity resumes.
- Run inspector operations off the UI thread, returning results on the UI thread.
- Do not intercept touches outside the menu controls.
- Remove any attached menu on activity destruction.
- Test with the host application's renderer and Android lifecycle.

## Not yet connected

The APK repacker at `tools/inject_apk.py` only adds a native library
loader. It does not bundle the Android activity panel or invoke its lifecycle
registration. An APK built by that script will not automatically show a JEV
menu. Third-party Unity games may also enforce signature/integrity checks.

The confirmed successful device test is for the JEV Toolkit standalone
native overlay, not for the game-integrated APK.
