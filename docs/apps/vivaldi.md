# 🔴 Vivaldi Browser: Complete Patch, Architecture & Configuration Guide

Comprehensive technical, architectural, and configuration guide for **Vivaldi Browser - Fast & Safe** (`com.vivaldi.browser`), pinned to target version **`8.2.4147.93`**.

---

## 🎯 Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target Application** | Vivaldi Browser - Fast & Safe |
| **Package Name** | `com.vivaldi.browser` |
| **Supported Target Version** | **`8.2.4147.93`** |
| **Target File Format** | Split APK Bundle (`BUNDLE` / APKM) |
| **Target Architecture** | `arm64-v8a` |
| **Official Download Source** | [APKMirror: Vivaldi Browser - Fast & Safe](https://www.apkmirror.com/apk/vivaldi-technologies/vivaldi-browser-beta/vivaldi-browser-beta-8-2-4147-93-release/) |

> [!NOTE]
> Morphe Manager automatically extracts and merges the necessary split modules (`base.apk`, `split_config.arm64_v8a.apk`, device DPI, and locale splits) into a unified installable APK during patching.

---

## 📋 Applied Patches Catalog

| Category | Patch Name | Type | Primary Mechanism & Target |
| :--- | :--- | :--- | :--- |
| **Privacy & Telemetry** | **Block Vivaldi Telemetry** | Composite | Redirects 10 native telemetry/geolocation endpoints to `0.0.0.0`, neutralizes UKM event dispatch, strips DataTransport schedulers. |
| **Privacy & Telemetry** | **Block Vivaldi Sync** | `rawResourcePatch` | Redirects `bifrost.vivaldi.com` to `0.0.0.0` in `libchrome.so` for air-gapped operation. |
| **Privacy & Anti-Tracking** | **Clean Share URL** | `bytecodePatch` | Strips tracking query parameters (`utm_*`, `fbclid`, `gclid`, etc.) on share intents and clipboard copy. |
| **Privacy & Anti-Tracking** | **Sensor Privacy Guard** | `bytecodePatch` | Neutralizes W3C Generic Sensor APIs (gyroscope, accelerometer, ambient light) to prevent hardware fingerprinting. |
| **Privacy & Anti-Tracking** | **Disable Battery Status API & OS Listener** | `bytecodePatch` | Neutralizes `navigator.getBattery` and drops `BATTERY_CHANGED` broadcast events. |
| **De-promotional & UX** | **Disable Vivaldi Prompts & In-App Popups** | `bytecodePatch` | Dismisses "Rate Vivaldi" modals, blocks donation sheets, strips settings promo cards, silences privacy alarms, and hides the Vivaldia game. |
| **De-promotional & UX** | **Clean Speed Dial Bookmarks** | Composite | Sanitizes affiliate bookmarks JSON, hides 3-dot customize/add buttons, disables phantom touch lag on blank Start Page. |
| **Usability & UX** | **Close Tabs on Exit** | `bytecodePatch` | Neutralizes `TabStateFileManager.readTabState` to guarantee a clean Start Page session on every launch. |
| **Usability & UX** | **Skip First Run** | `bytecodePatch` | Skips first-run wizard, search engine picker, and terms of service onboarding. |
| **Performance & Battery** | **Vivaldi Startup Performance Optimization** | `bytecodePatch` | Bypasses OEM partner initialization, guards `CompositorView` against startup NPE crashes, and silences Automotive reflection spam. |
| **Performance & Battery** | **Disable Background Sync & Periodic Sync** | `bytecodePatch` | Neutralizes periodic and one-shot BackgroundSync task wakeups in Android JobScheduler. |
| **Performance & Battery** | **Disable Chromium Tips & Notification Scheduler** | `bytecodePatch` | Neutralizes background tips scheduling alarms (Job ID 105) and tips promo agents. |
| **APK Slimmer** | **Locale PAK Slimmer** | `rawResourcePatch` | Strips unselected language PAKs from `assets/locales/` (~21.2 MB saved) using zero-crash binary fallback substitution. |
| **APK Slimmer** | **Resource Slimmer** | `rawResourcePatch` | Empties bundled stock wallpapers, Speed Dial thumbnails, partner favicons, Privacy Sandbox attestations, and hardens defaults (~6.7 MB saved). |
| **Universal Patches Suite** | Multiple | Optimization & Privacy | Contextual | Compatible with universal slimmers and privacy patches (Telemetry Neutralizer, Native Binary Trimmer, WebP/PNG Optimizers, DPI/Locale Slimmers). See [Universal Patch Reference](../universal-patches.md). |

---

## 🔒 Deep Technical Patch Breakdown

### 1. Block Vivaldi Telemetry (`vivaldiBlockTelemetryPatch`)
- **Native `libchrome.so` Redirection with Scheme Preservation**:
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
- **Bytecode Telemetry Neutralization**:
  - **Chromium UKM (URL-Keyed Metrics)**: Neutralizes `UkmRecorder.c()V` to `return-void`, stopping URL browsing metrics from being dispatched.
  - **Minidump Uploader**: Forces `ChromeMinidumpUploadJobService.onStartJob` to `return 0` (`false`).
  - **Install Referrer**: Neutralizes `InstallReferrerHelper.storeReferrerData` (blocking feedback pings to `feedback.vivaldi.com`).
  - **DirectMatch Suggestions**: Forces `DirectMatchBridge.a()` to return an empty `ArrayList`.
  - **Privacy Preferences**: Forces `PrivacyPreferencesManagerImpl.isUsageAndCrashReportingPermitted` to return `0` (`false`).
- **Resource & Manifest Hardening**:
  - Disables Google Play DataTransport background schedulers (`JobInfoSchedulerService`).
  - Strips dynamic split installation attributes (`requiredSplitTypes`, `splitTypes`) from `AndroidManifest.xml` to allow seamless standalone APK operation.

### 2. Block Vivaldi Sync (`vivaldiBlockSyncPatch`)
- Redirects `bifrost.vivaldi.com` to `0.0.0.0\0` at verified binary offsets in `lib/arm64-v8a/libchrome.so` for air-gapped operation.

### 3. Clean Speed Dial Bookmarks (`vivaldiCleanSpeedDialPatch`)
- **Resource Layer**: Sanitizes all `assets/default-bookmarks/*.json` configurations, replacing commercial partner databases (`partners.json`, `partners-locale-map.json`, `fallback-partner-ids.json`) with minimal clean schemas.
- **Button Visibility**: Locates `SpeedDialTopLevelManager` and forces visibility of the 3-dot customize button (`show_customize_on_startpage`) and add button (`show_add_on_startpage`) to `View.GONE` (`0x8`).
- **Ghost Touch Neutralization**: Forces `SpeedDialMainView.onInterceptTouchEvent` to return `false`, eliminating phantom touch lag on blank Start Pages.

### 4. Close Tabs on Exit (`vivaldiCloseTabsOnExitPatch`)
- Locates `TabStateFileManager` via unique string constants (`tab_state`, `.bak`, `.new`).
- Neutralizes `readTabState()I` to return `0`, ensuring the browser always launches with a clean Start Page session without restoring previous tabs.

### 5. Resource Slimmer (`vivaldiResourceSlimmerPatch`)
- **Asset Trimming**: Zeros bundled stock wallpapers (`assets/wallpapers`), sponsored Speed Dial thumbnails (`assets/sd_thumbnails`), partner favicons (`assets/favicons`), and Google Privacy Sandbox attestations (`assets/privacy_sandbox_attestations/privacy-sandbox-attestations.dat`).
- **Declarative Preference Hardening**: Rewrites defaults in `assets/prefs_definitions.json`:
  - `enable_document_blocking`: `true` (enables ad/tracker blocking by default)
  - `direct_match_enabled`: `false` (disables sponsored search suggestions)
  - `direct_match_boosted`: `false`
  - `donation_promo_dismissed`: `1.0` (permanently suppresses donation prompts)
  - `promote`: `false`

---

## ⚙️ Configurable Options: Locale PAK Slimmer

The **`Locale PAK Slimmer`** patch strips unselected language PAKs from `assets/locales/` (~21.2 MB saved) using zero-crash binary fallback substitution:
- **Option Key**: `locales`
- **Default**: `en-US`
- **Accepted Format**: Comma-separated list of locale codes (e.g. `es, es-419, pt-BR, fr, de`).
- **Notes**: English (`en-US`) is always preserved automatically. Corresponding grammatical gender variants (e.g. `es-419_FEMININE`) are preserved automatically to prevent native C++ resource loader crashes. Consult the [Universal Patches Guide](../universal-patches.md#1-locale-pak-slimmer-localepakslimmerpatch) for the complete list of 81 supported locale codes.
