# 📱 Compatibility & Architecture Policy

Comprehensive guide to CPU architecture support, APK variant selection, and application-specific requirements across all targets supported by Morphe Patches.

---

## 📋 Target Applications & Dedicated Documentation

Each application has an authoritative, all-in-one guide covering prerequisites, patch catalogs, configuration parameters, and technical architecture:

| Application | Supported Version | Architectures | Target Format | Complete Documentation |
| :--- | :--- | :--- | :--- | :--- |
| **Brave Browser** | `1.97.59` | `arm64-v8a`, `armeabi-v7a` | Standalone APK (`Bravemonoarm64.apk` / `BraveMonoarm.apk`) | [Brave Guide](apps/brave.md) |
| **Gboard Lite** | `18.5.1.993608717-lite_beta` | `arm64-v8a`, `armeabi-v7a` | Standalone nodpi APK | [Gboard Lite Guide](apps/gboard.md) |
| **TikTok** | `47.1.4` | `arm64-v8a` | Standalone nodpi APK | [TikTok Guide](apps/tiktok.md) |
| **Universal Optimizations** | *Universal* | All Architectures | Any Android APK | [Universal Patches Guide](universal-patches.md) |

---

## CPU Architecture Support Policy (ARM64 vs ARMv7a)

> [!NOTE]
> **Architecture Matrix Summary:**
> - **Gboard Lite & Brave Browser**: Officially support both **`arm64-v8a` (64-bit)** and **`armeabi-v7a` (32-bit)**.
> - **TikTok**: Officially supports **`arm64-v8a`** (Global APK nodpi). Legacy `armeabi-v7a` libraries in dual-ABI APKs can be purged via debloat patches.

### Why Gboard Lite supports 32-bit:
All Gboard Lite patches in this suite operate strictly on Dalvik/ART DEX bytecode and Android XML resources. They contain **zero native `.so` binary dependencies** and run identically on both 64-bit and 32-bit Android runtimes. Consult the [Gboard Lite Guide](apps/gboard.md) for dictionary setup instructions.

### Why Brave Browser supports both 64-bit and 32-bit:
Brave Browser provides official Monochrome release APKs for both architectures (`Bravemonoarm64.apk` and `BraveMonoarm.apk`). All Dalvik bytecode and XML resource patches apply identically across ABIs, while native patches (`Block Telemetry` and `Remove Unused AI & VPN`) maintain deterministic string redirection offsets and companion `.so` pruning across both `lib/arm64-v8a` and `lib/armeabi-v7a` without runtime memory overhead. Consult the [Brave Guide](apps/brave.md).

---

## APK Variant Requirements

### 🦁 Brave Browser: `Bravemonoarm64.apk` & `BraveMonoarm.apk`
Download `Bravemonoarm64.apk` (for 64-bit ARM devices) or `BraveMonoarm.apk` (for 32-bit ARM devices) from [Brave GitHub Releases](https://github.com/brave/brave-browser/releases). Do **NOT** use `Bravearm64Universal.apk`, `Bravearm64.apk`, or x86 builds. Details in [Brave Guide](apps/brave.md).

### ⌨️ Gboard Lite: Standalone nodpi APK
Download the standalone `lite` or `lite_beta` APK (nodpi) from [APKMirror](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-1-993608717-beta/). Do **NOT** download split packages or bundle formats. Details in [Gboard Lite Guide](apps/gboard.md).

### 🎵 TikTok (`com.zhiliaoapp.musically`)
Download the official standalone APK release from [APKMirror (TikTok)](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/).
- **Current Target**: `47.1.4` (nodpi APK, `arm64-v8a`). Details in [TikTok Guide](apps/tiktok.md).
