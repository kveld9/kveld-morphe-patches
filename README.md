<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Target-Brave_Browser-FF4500?style=for-the-badge&logo=brave&logoColor=white" />
  <img src="https://img.shields.io/badge/Target-Gboard_Lite-4285F4?style=for-the-badge&logo=google&logoColor=white" />
  <img src="https://img.shields.io/badge/Runtime-Morphe_Patcher_1.8.0-8A2BE2?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge" />
</p>

<h1 align="center">🔮 Morphe Patches</h1>

<p align="center">
  Modular bytecode, resource, and native patch suite for <b>Brave Browser</b> and <b>Gboard Lite</b> on Android using the <b><a href="https://morphe.software">Morphe</a></b> patcher framework.
</p>

<p align="center">
  <a href="https://morphe.software/add-source?github=kveld9/kveld-morphe-patches"><img src="https://img.shields.io/badge/Morphe_Manager-Add_Patch_Source-8A2BE2?style=for-the-badge&logo=android" /></a>
</p>

---

## 📖 Overview

This repository provides modular, high-performance bytecode, resource, and native patches for Android applications built for the **[Morphe](https://morphe.software)** patcher runtime.

### 🌟 Key Highlights

#### 📱 Gboard Lite

- **🖤 Pure Black AMOLED Theme**: Injects native Pure Black palette (`style_sheet_color_black.binarypb`) alongside standard light/dark/dynamic themes.
- **🎨 UI Enhancements**: Activates Key Shape Selection (Default, Semi-rounded, Round) and the redesigned Access Points Menu (Panel V2).
- **🔒 Hardened Privacy & Debloat**: Neutralizes Google Primes profiling, WorkManager background wakeups, Phenotype sync, MDD background sync, Tenor tracking, in-app training, diagnostics telemetry, and forces Incognito mode.
- **🛡️ Signature Bypass**: Neutralizes internal APK signature validation to allow custom execution.

#### 🦁 Brave Browser

- **🔓 Brave Origin Unlock**: Unlocks Brave Origin features and developer toggles.
- **🚫 Complete Telemetry Block**: Strips P3A product analytics, stats pings, crash dump uploads, and variations seed fetching at both native (`libchrome.so`) and bytecode levels.
- **⚡ Performance & Battery Optimization**: Eliminates background scheduler wakeups (Job ID 105), OEM partner carrier customizations, BatteryStatusManager broadcast listeners, and Background / Periodic Sync tasks.
- **🔄 UI & Gesture Control**: Disables accidental pull-to-refresh overscroll reloads and streamlines the First Run Experience.

---

### 📲 Add to Morphe Manager

Click the badge above or add `kveld9/kveld-morphe-patches` directly into your Morphe Manager sources:

👉 **[Add Morphe Patches to Morphe Manager](https://morphe.software/add-source?github=kveld9/kveld-morphe-patches)**

---

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
<details open>
<summary>📦 Gboard Lite&nbsp;&nbsp;•&nbsp;&nbsp;17 patches</summary>
<br>

**🎯 Supported versions:**

| 18.0.3.954559732-lite_beta-arm64-v8a |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Add AMOLED Theme](#add-amoled-theme) | Adds a selectable Pure Black AMOLED theme using Gboard's native color_black theme package, without replacing Dark, Light, System Auto, or Dynamic Color. |  |
| [Allow Modified APK](#allow-modified-apk) | Bypasses internal signature check to allow custom APK execution. |  |
| [Block Telemetry](#block-telemetry) | Disables background metrics dispatch, event logging, daily pings, and crash reporting. |  |
| [Clone Gboard](#clone-gboard) | Changes the package name by appending a dot and custom suffix (defaults to 'kveld') to allow installing Gboard alongside the original application. | • Package name suffix |
| [Disable Diagnostics](#disable-diagnostics) | Disables Google's diagnostic and recovery telemetry. |  |
| [Disable Google Primes](#disable-google-primes) | Neutralizes Google Primes performance profiling, jank monitoring, native crash sidecars, and background telemetry threads. |  |
| [Disable In-App Training](#disable-in-app-training) | Neutralizes periodic in-app training cache generation and federated-learning background work while preserving normal keyboard functionality and explicit user-requested operations. |  |
| [Disable MDD Background Sync](#disable-mdd-background-sync) | Neutralizes Mobile Data Download (MDD) periodic background synchronization, automated polling, and prefetch worker tasks while preserving on-demand and user-requested downloads. |  |
| [Disable Remote Configuration](#disable-remote-configuration) | Disables periodic remote experiment flag synchronization and background updates. |  |
| [Disable Superpacks Eager Sync](#disable-superpacks-eager-sync) | Neutralizes eager background Superpacks synchronization during application startup, eliminating unnecessary disk sweeps and network polling while preserving on-demand pack downloads. |  |
| [Disable Tenor Share Tracking](#disable-tenor-share-tracking) | Disables Tenor GIF selection and share tracking telemetry. |  |
| [Disable WorkManager](#disable-workmanager) | Neutralizes background WorkManager initialization, database creation, and periodic maintenance workers. |  |
| [Enable Access Points Menu Redesign](#enable-access-points-menu-redesign) | Enables the redesigned access points menu bar and customization panel (Panel V2). |  |
| [Enable Key Shape Selection](#enable-key-shape-selection) | Enables the key border shape selection UI (Default, Semi-rounded, Round) in theme customization. |  |
| [Force Incognito Mode](#force-incognito-mode) | Forces Gboard to always operate in incognito mode (disabling personalized learning and persistent input logging) while keeping clipboard functionality enabled. |  |
| [Hardened Intent Security](#hardened-intent-security) | Enables Gboard internal external intent protection against unauthorized intent hijacking. |  |
| [Resource Slimmer](#resource-slimmer) | Strips embedded third-party license text, onboarding tutorial Lottie animations, and promotional GIFs using dynamic content heuristics. |  |

</details>

<details open>
<summary>📦 Brave Private Web Browser, VPN&nbsp;&nbsp;•&nbsp;&nbsp;9 patches</summary>
<br>

**🎯 Supported versions:**

| 1.93.138 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Block Brave Telemetry](#block-brave-telemetry) | Blocks P3A product analytics, Brave Stats usage pings, crash dump uploads, WDP, and Variations seed fetching. |  |
| [Brave In-Product Notification Scheduler Optimization](#brave-in-product-notification-scheduler-optimization) | Eliminates periodic background wakeups and native library loading caused by Chromium in-product tips/promo scheduler (Job ID 105). |  |
| [Brave Origin](#brave-origin) | Unlocks Brave Origin and enables local feature toggle controls. |  |
| [Brave Startup Performance Optimization](#brave-startup-performance-optimization) | Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations. |  |
| [Disable Background Sync & Periodic Sync](#disable-background-sync-periodic-sync) | Eliminates background wakeups, radio modem activity, and battery drain by forcing GooglePlayServicesChecker.shouldDisableBackgroundSync() -> true and neutralizing wakeup tasks. |  |
| [Disable Battery Status API & OS Listener](#disable-battery-status-api-os-listener) | Neutralizes the Android BatteryStatusManager broadcast listener to prevent continuous OS battery wakeups. |  |
| [Disable Pull To Refresh](#disable-pull-to-refresh) | Completely disables the pull-to-refresh overscroll gesture and animation to prevent accidental page reloads. |  |
| [Locale PAK Slimmer](#locale-pak-slimmer) | Strips unselected language resource PAKs from assets/locales/. | • Locales to keep |
| [Skip First Run](#skip-first-run) | Skips the welcome screen, search engine selection, and onboarding First Run Experience (FRE) on clean installs. |  |

</details>

<!-- PATCHES_END -->

---

## ⚠️ Known Issues & Important Notes

### ⌨️ Predictive Text & Glide Typing in Gboard Lite (Fresh Installations)

> [!IMPORTANT]
> **Gboard Lite does not bundle language dictionaries, predictive text models, or gesture/glide typing decoding models inside the APK.**
> Unlike the full Gboard APK, Gboard Lite downloads language models on-demand upon first launch via Google's **MDD (Mobile Data Download)** and **Superpacks** subsystems.

If you perform a clean install of Gboard Lite with background sync debloat patches enabled, Gboard will be prevented from downloading the initial dictionary and gesture model pack for your language. This results in an empty suggestion bar, no predictive text, and **Glide Typing (swipe to type) not working**.

#### 🔧 How to set up Gboard Lite with working predictive text & glide typing:

1. **When patching for a clean install (or when adding new languages):**
    - **Temporarily uncheck:**
        - ❌ `Disable MDD Background Sync`
        - ❌ `Disable Superpacks Eager Sync`
        - ❌ `Disable WorkManager`
    - _(Also ensure `Force Incognito Mode` is unchecked if you want personalized learning and history)._
2. **Open Gboard once with an active Internet connection:**
    - Type a few words, test a swipe gesture, or navigate to _Gboard Settings > Languages_ so it downloads your language dictionary and gesture pack into local storage (`/data/data/...`).
3. **Re-apply debloat patches (Optional):**
    - Once your language packs are cached locally on device, you can re-patch with `Disable MDD Background Sync`, `Disable Superpacks Eager Sync`, and `Disable WorkManager` to freeze background network traffic and disk polling.

---

### ⌨️ Gboard Lite: Required Target APK Variant (APKMirror)

> [!IMPORTANT]
> **Always download the standalone `lite` / `lite_beta` 64-bit APK (nodpi) from [APKMirror](https://www.apkmirror.com/apk/google-inc/gboard/):**
>
> - **Current Target**: `18.0.3.954559732-lite_beta-arm64-v8a`
> - **Format**: `APK` _(Do **NOT** download `BUNDLE` / split packages)_
> - **Architecture**: `arm64-v8a`
> - **Screen DPI**: `nodpi`

#### ❓ Why Gboard Lite?

- **Lite Architecture**: Gboard Lite has a distinct, streamlined codebase designed for a lightweight footprint without bulky pre-bundled dictionaries. Standard/full Gboard APKs use different internal class layouts that fail AST fingerprint assertions (AMOLED theme and debloat hooks).

---

### 🦁 Brave Browser: Required Target APK (`Bravemonoarm64.apk`)

> [!IMPORTANT]
> **Always use `Bravemonoarm64.apk` from [Brave GitHub Releases](https://github.com/brave/brave-browser/releases).**
> Do **NOT** use `Bravearm64Universal.apk`, `Bravearm64.apk`, or 32-bit / x86 builds (`Bravearm.apk`, `Bravemonox64.apk`, etc.).

#### ❓ Why `Bravemonoarm64.apk`?

- **Bytecode Consistency**: `Bravemonoarm64.apk` is the official 64-bit ARM Monochrome build. Other variants (especially _Universal_) use different R8/ProGuard obfuscation passes and aggressive method inlining, which causes Dalvik fingerprint mismatches (e.g. in Variations seed fetching and Brave Origin helpers).
- **Native ARM64 Hooks**: Byte-level offsets in `lib/arm64-v8a/libchrome.so` (such as domain redirection and native scheduler gates) are calculated strictly against ARM64 Monochrome binaries.

---

### 🦁 Brave Locale PAK Slimmer: Valid Language Codes

The **`Locale PAK Slimmer`** patch allows you to strip unneeded language resource PAKs from `assets/locales/` to reduce APK size.

When configuring the **`Locales to keep`** option in Morphe Manager, specify a comma-separated list of locale codes (e.g. `es-419, es, en-US, pt-BR`). English (`en-US`) is always preserved automatically as an essential Chromium fallback.

#### 📋 Popular Language Codes:

| Language                         | Locale Code(s)                                                                    |
| :------------------------------- | :-------------------------------------------------------------------------------- |
| **Spanish**                      | `es` (Spain / Global), `es-419` (Latin America)                                   |
| **English**                      | `en-US` (US - _Always kept_), `en-GB` (UK)                                        |
| **Portuguese**                   | `pt-BR` (Brazil), `pt-PT` (Portugal)                                              |
| **French**                       | `fr` (France), `fr-CA` (Canada)                                                   |
| **German / Italian / Dutch**     | `de` (German), `it` (Italian), `nl` (Dutch)                                       |
| **Russian / Ukrainian / Polish** | `ru`, `uk`, `pl`                                                                  |
| **Japanese / Korean / Chinese**  | `ja`, `ko`, `zh-CN` (Simplified), `zh-TW` (Traditional), `zh-HK` (Hong Kong)      |
| **Nordic Languages**             | `sv` (Swedish), `da` (Danish), `fi` (Finnish), `nb` (Norwegian), `is` (Icelandic) |
| **Regional Languages of Spain**  | `ca` (Catalan), `gl` (Galician), `eu` (Basque)                                    |
| **Arabic / Turkish / Hebrew**    | `ar`, `tr`, `he`                                                                  |

<details>
<summary><b>🔍 View all 81 available locale codes in Brave</b></summary>
<br>

```text
af, am, ar, as, az, be, bg, bn, bs, ca, cs, da, de, el, en-GB, en-US, es, es-419,
et, eu, fa, fi, fil, fr, fr-CA, gl, gu, he, hi, hr, hu, hy, id, is, it, ja, ka,
kk, km, kn, ko, ky, lo, lt, lv, mk, ml, mn, mr, ms, my, nb, ne, nl, or, pa, pl,
pt-BR, pt-PT, ro, ru, si, sk, sl, sq, sr, sr-Latn, sv, sw, ta, te, th, tr, uk,
ur, uz, vi, zh-CN, zh-HK, zh-TW, zu
```

</details>

---

## 🛠️ Building & Development

### Prerequisites

- **JDK 17+** (JDK 21 recommended)
- **Gradle 9.6.1** (via included `./gradlew`)
- **Python 3.x** (for catalog synchronization)

### Build Commands

```bash
# Build Android extension DEX + standalone Morphe Patch Package (.mpp)
./gradlew buildAndroid

# Run checks and tests
./gradlew check

# Generate updated patches-list.json catalog
./gradlew generatePatchesList

# Sync README markdown tables with patch metadata
python .github/scripts/generate_patches_readme.py kveld9/kveld-morphe-patches main patches-list.json README.md
```

The compiled patch package will be available at:

```
patches/build/libs/patches-*.mpp
```

---

## 📜 License

Morphe Patches is open-source software licensed under the [GNU General Public License v3.0](LICENSE).
