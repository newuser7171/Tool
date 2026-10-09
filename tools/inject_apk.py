#!/usr/bin/env python3
"""Repackage a decoded Android APK with a JEV native startup loader.

Requires apktool, zipalign, apksigner, and a Java keystore.
This does NOT hook the game's graphics renderer.
"""
import argparse
import pathlib
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
LOADER_CLASS = "com.jev.inject.JevApplication"
LOADER_DESC = "Lcom/jev/inject/JevApplication;"


def run(*args):
    subprocess.run([str(x) for x in args], check=True)


def android_class(package, name):
    if not name:
        return "android.app.Application"
    if name.startswith("."):
        return package + name
    if "." not in name:
        return package + "." + name
    return name


def patch_manifest(folder):
    path = folder / "AndroidManifest.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    app = root.find("application")
    if app is None:
        raise ValueError("No application element")
    package = root.attrib.get("package", "")
    original = android_class(package, app.get(ANDROID + "name"))
    if original == LOADER_CLASS:
        raise ValueError("APK already uses JEV loader")
    app.set(ANDROID + "name", LOADER_CLASS)
    tree.write(path, encoding="utf-8", xml_declaration=True)
    return original


def install_loader(folder, original):
    # A new class avoids rewriting arbitrary existing methods/register layouts.
    # The original Application is still initialized through super.onCreate().
    superclass = "L" + original.replace(".", "/") + ";"
    smali = folder / "smali" / "com" / "jev" / "inject" / "JevApplication.smali"
    if smali.exists():
        raise ValueError("Loader class already exists")
    smali.parent.mkdir(parents=True, exist_ok=True)
    smali.write_text(f""".class public {LOADER_DESC}
.super {superclass}

.method public constructor <init>()V
    .locals 0
    invoke-direct {{p0}}, {superclass}-><init>()V
    return-void
.end method

.method public onCreate()V
    .locals 1
    invoke-super {{p0}}, {superclass}->onCreate()V
    const-string v0, "jevtool"
    invoke-static {{v0}}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
    return-void
.end method
""", encoding="utf-8")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("apk", type=pathlib.Path)
    ap.add_argument("--lib", type=pathlib.Path, required=True, help="ARM64 libjevtool.so")
    ap.add_argument("--keystore", type=pathlib.Path, required=True)
    ap.add_argument("--alias", required=True)
    ap.add_argument("--store-pass", required=True)
    ap.add_argument("--key-pass", required=True)
    ap.add_argument("--output", type=pathlib.Path, required=True)
    args = ap.parse_args()
    for tool in ("apktool", "zipalign", "apksigner"):
        if not shutil.which(tool):
            ap.error(f"Missing executable: {tool}")
    if not args.apk.is_file() or not args.lib.is_file():
        ap.error("APK and ARM64 library must exist")
    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="jev-apk-") as tmp:
        work = pathlib.Path(tmp)
        decoded = work / "decoded"
        run("apktool", "d", "-f", "-o", decoded, args.apk.resolve())
        original = patch_manifest(decoded)
        install_loader(decoded, original)
        libpath = decoded / "lib" / "arm64-v8a" / "libjevtool.so"
        libpath.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(args.lib, libpath)
        unsigned = work / "unsigned.apk"
        aligned = work / "aligned.apk"
        run("apktool", "b", "-o", unsigned, decoded)
        run("zipalign", "-f", "-p", "4", unsigned, aligned)
        run("apksigner", "sign", "--ks", args.keystore.resolve(),
            "--ks-key-alias", args.alias, "--ks-pass", "pass:" + args.store_pass,
            "--key-pass", "pass:" + args.key_pass, "--out", output, aligned)
        run("apksigner", "verify", "--verbose", output)
        print(f"Signed JEV loader APK: {output}")
        print("Loader is installed; in-game rendering/input hooking is NOT installed.")


if __name__ == "__main__":
    main()
