# 🌐 Universal Patch Reference & Configuration Guide

Comprehensive reference for universal optimization and resource slimming patches provided by Morphe Patches. These patches operate without app-specific hardcoding and can be applied across any supported target APK (Brave, Gboard Lite, TikTok) or custom Android applications.

---

## 📋 Universal Patches Overview

| Patch | Type | Target Scope | Primary Mechanism | Space / Performance Impact |
| :--- | :--- | :--- | :--- | :--- |
| **[Remove Extra Languages (Universal)](#1-remove-extra-languages-universal-localeresourceslimmerpatch)** | `resourcePatch` | Android Resources (`res/values-*`) | Prunes unselected language directory trees bottom-up | **~5–25 MB** saved depending on target |
| **[Remove Unused Screen Sizes](#2-remove-unused-screen-sizes-dpiresourceslimmerpatch)** | `resourcePatch` | Android Drawables (`res/drawable-*`) | Retains target device screen density, safe orphan forward-copy | **~5–30 MB** saved, reduced bitmap memory footprint |
| **[Shrink PNG Images](#3-shrink-png-images-pngoptimizerpatch)** | `rawResourcePatch` | PNG Assets (`res/**`, `assets/**`) | In-memory RGBA-verified level 9 zlib recompression + metadata strip | **~1–8 MB** saved, 0% visual degradation |
| **[Clean App Packaging](#4-clean-app-packaging-apkjunkcleanerpatch)** | `rawResourcePatch` | Root & Metadata (`META-INF/**`, root) | Prunes compiler metadata, Kotlin debug tables, duplicate licenses | **~0.5–2 MB** saved, cleaner packaging |
| **[Go Fully Offline](#5-go-fully-offline-universalofflinepatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Revokes `INTERNET` & network permissions + blocks cleartext HTTP | Complete network isolation at OS kernel level |
| **[Block Trackers (Universal)](#6-block-trackers-universal-universaltelemetryneutralizerpatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Strips ad permissions + `android.adservices.*` config, disables analytics providers/services, prunes discovery registrars, injects opt-out flags | Neutralizes third-party analytics & telemetry dispatch |
| **[Remove Tracking Libraries](#7-remove-tracking-libraries-universalnativebinarytrimmerpatch)** | `rawResourcePatch` | Native Libraries (`lib/**`) | In-situ byte-level zeroing of non-essential tracking & debug `.so` files | **~1–15 MB** saved, removes resident native crash sidecars |
| **[Shrink WebP Images](#8-shrink-webp-images-universalwebpoptimizerpatch)** | `rawResourcePatch` | WebP Assets (`res/**`, `assets/**`) | Lossless chunk stripping (`EXIF`, `XMP`, `ICCP`) + VP8X header recalculation | **~0.5–5 MB** saved, 0% visual degradation |
| **[Stop Background Sync (Universal)](#9-stop-background-sync-universal-backgroundsyncpurgepatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Strips boot permissions and disables boot receivers & WorkManager schedulers | Eliminates background wakeups, radio alarms, and standby battery drain |
| **[Strip Sensitive Permissions (Universal)](#10-strip-sensitive-permissions-universal-universalprivacypermissionspatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Selectively revokes sensitive hardware, privacy, and sensor permissions | Eliminates OS permission grants and runtime capability access |
| **[Allow Screenshots (Universal)](#11-allow-screenshots-universal-universalscreenshotprotectionbypasspatch)** | `bytecodePatch` | Dalvik Bytecode & Manifest | Neutralizes `FLAG_SECURE`, unlocks audio playback capture, and suppresses Android 14+ screenshot detection | Allows screenshots, screen recordings, and internal audio capture across protected views |
| **[Force Screen Timeout](#12-force-screen-timeout-universalscreentimeoutenforcerpatch)** | `bytecodePatch` | Dalvik Bytecode & Windows | Neutralizes `keepScreenOn(Z)V` view calls and strips `FLAG_KEEP_SCREEN_ON` (`0x80`) | Enforces OS screen timeout and sleep timer during video playback |
| **[Lock Screen Brightness](#13-lock-screen-brightness-universalscreenbrightnessgovernorpatch)** | `bytecodePatch` | Dalvik Bytecode & Windows | Neutralizes direct writes to `WindowManager.LayoutParams.screenBrightness` (`iput`) | Prevents apps from overriding display brightness via window layout params |
| **[Block Tracking Servers (Universal)](#14-block-tracking-servers-universal-universalhostsblockerpatch)** | `bytecodePatch` | Dalvik `const-string` literals | Rewrites user-blocklisted URL/host literals to a sink IP (`0.0.0.0`) | Silences analytics/ads dispatch without touching native binaries |
| **[Block Tracking SDKs](#15-block-tracking-sdks-universalsdkblockerpatch)** | `bytecodePatch` | Dalvik Bytecode Methods | Neutralizes third-party APM, crash, analytics, attribution, session replay, location, bandwidth-sharing monetization, and push SDK init/event methods via early `return-void` | Neutralizes runtime SDK execution; companion layer to Block Trackers |
| **[Disable Play Store updates](#16-disable-play-store-updates-universaldisableplaystoreupdatespatch)** | `bytecodePatch` | Manifest & Dalvik Bytecode | Sets manifest `versionCode` to `2147483647` and rewrites `PackageInfo` version checks to preserve in-app compatibility | Prevents Play Store updates without breaking in-app version checks |
| **[Spoof Reinstall IDs](#17-spoof-reinstall-ids-universalspoofreinstallidspatch)** | `bytecodePatch` | Dalvik Bytecode Methods | Folds app-set and install/instance ID getters to neutral fixed values | Prevents tracking reinstalls and cross-install correlation |
| **[Silence System Logs](#18-silence-system-logs-universalsilencesystemlogspatch)** | `bytecodePatch` | Dalvik Bytecode Call Sites | Neutralizes log output invocations to prevent diagnostic log leakage | Stops application telemetry and sensitive strings leaking to logcat |
| **[Disable Browser File Associations](#19-disable-browser-file-associations-universalbrowserassociationpatch)** | `resourcePatch` | Manifest (`AndroidManifest.xml`) | Removes HTTP/HTTPS `data` entries from `VIEW` + `BROWSABLE` intent filters; drops filters left dataless | App no longer appears in the browser chooser for online files; local file handling untouched |

---

## 🛡️ Layered Architecture: Universal vs. App-Specific Patches

Morphe Patches uses a multi-tiered **defense-in-depth** model separating generic packaging-level mitigations from specialized Dalvik runtime hooks:

- **Universal Patches**: Transform manifest/resources/assets/lib and may also rewrite bytecode call sites that target public Android framework APIs (e.g. `FLAG_SECURE`, `FLAG_KEEP_SCREEN_ON`, `screenBrightness`) without depending on app-specific obfuscated classes.
- **App-Specific Patches**: Target obfuscated app code via fingerprints, `extension.mpe` payloads, and proprietary binaries (`libchrome.so`). They neutralize internal state machines, spoof hardware checks, and stub obfuscated methods.

### Coexistence & Pipeline Idempotency
Applying both universal and app-specific patches simultaneously to the same target APK is **completely safe and idempotent**:
1. **Manifest Operations**: If a universal patch (such as `Block Trackers (Universal)`) and an app-specific patch (such as `Brave Block Telemetry`) both target the same component (e.g., `AppMeasurementService`), assigning `android:enabled="false"` multiple times is idempotent.
2. **Permission Revocation**: If a permission node is pruned by an earlier patch pass, subsequent passes skip the missing node without throwing exceptions.
3. **Binary Trimming**: Universal native trimmers zero only standard crash/telemetry libraries without colliding with app-specific bloat trimmers (such as Brave's `Remove Unused AI & VPN` or TikTok's `Remove Extra Files`).

### Do Not Stack Universals on Maintained Apps
Dedicated app patches (Brave, Gboard Lite, TikTok) are a strict precision superset for their respective targets. Enabling Block Trackers (Universal) or Block Tracking SDKs on top of dedicated patches adds no extra protection.

If patcher logs report components already clean or 0 methods hooked, that is the expected outcome, not a failure. Stacking these universals on maintained apps only adds patch execution time (such as expensive full-DEX scans on large targets like TikTok) and risks breaking features like login, account sync, or push notifications if optional toggles (e.g., device-ID providers, Firebase init, or push engagement) are enabled. To cover a newly discovered generic SDK in a maintained app, port the rule into that app's dedicated patch instead of enabling the universal patch.

Exception: Block Tracking Servers (Universal) remains a valid opt-in patch on maintained apps when using a curated host blocklist.

### Patch Selection Matrix: Universal Suitability

| Universal Patch | Safe Across Any APK? | Notes & Usage Guidelines |
| :--- | :---: | :--- |
| **Block Trackers (Universal)** | ✅ Yes | Strips advertising IDs and disables third-party analytics providers/receivers without impacting app functionality. Not recommended on maintained apps (Brave, Gboard Lite, TikTok): dedicated telemetry patch already covers them; stacking adds no protection. |
| **Remove Tracking Libraries** | ✅ Yes | Zeroes standard crash reporting, profiler, and bandwidth-sharing `.so` files (`libcrashlytics`, `libsentry`, `libgwp-asan`, `libhopmn`). |
| **Shrink WebP Images** | ✅ Yes | Lossless metadata stripping adhering strictly to RFC 9649 / libwebp bitstream specification. |
| **Shrink PNG Images** | ✅ Yes | Lossless RGBA-verified zlib recompression and chunk stripping. |
| **Clean App Packaging** | ✅ Yes | Prunes compiler properties, Kotlin debug tables, and duplicate license files from the APK root. |
| **Remove Unused Screen Sizes** | ✅ Yes | Safely retains target device screen density and preserves orphan resources. |
| **Remove Extra Languages (Universal)** | ✅ Yes | Prunes unselected translation folders from `res/values-*`. |
| **Stop Background Sync (Universal)** | ⚠️ Safe by Default | Keep `stripWakeLock = false` (default) on web browsers (Brave) to prevent suspending background file downloads when the screen turns off. |
| **Strip Sensitive Permissions (Universal)** | ⚠️ Safe by Default | All toggles are opt-in (default: `false`). Revoke only permissions you wish to strip for your target application to prevent runtime `SecurityException` crashes in apps that lack error handling. |
| **Allow Screenshots (Universal)** | ✅ Yes | Safely neutralizes `FLAG_SECURE` bitwise and silences screenshot/screen recording callbacks across any application. |
| **Force Screen Timeout** | ✅ Yes | Safely neutralizes `FLAG_KEEP_SCREEN_ON` bitwise and silences view `setKeepScreenOn` calls to enforce system sleep timeout. |
| **Lock Screen Brightness** | ✅ Yes | Safely neutralizes writes to `WindowManager.LayoutParams.screenBrightness` so display brightness remains strictly under system and user control. |
| **Go Fully Offline** | ⚠️ Contextual | **Never apply to web browsers (Brave) or streaming media apps (TikTok)**, as it halts socket creation at the OS kernel level (`AID_INET`). For Gboard Lite, pair with its companion app-specific offline patch for graceful timeout handling. |
| **Block Tracking Servers (Universal)** | ⚠️ Curated list required | Ships disabled with no bundled blocklist (user supplies the hosts file at patch time). Block only telemetry/ads hosts (e.g. Hagezi `native.tiktok-onlydomains.txt` filtered to `log/mon/mcs/mssdk/analytics`); blocking functional hosts (`frontier/api/stream/open`) breaks feed, login, or CDN playback. |
| **Block Tracking SDKs** | ✅ Yes | Safely neutralizes APM, crash, analytics, and attribution SDK entrypoints via Dalvik early `return-void`. The optional Push Engagement toggle (`blockPushEngagement`) is disabled by default because silencing push SDKs breaks push notifications. Not recommended on maintained apps (Brave, Gboard Lite, TikTok): dedicated telemetry patch already covers them; stacking adds no protection. |
| **Disable Play Store updates** | ⚠️ Safe by Default | Disabled by default (`false`). Sets `versionCode` to `Int.MAX_VALUE` to detach Google Play Store updates; intercepts in-app `PackageInfo` checks to return the original version code. Incompatible with root mounting. |
| **Spoof Reinstall IDs** | ✅ Yes | Neutralizes app-set ID and instance/install ID getters to prevent cross-session device linking on reinstall. |
| **Silence System Logs** | ✅ Yes | Neutralizes diagnostic log calls (`Log.d/v/i/w/e/wtf` and `println`) with configurable level filtering. |
| **Disable Browser File Associations** | ✅ Yes | Only touches `VIEW` + `BROWSABLE` intent filters carrying `http`/`https` data. Local file handlers (`file`/`content` schemes), launcher entries (`MAIN`) and non-browser actions are never modified. Do not apply to apps that must open web links (browsers, readers). |

---

## 1. Remove Extra Languages (Universal) (`localeResourceSlimmerPatch`)

The **`Remove Extra Languages (Universal)`** patch strips unselected language translation directories from `res/` (such as `values-*`, `raw-*`, `xml-*`) across any supported target APK (Brave, Gboard Lite, TikTok) to reduce APK size.

> [!TIP]
> **Chromium Browsers (Brave)**: While `Remove Extra Languages (Universal)` trims standard Android wrapper resources in `res/values-*`, Chromium browsers store over 95% of their strings (~9.71 MB in Brave 1.97.57) in native binary `.pak` files inside `assets/locales/`. For complete multilingual slimming in Brave, combine this patch with the Brave-specific **`Remove Extra Languages`** patch (Chromium PAK slimmer), which trims those files.

### Configuration in Morphe Manager

When configuring the **`Languages to keep`** option (`locales`), specify a comma-separated list of language codes to preserve (e.g. `es, es-419, pt-BR, fr, de`).
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

## 2. Remove Unused Screen Sizes (`dpiResourceSlimmerPatch`)

The **`Remove Unused Screen Sizes`** patch strips unselected screen density asset directories (such as `drawable-mdpi`, `drawable-hdpi`, `drawable-xhdpi`, `mipmap-mdpi`, etc.) from `res/` across all supported target APKs to significantly reduce final APK size and RAM footprint during bitmap decoding.

### Configuration in Morphe Manager

- **`Screen sizes to keep` (`dpis`)**: Specify a comma-separated list of densities to retain:
  - **Default value**: `xxhdpi` (corresponds to standard 1080p displays, ~480 dpi, the most common modern smartphone resolution).
  - **Single density (maximum space savings)**: e.g. `xxhdpi` for 1080p devices, or `xxxhdpi` for 1440p / 2K devices.
  - **Multiple densities (broad device compatibility)**: e.g. `xhdpi, xxhdpi`.
  - **Friendly aliases**: Resolution aliases such as `1080p` (`xxhdpi`), `720p` (`xhdpi`), or `1440p` / `2k` (`xxxhdpi`) are supported.
- **`Watch resources` (`stripSmartwatch`)**: Boolean toggle (`false` by default). Safely purges non-values resources qualified for Wear OS smartwatches (`watch`).
- **`TV resources` (`stripTelevision`)**: Boolean toggle (`false` by default). Safely purges non-values resources qualified for Android TV / Leanback (`television`).
- **`Car, dock and VR resources` (`stripOtherFormFactors`)**: Boolean toggle (`false` by default). Safely purges non-values resources qualified for car head units, desk docks, appliances, or VR headsets (`car`, `desk`, `appliance`, `vrheadset`).
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

## 3. Shrink PNG Images (`pngOptimizerPatch`)

The **`Shrink PNG Images`** losslessly recompresses PNG assets inside `res/` and `assets/` with maximum zlib compression (`BEST_COMPRESSION`, level 9) and strips non-rendering metadata chunks (`pHYs`, `tEXt`, `tIME`).

> [!NOTE]
> **Pipeline Scope**: In full pipeline mode (with resource decoding), optimization covers both `res/` and `assets/`.
> In raw-only pipeline mode (where resources are not decoded), `res/` is safely skipped and optimization applies exclusively to `assets/`.

### 🛡️ Pixel Safety & 9-Patch Invariants
- **9-Patch Protection**: Files named `*.9.png` or files containing the Android compiled 9-patch chunk `npTc` are **strictly preserved** to prevent UI stretching distortion.
- **Decompression Verification**: Every recompressed PNG stream is inflated and compared in-memory against original raw RGBA pixel buffers prior to writing to disk, guaranteeing zero visual degradation.
- **Anti-Bloat Guard**: If recompression yields a larger file or saves less than 64 bytes, the original file is preserved untouched.

---

## 4. Clean App Packaging (`apkJunkCleanerPatch`)

The **`Clean App Packaging`** strips non-functional build metadata, compiler properties, Kotlin coroutines debug tables, and duplicate license notices from `META-INF` and APK root.

### 🛡️ Protected Core Invariants
- **Critical Extensions**: `.dex`, `.arsc`, `.xml`, `.so`, `.rsa`, `.sf`, `.dsa` are strictly protected.
- **Service Loader Integrations**: `META-INF/services/` and `META-INF/MANIFEST.MF` are strictly preserved to maintain dynamic dependency injection.
- **Root Whitelist**: Core root directories (`assets`, `res`, `lib`, `smali`, `kotlin`) are protected from accidental pruning.
- **Kotlin Runtime Descriptors**: APK-root `kotlin/` (`*.kotlin_builtins`, `kotlin-reflect` runtime descriptors) is strictly preserved.

---

## 5. Go Fully Offline (`universalOfflinePatch`)

The **`Go Fully Offline`** patch isolates any application from the network by stripping `android.permission.INTERNET` and associated network permissions from `AndroidManifest.xml` and enforcing `android:usesCleartextTraffic="false"` on the `<application>` element.

> [!NOTE]
> ### Kernel-Level Network Enforcement
> In the Android security architecture, removing `android.permission.INTERNET` prevents the Linux kernel from assigning the `AID_INET` supplementary group to the application process at fork time. As a result, all network socket syscalls (`socket()`, `connect()`, `bind()`) fail with `EACCES` (Permission Denied) at the OS level. No bytecode or native library (`.so`) can bypass this barrier.

> [!TIP]
> ### Universal vs. App-Specific Offline Patches
> - **`Go Fully Offline`**: Operates at the Android manifest level (`AndroidManifest.xml`) by revoking `INTERNET` and enforcing `usesCleartextTraffic="false"`. It provides kernel-level socket blocking across any standard application (such as Brave, Gboard Lite, or custom tools).
> - **App-Specific Companion Patches**: Applications maintaining internal network state machines provide companion Dalvik bytecode patches that should be applied alongside `Go Fully Offline`:
>   - **`Gboard Offline Only`**: Spoofs `DeviceStatusMonitor` to `NO_CONNECTION` and forces HTTP clients (`Cronet`, `OkHttp3`, `Superpacks`) to immediately fail with graceful `IOException("Offline mode")` rather than raw socket permission errors, eliminating download queue retry storms and UI latency.

> [!WARNING]
> ### Do Not Apply to Browsers or Streaming Apps
> Never enable **`Go Fully Offline`** on applications that inherently require network access, such as web browsers (**Brave**) or streaming platforms (**TikTok**). Revoking `android.permission.INTERNET` causes the Linux kernel to omit the `AID_INET` supplementary group at fork time, causing all web page navigation and video buffering to fail immediately at the OS level.

### Configuration in Morphe Manager

All options in **`Go Fully Offline`** are declared as native boolean switches (toggles) in Morphe Manager to avoid manual text entry or typing errors:

- **Network state (`stripNetworkState`)**: Removes `ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE` (Toggle, default: `false`). Keeps network queries permitted by default to prevent runtime `SecurityException` crashes in apps that check connection state without error handling, while socket creation remains blocked via `INTERNET` removal. Enable for strict manifest permission elimination.
- **Wi-Fi controls (`stripWifiControls`)**: Removes `CHANGE_NETWORK_STATE`, `CHANGE_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, and `NEARBY_WIFI_DEVICES` (Toggle, default: `true`).
- **Push permissions (`stripPush`)**: Removes `com.google.android.c2dm.permission.RECEIVE` (Toggle, default: `false`).
- **Google sync (`stripGoogleServices`)**: Removes `com.google.android.providers.gsf.permission.READ_GSERVICES` and `android.permission.GET_ACCOUNTS` (Toggle, default: `false`).
- **Block unencrypted traffic (`blockCleartext`)**: Enforces `android:usesCleartextTraffic="false"` in `AndroidManifest.xml` (Toggle, default: `true`).

---

## 6. Block Trackers (Universal) (`universalTelemetryNeutralizerPatch`)

The **`Block Trackers (Universal)`** patch neutralizes pervasive third-party tracking, ad-attribution SDKs, and crash analytics frameworks at the Android application manifest level (`AndroidManifest.xml`). It combines permission revocation, component deactivation, and declarative metadata opt-out injection. On repo-maintained apps, do not stack this patch on top of dedicated telemetry patches; see [Do Not Stack Universals on Maintained Apps](#do-not-stack-universals-on-maintained-apps).

> [!NOTE]
> ### The Multi-Layer Telemetry Defense
> Rather than relying exclusively on network blocking or domain filtering, `Block Trackers (Universal)` operates across three structural tiers in Android:
> 1. **OS Capability Revocation**: Strips the Android Advertising ID (`AD_ID`) and Privacy Sandbox permissions so the Google Play Services subsystem cannot return a hardware-bound advertising identifier or assign ad attribution topics.
> 2. **Component Execution Halting**: Explicitly sets `android:enabled="false"` on known third-party analytics ContentProviders and background upload services. Since Android initializes `ContentProvider.onCreate()` before `Application.onCreate()`, disabling these providers halts SDK bootstrap before any user code or tracking loops run.
> 3. **Declarative Opt-Out Injection**: Injects vendor-standard `<meta-data>` opt-out flags directly into `<application>`. Modern SDKs (such as Firebase Analytics, Google Analytics, Firebase Crashlytics, Firebase Performance, AppsFlyer, and Sentry) check these manifest flags during early initialization and automatically deactivate internal collectors and schedulers.

### Neutralized Tracking & Analytics Frameworks

- **Google & Firebase Measurement**: `AppMeasurementContentProvider`, `AppMeasurementService`, `AppMeasurementJobService`, `AppMeasurementReceiver`.
- **Google Analytics (legacy)**: `AnalyticsService`, `AnalyticsJobService`, `AnalyticsReceiver`.
- **Google DataTransport & Firebase Sessions**: `JobInfoSchedulerService`, `TransportBackendDiscovery`, `AlarmManagerSchedulerBroadcastReceiver`, `SessionLifecycleService`, `MlKitComponentDiscoveryService`.
- **Firebase ComponentDiscovery Registrars**: Prunes registrar `<meta-data>` tags within `ComponentDiscoveryService` for pure telemetry (Analytics, Performance Monitoring, Sessions, Measurement) by default, preventing dependency injection from instantiating tracking classes in memory. Functional and app-required registrars (Crashlytics, RemoteConfig, MLKit vision, Installations/FCM) are gated behind explicit opt-in toggles.
- **Sentry Crash & Performance**: `SentryInitProvider`, `SentryPerformanceProvider`.
- **Facebook AppEvents**: `FacebookInitProvider`.
- **Meta Analytics2 / OneFabric**: `FFAlarmUploadJobService`, `GooglePlayUploadService`, `AlarmBasedUploadService`, `Analytics2UploadService`, `LollipopUploadService`, `LollipopUploadSafeService`, `DelayedWorkerService`, `OneFabricUploadAlarmReceiver`, `HighPriUploadRetryReceiver`, `AnalyticsUploadAlarmReceiver`, `DelayedWorkerServiceReceiver`.
- **Crash Detectors & Dump Upload (Lacrima)**: `DumperUploadService`, `ExceptionsUploadService`, `ProfiloUploadService`, `ProtectedLockScreenBroadcastReceiver`, `PublicLockScreenBroadcastReceiver`, `SystemShutdownBootBroadcastReceiver`, `InternalShutdownBootBroadcastReceiver`, `SecureShutdownBootBroadcastReceiver`, `CrashLoop$LastState`.
- **Device-ID & Cross-App Identity**: `AccessLibraryContentProvider`, `AttributionIdProvider`, `InstallReferrerProvider`, `LastUsedTimestampProvider`, `FDIDLiteProvider`, `AsyncInstagramFDIDLiteProvider`, `AsyncInstagramPhoneIdProvider`, `UsdidValuesProvider`, `FirstPartyUserValuesLiteProvider`, `FirstPartyUserValuesLiteProviderV2`, `BarcelonaLiteContentProvider`, `AsyncFamilyAppsUserValuesProvider`, `FamilyAppsUserValuesProvider`, `FamilyAppsUserValuesLiteProvider`, `CrossSigningService`, `InstallReferrerFetchJobIntentService`, `GooglePlayInstallReferrerReceiver`, `InstagramPhoneIdRequestReceiver`, `PhoneIdRequestReceiver`, `CrossSigningBroadcastReceiver`.
- **AppsFlyer Attribution**: `PluginInfoContentProvider`, `AFJobSchedulerService`, `SingleInstallBroadcastReceiver`, `MultipleInstallBroadcastReceiver`.
- **Adjust Attribution**: `AdjustReferrerReceiver`.
- **Flurry & Branch Analytics**: `FlurryContentProvider`, `BranchInitProvider`.
- **Third-Party Ad & Engagement SDKs**: AudienceNetwork, Vungle, Fairtiq telemetry, AdMob (`MobileAdsInitProvider`, `AdService`), and mediation init providers (AppLovin, ironSource/LevelPlay, Mintegral, BidMachine) (`AudienceNetworkContentProvider`, `FacebookContentProvider`, `VungleProvider`, `StartupTimeProvider`, `TrackingServiceImpl`, `AppLovinInitProvider`, `FullscreenAdService`, `IronsourceLifecycleProvider`, `LevelPlayActivityLifecycleProvider`, `MBComponentLifecycleProvider`, `BidMachineInitProvider`).
- **Bandwidth-Sharing Monetization SDKs**: Honeygain (`MoneytiserService`, `NetworkStateReceiver`), Appsalt (`RewardService`, `BootReceiver`). Other Honeygain classes (ConfigSyncService, ConfigSyncJob, ConfigSyncAlarmTrigger, HttpManager, SeedDiscovery, BandwidthOptReporter) are not declared as components in analyzed manifests. Appsalt consent activities (`ConsentActivity`, `ConsentUiActivity`) are inert without their background services.
- **Ad SDK Startup Initializers (`androidx.startup`)**: `AdsSdkInitializer` (Unity Ads auto-init entry within `InitializationProvider`).
- **Push Notification Services & Receivers (optional)**: Meta Fbns (`FbnsService`, `InappFbnsService`), PushLite (`PushLiteFallbackJobService`, `PushLiteGCMJobService`, `PushLiteLollipopJobService`, `PushLiteFcmListenerService`, `PushLiteFirebaseMessagingService`), Firebase Cloud Messaging (`FirebaseMessagingService`, `FirebaseInstanceIdReceiver`), and Braze push receivers (`BrazePushReceiver`, `BrazeFlushPushDeliveryReceiver`).

### Configuration in Morphe Manager

- **Ad permissions (`revokePermissions`)**: Strips `com.google.android.gms.permission.AD_ID`, Android Privacy Sandbox permissions (`ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_CUSTOM_AUDIENCE`, `ACCESS_ADSERVICES_TOPICS`), install referrer permissions, plus residual `android.adservices.*` `<property>` / `<meta-data>` config entries inside `<application>` (Toggle, default: `true`). Removing the config is defense-in-depth and manifest hygiene, not a new block: once the permissions are gone the OS already rejects AdServices API calls. `<uses-library android:name="android.ext.adservices">` is deliberately never removed (missing shared library = `NoClassDefFoundError`).
- **Tracking providers (`disableProviders`)**: Sets `android:enabled="false"` on analytics and tracker ContentProviders (Toggle, default: `true`).
- **Tracking services (`disableServices`)**: Sets `android:enabled="false"` on telemetry upload, JobScheduler, DataTransport, and Firebase session background services (Toggle, default: `true`).
- **Tracking receivers (`disableReceivers`)**: Sets `android:enabled="false"` on campaign, install referrer, and measurement broadcast receivers (Adjust, AppsFlyer, AppMeasurement, DataTransport Alarm) (Toggle, default: `true`).
- **Opt-out flags (`injectOptOutFlags`)**: Injects declarative opt-out `<meta-data>` tags into `<application>` for Firebase Analytics, Crashlytics, Performance, Google Analytics, Sentry, AppsFlyer, and the Facebook SDK (`AutoLogAppEventsEnabled`, `AdvertiserIDCollectionEnabled`), and prunes pure telemetry Firebase discovery registrars (`Analytics`, `Perf`, `Sessions`, `Measurement`) (Toggle, default: `true`).
- **Firebase init (may break apps) (`disableFirebaseInit`)**: Sets `android:enabled="false"` on `FirebaseInitProvider` (Toggle, default: `false`). *Keep disabled if the target app relies on Firebase Core, Auth, or Cloud Messaging (FCM).*
- **Push services (breaks push!) (`disablePushServices`)**: Sets `android:enabled="false"` on Meta Fbns, PushLite, and Firebase Cloud Messaging services plus push receivers (`FirebaseInstanceIdReceiver`, Braze push receivers) and prunes push discovery registrars (`Installations`, `Transport`, `Iid`) (Toggle, default: `false`). *WARNING: this breaks push notifications; enable only to fully silence background push delivery.*
- **Google Analytics (`disableGoogleAnalytics`)**: Sets `android:enabled="false"` on legacy Google Analytics background services (`AnalyticsService`, `AnalyticsJobService`) and receivers (`AnalyticsReceiver`) (Toggle, default: `true`).
- **Meta analytics (`disableMetaAnalytics`)**: Sets `android:enabled="false"` on Meta Analytics2/OneFabric upload services, Instagram upload scheduler receiver, and deferred analytics worker components (Toggle, default: `true`).
- **Crash uploaders (`disableCrashDetectors`)**: Sets `android:enabled="false"` on Lacrima lock-screen/shutdown crash detectors, crash-loop state trackers, and background crash-dump upload services (Toggle, default: `true`).
- **Device ID (may break login!) (`disableDeviceIdProviders`)**: Sets `android:enabled="false"` on attribution, FDID/PhoneId/USDiD, and FamilyApps cross-app identity providers plus referrer and cross-signing components (Toggle, default: `false`). *WARNING: may break login, account switching, and deferred deep links; enable only to fully silence device-identity collection.*
- **Remote config (may break app!) (`disableRemoteConfig`)**: Prunes Firebase RemoteConfig, DynamicLoading, and AB testing (`Abt`) discovery registrars (Toggle, default: `false`). *WARNING: May break dynamic backend configuration and remote feature flags.*
- **Crashlytics (may break app!) (`disableCrashlyticsRegistrars`)**: Prunes Firebase Crashlytics discovery registrars (Toggle, default: `false`). *WARNING: Pruning breaks apps hard-requiring the Crashlytics component at startup.*
- **Ad auto-start (`disableAdStartupInitializers`)**: Removes ad SDK auto-init entries (`AdsSdkInitializer` / Unity Ads) from `androidx.startup.InitializationProvider` (Toggle, default: `false`). *WARNING: may break rewarded ads and ad-gated features; enable only to block SDK auto-initialization.*
- **ML Kit vision (breaks scanner!) (`disableMlKit`)**: Sets `android:enabled="false"` on `MlKitInitProvider` and `MlKitComponentDiscoveryService`, and prunes ML Kit vision registrars (barcode, face, text) (Toggle, default: `false`). *WARNING: breaks on-device barcode scanning, face detection, and text recognition.*

---

## 7. Remove Tracking Libraries (`universalNativeBinaryTrimmerPatch`)

The **`Remove Tracking Libraries`** patch inspects native architecture directories in `lib/**` (`lib/arm64-v8a/`, `lib/armeabi-v7a/`, `lib/x86_64/`, etc.) and performs byte-level in-situ zeroing (`writeBytes(byteArrayOf())`) on non-essential crash reporters, telemetry engines, debugging/profiling companions, and bandwidth-sharing monetization shared libraries (`.so`).

### 🛡️ In-Situ Zeroing vs. File Deletion
- **ZIP Central Directory Invariant**: In Morphe's patching pipeline, deleting native `.so` files from the resource tree can break APK alignment and trigger `UnsatisfiedLinkError` if the application's Java/Kotlin code contains strict class-level `System.loadLibrary(...)` calls without exception handlers.
- **Empty Stub Execution**: By replacing the payload of tracking `.so` files with a 0-byte stub directly in the APK, APK storage is fully reclaimed while eliminating native crash reporting background threads, memory dump scanners, and watchdog sidecars.
- **Eager Loading Warning**: Trimmed libraries are only safe if not loaded eagerly by bytecode. Pair with Block Tracking SDKs (`blockCrashReporters`) or verify no `System.loadLibrary` calls to avoid `UnsatisfiedLinkError` boot crashes. For Honeygain (`libhopmn.so`), zeroing is safe at boot because loader errors are caught, but `HopmnSrv.reload()` requires pairing with Block Trackers (Universal) service deactivation and Block Tracking SDKs (`blockBandwidthSharing`) to prevent runtime `UnsatisfiedLinkError` if the SDK runs.

### Targeted Native Libraries

- **Crash Reporting & Telemetry**: `libcrashlytics.so`, `libcrashlytics-trampoline.so`, `libsentry.so`, `libsentry-android.so`, `libbugly.so`, `libfirebase-crashlytics.so`, `libapp-measurement.so`, `libplcrashreporter.so`.
- **Debuggers & Profilers**: `libgwp-asan.so`, `libprofiler-service.so`, `libperfa.so`, `libperfa_arm.so`, `libperfa_arm64.so`, `libsimpleperf.so`, `libleaktracer.so`.
- **Bandwidth-Sharing Monetization**: `libhopmn.so`.

### Configuration in Morphe Manager

- **Crash reporters (`trimCrashReporters`)**: Replaces native crash reporting and telemetry `.so` binaries with 0-byte stubs (Toggle, default: `true`).
- **Debuggers (`trimDebugProfilers`)**: Replaces runtime profilers, memory leak detectors, and ASan instrumentation `.so` binaries with 0-byte stubs (Toggle, default: `true`).
- **Bandwidth-sharing SDKs (`trimBandwidthSharing`)**: Replaces Honeygain bandwidth monetization native library (`libhopmn.so`) with a 0-byte stub (Toggle, default: `true`).

---

## 8. Shrink WebP Images (`universalWebpOptimizerPatch`)

The **`Shrink WebP Images`** losslessly strips non-rendering metadata chunks (`EXIF`, `XMP `, `ICCP`) from WebP images located in `res/**` and `assets/**` across any Android application.

> [!NOTE]
> **Pipeline Scope**: In full pipeline mode (with resource decoding), optimization covers both `res/` and `assets/`.
> In raw-only pipeline mode (where resources are not decoded), `res/` is safely skipped and optimization applies exclusively to `assets/`.

### 🛡️ RFC 9649 / libwebp Bitstream Safety
- **Extended Header Recalculation**: WebP files using the extended `VP8X` chunk format store feature flags in byte 0 of their payload. When `EXIF`, `XMP `, or `ICCP` chunks are stripped, `Shrink WebP Images` updates the bitmask to clear the corresponding flag bits (`0x08` for EXIF, `0x04` for XMP, `0x20` for ICCP) while strictly preserving image dimensions, the alpha channel bit (`0x10`), and the animation flag (`0x02`).
- **Container Simplification**: If an extended WebP contains only a single visual frame (`VP8 ` lossy or `VP8L` lossless) and no alpha or animation data after metadata stripping, the patch downgrades the file to a standard simple WebP container (`RIFF....WEBPVP8 ...`), eliminating the unnecessary 18-byte `VP8X` header entirely.
- **Strict Bitstream Bounds**: Any malformed, truncated, or non-conforming WebP file where chunk offsets do not cleanly match the total file size is skipped untouched to prevent visual corruption.

### Configuration in Morphe Manager

- **Camera data (`stripExif`)**: Removes `EXIF` chunks containing camera metadata, GPS tags, timestamps, and device serials (Toggle, default: `true`).
- **Editing history (`stripXmp`)**: Removes `XMP ` chunks containing XML-based authoring and editing history (Toggle, default: `true`).
- **Color profiles (`stripIcc`)**: Removes embedded `ICCP` color profiles (Toggle, default: `false`).

---

## 9. Stop Background Sync (Universal) (`backgroundSyncPurgePatch`)

The **`Stop Background Sync (Universal)`** patch stops unneeded background wakeups, radio modem alarms, and persistent standby battery drain by removing startup permissions and disabling boot-triggered broadcast receivers and periodic WorkManager scheduler components in `AndroidManifest.xml`.

> [!NOTE]
> ### Eliminating Standby Battery Drain
> Many modern Android applications register broadcast receivers with `android.intent.action.BOOT_COMPLETED` and `android.intent.action.MY_PACKAGE_REPLACED`. Whenever the device boots or an app is updated, Android awakens the app process to reschedule background sync tasks, initialize databases, and ping remote servers. `Stop Background Sync (Universal)` eliminates this behavior entirely.

### Neutralized Components

- **Boot & Restart Receivers**: Receivers listening for `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `QUICKBOOT_POWERON`, `REBOOT`, `MY_PACKAGE_REPLACED`, `PACKAGE_REPLACED`, and `PACKAGE_RESTARTED`.
- **WorkManager & JobScheduler Components**: `SystemJobService`, `SystemAlarmService`, `RescheduleReceiver`, `ForceStopRunnable$BroadcastReceiver`, `ConstraintProxy`, and `DiagnosticsReceiver`.
- **DataTransport Scheduling**: `JobInfoSchedulerService`, `TransportBackendDiscovery`, `AlarmManagerSchedulerBroadcastReceiver`.

### Configuration in Morphe Manager

- **Strip RECEIVE_BOOT_COMPLETED Permission (`stripBootPermission`)**: Removes `android.permission.RECEIVE_BOOT_COMPLETED` and HTC/OEM quickboot permissions from `AndroidManifest.xml` (Toggle, default: `true`).
- **Disable Boot & Package Receivers (`disableBootReceivers`)**: Disables broadcast receivers registered for device startup, reboot, and app replacement events (Toggle, default: `true`).
- **Disable WorkManager & Job Schedulers (`disableWorkManager`)**: Disables WorkManager background services and constraint-checking broadcast receivers (Toggle, default: `true`).
- **Strip WAKE_LOCK Permission (`stripWakeLock`)**: Removes `android.permission.WAKE_LOCK` from `AndroidManifest.xml` (Toggle, default: `false`). *Keep disabled if the target application requires wake locks for continuous audio playback, video recording, screen-off background downloads (Brave), or foreground navigation.*

---

## 10. Strip Sensitive Permissions (Universal) (`universalPrivacyPermissionsPatch`)

The **`Strip Sensitive Permissions (Universal)`** patch selectively strips sensitive privacy, sensor, and hardware permissions from `AndroidManifest.xml` via modular boolean toggles. It enables strict isolation of host applications by removing permissions at the Android packaging level.

> [!NOTE]
> ### Packaging-Tier vs. Runtime Revocation
> In the Android platform architecture, revoking a permission in system settings causes `checkSelfPermission()` to return `PERMISSION_DENIED`. In contrast, completely stripping the `<uses-permission>` and `<uses-permission-sdk-23>` nodes from `AndroidManifest.xml` causes the Android OS to treat the permission as never requested.
> - **Silent Permission Denial**: When an application calls `requestPermissions()` for an unmanifested permission, Android automatically denies it without displaying the runtime permission request dialog.
> - **Zero Attack Surface**: Applications cannot be granted unmanifested permissions via ADB, device owner MDM profiles, or accessibility automation.

> [!IMPORTANT]
> ### Safety & Runtime Assumptions
> To prevent runtime `SecurityException` crashes in applications that invoke hardware APIs without error-handling wrappers, all toggles in `Strip Sensitive Permissions (Universal)` are **disabled (`false`) by default**. Enable only the specific permission groups you wish to isolate for your target application.

### Targeted Permission Groups

| Toggle (`booleanOption`) | Revoked Manifest Permissions |
| :--- | :--- |
| **Notifications** | `android.permission.POST_NOTIFICATIONS` |
| **Camera** | `android.permission.CAMERA` |
| **Microphone** | `android.permission.RECORD_AUDIO`, `android.permission.CAPTURE_AUDIO_OUTPUT` |
| **Photos & files** | `android.permission.READ_EXTERNAL_STORAGE`, `android.permission.WRITE_EXTERNAL_STORAGE`, `android.permission.MANAGE_EXTERNAL_STORAGE`, `android.permission.READ_MEDIA_IMAGES`, `android.permission.READ_MEDIA_VIDEO`, `android.permission.READ_MEDIA_AUDIO`, `android.permission.READ_MEDIA_VISUAL_USER_SELECTED`, `android.permission.ACCESS_MEDIA_LOCATION` |
| **Location** | `android.permission.ACCESS_FINE_LOCATION`, `android.permission.ACCESS_COARSE_LOCATION`, `android.permission.ACCESS_BACKGROUND_LOCATION` |
| **Contacts** | `android.permission.READ_CONTACTS`, `android.permission.WRITE_CONTACTS`, `android.permission.GET_ACCOUNTS` |
| **Calendar** | `android.permission.READ_CALENDAR`, `android.permission.WRITE_CALENDAR` |
| **Nearby devices** | `android.permission.BLUETOOTH_SCAN`, `android.permission.BLUETOOTH_CONNECT`, `android.permission.BLUETOOTH_ADVERTISE`, `android.permission.NEARBY_WIFI_DEVICES`, `android.permission.UWB_RANGING` |
| **Body sensors** | `android.permission.BODY_SENSORS`, `android.permission.BODY_SENSORS_BACKGROUND`, `android.permission.ACTIVITY_RECOGNITION` |
| **Installed apps** | `android.permission.QUERY_ALL_PACKAGES` |

### Configuration in Morphe Manager

- **Notifications (`stripNotifications`)**: Removes `POST_NOTIFICATIONS` (Android 13+) from `AndroidManifest.xml` (Toggle, default: `false`).
- **Camera (`stripCamera`)**: Removes `CAMERA` from `AndroidManifest.xml` (Toggle, default: `false`).
- **Microphone (`stripMicrophone`)**: Removes `RECORD_AUDIO` and audio capture permissions from `AndroidManifest.xml` (Toggle, default: `false`).
- **Photos & files (`stripMediaAndStorage`)**: Removes legacy external storage and Android 13+ granular media permissions from `AndroidManifest.xml` (Toggle, default: `false`).
- **Location (`stripLocation`)**: Removes `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, and `ACCESS_BACKGROUND_LOCATION` from `AndroidManifest.xml` (Toggle, default: `false`).
- **Contacts (`stripContacts`)**: Removes `READ_CONTACTS`, `WRITE_CONTACTS`, and `GET_ACCOUNTS` from `AndroidManifest.xml` (Toggle, default: `false`).
- **Calendar (`stripCalendar`)**: Removes `READ_CALENDAR` and `WRITE_CALENDAR` from `AndroidManifest.xml` (Toggle, default: `false`).
- **Nearby devices (`stripNearbyDevices`)**: Removes Bluetooth scan/connect/advertise and nearby Wi-Fi device permissions from `AndroidManifest.xml` (Toggle, default: `false`).
- **Body sensors (`stripSensors`)**: Removes `BODY_SENSORS`, `BODY_SENSORS_BACKGROUND`, and `ACTIVITY_RECOGNITION` from `AndroidManifest.xml` (Toggle, default: `false`).
- **Installed apps (`stripInstalledApps`)**: Removes `QUERY_ALL_PACKAGES` from `AndroidManifest.xml`. On Android 11+ with targetSdk 30 or higher, the app then only sees packages declared in its `<queries>` element (left untouched); apps targeting older SDKs are unaffected. May break launchers, app managers and share menus (Toggle, default: `false`).

---

## 11. Allow Screenshots (Universal) (`universalScreenshotProtectionBypassPatch`)

The **`Allow Screenshots (Universal)`** patch neutralizes `WindowManager.LayoutParams.FLAG_SECURE` (`0x2000`) across all windows, layout parameters, and `SurfaceView` instances, unlocks internal audio playback capture for screen recording on Android 10+, and suppresses native Android 14+ screenshot and screen recording detection callbacks.

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

## 12. Force Screen Timeout (`universalScreenTimeoutEnforcerPatch`)

The **`Force Screen Timeout`** patch forces the host application to respect the operating system's configured screen timeout (`SCREEN_OFF_TIMEOUT`) and sleep timers by neutralizing keepScreenOn view calls and stripping `FLAG_KEEP_SCREEN_ON` from windows and layout parameters.

### 🛡️ Low-Level Bytecode & Window Enforcement

1. **Bitwise Flag Masking**: For any call to `Window.addFlags(flags)`, `Window.setFlags(flags, mask)`, or direct write to `WindowManager.LayoutParams.flags`, the patch injects in-place bitwise masking (`and-int/lit16 vReg, vReg, -0x81`) immediately prior to execution. The mask `-0x81` (`0xffffff7f` sign-extended) deterministically clears bit 7 (`0x00000080` / `FLAG_KEEP_SCREEN_ON`) while preserving all other layout and window flags (`FLAG_FULLSCREEN`, `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS`, `FLAG_TRANSLUCENT_STATUS`, etc.) intact without modifying register allocations.
2. **View & SurfaceHolder Neutralization**: Invocations to `View.setKeepScreenOn(Z)V`, `SurfaceView.setKeepScreenOn(Z)V`, `TextureView.setKeepScreenOn(Z)V`, and `SurfaceHolder.setKeepScreenOn(Z)V` are cleanly replaced with `nop`. This ensures views retain their default Android state (`mKeepScreenOn = false`) and never inhibit the OS sleep timer.

### 🛡️ Plug & Play Operation

The patch operates without any manual configuration or boolean options (`default = false`). When enabled, it automatically executes the complete screen timeout enforcement pipeline across all bytecode components:
- Clears `FLAG_KEEP_SCREEN_ON` (`0x80`) on `Window.setFlags`, `Window.addFlags`, and `WindowManager.LayoutParams.flags`.
- Neutralizes `setKeepScreenOn(Z)V` calls across all UI views and surface holders.

---

## 13. Lock Screen Brightness (`universalScreenBrightnessGovernorPatch`)

The **`Lock Screen Brightness`** patch prevents applications from overriding device screen brightness (such as in-app brightness sliders, barcode/QR full-screen brightness, or window-level overrides) by neutralizing all direct writes to `WindowManager.LayoutParams.screenBrightness`.

> [!NOTE]
> **Hardware HDR Video Playback Scope**: Direct field writes to `WindowManager.LayoutParams.screenBrightness` only control window-level UI brightness overrides. Hardware HDR video brightness boosts (HDR10 / PQ / HLG) are driven at the display subsystem level by graphic buffer dataspaces (`BT2020_ITU_PQ`) passed to `SurfaceFlinger`. Disabling HDR video playback requires app-specific stream or codec governor patches (such as `Disable HDR Video Playback` in TikTok) rather than window layout parameters.

### 🛡️ Low-Level Bytecode & Window Enforcement

1. **Instruction Neutralization**: Scans all Dalvik bytecode instructions across the application for direct field store operations targeting `Landroid/view/WindowManager$LayoutParams;->screenBrightness:F` (`iput`). Each matching instruction is cleanly replaced with `nop`.
2. **Framework Default Invariance**: Because the Android OS framework initializes `WindowManager.LayoutParams.screenBrightness` to `BRIGHTNESS_OVERRIDE_NONE` (`-1.0f`), preventing any in-app write guarantees that the window's layout parameters retain `-1.0f` throughout the application lifecycle.
3. **OS-Level Control Guarantee**: The system WindowManager and DisplayManager continue to govern screen brightness according to the user's manual brightness setting and ambient light sensor, completely eliminating unwanted in-app brightness spikes without destabilizing registers or altering method control flow.

### 🛡️ Plug & Play Operation

The patch operates without any manual configuration or boolean options (`default = false`). When enabled in Morphe Manager or CLI, it automatically neutralizes all screen brightness override attempts across all classes in the target APK.

---

## 14. Block Tracking Servers (Universal) (`universalHostsBlockerPatch`)

The **`Block Tracking Servers (Universal)`** patch rewrites Dalvik `const-string` / `const-string/jumbo` URL and host literals whose host matches a user-supplied blocklist, replacing the blocked host in place with a sink IP (default `0.0.0.0`). The blocklist file is read at patch time from the patching machine, so daily DNS-list updates (e.g. Hagezi `native.tiktok`) apply on re-patch without a patch release and without embedding third-party lists in this repository.

### Configuration in Morphe Manager

- **Blocklist file (`hostsFile`)**: File-picker path to a hosts or plain-domain blocklist on the patching machine (e.g. Hagezi `native.tiktok-onlydomains.txt`). Empty by default: the patch logs `Skipped` and changes nothing until a file is selected.
- **Replace with (`sinkIp`)**: IPv4 replacing blocked hosts inside literals (`https://log.example.com/v1` becomes `https://0.0.0.0/v1`). Default: `0.0.0.0`.
- **Include subdomains (`matchSubdomains`)**: When enabled (default `true`), entry `example.com` also matches `a.example.com`.

### Supported blocklist line formats

Plain domains (`log.example.com`), classic hosts lines (`0.0.0.0 log.example.com`), full URLs (`https://log.example.com/v1`), Hagezi wildcard lines (`*.log.example.com`), and adblock-style rules (`||log.example.com^`). Inline `#` comments and `@@` allowlist lines are ignored. Reserved hosts (`localhost`, `127.0.0.1`, `0.0.0.0`) are never blocked.

### Scope & limits

- Rewrites Dex string literals only. Native `.so` endpoint strings (e.g. Brave `libchrome.so` telemetry hosts), dynamically assembled hosts (`StringBuilder` concatenation), encrypted configs, raw IPs, and DoH flows are out of scope.
- A curated telemetry/ads-only list is required: blocking functional hosts breaks the app. For TikTok, prefer the `log/mon/mcs/mssdk/analytics` subset and leave `frontier/api/stream/open` untouched.

> [!WARNING]
> Do not apply Block Tracking Servers (Universal) on a fresh TikTok install where no login has happened yet: blocking telemetry/ads hosts before the first login trips server-side rate limiting ('too many attempts' errors) during login/registration. Log in first and patch afterwards, or use a minimal curated list.

---

## 15. Block Tracking SDKs (`universalSdkBlockerPatch`)

The **`Block Tracking SDKs`** patch neutralizes pervasive third-party APM, crash reporting, analytics, attribution, session replay, location tracking, bandwidth-sharing monetization, and push engagement SDKs directly at the Dalvik bytecode level (`classes*.dex`). It serves as the runtime execution counterpart to **`Block Trackers (Universal)`** (manifest layer). On repo-maintained apps, do not stack this patch on top of dedicated telemetry patches; see [Do Not Stack Universals on Maintained Apps](#do-not-stack-universals-on-maintained-apps).

Tracker catalog derived from the Exodus Privacy tracker database (https://exodus-privacy.eu.org), database contents under ODbL 1.0 / DbCL 1.0. Only Analytics, Crash reporting and Profiling category SDKs are covered; advertisement and functional SDKs are excluded or kept behind disabled-by-default toggles.

> [!NOTE]
> ### Runtime Early Return-Void Neutralization
> While `Block Trackers (Universal)` operates at packaging time by revoking manifest permissions, disabling `ContentProvider` / background `Service` components, and injecting opt-out `<meta-data>`, applications often initialize SDKs programmatically inside `Application.onCreate()` or activity lifecycles.
>
> `Block Tracking SDKs` neutralizes programmatic initialization and telemetry dispatch by scanning for known SDK package prefixes and conservative method signatures (such as `init`, `initialize`, `start`, `recordMetric`, `trackEvent`, `reportException`), prepending a zero-register Dalvik `return-void` instruction (`0x0e`) at instruction index 0 of matched void methods. The method returns immediately upon invocation before executing background threads, socket connections, or device profiling loops.

### Neutralized SDKs by Category

- **Application Performance Monitoring (APM)**: New Relic (`Lcom/newrelic`), Datadog (`Lcom/datadog`), Dynatrace (`Lcom/dynatrace`).
- **Crash Reporting**: Raygun (`Lcom/mindscapehq`), Shake (`Lcom/shakebugs`), Embrace (`Lio/embrace`), Splunk Mint (`Lcom/splunk`), Microsoft App Center (`Lcom/microsoft/appcenter`), OpenTelemetry (`Lio/opentelemetry`), ACRA (`Lorg/acra`), Sentry (`Lio/sentry`), Bugsnag (`Lcom/bugsnag`), Crashlytics (legacy `Lcom/crashlytics/android` and modern `Lcom/google/firebase/crashlytics`), Fabric (`Lio/fabric/sdk`), Instabug (`Lcom/instabug`), Countly (`Lly/count/android`), HockeyApp (`Lnet/hockeyapp`).
- **Analytics**: Firebase Analytics (`Lcom/google/firebase/analytics`; consent and data-clear setters preserved to prevent freezing granted permissions), Matomo (`Lorg/matomo`), Leanplum (`Lcom/leanplum`), Localytics (`Lcom/localytics`), WebEngage (`Lcom/webengage`), PostHog (`Lcom/posthog`), MoEngage (`Lcom/moengage`), Snowplow (`Lcom/snowplowanalytics`), mParticle (`Lcom/mparticle`), Treasure Data (`Lcom/treasuredata`), Huawei Analytics (`Lcom/huawei/hms/analytics`; consent and data-clear setters preserved), Yandex Metrica (`Lcom/yandex/metrica`), Facebook AppEvents (`Lcom/facebook/appevents`), Sensors Analytics (`Lcom/sensorsdata`).
- **Attribution & Engagement**: AppsFlyer (`Lcom/appsflyer`), Adjust (`Lcom/adjust`; incl. `onCreate` (v4) / `initSdk` (v5), scoped to Adjust only), Amplitude (`Lcom/amplitude`; incl. `identify` / `groupIdentify` / `setGroup`), Mixpanel (`Lcom/mixpanel`; incl. `identify` / `alias`), CleverTap (`Lcom/clevertap`; incl. `pushEvent` / `onUserLogin` / `pushProfile`), Segment (`Lcom/segment`), Branch (`Lio/branch`, `Lcom/branch`; non-void `init` intentionally skipped — void-only design prevents NPEs), Singular (`Lcom/singular`; partial coverage of void setup and referrer methods; boolean public API out of scope), Kochava (`Lcom/kochava`), Tenjin (`Lcom/tenjin`), Unity Analytics (`Lcom/unity3d/services/analytics` + `Lcom/unity3d/analytics`; incl. static `Transaction`), Flurry (`Lcom/flurry`), GameAnalytics (`Lcom/gameanalytics`; incl. `initializeWithGameKey` / `addDesignEvent`). Non-void initializers (`Branch.init`, `FirebaseApp.initializeApp`) are intentionally skipped: stubbing object-returning methods with null risks NPEs at call sites. Chainable instance `track` / `identify` / `groupIdentify` / `setGroup` methods whose return type is their own defining class are hooked with `return-object p0` (type-correct, never null); SDK classes renamed by app obfuscation fall outside the package prefix and are not covered. Any other non-void return stays out of scope unless hand-reviewed per method. Kotlin interface default-method bridges (`$DefaultImpls` classes) are never hooked: they carry plugin wiring such as Segment's `Plugin.setup`, and stubbing them crashes the app at launch.
- **Session Replay**: UXCam (`Lcom/uxcam`), Smartlook (`Lcom/smartlook`), FullStory (`Lcom/fullstory`), Contentsquare (`Lcom/contentsquare`), Bugsee (`Lcom/bugsee`).
- **Location & Beacon Tracking**: Radar (`Lio/radar`), Gimbal (`Lcom/gimbal`), Estimote (`Lcom/estimote`).
- **Legacy Google Analytics**: Pre-Firebase Google Analytics v4 / GMS Analytics (`Lcom/google/analytics`, `Lcom/google/android/gms/analytics`).
- **Push Engagement**: OneSignal (`Lcom/onesignal`), Airship (`Lcom/urbanairship`), Braze (`Lcom/braze`, `Lcom/appboy`).
- **Bandwidth-Sharing Monetization**: Honeygain (`Lio/hopmonsdk`), Appsalt (`Lcom/appsalt`), EarnFM (`Lcom/fleet`). Manifest component deactivation (`Block Trackers (Universal)`) is the primary kill; bytecode hooks provide defense-in-depth on cold-start entry points. Honeygain entry points return non-void objects or booleans (`create`, `getInstance`, `builder`, `start()Z`), while `startWithConsent`/`showConsent` trigger consent dialog UI and `stop`/`resetConsent` invert direction; with zero safe void start methods, Honeygain carries empty method sets to prevent unsafe stubbing. Appsalt hooks safe void static entry points (`initialize`, `start`, `optIn`, `requestConsent`); non-void getters and direction-inverting methods (`stop`, `optOut`) are excluded. EarnFM (`com.fleet.FleetSdk`) carries no direct bytecode hooks (entries are constructors, non-void booleans `initialize`/`startSdk`/`stopSdk`/`destroyServer`/`storeConsent`/`isInitialized`, non-void int `getActivePort`, config setter `setStoreLogs`, or internal update callback `_performVersionCheck`) and declares no manifest components; it carries empty method sets for label registration without hook risk. It is neutralized by (a) the Appsalt opt-in cascade on fresh installs (FleetSdk is instantiated and started only when Appsalt reports opted-in; a pre-existing opt-in persisted in `appsalt_prefs` requires a one-time in-app opt-out or app data clear), and (b) blocking CDN download endpoints (`https://cdn.earn.fm/native_libs_v2` and fallback `https://cdn.earnfm.fo/native_libs_v2`) via user host blocklists.

### Configuration in Morphe Manager

- **Performance monitors (`blockApm`)**: Neutralize New Relic, Datadog, and Dynatrace initialization, metric recording, and HTTP transaction tracing methods (Toggle, default: `true`).
- **Crash reporters (`blockCrashReporters`)**: Neutralize Sentry, Bugsnag, Crashlytics (legacy and Firebase), Fabric, Raygun, Shake, Embrace, Splunk Mint, App Center, OpenTelemetry, ACRA, Instabug, Countly, and HockeyApp initialization and exception reporting methods (Toggle, default: `true`).
- **Analytics (`blockAnalytics`)**: Neutralize Firebase Analytics, Matomo, Leanplum, Localytics, WebEngage, PostHog, MoEngage, Snowplow, mParticle, Treasure Data, Huawei Analytics, Yandex Metrica, Facebook AppEvents, and Sensors Analytics event tracking, capture, identification, and session upload methods (Toggle, default: `true`).
- **Ad attribution (`blockAttribution`)**: Neutralize AppsFlyer (incl. instance `start`; install/session dispatch stops, but a deferred-conversion listener never fires — an app splash screen waiting for conversion without timeout may hang), Adjust (incl. `onCreate(AdjustConfig)` v4 and `initSdk(AdjustConfig)` v5), Amplitude (incl. `identify` / `groupIdentify` / `setGroup`), Mixpanel (incl. `identify` / `alias`), CleverTap (incl. `pushEvent` / `onUserLogin` / `pushProfile`), Segment, Branch, Singular, Kochava, Tenjin, Unity Analytics (incl. `Transaction` under `com.unity3d/analytics`; `com.unity3d/services/analytics` uses `on*` gameplay events, coverage unverified), Flurry (mostly manifest-level: `FlurryAgent.logEvent` returns a status object and `Builder.build` is pending verification), and GameAnalytics (incl. `initializeWithGameKey` / `addDesignEvent`) conversion, attribution, and event dispatch methods (Toggle, default: `true`).
- **SDKs to keep (`excludedSdks`)**: Comma-separated SDK labels to leave untouched (e.g. `Adjust, Branch`). Empty by default. Unknown labels are reported in the patch log. Covers per-SDK control without one toggle per SDK; no generic heuristic matching.
- **Screen recorders (`blockSessionReplay`)**: Neutralize UXCam, Smartlook, FullStory, Contentsquare, and Bugsee screen and session recording methods (Toggle, default: `true`).
- **Location trackers (`blockLocationTrackers`)**: Neutralize Radar, Gimbal, and Estimote beacon and location tracking methods; note OS location permission controls remain the primary gate (Toggle, default: `true`).
- **Old Google Analytics (`blockLegacyAnalytics`)**: Neutralize pre-Firebase Google Analytics tracking, hit dispatching, and activity reporting methods across `com.google.analytics` and `com.google.android.gms.analytics` (Toggle, default: `true`).
- **Bandwidth-sharing SDKs (`blockBandwidthSharing`)**: Neutralize Honeygain and Appsalt bandwidth monetization initialization and start methods (Toggle, default: `true`).
- **Push SDKs (breaks push!) (`blockPushEngagement`)**: Neutralize OneSignal, Airship, and Braze push engagement and tagging SDKs (Toggle, default: `false`). *WARNING: this breaks push notifications; enable only to fully silence background push engagement SDK runtimes.*

---

## 16. Disable Play Store updates (`universalDisablePlayStoreUpdatesPatch`)

The **`Disable Play Store updates`** patch sets the manifest `versionCode` to the maximum allowed value (`2147483647` / `Int.MAX_VALUE`), detaching the application from Google Play Store update checks so the store never offers automatic or manual updates. At the bytecode layer, it intercepts `PackageInfo.versionCode` field reads and `PackageInfo.getLongVersionCode()` calls across application classes, redirecting them to runtime extension helpers that return the app's original version code recorded from `AndroidManifest.xml` before patching.

> [!NOTE]
> ### In-App Version Spoofing & Compatibility
> Setting `versionCode` to `2147483647` in `AndroidManifest.xml` prevents the Play Store from finding higher version updates. However, many applications inspect their own version code at runtime and transmit it in API request headers or use it in local migration checks.
>
> If an app reads the maximum version code directly, backend APIs may reject client requests or crash. To prevent breakage, this patch scans application bytecode in a single pass for `PackageInfo.versionCode` reads and `PackageInfo.getLongVersionCode()` calls, replacing them with calls to `DisablePlayStoreUpdatesPatch` in `extension.mpe`. These helpers return the original version code (preserving `versionCodeMajor` in the upper 32 bits for long version checks) whenever the version code equals `Int.MAX_VALUE`.

### Scope & Compatibility Notes

- **Opt-in by default**: Disabled by default (`default = false`) because it changes package update behavior.
- **Root mounting incompatibility**: Does not work if the application is installed via root mount.
- **Universal applicability**: Operates across any Android application without hardcoded class fingerprints or app-specific targets.

---

## 17. Spoof Reinstall IDs (`universalSpoofReinstallIdsPatch`)

The **`Spoof Reinstall IDs`** patch neutralizes persistent installation and app-set tracking identifiers by replacing identifier getters directly at the Dalvik bytecode level (`classes*.dex`) with neutral constants. This prevents applications and integrated analytics libraries from correlating reinstalls or linking activity across applications from the same developer family.

> [!NOTE]
> ### Identifier Constant Folding
> Applications use identifier frameworks (such as Google Play Services AppSet and Firebase Instance ID) to correlate user accounts and track repeat installations even after application data and cache have been wiped.
>
> `Spoof Reinstall IDs` intercepts identifier getters (`getId` and `getScope`) and folds their entire method bodies into constant returns via whole-body bytecode replacement. By default, app-set identifiers return a zeroed UUID (`00000000-0000-0000-0000-000000000000`), install identifiers return a neutral zero sequence (`0000000000000000000000`), and scope queries report app-scoped isolation (`1`) rather than developer-scoped cross-app tracking (`2`).

### Configuration in Morphe Manager

- **App set ID (`appSetId`)**: Fixed identifier reported when querying the app-set ID (Text, default: `00000000-0000-0000-0000-000000000000`).
- **Install ID (`installId`)**: Fixed identifier reported for installation and instance ID lookups (Text, default: `0000000000000000000000`).
- **Scope to single app (`spoofScope`)**: Report app-set ID scope as single-application (`1`) instead of developer-wide (`2`) (Toggle, default: `true`).

---

## 18. Silence System Logs (`universalSilenceSystemLogsPatch`)

The **`Silence System Logs`** patch neutralizes diagnostic logging invocations targeting `android.util.Log` directly at Dalvik bytecode call sites (`classes*.dex`). This stops applications from spamming system log buffers and prevents sensitive identifiers, request URLs, and device telemetry from leaking into Android logcat buffers accessible to other applications with system log permissions.

> [!NOTE]
> ### Log Call Site Neutralization
> Production Android applications often retain verbose debug and diagnostic logging statements (`Log.d`, `Log.v`, `Log.i`, `Log.w`, `Log.e`, `Log.wtf`, and `Log.println`) across their codebase and embedded third-party libraries.
>
> `Silence System Logs` executes a single union pass across application bytecode, identifying logging invocations matching the configured verbosity threshold. The patch safely replaces the invoke instruction with `nop` (or neutral zero constants when the caller captures the returned integer value via `move-result`), eliminating log message construction overhead and logcat buffer emission without triggering bytecode verification errors.

### Configuration in Morphe Manager

- **Log level (`logLevel`)**: Select which log severity levels to silence: `all` (silences all debug, verbose, info, warning, error, wtf, and println calls; default), `debug_verbose` (silences only debug and verbose messages), or `no_errors` (silences debug, verbose, info, and warning logs while preserving error entries) (Dropdown, default: `all`).

---

## 19. Disable Browser File Associations (`universalBrowserAssociationPatch`)

The **`Disable Browser File Associations`** patch stops an application from appearing in the Android browser app chooser for online files by pruning web-link intent filters in `AndroidManifest.xml`. It pairs with `Universal Offline Mode` for apps that should neither reach the network nor be reachable from it.

### 🛡️ Surgical Filter Pruning

For every `activity`, `activity-alias`, `service` and `receiver`, each `intent-filter` combining `android.intent.action.VIEW` + `android.intent.category.BROWSABLE` with `http`/`https` `<data>` entries is rewritten:

1. **Web Scheme Stripping**: `http`/`https` `<data>` nodes are removed (scheme match is case-insensitive). Entries with local schemes (`file`, `content`) or other matchers are strictly preserved.
2. **Emptied Filter Removal**: A filter left with no `<data>` nodes whose only action is `VIEW` existed purely for web links and is dropped entirely. Leaving a dataless `VIEW` + `BROWSABLE` filter would match every `VIEW` intent, so removal is the safe default.
3. **Untouched by Design**: Launcher entries (`MAIN`), non-`BROWSABLE` filters, and filters without web schemes are never modified.

### Configuration in Morphe Manager

- **Strip HTTP/HTTPS Web Schemes (`stripWebSchemes`)**: Remove `http`/`https` data entries from `VIEW` + `BROWSABLE` intent filters (Toggle, default: `true`). Disable to run a no-op pass.
- **Remove Emptied Filters (`removeEmptiedFilters`)**: Drop filters left dataless with only `VIEW` actions (Toggle, default: `true`). Disable to keep the emptied filter skeleton.

> [!WARNING]
> Do not apply to applications that must open web links themselves (web browsers such as Brave, readers, link aggregators): stripping their `BROWSABLE` filters breaks opening links from other apps.

