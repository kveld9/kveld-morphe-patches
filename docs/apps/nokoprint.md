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
| **NokoPrint Ad Dispatch Governor** | `bytecodePatch` | Premium & Ad Blocking | ✅ Yes | Stubs banner, interstitial, and ad loaders; neutralizes MobileAds and AppLovin SDK initializations; blocks ad revenue tracking; forces `is_no_ads` active; neutralizes anti-tamper exit checks; rewrites driver download URL to HTTPS. |
| **NokoPrint Ad Activities Slimmer** | `resourcePatch` | Debloat & Manifest | ✅ Yes | Strips over 100 advertising activities, receivers, and metadata components from `AndroidManifest.xml`. |
| **NokoPrint Asset De-bloat** | `rawResourcePatch` | Storage Reclamation | ✅ Yes | Strips secondary Meta Audience Network DEX, tracking scripts, ad-viewer HTML/JS templates, RuStore TLS certs, and third-party ad assets to reduce APK footprint. |
| **NokoPrint Background Sync Optimizer** | `resourcePatch` | Battery & Optimization | ✅ Yes | Disables AndroidX WorkManager background alarms, constraint proxies, diagnostic receivers, and non-essential schedulers. |
| **NokoPrint Block Telemetry & Trackers** | `bytecodePatch` | Privacy & Telemetry | ✅ Yes | Strips `AD_ID` advertising permissions, neutralizes Google Firebase/Measurement components in `AndroidManifest.xml`, stubs TikTok Business SDK init/events, and stubs Firebase/Measurement telemetry dispatchers. |
| **NokoPrint Multi-Store Debridger** | `resourcePatch` | Debloat & Performance | ✅ Yes | Disables orphan billing activities, services, and permissions for alternative OEM stores (Huawei HMS/AGConnect & OTA, Xiaomi Billing & Common IAP, Samsung IAP, RuStore, OneStore, CafeBazaar, Amazon IAP). |
| **NokoPrint Network Security Hardening** | `resourcePatch` | Security & Network | ✅ Yes | Enforces HTTPS encryption for driver downloads and backend communication while preserving cleartext HTTP traffic for local LAN printers. |
| **NokoPrint Startup Performance Optimizer** | `resourcePatch` | Performance & Startup | ✅ Yes | Disables non-essential ad mediation and tracking ContentProviders that auto-initialize before `Application.onCreate`. |
| **Universal Patches Suite** | Multiple | Optimization & Privacy | Contextual | Compatible with universal slimmers and privacy patches (Telemetry Neutralizer, Native Binary Trimmer, WebP/PNG Optimizers, DPI/Locale Slimmers, Offline Mode). See [Universal Patch Reference](../universal-patches.md). |

---

## 🔒 Deep Technical Patch Breakdown

### 1. NokoPrint Ad Dispatch Governor (`nokoPrintAdDispatchGovernorPatch`)
- **Objective**: Neutralize all advertising dispatchers, preloading schedulers, anti-tamper exit routines, and nagware while unlocking premium functionality.
- **Mechanisms**:
  - **No-Ads Status Enforcement**: Forces `com.nokoprint.ActivityRoot.g(Z)Z` (`is_no_ads`) to return `true` (`const/4 v0, 0x1; return v0`), bypassing all client-side ad gates.
  - **Ad Container & Dispatcher Neutralization**: Stubs `ActivityRoot.w(Z)V` (banner container setup and dispatch) with immediate `return-void`.
  - **Ad Callback Neutralization**: Stubs `ActivityRoot.r(String, AdValue)V` and `ActivityRoot.s(String, String, MaxAd)V` with immediate `return-void`.
  - **MobileAds Initialization Neutralization**: Stubs `ActivityRoot.i()Z` to return `false` (`const/4 v0, 0x0; return v0`), preventing Google Mobile Ads SDK initialization.
  - **AppLovin SDK Initialization Neutralization**: Stubs `ActivityRoot.k()Z` to return `false` (`const/4 v0, 0x0; return v0`), preventing AppLovin MAX SDK bootstrap.
  - **Ad Revenue Tracking Neutralization**: Stubs `ActivityRoot.a(J, Z, String)V` with immediate `return-void`, blocking ad revenue telemetry to Facebook and TikTok.
  - **Interstitial Preload Neutralization**: Stubs `f4.b(carousel.d, ActivityRoot, Hashtable)V` with immediate `return-void`.
  - **Anti-Tamper & License Check Bypass**: Stubs Google Play Protect / Pairip license check in `com.pairip.licensecheck.LicenseClient.checkLicense(Context)V` with immediate `return-void`, preventing forced application shutdown.
  - **Driver Download HTTPS Rewrite**: Rewrites hardcoded cleartext driver URL scheme in `com.nokoprint.ActivityCore.J` from `http://` to `https://`.

### 2. Network Security Hardening (`nokoPrintNetworkSecurityHardeningPatch`)
- **Objective**: Protect printer driver downloads and backend API communication against Man-in-the-Middle (MitM) eavesdropping without breaking local printing.
- **Mechanisms**:
  - Replaces `res/xml/network_security_config.xml`.
  - Configures strict system trust anchors.
  - Preserves `base-config cleartextTrafficPermitted="true"` exclusively for local LAN subnets, ensuring direct communication with WiFi and Ethernet printers (raw port 9100 / IPP / LPD) functions normally without TLS negotiation errors.

