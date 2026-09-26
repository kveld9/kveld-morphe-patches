# 🦁 Brave Browser: Complete Patch & Configuration Guide

Comprehensive technical and configuration guide for **Brave Browser** (`com.brave.browser`), covering target requirements, applied patches, configuration options, and architectural invariants.

---

## 🎯 Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target Application** | Brave Private Web Browser, VPN |
| **Package Name** | `com.brave.browser` |
| **Supported Target Version** | **`1.96.59`** |
| **Target File Format** | Standalone APK (`APK`) |
| **Recommended Architecture** | `arm64-v8a` (or `armeabi-v7a` for 32-bit devices) |
| **Official Download Source** | [GitHub: brave/brave-browser/releases](https://github.com/brave/brave-browser/releases/tag/v1.96.59) |
| **Recommended APK Assets** | `Bravemonoarm64.apk` (64-bit) or `BraveMonoarm.apk` (32-bit) |

> [!IMPORTANT]
> Always download the official standalone `Bravemonoarm64.apk` or `BraveMonoarm.apk` release builds directly from Brave's GitHub repository. Avoid multi-architecture split bundles or unofficial third-party repacks.

---

## 📋 Applied Patches Catalog

| Patch Name | Type | Category | Default | Primary Mechanism |
| :--- | :--- | :--- | :---: | :--- |
| **Brave Origin** | `bytecodePatch` + `resourcePatch` | Feature Unlock & UI | ✅ Yes | Unlocks Brave Origin preference screens and injects toggle switches for Origin policy gates. |
| **Block Brave Telemetry** | `bytecodePatch` + `rawResourcePatch` | Privacy & Telemetry | ✅ Yes | Intercepts `PrefService.e` (P3A, stats, WDP), aborts variations seed HTTP connection, and redirects 12 native host endpoints to `0.0.0.0` in `libchrome.so`. |
| **Clean New Tab Page** | `bytecodePatch` + `resourcePatch` | Debloat & UX | ✅ Yes | Neutralizes sponsored wallpaper loading via `PrefService.b` and `PrefService.e`, suppresses the Brave Shields stats card, and offers an opt-in toggle to hide top sites shortcuts. |
| **Suppress In-App Promos & Surveys** | `bytecodePatch` | Debloat & UX | ✅ Yes | Forces `RateEligibilityGate.d -> false`, neutralizes rating survey bottom sheets (`BraveRateDialogFragment`), drops promotional dialogs (YouTube, ad-free callouts), and disables Brave Ads onboarding. |
| **Brave In-Product & Commercial Notification Optimizer** | `bytecodePatch` | Debloat & Battery | ✅ Yes | Eliminates background wakeups and notifications from Chromium tips scheduler (Job ID 105), Brave Rewards onboarding promo, and retention marketing campaigns. |
| **Clean Share URL** | `bytecodePatch` | Privacy & Anti-Tracking | ✅ Yes | Hooks Android share intent builder (`Lcch.a`) and clipboard copy (`Clipboard.setText`) to purge tracking parameters (`utm_*`, `fbclid`, `gclid`, etc.). |
| **Disable Background Sync & Periodic Sync** | `bytecodePatch` | Battery & Performance | ✅ Yes | Eliminates background wakeups, radio modem activity, and battery drain by forcing `GooglePlayServicesChecker.shouldDisableBackgroundSync() -> true` and neutralizing sync tasks. |
| **Disable Battery Status API & OS Listener** | `bytecodePatch` | Privacy & Anti-Fingerprinting | ✅ Yes | Neutralizes the Battery Status API (`navigator.getBattery`) to prevent cross-site device fingerprinting and drops OS `BATTERY_CHANGED` broadcast events. |
| **Brave Startup Performance Optimization** | `bytecodePatch` | Performance & Startup | ✅ Yes | Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations (`PartnerBrowserCustomizations`). |
| **Native Bloat Slimmer** | `rawResourcePatch` | Storage Reclamation | ✅ Yes | Strips 6 unused native companion binaries (Impress Vision AI, WireGuard VPN, and Android XR / ARCore) to reclaim **~22.35 MB** of APK space. |
| **Locale PAK Slimmer** | `rawResourcePatch` | Storage Reclamation | ✅ Yes | Strips unselected language PAKs from `assets/locales/` (~9.64 MB saved) using zero-crash binary fallback substitution. |
| **Sensor Privacy Guard** | `bytecodePatch` | Privacy & Anti-Fingerprinting | ✅ Yes | Forces `PlatformSensorProvider.hasSensorType -> false` and `PlatformSensor.create -> null`. Neutralizes W3C Generic Sensor APIs. |
| **Skip First Run** | `bytecodePatch` | Usability & UX | ✅ Yes | Skips the welcome screen, search engine selection, and onboarding First Run Experience (FRE) on clean installs. |
| **Universal Patches Suite** | Multiple | Optimization & Privacy | Contextual | Compatible with universal slimmers and privacy patches (Telemetry Neutralizer, Native Binary Trimmer, WebP/PNG Optimizers, DPI/Locale Slimmers). See [Universal Patch Reference](../universal-patches.md). |

---

## 🔒 Deep Technical Patch Breakdown

### 1. Brave Origin (`braveOriginPatch`)
- **Objective**: Unlock Brave Origin feature toggles and preferences without requiring remote enterprise management profiles.
- **Mechanisms**:
  - Injects Origin preference switch items into settings layout XML (`xml_0x7f18001a.xml`).
  - Hooks `BraveOriginPreferences` methods (`k5`, `T3`, `j5`, `X4`) to persist toggles locally in `SharedPreferences`.
  - Stubs subscription check methods (`getIsSubscriptionActive`, `requestCredentialSummary`) to return active credentials.

### 2. Block Brave Telemetry (`braveBlockTelemetryPatch`)
- **Objective**: Halt outbound telemetry pings, usage metrics, and variations seed fetching.
- **Mechanisms**:
  - **Bytecode Neutralization**: Intercepts `PrefService.e` queries for P3A (*Privacy-Preserving Product Analytics*), Brave Stats, and WDP (*Web Discovery Project*).
  - **Variations Connection Abort**: Injects early returns into HTTP loaders fetching experimentation variations seeds.
  - **Native Socket Redirection**: In `libchrome.so`, redirects 12 native telemetry endpoints (`*.bsg.brave.com`, `*.wdp.brave.com`, `usage-ping.brave.com`, `crashpad.chromium.org`, `variations.brave.com`) to `0.0.0.0`.

### 3. Clean New Tab Page (`braveCleanNewTabPagePatch`)
- **Objective**: Completely eliminate sponsored advertising wallpapers, background campaign asset downloads, Brave News/Today promotional cards, and Brave Shields stats cards.
- **Mechanisms**:
  - **Preference Defaults**: Overrides NTP preference keys in XML (`show_background_images`, `show_sponsored_images`, `brave_news_switch`, `show_brave_stats`, etc.) to ensure marketing features are disabled out-of-the-box.
  - **Runtime Gate Interception**: Intercepts both `PrefService.b` and `PrefService.e` calls in `BraveExtension.filterNtpPref` to reliably suppress sponsored background image fetches.
  - **Stats Card Neutralization**: Injects Smali opcodes (`const/4 v$reg, 0x0`) prior to `iput-boolean` in `BraveNewTabPageLayout` initialization and preference change listeners, permanently hiding the Brave Shields statistics card.
  - **Top Sites / Shortcuts Opt-In**: Offers the configurable `hideTopSites` option to remove most visited tiles and shortcuts for a completely blank home screen.
- **Impact**: Saves ~15–30 MB/month of cellular background wallpaper downloads and produces an instant, pristine New Tab Page.

### 4. Suppress In-App Promos & Surveys (`braveSuppressInAppPromosPatch`)
- **Objective**: Eliminate intrusive in-app rating survey bottom sheets, Play Store review prompts, and unsolicited marketing promo popups while browsing.
- **Mechanisms**:
  - **Rating Eligibility Neutralization**: Forces `RateEligibilityGate.d(Context) -> false`, halting rating dialog triggers in `ChromeTabbedActivity` startup.
  - **Survey Bottom Sheet Suppressor**: Intercepts `BraveRateDialogFragment`, `BraveAskPlayStoreRatingDialog`, and `BraveRateThanksFeedbackDialog` with immediate `return-void` so dialog fragments never attach or display.
  - **Promotional Dialog Interceptor**: Intercepts `BraveDialogFragment` to suppress `OpenYtInBraveDialogFragment` and `BraveAdFreeCalloutDialogFragment` popups.
  - **Brave Ads Onboarding Neutralization**: Stubs `BraveAdsSignupDialog` builders and dialog launchers.

### 5. Brave In-Product & Commercial Notification Optimizer (`braveNotificationOptimizerPatch`)
- **Objective**: Neutralize background notification scheduling tasks and promotional onboarding pushes.
- **Mechanisms**:
  - Stubs `NotificationSchedulerTask` and `TipsAgent` to halt Chromium tips scheduler alarms (Job ID 105).
  - Neutralizes `BraveOnboardingNotification` and `RetentionNotificationPublisher` tasks.

### 6. Clean Share URL (`cleanShareUrlPatch`)
- **Objective**: Strip invasive analytics and tracking tokens when copying or sharing links from the browser.
- **Mechanisms**:
  - Intercepts link sharing via `Lcch.a` and clipboard copy via `Clipboard.setText` and `ClipboardImpl.setPrimaryClip`.
  - Routes URLs through companion extension logic [`BraveExtension`](../../extensions/extension/src/main/java/com/kveld9/morphe/extension/BraveExtension.java).
  - Strips query parameters: `utm_*`, `fbclid`, `gclid`, `igshid`, `si`, `msclkid`, `mc_eid`, `vero_id`, etc.
  - Preserves legitimate functional parameters (`id`, `v`, `q`, `t`, `list`).

### 7. Disable Background Sync & Periodic Sync (`disableBackgroundSyncPatch`)
- **Objective**: Prevent background radio wakeups, polling cycles, and network traffic when the browser is backgrounded.
- **Mechanisms**:
  - Forces `GooglePlayServicesChecker.shouldDisableBackgroundSync() -> true`.
  - Stubs background sync task triggers in `Lcee.d()` and `Lyfc.d()`.

### 8. Disable Battery Status API & OS Listener (`disableBatteryApiPatch`)
- **Objective**: Prevent cross-site battery level tracking and eliminate battery change broadcast receiver wakeups.
- **Mechanisms**:
  - Hooks `onReceive` in `Lwi1` to drop `ACTION_BATTERY_CHANGED` broadcast intents.
  - Prevents fingerprinters from querying battery charging status and remaining percentage.

### 9. Brave Startup Performance Optimization (`bravePerformanceOptimizationPatch`)
- **Objective**: Accelerate cold launch times and reduce memory allocation on startup.
- **Mechanisms**:
  - Neutralizes asynchronous partner carrier initialization in `PartnerBrowserCustomizations`.

### 10. Native Bloat Slimmer (`nativeBloatSlimmerPatch`)
- **Objective**: Strip unneeded bundled native libraries to reduce application size on disk and in memory.
- **Mechanisms**:
  - Zeroes out 6 non-essential native libraries:
    - `lib/arm64-v8a/libimpress_api_jni.so` (Vision AI)
    - `lib/arm64-v8a/libwg-go.so` (WireGuard VPN)
    - `lib/arm64-v8a/libandroidx.xr.arcore.openxr.so`
    - `lib/arm64-v8a/libandroidx.xr.runtime.openxr.so`
    - `lib/arm64-v8a/libarcore_sdk_c.so`
    - `lib/arm64-v8a/libarcore_sdk_jni.so`
  - Reclaims **~22.35 MB** of storage without impacting standard web browsing.

### 11. Locale PAK Slimmer (`localePakSlimmerPatch`)
- **Objective**: Strip unselected language binary PAK archives from `assets/locales/` to reclaim **~9.64 MB** of APK space.
- **Mechanisms**: Replaces stripped `.pak` files with the binary resource table of `en-US.pak` to satisfy Chromium's native C++ `ui::ResourceBundle` loader without triggering fatal assertion aborts.

### 12. Sensor Privacy Guard (`sensorPrivacyPatch`)
- **Objective**: Prevent side-channel hardware fingerprinting, battery drain, and acoustic keystroke sniffing via web sensor APIs.
- **Mechanisms**:
  - Forces `PlatformSensorProvider.hasSensorType(int)` to return `false`.
  - Forces `PlatformSensor.create(...)` to return `null`.
  - Conforms to W3C specifications so web applications receive standard "unsupported" states gracefully rather than crashing.

### 13. Skip First Run (`skipFirstRunPatch`)
- **Objective**: Bypass the first-run experience wizard, search engine selector, and onboarding prompts on fresh installations.
- **Mechanisms**:
  - Forces completion in `Lem7` and bypasses onboarding sequencer in `Lam7`.

---

## ⚙️ Configurable Options in Morphe Manager

### 1. Clean New Tab Page: Hide Top Sites & Shortcuts
- **Option Key**: `hideTopSites`
- **Default**: `false`
- **Description**: When enabled (opt-in), removes top sites shortcuts, most visited tiles, and the add shortcut button (`+`) from the New Tab Page, rendering a completely blank, distraction-free home screen.

### 2. Locale PAK Slimmer: Locales to Keep
- **Option Key**: `locales`
- **Default**: `en-US`
- **Accepted Format**: Comma-separated list of locale codes (e.g. `es, es-419, pt-BR, fr, de`).
- **Notes**: English (`en-US`) is always preserved automatically as an essential Chromium fallback. For the full list of 81 supported locale codes, consult the [Universal Patches Guide](../universal-patches.md#1-locale-pak-slimmer-localepakslimmerpatch).

---

## 📐 Architectural Rationale: Omission of Additional Chromium Patches in Brave

The following patches present in other Chromium forks are intentionally omitted from Brave Browser due to upstream differences:

1. **Google Privacy Sandbox Attestations Zeroing (`privacy-sandbox-attestations.dat`)**:
   - Brave removes and disables Google Privacy Sandbox APIs (Topics API, FLEDGE / Protected Audience, Attribution Reporting) at the C++ level in `brave-core`. The underlying APIs are completely uncallable by web content, making zeroing the `.dat` file redundant.
2. **UKM (URL-Keyed Metrics) Recorder Neutralization (`UkmRecorder.c()V`)**:
   - Brave completely removes upstream Google UMA and UKM pipelines in C++ and replaces them with P3A and WDP, which are already fully neutralized by `Block Brave Telemetry`.
3. **Close Tabs on Exit (`TabStateFileManager` Hook)**:
   - Brave already provides a native user preference in *Settings -> Close tabs on exit*. Enforcing tab state discard via Dalvik bytecode hooks would override user choice and break intentional session retention.
