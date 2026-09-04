<p align="center">
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases/latest"><img src="https://img.shields.io/github/v/release/kveld9/kveld-morphe-patches?color=7928CA&label=Release&logo=github&style=for-the-badge" alt="Latest Release" /></a>
  <a href="https://github.com/kveld9/kveld-morphe-patches/actions/workflows/release.yml"><img src="https://img.shields.io/github/actions/workflow/status/kveld9/kveld-morphe-patches/release.yml?branch=main&label=CI&logo=githubactions&logoColor=white&style=for-the-badge" alt="Build Status" /></a>
  <img src="https://img.shields.io/badge/Runtime-Morphe_Patcher_1.8.0-8A2BE2?style=for-the-badge" alt="Runtime" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge" alt="License" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform Android" />
  <img src="https://img.shields.io/badge/Target-Brave_Browser-FF4500?style=for-the-badge&logo=brave&logoColor=white" alt="Target Brave" />
  <img src="https://img.shields.io/badge/Target-Vivaldi_Snapshot-EF3939?style=for-the-badge&logo=vivaldi&logoColor=white" alt="Target Vivaldi" />
  <img src="https://img.shields.io/badge/Target-Gboard_Lite-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="Target Gboard" />
</p>

<h1 align="center">🔮 Morphe Patches</h1>

<p align="center">
  Modular bytecode, resource, and native patch suite for <b>Brave Browser</b>, <b>Vivaldi Browser Snapshot</b>, and <b>Gboard Lite</b> on Android using the <b><a href="https://morphe.software">Morphe</a></b> patcher framework.
</p>

<p align="center">
  <a href="https://morphe.software/add-source?github=kveld9/kveld-morphe-patches"><img src="https://img.shields.io/badge/Morphe_Manager-Add_Patch_Source-8A2BE2?style=for-the-badge&logo=android&logoColor=white" alt="Add Source to Morphe Manager" /></a>
  &nbsp;&nbsp;
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases/latest"><img src="https://img.shields.io/badge/Direct_Download-Get_.MPP_Bundle-0070F3?style=for-the-badge&logo=github&logoColor=white" alt="Download Latest Release" /></a>
</p>

---

## 📖 Overview

