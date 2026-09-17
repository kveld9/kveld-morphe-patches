# 🔴 Vivaldi Browser: Technical Patch Specifications & Architecture

Comprehensive technical breakdown of all **14 patches** included in the Morphe Vivaldi patch suite pinned to stable target version **`8.2.4147.77`** (`com.vivaldi.browser` ARM64 APKM bundle).

---

## 📋 Overview of Vivaldi Patches

| Category | Patch Name | Type | Primary Mechanism & Target |
| :--- | :--- | :--- | :--- |
| **Privacy & Telemetry** | **[Block Vivaldi Telemetry](#1-block-vivaldi-telemetry-vivaldiblocktelemetrypatch)** | Composite (`bytecode` + `rawResource` + `resource`) | Redirects 10 native telemetry/geolocation endpoints to `0.0.0.0`, neutralizes UKM event dispatch, strips DataTransport schedulers |
| **Privacy & Telemetry** | **[Block Vivaldi Sync](#2-block-vivaldi-sync-vivaldiblocksyncpatch)** | `rawResourcePatch` | Redirects `bifrost.vivaldi.com` to `0.0.0.0` in `libchrome.so` for air-gapped operation |
| **Privacy & Anti-Tracking** | **[Clean Share URL](#3-clean-share-url-cleanshareurlpatch)** | `bytecodePatch` | Strips tracking query parameters (`utm_*`, `fbclid`, `gclid`, etc.) on share intents and clipboard copy |
| **Privacy & Anti-Tracking** | **[Sensor Privacy Guard](#4-sensor-privacy-guard-sensorprivacypatch)** | `bytecodePatch` | Neutralizes W3C Generic Sensor APIs (gyroscope, accelerometer, ambient light) to prevent hardware fingerprinting |
| **Privacy & Anti-Tracking** | **[Disable Battery Status API & OS Listener](#5-disable-battery-status-api--os-listener-batteryoptimizationpatch)** | `bytecodePatch` | Neutralizes `navigator.getBattery` and drops `BATTERY_CHANGED` broadcast events |
| **De-promotional & UX** | **[Disable Vivaldi Prompts & In-App Popups](#6-disable-prompts--in-app-popups-vivaldidisablepromptspatch)** | `bytecodePatch` | Dismisses "Rate Vivaldi" modals, blocks donation sheets, strips settings promo cards, silences privacy alarms, and hides the Vivaldia game |
| **De-promotional & UX** | **[Clean Speed Dial Bookmarks](#7-clean-speed-dial-bookmarks-vivaldicleanspeeddialpatch)** | Composite (`bytecode` + `rawResource`) | Sanitizes affiliate bookmarks JSON, hides 3-dot customize/add buttons, disables phantom touch lag on blank Start Page |
| **Usability & UX** | **[Close Tabs on Exit](#8-close-tabs-on-exit-vivaldiclosetabsonexitpatch)** | `bytecodePatch` | Neutralizes `TabStateFileManager.readTabState` to guarantee a clean Start Page session on every launch |
| **Usability & UX** | **[Skip First Run](#9-skip-first-run-skipfirstrunpatch)** | `bytecodePatch` | Skips first-run wizard, search engine picker, and terms of service onboarding |
| **Performance & Battery** | **[Vivaldi Startup Performance Optimization](#10-startup-performance-optimization-vivaldistartupperformancepatch)** | `bytecodePatch` | Bypasses OEM partner initialization, guards `CompositorView` against startup NPE crashes, and silences Automotive reflection spam |
| **Performance & Battery** | **[Disable Background Sync & Periodic Sync](#11-disable-background-sync--periodic-sync-vivaldibackgroundsyncpatch)** | `bytecodePatch` | Neutralizes periodic and one-shot BackgroundSync task wakeups in Android JobScheduler |
| **Performance & Battery** | **[Disable Chromium Tips & Notification Scheduler](#12-disable-chromium-tips--notification-scheduler-vivaldinotificationoptimizerpatch)** | `bytecodePatch` | Neutralizes background tips scheduling alarms (Job ID 105) and tips promo agents |
| **APK Slimmer** | **[Locale PAK Slimmer](#13-locale-pak-slimmer-localepakslimmerpatch)** | `rawResourcePatch` | Strips unselected language PAKs from `assets/locales/` (~21.2 MB saved) using zero-crash binary fallback substitution |
| **APK Slimmer** | **[Resource Slimmer](#14-resource-slimmer-vivaldiresourceslimmerpatch)** | `rawResourcePatch` | Empties bundled stock wallpapers, Speed Dial thumbnails, partner favicons, Privacy Sandbox attestations, and hardens defaults (~6.7 MB saved) |

---

## 🔒 Privacy & Telemetry Neutralization

### 1. Block Vivaldi Telemetry (`vivaldiBlockTelemetryPatch`)
* **Objective**: Completely eliminate outbound diagnostic reporting, geolocation polling, partner update queries, and background trackers.
* **Internal Mechanisms**:
  1. **Native `libchrome.so` Redirection with Scheme Preservation**:
     - Upstream Chromium C++ `GURL` assertions fail with `SIGTRAP` (`CHECK(url.is_valid())`) when replacing full `https://` URLs with `0.0.0.0\0` because `0.0.0.0` lacks a valid protocol scheme.
     - All full URL endpoints are deterministically redirected to `https://0.0.0.0\0`:
       - `https://location.vivaldi.com/lookup` (Network Geolocation lookup)
       - `https://update.vivaldi.com/partners/partners-current.txt` (Commercial partner exception lists)
       - `https://update.vivaldi.com/search_engines/search_engines.json` (Remote search engine definitions)
       - `https://update.vivaldi.com/search_engines/search_engines_prompt.json` (Search engine switch promo configs)
       - `https://vivaldi.com/bk/` (Affiliate partner bookmark updates)
       - `https://update.vivaldi.com/partners/sd/` (Speed Dial partner sync)
     - Hostname-only targets redirected to `0.0.0.0\0`:
       - `crashpad.chromium.org` (Crashpad minidump uploader)
  2. **Bytecode Telemetry Neutralization**:
     - **Chromium UKM (URL-Keyed Metrics)**: Neutralizes `Lorg/chromium/components/ukm/UkmRecorder;->c()V` to `return-void`, stopping URL browsing metrics from being dispatched to native handlers.
     - **Minidump Uploader**: Forces `ChromeMinidumpUploadJobService.onStartJob` to `return 0` (`false`).
     - **Install Referrer**: Neutralizes `InstallReferrerHelper.storeReferrerData` (blocking feedback pings to `feedback.vivaldi.com`).
     - **Daily Wallpaper Downloader**: Neutralizes background fetch of `image-mobile.json`.
     - **DirectMatch Suggestions**: Forces `DirectMatchBridge.a()` to return an empty `ArrayList` and stubs download completion callbacks.
     - **Privacy Preferences**: Forces `PrivacyPreferencesManagerImpl.isUsageAndCrashReportingPermitted` to return `0` (`false`).
  3. **Resource & Manifest Hardening**:
     - Disables Google Play DataTransport background schedulers (`JobInfoSchedulerService`).
     - Strips dynamic split installation attributes (`requiredSplitTypes`, `splitTypes`) from `AndroidManifest.xml` to allow seamless standalone APK operation.

### 2. Block Vivaldi Sync (`vivaldiBlockSyncPatch`)
* **Objective**: Prevent accidental account synchronization and background connection attempts to Vivaldi sync servers.
* **Internal Mechanism**:
  - Redirects `bifrost.vivaldi.com` to `0.0.0.0\0` at verified binary offsets in `lib/arm64-v8a/libchrome.so`.

### 3. Clean Share URL (`cleanShareUrlPatch`)
* **Objective**: Protect user privacy when sharing or copying links from the browser.
* **Internal Mechanism**:
  - Hooks Vivaldi share intent builder (`Latb.a`) and system clipboard copy (`Clipboard.setText`) via companion Java extension logic.
  - Strips tracking tokens (`utm_*`, `fbclid`, `gclid`, `igshid`, `si`, `msclkid`, `mc_eid`, etc.) while strictly preserving functional query parameters (`v`, `id`, `q`, `t`).

### 4. Sensor Privacy Guard (`sensorPrivacyPatch`)
* **Objective**: Prevent hardware-based device fingerprinting and side-channel keystroke acoustic sniffing via W3C Generic Sensor APIs.
* **Internal Mechanism**:
  - Forces `PlatformSensorProvider.hasSensorType(int)` -> `false`.
  - Forces `PlatformSensor.create(...)` -> `null`.
  - Preserves `PlatformSensorProvider` instance stability for C++ JNI bridge bindings.
  - Conforms to W3C specifications so web pages receive standard unavailable states without crashing.

### 5. Disable Battery Status API & OS Listener (`batteryOptimizationPatch`)
* **Objective**: Stop battery level tracking (`navigator.getBattery()`) used for cross-site visitor correlation.
* **Internal Mechanism**:
  - Injects early `return-void` into the `BATTERY_CHANGED` broadcast receiver (`onReceive`), eliminating wakeups and battery querying.

---

## 🚫 De-Promotional & Interface Cleanliness

### 6. Disable Vivaldi Prompts & In-App Popups (`vivaldiDisablePromptsPatch`)
* **Objective**: Remove monetization and marketing nagware from the browser interface.
* **Internal Mechanisms**:
  - **Rate Vivaldi Dialog**: Injects `invoke-virtual {p0}, BottomSheetDialogFragment;->dismissAllowingStateLoss()V` into `onViewCreated`.
  - **Search Engine Switch & Donation Promos**: Stubs `showBottomSheetPrompt` with `return-void`.
  - **Donation Click Handler**: Stubs promotional click listener (`https://login.vivaldi.net/profile/donations#...`).
  - **Privacy Report Notifications**: Neutralizes `PrivacyReportNotificationReceiver.onReceive` and alarm scheduler `a`.
  - **Default Browser Promos**: Neutralizes `DefaultBrowserNotificationReceiver.onReceive`.
  - **Vivaldia Game**: Disables main menu entry (`show_vivaldi_game_in_menu -> false`) and flips preference default switch to `false`.
  - **MainSettings**: Strips `rate_vivaldi` and `default_browser_promo` preference cards from the main settings screen.

### 7. Clean Speed Dial Bookmarks (`vivaldiCleanSpeedDialPatch`)
* **Objective**: Eliminate commercial affiliate bookmarks, hide unwanted customization controls, and optimize blank Start Page rendering.
* **Internal Mechanisms**:
  - **Resource Layer**: Sanitizes all `assets/default-bookmarks/*.json` configurations, replacing commercial partner databases (`partners.json`, `partners-locale-map.json`, `fallback-partner-ids.json`) with minimal clean schemas.
  - **Button Visibility**: Locates `SpeedDialTopLevelManager` and forces visibility of the 3-dot customize button (`show_customize_on_startpage`) and add button (`show_add_on_startpage`) to `View.GONE` (`0x8`).
  - **Ghost Touch Neutralization**: Forces `SpeedDialMainView.onInterceptTouchEvent` to return `false`, eliminating phantom touch lag on blank Start Pages.

---

## ⚡ Usability & Startup Performance

### 8. Close Tabs on Exit (`vivaldiCloseTabsOnExitPatch`)
* **Objective**: Guarantee that the browser always opens to a fresh Start Page session without restoring previous background tabs.
* **Internal Mechanism**:
  - Locates `TabStateFileManager` via unique string constants (`tab_state`, `.bak`, `.new`).
  - Neutralizes `readTabState()I` to return `0`, bypassing session tab reconstruction.

### 9. Skip First Run (`skipFirstRunPatch`)
* **Objective**: Eliminate first-launch onboarding wizards, default search engine selection prompts, and terms of service dialogs on clean installs.
* **Internal Mechanism**:
  - Forces `first_run_flow_complete` flag to `true` and bypasses the FRE sequencer.

### 10. Vivaldi Startup Performance Optimization (`vivaldiStartupPerformancePatch`)
* **Objective**: Eliminate cold-start latency and protect against fatal null pointer crashes during initialization.
* **Internal Mechanisms**:
  - **Carrier Customizations Bypass**: Bypasses `PartnerBrowserCustomizations.initializeAsync(Context)` by immediately setting field `Boolean.TRUE` and returning, avoiding main-thread SharedPreferences reads, background ThreadPool tasks, ContentResolver queries, and 10-second timeout task scheduling.
  - **CompositorView LayoutStateObserver Null Safety**: Guards `CompositorView` against early cold-startup NPE when `TabbedRootUiCoordinator` triggers `onStartedShowing(2)` before child views are inflated.
  - **Automotive Reflection Suppression**: Stubs `CarDataProvider.isModel(String)` to return `false`, suppressing `ClassNotFoundException` reflection error log spam on non-automotive Android devices.

### 11. Disable Background Sync & Periodic Sync (`vivaldiBackgroundSyncPatch`)
* **Objective**: Prevent web pages from scheduling wakeups via Web Background Sync and Periodic Sync APIs.
* **Internal Mechanism**:
  - Injects `return-void` into `BackgroundSync.Periodic.Wakeup.DelayTime` and `BackgroundSync.Wakeup.DelayTime` runners in Android JobScheduler.

### 12. Disable Chromium Tips & Notification Scheduler (`vivaldiNotificationOptimizerPatch`)
* **Objective**: Eliminate background wakeups, native library loading, and in-product promotional tips from Chromium's tips scheduler.
* **Internal Mechanism**:
  - Stubs `NotificationSchedulerTask.schedule(JJ)V` to `return-void`.
  - Forces `NotificationSchedulerTask.c()I` to invoke `cancel()` and return `1`.
  - Stubs `TipsAgent.showTipsPromo(I)V` to `return-void`.

---

## 📦 APK Slimming & Storage Reclamation

### 13. Locale PAK Slimmer (`localePakSlimmerPatch`)
* **Objective**: Strip unselected language resource PAKs from `assets/locales/` to reclaim **~21.2 MB** of storage.
* **Internal Mechanism**:
  - Chromium DataPack v5 files cannot be truncated to 0 bytes or deleted without causing native C++ crashes.
  - Replaces stripped base language PAKs with `en-US.pak` byte tables and populates grammatical gender variants (`*_FEMININE`, `*_MASCULINE`) with 18-byte minimal valid DataPack headers, maintaining 100% startup stability.

### 14. Resource Slimmer (`vivaldiResourceSlimmerPatch`)
* **Objective**: Strip bundled bloat assets and harden declarative preference defaults to save **~6.7 MB**.
* **Internal Mechanisms**:
  - **Asset Trimming**: Zeros bundled stock wallpapers (`assets/wallpapers`), sponsored Speed Dial thumbnails (`assets/sd_thumbnails`), partner favicons (`assets/favicons`), and Google Privacy Sandbox attestations (`assets/privacy_sandbox_attestations/privacy-sandbox-attestations.dat`).
  - **Declarative Preference Hardening**: Rewrites defaults in `assets/prefs_definitions.json`:
    - `enable_document_blocking`: `true` (enables ad/tracker blocking by default)
    - `direct_match_enabled`: `false` (disables sponsored search suggestions)
    - `direct_match_boosted`: `false`
    - `donation_promo_dismissed`: `1.0` (permanently suppresses donation prompts)
    - `promote`: `false`
