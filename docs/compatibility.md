# 📱 Compatibility & Architecture Policy

## CPU Architecture Support Policy (ARM64 vs ARMv7a)

> [!NOTE]
> **Architecture Matrix Summary:**
> - **Gboard Lite**: Officially supports both **`arm64-v8a` (64-bit)** and **`armeabi-v7a` (32-bit)**.
> - **Brave Browser, Vivaldi Browser & Hevy**: **`arm64-v8a` (64-bit only)**.

### Why Gboard Lite supports 32-bit:
All Gboard Lite patches in this suite operate strictly on Dalvik/ART DEX bytecode and Android XML resources. They contain **zero native `.so` binary dependencies** and run identically on both 64-bit and 32-bit Android runtimes.

### Why Brave & Vivaldi are strictly 64-bit:
Both Chromium-based browsers depend on surgical byte-level patching of the native ELF binary `lib/arm64-v8a/libchrome.so` (redirecting background telemetry, Crashpad crash uploaders, and DirectMatch endpoints to `0.0.0.0` at hardcoded 64-bit virtual memory and file offsets).

Supporting 32-bit ARM for these browsers would require reverse-engineering and maintaining an entirely separate set of 32-bit ELF binary offsets for a legacy target. **Please do not open issues requesting `armeabi-v7a` support for Brave or Vivaldi.**

### Why Hevy is strictly 64-bit:
Hevy runs on the Meta React Native framework with a pre-compiled 64-bit Hermes JavaScript runtime (`lib/arm64-v8a/libhermes.so`). The `Unlock Pro` patch performs dynamic Hermes Bytecode (HBC96) table parsing and surgical instruction replacement in `assets/index.android.bundle`.

---

## APK Variant Requirements

### 🦁 Brave Browser: Why `Bravemonoarm64.apk`?
Always use `Bravemonoarm64.apk` from [Brave GitHub Releases](https://github.com/brave/brave-browser/releases). Do **NOT** use `Bravearm64Universal.apk`, `Bravearm64.apk`, or 32-bit/x86 builds.

- **Bytecode Consistency**: `Bravemonoarm64.apk` is the official 64-bit ARM Monochrome build. Other variants (especially *Universal*) use different R8/ProGuard obfuscation passes and aggressive method inlining, causing Dalvik fingerprint mismatches.
- **Native ARM64 Hooks**: Byte-level offsets in `lib/arm64-v8a/libchrome.so` are calculated strictly against ARM64 Monochrome binaries.

### 🔴 Vivaldi Browser: Stable Transition (`com.vivaldi.browser`)
Download the official `arm64-v8a` bundle release from [APKMirror](https://www.apkmirror.com/apk/vivaldi-technologies/vivaldi-browser-beta/).

- **Stable Target**: Morphe Patches has transitioned to the stable release of Vivaldi Browser (`com.vivaldi.browser`, v8.2.4147.58).
- **APKM Bundle Requirement**: Distributed as an Android App Bundle (`.apkm` / split APKs) containing `base.apk` and `split_chrome.apk`. **Do not download the ~320 MB standalone APK option on APKMirror**, as that is merely an unbundled `base.apk` stripped of browser bytecode. Select the **BUNDLE (`.apkm`)** file so Morphe can fuse split modules and apply the complete patch suite.

### 🏋️ Hevy: Gym Log Workout Tracker (`com.hevy`)
Download the official `arm64-v8a` bundle release from [APKMirror](https://www.apkmirror.com/apk/hevy/hevy-gym-log-workout-tracker/).

- **Current Target**: `3.1.13` (`com.hevy`, APKM bundle).
- **Bundle Format**: Distributed as an APKM / split APK set (`base.apk`, `split_config.arm64_v8a.apk`, `split_config.xxhdpi.apk`, etc.). Morphe patches both Dalvik bytecode in `classes*.dex`, manifest components in `AndroidManifest.xml`, and the Hermes bytecode bundle in `assets/index.android.bundle`.
