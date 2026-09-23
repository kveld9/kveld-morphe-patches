# 🎧 Xiaomi Earbuds: Complete Patch, Architecture & Configuration Guide

Comprehensive technical, setup, and configuration guide for **Xiaomi Earbuds** (`com.mi.earphone`), pinned to target version **`1.38.0i`**.

---

## 🎯 Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target Application** | Xiaomi Earbuds |
| **Package Name** | `com.mi.earphone` |
| **Supported Target Version** | **`1.38.0i`** |
| **Target File Format** | Split APK Bundle (`BUNDLE` / XAPK / APKM) |
| **Official Download Source** | [APKPure: Xiaomi Earbuds](https://apkpure.com/xiaomi-earbuds/com.mi.earphone) |

> [!NOTE]
> Morphe Manager automatically merges split modules and asset packs into a unified installable APK during patching.

---

## ⚠️ First Boot & Initial Device Pairing Invariant (`Offline Only`)

> [!WARNING]
> Do **NOT** activate the **`Xiaomi Earbuds Offline Only`** patch on your first application launch or when pairing a new earphone model for the first time.
> 
> **Why this matters:**
> - When pairing a device for the first time, Xiaomi Earbuds must reach Xiaomi CDN servers to download model-specific UI plugin assets, translation files, and earphone feature schemas to local storage (`/data/data/com.mi.earphone/files/plugins/`).
> - The `Xiaomi Earbuds Offline Only` patch completely strips `INTERNET` and `ACCESS_NETWORK_STATE` permissions from `AndroidManifest.xml` and spoofs network availability queries to disconnected.
> - If applied on initial setup, the app cannot download the device plugin, resulting in missing device management screens or connection timeouts.
> 
> **Recommended Workflow:**
> 1. **Initial Setup**: Patch and install the app with default settings (`Xiaomi Earbuds Offline Only` unchecked/disabled).
> 2. **Pair Device**: Open the app and pair your earphones, allowing the app to download the local plugin and device configuration assets.
> 3. **Full Network Isolation (Optional)**: Once your earphones appear and are fully functional in the app, re-patch the APK with `Xiaomi Earbuds Offline Only` enabled for complete air-gapped privacy.

---

## 📋 Applied Patches Catalog

| Patch Name | Category | Default | Primary Mechanism | Technical Impact |
| :--- | :--- | :---: | :--- | :--- |
| **Xiaomi Earbuds Anti-Tamper Bypass** | Security & Integrity | ✅ Yes | Intercepts Xposed/hook detection methods (`isHookByStack`, `isXposedHook`), bypasses root/emulator detection, neutralizes VPN/ADB checks, and hardens WebView interfaces. | Eliminates false-positive app crashes or restrictions when running on modified devices, rootless environments, or under network proxies. |
| **Xiaomi Earbuds Background Optimizer** | Battery & Performance | ✅ Yes | Disables `KeepAliveForegroundService`, background BLE observation threads, CompanionDeviceManager registration, and MIUI Nearby discovery workers in bytecode and manifest. | Eliminates persistent foreground notification icons, background wakelocks, and battery drain during screen-off sleep. |
| **Xiaomi Earbuds Block Telemetry & Trackers** | Privacy & Telemetry | ✅ Yes | Neutralizes Xiaomi OneTrack analytics dispatching, AutoReportHelper crash logs, and GlobalReport telemetry dispatchers. | Completely halts outbound telemetry pings and analytics data collection to Xiaomi servers. |
| **Xiaomi Earbuds Device Privacy Guard** | Privacy & Anti-Profiling | ✅ Yes | Anonymizes hardware identifiers (OAID, GAID, Android ID, MAC addresses) and bypasses location permission assertions for Bluetooth scanning. | Prevents device tracking and persistent cross-device profiling while allowing BLE discovery without location services. |
| **Xiaomi Earbuds Disable Promos & Nags** | Debloat & Usability | ✅ Yes | Forces startup privacy agreement checks, onboarding tutorials, region selector dialogs, and Bluetooth permission nags to return accepted/completed, and disables in-app promotional banners. | Instant app startup with clean navigation directly to connected devices without modal dialogs or store review nags. |
| **Xiaomi Earbuds Guest OTA Unlock** | Usability & Firmware | ✅ Yes | Bypasses mandatory Xiaomi account login gates for device firmware updates. | Allows guest users without a Xiaomi account to check, download, and install official device firmware updates over-the-air. |
| **Xiaomi Earbuds Network Security & TLS Inspection** | Security & Network | ✅ Yes | Disables cleartext HTTP traffic, adds user-installed certificate authority trusts, and bypasses OkHttp certificate pinning. | Protects transport security while permitting network inspection and reverse engineering in security research environments. |
| **Xiaomi Earbuds Offline Only** | Privacy & Isolation | ❌ *Optional* | Revokes internet permissions in `AndroidManifest.xml` and spoofs network availability state to offline. | Completely isolates the app from the internet for zero-network operation while keeping local Bluetooth device management fully operational. |
| **Xiaomi Earbuds Model Catalog Unlock** | Usability & Device Support | ✅ Yes | Forces `DeviceInfoListCache.isShowProduct` to return `true`, bypassing version gating and distribution channel checks. | Ensures all device models in the catalog are discoverable and selectable for Bluetooth pairing regardless of app version or distribution channel. |
| **Xiaomi Earbuds Sound Features Unlock** | Audio & Features | ✅ Yes | Unblocks Spatial Audio, hearing enhancement, and voice wake-up restrictions, and bypasses the 96kHz aptX Adaptive sample rate restriction and XPAN requirements. | Activates advanced sound enhancements (Spatial Audio, aptX Adaptive 96kHz) across all supported companion hardware. |
| **Xiaomi Earbuds Surgical OEM Unlock** | Hardware & OEM Bypass | ✅ Yes | Bypasses Xiaomi OEM device gating for SuperAivs (function 5009) and suppresses MIUI-specific spatial audio system dialog nags. | Unlocks full feature parity on non-Xiaomi/non-MIUI Android devices (Samsung, Google Pixel, Motorola, OnePlus, etc.). |
| **Universal Patches Suite** | Optimization & Privacy | Contextual | Compatible with universal slimmers and privacy patches (Telemetry Neutralizer, Native Binary Trimmer, WebP/PNG Optimizers, DPI/Locale Slimmers, Offline Mode). See [Universal Patch Reference](../universal-patches.md). | Significantly reduces final APK size and optimizes assets. |
