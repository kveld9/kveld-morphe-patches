<p align="center">
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases/latest"><img src="https://img.shields.io/github/v/release/kveld9/kveld-morphe-patches?color=7928CA&label=Release&logo=github&style=for-the-badge" alt="Latest Release" /></a>
  <a href="https://github.com/kveld9/kveld-morphe-patches/actions/workflows/release.yml"><img src="https://img.shields.io/github/actions/workflow/status/kveld9/kveld-morphe-patches/release.yml?branch=main&label=CI&logo=githubactions&logoColor=white&style=for-the-badge" alt="Build Status" /></a>
  <img src="https://img.shields.io/badge/Runtime-Morphe_Patcher_1.8.0-8A2BE2?style=for-the-badge" alt="Runtime" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge" alt="License" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform Android" />
  <img src="https://img.shields.io/badge/Target-Brave_Browser-FF4500?style=for-the-badge&logo=brave&logoColor=white" alt="Target Brave" />
  <img src="https://img.shields.io/badge/Target-Vivaldi_Browser-EF3939?style=for-the-badge&logo=vivaldi&logoColor=white" alt="Target Vivaldi" />
  <img src="https://img.shields.io/badge/Target-Gboard_Lite-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="Target Gboard" />
  <img src="https://img.shields.io/badge/Target-Hevy_Gym_Log-FF4500?style=for-the-badge&logo=target&logoColor=white" alt="Target Hevy" />
</p>

<h1 align="center">🔮 Morphe Patches</h1>

<p align="center">
  Modular bytecode, resource, and native patch suite for <b>Brave Browser</b>, <b>Vivaldi Browser</b>, <b>Gboard Lite</b>, and <b>Hevy</b> on Android using the <b><a href="https://morphe.software">Morphe</a></b> patcher framework.
</p>

<p align="center">
  <a href="https://morphe.software/add-source?github=kveld9/kveld-morphe-patches"><img src="https://img.shields.io/badge/Morphe_Manager-Add_Patch_Source-8A2BE2?style=for-the-badge&logo=android&logoColor=white" alt="Add Source to Morphe Manager" /></a>
  &nbsp;&nbsp;
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases/latest"><img src="https://img.shields.io/badge/Direct_Download-Get_.MPP_Bundle-0070F3?style=for-the-badge&logo=github&logoColor=white" alt="Download Latest Release" /></a>
</p>

---

## 📖 Overview

**Morphe Patches** enhances target applications with native Pure Black AMOLED themes, comprehensive telemetry and diagnostic blocking, background task debloating, cold-start performance optimizations, and APK footprint reduction across **Gboard Lite**, **Brave Browser**, **Vivaldi Browser**, and **Hevy**.

---

## 🚀 Quick Start

