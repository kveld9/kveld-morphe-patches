<p align="center">
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases/latest"><img src="https://img.shields.io/github/v/release/kveld9/kveld-morphe-patches?color=7928CA&label=Release&logo=github&style=for-the-badge" alt="Latest Release" /></a>
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases"><img src="https://img.shields.io/github/downloads/kveld9/kveld-morphe-patches/total?style=for-the-badge&logo=github" alt="Total Downloads" /></a>
  <img src="https://img.shields.io/badge/Runtime-Morphe_Patcher_1.8.0-8A2BE2?style=for-the-badge" alt="Runtime" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge" alt="License" />
</p>

<h1 align="center">🔮 Morphe Patches</h1>

<p align="center">
  Modular bytecode, resource, and native patch suite for <b>Brave Browser</b>, <b>Vivaldi Browser</b>, <b>Gboard Lite</b>, <b>Hevy</b>, and <b>TikTok</b> on Android using the <b><a href="https://morphe.software">Morphe</a></b> patcher framework.
</p>

<p align="center">
  <a href="https://morphe.software/add-source?github=kveld9/kveld-morphe-patches"><img src="https://img.shields.io/badge/Morphe_Manager-Add_Patch_Source-8A2BE2?style=for-the-badge&logo=android&logoColor=white" alt="Add Source to Morphe Manager" /></a>
  &nbsp;&nbsp;
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases/latest"><img src="https://img.shields.io/badge/Direct_Download-Get_.MPP_Bundle-0070F3?style=for-the-badge&logo=github&logoColor=white" alt="Download Latest Release" /></a>
</p>

---

## 🎯 Supported Targets & Downloads

> [!TIP]
> For architecture compatibility details (ARM64 vs 32-bit ARMv7a) and variant selection guidelines, see the [Compatibility Guide](docs/compatibility.md).

### ⌨️ Gboard Lite
- **Current Target**: `18.2.4.969776716-lite_beta-arm64-v8a` (or `armeabi-v7a`)
- **Architecture**: `arm64-v8a` or `armeabi-v7a` (nodpi APK)
- **Package**: `com.google.android.inputmethod.latin`
- ⚠️ *Fresh install?* Read the [Gboard Offline Dictionary & Glide Typing Setup](docs/gboard-setup.md) before applying background debloat patches.

<a href="https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-2-4-969776716-release/"><img src="https://img.shields.io/badge/Download-Gboard_Lite_18.2.4.969776716_(APK_nodpi)-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="Download Gboard Lite APK" /></a>

### 🦁 Brave Browser
- **Current Target**: `1.95.101` (`Bravemonoarm64.apk`)
- **Architecture**: `arm64-v8a` (Monochrome only)
- **Package**: `com.brave.browser`

<a href="https://github.com/brave/brave-browser/releases/download/v1.95.101/Bravemonoarm64.apk"><img src="https://img.shields.io/badge/Download-Bravemonoarm64.apk_(v1.95.101)-FF4500?style=for-the-badge&logo=brave&logoColor=white" alt="Download Brave APK" /></a>

### 🔴 Vivaldi Browser
- **Current Target**: `8.2.4147.77` (APKM bundle)
- **Architecture**: `arm64-v8a`
- **Package**: `com.vivaldi.browser`
- ⚠️ *APKM Bundle Required*: You **must** download the **BUNDLE (`.apkm`)** variant from APKMirror. The standalone APK option on APKMirror is an unbundled base split lacking browser bytecode and will fail patching.

<a href="https://www.apkmirror.com/apk/vivaldi-technologies/vivaldi-browser-beta/vivaldi-browser-fast-safe-8-2-4147-77-release/"><img src="https://img.shields.io/badge/Download-Vivaldi_8.2.4147.77_(APKM)-EF3939?style=for-the-badge&logo=vivaldi&logoColor=white" alt="Download Vivaldi APKM" /></a>

### 🏋️ Hevy - Gym Log Workout Tracker
- **Current Target**: `3.1.14` (APKM bundle)
- **Architecture**: `arm64-v8a`
- **Package**: `com.hevy`
- ⚠️ *Google Sign-In Issue?* Read the [Hevy Setup & Authentication Guide](docs/hevy-setup.md) to log in with Email & Password.

<a href="https://www.apkmirror.com/apk/hevy-gym-workout-tracker/hevy-gym-log-workout-tracker/hevy-gym-log-workout-tracker-3-1-14-release/"><img src="https://img.shields.io/badge/Download-Hevy_3.1.14_(APKM)-FF4500?style=for-the-badge&logo=target&logoColor=white" alt="Download Hevy APKM" /></a>

### 🎵 TikTok (Global & Asia)
- **Current Target**: `46.9.3`
- **Architecture**: `arm64-v8a` (nodpi APK)
- **Package**: `com.zhiliaoapp.musically` (Global) & `com.ss.android.ugc.trill` (Asia)

