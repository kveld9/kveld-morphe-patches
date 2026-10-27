# TikTok Lite Patches Guide

Technical documentation and patch catalog for TikTok Lite on Android.

---

## 1. Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target App** | TikTok Lite |
| **Package Name** | `com.tiktok.lite.go` |
| **Target Version** | `47.5.52` |
| **Target Version Code** | `470552` |
| **Min SDK** | `21` |
| **Target Package Format** | APKM bundle (`base.apk` + split config APKs) |
| **Primary Architecture** | `arm64-v8a` / `armeabi-v7a` |
| **Download Target** | [APKMirror](https://www.apkmirror.com/apk/tiktok-pte-ltd/tiktok-lite-android-go/) |
| **Authoritative Source** | APKMirror |

---

## 2. Supported Patches Catalog (16 Patches)

The following 16 patches are verified and fully supported on TikTok Lite `47.5.52`:

| Patch Name | Typology | Default State | Dependencies | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Allow Screenshots** | `bytecodePatch` | `true` (Enabled) | None | Removes screenshot and screen recording blocking. |
| **Battery & Resource Saver** | `bytecodePatch` | `true` (Enabled) | None | Stops aggressive video preloading to save battery and data. |
| **Clean Share Links** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Removes tracking parameters from shared links. |
| **Download Without Watermark** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Unlocks downloading and saves videos without watermarks. |
| **In-App Browser Privacy Guard** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Opens web links in your browser and blocks tracking in the remaining web views. |
| **Offline Video Limit** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Customizes the maximum number of videos saved for offline viewing. |
| **SIM Region Selector** | `bytecodePatch` | `false` (Opt-in) | None | Locks the app to a chosen country to unlock regional content. |
| **Show Seekbar** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Restores the video progress seekbar and scrubbing controls. |
| **Block Trackers (Setup)** | `resourcePatch` | `true` (Enabled) | None | Removes tracking permissions and disables telemetry components. |
| **Strip Sensitive Permissions** | `resourcePatch` | `false` (Opt-in) | None | Removes sensitive hardware, sensor, and tracking permissions. |
| **Block Trackers** | `bytecodePatch` | `true` (Enabled) | None | Neutralizes ByteDance AppLog tracking, crash monitors, and analytics. |
| **Bypass Mandatory Login** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Skips the forced login wall via Lite's mini-account gate. |
| **Camera & Microphone Indicator** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Shows a corner mark while TikTok holds camera/mic open. |
| **Disable Push Notifications** | `bytecodePatch` | `false` (Opt-in) | None | Stops background push notifications. |
| **Force Auto-Scroll** | `bytecodePatch` | `true` (Enabled) | None | Enables native auto-scrolling to the next video. |
| **Show Exact Counts** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Shows exact numbers on likes, comments, views and followers. |
| **Stop Video Looping** | `bytecodePatch` | `false` (Opt-in) | None | Stops videos at the end instead of replaying them in an infinite loop. |
| **Disable QUIC** | `bytecodePatch` | `false` (Opt-in) | None | Loads videos, uploads, and live connections over standard HTTPS instead of QUIC. |
| **Playback & Display Governor** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Limits playback resolution and HDR to save battery, and locks the screen refresh rate. |
| **Feed Ads & Content Filter** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Filters ads, shopping tags, live cards, and clutter from your feed. |
| **Friends & Suggestions Filter** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Keeps the Friends feed strictly mutual and hides suggested accounts across the app. |
---

## 3. Non-Applicable on Lite (31 Dropped Patches)

The following 35 patches from the standard TikTok patch suite are excluded on TikTok Lite because the corresponding target classes, components, or assets are absent from the Lite build:

### Group B: Feature Absent / Inert (10 Patches)

- **Always Show Publish Date**: Anchor absent; `VideoItemParams.getAweme()` and `aweme.getCreateTime()` feed item UI hook absent in Lite DEX.
- **Remember Clear Display**: Anchor absent; `CleanModeManager` and `CleanModeService` clear display services absent in Lite DEX.
- **Remember Playback Speed**: Anchor absent; `PlaybackSpeedHelper` and `PlaybackSpeedService` speed persistence helpers absent in Lite DEX.
- **Disable Live Gift Effects**: Anchor absent; `LiveGiftPlayer` and `GiftEffectWidget` live gift rendering widgets absent in Lite DEX.
- **Block P2P Video Relay**: Anchor absent; P2P relay binaries (`libns_p2p.so`, `libbytedance_p2p.so`) absent in Lite APK bundle.
- **Remove Creator Tools**: Anchor absent; `CreatorToolsActivity` and creator studio entrypoints absent in Lite DEX.
- **Remove Live Stream Files**: Anchor absent; live stream assets and `LivePlayActivity` stream player absent in Lite DEX.
- **Remove Voice Tools**: Anchor absent; `VoiceConversionService` and audio filter voice engines absent in Lite DEX.
- **Remove Extra Files**: Anchor absent; stripped APK asset layout in Lite APK, no redundant sample files present.
- **Remove Extra Languages**: Anchor absent; Lite APK is already split into trimmed language bundles.

### Group C: Architecture Divergence / Incompatible Targets (25 Patches)

- **Auto-Pause First Video**: Anchor absent; `PlayerController` class and first-frame playback interception absent in Lite DEX.
- **Block Popups & Prompts**: Anchor absent; `RecUserPopupInMainActivityController.LIZLLL` popup controller absent in Lite DEX.
- **Comment Customizer**: Anchor absent; `comment_sort_opt_style` string and comment sorting logic absent in Lite DEX.
- **Custom Share Sheet**: Anchor absent; `isImFunctionOff` method and custom share sheet layout absent in Lite DEX.
- **Customize Search**: Anchor absent; `RecomDataWrapper` class and search feed injection absent in Lite DEX.
- **Declutter Direct Messages**: Anchor absent; DM / IM messaging components and inbox message protocols absent in Lite DEX.
- **Declutter Navigation Bar**: Anchor absent; `ShopBottomTabProtocol` and shop bottom navigation tab absent in Lite DEX.
- **Declutter Video Interface**: Anchor absent; `UpvoteVideoTrigger.Qp` and declutter anchor classes absent in Lite DEX.
- **Device Privacy Guard**: Anchor absent; `LX/0OBB` device identifier collector class absent in Lite DEX.
- **Disable History Recording**: Anchor absent; `AwemeStatsApi` class and batch watch history reporting absent in Lite DEX.
- **Disable On-Device AI**: Anchor absent; `PitayaBootLoader` class and Pitaya AI engine absent in Lite DEX.
- **Enable Profile Banner**: Anchor absent; `ProfileBackgroundExp` class and profile banner customization absent in Lite DEX.
- **Hide Inbox Promos & Alerts**: Anchor absent; `TopBannerWidgetContainerInjector` and promo banner widgets absent in Lite DEX.
- **Hide Inbox Story Tray**: Anchor absent; `StoryGetFeedByPageResponse` class and story tray container absent in Lite DEX.
- **Instant Launch**: Anchor absent; `SplashAdManagerPreloadTask` and splash ad preloader absent in Lite DEX.
- **Login Compatibility Fix**: Anchor absent; `WebLoginActivity` and third-party login compatibility wrappers absent in Lite DEX.
- **Resume Video After Scroll**: Anchor absent; `ScrollAutoPlayManager` and video resume on scroll handlers absent in Lite DEX.
- **Show Author Region**: Anchor absent; `VideoAuthorInfoVM` and author region metadata badge absent in Lite DEX.
- **Use System Font**: Anchor absent; `TikTokSans-VF.otf` font asset absent in Lite APK bundle.
- **Video Fit Mode**: Anchor absent; `VideoAdaptionResult` class and video aspect ratio adaptation absent in Lite DEX.

---

## 4. Structural Notes

- **Bundle Format**: TikTok Lite is distributed as an APKM bundle with code split across 15 DEX files (77,111 classes).
- **Native Libraries**: TikTok Lite contains `libtt_lib.so`, `liborbuculum.so`, and `libzoin.so`, but lacks `libchrome.so`.
- **Target Pinning**: TikTok Lite pins exclusively version `47.5.52` (`versionCode: 470552`, `minSdk: 21`).

---

## 5. Traffic Audit (coverage-closure, 2026-10-26, whyred, UID-isolated, DoT)

Method: tcpdump on wlan0, per-UID iptables isolation (ACCEPT app UID before DROP 10000-19999),
Private DNS forced to DoT (lab plaintext DNS sinkholes telemetry domains -- documented exercise
limitation for windows 1-3, bypassed from window 4 on). Windows: 60 s baseline (force-stopped)
+ ~200 s exercise on the login wall (fresh install per window, TLS hellos inside the window).
Verdicts per flow (SNI + OUT/IN volumes):

| Flow | Window 4 (pre-5b) | Window 5 (post-iter1) | Window 6 (post-iter2) | Verdict |
|---|---|---|---|---|
| log-boot.tiktokv.com (AppLog settings+register) | 4.7 KB out | gone | gone | SILENCED (AppLogNetworkClient + tryUpdateConfig stubs) |
| log16-normal-no1a.tiktokv.eu (AppLog upload) | 12.2 KB out | gone | gone | SILENCED (LCC synthetic success, flush/reaper stubs) |
| mon-boot.tiktokv.com (APM/SDKMonitor) | 2.4 KB out | 4.6 KB out | 4.8 KB out | SURVIVING GAP (see below) |
| tnc16-normal-useastred.tiktokv.eu (TNC net config) | 3.0 KB out | 2.9 KB out | 2.6 KB out | FUNCTIONAL (Traffic Network Controller config fetch, IN-dominant) |
| Google Analytics trio (manifest) | enabled | disabled | disabled | SILENCED (Setup BLOCKED_COMPONENTS, verified in output manifest) |
| api/gecko/vcs boots, CDN thumbs, FIS push | present | present | present | FUNCTIONAL (login bootstrap, media, push infra) |

Surviving gap: mon-boot APM uploads persist (~5 KB OUT per window) although NpthTask.run/LCCII,
Npth.init and DefaultTTNetImpl.doPost/doGet are all stubbed (gates green, ART PASS). The emitter
reaches mon-boot through a transport path other than DefaultTTNetImpl (MonitorInitTask registers
it for SDKMonitorUtils, but the observed uploads bypass those overloads). Two 5b iterations spent
(max); not widened further blindly. Next step when resumed: XREF the mon-boot/monitorsdk URL
construction sites beyond NpthTask (dynamic `%smonitorsdk.%s` builder) and hook the real transport.