**Morphe Patches** is an open-source, modular patch repository for Android applications running on the **[Morphe](https://morphe.software)** patcher runtime.

It enhances target applications with native Pure Black AMOLED themes, comprehensive telemetry and diagnostic blocking, background task debloating, cold-start performance optimizations, and APK footprint reduction across **Gboard Lite**, **Brave Browser**, and **Vivaldi Browser Snapshot**.

---

## 🎯 Supported Targets

| Target App | Target Version | Channel | Architecture | Package Name | Required Variant / Source |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Gboard Lite** | `18.1.3.962075747` | Lite Beta | `arm64-v8a` (`nodpi`) | `com.google.android.inputmethod.latin` | Standalone APK ([APKMirror](https://www.apkmirror.com/apk/google-inc/gboard/)) |
| **Brave Browser** | `1.94.119` | Release | `arm64-v8a` (Monochrome) | `com.brave.browser` | `Bravemonoarm64.apk` ([GitHub Releases](https://github.com/brave/brave-browser/releases)) |
| **Vivaldi Browser** | `8.2.4147.28` | Snapshot | `arm64-v8a` | `com.vivaldi.browser.snapshot` | Official APK ([Vivaldi Blog](https://vivaldi.com/blog/android/)) |

---

## 🚀 Quick Start

1. **Install Morphe Manager:** Download and install the latest **[Morphe Manager](https://morphe.software)** on your Android device.
2. **Add Patch Source:** Tap the badge above or navigate to *Settings > Sources* in Morphe Manager and add:
   ```text
   kveld9/kveld-morphe-patches
   ```
   *(Or click: **[Add Source to Morphe Manager](https://morphe.software/add-source?github=kveld9/kveld-morphe-patches)**)*
3. **Download Supported APK:** Obtain the exact target APK variant from the links in [Supported Targets](#supported-targets).
4. **Select Patches:** Pick the patches you want to apply (privacy hardening, AMOLED theme, slimming, etc.).
5. **Patch & Install:** Tap **Patch**, wait for compilation to complete, and install your enhanced APK.

---

## 💊 Patch Catalog

<!-- PATCHES_START EXPANDED -->
<details open>
<summary>📦 Gboard Lite&nbsp;&nbsp;•&nbsp;&nbsp;18 patches</summary>
<br>

**🎯 Supported versions:**

| 18.1.3.962075747-lite_beta-arm64-v8a |
| :---: |

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
| **Locale Resource Slimmer** | Strips unselected language translation directories from res/ (e.g. values-*, raw-*, xml-*). Base fallback resources with no language qualifiers are always preserved. | • Locales to keep |
| **PNG Asset Optimizer** | Losslessly recompresses PNG assets with maximum zlib compression and strips non-rendering metadata chunks (pHYs, tEXt, tIME) while preserving 9-patch structures and pixel accuracy. |  |
| **Resource Slimmer** | Strips embedded third-party license text, onboarding tutorial Lottie animations, promotional GIFs, and APK root metadata/junk files. |  |

</details>

<details open>
<summary>📦 Brave Private Web Browser, VPN&nbsp;&nbsp;•&nbsp;&nbsp;11 patches</summary>
<br>

**🎯 Supported versions:**

| 1.94.119 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| **Block Brave Telemetry** | Blocks P3A product analytics, Brave Stats usage pings, crash dump uploads, WDP, and Variations seed fetching. |  |
| **Brave In-Product & Commercial Notification Optimizer** | Eliminates background wakeups and notifications from Chromium tips scheduler (Job ID 105), Brave Rewards onboarding promo, and retention marketing campaigns. |  |
| **Brave Origin** | Unlocks Brave Origin and enables local feature toggle controls. |  |
| **Brave Startup Performance Optimization** | Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations. |  |
| **Disable Background Sync & Periodic Sync** | Eliminates background wakeups, radio modem activity, and battery drain by forcing GooglePlayServicesChecker.shouldDisableBackgroundSync() -> true and neutralizing wakeup tasks. |  |
| **Disable Battery Status API & OS Listener** | Neutralizes the Android BatteryStatusManager broadcast listener to prevent continuous OS battery wakeups. |  |
| **Disable Pull To Refresh** | Completely disables the pull-to-refresh overscroll gesture and animation to prevent accidental page reloads. |  |
| **Locale PAK Slimmer** | Strips unselected language resource PAKs from assets/locales/. | • Locales to keep |
| **Native Bloat Slimmer** | Strips unused native companion binaries (Impress Vision AI, WireGuard VPN, and Android XR) to significantly reduce APK size. |  |
| **PNG Asset Optimizer** | Losslessly recompresses PNG assets with maximum zlib compression and strips non-rendering metadata chunks (pHYs, tEXt, tIME) while preserving 9-patch structures and pixel accuracy. |  |
| **Skip First Run** | Skips the welcome screen, search engine selection, and onboarding First Run Experience (FRE) on clean installs. |  |

</details>

<details open>
<summary>📦 Vivaldi Browser Snapshot&nbsp;&nbsp;•&nbsp;&nbsp;13 patches</summary>
<br>

**🎯 Supported versions:**

| 8.2.4147.28 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| **Block Vivaldi Sync** | Redirects bifrost.vivaldi.com to 0.0.0.0 in libchrome.so to completely disable Vivaldi account synchronization for an air-gapped experience. |  |
| **Block Vivaldi Telemetry** | Blocks Vivaldi usage statistics, crash reporting, Crashpad endpoints, and DirectMatch suggestions. |  |
| **Clean Speed Dial Bookmarks** | Removes sponsored commercial affiliate bookmarks, hides the 3-dot customize button, and disables phantom touch actions on the blank Start Page. |  |
| **Close Tabs on Exit** | Prevents tab restoration on startup, ensuring Vivaldi always opens with a clean Start Page session regardless of how the app was terminated. |  |
| **Disable Background Sync & Periodic Sync** | Eliminates background wakeups, radio modem activity, and battery drain by neutralizing periodic and one-shot background sync tasks. |  |
| **Disable Battery Status API & OS Listener** | Neutralizes the Android BatteryStatusManager broadcast listener to prevent continuous OS battery wakeups. |  |
| **Disable Chromium Tips & Notification Scheduler** | Eliminates periodic background wakeups, native library loading, and in-product promotional tips from Chromium tips scheduler. |  |
| **Disable Vivaldi Prompts & In-App Popups** | Disables in-app donation prompts, search engine switch promos, rate-app popups, background privacy report alarms, and default browser promotional handlers. |  |
| **Locale PAK Slimmer** | Strips unselected language resource PAKs from assets/locales/. | • Locales to keep |
| **PNG Asset Optimizer** | Losslessly recompresses PNG assets with maximum zlib compression and strips non-rendering metadata chunks (pHYs, tEXt, tIME) while preserving 9-patch structures and pixel accuracy. |  |
| **Resource Slimmer** | Strips bundled stock wallpapers and sponsored Speed Dial thumbnails to reduce APK size. |  |
| **Skip First Run** | Skips the welcome screen, search engine selection, and onboarding First Run Experience (FRE) on clean installs. |  |
| **Vivaldi Startup Performance Optimization** | Optimizes cold startup time, eliminates background CPU/disk overhead, and guards against early CompositorView LayoutStateObserver crashes. |  |

</details>

<!-- PATCHES_END -->

---

## ⚠️ Compatibility & Operational Notes

### ⌨️ Gboard Lite: Target Variant & Offline Dictionary Setup

> [!IMPORTANT]
> **Always download the standalone `lite` / `lite_beta` 64-bit APK (nodpi) from [APKMirror](https://www.apkmirror.com/apk/google-inc/gboard/):**
>
> - **Current Target**: `18.1.3.962075747-lite_beta-arm64-v8a`
> - **Format**: `APK` _(Do **NOT** download `BUNDLE` / split packages)_
> - **Architecture**: `arm64-v8a`
> - **Screen DPI**: `nodpi`

#### 🔧 Predictive Text & Glide Typing on Fresh Installations

> [!IMPORTANT]
> **Gboard Lite does not bundle language dictionaries, predictive text models, or gesture/glide typing decoding models inside the APK.**
> Unlike the full Gboard APK, Gboard Lite downloads language models on-demand upon first launch via Google's **MDD (Mobile Data Download)** and **Superpacks** subsystems.

If you perform a clean install of Gboard Lite with background sync debloat patches enabled, Gboard will be prevented from downloading the initial dictionary and gesture model pack for your language. This results in an empty suggestion bar, no predictive text, and **Glide Typing (swipe to type) not working**.

##### How to set up Gboard Lite with working predictive text & glide typing:

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

### 🦁 Brave Browser: Required Variant & Native Offsets

> [!IMPORTANT]
> **Always use `Bravemonoarm64.apk` from [Brave GitHub Releases](https://github.com/brave/brave-browser/releases).**
> Do **NOT** use `Bravearm64Universal.apk`, `Bravearm64.apk`, or 32-bit / x86 builds (`Bravearm.apk`, `Bravemonox64.apk`, etc.).
>
> - **Current Target**: `1.94.119` (`Bravemonoarm64.apk`)
> - **Architecture**: `arm64-v8a` (Monochrome)
> - **Package Name**: `com.brave.browser`
>
> <a href="https://github.com/brave/brave-browser/releases/download/v1.94.119/Bravemonoarm64.apk"><img src="https://img.shields.io/badge/Download-Bravemonoarm64.apk_(v1.94.119)-FF4500?style=for-the-badge&logo=brave&logoColor=white" alt="Download Brave APK" /></a>

#### ❓ Why `Bravemonoarm64.apk`?

- **Bytecode Consistency**: `Bravemonoarm64.apk` is the official 64-bit ARM Monochrome build. Other variants (especially _Universal_) use different R8/ProGuard obfuscation passes and aggressive method inlining, which causes Dalvik fingerprint mismatches (e.g. in Variations seed fetching and Brave Origin helpers).
- **Native ARM64 Hooks**: Byte-level offsets in `lib/arm64-v8a/libchrome.so` (such as domain redirection and native scheduler gates) are calculated strictly against ARM64 Monochrome binaries.

---

### 🔴 Vivaldi Browser Snapshot: Target APK & Architecture

> [!IMPORTANT]
> **Always download the official `arm64-v8a` APK directly from the [Vivaldi Android Blog](https://vivaldi.com/blog/android/).**
> In every release announcement blog post, Vivaldi provides direct official APK download links at the bottom.
>
> - **Current Target**: `Vivaldi.8.2.4147.28_arm64-v8a.apk`
> - **Architecture**: `arm64-v8a` (Do **NOT** use `armeabi-v7a`, `x86`, or `x86_64`)
> - **Package Name**: `com.vivaldi.browser.snapshot`
>
> <a href="https://downloads.vivaldi.com/snapshot/Vivaldi.8.2.4147.28_arm64-v8a.apk"><img src="https://img.shields.io/badge/Download-Vivaldi.8.2.4147.28_arm64--v8a.apk-EF3939?style=for-the-badge&logo=vivaldi&logoColor=white" alt="Download Vivaldi APK" /></a>

#### ❓ Why Vivaldi Snapshot & Future Transition to Stable

- **Extension Support & Experimental Features**: Morphe Patches currently targets **Vivaldi Snapshot** to provide native Desktop Extension support and recent Chromium components.
- **Stable Transition Roadmap**: Vivaldi Snapshot is maintained as the primary target **only until Web Extensions support is enabled by default in the stable release** of Vivaldi Browser (`com.vivaldi.browser`), at which point patch compatibility will expand/transition to the stable channel.
- **Native 64-bit Binary Offsets**: Byte-level offsets in `lib/arm64-v8a/libchrome.so` (telemetry endpoints, Crashpad, and DirectMatch suggestions) are strictly calculated against 64-bit ARM builds.

---

## 🔬 Technical Notes & Architecture

### 🛡️ Brave Browser: Privacy Scanner False Positives (App Manager / Exodus)

> [!NOTE]
> **Component scanners such as App Manager or Exodus Privacy may flag Google Play Billing and Google ML Kit components as "trackers" inside Brave. These are false positives caused by generic package name signatures.**

When inspecting Brave with package analysis tools, the following components may be highlighted:

| Component | Origin / Library | Actual Function | Privacy & Telemetry Impact |
| :--- | :--- | :--- | :--- |
| `com.android.billingclient.api.ProxyBillingActivity`<br>`com.android.billingclient.api.ProxyBillingActivityV2` | **Google Play Billing** (`billingclient`) | Handles user-initiated in-app subscriptions (Brave Leo AI Premium, Brave VPN). | **None (Transactional only).** These are trampoline UI activities for Google Play checkout sheets. They collect zero browsing analytics or telemetry. Stripping them breaks subscription handling and triggers `ActivityNotFoundException`. |
| `com.google.mlkit.common.internal.MlKitInitProvider`<br>`com.google.mlkit.common.internal.MlKitComponentDiscoveryService` | **Google ML Kit** (`mlkit.common`) | Powers local, on-device OCR and vision features (e.g. camera-based credit card scanning in Autofill and QR code scanning). | **None (On-device execution).** Flagged by Exodus because ML Kit uses the Firebase component dependency injector (`CommonComponentRegistrar`). All model operations run strictly local to the device. |

#### 🔒 Genuine Telemetry Neutralization

Genuine Brave telemetry is fully neutralized by the **[Block Brave Telemetry](#patch-catalog)** patch:
- **P3A (Privacy-Preserving Product Analytics)**: Preference getters forced to return `false` in Dalvik bytecode (`PrefService.e`).
- **Brave Stats & Web Discovery Project (WDP)**: Reporting loops disabled and all 7 telemetry endpoints redirected to `0.0.0.0` in `libchrome.so`.
- **Crashpad & Minidump Uploads**: Upload hooks aborted before dispatch (`MinidumpUploadServiceImpl`, `ChromeMinidumpUploadJobService`) and endpoints zeroed in native binary.
- **Variations Seed Fetching**: Blocked before HTTP socket creation (`IOException("Blocked by Morphe")`).

---

### 🌐 Patch Documentation: Locale PAK Slimmer (Brave & Vivaldi)

The **`Locale PAK Slimmer`** patch allows you to strip unneeded language resource PAKs from `assets/locales/` to reduce APK size (saving **~28 MB in Brave** and **~52 MB in Vivaldi**).

When configuring the **`Locales to keep`** option in Morphe Manager, specify a comma-separated list of locale codes (e.g. `es-419, es, en-US, pt-BR`). English (`en-US`) is always preserved automatically as an essential Chromium fallback. In Vivaldi, any corresponding grammatical gender variants (e.g. `es-419_FEMININE`) for your chosen languages are preserved automatically.

#### 📋 Popular Language Codes:

| Language                         | Locale Code(s)                                                                    |
| :--- | :--- |
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
<summary><b>🔍 View all 81 available base locale codes in Brave & Vivaldi</b></summary>
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

## 🤝 Credits & Contributors

A huge thanks to the contributors and testers who help improve, validate, and maintain Morphe Patches:

| Contributor | Role & Contributions |
| :--- | :--- |
| <a href="https://github.com/Lxchoooo"><img src="https://github.com/Lxchoooo.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@Lxchoooo</b></a> | 🧪 Daily patch testing, runtime APK validation, and bug diagnostics. |

---

## 📜 License

Morphe Patches is open-source software licensed under the [GNU General Public License v3.0](LICENSE).
