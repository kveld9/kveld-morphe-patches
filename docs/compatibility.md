# 📱 Compatibility & Architecture Policy

Comprehensive guide to CPU architecture support, APK variant selection, and application-specific requirements across all targets supported by Morphe Patches.

---

## 📋 Target Applications & Dedicated Documentation

Each application has an authoritative, all-in-one guide covering prerequisites, patch catalogs, configuration parameters, and technical architecture:

| Application | Supported Version | Architectures | Target Format | Complete Documentation |
| :--- | :--- | :--- | :--- | :--- |
| **Brave Browser** | `1.96.59` | `arm64-v8a`, `armeabi-v7a` | Standalone APK (`Bravemonoarm64.apk` / `BraveMonoarm.apk`) | [Brave Guide](apps/brave.md) |
| **Gboard Lite** | `18.2.4.969776716` | `arm64-v8a`, `armeabi-v7a` | Standalone nodpi APK | [Gboard Lite Guide](apps/gboard.md) |
| **Hevy** | `3.1.14` | `arm64-v8a` (64-bit only) | Split APK Bundle (`.apkm`) | [Hevy Guide](apps/hevy.md) |
| **NokoPrint** | `5.28.4` | `arm64-v8a`, `armeabi-v7a`, `universal` | Standalone nodpi APK / Bundle | [NokoPrint Guide](apps/nokoprint.md) |
| **TikTok** | `47.0.3` | `arm64-v8a` | Standalone nodpi APK (Global & Asia) | [TikTok Guide](apps/tiktok.md) |
| **Vivaldi Browser** | `8.2.4147.93` | `arm64-v8a` (64-bit only) | Split APK Bundle (`.apkm`) | [Vivaldi Guide](apps/vivaldi.md) |
| **Xiaomi Earbuds** | `1.38.0i` | `arm64-v8a`, `armeabi-v7a`, `universal` | Split Bundle (`.xapk` / `.apkm`) | [Xiaomi Earbuds Guide](apps/xiaomi-earbuds.md) |
| **Universal Optimizations** | *Universal* | All Architectures | Any Android APK | [Universal Patches Guide](universal-patches.md) |

---

## CPU Architecture Support Policy (ARM64 vs ARMv7a)

> [!NOTE]
> **Architecture Matrix Summary:**
> - **Gboard Lite, Brave Browser, NokoPrint & Xiaomi Earbuds**: Officially support both **`arm64-v8a` (64-bit)** and **`armeabi-v7a` (32-bit)**.
> - **TikTok**: Officially supports **`arm64-v8a`** (Global and Asia APK nodpi). Legacy `armeabi-v7a` libraries in dual-ABI APKs can be purged via debloat patches.
> - **Vivaldi Browser & Hevy**: **`arm64-v8a` (64-bit only)**.

### Why Gboard Lite supports 32-bit:
All Gboard Lite patches in this suite operate strictly on Dalvik/ART DEX bytecode and Android XML resources. They contain **zero native `.so` binary dependencies** and run identically on both 64-bit and 32-bit Android runtimes. Consult the [Gboard Lite Guide](apps/gboard.md) for dictionary setup instructions.

### Why Brave Browser supports both 64-bit and 32-bit:
Brave Browser provides official Monochrome release APKs for both architectures (`Bravemonoarm64.apk` and `BraveMonoarm.apk`). All Dalvik bytecode and XML resource patches apply identically across ABIs, while native patches (`Block Brave Telemetry` and `Native Bloat Slimmer`) maintain deterministic string redirection offsets and companion `.so` pruning across both `lib/arm64-v8a` and `lib/armeabi-v7a` without runtime memory overhead. Consult the [Brave Guide](apps/brave.md).

### Why Vivaldi Browser is strictly 64-bit:
Vivaldi Browser depends on surgical byte-level patching of the native ELF binary `lib/arm64-v8a/libchrome.so` (redirecting background telemetry, Crashpad crash uploaders, and DirectMatch endpoints to `0.0.0.0` at hardcoded 64-bit virtual memory and file offsets). Supporting 32-bit ARM would require reverse-engineering and maintaining an entirely separate set of 32-bit ELF binary offsets. **Please do not open issues requesting `armeabi-v7a` support for Vivaldi.** Consult the [Vivaldi Guide](apps/vivaldi.md).

