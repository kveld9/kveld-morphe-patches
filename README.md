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
  <a href="https://morphe.software/add-source?github=kveld9/morphe-patches"><img src="https://img.shields.io/badge/Morphe_Manager-Add_Patch_Source-8A2BE2?style=for-the-badge&logo=android" /></a>
</p>

---

## 📖 Overview

This repository provides modular, high-performance bytecode, resource, and native patches for Android applications built for the **[Morphe](https://morphe.software)** patcher runtime.

### 🌟 Key Highlights

#### 📱 Gboard Lite
- **🖤 Pure Black AMOLED Theme**: Injects native Pure Black palette (`style_sheet_color_black.binarypb`) alongside standard light/dark/dynamic themes.
- **🎯 2D Free Cursor Trackpad**: Unlocks two-dimensional cursor navigation with spacebar drag, including vertical movement and magnifier HUD.
- **🎨 UI Enhancements**: Activates Key Shape Selection (Default, Semi-rounded, Round) and the redesigned Access Points Menu (Panel V2).
- **🔒 Hardened Privacy & Debloat**: Neutralizes Google Primes profiling, WorkManager background wakeups, Phenotype sync, MDD background sync, Tenor tracking, in-app training, diagnostics telemetry, and forces Incognito mode.
- **🛡️ Signature Bypass**: Neutralizes internal APK signature validation to allow custom execution.

#### 🦁 Brave Browser
- **🔓 Brave Origin Unlock**: Unlocks Brave Origin features and developer toggles.
- **🚫 Complete Telemetry Block**: Strips P3A product analytics, stats pings, crash dump uploads, and variations seed fetching at both native (`libchrome.so`) and bytecode levels.
- **⚡ Performance & Battery Optimization**: Eliminates background scheduler wakeups (Job ID 105) and OEM partner carrier customizations.

---

### 📲 Add to Morphe Manager

Click the badge above or add `kveld9/morphe-patches` directly into your Morphe Manager sources:

👉 **[Add Morphe Patches to Morphe Manager](https://morphe.software/add-source?github=kveld9/morphe-patches)**

---

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
<details open>
<summary>📦 Gboard Lite&nbsp;&nbsp;•&nbsp;&nbsp;16 patches</summary>
<br>

**🎯 Supported versions:**

| 18.0.3.954559732-lite_beta-arm64-v8a |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| [Add AMOLED Theme](#add-amoled-theme) | Adds a selectable Pure Black AMOLED theme using Gboard's native color_black theme package, without replacing Dark, Light, System Auto, or Dynamic Color. |
| [Allow Modified APK](#allow-modified-apk) | Bypasses internal signature check to allow custom APK execution. |
| [Block Telemetry](#block-telemetry) | Disables background metrics dispatch, event logging, daily pings, and crash reporting. |
| [Disable Diagnostics](#disable-diagnostics) | Disables Google's diagnostic and recovery telemetry. |
| [Disable Google Primes](#disable-google-primes) | Neutralizes Google Primes performance profiling, jank monitoring, native crash sidecars, and background telemetry threads. |
| [Disable In-App Training](#disable-in-app-training) | Neutralizes periodic in-app training cache generation and federated-learning background work while preserving normal keyboard functionality and explicit user-requested operations. |
| [Disable MDD Background Sync](#disable-mdd-background-sync) | Neutralizes Mobile Data Download (MDD) periodic background synchronization, automated polling, and prefetch worker tasks while preserving on-demand and user-requested downloads. |
| [Disable Remote Configuration](#disable-remote-configuration) | Disables periodic remote experiment flag synchronization and background updates. |
| [Disable Superpacks Eager Sync](#disable-superpacks-eager-sync) | Neutralizes eager background Superpacks synchronization during application startup, eliminating unnecessary disk sweeps and network polling while preserving on-demand pack downloads. |
| [Disable Tenor Share Tracking](#disable-tenor-share-tracking) | Disables Tenor GIF selection and share tracking telemetry. |
| [Disable WorkManager](#disable-workmanager) | Neutralizes background WorkManager initialization, database creation, and periodic maintenance workers. |
| [Enable Access Points Menu Redesign](#enable-access-points-menu-redesign) | Enables the redesigned access points menu bar and customization panel (Panel V2). |
| [Enable Free Cursor 2D Trackpad](#enable-free-cursor-2d-trackpad) | Unlocks Gboard's 2D Free Cursor navigation through the spacebar, including vertical cursor movement and the magnifier HUD. |
| [Enable Key Shape Selection](#enable-key-shape-selection) | Enables the key border shape selection UI (Default, Semi-rounded, Round) in theme customization. |
| [Force Incognito Mode](#force-incognito-mode) | Forces Gboard to always operate in incognito mode, disabling personalized learning and persistent input logging across all sessions. |
| [Hardened Intent Security](#hardened-intent-security) | Enables Gboard internal external intent protection against unauthorized intent hijacking. |

</details>

<details open>
<summary>📦 Brave Private Web Browser, VPN&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 1.93.137 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| [Block Brave Telemetry](#block-brave-telemetry) | Blocks P3A product analytics, Brave Stats usage pings, crash dump uploads, WDP, and Variations seed fetching. |
| [Brave In-Product Notification Scheduler Optimization](#brave-in-product-notification-scheduler-optimization) | Eliminates periodic background wakeups and native library loading caused by Chromium in-product tips/promo scheduler (Job ID 105). |
| [Brave Origin](#brave-origin) | Unlocks Brave Origin and enables local feature toggle controls. |
| [Brave Startup Performance Optimization](#brave-startup-performance-optimization) | Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations. |

</details>

<!-- PATCHES_END -->

---

## ⚠️ Known Issues & Important Notes

### ⌨️ Predictive Text / Suggestions in Gboard Lite (Fresh Installations)

> [!IMPORTANT]
> **Gboard Lite does not bundle language dictionaries or prediction models inside the APK.**
> Unlike the full Gboard APK, Gboard Lite downloads language models on-demand upon first launch via Google's **MDD (Mobile Data Download)** and **Superpacks** subsystems.

If you perform a clean install of Gboard Lite with background sync debloat patches enabled, Gboard will be prevented from downloading the initial dictionary pack for your language, resulting in an empty suggestion bar and no predictive text.

#### 🔧 How to set up Gboard Lite with working predictive text:

1. **When patching for a clean install (or when adding new languages):**
   - **Temporarily uncheck:**
     - ❌ `Disable MDD Background Sync`
     - ❌ `Disable Superpacks Eager Sync`
     - ❌ `Disable WorkManager`
   - *(Also ensure `Force Incognito Mode` is unchecked if you want personalized learning and history).*
2. **Open Gboard once with an active Internet connection:**
   - Type a few words or navigate to *Gboard Settings > Languages* so it downloads your language dictionary pack into local storage (`/data/data/...`).
3. **Re-apply debloat patches (Optional):**
   - Once your language packs are cached locally on device, you can re-patch with `Disable MDD Background Sync`, `Disable Superpacks Eager Sync`, and `Disable WorkManager` to freeze background network traffic and disk polling.

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
python .github/scripts/generate_patches_readme.py kveld9/morphe-patches main patches-list.json README.md
```

The compiled patch package will be available at:
```
patches/build/libs/patches-*.mpp
```

---

## 📜 License

Morphe Patches is open-source software licensed under the [GNU General Public License v3.0](LICENSE).
