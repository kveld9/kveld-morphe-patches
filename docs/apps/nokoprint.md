# 🖨️ NokoPrint: Complete Patch & Architecture Guide

Comprehensive technical, architecture, and patch guide for **NokoPrint - WiFi, Bluetooth, USB** (`com.nokoprint`), covering target requirements, ad suppression, telemetry neutralization, and network security hardening.

---

## 🎯 Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target Application** | NokoPrint - WiFi, Bluetooth, USB |
| **Package Name** | `com.nokoprint` |
| **Supported Target Version** | **`5.28.4`** |
| **Target File Format** | Standalone APK (`APK`) or Bundle (`XAPK`) |
| **Screen Density** | `nodpi` / universal |
| **Official Download Source** | [APKPure](https://d.apkpure.com/b/XAPK/com.nokoprint?versionCode=52804) |

---

## 📋 Applied Patches Catalog

| Patch Name | Type | Category | Default | Primary Mechanism |
| :--- | :--- | :--- | :---: | :--- |
| **Ad Dispatch Governor** | `bytecodePatch` | Premium & Ad Blocking | ✅ Yes | Stubs banner, interstitial, and ad loaders; forces `is_no_ads` active; neutralizes anti-tamper exit checks; bypasses rewarded ad gates during printer driver downloads; and purges ad activities and startup providers via companion manifest patch. |
| **Asset Debloat** | `rawResourcePatch` | Storage Reclamation | ✅ Yes | Strips secondary Meta Audience Network DEX, tracking scripts, ad-viewer HTML/JS templates, RuStore TLS certs, and third-party ad assets to reduce APK footprint. |
| **Background Sync Optimizer** | `bytecodePatch` | Battery & Optimization | ✅ Yes | Disables AndroidX WorkManager background alarms, constraint proxies, diagnostic receivers, and non-essential schedulers. |
| **Block Telemetry & Trackers** | `bytecodePatch` | Privacy & Telemetry | ✅ Yes | Strips `AD_ID` advertising permissions, neutralizes Google Firebase/Measurement components in `AndroidManifest.xml`, stubs TikTok Business SDK init/events, and stubs Firebase/Measurement telemetry dispatchers. |
| **Multi-Store Debridger** | `resourcePatch` | Debloat & Performance | ✅ Yes | Disables orphan billing activities, services, and permissions for alternative OEM stores (Huawei HMS/AGConnect & OTA, Xiaomi Billing & Common IAP, Samsung IAP, RuStore, OneStore, CafeBazaar, Amazon IAP). |
| **Network Security Hardening** | `resourcePatch` | Security & Network | ✅ Yes | Enforces user trust anchors while preserving HTTP cleartext traffic for driver downloads and LAN printers. |
| **Universal Patches Suite** | Multiple | Optimization & Privacy | Contextual | Compatible with universal slimmers and privacy patches (Telemetry Neutralizer, Native Binary Trimmer, WebP/PNG Optimizers, DPI/Locale Slimmers, Offline Mode). See [Universal Patch Reference](../universal-patches.md). |

---

## 🔒 Deep Technical Patch Breakdown

### 1. Ad Dispatch Governor (`nokoPrintAdDispatchGovernorPatch`)
- **Objective**: Neutralize all advertising dispatchers, preloading schedulers, anti-tamper exit routines, and nagware while unlocking premium functionality and debloating ad components.
- **Companion Manifest Purge (`nokoPrintAdManifestResourcePatch`)**:
  - **Ad Activities & Receivers**: Disables over 100 components matching ad SDK prefixes (AppLovin, MBridge, Facebook, Unity3D, IronSource, Fyber, Vungle, Chartboost, InMobi, Amazon, AppBrain, PubNative, Smaato, ByteDance, Bigo, Ogury, Moloco, PubMatic, and Tappx).
  - **Startup ContentProviders**: Strips ad mediation and tracker `ContentProvider` declarations (`AppLovinInitProvider`, `AudienceNetworkContentProvider`, `FacebookInitProvider`, `IronsourceLifecycleProvider`, `ProcessLifecycleOwnerInitializer`, `AppBrainInitProvider`, `AGConnectInitializeProvider`, `BigoAdsProvider`, `VungleProvider`, `MBComponentLifecycleProvider`, `LevelPlayActivityLifecycleProvider`).
  - **Initializer Removal**: Removes `AdsSdkInitializer` meta-data from `androidx.startup.InitializationProvider`.
- **Bytecode Hooks**:
  - **No-Ads Status Enforcement**: Forces `com.nokoprint.ActivityRoot.g(Z)Z` (`is_no_ads`) to return `true` (`const/4 v0, 0x1; return v0`), bypassing all client-side ad gates.
  - **Ad Container & Dispatcher Neutralization**: Stubs `ActivityRoot.w(Z)V` (banner container setup and dispatch) with immediate `return-void`.
  - **Ad Callback Neutralization**: Stubs `ActivityRoot.r(String, AdValue)V` and `ActivityRoot.s(String, String, MaxAd)V` with immediate `return-void`.
  - **MobileAds Initialization Neutralization**: Stubs `ActivityRoot.i()Z` to return `false` (`const/4 v0, 0x0; return v0`), preventing Google Mobile Ads SDK initialization.
  - **AppLovin SDK Initialization Neutralization**: Stubs `ActivityRoot.k()Z` to return `false` (`const/4 v0, 0x0; return v0`), preventing AppLovin MAX SDK bootstrap.
  - **Ad Revenue Tracking Neutralization**: Stubs `ActivityRoot.a(J, Z, String)V` with immediate `return-void`, blocking ad revenue telemetry to Facebook and TikTok.
  - **Interstitial Preload Neutralization**: Stubs `f4.b(carousel.d, ActivityRoot, Hashtable)V` with immediate `return-void`.
  - **Anti-Tamper & License Check Bypass**: Stubs Google Play Protect / Pairip license check in `com.pairip.licensecheck.LicenseClient.checkLicense(Context)V` with immediate `return-void`, preventing forced application shutdown.
  - **Rewarded Ad Bypass**: Stubs `com.nokoprint.j4.b` to dismiss the progress dialog and invoke the target driver download callback immediately, avoiding hangs and ad gates.

### 2. Asset Debloat (`nokoPrintAssetDebloatPatch`)
- **Objective**: Reclaim APK storage by zeroing obsolete ad web assets, secondary DEX containers, tracking scripts, and RuStore certificates.
- **Mechanisms**:
  - Strips secondary DEX (`assets/audience_network/classes.dex`, `audience_network.dex`).
  - Strips ad directories: `assets/ad-viewer`, `assets/audience_network`, `assets/iads`, `assets/template`.
  - Cleans ad/tracking files in root assets: PubMatic, Moloco, MBridge, HMS route configs, MRAID bridges.
  - Strips RuStore TLS certificates in `res/raw/` (`sdk_imaging_*.pem`, `sdk_pay_*.pem`, `ministry_of_digital_*.pem`, `rootca_ssl_rsa2022.cer`) while strictly preserving essential UI assets.
  - Strips third-party ad network drawables matching ad tokens (`bigo_`, `tt_`, `pob_`, `tappx_`, `ogury_`, `moloco_`, etc.).

### 3. Background Sync Optimizer (`nokoPrintBackgroundSyncOptimizerPatch`)
- **Objective**: Prevent background wakeups and battery drain from scheduler components.
- **Mechanisms**:
  - Disables AndroidX WorkManager background alarms, constraint proxies, diagnostic receivers, and non-essential schedulers.
  - Neutralizes `WorkManagerInitializer.create` to prevent automatic scheduler startup.

### 4. Block Telemetry & Trackers (`nokoPrintBlockTelemetryPatch`)
- **Objective**: Terminate Firebase Analytics, Google Measurement, Crashlytics, and TikTok Business telemetry.
- **Mechanisms**:
  - Manifest purge: Strips `AD_ID`, `BIND_GET_INSTALL_REFERRER_SERVICE`, and `BIND_APPHUB_SERVICE` permissions, disabling measurement and Firebase components.
  - Stubs `FirebaseAnalytics.a` event logging, `AppMeasurement.logEventInternal`, and Crashlytics reporting in `com.nokoprint.App`.
  - Stubs TikTok Business SDK initialization (`TikTokBusinessSdk.initializeSdk`, `TikTokBusinessSdk.startTrack`).
  - Stubs Measurement signing certificate telemetry reporting (`zzez.zzL`).

### 5. Multi-Store Debridger (`nokoPrintMultiStoreDebridgerPatch`)
- **Objective**: Neutralize orphan billing activities, background sync services, and store permissions.
- **Mechanisms**:
  - Disables alternative OEM store components: Huawei HMS/AGConnect & OTA, Xiaomi Billing & Common IAP, Samsung IAP, RuStore Pay & Imaging, OneStore, CafeBazaar, Amazon IAP, Google Play Billing & Core, and LicenseActivity.
  - Strips orphan store permissions (`BILLING`, `CHECK_LICENSE`, `GET_COMMON_DATA`, `ATTEST`, `ACCESS_ADSERVICES_*`).

### 6. Network Security Hardening (`nokoPrintNetworkSecurityHardeningPatch`)
- **Objective**: Protect printer driver downloads and backend API communication while allowing user trust anchors and preserving local network printing and HTTP fallback downloads.
- **Mechanisms**:
  - Replaces `res/xml/network_security_config.xml`.
  - Configures system and user trust anchors (`<certificates src="system" />`, `<certificates src="user" />`).
  - Preserves `base-config cleartextTrafficPermitted="true"` globally to ensure driver download fallback retries (`http://www.nokoprint.com/android_packs/`) and direct communication with WiFi and Ethernet printers (raw port 9100 / IPP / LPD) function normally without `Cleartext HTTP traffic not permitted` errors.
