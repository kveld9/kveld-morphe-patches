# 🌐 Universal Patch Reference & Configuration Guide

Comprehensive reference for universal optimization and resource slimming patches provided by Morphe Patches. These patches operate without app-specific hardcoding and can be applied across any supported target APK (Brave, Vivaldi, Gboard Lite, Hevy, TikTok, NokoPrint, Xiaomi Earbuds) or custom Android applications.

---

## 📋 Universal Patches Overview

| Patch | Type | Target Scope | Primary Mechanism | Space / Performance Impact |
| :--- | :--- | :--- | :--- | :--- |
| **[Locale PAK Slimmer](#1-locale-pak-slimmer-localepakslimmerpatch)** | `rawResourcePatch` | Chromium (`assets/locales/*.pak`) | Binary fallback substitution with `en-US` tables | **~10.5 MB** (Brave) / **~21.2 MB** (Vivaldi) saved |
| **[Locale Resource Slimmer](#2-locale-resource-slimmer-localeresourceslimmerpatch)** | `resourcePatch` | Android Resources (`res/values-*`) | Prunes unselected language directory trees bottom-up | **~5–25 MB** saved depending on target |
| **[DPI Resource Slimmer](#3-dpi-resource-slimmer-dpiresourceslimmerpatch)** | `resourcePatch` | Android Drawables (`res/drawable-*`) | Retains target device screen density, safe orphan forward-copy | **~5–30 MB** saved, reduced bitmap memory footprint |
| **[PNG Asset Optimizer](#4-png-asset-optimizer-pngassetoptimizerpatch)** | `rawResourcePatch` | PNG Assets (`res/**`, `assets/**`) | In-memory RGBA-verified level 9 zlib recompression + metadata strip | **~1–8 MB** saved, 0% visual degradation |
| **[APK Junk Cleaner](#5-apk-junk-cleaner-apkjunkcleanerpatch)** | `rawResourcePatch` | Root & Metadata (`META-INF/**`, root) | Prunes compiler metadata, Kotlin debug tables, duplicate licenses | **~0.5–2 MB** saved, cleaner packaging |
| **[Universal Offline Mode](#6-universal-offline-mode-universalofflinepatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Revokes `INTERNET` & network permissions + blocks cleartext HTTP | Complete network isolation at OS kernel level |
| **[Universal Telemetry Neutralizer](#7-universal-telemetry-neutralizer-universaltelemetryneutralizerpatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Strips ad permissions, disables analytics providers/services, injects opt-out flags | Neutralizes third-party analytics & telemetry dispatch |
| **[Universal Native Binary Trimmer](#8-universal-native-binary-trimmer-universalnativebinarytrimmerpatch)** | `rawResourcePatch` | Native Libraries (`lib/**`) | In-situ byte-level zeroing of non-essential tracking & debug `.so` files | **~1–15 MB** saved, removes resident native crash sidecars |
| **[Universal WebP Asset Optimizer](#9-universal-webp-asset-optimizer-universalwebpoptimizerpatch)** | `rawResourcePatch` | WebP Assets (`res/**`, `assets/**`) | Lossless chunk stripping (`EXIF`, `XMP`, `ICCP`) + VP8X header recalculation | **~0.5–5 MB** saved, 0% visual degradation |
| **[Background Sync & JobScheduler Purge](#10-background-sync--jobscheduler-purge-backgroundsyncpurgepatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Strips boot permissions and disables boot receivers & WorkManager schedulers | Eliminates background wakeups, radio alarms, and standby battery drain |

---

## 🛡️ Layered Architecture: Universal vs. App-Specific Patches

Morphe Patches uses a multi-tiered **defense-in-depth** model separating generic packaging-level mitigations from specialized Dalvik runtime hooks:

- **Universal Patches**: Operate at the platform and packaging tier. They transform Android XML resources (`AndroidManifest.xml`), optimize asset payloads (`res/**`, `assets/**`), and inspect standard native architecture directories (`lib/**`).
- **App-Specific Patches**: Operate at the Dalvik bytecode execution tier (`dexlib2`), companion runtime DEX payloads (`extension.mpe`), and proprietary binary targets (`libchrome.so`). They neutralize internal state machines, spoof hardware checks, and stub obfuscated methods.

### Coexistence & Pipeline Idempotency
Applying both universal and app-specific patches simultaneously to the same target APK is **completely safe and idempotent**:
1. **Manifest Operations**: If a universal patch (such as `Universal Telemetry Neutralizer`) and an app-specific patch (such as `NokoPrint Block Telemetry`) both target the same component (e.g., `AppMeasurementService`), assigning `android:enabled="false"` multiple times is idempotent.
2. **Permission Revocation**: If a permission node is pruned by an earlier patch pass, subsequent passes skip the missing node without throwing exceptions.
3. **Binary Trimming**: Universal native trimmers zero only standard crash/telemetry libraries without colliding with app-specific bloat trimmers (such as `Brave Native Bloat Slimmer` or `TikTok Core Asset De-bloat`).

### Patch Selection Matrix: Universal Suitability

| Universal Patch | Safe Across Any APK? | Notes & Usage Guidelines |
| :--- | :---: | :--- |
| **Universal Telemetry Neutralizer** | ✅ Yes | Strips advertising IDs and disables third-party analytics providers/receivers without impacting app functionality. |
| **Universal Native Binary Trimmer** | ✅ Yes | Zeroes standard crash reporting and profiler `.so` files (`libcrashlytics`, `libsentry`, `libgwp-asan`). |
| **Universal WebP Asset Optimizer** | ✅ Yes | Lossless metadata stripping adhering strictly to RFC 9649 / libwebp bitstream specification. |
| **PNG Asset Optimizer** | ✅ Yes | Lossless RGBA-verified zlib recompression and chunk stripping. |
| **APK Junk Cleaner** | ✅ Yes | Prunes compiler properties, Kotlin debug tables, and duplicate license files from the APK root. |
| **DPI Resource Slimmer** | ✅ Yes | Safely retains target device screen density and preserves orphan resources. |
| **Locale Resource Slimmer** | ✅ Yes | Prunes unselected translation folders from `res/values-*`. |
| **Background Sync & JobScheduler Purge** | ⚠️ Safe by Default | Keep `stripWakeLock = false` (default) on web browsers (Brave, Vivaldi) to prevent suspending background file downloads when the screen turns off. |
| **Universal Offline Mode** | ⚠️ Contextual | **Never apply to web browsers (Brave, Vivaldi) or streaming media apps (TikTok)**, as it halts socket creation at the OS kernel level (`AID_INET`). For Gboard Lite and Xiaomi Earbuds, pair with their companion app-specific offline patches for graceful timeout handling. |

---

## 1. Locale PAK Slimmer (`localePakSlimmerPatch`)

The **`Locale PAK Slimmer`** patch strips unneeded language resource PAKs from `assets/locales/` in Chromium-based browsers (Brave, Vivaldi) to reclaim substantial APK storage (**~10.5 MB in Brave** and **~21.2 MB in Vivaldi**).

> [!NOTE]
> ### Why Chromium Browsers Require a Dedicated PAK Slimmer
> Standard Android applications (such as Gboard Lite, Hevy, or NokoPrint) store localized strings in the standard Android resource hierarchy (`res/values-<lang>/strings.xml`). Android's `AssetManager` automatically falls back to base `res/values/` when a localized folder is deleted, making universal directory removal safe via **`Locale Resource Slimmer`**.
>
> In contrast, Chromium-based browsers compile over 95% of their browser UI strings, Omnibox text, Brave Shields, and core Chromium components into **Chromium DataPack v5 binary archives** located in `assets/locales/<locale>.pak`.
> - **Native C++ Loader Invariant**: Chromium's native C++ resource loader (`ui::ResourceBundle`) expects a valid binary DataPack file for the active device locale. Deleting `<locale>.pak` or replacing it with an empty (0-byte) stub triggers a native segmentation fault or assertion failure on startup (`Check failed: file_is_valid`) whenever the device is set to an unselected language.
> - **Zero-Crash Fallback Substitution**: Instead of deleting files, `Locale PAK Slimmer` safely replaces stripped `.pak` files with the binary table of `en-US.pak` (or 18-byte minimal valid DataPack headers for Vivaldi grammatical gender variants). This satisfies the native C++ loader while reclaiming 10 to 22 MB of storage.
> - **Complementary Operation**: For maximum slimming in Brave and Vivaldi, both `Locale PAK Slimmer` (targeting native `assets/locales/*.pak`) and `Locale Resource Slimmer` (targeting Android wrapper `res/values-*`) can be applied together without conflict.

### Configuration in Morphe Manager

When configuring the **`Locales to keep`** option, specify a comma-separated list of locale codes (e.g. `es-419, es, en-US, pt-BR`).
- English (`en-US`) is always preserved automatically as an essential Chromium fallback.
- To prevent Chromium startup crashes on devices configured with unselected system languages, stripped language PAKs are safely populated with the base `en-US` resource table fallback rather than empty stubs.
- In Vivaldi, corresponding grammatical gender variants (e.g. `es-419_FEMININE`) are preserved automatically.

### Popular Language Codes

| Language | Locale Code(s) |
| :--- | :--- |
| **Spanish** | `es` (Spain / Global), `es-419` (Latin America) |
| **English** | `en-US` (US - *Always kept*), `en-GB` (UK) |
| **Portuguese** | `pt-BR` (Brazil), `pt-PT` (Portugal) |
| **French** | `fr` (France), `fr-CA` (Canada) |
| **German / Italian / Dutch** | `de` (German), `it` (Italian), `nl` (Dutch) |
| **Russian / Ukrainian / Polish** | `ru`, `uk`, `pl` |
| **Japanese / Korean / Chinese** | `ja`, `ko`, `zh-CN` (Simplified), `zh-TW` (Traditional), `zh-HK` (Hong Kong) |
| **Nordic Languages** | `sv` (Swedish), `da` (Danish), `fi` (Finnish), `nb` (Norwegian), `is` (Icelandic) |
| **Regional Languages of Spain** | `ca` (Catalan), `gl` (Galician), `eu` (Basque) |
| **Arabic / Turkish / Hebrew** | `ar`, `tr`, `he` |

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

## 2. Locale Resource Slimmer (`localeResourceSlimmerPatch`)

The **`Locale Resource Slimmer`** patch strips unselected language translation directories from `res/` (such as `values-*`, `raw-*`, `xml-*`) across any supported target APK (Gboard Lite, Hevy, Brave, Vivaldi, TikTok, NokoPrint, Xiaomi Earbuds) to reduce APK size.

> [!TIP]
> **Chromium Browsers (Brave & Vivaldi)**: While `Locale Resource Slimmer` trims standard Android wrapper resources in `res/values-*`, Chromium browsers store over 95% of their strings (~10–22 MB) in native binary `.pak` files inside `assets/locales/`. For complete multilingual slimming in Brave and Vivaldi, combine this patch with the specialized **`Locale PAK Slimmer`**.

### Configuration in Morphe Manager

When configuring the **`Locales to keep`** option (`locales`), specify a comma-separated list of language codes to preserve (e.g. `es, es-419, pt-BR, fr, de`).
- **Default**: `en` (English `en` and `en-US` are always retained).
- **Base Fallback Safety**: Resource directories without language qualifiers (e.g. `res/values/`, `res/xml/`) are strictly preserved.
- **Prefix Matching**: Specifying a base code like `es` automatically preserves both global Spanish and regional variants (`es-rUS`, `es-rES`, `es-r419`).

### Popular Language Codes

| Language | Locale Code(s) |
| :--- | :--- |
| **English** | `en` (*Always kept by default*), `en-GB`, `en-CA`, `en-AU`, `en-IN` |
| **Spanish** | `es` (Spain / Global), `es-419` / `es-US` (Latin America / US) |
| **Portuguese** | `pt` (Global), `pt-BR` (Brazil), `pt-PT` (Portugal) |
| **French** | `fr` (France / Global), `fr-CA` (Canada) |
| **German / Italian / Dutch** | `de` (German), `it` (Italian), `nl` (Dutch) |
| **Russian / Ukrainian / Polish** | `ru`, `uk`, `pl` |
| **Japanese / Korean / Chinese** | `ja`, `ko`, `zh` (Global), `zh-CN` (Simplified), `zh-TW` (Traditional), `zh-HK` (Hong Kong) |
| **Nordic Languages** | `sv` (Swedish), `da` (Danish), `fi` (Finnish), `nb` (Norwegian), `is` (Icelandic) |
| **Regional Languages of Spain** | `ca` (Catalan), `gl` (Galician), `eu` (Basque) |
| **Arabic / Turkish / Hebrew** | `ar`, `tr`, `iw` (Hebrew) |

<details>
<summary><b>🔍 View all 100 available locale codes in Gboard Lite</b></summary>
<br>

```text
af, ak, am, ar, as, az, be, bg, bn, bo, bs, ca, cs, da, de, el, en, en-rAU,
en-rCA, en-rGB, en-rIN, en-rXC, es, es-r419, es-rES, es-rUS, et, eu, fa, ff,
fi, fr, fr-rCA, gl, gu, ha, hi, hr, hu, hy, id, ig, in, is, it, iw, ja, ka,
kk, km, kn, ko, ky, lo, lt, lv, mk, ml, mn, mr, ms, my, my-rZG, nb, ne, nl,
nod, or, pa, pl, pt, pt-rBR, pt-rPT, ro, ru, se, si, sk, sl, sou, sq, sr, sv,
sw, ta, te, th, tl, tr, uk, ur, uz, vi, yo, zh, zh-rCN, zh-rHK, zh-rTW, zu
```

</details>

---

## 3. DPI Resource Slimmer (`dpiResourceSlimmerPatch`)

The **`DPI Resource Slimmer`** patch strips unselected screen density asset directories (such as `drawable-mdpi`, `drawable-hdpi`, `drawable-xhdpi`, `mipmap-mdpi`, etc.) from `res/` across all supported target APKs to significantly reduce final APK size and RAM footprint during bitmap decoding.

### Configuration in Morphe Manager

Specify a comma-separated list of densities to retain:
- **Default value**: `xxhdpi` (corresponds to standard 1080p displays, ~480 dpi, the most common modern smartphone resolution).
- **Single density (maximum space savings)**: e.g. `xxhdpi` for 1080p devices, or `xxxhdpi` for 1440p / 2K devices.
- **Multiple densities (broad device compatibility)**: e.g. `xhdpi, xxhdpi`.
- **Friendly aliases**: Resolution aliases such as `1080p` (`xxhdpi`), `720p` (`xhdpi`), or `1440p` / `2k` (`xxxhdpi`) are supported.

### Screen Density Reference Guide

| Density Qualifier | Screen DPI Range | Typical Screen Resolution | Example Devices |
| :--- | :--- | :--- | :--- |
| **`mdpi`** | ~160 dpi (1.0x baseline) | 320x480 / 480x800 | Legacy / ultra low-end devices |
| **`hdpi`** | ~240 dpi (1.5x) | 480x854 / 540x960 | Budget entry-level phones |
| **`xhdpi`** | ~320 dpi (2.0x) | 720x1280 (720p HD) | Entry-level / older 720p phones |
| **`xxhdpi`** *(Default)* | ~480 dpi (3.0x) | 1080x1920 / 1080x2400 (1080p FHD+) | **Most modern smartphones** |
| **`xxxhdpi`** | ~640 dpi (4.0x) | 1440x2560 / 1440x3120 (1440p QHD+) | Premium flagships (Galaxy Ultra, Pixel Pro) |

### 🛡️ Zero-Crash Safety Invariants

1. **Protected Density Qualifiers**: Density-independent directories (`drawable-nodpi`, `drawable-anydpi`, `mipmap-anydpi-v26` for vector drawables and adaptive icons) and unquantified base directories (`drawable`, `mipmap`, `values`, `layout`, etc.) are **strictly preserved and never removed**.
2. **Orphan Asset Preservation**: If a graphical asset exists *exclusively* in a directory marked for deletion, it is automatically copied forward to the target preserved directory before deletion. This prevents runtime `Resources$NotFoundException`.
3. **Empty Folder Pruning**: All empty directories left behind by the removal process are cleaned up bottom-up.

---

## 4. PNG Asset Optimizer (`pngAssetOptimizerPatch`)

The **`PNG Asset Optimizer`** losslessly recompresses PNG assets inside `res/` and `assets/` with maximum zlib compression (`BEST_COMPRESSION`, level 9) and strips non-rendering metadata chunks (`pHYs`, `tEXt`, `tIME`).

### 🛡️ Pixel Safety & 9-Patch Invariants
- **9-Patch Protection**: Files named `*.9.png` or files containing the Android compiled 9-patch chunk `npTc` are **strictly preserved** to prevent UI stretching distortion.
- **Decompression Verification**: Every recompressed PNG stream is inflated and compared in-memory against original raw RGBA pixel buffers prior to writing to disk, guaranteeing zero visual degradation.
- **Anti-Bloat Guard**: If recompression yields a larger file or saves less than 64 bytes, the original file is preserved untouched.

---

## 5. APK Junk Cleaner (`apkJunkCleanerPatch`)

The **`APK Junk Cleaner`** strips non-functional build metadata, compiler properties, Kotlin coroutines debug tables, and duplicate license notices from `META-INF` and APK root.

### 🛡️ Protected Core Invariants
- **Critical Extensions**: `.dex`, `.arsc`, `.xml`, `.so`, `.rsa`, `.sf`, `.dsa` are strictly protected.
- **Service Loader Integrations**: `META-INF/services/` and `META-INF/MANIFEST.MF` are strictly preserved to maintain dynamic dependency injection.
- **Root Whitelist**: Core root directories (`assets`, `res`, `lib`, `smali`) are protected from accidental pruning.

---

## 6. Universal Offline Mode (`universalOfflinePatch`)

The **`Universal Offline Mode`** patch isolates any application from the network by stripping `android.permission.INTERNET` and associated network permissions from `AndroidManifest.xml` and enforcing `android:usesCleartextTraffic="false"` on the `<application>` element.

> [!NOTE]
> ### Kernel-Level Network Enforcement
> In the Android security architecture, removing `android.permission.INTERNET` prevents the Linux kernel from assigning the `AID_INET` supplementary group to the application process at fork time. As a result, all network socket syscalls (`socket()`, `connect()`, `bind()`) fail with `EACCES` (Permission Denied) at the OS level. No bytecode or native library (`.so`) can bypass this barrier.

> [!TIP]
> ### Universal vs. App-Specific Offline Patches
> - **`Universal Offline Mode`**: Operates at the Android manifest level (`AndroidManifest.xml`) by revoking `INTERNET` and enforcing `usesCleartextTraffic="false"`. It provides kernel-level socket blocking across any standard application (such as NokoPrint, Hevy, or custom tools).
> - **App-Specific Companion Patches**: Applications managing local hardware or maintaining internal network state machines provide companion Dalvik bytecode patches that should be applied alongside `Universal Offline Mode`:
>   - **`Xiaomi Earbuds Offline Only`**: Hooks `NetworkExtKt.isNetworkAvailable() -> false` and network predicates so local Bluetooth controls respond instantly without waiting for cloud timeout loops or hanging on infinite loading spinners.
>   - **`Gboard Offline Only`**: Spoofs `DeviceStatusMonitor` to `NO_CONNECTION` and forces HTTP clients (`Cronet`, `OkHttp3`, `Superpacks`) to immediately fail with graceful `IOException("Offline mode")` rather than raw socket permission errors, eliminating download queue retry storms and UI latency.

> [!WARNING]
> ### Do Not Apply to Browsers or Streaming Apps
> Never enable **`Universal Offline Mode`** on applications that inherently require network access, such as web browsers (**Brave**, **Vivaldi**) or streaming platforms (**TikTok**). Revoking `android.permission.INTERNET` causes the Linux kernel to omit the `AID_INET` supplementary group at fork time, causing all web page navigation and video buffering to fail immediately at the OS level.

### Configuration in Morphe Manager

All options in **`Universal Offline Mode`** are declared as native boolean switches (toggles) in Morphe Manager to avoid manual text entry or typing errors:

- **Strip Network State Permissions (`stripNetworkState`)**: Removes `ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE` (Toggle, default: `false`). Keeps network queries permitted by default to prevent runtime `SecurityException` crashes in apps that check connection state without error handling, while socket creation remains blocked via `INTERNET` removal. Enable for strict manifest permission elimination.
- **Strip Wi-Fi Control Permissions (`stripWifiControls`)**: Removes `CHANGE_NETWORK_STATE`, `CHANGE_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, and `NEARBY_WIFI_DEVICES` (Toggle, default: `true`).
- **Strip Push Notification Permissions (`stripPush`)**: Removes `com.google.android.c2dm.permission.RECEIVE` (Toggle, default: `false`).
- **Strip Google Services Sync Permissions (`stripGoogleServices`)**: Removes `com.google.android.providers.gsf.permission.READ_GSERVICES` and `android.permission.GET_ACCOUNTS` (Toggle, default: `false`).
- **Block Cleartext Traffic (`blockCleartext`)**: Enforces `android:usesCleartextTraffic="false"` in `AndroidManifest.xml` (Toggle, default: `true`).

---

## 7. Universal Telemetry Neutralizer (`universalTelemetryNeutralizerPatch`)

The **`Universal Telemetry Neutralizer`** patch neutralizes pervasive third-party tracking, ad-attribution SDKs, and crash analytics frameworks at the Android application manifest level (`AndroidManifest.xml`). It combines permission revocation, component deactivation, and declarative metadata opt-out injection.

> [!NOTE]
> ### The Multi-Layer Telemetry Defense
> Rather than relying exclusively on network blocking or domain filtering, `Universal Telemetry Neutralizer` operates across three structural tiers in Android:
> 1. **OS Capability Revocation**: Strips the Android Advertising ID (`AD_ID`) and Privacy Sandbox permissions so the Google Play Services subsystem cannot return a hardware-bound advertising identifier or assign ad attribution topics.
> 2. **Component Execution Halting**: Explicitly sets `android:enabled="false"` on known third-party analytics ContentProviders and background upload services. Since Android initializes `ContentProvider.onCreate()` before `Application.onCreate()`, disabling these providers halts SDK bootstrap before any user code or tracking loops run.
> 3. **Declarative Opt-Out Injection**: Injects vendor-standard `<meta-data>` opt-out flags directly into `<application>`. Modern SDKs (such as Firebase Analytics, Google Analytics, Firebase Crashlytics, Firebase Performance, AppsFlyer, and Sentry) check these manifest flags during early initialization and automatically deactivate internal collectors and schedulers.

### Neutralized Tracking & Analytics Frameworks

- **Google & Firebase Measurement**: `AppMeasurementContentProvider`, `AppMeasurementService`, `AppMeasurementJobService`, `AppMeasurementReceiver`.
- **Google DataTransport**: `JobInfoSchedulerService`, `TransportBackendDiscovery`, `AlarmManagerSchedulerBroadcastReceiver`.
- **Sentry Crash & Performance**: `SentryInitProvider`, `SentryPerformanceProvider`.
- **Facebook AppEvents**: `FacebookInitProvider`.
- **AppsFlyer Attribution**: `PluginInfoContentProvider`, `AFJobSchedulerService`, `SingleInstallBroadcastReceiver`, `MultipleInstallBroadcastReceiver`.
- **Adjust Attribution**: `AdjustReferrerReceiver`.
- **Flurry & Branch Analytics**: `FlurryContentProvider`, `BranchInitProvider`.

### Configuration in Morphe Manager

- **Revoke Advertising & Tracking Permissions (`revokePermissions`)**: Strips `com.google.android.gms.permission.AD_ID`, Android Privacy Sandbox permissions (`ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_CUSTOM_AUDIENCE`, `ACCESS_ADSERVICES_TOPICS`), and install referrer permissions (Toggle, default: `true`).
- **Disable Telemetry ContentProviders (`disableProviders`)**: Sets `android:enabled="false"` on analytics and tracker ContentProviders (Toggle, default: `true`).
- **Disable Telemetry Background Services (`disableServices`)**: Sets `android:enabled="false"` on telemetry upload, JobScheduler, and DataTransport background services (Toggle, default: `true`).
- **Disable Telemetry Receivers (`disableReceivers`)**: Sets `android:enabled="false"` on campaign, install referrer, and measurement broadcast receivers (Toggle, default: `true`).
- **Inject Telemetry Opt-Out Flags (`injectOptOutMetadata`)**: Injects declarative opt-out `<meta-data>` tags into `<application>` for Firebase Analytics, Crashlytics, Performance, Google Analytics, Sentry, and AppsFlyer (Toggle, default: `true`).
- **Disable Firebase Init Provider (`disableFirebaseInitProvider`)**: Sets `android:enabled="false"` on `FirebaseInitProvider` (Toggle, default: `false`). *Keep disabled if the target app relies on Firebase Core, Auth, or Cloud Messaging (FCM).*

---

## 8. Universal Native Binary Trimmer (`universalNativeBinaryTrimmerPatch`)

The **`Universal Native Binary Trimmer`** patch inspects native architecture directories in `lib/**` (`lib/arm64-v8a/`, `lib/armeabi-v7a/`, `lib/x86_64/`, etc.) and performs byte-level in-situ zeroing (`writeBytes(byteArrayOf())`) on non-essential crash reporters, telemetry engines, and debugging/profiling companion shared libraries (`.so`).

### 🛡️ In-Situ Zeroing vs. File Deletion
- **ZIP Central Directory Invariant**: In Morphe's patching pipeline, deleting native `.so` files from the resource tree can break APK alignment and trigger `UnsatisfiedLinkError` if the application's Java/Kotlin code contains strict class-level `System.loadLibrary(...)` calls without exception handlers.
- **Empty Stub Execution**: By replacing the payload of tracking `.so` files with a 0-byte stub directly in the APK, APK storage is fully reclaimed while eliminating native crash reporting background threads, memory dump scanners, and watchdog sidecars.

### Targeted Native Libraries

- **Crash Reporting & Telemetry**: `libcrashlytics.so`, `libcrashlytics-trampoline.so`, `libsentry.so`, `libsentry-android.so`, `libbugly.so`, `libfirebase-crashlytics.so`, `libapp-measurement.so`, `libplcrashreporter.so`.
- **Debuggers & Profilers**: `libgwp-asan.so`, `libprofiler-service.so`, `libperfa.so`, `libperfa_arm.so`, `libperfa_arm64.so`, `libsimpleperf.so`, `libleaktracer.so`.

### Configuration in Morphe Manager

- **Trim Crash Reporting Libraries (`trimCrashReporters`)**: Replaces native crash reporting and telemetry `.so` binaries with 0-byte stubs (Toggle, default: `true`).
- **Trim Debug & Profiling Libraries (`trimDebugProfilers`)**: Replaces runtime profilers, memory leak detectors, and ASan instrumentation `.so` binaries with 0-byte stubs (Toggle, default: `true`).

---

## 9. Universal WebP Asset Optimizer (`universalWebpOptimizerPatch`)

The **`Universal WebP Asset Optimizer`** losslessly strips non-rendering metadata chunks (`EXIF`, `XMP `, `ICCP`) from WebP images located in `res/**` and `assets/**` across any Android application.

### 🛡️ RFC 9649 / libwebp Bitstream Safety
- **Extended Header Recalculation**: WebP files using the extended `VP8X` chunk format store feature flags in byte 0 of their payload. When `EXIF`, `XMP `, or `ICCP` chunks are stripped, `Universal WebP Asset Optimizer` updates the bitmask to clear the corresponding flag bits (`0x08` for EXIF, `0x04` for XMP, `0x20` for ICCP) while strictly preserving image dimensions, the alpha channel bit (`0x10`), and the animation flag (`0x02`).
- **Container Simplification**: If an extended WebP contains only a single visual frame (`VP8 ` lossy or `VP8L` lossless) and no alpha or animation data after metadata stripping, the patch downgrades the file to a standard simple WebP container (`RIFF....WEBPVP8 ...`), eliminating the unnecessary 18-byte `VP8X` header entirely.
- **Strict Bitstream Bounds**: Any malformed, truncated, or non-conforming WebP file where chunk offsets do not cleanly match the total file size is skipped untouched to prevent visual corruption.

### Configuration in Morphe Manager

- **Strip EXIF Metadata (`stripExif`)**: Removes `EXIF` chunks containing camera metadata, GPS tags, timestamps, and device serials (Toggle, default: `true`).
- **Strip XMP Metadata (`stripXmp`)**: Removes `XMP ` chunks containing XML-based authoring and editing history (Toggle, default: `true`).
- **Strip ICC Color Profiles (`stripIcc`)**: Removes embedded `ICCP` color profiles (Toggle, default: `true`).

---

## 10. Background Sync & JobScheduler Purge (`backgroundSyncPurgePatch`)

The **`Background Sync & JobScheduler Purge`** patch stops unneeded background wakeups, radio modem alarms, and persistent standby battery drain by removing startup permissions and disabling boot-triggered broadcast receivers and periodic WorkManager scheduler components in `AndroidManifest.xml`.

> [!NOTE]
> ### Eliminating Standby Battery Drain
> Many modern Android applications register broadcast receivers with `android.intent.action.BOOT_COMPLETED` and `android.intent.action.MY_PACKAGE_REPLACED`. Whenever the device boots or an app is updated, Android awakens the app process to reschedule background sync tasks, initialize databases, and ping remote servers. `Background Sync & JobScheduler Purge` eliminates this behavior entirely.

### Neutralized Components

- **Boot & Restart Receivers**: Receivers listening for `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `QUICKBOOT_POWERON`, `REBOOT`, `MY_PACKAGE_REPLACED`, `PACKAGE_REPLACED`, and `PACKAGE_RESTARTED`.
- **WorkManager & JobScheduler Components**: `SystemJobService`, `SystemAlarmService`, `SystemForegroundService`, `RescheduleReceiver`, `ForceStopRunnable$BroadcastReceiver`, `ConstraintProxy`, and `DiagnosticsReceiver`.
- **DataTransport Scheduling**: `JobInfoSchedulerService`, `TransportBackendDiscovery`, `AlarmManagerSchedulerBroadcastReceiver`.

### Configuration in Morphe Manager

- **Strip RECEIVE_BOOT_COMPLETED Permission (`stripBootPermission`)**: Removes `android.permission.RECEIVE_BOOT_COMPLETED` and HTC/OEM quickboot permissions from `AndroidManifest.xml` (Toggle, default: `true`).
- **Disable Boot & Package Receivers (`disableBootReceivers`)**: Disables broadcast receivers registered for device startup, reboot, and app replacement events (Toggle, default: `true`).
- **Disable WorkManager & Job Schedulers (`disableWorkManager`)**: Disables WorkManager background services and constraint-checking broadcast receivers (Toggle, default: `true`).
- **Strip WAKE_LOCK Permission (`stripWakeLock`)**: Removes `android.permission.WAKE_LOCK` from `AndroidManifest.xml` (Toggle, default: `false`). *Keep disabled if the target application requires wake locks for continuous audio playback, video recording, screen-off background downloads (Brave, Vivaldi), or foreground navigation.*


