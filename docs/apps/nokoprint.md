# 🖨️ NokoPrint: Complete Patch & Architecture Guide

Comprehensive technical, architecture, and patch guide for **NokoPrint - WiFi, Bluetooth, USB** (`com.nokoprint`), covering target requirements, ad suppression, telemetry neutralization, and network security hardening.

---

## 🎯 Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target Application** | NokoPrint - WiFi, Bluetooth, USB |
| **Package Name** | `com.nokoprint` |
| **Supported Target Version** | **`5.10.8`** |
| **Target File Format** | Standalone APK (`APK`) |
| **Screen Density** | `nodpi` |
| **Official Download Source** | [APKMirror: NokoPrint WiFi Bluetooth USB](https://www.apkmirror.com/apk/nokoprint/nokoprint-wifi-bluetooth-usb/nokoprint-wifi-bluetooth-usb-5-10-8-release/) |

---

## 📋 Applied Patches Catalog

| Patch Name | Type | Category | Default | Primary Mechanism |
| :--- | :--- | :--- | :---: | :--- |
| **NokoPrint Ad Dispatch Governor** | `bytecodePatch` | Premium & Ad Blocking | ✅ Yes | Stubs banner, interstitial, and rewarded ad loaders; forces `isNoAds` active; silences review and subscription prompts; rewrites driver download URL to HTTPS. |
| **NokoPrint Ad Activities Slimmer** | `resourcePatch` | Debloat & Manifest | ✅ Yes | Strips advertising activities, receivers, and metadata for AppLovin, MBridge, and Facebook from `AndroidManifest.xml`. |
| **NokoPrint Asset De-bloat** | `rawResourcePatch` | Storage Reclamation | ✅ Yes | Strips embedded secondary Meta Audience Network DEX and redundant web ad-viewer HTML/JS assets to reduce APK footprint. |
| **NokoPrint Background Sync Optimizer** | `resourcePatch` | Battery & Optimization | ✅ Yes | Disables AndroidX WorkManager background alarms, constraint proxies, diagnostic receivers, and non-essential schedulers. |
| **NokoPrint Block Telemetry** | `resourcePatch` | Privacy & Telemetry | ✅ Yes | Strips `AD_ID` advertising permissions and neutralizes Google Firebase/Measurement components in `AndroidManifest.xml`. |
| **NokoPrint Multi-Store Debridger** | `resourcePatch` | Debloat & Performance | ✅ Yes | Disables orphan billing activities, services, and permissions for alternative OEM stores (Huawei HMS, Xiaomi, Samsung, CafeBazaar, OneStore). |
| **NokoPrint Network Security Hardening** | `resourcePatch` | Security & Network | ✅ Yes | Enforces HTTPS encryption for driver downloads and backend communication while preserving cleartext HTTP traffic for local LAN printers. |
| **NokoPrint Startup Performance Optimizer** | `resourcePatch` | Performance & Startup | ✅ Yes | Disables non-essential ad mediation and tracking ContentProviders that auto-initialize before `Application.onCreate`. |
| **Universal Slimmers** | `resourcePatch` + `rawResourcePatch` | Optimization | ✅ Yes | `Locale Resource Slimmer`, `DPI Resource Slimmer`, `PNG Asset Optimizer`, and `APK Junk Cleaner`. |

---

## 🔒 Deep Technical Patch Breakdown

### 1. NokoPrint Ad Dispatch Governor (`nokoPrintAdDispatchGovernorPatch`)
- **Objective**: Neutralize all advertising dispatchers and subscription nagware while unlocking premium functionality.
- **Mechanisms**:
  - **Ad Loaders Stubbing**: Stubs preloading and dispatch methods in `com.nokoprint.ads.a` (`i`, `j`, `k`), `com.nokoprint.ads.b` (`i`, `j`), and `com.nokoprint.ads.c` (`i`) with immediate `return-void`.
  - **No-Ads Status Enforcement**: Forces `com.nokoprint.n.A(Z)Z` (`isNoAds`) to return `true` (`const/4 v0, 0x1; return v0`), bypassing all client-side ad gates.
  - **Nagware & Prompt Neutralization**: In `com.nokoprint.c`, stubs `y0` (update checker), `T` (exit interstitial preload), `z0` (show interstitial), and `C0` (subscription prompt).
  - **Store Adapters Disabling**: Forces `com.nokoprint.o.k` to return an empty `ArrayList`, disabling dynamic billing store adapters.
  - **Review Prompt Silencing**: Neutralizes `h$t$c.b` review prompt triggers.
  - **Driver Download HTTPS Rewrite**: Rewrites hardcoded cleartext driver URL in `com.nokoprint.d.r0` from `http://nokoprint.com/android_packs/` to `https://nokoprint.com/android_packs/`.

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
