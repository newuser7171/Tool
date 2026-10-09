# APK startup-loader integration

`tools/inject_apk.py` is a non-root APK repacker for an ARM64 Android
package. It decodes the APK with apktool, inserts an Application subclass
that loads `libjevtool.so`, rebuilds, zipaligns and signs the APK.

## Requirements

- Python 3.9+
- apktool
- Android SDK Build Tools (`zipalign`, `apksigner`)
- An existing Java keystore
- A compiled ARM64 `libjevtool.so`

Example:

```sh
python3 tools/inject_apk.py game.apk \
  --lib libjevtool.so \
  --keystore my-release-key.jks \
  --alias mykey \
  --store-pass 'PASSWORD' \
  --key-pass 'PASSWORD' \
  --output game-jev.apk
```

Do not use your primary signing keystore or password on an untrusted host.
The output has a new signature, so it normally cannot update an installed
official app with a different signing certificate. Back up app data before
uninstalling anything. Google Play integrity or other signature checks may
prevent the repackaged game from working.

## Implementation boundaries

The injected Application loader runs `System.loadLibrary("jevtool")` after
the original Application.onCreate(). It does not intercept `eglSwapBuffers`,
inject touches, create a Unity render callback, bypass integrity checks, or
grant memory access to other processes.

Consequently, the modified APK **will not automatically show the JEV menu**.
The existing `unity/JevUnityOverlay.cs` requires integration into a Unity
project, or a separately implemented and tested game-specific rendering hook.

This loader also assumes the original Application is subclassable, has an
accessible no-argument constructor, and that app startup is not guarded by
checks that reject changes. Verify the original class and dex layout before
installing. The repacker currently handles single APK inputs, not APKM/XAPK
or split-install bundles.
