# 🌐 Universal Patch Reference & Configuration Guide

Comprehensive reference for universal optimization and resource slimming patches provided by Morphe Patches. These patches operate without app-specific hardcoding and can be applied across any supported target APK (Brave, Gboard Lite, Hevy, TikTok, NokoPrint, Xiaomi Earbuds) or custom Android applications.

---

## 📋 Universal Patches Overview

| Patch | Type | Target Scope | Primary Mechanism | Space / Performance Impact |
| :--- | :--- | :--- | :--- | :--- |
| **[Locale Resource Slimmer](#1-locale-resource-slimmer-localeresourceslimmerpatch)** | `resourcePatch` | Android Resources (`res/values-*`) | Prunes unselected language directory trees bottom-up | **~5–25 MB** saved depending on target |
| **[DPI Resource Slimmer](#2-dpi-resource-slimmer-dpiresourceslimmerpatch)** | `resourcePatch` | Android Drawables (`res/drawable-*`) | Retains target device screen density, safe orphan forward-copy | **~5–30 MB** saved, reduced bitmap memory footprint |
| **[PNG Asset Optimizer](#3-png-asset-optimizer-pngoptimizerpatch)** | `rawResourcePatch` | PNG Assets (`res/**`, `assets/**`) | In-memory RGBA-verified level 9 zlib recompression + metadata strip | **~1–8 MB** saved, 0% visual degradation |
| **[APK Junk Cleaner](#4-apk-junk-cleaner-apkjunkcleanerpatch)** | `rawResourcePatch` | Root & Metadata (`META-INF/**`, root) | Prunes compiler metadata, Kotlin debug tables, duplicate licenses | **~0.5–2 MB** saved, cleaner packaging |
| **[Universal Offline Mode](#5-universal-offline-mode-universalofflinepatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Revokes `INTERNET` & network permissions + blocks cleartext HTTP | Complete network isolation at OS kernel level |
| **[Universal Telemetry Neutralizer](#6-universal-telemetry-neutralizer-universaltelemetryneutralizerpatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Strips ad permissions, disables analytics providers/services, prunes discovery registrars, injects opt-out flags | Neutralizes third-party analytics & telemetry dispatch |
| **[Universal Native Binary Trimmer](#7-universal-native-binary-trimmer-universalnativebinarytrimmerpatch)** | `rawResourcePatch` | Native Libraries (`lib/**`) | In-situ byte-level zeroing of non-essential tracking & debug `.so` files | **~1–15 MB** saved, removes resident native crash sidecars |
| **[Universal WebP Asset Optimizer](#8-universal-webp-asset-optimizer-universalwebpoptimizerpatch)** | `rawResourcePatch` | WebP Assets (`res/**`, `assets/**`) | Lossless chunk stripping (`EXIF`, `XMP`, `ICCP`) + VP8X header recalculation | **~0.5–5 MB** saved, 0% visual degradation |
| **[Background Sync & JobScheduler Purge](#9-background-sync--jobscheduler-purge-backgroundsyncpurgepatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Strips boot permissions and disables boot receivers & WorkManager schedulers | Eliminates background wakeups, radio alarms, and standby battery drain |
| **[Universal Privacy Permissions Stripper](#10-universal-privacy-permissions-stripper-universalprivacypermissionspatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Selectively revokes sensitive hardware, privacy, and sensor permissions | Eliminates OS permission grants and runtime capability access |
| **[Universal Screenshot Protection Bypass](#11-universal-screenshot-protection-bypass-universalscreenshotprotectionbypasspatch)** | `bytecodePatch` | Dalvik Bytecode & Manifest | Neutralizes `FLAG_SECURE`, unlocks audio playback capture, and suppresses Android 14+ screenshot detection | Allows screenshots, screen recordings, and internal audio capture across protected views |
| **[Universal Screen Timeout Enforcer](#12-universal-screen-timeout-enforcer-universalscreentimeoutenforcerpatch)** | `bytecodePatch` | Dalvik Bytecode & Windows | Neutralizes `keepScreenOn(Z)V` view calls and strips `FLAG_KEEP_SCREEN_ON` (`0x80`) | Enforces OS screen timeout and sleep timer during video playback |
| **[Universal Screen Brightness Governor](#13-universal-screen-brightness-governor-universalscreenbrightnessgovernorpatch)** | `bytecodePatch` | Dalvik Bytecode & Windows | Neutralizes direct writes to `WindowManager.LayoutParams.screenBrightness` (`iput`) | Prevents apps from overriding display brightness via window layout params |

---

## 🛡️ Layered Architecture: Universal vs. App-Specific Patches

Morphe Patches uses a multi-tiered **defense-in-depth** model separating generic packaging-level mitigations from specialized Dalvik runtime hooks:

- **Universal Patches**: Transform manifest/resources/assets/lib and may also rewrite bytecode call sites that target public Android framework APIs (e.g. `FLAG_SECURE`, `FLAG_KEEP_SCREEN_ON`, `screenBrightness`) without depending on app-specific obfuscated classes.
- **App-Specific Patches**: Target obfuscated app code via fingerprints, `extension.mpe` payloads, and proprietary binaries (`libchrome.so`). They neutralize internal state machines, spoof hardware checks, and stub obfuscated methods.

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
| **Background Sync & JobScheduler Purge** | ⚠️ Safe by Default | Keep `stripWakeLock = false` (default) on web browsers (Brave) to prevent suspending background file downloads when the screen turns off. |
| **Universal Privacy Permissions Stripper** | ⚠️ Safe by Default | All toggles are opt-in (default: `false`). Revoke only permissions you wish to strip for your target application to prevent runtime `SecurityException` crashes in apps that lack error handling. |
| **Universal Screenshot Protection Bypass** | ✅ Yes | Safely neutralizes `FLAG_SECURE` bitwise and silences screenshot/screen recording callbacks across any application. |
| **Universal Screen Timeout Enforcer** | ✅ Yes | Safely neutralizes `FLAG_KEEP_SCREEN_ON` bitwise and silences view `setKeepScreenOn` calls to enforce system sleep timeout. |
| **Universal Screen Brightness Governor** | ✅ Yes | Safely neutralizes writes to `WindowManager.LayoutParams.screenBrightness` so display brightness remains strictly under system and user control. |
| **Universal Offline Mode** | ⚠️ Contextual | **Never apply to web browsers (Brave) or streaming media apps (TikTok)**, as it halts socket creation at the OS kernel level (`AID_INET`). For Gboard Lite and Xiaomi Earbuds, pair with their companion app-specific offline patches for graceful timeout handling. |

---

## 1. Locale Resource Slimmer (`localeResourceSlimmerPatch`)

The **`Locale Resource Slimmer`** patch strips unselected language translation directories from `res/` (such as `values-*`, `raw-*`, `xml-*`) across any supported target APK (Gboard Lite, Hevy, Brave, TikTok, NokoPrint, Xiaomi Earbuds) to reduce APK size.

> [!TIP]
> **Chromium Browsers (Brave)**: While `Locale Resource Slimmer` trims standard Android wrapper resources in `res/values-*`, Chromium browsers store over 95% of their strings (~9.64 MB in Brave 1.96.61) in native binary `.pak` files inside `assets/locales/`. For complete multilingual slimming in Brave, combine this patch with the Brave-specific [**`Locale PAK Slimmer`**](apps/brave.md#11-locale-pak-slimmer-localepakslimmerpatch).

### Configuration in Morphe Manager

When configuring the **`Locales to keep`** option (`locales`), specify a comma-separated list of language codes to preserve (e.g. `es, es-419, pt-BR, fr, de`).
- **Default**: `en` (English `en` and `en-US` are always retained).
- **Base Fallback Safety**: Resource directories without language qualifiers (e.g. `res/values/`, `res/xml/`) are strictly preserved.
- **Prefix Matching**: Specifying a base code like `es` automatically preserves both global Spanish and regional variants (`es-rUS`, `es-rES`, `es-r419`).
- **Multi-Package ARSC Traversal**: Automatically scans and slims across all decoded resource packages (`resources/<package>/res`), ensuring split or modularized application packages are cleaned without leaving secondary package folders untouched.

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

## 2. DPI Resource Slimmer (`dpiResourceSlimmerPatch`)

The **`DPI Resource Slimmer`** patch strips unselected screen density asset directories (such as `drawable-mdpi`, `drawable-hdpi`, `drawable-xhdpi`, `mipmap-mdpi`, etc.) from `res/` across all supported target APKs to significantly reduce final APK size and RAM footprint during bitmap decoding.

### Configuration in Morphe Manager

- **`DPI densities to keep` (`dpis`)**: Specify a comma-separated list of densities to retain:
  - **Default value**: `xxhdpi` (corresponds to standard 1080p displays, ~480 dpi, the most common modern smartphone resolution).
  - **Single density (maximum space savings)**: e.g. `xxhdpi` for 1080p devices, or `xxxhdpi` for 1440p / 2K devices.
  - **Multiple densities (broad device compatibility)**: e.g. `xhdpi, xxhdpi`.
  - **Friendly aliases**: Resolution aliases such as `1080p` (`xxhdpi`), `720p` (`xhdpi`), or `1440p` / `2k` (`xxxhdpi`) are supported.
- **`Remove smartwatch (Wear OS) resources` (`stripSmartwatch`)**: Boolean toggle (`false` by default). Safely purges non-values resources qualified for Wear OS smartwatches (`watch`).
- **`Remove Android TV resources` (`stripTelevision`)**: Boolean toggle (`false` by default). Safely purges non-values resources qualified for Android TV / Leanback (`television`).
- **`Remove automotive, dock, and VR resources` (`stripOtherFormFactors`)**: Boolean toggle (`false` by default). Safely purges non-values resources qualified for car head units, desk docks, appliances, or VR headsets (`car`, `desk`, `appliance`, `vrheadset`).
- **Multi-Package ARSC Traversal**: Scans across all decoded package resource directories (`resources/<package>/res`) to deduplicate densities and purge device bloat across primary and modularized packages.

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

## 3. PNG Asset Optimizer (`pngOptimizerPatch`)

The **`PNG Asset Optimizer`** losslessly recompresses PNG assets inside `res/` and `assets/` with maximum zlib compression (`BEST_COMPRESSION`, level 9) and strips non-rendering metadata chunks (`pHYs`, `tEXt`, `tIME`).

### 🛡️ Pixel Safety & 9-Patch Invariants
- **9-Patch Protection**: Files named `*.9.png` or files containing the Android compiled 9-patch chunk `npTc` are **strictly preserved** to prevent UI stretching distortion.
- **Decompression Verification**: Every recompressed PNG stream is inflated and compared in-memory against original raw RGBA pixel buffers prior to writing to disk, guaranteeing zero visual degradation.
- **Anti-Bloat Guard**: If recompression yields a larger file or saves less than 64 bytes, the original file is preserved untouched.

---

## 4. APK Junk Cleaner (`apkJunkCleanerPatch`)

The **`APK Junk Cleaner`** strips non-functional build metadata, compiler properties, Kotlin coroutines debug tables, and duplicate license notices from `META-INF` and APK root.

### 🛡️ Protected Core Invariants
- **Critical Extensions**: `.dex`, `.arsc`, `.xml`, `.so`, `.rsa`, `.sf`, `.dsa` are strictly protected.
- **Service Loader Integrations**: `META-INF/services/` and `META-INF/MANIFEST.MF` are strictly preserved to maintain dynamic dependency injection.
- **Root Whitelist**: Core root directories (`assets`, `res`, `lib`, `smali`) are protected from accidental pruning.

---

## 5. Universal Offline Mode (`universalOfflinePatch`)

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
> Never enable **`Universal Offline Mode`** on applications that inherently require network access, such as web browsers (**Brave**) or streaming platforms (**TikTok**). Revoking `android.permission.INTERNET` causes the Linux kernel to omit the `AID_INET` supplementary group at fork time, causing all web page navigation and video buffering to fail immediately at the OS level.

### Configuration in Morphe Manager

All options in **`Universal Offline Mode`** are declared as native boolean switches (toggles) in Morphe Manager to avoid manual text entry or typing errors:

- **Strip Network State Permissions (`stripNetworkState`)**: Removes `ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE` (Toggle, default: `false`). Keeps network queries permitted by default to prevent runtime `SecurityException` crashes in apps that check connection state without error handling, while socket creation remains blocked via `INTERNET` removal. Enable for strict manifest permission elimination.
- **Strip Wi-Fi Control Permissions (`stripWifiControls`)**: Removes `CHANGE_NETWORK_STATE`, `CHANGE_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, and `NEARBY_WIFI_DEVICES` (Toggle, default: `true`).
- **Strip Push Notification Permissions (`stripPush`)**: Removes `com.google.android.c2dm.permission.RECEIVE` (Toggle, default: `false`).
- **Strip Google Services Sync Permissions (`stripGoogleServices`)**: Removes `com.google.android.providers.gsf.permission.READ_GSERVICES` and `android.permission.GET_ACCOUNTS` (Toggle, default: `false`).
- **Block Cleartext Traffic (`blockCleartext`)**: Enforces `android:usesCleartextTraffic="false"` in `AndroidManifest.xml` (Toggle, default: `true`).

---

## 6. Universal Telemetry Neutralizer (`universalTelemetryNeutralizerPatch`)

The **`Universal Telemetry Neutralizer`** patch neutralizes pervasive third-party tracking, ad-attribution SDKs, and crash analytics frameworks at the Android application manifest level (`AndroidManifest.xml`). It combines permission revocation, component deactivation, and declarative metadata opt-out injection.

> [!NOTE]
> ### The Multi-Layer Telemetry Defense
> Rather than relying exclusively on network blocking or domain filtering, `Universal Telemetry Neutralizer` operates across three structural tiers in Android:
> 1. **OS Capability Revocation**: Strips the Android Advertising ID (`AD_ID`) and Privacy Sandbox permissions so the Google Play Services subsystem cannot return a hardware-bound advertising identifier or assign ad attribution topics.
> 2. **Component Execution Halting**: Explicitly sets `android:enabled="false"` on known third-party analytics ContentProviders and background upload services. Since Android initializes `ContentProvider.onCreate()` before `Application.onCreate()`, disabling these providers halts SDK bootstrap before any user code or tracking loops run.
> 3. **Declarative Opt-Out Injection**: Injects vendor-standard `<meta-data>` opt-out flags directly into `<application>`. Modern SDKs (such as Firebase Analytics, Google Analytics, Firebase Crashlytics, Firebase Performance, AppsFlyer, and Sentry) check these manifest flags during early initialization and automatically deactivate internal collectors and schedulers.

### Neutralized Tracking & Analytics Frameworks

- **Google & Firebase Measurement**: `AppMeasurementContentProvider`, `AppMeasurementService`, `AppMeasurementJobService`, `AppMeasurementReceiver`.
- **Google Analytics (legacy)**: `AnalyticsService`, `AnalyticsJobService`, `AnalyticsReceiver`.
- **Google DataTransport & Firebase Sessions**: `JobInfoSchedulerService`, `TransportBackendDiscovery`, `AlarmManagerSchedulerBroadcastReceiver`, `SessionLifecycleService`.
- **Firebase ComponentDiscovery Registrars**: Prunes registrar `<meta-data>` tags within `ComponentDiscoveryService` for Analytics, Crashlytics, Performance Monitoring, and Sessions, preventing dependency injection from instantiating tracking classes in memory.
- **Sentry Crash & Performance**: `SentryInitProvider`, `SentryPerformanceProvider`.
- **Facebook AppEvents**: `FacebookInitProvider`.
- **Meta Analytics2 / OneFabric**: `FFAlarmUploadJobService`, `GooglePlayUploadService`, `AlarmBasedUploadService`, `Analytics2UploadService`, `LollipopUploadService`, `LollipopUploadSafeService`, `DelayedWorkerService`, `OneFabricUploadAlarmReceiver`, `HighPriUploadRetryReceiver`, `AnalyticsUploadAlarmReceiver`, `DelayedWorkerServiceReceiver`.
- **Crash Detectors & Dump Upload (Lacrima)**: `DumperUploadService`, `ExceptionsUploadService`, `ProfiloUploadService`, `ProtectedLockScreenBroadcastReceiver`, `PublicLockScreenBroadcastReceiver`, `SystemShutdownBootBroadcastReceiver`, `InternalShutdownBootBroadcastReceiver`, `SecureShutdownBootBroadcastReceiver`, `CrashLoop$LastState`.
- **AppsFlyer Attribution**: `PluginInfoContentProvider`, `AFJobSchedulerService`, `SingleInstallBroadcastReceiver`, `MultipleInstallBroadcastReceiver`.
- **Adjust Attribution**: `AdjustReferrerReceiver`.
- **Flurry & Branch Analytics**: `FlurryContentProvider`, `BranchInitProvider`.

### Configuration in Morphe Manager

- **Revoke Advertising & Tracking Permissions (`revokePermissions`)**: Strips `com.google.android.gms.permission.AD_ID`, Android Privacy Sandbox permissions (`ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_CUSTOM_AUDIENCE`, `ACCESS_ADSERVICES_TOPICS`), and install referrer permissions (Toggle, default: `true`).
- **Disable Telemetry ContentProviders (`disableProviders`)**: Sets `android:enabled="false"` on analytics and tracker ContentProviders (Toggle, default: `true`).
- **Disable Telemetry Background Services (`disableServices`)**: Sets `android:enabled="false"` on telemetry upload, JobScheduler, DataTransport, and Firebase session background services (Toggle, default: `true`).
- **Disable Telemetry Receivers (`disableReceivers`)**: Sets `android:enabled="false"` on campaign, install referrer, and measurement broadcast receivers (Toggle, default: `true`).
- **Inject Telemetry Opt-Out Flags & Prune Registrars (`injectOptOutFlags`)**: Injects declarative opt-out `<meta-data>` tags into `<application>` for Firebase Analytics, Crashlytics, Performance, Google Analytics, Sentry, AppsFlyer, and the Facebook SDK (`AutoLogAppEventsEnabled`, `AdvertiserIDCollectionEnabled`), and prunes Firebase discovery registrars (Toggle, default: `true`).
- **Disable Firebase Init Provider (`disableFirebaseInit`)**: Sets `android:enabled="false"` on `FirebaseInitProvider` (Toggle, default: `false`). *Keep disabled if the target app relies on Firebase Core, Auth, or Cloud Messaging (FCM).*
- **Disable Push Notification Services (`disablePushServices`)**: Sets `android:enabled="false"` on Meta Fbns, PushLite, and Firebase Cloud Messaging services (Toggle, default: `false`). *WARNING: this breaks push notifications; enable only to fully silence background push delivery.*
- **Disable Google Analytics Services (`disableGoogleAnalytics`)**: Sets `android:enabled="false"` on legacy Google Analytics background services (`AnalyticsService`, `AnalyticsJobService`) and receivers (`AnalyticsReceiver`) (Toggle, default: `true`).
- **Disable Meta Analytics Upload Pipeline (`disableMetaAnalytics`)**: Sets `android:enabled="false"` on Meta Analytics2/OneFabric upload services, Instagram upload scheduler receiver, and deferred analytics worker components (Toggle, default: `true`).
- **Disable Crash Detectors & Dump Upload (`disableCrashDetectors`)**: Sets `android:enabled="false"` on Lacrima lock-screen/shutdown crash detectors, crash-loop state trackers, and background crash-dump upload services (Toggle, default: `true`).

---

## 7. Universal Native Binary Trimmer (`universalNativeBinaryTrimmerPatch`)

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

## 8. Universal WebP Asset Optimizer (`universalWebpOptimizerPatch`)

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

## 9. Background Sync & JobScheduler Purge (`backgroundSyncPurgePatch`)

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
- **Strip WAKE_LOCK Permission (`stripWakeLock`)**: Removes `android.permission.WAKE_LOCK` from `AndroidManifest.xml` (Toggle, default: `false`). *Keep disabled if the target application requires wake locks for continuous audio playback, video recording, screen-off background downloads (Brave), or foreground navigation.*

---

## 10. Universal Privacy Permissions Stripper (`universalPrivacyPermissionsPatch`)

The **`Universal Privacy Permissions Stripper`** patch selectively strips sensitive privacy, sensor, and hardware permissions from `AndroidManifest.xml` via modular boolean toggles. It enables strict isolation of host applications by removing permissions at the Android packaging level.

> [!NOTE]
> ### Packaging-Tier vs. Runtime Revocation
> In the Android platform architecture, revoking a permission in system settings causes `checkSelfPermission()` to return `PERMISSION_DENIED`. In contrast, completely stripping the `<uses-permission>` and `<uses-permission-sdk-23>` nodes from `AndroidManifest.xml` causes the Android OS to treat the permission as never requested.
> - **Silent Permission Denial**: When an application calls `requestPermissions()` for an unmanifested permission, Android automatically denies it without displaying the runtime permission request dialog.
> - **Zero Attack Surface**: Applications cannot be granted unmanifested permissions via ADB, device owner MDM profiles, or accessibility automation.

> [!IMPORTANT]
> ### Safety & Runtime Assumptions
> To prevent runtime `SecurityException` crashes in applications that invoke hardware APIs without error-handling wrappers, all toggles in `Universal Privacy Permissions Stripper` are **disabled (`false`) by default**. Enable only the specific permission groups you wish to isolate for your target application.

### Targeted Permission Groups

| Toggle (`booleanOption`) | Revoked Manifest Permissions |
| :--- | :--- |
| **Strip Notification Permission** | `android.permission.POST_NOTIFICATIONS` |
| **Strip Camera Permission** | `android.permission.CAMERA` |
| **Strip Microphone Permissions** | `android.permission.RECORD_AUDIO`, `android.permission.CAPTURE_AUDIO_OUTPUT` |
| **Strip Storage & Media Permissions** | `android.permission.READ_EXTERNAL_STORAGE`, `android.permission.WRITE_EXTERNAL_STORAGE`, `android.permission.MANAGE_EXTERNAL_STORAGE`, `android.permission.READ_MEDIA_IMAGES`, `android.permission.READ_MEDIA_VIDEO`, `android.permission.READ_MEDIA_AUDIO`, `android.permission.READ_MEDIA_VISUAL_USER_SELECTED`, `android.permission.ACCESS_MEDIA_LOCATION` |
| **Strip Location Permissions** | `android.permission.ACCESS_FINE_LOCATION`, `android.permission.ACCESS_COARSE_LOCATION`, `android.permission.ACCESS_BACKGROUND_LOCATION` |
| **Strip Contacts & Accounts Permissions** | `android.permission.READ_CONTACTS`, `android.permission.WRITE_CONTACTS`, `android.permission.GET_ACCOUNTS` |
| **Strip Calendar Permissions** | `android.permission.READ_CALENDAR`, `android.permission.WRITE_CALENDAR` |
| **Strip Nearby Devices Permissions** | `android.permission.BLUETOOTH_SCAN`, `android.permission.BLUETOOTH_CONNECT`, `android.permission.BLUETOOTH_ADVERTISE`, `android.permission.NEARBY_WIFI_DEVICES`, `android.permission.UWB_RANGING` |
| **Strip Body Sensors Permissions** | `android.permission.BODY_SENSORS`, `android.permission.BODY_SENSORS_BACKGROUND`, `android.permission.ACTIVITY_RECOGNITION` |

### Configuration in Morphe Manager

- **Strip Notification Permission (`stripNotifications`)**: Removes `POST_NOTIFICATIONS` (Android 13+) from `AndroidManifest.xml` (Toggle, default: `false`).
- **Strip Camera Permission (`stripCamera`)**: Removes `CAMERA` from `AndroidManifest.xml` (Toggle, default: `false`).
- **Strip Microphone Permissions (`stripMicrophone`)**: Removes `RECORD_AUDIO` and audio capture permissions from `AndroidManifest.xml` (Toggle, default: `false`).
- **Strip Storage & Media Permissions (`stripMediaAndStorage`)**: Removes legacy external storage and Android 13+ granular media permissions from `AndroidManifest.xml` (Toggle, default: `false`).
- **Strip Location Permissions (`stripLocation`)**: Removes `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, and `ACCESS_BACKGROUND_LOCATION` from `AndroidManifest.xml` (Toggle, default: `false`).
- **Strip Contacts & Accounts Permissions (`stripContacts`)**: Removes `READ_CONTACTS`, `WRITE_CONTACTS`, and `GET_ACCOUNTS` from `AndroidManifest.xml` (Toggle, default: `false`).
- **Strip Calendar Permissions (`stripCalendar`)**: Removes `READ_CALENDAR` and `WRITE_CALENDAR` from `AndroidManifest.xml` (Toggle, default: `false`).
- **Strip Nearby Devices Permissions (`stripNearbyDevices`)**: Removes Bluetooth scan/connect/advertise and nearby Wi-Fi device permissions from `AndroidManifest.xml` (Toggle, default: `false`).
- **Strip Body Sensors Permissions (`stripSensors`)**: Removes `BODY_SENSORS`, `BODY_SENSORS_BACKGROUND`, and `ACTIVITY_RECOGNITION` from `AndroidManifest.xml` (Toggle, default: `false`).

---

## 11. Universal Screenshot Protection Bypass (`universalScreenshotProtectionBypassPatch`)

The **`Universal Screenshot Protection Bypass`** patch neutralizes `WindowManager.LayoutParams.FLAG_SECURE` (`0x2000`) across all windows, layout parameters, and `SurfaceView` instances, unlocks internal audio playback capture for screen recording on Android 10+, and suppresses native Android 14+ screenshot and screen recording detection callbacks.

### 🛡️ Low-Level Bytecode & Manifest Enforcement

1. **Bitwise Flag Masking**: For any call to `Window.addFlags(flags)`, `Window.setFlags(flags, mask)`, or direct write to `WindowManager.LayoutParams.flags`, the patch injects in-place bitwise masking (`and-int/lit16 vReg, vReg, -0x2001`) immediately prior to execution. The mask `-0x2001` (`0xffffdfff`) deterministically clears bit 13 (`0x2000` / `FLAG_SECURE`) while keeping all other layout flags (`FLAG_FULLSCREEN`, `FLAG_KEEP_SCREEN_ON`, etc.) intact without modifying register allocations.
2. **SurfaceView Unrestricting**: Invocations to `SurfaceView.setSecure(boolean)` (used by DRM players and protected views) are forced to `isSecure = false` (`const/4 vReg, 0x0`).
3. **Audio Playback Capture Unlocking**:
   - Injects `android:allowAudioPlaybackCapture="true"` into `<application>` in `AndroidManifest.xml` (Android 10 / API 29+).
   - Incepts calls to `AudioAttributes.Builder.setAllowedCapturePolicy(int)` and `AudioManager.setAllowedCapturePolicy(int)` to force `AudioAttributes.ALLOW_CAPTURE_BY_ALL` (`0x1`), allowing screen recorders to record crisp internal audio.
4. **Android 14+ Detection Suppression**: Invocations to `Activity.registerScreenCaptureCallback(...)`, `Activity.unregisterScreenCaptureCallback(...)` (Android 14 / API 34), and `WindowManager.removeScreenRecordingCallback(...)` (Android 15 / API 35) are replaced with `nop`. Calls to `WindowManager.addScreenRecordingCallback(...)` are intercepted to immediately return `SCREEN_RECORDING_STATE_NOT_RECORDING` (`0`), preventing apps from detecting screen captures or recording sessions.
5. **Recents Preview Protection**: Enforces `Activity.setRecentsScreenshotEnabled(true)` to prevent apps from obscuring previews in the system app switcher.

### 🛡️ Plug & Play Operation

The patch operates without any manual configuration or boolean options. When enabled, it automatically executes the complete bypass pipeline across all bytecode and manifest components:
- Clears `FLAG_SECURE` (`0x2000`) on `Window.setFlags`, `Window.addFlags`, and `WindowManager.LayoutParams.flags`.
- Forces `SurfaceView.setSecure(false)` on secure surfaces.
- Unlocks internal audio playback capture in `AndroidManifest.xml` and Dalvik audio policies (`ALLOW_CAPTURE_BY_ALL`).
- Silences `Activity.registerScreenCaptureCallback` / `unregisterScreenCaptureCallback` (Android 14) and `WindowManager.addScreenRecordingCallback` (Android 15).
- Forces `Activity.setRecentsScreenshotEnabled(true)` to preserve app switcher visibility.

---

## 12. Universal Screen Timeout Enforcer (`universalScreenTimeoutEnforcerPatch`)

The **`Universal Screen Timeout Enforcer`** patch forces the host application to respect the operating system's configured screen timeout (`SCREEN_OFF_TIMEOUT`) and sleep timers by neutralizing keepScreenOn view calls and stripping `FLAG_KEEP_SCREEN_ON` from windows and layout parameters.

### 🛡️ Low-Level Bytecode & Window Enforcement

1. **Bitwise Flag Masking**: For any call to `Window.addFlags(flags)`, `Window.setFlags(flags, mask)`, or direct write to `WindowManager.LayoutParams.flags`, the patch injects in-place bitwise masking (`and-int/lit16 vReg, vReg, -0x81`) immediately prior to execution. The mask `-0x81` (`0xffffff7f` sign-extended) deterministically clears bit 7 (`0x00000080` / `FLAG_KEEP_SCREEN_ON`) while preserving all other layout and window flags (`FLAG_FULLSCREEN`, `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS`, `FLAG_TRANSLUCENT_STATUS`, etc.) intact without modifying register allocations.
2. **View & SurfaceHolder Neutralization**: Invocations to `View.setKeepScreenOn(Z)V`, `SurfaceView.setKeepScreenOn(Z)V`, `TextureView.setKeepScreenOn(Z)V`, and `SurfaceHolder.setKeepScreenOn(Z)V` are cleanly replaced with `nop`. This ensures views retain their default Android state (`mKeepScreenOn = false`) and never inhibit the OS sleep timer.

### 🛡️ Plug & Play Operation

The patch operates without any manual configuration or boolean options (`default = false`). When enabled, it automatically executes the complete screen timeout enforcement pipeline across all bytecode components:
- Clears `FLAG_KEEP_SCREEN_ON` (`0x80`) on `Window.setFlags`, `Window.addFlags`, and `WindowManager.LayoutParams.flags`.
- Neutralizes `setKeepScreenOn(Z)V` calls across all UI views and surface holders.

---

## 13. Universal Screen Brightness Governor (`universalScreenBrightnessGovernorPatch`)

The **`Universal Screen Brightness Governor`** patch prevents applications from overriding device screen brightness (such as in-app brightness sliders, barcode/QR full-screen brightness, or window-level overrides) by neutralizing all direct writes to `WindowManager.LayoutParams.screenBrightness`.

> [!NOTE]
> **Hardware HDR Video Playback Scope**: Direct field writes to `WindowManager.LayoutParams.screenBrightness` only control window-level UI brightness overrides. Hardware HDR video brightness boosts (HDR10 / PQ / HLG) are driven at the display subsystem level by graphic buffer dataspaces (`BT2020_ITU_PQ`) passed to `SurfaceFlinger`. Disabling HDR video playback requires app-specific stream or codec governor patches (such as `Disable HDR Video Playback` in TikTok) rather than window layout parameters.

### 🛡️ Low-Level Bytecode & Window Enforcement

1. **Instruction Neutralization**: Scans all Dalvik bytecode instructions across the application for direct field store operations targeting `Landroid/view/WindowManager$LayoutParams;->screenBrightness:F` (`iput`). Each matching instruction is cleanly replaced with `nop`.
2. **Framework Default Invariance**: Because the Android OS framework initializes `WindowManager.LayoutParams.screenBrightness` to `BRIGHTNESS_OVERRIDE_NONE` (`-1.0f`), preventing any in-app write guarantees that the window's layout parameters retain `-1.0f` throughout the application lifecycle.
3. **OS-Level Control Guarantee**: The system WindowManager and DisplayManager continue to govern screen brightness according to the user's manual brightness setting and ambient light sensor, completely eliminating unwanted in-app brightness spikes without destabilizing registers or altering method control flow.

### 🛡️ Plug & Play Operation

The patch operates without any manual configuration or boolean options (`default = false`). When enabled in Morphe Manager or CLI, it automatically neutralizes all screen brightness override attempts across all classes in the target APK.


