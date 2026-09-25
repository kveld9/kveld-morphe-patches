<p align="center">
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases/latest"><img src="https://img.shields.io/github/v/release/kveld9/kveld-morphe-patches?color=7928CA&label=Release&logo=github&style=flat-square" alt="Latest Release" /></a>
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases"><img src="https://img.shields.io/github/downloads/kveld9/kveld-morphe-patches/total?style=flat-square&logo=github" alt="Total Downloads" /></a>
  <img src="https://img.shields.io/badge/Runtime-Morphe_Patcher_1.8.0-8A2BE2?style=flat-square" alt="Runtime" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue?style=flat-square" alt="License" />
</p>

<h1 align="center">🔮 Morphe Patches</h1>

<p align="center">
  Modular bytecode, resource, and native patch suite for <b>Brave Browser</b>, <b>Vivaldi Browser</b>, <b>Gboard Lite</b>, <b>Hevy</b>, <b>TikTok</b>, <b>NokoPrint</b>, and <b>Xiaomi Earbuds</b> on Android using the <b><a href="https://morphe.software">Morphe</a></b> patcher framework.
</p>

<p align="center">
  <a href="https://morphe.software/add-source?github=kveld9/kveld-morphe-patches"><img src="https://img.shields.io/badge/Morphe_Manager-Add_Patch_Source-8A2BE2?style=for-the-badge&logo=android&logoColor=white" alt="Add Source to Morphe Manager" /></a>
  &nbsp;&nbsp;
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases/latest"><img src="https://img.shields.io/badge/Direct_Download-Get_.MPP_Bundle-0070F3?style=for-the-badge&logo=github&logoColor=white" alt="Download Latest Release" /></a>
  &nbsp;&nbsp;
  <a href="https://t.me/kveldmorphe"><img src="https://img.shields.io/badge/Telegram-Official_Support-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white" alt="Official Telegram Support Group" /></a>
</p>

---

## ⚡ Key Advantages

- **Zero Runtime Overhead**: Direct compile-time Dalvik bytecode manipulation, XML transformation, and native ELF patching without resident background daemons, proxy servers, or Xposed frameworks.
- **Deep Telemetry Neutralization**: Neutralizes tracking, diagnostic pings, crash reporting, and analytics at method call sites (Google Primes, Chromium UMA, P3A, ByteDance AppLog, Adjust, Firebase, Sentry) instead of fragile network-level blackholing.
- **Aggressive Asset Trimming**: Strips unneeded companion `.so` binaries, unused font families, localized resource bundles, and promotional assets to significantly reduce APK size and memory consumption.
- **Rootless & Standalone**: Operates directly on userland APKs and APKM split bundles; no Magisk, KernelSU, or root privileges required.
- **Strict Upstream Parity**: Enforces a strict single-version invariant targeting latest stable upstream releases with exact AST and fingerprint assertions.

---

## 🎯 Supported Targets & Downloads

> [!TIP]
> For CPU architecture guidelines (ARM64 vs 32-bit ARMv7a) and variant selection rationales, consult the [Compatibility Guide](docs/compatibility.md).

