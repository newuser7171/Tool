# JEV Native Toolkit v0.5 — Android test app

This is a **source project**, not a compiled APK. It runs its own GLES3 surface and renders the existing ImGui panel; the Android `JEV` button opens the existing JNI inspection panel.

## Build

1. Install Android SDK 35, Android NDK, CMake 3.22.1, Java 17, Gradle 8.x.
2. From project root: `git clone --depth 1 --branch v1.92.5 https://github.com/ocornut/imgui.git native/third_party/imgui`
3. Run `gradle assembleDebug` with Gradle 8.x, or build using Android Studio.
4. APK is `app/build/outputs/apk/debug/app-debug.apk`.

On a Termux-only installation, compiling `libjevtool.so` is not sufficient to build an APK. Use the included GitHub Actions workflow if Android SDK/Gradle are unavailable locally. Push the project contents to a repository and run **Build JEV test APK**.

This app does not attach to other apps, inject code, or inspect other app processes. IL2CPP will normally be unavailable in this standalone test process. JNI methods remain usable for host integration. The native ImGui editor uses registered variables only. Text input to the ImGui editor is not wired to the Android IME yet; use the Android panel for editing. Touch coordinates are passed from GLSurfaceView to ImGui.
