# 🦁 Brave Browser: Complete Patch & Configuration Guide

Comprehensive technical and configuration guide for **Brave Browser** (`com.brave.browser`), covering target requirements, applied patches, configuration options, and architectural invariants.

---

## 🎯 Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target Application** | Brave Private Web Browser, VPN |
| **Package Name** | `com.brave.browser` |
| **Supported Target Version** | **`1.95.104`** |
| **Target File Format** | Standalone APK (`APK`) |
| **Recommended Architecture** | `arm64-v8a` (or `armeabi-v7a` for 32-bit devices) |
| **Official Download Source** | [GitHub: brave/brave-browser/releases](https://github.com/brave/brave-browser/releases/tag/v1.95.104) |
| **Recommended APK Assets** | `Bravemonoarm64.apk` (64-bit) or `BraveMonoarm.apk` (32-bit) |

> [!IMPORTANT]
> Always download the official standalone `Bravemonoarm64.apk` or `BraveMonoarm.apk` release builds directly from Brave's GitHub repository. Avoid multi-architecture split bundles or unofficial third-party repacks.

---

## 📋 Applied Patches Catalog

| Patch Name | Type | Category | Default | Primary Mechanism |
| :--- | :--- | :--- | :---: | :--- |
| **Clean New Tab Page** | `bytecodePatch` + `rawResourcePatch` | Debloat & Performance | ✅ Yes | Sanitizes `default.json` asset campaigns, forces NTP marketing preference defaults to `false`, and intercepts `PrefService.e` gates. |
| **Sensor Privacy Guard** | `bytecodePatch` | Privacy & Anti-Fingerprinting | ✅ Yes | Forces `PlatformSensorProvider.hasSensorType -> false` and `PlatformSensor.create -> null`. Neutralizes W3C Generic Sensor APIs. |
| **Clean Share URL** | `bytecodePatch` | Privacy & Anti-Tracking | ✅ Yes | Hooks Android share intent builder (`Lcch.a`) and clipboard copy (`Clipboard.setText`) to purge tracking parameters (`utm_*`, `fbclid`, `gclid`, etc.). |
| **Block Brave Telemetry** | `bytecodePatch` + `rawResourcePatch` | Privacy & Telemetry | ✅ Yes | Intercepts `PrefService.e` (P3A, stats, WDP), aborts variations seed HTTP connection, and redirects native endpoints to `0.0.0.0`. |
| **Suppress In-App Promos & Surveys** | `bytecodePatch` | Debloat & UX | ✅ Yes | Forces `RateEligibilityGate.d -> false`, neutralizes rating survey bottom sheets (`BraveRateDialogFragment`), and drops promotional dialogs (YouTube, ad-free callouts, Brave Ads). |
| **Locale PAK Slimmer** | `rawResourcePatch` | Storage Reclamation | ✅ Yes | Strips unselected language PAKs from `assets/locales/` (~10.5 MB saved) using zero-crash binary fallback substitution. |
| **Universal Patches Suite** | Multiple | Optimization & Privacy | Contextual | Compatible with universal slimmers and privacy patches (Telemetry Neutralizer, Native Binary Trimmer, WebP/PNG Optimizers, DPI/Locale Slimmers). See [Universal Patch Reference](../universal-patches.md). |

---

## 🔒 Deep Technical Patch Breakdown

### 1. Clean New Tab Page (`braveCleanNtpPatch`)
- **Objective**: Completely eliminate sponsored advertising wallpapers, background campaign asset downloads, and Brave News/Today promotional cards.
- **Mechanisms**:
  - **Asset Campaign Sanitization**: Parses and empties `assets/default.json` sponsored image payload definitions.
  - **Preference Defaults**: Overrides NTP preference keys to ensure sponsored image rotation and Brave News are disabled out-of-the-box without requiring manual toggling.
  - **Gate Interception**: Intercepts `PrefService.e` checks for NTP marketing features to return disabled.
- **Impact**: Saves ~15–30 MB/month of cellular background wallpaper downloads and produces an instant, pristine New Tab Page.

### 2. Sensor Privacy Guard (`sensorPrivacyPatch`)
- **Objective**: Prevent side-channel hardware fingerprinting, battery drain, and acoustic keystroke sniffing via web sensor APIs.
- **Mechanisms**:
  - Forces `PlatformSensorProvider.hasSensorType(int)` to return `false`.
  - Forces `PlatformSensor.create(...)` to return `null`.
  - Conforms to W3C specifications so web applications receive standard "unsupported" states gracefully rather than crashing.

### 3. Clean Share URL (`cleanShareUrlPatch`)
- **Objective**: Strip invasive analytics and tracking tokens when copying or sharing links from the browser.
- **Mechanisms**:
  - Intercepts link sharing via `Lcch.a` and clipboard copy via `Clipboard.setText`.
  - Routes URLs through companion extension logic [`BraveExtension`](file:///home/kveld/Documentos/repos/brave-origin-patches/extensions/extension/src/main/java/com/kveld9/morphe/extension/BraveExtension.java).
  - Strips query parameters: `utm_*`, `fbclid`, `gclid`, `igshid`, `si`, `msclkid`, `mc_eid`, `vero_id`, etc.
  - Preserves legitimate functional parameters (`id`, `v`, `q`, `t`, `list`).

### 4. Block Brave Telemetry (`braveBlockTelemetryPatch`)
- **Objective**: Halt outbound telemetry pings, usage metrics, and variations seed fetching.
- **Mechanisms**:
  - **Bytecode Neutralization**: Intercepts `PrefService.e` queries for P3A (*Privacy-Preserving Product Analytics*), Brave Stats, and WDP (*Web Discovery Project*).
  - **Variations Connection Abort**: Injects early returns into HTTP loaders fetching experimentation variations seeds.
  - **Native Socket Redirection**: In `libchrome.so`, redirects native telemetry endpoints (`*.bsg.brave.com`, `*.wdp.brave.com`, `usage-ping.brave.com`) to `0.0.0.0`.

### 5. Suppress In-App Promos & Surveys (`braveSuppressInAppPromosPatch`)
- **Objective**: Eliminate intrusive in-app rating survey bottom sheets, Play Store review prompts, and unsolicited marketing promo popups while browsing.
- **Mechanisms**:
  - **Rating Eligibility Neutralization**: Forces `RateEligibilityGate.d(Context) -> false`, halting rating dialog triggers in `ChromeTabbedActivity` startup and suppressing promotional rating card insertion into the New Tab / News feed.
  - **Survey Bottom Sheet Suppressor**: Intercepts `BraveRateDialogFragment.c5`, `BraveAskPlayStoreRatingDialog.c5`, and `BraveRateThanksFeedbackDialog.c5` with immediate `return-void` so dialog fragments never attach or display.
  - **Promotional Dialog Interceptor**: Intercepts `BraveDialogFragment.c5` to suppress `OpenYtInBraveDialogFragment` and `BraveAdFreeCalloutDialogFragment` popups.
  - **Brave Ads Onboarding Neutralization**: Forces `BraveAdsSignupDialog.a() -> false` and `BraveAdsSignupDialog.b(Context) -> return-void`.

### 6. Locale PAK Slimmer (`localePakSlimmerPatch`)
- **Objective**: Strip unselected language binary PAK archives from `assets/locales/` to reclaim **~10.5 MB** of APK space.
- **Mechanisms**: Replaces stripped `.pak` files with the binary resource table of `en-US.pak` to satisfy Chromium's native C++ `ui::ResourceBundle` loader without triggering fatal assertion aborts.

---

## ⚙️ Configurable Options in Morphe Manager

### Locale PAK Slimmer: Locales to Keep
- **Option Key**: `locales`
- **Default**: `en-US`
- **Accepted Format**: Comma-separated list of locale codes (e.g. `es, es-419, pt-BR, fr, de`).
- **Notes**: English (`en-US`) is always preserved automatically as an essential Chromium fallback. For the full list of 81 supported locale codes, consult the [Universal Patches Guide](../universal-patches.md#1-locale-pak-slimmer-localepakslimmerpatch).

---

## 📐 Architectural Rationale: Omission of Vivaldi Patches in Brave

The following patches present in Vivaldi are intentionally omitted from Brave Browser due to upstream differences:

1. **Google Privacy Sandbox Attestations Zeroing (`privacy-sandbox-attestations.dat`)**:
   - Brave removes and disables Google Privacy Sandbox APIs (Topics API, FLEDGE / Protected Audience, Attribution Reporting) at the C++ level in `brave-core`. The underlying APIs are completely uncallable by web content, making zeroing the `.dat` file redundant.
2. **UKM (URL-Keyed Metrics) Recorder Neutralization (`UkmRecorder.c()V`)**:
   - Brave completely removes upstream Google UMA and UKM pipelines in C++ and replaces them with P3A and WDP, which are already fully neutralized by `Block Brave Telemetry`.
3. **Close Tabs on Exit (`TabStateFileManager` Hook)**:
   - Brave already provides a native user preference in *Settings -> Close tabs on exit*. Enforcing tab state discard via Dalvik bytecode hooks would override user choice and break intentional session retention.