<a href="https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-46-9-3-release/"><img src="https://img.shields.io/badge/Download-TikTok_Global_46.9.3_(APK_nodpi)-FE2C55?style=for-the-badge&logo=tiktok&logoColor=white" alt="Download TikTok Global APK" /></a>
&nbsp;&nbsp;
<a href="https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/tiktok-46-9-3-2-release/"><img src="https://img.shields.io/badge/Download-TikTok_Asia_46.9.3_(APK_nodpi)-25F4EE?style=for-the-badge&logo=tiktok&logoColor=white" alt="Download TikTok Asia APK" /></a>

---

## 💊 Patch Catalog

<!-- PATCHES_START -->
<details>
<summary>Gboard Lite&nbsp;&nbsp;•&nbsp;&nbsp;<b>17 patches</b></summary>
<br>

**Supported versions:**

| 18.2.4.969776716-lite_beta-arm64-v8a | 18.2.4.969776716-lite_beta-armeabi-v7a |
| :---: | :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Add AMOLED Theme** | Adds a selectable Pure Black AMOLED theme using Gboard's native color_black theme package, without replacing Dark, Light, System Auto, or Dynamic Color. |  |
| **Allow Modified APK** | Bypasses internal signature check to allow custom APK execution. |  |
| **Block Telemetry** | Disables background metrics dispatch, event logging, daily pings, and crash reporting. |  |
| **Clipboard Enhancements** | Extends unpinned clipboard history retention duration, raises the maximum number of displayed unpinned clips, and allows customizing keyboard grid columns. | • Unpinned clip limit<br>• Retention time limit (hours)<br>• Clipboard grid columns |
| **Clone Gboard** | Changes the package name by appending a dot and custom suffix (defaults to 'clone') to allow installing Gboard alongside the original application. | • Package name suffix |
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

<details>
<summary>Brave Private Web Browser, VPN&nbsp;&nbsp;•&nbsp;&nbsp;<b>9 patches</b></summary>
<br>

**Supported versions:**

| 1.95.101 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Block Brave Telemetry** | Blocks P3A product analytics, Brave Stats usage pings, crash dump uploads, WDP, and Variations seed fetching. |  |
| **Brave In-Product & Commercial Notification Optimizer** | Eliminates background wakeups and notifications from Chromium tips scheduler (Job ID 105), Brave Rewards onboarding promo, and retention marketing campaigns. |  |
| **Brave Origin** | Unlocks Brave Origin and enables local feature toggle controls. |  |
| **Brave Startup Performance Optimization** | Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations. |  |
| **Disable Background Sync & Periodic Sync** | Eliminates background wakeups, radio modem activity, and battery drain by forcing GooglePlayServicesChecker.shouldDisableBackgroundSync() -> true and neutralizing wakeup tasks. |  |
| **Disable Battery Status API & OS Listener** | Neutralizes the Battery Status API (navigator.getBattery) to prevent cross-site device fingerprinting and drops OS battery change broadcasts. |  |
| **Locale PAK Slimmer** | Strips unselected language resource PAKs from assets/locales/. | • Locales to keep |
| **Native Bloat Slimmer** | Strips unused native companion binaries (Impress Vision AI, WireGuard VPN, and Android XR) to significantly reduce APK size. |  |
| **Skip First Run** | Skips the welcome screen, search engine selection, and onboarding First Run Experience (FRE) on clean installs. |  |

</details>

<details>
<summary>Hevy - Gym Log Workout Tracker&nbsp;&nbsp;•&nbsp;&nbsp;<b>4 patches</b></summary>
<br>

**Supported versions:**

| 3.1.14 | 3.1.13 |
| :---: | :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Block Hevy Telemetry & Trackers** | Neutralizes Sentry crash reporting, Adjust attribution, Facebook AppEvents, Branch referral tracking, and WearOS background sync. |  |
| **Hevy Battery Optimization & Background Sync Killer** | Disables background WorkManager alarms, periodic job schedulers, Google Play Billing IPC (~89 MB RAM), and removes largeHeap to force aggressive Garbage Collection. |  |
| **Hevy Resource Slimmer** | Strips embedded onboarding MP4 tutorial video, heavy IMG.LY photo editor stickers/textures, and compiler junk metadata. |  |
| **Unlock Pro** | Unlocks local Hevy Pro capabilities (unlimited workout routines, routine folders, advanced graphs, and local analytics) by dynamically enabling Pro getters in Hermes Bytecode (HBC96). |  |

</details>

<details>
<summary>Vivaldi Browser - Fast & Safe&nbsp;&nbsp;•&nbsp;&nbsp;<b>12 patches</b></summary>
<br>

**Supported versions:**

| 8.2.4147.77 |
| :---: |

| Patch | Description | Options |
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

<details>
<summary>TikTok&nbsp;&nbsp;•&nbsp;&nbsp;<b>17 patches</b></summary>
<br>

**Supported versions:**

