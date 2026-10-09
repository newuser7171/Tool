# JEV Native Toolkit — v0.3 source

An Android ARM64-oriented native inspection toolkit for **applications you control**.

## Implemented
- C++17 JNI library `libjevtool.so` with Android log initialization.
- Loaded-engine detection (Unity, IL2CPP, Mono, Unreal).
- IL2CPP assembly listing through exported runtime APIs, when available.
- Controlled float editing of explicitly registered in-app variables, with min/max bounds.
- Kotlin in-app floating **JEV** button, scan panel, and field editor.

- IL2CPP class, method and field names explorer with optional filter and bounded results (when exports are available).

## Not implemented yet
- ImGui graphics backend (the included UI is an Android View-based floating panel).
- Arbitrary IL2CPP class/method/field modification; the editor currently supports registered native float variables only.
- Automatic APK injection, hook installation, runtime dumping or bypasses.
- ARM32/x86 builds, production error handling, live reload.

## Integration
1. Add `native/jevtool.cpp` and `native/CMakeLists.txt` to an Android NDK module.
2. Configure `externalNativeBuild.cmake` in your app Gradle module.
3. Copy `android/JevBridge.kt` and `android/JevPanel.kt` into your Android project.
4. In your Activity, call `JevPanel(this).attach(findViewById(android.R.id.content))`.
5. For your own game values, register pointers using `jev::registerFloat(name, pointer, min, max)` from native code after their lifetime is guaranteed.
6. Use `adb logcat -s JEVTool:I` for startup diagnostics.

**Security:** The uploaded `libTool.so` and `libpsh.so` were not linked into this project. Untrusted native binaries should not be loaded into production apps without review.

**Build status:** Source packaged; NDK compilation and device execution not performed in this environment.

## v0.2 IL2CPP explorer
Tap **Explore IL2CPP classes** and optionally filter by class name, namespace or assembly.
The native scanner attaches its worker thread to IL2CPP, enumerates a bounded set of classes,
and detaches before returning. The implementation uses exported IL2CPP APIs and does not
resolve stripped internal symbols. Some Unity builds will not expose these APIs.

**Caution:** Use in an authorized test build. Native runtime APIs can change between Unity
versions; do not scan before IL2CPP initialization. The Android panel is still a View-based
prototype, not an ImGui renderer.

## v0.3 typed metadata browsing
- The IL2CPP explorer now displays field type names (when the runtime exports `il2cpp_field_get_type`, `il2cpp_type_get_name` and `il2cpp_free`).
- Displays method argument counts when the runtime exports `il2cpp_method_get_param_count`.
- Assembly inspection runs on a worker thread rather than blocking the UI thread.
- Removes unused window-manager code and uses density-aware report sizing.
- These are **read-only** metadata queries. Editing remains limited to application-registered float values.
- Still uses Android Views; an ImGui renderer is **not yet implemented**.

**Note:** Field type-name ownership follows the IL2CPP API convention that returned type-name strings are released with `il2cpp_free`. Exports are optional and must be checked per Unity version.

## v0.4 changes
- Optional Dear ImGui panel source with launcher, Editor, Explorer and Logs tabs.
- The host application must provide its own ImGui renderer, EGL context, input and frame lifecycle; this is **not** a standalone floating overlay.
- Build with `-DJEV_WITH_IMGUI=ON` after placing Dear ImGui source in `native/third_party/imgui`.
- Adds registered int32 and bool variable editing via `jev::registerInt`, `jev::registerBool`, and `JevBridge.setTyped`.
- Registered float editing now rejects NaN/infinity.
- Arbitrary IL2CPP field writes and runtime memory patching are not implemented.
- Android NDK build and real-device testing remain pending.