| Application | Package ID | Target Version | Architecture / Variant | Download Source | Complete Guide |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Brave Browser** | `com.brave.browser` | `1.96.59` | `arm64-v8a`<br>`armeabi-v7a` (Monolithic) | [ARM64](https://github.com/brave/brave-browser/releases/download/v1.96.59/Bravemonoarm64.apk) · [ARM32](https://github.com/brave/brave-browser/releases/download/v1.96.59/BraveMonoarm.apk) | [Brave Guide](docs/apps/brave.md) |
| **Gboard Lite** | `com.google.android.inputmethod.latin` | `18.2.4.969776716` | `arm64-v8a`<br>`armeabi-v7a` (nodpi) | [APKMirror](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-2-4-969776716-release/) | [Gboard Lite Guide](docs/apps/gboard.md) |
| **Hevy** | `com.hevy` | `3.1.14` | `arm64-v8a` (APKM Bundle) | [APKMirror](https://www.apkmirror.com/apk/hevy-gym-workout-tracker/hevy-gym-log-workout-tracker/hevy-gym-log-workout-tracker-3-1-14-release/) | [Hevy Guide](docs/apps/hevy.md) |
| **NokoPrint** | `com.nokoprint` | `5.28.4` | Universal (nodpi) | [APKPure](https://d.apkpure.com/b/XAPK/com.nokoprint?versionCode=52804) | [NokoPrint Guide](docs/apps/nokoprint.md) |
| **TikTok** | `com.zhiliaoapp.musically`<br>`com.ss.android.ugc.trill` | `47.0.3` | `arm64-v8a` (nodpi) | [Global](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-47-0-3-release/) · [Asia](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/tiktok-47-0-3-2-release/) | [TikTok Guide](docs/apps/tiktok.md) |
| **Vivaldi Browser** | `com.vivaldi.browser` | `8.2.4147.93` | `arm64-v8a` (APKM Bundle) | [APKMirror](https://www.apkmirror.com/apk/vivaldi-technologies/vivaldi-browser-beta/vivaldi-browser-fast-safe-8-2-4147-93-release/) | [Vivaldi Guide](docs/apps/vivaldi.md) |
| **Xiaomi Earbuds** | `com.mi.earphone` | `1.38.0i` | Universal (XAPK Bundle) | [APKPure](https://d.apkpure.com/b/XAPK/com.mi.earphone?versionCode=138000) | [Xiaomi Earbuds Guide](docs/apps/xiaomi-earbuds.md) |

---

## 💊 Patch Catalog

<!-- PATCHES_START -->
<details>
<summary>Gboard Lite&nbsp;&nbsp;•&nbsp;&nbsp;<b>25 patches</b></summary>
<br>

**Supported versions:**

| 18.2.4.969776716-lite_beta-arm64-v8a | 18.2.4.969776716-lite_beta-armeabi-v7a |
| :---: | :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Add AMOLED Theme** | Adds a selectable Pure Black AMOLED theme using Gboard's native color_black theme package, without replacing Dark, Light, System Auto, or Dynamic Color. |  |
| **Allow Modified APK** | Bypasses internal signature check to allow custom APK execution. |  |
| **Block Telemetry** | Disables background metrics dispatch, event logging, daily pings, and crash reporting. |  |
| **Clipboard Enhancements** | Extends unpinned clipboard history retention duration, raises the maximum number of displayed unpinned clips, and allows customizing keyboard grid columns. | • Unpinned clip limit<br>• Retention time limit (hours)<br>• Clipboard grid columns |
| **Clone Gboard** | Changes the package name by appending a dot and custom suffix (defaults to 'clone') to allow installing Gboard alongside the original application. | • Package name suffix |
| **Disable Diagnostics** | Disables Google's diagnostic and recovery telemetry. |  |
| **Disable Google Primes** | Neutralizes Google Primes performance profiling, jank monitoring, native crash sidecars, and background telemetry threads. |  |
| **Disable MDD Background Sync** | Neutralizes Mobile Data Download (MDD) periodic background synchronization, automated polling, and prefetch worker tasks while preserving on-demand and user-requested downloads. |  |
| **Disable Remote Configuration** | Disables periodic remote experiment flag synchronization and background updates. |  |
| **Disable Superpacks Eager Sync** | Neutralizes eager background Superpacks synchronization during application startup, eliminating unnecessary disk sweeps and network polling while preserving on-demand pack downloads. |  |
| **Disable Tenor Share Tracking** | Disables Tenor GIF selection and share tracking telemetry. |  |
| **Disable WorkManager** | Neutralizes background WorkManager initialization, database creation, and periodic maintenance workers. |  |
| **Enable Access Points Menu Redesign** | Enables the redesigned access points menu bar and customization panel (Panel V2). |  |
| **Enable Bluetooth Microphone** | Unlocks the 'Use Bluetooth microphone' setting under Voice typing preferences. |  |
| **Enable Cursor Trackpad** | Enables 2D trackpad cursor navigation and cursor lock mode by holding the spacebar. |  |
| **Enable Dismiss Suggestions Button** | Adds a close button (X) to dismiss proactive suggestions on the suggestion bar. |  |
| **Enable Emoji Scale Setting** | Unlocks the emoji size scaling setting in Gboard preferences. |  |
| **Enable Grammar Checker** | Unlocks Grammar check and Smart Compose / inline suggestions settings under Text correction preferences. |  |
| **Enable Key Shape Selection** | Enables the key border shape selection UI (Default, Semi-rounded, Round) in theme customization. |  |
| **Force Incognito Mode** | Forces Gboard to always operate in incognito mode (disabling personalized learning and persistent input logging) while keeping clipboard functionality enabled. |  |
| **Hardened Intent Security** | Enables Gboard internal external intent protection against unauthorized intent hijacking. |  |
| **Offline Only** | Completely isolates Gboard from the network by revoking network permissions, neutralizing HTTP clients (Cronet, OkHttp, Superpacks), and spoofing offline status. | • Strip Contacts Permission |
| **Phenotype Flag Resilience** | Neutralizes Phenotype flag registration conflicts to allow runtime flag overrides without crashes. |  |
| **Resource Slimmer** | Strips embedded third-party license text, onboarding tutorial Lottie animations, promotional GIFs, and APK root metadata/junk files. |  |
| **Top Toolbar Item Count** | Customizes the maximum number of access point icons displayed on the top toolbar. | • Toolbar item count |

</details>

<details>
<summary>TikTok&nbsp;&nbsp;•&nbsp;&nbsp;<b>49 patches</b></summary>
<br>

**Supported versions:**

| 47.0.3 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Always Show Publish Date** | Forces video publish/upload date to remain visible in video author information across all feed types. |  |
| **Auto-Pause First Video** | Automatically pauses the first video when opening TikTok, allowing the application to finish background initialization and preventing playback lag. |  |
| **Auto-Translate Comments** | Automatically translates comments into your preferred language using TikTok's native translation engine. |  |
| **Bypass Mandatory Login** | Neutralizes mandatory login walls, dynamic regional forced login gates, and guest browsing restrictions. |  |
| **Clean Share URL** | Strips tracking parameters, user IDs, device fingerprints, and marketing tokens from shared TikTok links. |  |
| **Client-Side AI & Behavioral Profiling Governor** | Neutralizes on-device machine learning inference (Pitaya), Tako AI chatbot entry points and icons, and AI smart search suggestion clutter. |  |
| **Copy Comments Without Username** | Copies only the comment text without prepending the author username. |  |
| **Core Asset De-bloat** | Strips embedded Microblink/FinTech card scanner models, Pitaya AI & ByteNN LLM engines, C2PA origin verification, DLNA cast scanners, and redundant non-Latin fonts to save APK space. |  |
| **Custom Offline Videos Limit** | Customizes the maximum number of videos available for offline download caching. | • Custom Offline Videos Limit |
| **Custom Share Sheet** | Customizes and simplifies the native TikTok share sheet by removing third-party app bloat, hiding specified apps or actions, and optionally suppressing the friends/contacts direct message row. | • Simplify Share Sheet<br>• Hide Friends Suggestion Row<br>• Hide Specific Apps<br>• Hide Specific Actions |
| **Device Privacy Guard** | Neutralizes invasive runtime permissions (contacts sync, location tracking, nearby devices), advertising ID profiling, background clipboard snooping routines, motion sensor profiling, and screenshot/recording detection to protect user data. |  |
| **Disable Comment Suggested Emojis** | Removes the horizontal bar of suggested quick emojis displayed above the comment input box. |  |
| **Disable Double Tap to Like** | Disables the double tap gesture to like videos in the feed, preventing accidental likes while scrolling or pausing. Videos can still be liked using the like button. |  |
| **Disable Profile Photo LIVE Status** | Removes the pulsing LIVE ring and badge from creator avatars in the feed and ensures clicking navigates strictly to the user profile instead of launching the live stream. |  |
| **Disable Push Notifications** | Neutralizes background push notification tasks and persistent socket wake locks to eliminate background battery drain. |  |
| **Disable Search History Recording** | Prevents search queries and keywords from being recorded in local history, databases, and analytics stores. |  |
| **Disable Story Feed Indicators** | Removes the top-center story drop-down indicator pill (e.g. '1 Story') and creator profile photo story rings from feed videos, ensuring avatar photos remain clean. |  |
| **Disable Watch History Recording** | Prevents viewed videos from being recorded in account watch history, playback duration stores, and local history caches. |  |
| **Display Refresh Rate Governor** | Forces TikTok to run at peak display refresh rate (120Hz/90Hz/60Hz) and neutralizes video playback framerate downclocking routines. | • Target Refresh Rate |
| **Enable Voice Comments** | Forces the native voice comment recording button in comment input bars, bypassing regional rollout restrictions and remote server blocks. |  |
| **Feed Ad Blocker** | Removes sponsored advertisements, brand promotions, and promotional audio from the For You and Following feeds. |  |
| **Feed Bloat & Distraction Blocker** | Removes non-video clutter and floating ad widgets from the For You, Following, and Friends feeds, including Touchpoint Rewards pendants, floating ad stickers, suggested friend cards, mini-games, CapCut/template creation prompts, memories ('On This Day'), surveys, and mini-drama paywalls. |  |
| **Feed Live Stream Blocker** | Removes live stream broadcast cards and live recommendations from the For You and Following feeds. |  |
| **Fix Google Login** | Restores Google account sign-in after patching by forcing fallback to Web-based OAuth when Google Play Services rejects the modified APK signature. |  |
| **Force Auto-Scroll** | Forces the activation of the native video auto-scroll experiment flag for accounts and regions that lack it due to A/B testing. |  |
| **Hide AI-Generated Content** | Filters and skips videos tagged with native AI-generated metadata, C2PA content credentials, or creator AI disclosure tags across the For You, Following, and Friends feeds. |  |
| **Hide Feed Search Bar** | Removes the search suggestion pill and trending bar ('Search · <keyword>') from the bottom of feed videos, providing a clean viewing area without search distractions. |  |
| **Hide Popular Lives In Search** | Removes the Popular LIVEs recommendation card and live stream broadcasts from the search discovery page. |  |
| **Hide Profile Photo Follow Button** | Hides the plus (+) follow badge on creator profile avatars in the feed and disables its touch interaction. |  |
| **Hide STEM and Community Tabs** | Removes the STEM and Topics (Community / Explore) tabs from the top navigation feed strip. |  |
| **Hide Suggested Searches** | Removes the suggested search keywords section ('You may like' / 'Search suggestions') from the search discovery page. |  |
| **Hide TikTok Shop & Mall** | Removes product showcase badges, shopping cart tags, and the TikTok Shop / Mall tab from navigation bars and video posts. |  |
| **Hide Top-Left LIVE Button** | Removes the top-left LIVE broadcast button and tab entry point from the top navigation bar. |  |
| **In-App Browser Privacy Guard** | Redirects external and third-party web links to the default system browser and neutralizes inline JavaScript tracking, DOM monitoring, and AJAX hooking in residual in-app WebViews. |  |
| **Instant Launch & Splash Blocker** | Eliminates cold startup delays, real-time splash advertisements, and background TopView ad preloading. |  |
| **Language Pack Purger** | Strips unselected language string bundles from assets/strings#lang_* to save APK space. | • Languages to keep |
| **Live Stream 3D Gift Optimizer** | Disables Live 3D gift particle effect engine and widget rendering lifecycle to eliminate frame drops during live streams. |  |
| **Live Stream SDK & Minigame De-bloat** | Strips Live link mic SDK (liblink_mic_sdk.so), Lyrax RTC broadcasting engines (liblyrax.so), and live stream interactive minigames to reduce APK size and memory footprint. |  |
| **Media Usability & Watermark-Free Downloader** | Unblocks the download button on creator-restricted videos inside the Share panel, and routes downloads to clean unwatermarked media streams. |  |
| **P2P Video Relay & Mesh CDN Blocker** | Strips background Peer-to-Peer CDN distribution binaries (libavmdlp2pv2.so and libp2plivevdp.so) to prevent battery drain, background data upload, and mesh relay. |  |
| **Playback Speed Persistence** | Persists selected video playback speed across all feed videos and application restarts. |  |
| **Resource & Battery Governor** | Throttles background sensor polling (gyroscope/accelerometer 3D ads) and prevents aggressive video buffer preloading to conserve battery and CPU resources. |  |
| **SIM Region Selector** | Spoofs the detected SIM and network country ISO code to bypass regional feed restrictions and catalog blocks. | • Spoofed Region ISO Code |
| **Show Seekbar** | Restores TikTok's native video seekbar and scrubbing controls where normally hidden or disabled. |  |
| **Skip First-Launch Onboarding** | Bypasses the entire first-run introduction funnel (interest pickers, swipe tutorials, language prompts, and consent sheets) directly to the feed. |  |
| **Studio & Creation De-bloat** | Strips heavy video creation plugins, CapCut NLE editor SDKs, effect plugins, and AR camera face models to significantly reduce APK size. |  |
| **Unified Telemetry & Tracker Silencer** | Neutralizes ByteDance AppLog user tracking, APM/Npth/Heimdallr crash monitors, AppsFlyer attribution, and Firebase analytics. |  |
| **Update Prompt Suppressor** | Neutralizes background update polling tasks and device ID check routines to prevent forced update popups. |  |
| **Video Quality Governor** | Caps video playback and download resolutions (1080p, 720p, 540p, 480p, 360p) independently to conserve battery, GPU/MediaCodec load, and mobile data. | • Maximum Playback Resolution<br>• Maximum Download Resolution |

</details>

<details>
<summary>Brave Private Web Browser, VPN&nbsp;&nbsp;•&nbsp;&nbsp;<b>15 patches</b></summary>
<br>

**Supported versions:**

| 1.96.59 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Block Brave Telemetry** | Blocks P3A product analytics, Brave Stats usage pings, crash dump uploads, WDP, Chromium UMA metrics, and Variations seed fetching. |  |
| **Brave ARM64 BTI Compatibility** | Neutralizes GNU_PROPERTY_AARCH64_FEATURE_1_BTI in libchrome.so to prevent Branch Target Exception SIGILL crashes on ARMv8.5+ devices. |  |
| **Brave In-Product & Commercial Notification Optimizer** | Eliminates background wakeups and notifications from Chromium tips scheduler (Job ID 105), Brave Rewards onboarding promo, and retention marketing campaigns. |  |
| **Brave Native Library Extraction Compatibility** | Enforces native library extraction in AndroidManifest.xml to ensure 16 KB page and BTI compatibility across modern ARM64 devices. |  |
| **Brave Origin** | Unlocks Brave Origin and enables local feature toggle controls. |  |
| **Brave Startup Performance Optimization** | Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations. |  |
| **Clean New Tab Page** | Removes sponsored wallpaper images, Brave News/Today feeds, marketing widgets, and promo cards from the New Tab Page. | • Hide Top Sites & Shortcuts |
| **Clean Share URL** | Strips tracking parameters (utm_*, fbclid, gclid, igshid, si, msclkid) when sharing or copying links. |  |
| **Disable Background Sync & Periodic Sync** | Eliminates background wakeups, radio modem activity, and battery drain by forcing GooglePlayServicesChecker.shouldDisableBackgroundSync() -> true and neutralizing wakeup tasks. |  |
| **Disable Battery Status API & OS Listener** | Neutralizes the Battery Status API (navigator.getBattery) to prevent cross-site device fingerprinting and drops OS battery change broadcasts. |  |
| **Locale PAK Slimmer** | Strips unselected language resource PAKs from assets/locales/. | • Locales to keep |
| **Native Bloat Slimmer** | Strips unused native companion binaries (Impress Vision AI, WireGuard VPN, and Android XR) to significantly reduce APK size. |  |
| **Sensor Privacy Guard** | Neutralizes motion, ambient, and orientation sensor providers to prevent hardware fingerprinting and tracking via Generic Sensor APIs. |  |
| **Skip First Run** | Skips the welcome screen, search engine selection, and onboarding First Run Experience (FRE) on clean installs. |  |
| **Suppress In-App Promos & Surveys** | Suppresses intrusive in-app rating surveys, Play Store review prompts, and marketing promo popups (YouTube promo, ad-free callouts, and Brave Ads onboarding). |  |

</details>

<details>
<summary>Hevy - Gym Log Workout Tracker&nbsp;&nbsp;•&nbsp;&nbsp;<b>4 patches</b></summary>
<br>

**Supported versions:**

| 3.1.14 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Block Hevy Telemetry & Trackers** | Neutralizes Sentry crash reporting, Adjust attribution, Facebook AppEvents, Branch referral tracking, and WearOS background sync. |  |
| **Hevy Battery Optimization & Background Sync Killer** | Disables background WorkManager alarms, periodic job schedulers, Google Play Billing IPC (~89 MB RAM), and removes largeHeap to force aggressive Garbage Collection. |  |
| **Hevy Resource Slimmer** | Strips embedded onboarding MP4 tutorial video, heavy IMG.LY photo editor stickers/textures, and compiler junk metadata. |  |
| **Unlock Pro** | Unlocks local Hevy Pro capabilities (unlimited workout routines, routine folders, advanced graphs, and local analytics) by dynamically enabling Pro getters and suppressing grace period payment warnings in Hermes Bytecode (HBC96). |  |

</details>

<details>
<summary>Vivaldi Browser - Fast & Safe&nbsp;&nbsp;•&nbsp;&nbsp;<b>15 patches</b></summary>
<br>

**Supported versions:**

| 8.2.4147.93 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Block Vivaldi Sync** | Redirects bifrost.vivaldi.com to 0.0.0.0 in libchrome.so to completely disable Vivaldi account synchronization for an air-gapped experience. |  |
| **Block Vivaldi Telemetry** | Blocks Vivaldi usage statistics, crash reporting, Crashpad endpoints, DirectMatch suggestions, install referrer transmission, and background wallpaper polling. |  |
| **Clean Share URL** | Strips tracking parameters (utm_*, fbclid, gclid, igshid, si, msclkid) when sharing or copying links. |  |
| **Clean Speed Dial Bookmarks** | Removes sponsored commercial affiliate bookmarks, hides the 3-dot customize button, and disables phantom touch actions on the blank Start Page. |  |
| **Close Tabs on Exit** | Prevents tab restoration on startup, ensuring Vivaldi always opens with a clean Start Page session regardless of how the app was terminated. |  |
| **Disable Background Sync & Periodic Sync** | Prevents Web Background Sync and Periodic Sync APIs from scheduling wakeups and background tasks in Android JobScheduler. |  |
| **Disable Battery Status API & OS Listener** | Neutralizes the Battery Status API (navigator.getBattery) to prevent cross-site device fingerprinting and drops OS battery change broadcasts. |  |
| **Disable Chromium Tips & Notification Scheduler** | Eliminates periodic background wakeups, native library loading, and in-product promotional tips from Chromium tips scheduler. |  |
| **Disable Vivaldi Prompts & In-App Popups** | Disables in-app donation prompts, search engine switch promos, rate-app popups, background privacy report alarms, default browser promo handlers, and hides the Vivaldia game from the main menu. |  |
| **Locale PAK Slimmer** | Strips unselected language resource PAKs from assets/locales/. | • Locales to keep |
| **Resource Slimmer** | Strips bundled stock wallpapers, sponsored Speed Dial thumbnails, partner favicons, Privacy Sandbox attestations, and hardens declarative preference defaults. |  |
| **Sensor Privacy Guard** | Neutralizes motion, ambient, and orientation sensor providers to prevent hardware fingerprinting and tracking via Generic Sensor APIs. |  |
| **Skip First Run** | Skips the welcome screen, search engine selection, and onboarding First Run Experience (FRE) on clean installs. |  |
| **Split Bundle Compatibility** | Removes split APK constraints (isSplitRequired, isolatedSplits) and split metadata from AndroidManifest.xml for standalone APK installation. |  |
| **Vivaldi Startup Performance Optimization** | Guards against fatal CompositorView NullPointerException crashes on cold startup and bypasses carrier partner customizations. |  |

</details>

<details>
<summary>NokoPrint - WiFi, Bluetooth, USB&nbsp;&nbsp;•&nbsp;&nbsp;<b>8 patches</b></summary>
<br>

**Supported versions:**

| 5.28.4 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **NokoPrint Ad Activities Slimmer** | Disables over 100 third-party ad mediation activities, internal web browsers, and debuggers registered in AndroidManifest.xml. |  |
| **NokoPrint Ad Dispatch Governor** | Neutralizes in-app banner, interstitial, and rewarded ad loaders, preloading schedulers, and refresh handlers. |  |
| **NokoPrint Asset De-bloat** | Strips embedded secondary Meta Audience Network DEX, tracking scripts, ad-viewer HTML/JS templates, RuStore TLS certs, and third-party ad assets to save APK space. |  |
| **NokoPrint Background Sync Optimizer** | Neutralizes background WorkManager constraint tasks and diagnostic wakelocks. |  |
| **NokoPrint Block Telemetry & Trackers** | Neutralizes Firebase Analytics and Google Measurement tracking and event dispatching. |  |
| **NokoPrint Multi-Store Debridger** | Disables orphan billing activities, background services, and permissions for alternative OEM stores (Huawei HMS, Xiaomi, Samsung, CafeBazaar, OneStore). |  |
| **NokoPrint Network Security Hardening** | Enforces HTTPS encryption for driver downloads and backend communications with nokoprint.com while preserving local cleartext traffic for LAN network printers. |  |
| **NokoPrint Startup Performance Optimizer** | Disables non-essential ad mediation and tracker ContentProviders that auto-initialize before Application.onCreate, accelerating cold startup. |  |

</details>

<details>
<summary>Xiaomi Earbuds&nbsp;&nbsp;•&nbsp;&nbsp;<b>11 patches</b></summary>
<br>

**Supported versions:**

| 1.38.0i |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Xiaomi Earbuds Anti-Tamper Bypass** | Bypasses Xposed and hook detection, neutralizes VPN and ADB security checks, bypasses root and emulator detection, and hardens WebView JavaScript bridges. |  |
| **Xiaomi Earbuds Background Optimizer** | Neutralizes KeepAliveForegroundService, background BLE observation, companion device manager, and MIUI Nearby discovery to eliminate persistent notifications, wakelocks, and background battery drain. |  |
| **Xiaomi Earbuds Block Telemetry & Trackers** | Neutralizes Firebase Analytics, Xiaomi OneTrack, AutoReportHelper, and GlobalReport telemetry and event dispatching. |  |
| **Xiaomi Earbuds Device Privacy Guard** | Blinds hardware device IDs, anonymizes device identifiers, neutralizes environment info leakage, and bypasses location checks for Bluetooth scanning. |  |
| **Xiaomi Earbuds Disable Promos & Nags** | Bypasses startup privacy agreements, onboarding guides, region selector prompts, and Bluetooth permission nags, and disables in-app promotional banners, marketing activities, and store review nag dialogs. |  |
| **Xiaomi Earbuds Guest OTA Unlock** | Bypasses mandatory Xiaomi account login checks for firmware update queries, allowing guest users to check and perform device OTA updates. |  |
| **Xiaomi Earbuds Model Catalog Unlock** | Forces DeviceInfoListCache.isShowProduct to return true, bypassing version-gating and distribution restrictions so that all device models are always displayed and discoverable. |  |
| **Xiaomi Earbuds Network Security & TLS Inspection** | Disables cleartext traffic, trusts user-installed certificates, and bypasses OkHttp certificate pinning. |  |
| **Xiaomi Earbuds Offline Only** | Completely isolates the app from the network by revoking internet permissions and spoofing offline status to prevent background socket failures. |  |
| **Xiaomi Earbuds Sound Features Unlock** | Unblocks Spatial Audio, hearing enhancement, and voice wake-up restrictions, unbans Spatial Audio on 96kHz aptX Adaptive connections, and bypasses XPAN requirements. |  |
| **Xiaomi Earbuds Surgical OEM Unlock** | Bypasses Xiaomi OEM hardware gating for SuperAivs (function 5009) and suppresses MIUI-specific spatial audio system dialog nags. |  |

</details>

<details>
<summary>Universal&nbsp;&nbsp;•&nbsp;&nbsp;<b>9 patches</b></summary>
<br>

| Patch | Description | Options |
|----------|----------------|-----------|
| **APK Junk Cleaner** | Strips non-functional build metadata, compiler properties, Kotlin coroutines debug tables, and duplicate license texts from META-INF and APK root. |  |
| **Background Sync & JobScheduler Purge** | Strips RECEIVE_BOOT_COMPLETED and disables boot, package-replacement, and periodic background sync receivers and services in AndroidManifest.xml to eliminate background wakeups and conserve battery. | • Strip RECEIVE_BOOT_COMPLETED Permission<br>• Disable Boot & Package Receivers<br>• Disable WorkManager & Job Schedulers<br>• Strip WAKE_LOCK Permission |
| **DPI Resource Slimmer** | Strips unselected screen density resource directories from res/ (e.g. drawable-mdpi, drawable-hdpi, mipmap-xhdpi). Density-independent resources (nodpi, anydpi) and orphan resources are safely preserved. | • DPI densities to keep |
| **Locale Resource Slimmer** | Strips unselected language translation directories from res/ (e.g. values-*, raw-*, xml-*). Base fallback resources with no language qualifiers are always preserved. | • Locales to keep |
| **PNG Asset Optimizer** | Losslessly recompresses PNG assets with maximum zlib compression and strips non-rendering metadata chunks (pHYs, tEXt, tIME) while preserving 9-patch structures and pixel accuracy. |  |
| **Universal Native Binary Trimmer** | Strips non-essential tracking, crash reporting, and debug companion native libraries in lib/** (e.g. libcrashlytics, libsentry, libbugly, libgwp-asan) by zeroing bytes in-situ. | • Trim Crash Reporting Libraries<br>• Trim Debug & Profiling Libraries |
| **Universal Offline Mode** | Forces offline execution across any application by revoking INTERNET and network permissions from AndroidManifest.xml and blocking cleartext HTTP traffic at the OS level. | • Strip Network State Permissions<br>• Strip Wi-Fi Control Permissions<br>• Strip Push Notification Permissions<br>• Strip Google Services Sync Permissions<br>• Block Cleartext Traffic |
| **Universal Telemetry Neutralizer** | Strips advertising and Privacy Sandbox permissions, disables analytics ContentProviders and telemetry background services (Firebase, Sentry, Adjust, AppsFlyer, DataTransport), and injects telemetry opt-out metadata. | • Revoke Advertising & Tracking Permissions<br>• Disable Telemetry ContentProviders<br>• Disable Telemetry Background Services<br>• Disable Telemetry Receivers<br>• Inject Telemetry Opt-Out Flags<br>• Disable Firebase Init Provider |
| **Universal WebP Asset Optimizer** | Losslessly strips non-rendering metadata and ancillary chunks (EXIF, XMP, ICCP) from WebP assets across res/ and assets/ to reduce APK size. | • Strip EXIF Metadata<br>• Strip XMP Metadata<br>• Strip ICC Color Profiles |

</details>

<!-- PATCHES_END -->

---

## 📚 Documentation & Guides

Technical references, setup manuals, and architecture notes are organized by focus area:

### 📱 Dedicated Application Guides
| Application | Guide | Description |
| :--- | :--- | :--- |
| **Brave Browser** | **[Brave Guide](docs/apps/brave.md)** | Zero-configuration debloat, telemetry neutralization, sponsored NTP removal, and PAK slimmer. |
| **Gboard Lite** | **[Gboard Lite Guide](docs/apps/gboard.md)** | Offline dictionaries & Glide Typing setup, MDD sync debloat, and clipboard retention options. |
| **Hevy** | **[Hevy Guide](docs/apps/hevy.md)** | Email authentication workflow, Hermes bytecode Pro unlocks, client vs server limits, and telemetry. |
| **NokoPrint** | **[NokoPrint Guide](docs/apps/nokoprint.md)** | In-app ad loaders neutralization, premium status enforcement, multi-store debloat, and network security. |
| **TikTok** | **[TikTok Guide](docs/apps/tiktok.md)** | SIM region spoofing, decoupled quality governor, 120Hz refresh lock, watermark-free downloader, and ad filters. |
| **Vivaldi Browser** | **[Vivaldi Guide](docs/apps/vivaldi.md)** | Native GURL redirection in `libchrome.so`, UKM telemetry neutralization, Start Page debloat, and PAK slimmer. |
| **Xiaomi Earbuds** | **[Xiaomi Earbuds Guide](docs/apps/xiaomi-earbuds.md)** | Initial pairing workflow, anti-tamper bypass, spatial audio / aptX 96kHz unlocks, and offline isolation. |

### 🌐 Universal & Architecture Reference
| Guide | Description |
| :--- | :--- |
| **[Universal Patches & Options](docs/universal-patches.md)** | Universal debloat & privacy suite: Telemetry Neutralizer, Native Binary Trimmer, WebP Optimizer, Background Sync Purge, Offline Mode, DPI/Locale Slimmers, and Asset Cleaners. |
| **[Compatibility Guide](docs/compatibility.md)** | CPU architecture policy (`arm64-v8a` vs `armeabi-v7a`), APK variant requirements, and SHA-256 baseline. |
| **[Project Scope & Out of Scope](docs/out-of-scope.md)** | Non-negotiable design philosophy, compile-time invariants, and rejected feature categories. |
| **[Architecture & Security Notes](docs/architecture-security.md)** | Static analysis scanner false positives (ML Kit, Play Billing) and native ELF telemetry neutralization. |

### 🛠️ Developer & Tooling
| Guide | Description |
| :--- | :--- |
| **[Building & Development](docs/building.md)** | Toolchain prerequisites, Gradle build tasks, test execution, and catalog synchronization. |
| **[Reverse Engineering & Update Harness](harness/README.md)** | Automated Python pipeline for multi-DEX indexing, obfuscated symbol resolution, and APK audits. |
| **[Physical & Runtime ADB Test Suite](validation/physical_harness/README.md)** | Automated on-device validation for battery consumption, ServiceWorker sync, and runtime telemetry. |

---

## 🎯 Contributing & Feature Requests

Before proposing new features or submitting modifications, please review our **[Project Scope & Out-of-Scope Philosophy](docs/out-of-scope.md)**.

We prioritize **surgical, lightweight, zero-overhead compile-time transformations** and rapid upstream synchronization with the latest app versions. Dynamic in-app settings panels, legacy multi-version support, server-side exploits, or heavy feature bloat are explicitly out of scope.

### Mandatory Verification Gates
Every patch modification or contribution must pass the official Morphe Patcher in-situ verification gate with 100% success (0 failed patches, 0 fingerprint mismatches):
```bash
# Execute in-situ patching test for target application
./gradlew runPatchTest -Papp=<targetApp>   # e.g., brave, gboard, vivaldi, hevy, tiktok, nokoprint, xiaomi_earbuds
```
Additionally, ensure all Gradle checks and automated harness tests pass cleanly:
```bash
./gradlew check
./venv/bin/python -m unittest discover harness/tests
```

---

## 💬 Community & Support

Need assistance, have questions regarding patch configurations, or want to follow release announcements? Join the official Telegram community:

- 💬 **Telegram Support Group**: [t.me/kveldmorphe](https://t.me/kveldmorphe)

---

## 🤝 Credits & Contributors

<!-- CONTRIBUTORS_START -->
| Contributor | Role & Contributions |
| :--- | :--- |
| <a href="https://github.com/Lxchoooo"><img src="https://github.com/Lxchoooo.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@Lxchoooo</b></a> | 🧪 Daily patch testing, runtime APK validation, and bug diagnostics. |
| <a href="https://github.com/ll0r3nt3"><img src="https://github.com/ll0r3nt3.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@ll0r3nt3</b></a> | 💡 Proposed DPI Resource Slimmer ([#16](https://github.com/kveld9/kveld-morphe-patches/issues/16)) and Vivaldi Browser Stable migration ([#20](https://github.com/kveld9/kveld-morphe-patches/issues/20)) feature requests. |
| <a href="https://github.com/aidenking2102-dotcom"><img src="https://github.com/aidenking2102-dotcom.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@aidenking2102-dotcom</b></a> | 💡 Proposed TikTok Feed Ad Blocker ([#23](https://github.com/kveld9/kveld-morphe-patches/issues/23)), Playback Speed Setter ([#25](https://github.com/kveld9/kveld-morphe-patches/issues/25)), and Floating Ad Pendant / Sticker Blocker ([#33](https://github.com/kveld9/kveld-morphe-patches/issues/33)) feature requests. |
| <a href="https://github.com/mparvezalam808"><img src="https://github.com/mparvezalam808.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@mparvezalam808</b></a> | 💡 Proposed TikTok Always show publish date, Copy comments without username, Fix Google login, and Show seekbar feature request ([#35](https://github.com/kveld9/kveld-morphe-patches/issues/35)). |
| <a href="https://github.com/raxelbyte"><img src="https://github.com/raxelbyte.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@raxelbyte</b></a> | 💡 Proposed Gboard Lite Clipboard Enhancements feature request ([#17](https://github.com/kveld9/kveld-morphe-patches/issues/17)). |
| <a href="https://github.com/rafipasya"><img src="https://github.com/rafipasya.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@rafipasya</b></a> | 💡 Proposed TikTok Auto-translate comments feature request ([#48](https://github.com/kveld9/kveld-morphe-patches/issues/48)). |
| <a href="https://github.com/Fahry-a"><img src="https://github.com/Fahry-a.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@Fahry-a</b></a> | 💡 Proposed Brave ARMv7a (32-bit) architecture support feature request ([#50](https://github.com/kveld9/kveld-morphe-patches/issues/50)). |
<!-- CONTRIBUTORS_END -->

---

## ⚖️ Legal Disclaimer

**Morphe Patches** is an independent, community-driven open-source project and is not affiliated, associated, authorized, endorsed by, or in any way officially connected with Brave Software, Inc., Google LLC, Vivaldi Technologies AS, Hevy App, ByteDance Ltd., NokoPrint LLC, Xiaomi Inc., or any of their subsidiaries or affiliates.

All product names, logos, brands, and registered trademarks mentioned in this repository are the property of their respective holders. Their inclusion does not imply affiliation with or endorsement by them.

---

## 📜 License

Morphe Patches is open-source software licensed under the [GNU General Public License v3.0](LICENSE).