| 46.9.3 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Clean Share URL** | Strips tracking parameters, user IDs, device fingerprints, and marketing tokens from shared TikTok links. |  |
| **Core Asset De-bloat** | Strips embedded Microblink credit card OCR models, C2PA AI content origin verification libraries, and redundant non-Latin fonts to save APK space. |  |
| **Device Privacy Guard** | Neutralizes background clipboard snooping routines and blocks screenshot/recording detection listeners to protect user data from unauthorized tracking and popup modals. |  |
| **Disable Push Notifications** | Neutralizes background push notification tasks and persistent socket wake locks to eliminate background battery drain. |  |
| **Feed Ad Blocker** | Removes sponsored advertisements, brand promotions, and promotional audio from the For You and Following feeds. |  |
| **Feed Live Stream Blocker** | Removes live stream broadcast cards and live recommendations from the For You and Following feeds. |  |
| **Hide TikTok Shop Anchors** | Removes product showcase badges, shopping cart tags, and TikTok Shop commercial anchors from video posts in the feed. |  |
| **Instant Launch & Splash Blocker** | Eliminates cold startup delays, real-time splash advertisements, and background TopView preloading (<0.4s launch). |  |
| **Language Pack Purger** | Strips unselected language string bundles from assets/strings#lang_* to save APK space. | • Languages to keep |
| **Live Stream 3D Gift Optimizer** | Disables Live 3D gift particle effect engine and widget rendering lifecycle to eliminate frame drops during live streams. |  |
| **Live Stream SDK & Minigame De-bloat** | Strips Live link mic SDK (liblink_mic_sdk.so) and live stream interactive minigames to save APK storage. |  |
| **Media Usability & Watermark-Free Downloader** | Enables progress seekbar scrubbing on all videos, unblocks the download button on creator-restricted videos, and routes downloads to clean unwatermarked media streams. |  |
| **Playback Speed Persistence** | Persists selected video playback speed across all feed videos and application restarts. |  |
| **Resource & Battery Governor** | Throttles background sensor polling (gyroscope/accelerometer 3D ads), prevents aggressive video buffer preloading, and caps Fresco animated image memory retention to conserve battery and RAM. |  |
| **Studio & Creation De-bloat** | Strips heavy video creation, CapCut-like video editor SDK (libttvesdk_plugin.so), and AR camera face models to save over 22MB. |  |
| **Unified Telemetry & Tracker Silencer** | Neutralizes ByteDance AppLog user tracking, APM/Npth/Heimdallr crash monitors, AppsFlyer attribution, and Firebase analytics. |  |
| **Update Prompt Suppressor** | Neutralizes background update polling tasks and device ID check routines to prevent forced update popups. |  |

</details>

<details>
<summary>Universal&nbsp;&nbsp;•&nbsp;&nbsp;<b>4 patches</b></summary>
<br>

| Patch | Description | Options |
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
| 🏋️ **[Hevy Setup & Authentication](docs/hevy-setup.md)** | Account authentication notice (Google Sign-In SHA-1 limitation & Email/Password login) and Pro features overview. |
| 🎵 **[TikTok Patch Specifications](docs/tiktok-patches.md)** | Deep technical breakdown of all 17 TikTok patches, bytecode hooks, privacy guards, and media downloaders. |
| ⚙️ **[Patch Configuration & Options](docs/patch-configuration.md)** | Detailed guides for `Clipboard Enhancements` (retention & layout), `Locale Slimmer`, and `DPI Resource Slimmer`. |
| 🛡️ **[Architecture & Security Notes](docs/architecture-security.md)** | Explanation of privacy scanner false positives (ML Kit / Play Billing) and native ELF/Dalvik telemetry neutralization. |
| 🛠️ **[Building & Development](docs/building.md)** | Toolchain prerequisites, Gradle build tasks, test execution, and catalog synchronization. |

---

## 🤝 Credits & Contributors

| Contributor | Role & Contributions |
| :--- | :--- |
| <a href="https://github.com/Lxchoooo"><img src="https://github.com/Lxchoooo.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@Lxchoooo</b></a> | 🧪 Daily patch testing, runtime APK validation, and bug diagnostics. |
| <a href="https://github.com/ll0r3nt3"><img src="https://github.com/ll0r3nt3.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@ll0r3nt3</b></a> | 💡 Proposed DPI Resource Slimmer feature request ([#16](https://github.com/kveld9/kveld-morphe-patches/issues/16)). |
| <a href="https://github.com/aidenking2102-dotcom"><img src="https://github.com/aidenking2102-dotcom.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@aidenking2102-dotcom</b></a> | 💡 Proposed TikTok Feed Ad Blocker ([#23](https://github.com/kveld9/kveld-morphe-patches/issues/23)) and Playback Speed Setter ([#25](https://github.com/kveld9/kveld-morphe-patches/issues/25)) feature requests. |
| <a href="https://github.com/raxelbyte"><img src="https://github.com/raxelbyte.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@raxelbyte</b></a> | 💡 Proposed Gboard Lite Clipboard Enhancements feature request ([#17](https://github.com/kveld9/kveld-morphe-patches/issues/17)). |

---

## 📜 License

Morphe Patches is open-source software licensed under the [GNU General Public License v3.0](LICENSE).
