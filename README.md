<p align="center">
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases/latest"><img src="https://img.shields.io/github/v/release/kveld9/kveld-morphe-patches?color=7928CA&label=Release&logo=github&style=flat-square" alt="Latest Release" /></a>
  <a href="https://github.com/kveld9/kveld-morphe-patches/releases"><img src="https://img.shields.io/github/downloads/kveld9/kveld-morphe-patches/total?style=flat-square&logo=github" alt="Total Downloads" /></a>
  <img src="https://img.shields.io/badge/Runtime-Morphe_Patcher_1.8.0-8A2BE2?style=flat-square" alt="Runtime" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue?style=flat-square" alt="License" />
</p>

<h1 align="center">🔮 Morphe Patches</h1>

<p align="center">
  Modular bytecode, resource, and native patch suite for <b>Brave Browser</b>, <b>Gboard Lite</b>, <b>Hevy</b>, <b>TikTok</b>, <b>NokoPrint</b>, and <b>Xiaomi Earbuds</b> on Android using the <b><a href="https://morphe.software">Morphe</a></b> patcher framework.
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
| **Brave Browser** | `com.brave.browser` | `1.96.61` | `arm64-v8a`<br>`armeabi-v7a` (Monolithic) | [ARM64](https://github.com/brave/brave-browser/releases/download/v1.96.61/Bravemonoarm64.apk) · [ARM32](https://github.com/brave/brave-browser/releases/download/v1.96.61/BraveMonoarm.apk) | [Brave Guide](docs/apps/brave.md) |
| **Gboard Lite** | `com.google.android.inputmethod.latin` | `18.4.1.985164140` | `arm64-v8a`<br>`armeabi-v7a` (nodpi) | [APKMirror](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-4-1-985164140-beta/) | [Gboard Lite Guide](docs/apps/gboard.md) |
| **Hevy** | `com.hevy` | `3.1.14` | `arm64-v8a` (APKM Bundle) | [APKMirror](https://www.apkmirror.com/apk/hevy-gym-workout-tracker/hevy-gym-log-workout-tracker/hevy-gym-log-workout-tracker-3-1-14-release/) | [Hevy Guide](docs/apps/hevy.md) |
| **NokoPrint** | `com.nokoprint` | `5.28.6` | Universal (nodpi) | [APKPure](https://d.apkpure.com/b/XAPK/com.nokoprint?versionCode=52806) | [NokoPrint Guide](docs/apps/nokoprint.md) |
| **TikTok** | `com.zhiliaoapp.musically` | `47.1.4` | `arm64-v8a` (nodpi) | [APKMirror](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-47-1-4-release/) | [TikTok Guide](docs/apps/tiktok.md) |
| **Xiaomi Earbuds** | `com.mi.earphone` | `1.38.0i` | Universal (XAPK Bundle) | [APKPure](https://d.apkpure.com/b/XAPK/com.mi.earphone?versionCode=138000) | [Xiaomi Earbuds Guide](docs/apps/xiaomi-earbuds.md) |

---

## 💊 Patch Catalog

<!-- PATCHES_START -->
<details>
<summary>NokoPrint - WiFi, Bluetooth, USB&nbsp;&nbsp;•&nbsp;&nbsp;<b>6 patches</b></summary>
<br>

**Supported versions:**

| 5.28.6 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Ad Dispatch Governor** | Neutralizes ad loaders, unlocks ad-free status, and strips mediation components & startup providers. |  |
| **Asset Debloat** | Strips embedded ad DEX, tracking scripts, web templates, and ad drawables to reduce APK size. |  |
| **Background Sync Optimizer** | Neutralizes background WorkManager constraint tasks and diagnostic wakelocks. |  |
| **Block Telemetry & Trackers** | Neutralizes Firebase Analytics, Google Measurement, TikTok Business SDK, and crashlytics tracking. |  |
| **Multi-Store Debridger** | Disables orphan billing activities, services, and permissions for alternative OEM stores (Huawei, Xiaomi, Samsung). |  |
| **Network Security Hardening** | Enforces user trust anchors while preserving HTTP cleartext traffic for driver downloads and LAN printers. |  |

</details>

<details>
<summary>TikTok&nbsp;&nbsp;•&nbsp;&nbsp;<b>60 patches</b></summary>
<br>

**Supported versions:**

| 47.1.4 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Always Show Publish Date** | Forces video publish/upload date to remain visible in video author information across all feed types. |  |
| **Auto-Pause First Video** | Automatically pauses the first video when opening TikTok, allowing the application to finish background initialization and preventing playback lag. |  |
| **Bypass Mandatory Login** | Neutralizes mandatory login walls, dynamic regional forced login gates, and guest browsing restrictions. |  |
| **Bypass Screen Capture Detection** | Clears FLAG_SECURE on protected windows, restores Circle to Search / screen translate and recent apps snapshots, and neutralizes screenshot detection listeners and feedback prompts. |  |
| **Clean Share Panel** | Removes clutter from the share panel and direct message dialog, including suggested quick emojis and the 'Send to new group' button. | • Hide Quick Emojis<br>• Hide 'Send to New Group' |
| **Clean Share URL** | Strips tracking parameters, user IDs, device fingerprints, and marketing tokens from shared TikTok links. |  |
| **Client-Side AI & Behavioral Profiling Governor** | Neutralizes on-device machine learning inference (Pitaya), Tako AI chatbot entry points and icons, and AI smart search suggestion clutter. |  |
| **Comment Customizer** | Customizes TikTok's comment section, including native sort controls, clean text copying, disabling suggested emojis bar, hiding comment quick actions, hiding in-comment surveys and feedback cards, hiding profile photo story rings, enabling voice comments, and automatic comment translation. | • Comment Sort Controls<br>• Copy Comments Without Username<br>• Disable Suggested Emojis<br>• Hide Comment Quick Actions<br>• Hide Comment Surveys & Feedback Cards<br>• Hide Comment Story Rings<br>• Enable Voice Comments<br>• Auto-Translate Comments |
| **Core Asset De-bloat** | Strips embedded Microblink/FinTech card scanner models, Pitaya AI & ByteNN LLM engines, C2PA origin verification, DLNA cast scanners, and redundant non-Latin fonts to save APK space. |  |
| **Custom Offline Videos Limit** | Customizes the maximum number of videos available for offline download caching. | • Custom Offline Videos Limit |
| **Custom Share Sheet** | Customizes and cleans the native TikTok share sheet via individual toggle switches for third-party apps, essential sharing features, and secondary utility actions. | • Hide WhatsApp<br>• Hide Instagram<br>• Hide Facebook & Messenger<br>• Hide Telegram<br>• Hide X / Twitter<br>• Hide Snapchat<br>• Hide Reddit & Discord<br>• Hide SMS & Messages<br>• Hide Secondary Networks<br>• Hide 'Repost' Button<br>• Hide QR Code<br>• Hide 'Copy Link'<br>• Hide System Share ('More')<br>• Hide Friends / Direct Messages Row<br>• Hide 'Promote' Action<br>• Hide 'Why This Video'<br>• Hide 'Create Group' Action<br>• Hide 'Add to Story'<br>• Hide 'Create Sticker'<br>• Hide 'Duet' Action<br>• Hide 'Stitch' Action<br>• Hide 'Picture-in-Picture' (PiP)<br>• Hide 'Clear Display'<br>• Hide 'Background Audio'<br>• Hide Live Wallpaper & GIF<br>• Hide 'Not Interested'<br>• Hide 'Report' |
| **Device Privacy Guard** | Neutralizes invasive runtime permissions (contacts sync, location tracking, nearby devices), advertising ID profiling, background clipboard snooping routines, and motion sensor profiling to protect user data. |  |
| **Direct Message Declutter** | Removes visual clutter in direct messages and chat list, including the call button, reaction tray, message forward button, camera icons, input action buttons, try effect button, and sticker reply suggestions. | • Hide Chat List Camera Button<br>• Hide Header Call Button<br>• Hide Message Forward Button<br>• Hide Reaction and Streak Bar<br>• Hide Input Bar Camera Button<br>• Hide Gallery Button<br>• Hide Emoji/Stickers Button<br>• Hide Voice Recording Button<br>• Hide Try Effect Button<br>• Hide Sticker Reply Suggestions |
| **Disable Double Tap to Like** | Disables the double tap gesture to like videos in the feed, preventing accidental likes while scrolling or pausing. Videos can still be liked using the like button. |  |
| **Disable Feed Long-Press Actions** | Disables long-press action gestures on feed buttons, including Like to repost, Share to quick DMs, and Comment to quick emojis. | • Disable Long-Press Like (Repost)<br>• Disable Long-Press Share (Quick DMs)<br>• Disable Long-Press Comment (Quick Emojis) |
| **Disable HDR Video Playback** | Forces the video playback engine to select standard SDR bitrates (BT.709/sRGB) instead of HDR (HDR10/PQ/HLG), preventing blinding screen brightness spikes and display thermal throttling while preserving smooth playback. |  |
| **Disable Post-Download Share Dialog** | Suppresses the automatic 'Share to' and friend suggestions bottom sheet that pops up after finishing a download. |  |
| **Disable Profile Photo LIVE Status** | Removes the pulsing LIVE ring and badge from creator avatars in the feed and ensures clicking navigates strictly to the user profile instead of launching the live stream. |  |
| **Disable Push Notifications** | Neutralizes background push notification tasks and persistent socket wake locks to eliminate background battery drain. |  |
| **Disable Search History Recording** | Prevents search queries and keywords from being recorded in local history, databases, and analytics stores. |  |
| **Disable Search Video Autoplay** | Disables automatic video playback in search results. Videos only play when tapped to view in detail. |  |
| **Disable Watch History Recording** | Prevents viewed videos from being recorded in account watch history, playback duration stores, and local history caches. |  |
| **Display Refresh Rate Governor** | Forces TikTok to run at peak display refresh rate (120Hz/90Hz/60Hz) and neutralizes video playback framerate downclocking routines. | • Target Refresh Rate |
| **Enable Profile Banner** | Unlocks the custom profile banner (background header cover) feature on user profiles and enables the banner selection and editing tools in Edit Profile. |  |
| **Feed Ad Blocker** | Removes sponsored advertisements, brand promotions, and promotional audio from the For You, Following, and Search feeds. |  |
| **Feed Bloat & Distraction Blocker** | Removes non-video clutter and floating ad widgets from the For You, Following, and Friends feeds, including Touchpoint Rewards pendants, floating ad stickers, suggested friend cards, mini-games, CapCut/template creation prompts, memories ('On This Day'), community/topic cards, post-video surveys and evaluation questionnaires, mini-drama paywalls, and in-feed search recommendations/interest cards. |  |
| **Feed Interface Declutter** | Customizes and cleans feed video overlay elements, including the full screen button, repost pill, video descriptions, profile photo follow badges, story rings, playlist bottom bars, save buttons, and music discs. | • Hide Repost Badge<br>• Hide Video Descriptions<br>• Hide Profile Photo Follow Button<br>• Disable Story Feed Indicators<br>• Hide Playlist Bottom Bar<br>• Hide Save Button<br>• Hide Music Cover Disc<br>• Hide Full Screen Button |
| **Feed Live Stream Blocker** | Removes live stream broadcast cards and live recommendations from the For You and Following feeds. |  |
| **Fix Google Login** | Restores Google account sign-in after patching by forcing fallback to Web-based OAuth when Google Play Services rejects the modified APK signature. |  |
| **Fix Spotify Login** | Restores the 'Add to Spotify' music button after patching by routing the Spotify app sign-in, which rejects the modified APK signature, through Spotify's Web-based OAuth. |  |
| **Force Auto-Scroll** | Forces the activation of the native video auto-scroll experiment flag for accounts and regions that lack it due to A/B testing. |  |
| **Friends Feed Strict Mutuals** | Filters out suggested accounts, recommended videos, and non-mutual profiles (such as 'People you may know') from the Friends feed so it only plays videos from accounts you mutually follow. |  |
| **Hide AI-Generated Content** | Filters and skips videos tagged with native AI-generated metadata, C2PA content credentials, or creator AI disclosure tags across the For You, Following, and Friends feeds. |  |
| **Hide Inbox Promos & Alerts** | Hides promotional banners, streak mascot cards, contact sync suggestions, friend recommendations, and migration guide tooltips in the inbox and direct messages. | • Hide Top Promotional Banners<br>• Hide Contact & Friend Recommendations<br>• Hide Navigation Notices & Tooltips |
| **Hide Inbox Story & Status Tray** | Hides the horizontal story, notes, and status tray (Skylight) displayed at the top of direct messages and the inbox. |  |
| **Hide Popular Lives In Search** | Removes the Popular LIVEs recommendation card and live stream broadcasts from the search discovery page. |  |
| **Hide Seen Videos** | Filters previously watched videos from incoming For You feed batches. |  |
| **Hide Suggested Searches** | Removes the suggested search keywords section ('You may like' / 'Search suggestions') from the search discovery page. |  |
| **Hide TikTok Shop & Mall** | Removes product showcase badges, shopping cart tags, and the TikTok Shop / Mall tab from navigation bars and video posts. | • Hide Shop Navigation Tab<br>• Hide Video Product Anchors |
| **In-App Browser Privacy Guard** | Redirects external and third-party web links to the default system browser and neutralizes inline JavaScript tracking, DOM monitoring, and AJAX hooking in residual in-app WebViews. |  |
| **Instant Launch & Splash Blocker** | Eliminates cold startup delays, background resume splash advertisements, real-time splash requests, and TopView ad preloading. |  |
| **Language Pack Purger** | Strips unselected language string bundles from assets/strings#lang_* to save APK space. | • Languages to keep |
| **Live Stream 3D Gift Optimizer** | Disables Live 3D gift particle effect engine and widget rendering lifecycle to eliminate frame drops during live streams. |  |
| **Live Stream SDK & Minigame De-bloat** | Strips Live link mic SDK (liblink_mic_sdk.so), Lyrax RTC broadcasting engines (liblyrax.so), and live stream interactive minigames to reduce APK size and memory footprint. |  |
| **Media Usability & Watermark-Free Downloader** | Unblocks the download button on creator-restricted videos inside the Share panel, and routes downloads to clean unwatermarked media streams. |  |
| **Navigation & Header Declutter** | Removes clutter from the feed navigation and top header bar, including the Nearby feed tab, Community (Explore) tab, top-left LIVE broadcast button, central '+' create content button, in-video bottom search suggestion bar, friend profile photo previews on the bottom Friends tab, and unread notification badges on the bottom Messages (Inbox) tab. | • Hide Nearby Feed Tab<br>• Hide Community Tab<br>• Hide Top-Left LIVE Button<br>• Hide Feed Search Bar<br>• Hide Create / Publish Button<br>• Hide Friends Tab Avatar Preview<br>• Hide Inbox Notification Badge |
| **P2P Video Relay & Mesh CDN Blocker** | Strips background Peer-to-Peer CDN distribution binaries (libavmdlp2pv2.so and libp2plivevdp.so) to prevent battery drain, background data upload, and mesh relay. |  |
| **Playback Speed Persistence** | Persists selected video playback speed across all feed videos and application restarts. |  |
| **Popups & Prompts Suppressor** | Suppresses intrusive popups, dialogs, and modal prompts, including 'Follow your friends' dialogs, contacts sync overlays, multi-account notification guides, 2SV security checkup modals, PopLayer promotional sheets, live stream teaser bubbles, sticker recommendations, and DM streak expiration warnings. | • Suppress Account & Permission Nags<br>• Suppress Sticker Recommendations<br>• Filter PopLayer Prompts & Nags<br>• Suppress Live Teaser Bubbles<br>• Suppress DM Streak Reminders |
| **Resource & Battery Governor** | Throttles background sensor polling (gyroscope/accelerometer 3D ads) and prevents aggressive video buffer preloading to conserve battery and CPU resources. |  |
| **Resume Video After Scroll** | Remembers playback timestamp when scrolling away and resumes from where playback stopped upon returning. |  |
| **SIM Region Selector** | Spoofs the detected SIM and network country ISO code to bypass regional feed restrictions and catalog blocks. | • Spoofed Region ISO Code |
| **Show Seekbar** | Restores TikTok's native video seekbar and scrubbing controls where normally hidden or disabled. | • Show Dragging Thumbnail Preview |
| **Skip First-Launch Onboarding** | Bypasses the entire first-run introduction funnel (interest pickers, swipe tutorials, language prompts, and consent sheets) directly to the feed. |  |
| **Stop Video Looping** | Stops videos at the end instead of replaying them in an infinite loop. |  |
| **Studio & Creation De-bloat** | Strips heavy video creation plugins, CapCut NLE editor SDKs, effect plugins, and AR camera face models to significantly reduce APK size. |  |
| **System Font** | Forces TikTok to use the Android system font instead of bundled proprietary TikTokSans fonts. |  |
| **Unified Telemetry & Tracker Silencer** | Neutralizes ByteDance AppLog user tracking, APM/Npth/Heimdallr crash monitors, AppsFlyer attribution, and Firebase analytics. |  |
| **Update Prompt Suppressor** | Neutralizes background update polling tasks and device ID check routines to prevent forced update popups. |  |
| **Video Quality Governor** | Caps video playback and download resolutions (1080p, 720p, 540p, 480p, 360p) independently to conserve battery, GPU/MediaCodec load, and mobile data. | • Maximum Playback Resolution<br>• Maximum Download Resolution |

</details>

<details>
<summary>Xiaomi Earbuds&nbsp;&nbsp;•&nbsp;&nbsp;<b>11 patches</b></summary>
<br>

**Supported versions:**

| 1.38.0i |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Anti-Tamper Bypass** | Bypasses Xposed and hook detection, neutralizes VPN and ADB security checks, bypasses root and emulator detection, and hardens WebView JavaScript bridges. |  |
| **Background Optimizer** | Neutralizes KeepAliveForegroundService, background BLE observation, companion device manager, and MIUI Nearby discovery to eliminate persistent notifications, wakelocks, and background battery drain. |  |
| **Block Telemetry & Trackers** | Neutralizes Firebase Analytics, Xiaomi OneTrack, AutoReportHelper, and GlobalReport telemetry and event dispatching. |  |
| **Device Privacy Guard** | Blinds hardware device IDs, anonymizes device identifiers, neutralizes environment info leakage, and bypasses location checks for Bluetooth scanning. |  |
| **Disable Promos & Nags** | Bypasses startup privacy agreements, onboarding guides, region selector prompts, and Bluetooth permission nags, and disables in-app promotional banners, marketing activities, and store review nag dialogs. |  |
| **Guest OTA Unlock** | Bypasses mandatory Xiaomi account login checks for firmware update queries, allowing guest users to check and perform device OTA updates. |  |
| **Model Catalog Unlock** | Forces DeviceInfoListCache.isShowProduct to return true, bypassing version-gating and distribution restrictions so that all device models are always displayed and discoverable. |  |
| **Network Security & TLS Inspection** | Disables cleartext traffic, trusts user-installed certificates, and bypasses OkHttp certificate pinning. |  |
| **Offline Only** | Completely isolates the app from the network by revoking internet permissions and spoofing offline status. Note: do not activate on first launch; pair your earbuds once before enabling. |  |
| **Sound Features Unlock** | Unblocks Spatial Audio, hearing enhancement, and voice wake-up restrictions, unbans Spatial Audio on 96kHz aptX Adaptive connections, and bypasses XPAN requirements. |  |
| **Surgical OEM Unlock** | Bypasses Xiaomi OEM gating to enable full feature parity (voice assistant & spatial audio dialogs) on non-Xiaomi devices (Samsung, Pixel, etc.). |  |

</details>

<details>
<summary>Hevy - Gym Log Workout Tracker&nbsp;&nbsp;•&nbsp;&nbsp;<b>4 patches</b></summary>
<br>

**Supported versions:**

| 3.1.14 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Battery & Background Sync Optimizer** | Disables background WorkManager alarms, periodic job schedulers, Google Play Billing IPC (~89 MB RAM), and removes largeHeap to force aggressive Garbage Collection. |  |
| **Block Telemetry & Trackers** | Neutralizes Sentry crash reporting, Adjust attribution, Facebook AppEvents, Branch referral tracking, and WearOS background sync. |  |
| **Resource Slimmer** | Strips embedded onboarding MP4 tutorial video, heavy IMG.LY photo editor stickers/textures, and compiler junk metadata. |  |
| **Unlock Pro** | Unlocks local Hevy Pro capabilities (unlimited workout routines, routine folders, advanced graphs, and local analytics) by dynamically enabling Pro getters and suppressing grace period payment warnings in Hermes Bytecode (HBC96). |  |

</details>

<details>
<summary>Brave Private Web Browser, VPN&nbsp;&nbsp;•&nbsp;&nbsp;<b>14 patches</b></summary>
<br>

**Supported versions:**

| 1.96.61 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Block Brave Telemetry** | Blocks P3A product analytics, Brave Stats usage pings, crash dump uploads, WDP, Chromium UMA metrics, and Variations seed fetching. |  |
| **Brave In-Product & Commercial Notification Optimizer** | Eliminates background wakeups and notifications from Chromium tips scheduler (Job ID 105), Brave Rewards onboarding promo, and retention marketing campaigns. |  |
| **Brave Origin** | Unlocks Brave Origin and enables local feature toggle controls. |  |
| **Brave Startup Performance Optimization** | Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations. |  |
| **Clean New Tab Page** | Removes sponsored wallpaper images, Brave News/Today feeds, marketing widgets, and promo cards from the New Tab Page. | • Hide Top Sites & Shortcuts |
| **Clean Share URL** | Strips tracking parameters (utm_*, fbclid, gclid, igshid, si, msclkid) when sharing or copying links. |  |
| **Disable Background Sync & Periodic Sync** | Eliminates background wakeups, radio modem activity, and battery drain by forcing GooglePlayServicesChecker.shouldDisableBackgroundSync() -> true and neutralizing wakeup tasks. |  |
| **Disable Battery Status API & OS Listener** | Neutralizes the Battery Status API (navigator.getBattery) to prevent cross-site device fingerprinting and drops OS battery change broadcasts. |  |
| **Disable Tab Auto-Minimization** | Prevents Brave from minimizing active tabs to the background and forcing a New Tab Page when returning to the browser after inactivity. |  |
| **Locale PAK Slimmer** | Strips unselected language resource PAKs from assets/locales/. | • Locales to keep |
| **Native Bloat Slimmer** | Strips unused native companion binaries (Impress Vision AI, WireGuard VPN, and Android XR) to significantly reduce APK size. |  |
| **Sensor Privacy Guard** | Neutralizes motion, ambient, and orientation sensor providers to prevent hardware fingerprinting and tracking via Generic Sensor APIs. |  |
| **Skip First Run** | Skips the welcome screen, search engine selection, and onboarding First Run Experience (FRE) on clean installs. |  |
| **Suppress In-App Promos & Surveys** | Suppresses intrusive in-app rating surveys, Play Store review prompts, and marketing promo popups (YouTube promo, ad-free callouts, and Brave Ads onboarding). |  |

</details>

<details>
<summary>Gboard Lite&nbsp;&nbsp;•&nbsp;&nbsp;<b>11 patches</b></summary>
<br>

**Supported versions:**

| 18.4.1.985164140-lite_beta-arm64-v8a | 18.4.1.985164140-lite_beta-armeabi-v7a |
| :---: | :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| **Block Telemetry** | Disables background metrics dispatch, event logging, daily pings, Google Primes profiling, crash reporting, AppDoctor diagnostics, and Tenor share tracking. |  |
| **Clone Gboard** | Changes the package name by appending a dot and custom suffix (defaults to 'clone') to allow installing Gboard alongside the original application. | • Package name suffix |
| **Disable Background Sync** | Neutralizes AndroidX WorkManager schedulers, MDD (Mobile Data Download) periodic sync, and Superpacks eager asset synchronization (opt-in to preserve initial dictionary downloads). |  |
| **Disable Cloud Backup** | Disables Android backup for Gboard (allowBackup=false and backup agent removed) so keyboard settings, learned words, and personal dictionary data are never uploaded to Google Drive backups or copied by device-to-device transfer. |  |
| **Disable Play Services Integration** | Makes Gboard's Google Play services availability check always report SERVICE_DISABLED, so GMS-backed code paths (Clearcut logging, Phenotype, account sync, Google Help feedback) are skipped at the source instead of being attempted. |  |
| **Disable Remote Configuration** | Disables periodic remote experiment flag synchronization and background updates. |  |
| **Gboard Enhancements** | Master customization suite bundling in-app toggleable features (AMOLED Pure Black theme, zero bottom inset, independent keyboard vibration, force incognito, voice typing in incognito, clipboard retention, top toolbar icons count, cursor trackpad, and smart flags) managed directly from a top-level Morphe Patches category in Gboard Settings. |  |
| **Hardened Intent Security** | Enables Gboard internal external intent protection against unauthorized intent hijacking and removes the exported, permissionless web debug bridge content provider. |  |
| **Offline Only** | Completely isolates Gboard from the network by revoking network permissions, neutralizing HTTP clients (Cronet, OkHttp, Superpacks), and spoofing offline status. |  |
| **Resource Slimmer** | Strips embedded third-party license text, onboarding tutorial Lottie animations, promotional GIFs, and APK root metadata/junk files. |  |
| **Strip Permissions** | Selectively revokes sensitive hardware, privacy, and system permissions from AndroidManifest.xml. | • Strip Contacts Permission<br>• Strip Microphone Permission<br>• Strip Media & Storage Permissions<br>• Strip System Dictionary Permissions<br>• Strip Cross-Profile Permission |

</details>

<details>
<summary>Universal&nbsp;&nbsp;•&nbsp;&nbsp;<b>13 patches</b></summary>
<br>

| Patch | Description | Options |
|----------|----------------|-----------|
| **APK Junk Cleaner** | Strips non-functional build metadata, compiler properties, Kotlin coroutines debug tables, and duplicate license texts from META-INF and APK root. |  |
| **Background Sync & JobScheduler Purge** | Strips RECEIVE_BOOT_COMPLETED and disables boot, package-replacement, and periodic background sync receivers and services in AndroidManifest.xml to eliminate background wakeups and conserve battery. | • Strip RECEIVE_BOOT_COMPLETED Permission<br>• Disable Boot & Package Receivers<br>• Disable WorkManager & Job Schedulers<br>• Strip WAKE_LOCK Permission |
| **DPI Resource Slimmer** | Strips unselected screen density resource directories from res/ (e.g. drawable-mdpi, drawable-hdpi, mipmap-xhdpi). Density-independent resources (nodpi, anydpi) and orphan resources are safely preserved in-situ. | • DPI densities to keep<br>• Remove smartwatch (Wear OS) resources<br>• Remove Android TV resources<br>• Remove automotive, dock, and VR resources |
| **Locale Resource Slimmer** | Strips unselected language translation directories from res/ (e.g. values-*, raw-*, xml-*). Base fallback resources with no language qualifiers are always preserved. | • Locales to keep |
| **PNG Asset Optimizer** | Losslessly recompresses PNG assets with maximum zlib compression and strips non-rendering metadata chunks (pHYs, tEXt, tIME) while preserving 9-patch structures and pixel accuracy. |  |
| **Universal Native Binary Trimmer** | Strips non-essential tracking, crash reporting, and debug companion native libraries in lib/** (e.g. libcrashlytics, libsentry, libbugly, libgwp-asan) by zeroing bytes in-situ. | • Trim Crash Reporting Libraries<br>• Trim Debug & Profiling Libraries |
| **Universal Offline Mode** | Forces offline execution across any application by revoking INTERNET and network permissions from AndroidManifest.xml and blocking cleartext HTTP traffic at the OS level. | • Strip Network State Permissions<br>• Strip Wi-Fi Control Permissions<br>• Strip Push Notification Permissions<br>• Strip Google Services Sync Permissions<br>• Block Cleartext Traffic |
| **Universal Privacy Permissions Stripper** | Selectively strips sensitive privacy, sensor, and hardware permissions from AndroidManifest.xml via configurable boolean toggles. | • Strip Notification Permission<br>• Strip Camera Permission<br>• Strip Microphone Permissions<br>• Strip Storage & Media Permissions<br>• Strip Location Permissions<br>• Strip Contacts & Accounts Permissions<br>• Strip Calendar Permissions<br>• Strip Nearby Devices Permissions<br>• Strip Body Sensors Permissions |
| **Universal Screen Brightness Governor** | Prevents applications from overriding display brightness (such as in-app brightness sliders, barcode/QR full-screen brightness, or window-level overrides) by neutralizing all direct writes to WindowManager.LayoutParams.screenBrightness. |  |
| **Universal Screen Timeout Enforcer** | Forces the target application to respect system screen timeout and sleep timers by neutralizing keepScreenOn view calls and stripping FLAG_KEEP_SCREEN_ON from windows and layout parameters. |  |
| **Universal Screenshot Protection Bypass** | Neutralizes FLAG_SECURE on windows, layout params, and SurfaceViews, unlocks audio playback capture, and suppresses Android 14+ screenshot and screen recording detection callbacks. |  |
| **Universal Telemetry Neutralizer** | Strips advertising and Privacy Sandbox permissions, disables analytics ContentProviders and telemetry background services (Firebase, Sentry, Adjust, AppsFlyer, DataTransport), prunes ComponentDiscovery registrars, and injects telemetry opt-out metadata. | • Revoke Advertising & Tracking Permissions<br>• Disable Telemetry ContentProviders<br>• Disable Telemetry Background Services<br>• Disable Telemetry Receivers<br>• Inject Telemetry Opt-Out Flags & Prune Registrars<br>• Disable Firebase Init Provider |
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
| **Xiaomi Earbuds** | **[Xiaomi Earbuds Guide](docs/apps/xiaomi-earbuds.md)** | Initial pairing workflow, anti-tamper bypass, spatial audio / aptX 96kHz unlocks, and offline isolation. |

### 🌐 Universal & Architecture Reference
| Guide | Description |
| :--- | :--- |
| **[Universal Patches & Options](docs/universal-patches.md)** | Universal debloat & privacy suite: Screen Brightness Governor, Screenshot Protection Bypass, Privacy Permissions Stripper, Telemetry Neutralizer, Native Binary Trimmer, WebP Optimizer, Background Sync Purge, Offline Mode, DPI/Locale Slimmers, and Asset Cleaners. |
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
./gradlew runPatchTest -Papp=<targetApp>   # e.g., brave, gboard, hevy, tiktok, nokoprint, xiaomi_earbuds
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
| <a href="https://github.com/Diego694"><img src="https://github.com/Diego694.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@Diego694</b></a> | 🧪 Daily patch testing, runtime APK validation, and bug diagnostics. |
| <a href="https://github.com/ll0r3nt3"><img src="https://github.com/ll0r3nt3.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@ll0r3nt3</b></a> | 💡 Proposed DPI Resource Slimmer feature request ([#16](https://github.com/kveld9/kveld-morphe-patches/issues/16)). |
| <a href="https://github.com/aidenking2102-dotcom"><img src="https://github.com/aidenking2102-dotcom.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@aidenking2102-dotcom</b></a> | 💡 Proposed TikTok Feed Ad Blocker ([#23](https://github.com/kveld9/kveld-morphe-patches/issues/23)), Playback Speed Setter ([#25](https://github.com/kveld9/kveld-morphe-patches/issues/25)), and Floating Ad Pendant / Sticker Blocker ([#33](https://github.com/kveld9/kveld-morphe-patches/issues/33)) feature requests. |
| <a href="https://github.com/mparvezalam808"><img src="https://github.com/mparvezalam808.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@mparvezalam808</b></a> | 💡 Proposed TikTok Always show publish date, Copy comments without username, Fix Google login, and Show seekbar ([#35](https://github.com/kveld9/kveld-morphe-patches/issues/35)) and TikTok Custom offline videos limit ([#40](https://github.com/kveld9/kveld-morphe-patches/issues/40)) feature requests. |
| <a href="https://github.com/raxelbyte"><img src="https://github.com/raxelbyte.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@raxelbyte</b></a> | 💡 Proposed Gboard Lite Clipboard Enhancements feature request ([#17](https://github.com/kveld9/kveld-morphe-patches/issues/17)). |
| <a href="https://github.com/rafipasya"><img src="https://github.com/rafipasya.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@rafipasya</b></a> | 💡 Proposed TikTok Auto-translate comments feature request ([#48](https://github.com/kveld9/kveld-morphe-patches/issues/48)). |
| <a href="https://github.com/Fahry-a"><img src="https://github.com/Fahry-a.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@Fahry-a</b></a> | 💡 Proposed Brave ARMv7a (32-bit) architecture support feature request ([#50](https://github.com/kveld9/kveld-morphe-patches/issues/50)). |
| <a href="https://github.com/marcoodhb"><img src="https://github.com/marcoodhb.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@marcoodhb</b></a> | 💡 Proposed TikTok Disable double tap to like feature request ([#55](https://github.com/kveld9/kveld-morphe-patches/issues/55)). |
| <a href="https://github.com/miyqwx-dev"><img src="https://github.com/miyqwx-dev.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@miyqwx-dev</b></a> | 💡 Proposed Gboard Lite Hide Incognito Icon toggle feature request. |
| <a href="https://github.com/rdx011"><img src="https://github.com/rdx011.png" width="48" height="48" style="border-radius: 50%;" /><br><b>@rdx011</b></a> | 💡 Proposed Gboard Lite Decouple keyboard vibration from system touch feedback feature request ([#69](https://github.com/kveld9/kveld-morphe-patches/issues/69)). |
<!-- CONTRIBUTORS_END -->

---

## ⚖️ Legal Disclaimer

**Morphe Patches** is an independent, community-driven open-source project and is not affiliated, associated, authorized, endorsed by, or in any way officially connected with Brave Software, Inc., Google LLC, Hevy App, ByteDance Ltd., NokoPrint LLC, Xiaomi Inc., or any of their subsidiaries or affiliates.

All product names, logos, brands, and registered trademarks mentioned in this repository are the property of their respective holders. Their inclusion does not imply affiliation with or endorsement by them.

---

## 📜 License

Morphe Patches is open-source software licensed under the [GNU General Public License v3.0](LICENSE).