1. **Install Morphe Manager:** Download and install the latest **[Morphe Manager](https://morphe.software)** on your Android device.
2. **Add Patch Source:** Tap the badge above or navigate to *Settings > Sources* in Morphe Manager and add:
   ```text
   kveld9/kveld-morphe-patches
   ```
   *(Or click: **[Add Source to Morphe Manager](https://morphe.software/add-source?github=kveld9/kveld-morphe-patches)**)*
3. **Download Supported APK:** Obtain the exact target APK variant from [Supported Targets & Downloads](#-supported-targets--downloads).
4. **Select Patches:** Pick the patches you want to apply (privacy hardening, AMOLED theme, slimming, etc.).
5. **Patch & Install:** Tap **Patch**, wait for compilation to complete, and install your enhanced APK.

---

## 🎯 Supported Targets & Downloads

> [!TIP]
> For architecture compatibility details (ARM64 vs 32-bit ARMv7a) and variant selection guidelines, see the [Compatibility Guide](docs/compatibility.md).

### ⌨️ Gboard Lite
- **Current Target**: `18.2.4.969776716-lite_beta-arm64-v8a` (or `armeabi-v7a`)
- **Architecture**: `arm64-v8a` or `armeabi-v7a` (nodpi APK)
- **Source**: [APKMirror (Gboard Lite)](https://www.apkmirror.com/apk/google-inc/gboard/)
- ⚠️ *Fresh install?* Read the [Gboard Offline Dictionary & Glide Typing Setup](docs/gboard-setup.md) before applying background debloat patches.

### 🦁 Brave Browser
- **Current Target**: `1.94.121` (`Bravemonoarm64.apk`)
- **Architecture**: `arm64-v8a` (Monochrome only)
- **Package**: `com.brave.browser`

<a href="https://github.com/brave/brave-browser/releases/download/v1.94.121/Bravemonoarm64.apk"><img src="https://img.shields.io/badge/Download-Bravemonoarm64.apk_(v1.94.121)-FF4500?style=for-the-badge&logo=brave&logoColor=white" alt="Download Brave APK" /></a>

### 🔴 Vivaldi Browser
- **Current Target**: `8.2.4147.58` (APKM bundle or standalone APK)
- **Architecture**: `arm64-v8a`
- **Package**: `com.vivaldi.browser`

<a href="https://www.apkmirror.com/apk/vivaldi-technologies/vivaldi-browser-beta/vivaldi-browser-fast-safe-8-2-4147-58-release/"><img src="https://img.shields.io/badge/Download-Vivaldi_8.2.4147.58_(APKM)-EF3939?style=for-the-badge&logo=vivaldi&logoColor=white" alt="Download Vivaldi APK" /></a>

### 🏋️ Hevy - Gym Log Workout Tracker
- **Current Target**: `3.1.13` (APKM bundle)
- **Architecture**: `arm64-v8a`
- **Package**: `com.hevy`

<a href="https://www.apkmirror.com/apk/hevy/hevy-gym-log-workout-tracker/hevy-gym-log-workout-tracker-3-1-13-release/"><img src="https://img.shields.io/badge/Download-Hevy_3.1.13_(APKM)-FF4500?style=for-the-badge&logo=target&logoColor=white" alt="Download Hevy APKM" /></a>

---

## 💊 Patch Catalog

<!-- PATCHES_START EXPANDED -->
<details open>
<summary>📦 Gboard Lite&nbsp;&nbsp;•&nbsp;&nbsp;16 patches</summary>
<br>

**🎯 Supported versions:**

| 18.2.4.969776716-lite_beta-arm64-v8a | 18.2.4.969776716-lite_beta-armeabi-v7a |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| **Add AMOLED Theme** | Adds a selectable Pure Black AMOLED theme using Gboard's native color_black theme package, without replacing Dark, Light, System Auto, or Dynamic Color. |  |
| **Allow Modified APK** | Bypasses internal signature check to allow custom APK execution. |  |
| **Block Telemetry** | Disables background metrics dispatch, event logging, daily pings, and crash reporting. |  |
| **Clone Gboard** | Changes the package name by appending a dot and custom suffix (defaults to 'kveld') to allow installing Gboard alongside the original application. | • Package name suffix |
| **Disable Diagnostics** | Disables Google's diagnostic and recovery telemetry. |  |
| **Disable Google Primes** | Neutralizes Google Primes performance profiling, jank monitoring, native crash sidecars, and background telemetry threads. |  |
| **Disable MDD Background Sync** | Neutralizes Mobile Data Download (MDD) periodic background synchronization, automated polling, and prefetch worker tasks while preserving on-demand and user-requested downloads. |  |
| **Disable Remote Configuration** | Disables periodic remote experiment flag synchronization and background updates. |  |
| **Disable Superpacks Eager Sync** | Neutralizes eager background Superpacks synchronization during application startup, eliminating unnecessary disk sweeps and network polling while preserving on-demand pack downloads. |  |
| **Disable Tenor Share Tracking** | Disables Tenor GIF selection and share tracking telemetry. |  |
| **Disable WorkManager** | Neutralizes background WorkManager initialization, database creation, and periodic maintenance workers. |  |
| **Enable Access Points Menu Redesign** | Enables the redesigned access points menu bar and customization panel (Panel V2). |  |
| **Enable Key Shape Selection** | Enables the key border shape selection UI (Default, Semi-rounded, Round) in theme customization. |  |
| **Force Incognito Mode** | Forces Gboard to always operate in incognito mode (disabling personalized learning and persistent input logging) while keeping clipboard functionality enabled. |  |
| **Hardened Intent Security** | Enables Gboard internal external intent protection against unauthorized intent hijacking. |  |
| **Resource Slimmer** | Strips embedded third-party license text, onboarding tutorial Lottie animations, promotional GIFs, and APK root metadata/junk files. |  |

</details>

<details open>
<summary>📦 Brave Private Web Browser, VPN&nbsp;&nbsp;•&nbsp;&nbsp;10 patches</summary>
<br>

**🎯 Supported versions:**

| 1.94.121 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| **Block Brave Telemetry** | Blocks P3A product analytics, Brave Stats usage pings, crash dump uploads, WDP, and Variations seed fetching. |  |
| **Brave In-Product & Commercial Notification Optimizer** | Eliminates background wakeups and notifications from Chromium tips scheduler (Job ID 105), Brave Rewards onboarding promo, and retention marketing campaigns. |  |
| **Brave Origin** | Unlocks Brave Origin and enables local feature toggle controls. |  |
| **Brave Startup Performance Optimization** | Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations. |  |
| **Disable Background Sync & Periodic Sync** | Eliminates background wakeups, radio modem activity, and battery drain by forcing GooglePlayServicesChecker.shouldDisableBackgroundSync() -> true and neutralizing wakeup tasks. |  |
| **Disable Battery Status API & OS Listener** | Neutralizes the Battery Status API (navigator.getBattery) to prevent cross-site device fingerprinting and drops OS battery change broadcasts. |  |
| **Disable Pull To Refresh** | Completely disables the pull-to-refresh overscroll gesture and animation to prevent accidental page reloads. |  |
| **Locale PAK Slimmer** | Strips unselected language resource PAKs from assets/locales/. | • Locales to keep |
| **Native Bloat Slimmer** | Strips unused native companion binaries (Impress Vision AI, WireGuard VPN, and Android XR) to significantly reduce APK size. |  |
| **Skip First Run** | Skips the welcome screen, search engine selection, and onboarding First Run Experience (FRE) on clean installs. |  |

</details>

<details open>
<summary>📦 Hevy - Gym Log Workout Tracker&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 3.1.13 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| **Block Hevy Telemetry & Trackers** | Neutralizes Sentry crash reporting, Adjust attribution, Facebook AppEvents, Branch referral tracking, and WearOS background sync. |  |
| **Hevy Battery Optimization & Background Sync Killer** | Disables background WorkManager alarms, periodic job schedulers, Firebase messaging wakeups, and DataTransport schedulers to eliminate battery drain in sleep. |  |
| **Hevy Resource Slimmer** | Strips embedded onboarding MP4 tutorial video, heavy IMG.LY photo editor stickers/textures, and compiler junk metadata. |  |
| **Unlock Pro** | Unlocks local Hevy Pro capabilities (unlimited workout routines, routine folders, advanced graphs, and local analytics) by dynamically enabling the offline-Pro grace period in Hermes Bytecode (HBC96). |  |

</details>

<details open>
<summary>📦 Vivaldi Browser - Fast & Safe&nbsp;&nbsp;•&nbsp;&nbsp;12 patches</summary>
<br>

**🎯 Supported versions:**

| 8.2.4147.58 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| **Block Vivaldi Sync** | Redirects bifrost.vivaldi.com to 0.0.0.0 in libchrome.so to completely disable Vivaldi account synchronization for an air-gapped experience. |  |
| **Block Vivaldi Telemetry** | Blocks Vivaldi usage statistics, crash reporting, Crashpad endpoints, and DirectMatch suggestions. |  |
| **Clean Speed Dial Bookmarks** | Removes sponsored commercial affiliate bookmarks, hides the 3-dot customize button, and disables phantom touch actions on the blank Start Page. |  |
| **Close Tabs on Exit** | Prevents tab restoration on startup, ensuring Vivaldi always opens with a clean Start Page session regardless of how the app was terminated. |  |
| **Disable Background Sync & Periodic Sync** | Prevents Web Background Sync and Periodic Sync APIs from scheduling wakeups and background tasks in Android JobScheduler. |  |
| **Disable Battery Status API & OS Listener** | Neutralizes the Battery Status API (navigator.getBattery) to prevent cross-site device fingerprinting and drops OS battery change broadcasts. |  |
| **Disable Chromium Tips & Notification Scheduler** | Eliminates periodic background wakeups, native library loading, and in-product promotional tips from Chromium tips scheduler. |  |
| **Disable Vivaldi Prompts & In-App Popups** | Disables in-app donation prompts, search engine switch promos, rate-app popups, background privacy report alarms, and default browser promotional handlers. |  |
| **Locale PAK Slimmer** | Strips unselected language resource PAKs from assets/locales/. | • Locales to keep |
| **Resource Slimmer** | Strips bundled stock wallpapers and sponsored Speed Dial thumbnails to reduce APK size. |  |
| **Skip First Run** | Skips the welcome screen, search engine selection, and onboarding First Run Experience (FRE) on clean installs. |  |
| **Vivaldi Startup Performance Optimization** | Guards against fatal CompositorView NullPointerException crashes on cold startup and bypasses carrier partner customizations. |  |

</details>

<details open>
<summary>🌐 Universal&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| **APK Junk Cleaner** | Strips non-functional build metadata, compiler properties, Kotlin coroutines debug tables, and duplicate license texts from META-INF and APK root. |  |
| **DPI Resource Slimmer** | Strips unselected screen density resource directories from res/ (e.g. drawable-mdpi, drawable-hdpi, mipmap-xhdpi). Density-independent resources (nodpi, anydpi) and orphan resources are safely preserved. | • DPI densities to keep |
| **Locale Resource Slimmer** | Strips unselected language translation directories from res/ (e.g. values-*, raw-*, xml-*). Base fallback resources with no language qualifiers are always preserved. | • Locales to keep |
| **PNG Asset Optimizer** | Losslessly recompresses PNG assets with maximum zlib compression and strips non-rendering metadata chunks (pHYs, tEXt, tIME) while preserving 9-patch structures and pixel accuracy. |  |

</details>

<!-- PATCHES_END -->

---

## 📚 Documentation & Guides

Comprehensive technical references and setup manuals have been modularized for fast navigation:

| Guide | Description |
| :--- | :--- |
| 📱 **[Compatibility Guide](docs/compatibility.md)** | CPU architecture policy (ARM64 vs 32-bit ARMv7a) and APK variant rationales (`Bravemonoarm64`, Vivaldi Browser). |
| ⌨️ **[Gboard Setup & Dictionaries](docs/gboard-setup.md)** | Step-by-step setup for predictive text, offline dictionaries, and Glide Typing on fresh installs. |
| ⚙️ **[Patch Configuration & Options](docs/patch-configuration.md)** | Detailed guides, density matrices, and complete locale code tables for `Locale Slimmer` and `DPI Resource Slimmer`. |
| 🛡️ **[Architecture & Security Notes](docs/architecture-security.md)** | Explanation of privacy scanner false positives (ML Kit / Play Billing) and native ELF/Dalvik telemetry neutralization. |
| 🛠️ **[Building & Development](docs/building.md)** | Toolchain prerequisites, Gradle build tasks, test execution, and catalog synchronization. |

---

## 🤝 Credits & Contributors

| Contributor | Role & Contributions |
| :--- | :--- |
| <a href="https://github.com/Lxchoooo"><img src="https://github.com/Lxchoooo.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@Lxchoooo</b></a> | 🧪 Daily patch testing, runtime APK validation, and bug diagnostics. |
| <a href="https://github.com/ll0r3nt3"><img src="https://github.com/ll0r3nt3.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@ll0r3nt3</b></a> | 💡 Proposed DPI Resource Slimmer feature request ([#16](https://github.com/kveld9/kveld-morphe-patches/issues/16)). |

---

## 📜 License

Morphe Patches is open-source software licensed under the [GNU General Public License v3.0](LICENSE).