### 3. Startup Performance Optimizer (`nokoPrintStartupPerformancePatch`)
- **Objective**: Accelerate application launch by preventing tracking SDKs from bootstrapping prior to `Application.onCreate`.
- **Mechanisms**:
  - Strips automated `ContentProvider` declarations from `AndroidManifest.xml`:
    - `com.applovin.sdk.AppLovinInitProvider`
    - `com.google.android.gms.ads.MobileAdsInitProvider`
    - `com.facebook.ads.AudienceNetworkContentProvider`
    - `com.facebook.internal.FacebookInitProvider`
    - `com.ironsource.lifecycle.IronsourceLifecycleProvider`
    - `com.smaato.sdk.core.lifecycle.ProcessLifecycleOwnerInitializer`
    - `com.appbrain.AppBrainInitProvider`
    - `com.huawei.agconnect.core.provider.AGConnectInitializeProvider`
    - `sg.bigo.ads.controller.provider.BigoAdsProvider`
    - `com.vungle.ads.VungleProvider`
    - `com.mbridge.msdk.config.component.status.MBComponentLifecycleProvider`
    - `com.ironsource.lifecycle.LevelPlayActivityLifecycleProvider`
  - Removes `AdsSdkInitializer` meta-data from `androidx.startup.InitializationProvider`.

### 4. NokoPrint Ad Activities Slimmer (`nokoPrintAdActivitiesSlimmerPatch`)
- **Objective**: Disable advertising activities, receivers, and debug services from `AndroidManifest.xml`.
- **Mechanisms**:
  - Disables components matching ad SDK prefixes: AppLovin, MBridge, Facebook, Unity3D, IronSource, Fyber, Vungle, Chartboost, InMobi, Amazon, AppBrain, PubNative, Smaato, Google Ads, ByteDance, Bigo, Ogury, Moloco, PubMatic, and Tappx.

### 5. NokoPrint Asset De-bloat (`nokoPrintAssetDebloatPatch`)
- **Objective**: Reclaim APK storage by zeroing obsolete ad web assets, secondary DEX containers, tracking scripts, and RuStore certificates.
- **Mechanisms**:
  - Strips secondary DEX (`assets/audience_network/classes.dex`, `audience_network.dex`).
  - Strips ad directories: `assets/ad-viewer`, `assets/audience_network`, `assets/iads`, `assets/template`.
  - Cleans ad/tracking files in root assets: PubMatic, Moloco, MBridge, HMS route configs, MRAID bridges.
  - Strips RuStore TLS certificates in `res/raw/` (`sdk_imaging_*.pem`, `sdk_pay_*.pem`, `ministry_of_digital_*.pem`, `rootca_ssl_rsa2022.cer`) while strictly preserving essential UI assets.
  - Strips third-party ad network drawables matching ad tokens (`bigo_`, `tt_`, `pob_`, `tappx_`, `ogury_`, `moloco_`, etc.).

### 6. NokoPrint Block Telemetry & Trackers (`nokoPrintBlockTelemetryPatch`)
- **Objective**: Terminate Firebase Analytics, Google Measurement, Crashlytics, and TikTok Business telemetry.
- **Mechanisms**:
  - Manifest purge: Strips `AD_ID`, `BIND_GET_INSTALL_REFERRER_SERVICE`, and `BIND_APPHUB_SERVICE` permissions, disabling measurement and Firebase components.
  - Stubs `FirebaseAnalytics.a` event logging, `AppMeasurement.logEventInternal`, and Crashlytics reporting in `com.nokoprint.App`.
  - Stubs TikTok Business SDK initialization (`TikTokBusinessSdk.initializeSdk`, `TikTokBusinessSdk.startTrack`).
  - Stubs Measurement signing certificate telemetry reporting (`zzez.zzL`).

### 7. NokoPrint Multi-Store Debridger (`nokoPrintMultiStoreDebridgerPatch`)
- **Objective**: Neutralize orphan billing activities, background sync services, and store permissions.
- **Mechanisms**:
  - Disables alternative OEM store components: Huawei HMS/AGConnect & OTA, Xiaomi Billing & Common IAP, Samsung IAP, RuStore Pay & Imaging, OneStore, CafeBazaar, Amazon IAP, Google Play Billing & Core, and LicenseActivity.
  - Strips orphan store permissions (`BILLING`, `CHECK_LICENSE`, `GET_COMMON_DATA`, `ATTEST`, `ACCESS_ADSERVICES_*`).

### 8. NokoPrint Background Sync Optimizer (`nokoPrintBackgroundSyncOptimizerPatch`)
- **Objective**: Prevent background wakeups and battery drain from scheduler components.
- **Mechanisms**:
  - Disables AndroidX WorkManager background alarms, constraint proxies, diagnostic receivers, and non-essential schedulers.
  - Neutralizes `WorkManagerInitializer.create` to prevent automatic scheduler startup.