### Why Hevy is strictly 64-bit:
Hevy runs on the Meta React Native framework with a pre-compiled 64-bit Hermes JavaScript runtime (`lib/arm64-v8a/libhermes.so`). The `Unlock Pro` patch performs dynamic Hermes Bytecode (HBC96) table parsing and surgical instruction replacement in `assets/index.android.bundle`. Consult the [Hevy Guide](apps/hevy.md).

### Why Xiaomi Earbuds & NokoPrint support both 64-bit and 32-bit:
All patches for Xiaomi Earbuds and NokoPrint operate strictly on Dalvik/ART MultiDEX bytecode and Android XML resources. They contain **zero native `.so` binary dependencies** and run identically across all Android CPU architectures (`arm64-v8a`, `armeabi-v7a`, `universal`). Consult the [Xiaomi Earbuds Guide](apps/xiaomi-earbuds.md) and [NokoPrint Guide](apps/nokoprint.md).

---

## APK Variant Requirements

### 🦁 Brave Browser: `Bravemonoarm64.apk` & `BraveMonoarm.apk`
Download `Bravemonoarm64.apk` (for 64-bit ARM devices) or `BraveMonoarm.apk` (for 32-bit ARM devices) from [Brave GitHub Releases](https://github.com/brave/brave-browser/releases). Do **NOT** use `Bravearm64Universal.apk`, `Bravearm64.apk`, or x86 builds. Details in [Brave Guide](apps/brave.md).

### ⌨️ Gboard Lite: Standalone nodpi APK
Download the standalone `lite` or `lite_beta` APK (nodpi) from [APKMirror](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-2-4-969776716-release/). Do **NOT** download split packages or bundle formats. Details in [Gboard Lite Guide](apps/gboard.md).

### 🔴 Vivaldi Browser: Stable Transition (`com.vivaldi.browser`)
Download the official `arm64-v8a` bundle release from [APKMirror](https://www.apkmirror.com/apk/vivaldi-technologies/vivaldi-browser-beta/).
- **Current Target**: `8.2.4147.93` (`com.vivaldi.browser`, arm64-v8a APKM bundle).
- **APKM Bundle Requirement**: Distributed as an Android App Bundle (`.apkm` / split APKs) containing `base.apk` and `split_chrome.apk`. **Do not download the standalone APK option on APKMirror**, as that is merely an unbundled `base.apk` stripped of browser bytecode. Select the **BUNDLE (`.apkm`)** file so Morphe can fuse split modules. Details in [Vivaldi Guide](apps/vivaldi.md).

### 🏋️ Hevy: Gym Log Workout Tracker (`com.hevy`)
Download the official `arm64-v8a` bundle release from [APKMirror](https://www.apkmirror.com/apk/hevy/hevy-gym-log-workout-tracker/).
- **Current Target**: `3.1.14` (`com.hevy`, APKM bundle).
- **Bundle Format**: Distributed as an APKM / split APK set. Morphe fuses split modules into a unified APK. Details in [Hevy Guide](apps/hevy.md).

### 🖨️ NokoPrint: WiFi, Bluetooth, USB (`com.nokoprint`)
Download the official release from [APKPure](https://d.apkpure.com/b/XAPK/com.nokoprint?versionCode=52804).
- **Current Target**: `5.28.4` (`com.nokoprint`, standalone nodpi APK or bundle). Details in [NokoPrint Guide](apps/nokoprint.md).

### 🎵 TikTok: Global & Asia (`com.zhiliaoapp.musically` / `com.ss.android.ugc.trill`)
Download the official standalone APK release from [APKMirror (TikTok)](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/) or [APKMirror (TikTok Asia)](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/).
- **Current Target**: `47.0.3` (nodpi APK, `arm64-v8a`). Details in [TikTok Guide](apps/tiktok.md).

### 🎧 Xiaomi Earbuds (`com.mi.earphone`)
Download the official release from [APKPure](https://apkpure.com/xiaomi-earbuds/com.mi.earphone).
- **Current Target**: `1.38.0i` (`com.mi.earphone`, XAPK bundle or standalone APK). Details in [Xiaomi Earbuds Guide](apps/xiaomi-earbuds.md).

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
