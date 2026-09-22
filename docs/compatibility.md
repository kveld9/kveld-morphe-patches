# 📱 Compatibility & Architecture Policy

## CPU Architecture Support Policy (ARM64 vs ARMv7a)

> [!NOTE]
> **Architecture Matrix Summary:**
> - **Gboard Lite & Brave Browser**: Officially support both **`arm64-v8a` (64-bit)** and **`armeabi-v7a` (32-bit)**.
> - **TikTok**: Officially supports **`arm64-v8a`** (Global and Asia APK nodpi). Legacy `armeabi-v7a` libraries in dual-ABI APKs can be purged via debloat patches.
> - **Vivaldi Browser & Hevy**: **`arm64-v8a` (64-bit only)**.

### Why Gboard Lite supports 32-bit:
All Gboard Lite patches in this suite operate strictly on Dalvik/ART DEX bytecode and Android XML resources. They contain **zero native `.so` binary dependencies** and run identically on both 64-bit and 32-bit Android runtimes.

### Why Brave Browser supports both 64-bit and 32-bit:
Brave Browser provides official Monochrome release APKs for both architectures (`Bravemonoarm64.apk` and `BraveMonoarm.apk`). All Dalvik bytecode and XML resource patches apply identically across ABIs, while native patches (`Block Brave Telemetry` and `Native Bloat Slimmer`) maintain deterministic string redirection offsets and companion `.so` pruning across both `lib/arm64-v8a` and `lib/armeabi-v7a` without runtime memory overhead.

### Why Vivaldi Browser is strictly 64-bit:
Vivaldi Browser depends on surgical byte-level patching of the native ELF binary `lib/arm64-v8a/libchrome.so` (redirecting background telemetry, Crashpad crash uploaders, and DirectMatch endpoints to `0.0.0.0` at hardcoded 64-bit virtual memory and file offsets). Supporting 32-bit ARM would require reverse-engineering and maintaining an entirely separate set of 32-bit ELF binary offsets. **Please do not open issues requesting `armeabi-v7a` support for Vivaldi.**

### Why Hevy is strictly 64-bit:
Hevy runs on the Meta React Native framework with a pre-compiled 64-bit Hermes JavaScript runtime (`lib/arm64-v8a/libhermes.so`). The `Unlock Pro` patch performs dynamic Hermes Bytecode (HBC96) table parsing and surgical instruction replacement in `assets/index.android.bundle`.

---

## APK Variant Requirements

### 🦁 Brave Browser: `Bravemonoarm64.apk` & `BraveMonoarm.apk`
Download `Bravemonoarm64.apk` (for 64-bit ARM devices) or `BraveMonoarm.apk` (for 32-bit ARM devices) from [Brave GitHub Releases](https://github.com/brave/brave-browser/releases). Do **NOT** use `Bravearm64Universal.apk`, `Bravearm64.apk`, or x86 builds.

- **Bytecode Consistency**: Both official Monochrome builds share identical DEX bytecode structures and R8 obfuscation dictionaries.
- **Native Dual-ABI Hooks**: Native telemetry domain redirection and bloat slimming support both `lib/arm64-v8a` and `lib/armeabi-v7a`.

### 🔴 Vivaldi Browser: Stable Transition (`com.vivaldi.browser`)
Download the official `arm64-v8a` bundle release from [APKMirror](https://www.apkmirror.com/apk/vivaldi-technologies/vivaldi-browser-beta/).

- **Stable Target**: Morphe Patches has transitioned to the stable release of Vivaldi Browser (`com.vivaldi.browser`, v8.2.4147.77).
- **APKM Bundle Requirement**: Distributed as an Android App Bundle (`.apkm` / split APKs) containing `base.apk` and `split_chrome.apk`. **Do not download the ~320 MB standalone APK option on APKMirror**, as that is merely an unbundled `base.apk` stripped of browser bytecode. Select the **BUNDLE (`.apkm`)** file so Morphe can fuse split modules and apply the complete patch suite.

### 🏋️ Hevy: Gym Log Workout Tracker (`com.hevy`)
Download the official `arm64-v8a` bundle release from [APKMirror](https://www.apkmirror.com/apk/hevy/hevy-gym-log-workout-tracker/).

- **Current Target**: `3.1.14` (`com.hevy`, APKM bundle).
- **Bundle Format**: Distributed as an APKM / split APK set (`base.apk`, `split_config.arm64_v8a.apk`, `split_config.xxhdpi.apk`, etc.). Morphe patches both Dalvik bytecode in `classes*.dex`, manifest components in `AndroidManifest.xml`, and the Hermes bytecode bundle in `assets/index.android.bundle`.

### 🎵 TikTok: Global & Asia (`com.zhiliaoapp.musically` / `com.ss.android.ugc.trill`)
Download the official APK release from [APKMirror (TikTok)](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/) or [APKMirror (TikTok Asia)](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/).

- **Package Names**: `com.zhiliaoapp.musically` (Global) and `com.ss.android.ugc.trill` (Asia).
- **APK Format**: Standalone nodpi APK (`arm64-v8a` or dual-ABI `arm64-v8a, armeabi-v7a`).
- **Compatibility**: Patches operate on stable ByteDance SDK boundaries and MultiDEX classes across versions, with slimmers targeting ByteDance assets and heavy native libraries.

---

## Cross-Browser Patch Policy: Brave vs. Vivaldi

While both browsers derive from Chromium, their underlying engine modifications differ fundamentally:

| Candidate Feature / Patch | Vivaldi Browser | Brave Browser | Technical Rationale |
| :--- | :--- | :--- | :--- |
| **Privacy Sandbox Attestations** | **Applied** (`Resource Slimmer`) | **Omitted** | Brave strips Google Topics, Protected Audience, and Attribution Reporting at the C++ engine level (`brave-core`). Pre-bundled `.dat` assets are uncallable by web content. |
| **UKM Metrics Neutralization** | **Applied** (`Block Telemetry`) | **Omitted** | Brave replaces Chromium UMA/UKM reporting pipelines with its own P3A/WDP telemetry in C++. `Block Brave Telemetry` intercepts P3A and WDP at bytecode and network levels (`0.0.0.0`), rendering upstream UKM hooks obsolete. |
| **Close Tabs on Exit** | **Applied** (Bytecode hook) | **Omitted** | Brave provides a native, first-party toggle in *Settings -> Close tabs on exit*. Bytecode overrides would break user configuration. |
| **Sensor Privacy Guard** | **Applied** (Shared patch) | **Applied** (Shared patch) | Both browsers expose W3C Generic Sensor APIs to web content. Intercepting `PlatformSensor` prevents gyroscope/accelerometer fingerprinting. |
| **Clean Share URL** | **Applied** (Shared patch) | **Applied** (Shared patch) | Both browsers invoke Android share intents and clipboard setters with tracking tokens. Intercepting share/clipboard sanitizes query parameters across both. |

