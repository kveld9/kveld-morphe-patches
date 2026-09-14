# 🎵 TikTok: Technical Patch Specifications & Deep Breakdown

Comprehensive breakdown of the **17 patches** included in the Morphe TikTok patch suite pinned to target version **`46.9.3`** (supporting both `com.zhiliaoapp.musically` Global and `com.ss.android.ugc.trill` Asia APKs).

---

## 📋 Overview of TikTok Patches

| Category | Patch Name | Type | Key Target / Mechanism |
| :--- | :--- | :--- | :--- |
| **Usability** | **Media Usability & Watermark-Free Downloader** | `bytecodePatch` | Forced seekbar scrubbing, unblock download button, unwatermarked stream routing |
| **Usability** | **Playback Speed Persistence** | `bytecodePatch` | Persists user-selected video speed across feed scrolling and restarts |
| **Privacy** | **Clean Share URL** | `bytecodePatch` | Strips tracking query parameters, user tokens, and campaign IDs |
| **Privacy** | **Device Privacy Guard** | `bytecodePatch` | Blocks background clipboard inspection and suppresses screenshot/recording triggers |
| **Privacy** | **Feed Ad Blocker** | `bytecodePatch` | Filters sponsored cards, brand promotions, and commercial audio |
| **Privacy** | **Hide TikTok Shop Anchors** | `bytecodePatch` | Removes product showcase badges, shopping cart tags, and shop anchors |
| **Privacy** | **Feed Live Stream Blocker** | `bytecodePatch` | Removes live broadcast cards and live recommendations from FYP and Following |
| **Privacy** | **Unified Telemetry & Tracker Silencer** | `bytecodePatch` | Neutralizes ByteDance AppLog, APM/Npth/Heimdallr crash telemetry, and AppsFlyer |
| **Privacy** | **Update Prompt Suppressor** | `bytecodePatch` | Neutralizes background update polling tasks and device ID check routines |
| **Performance** | **Instant Launch & Splash Blocker** | `bytecodePatch` | Eliminates cold start delays, splash ad tasks, and TopView preload waits (<0.4s) |
| **Performance** | **Resource & Battery Governor** | `bytecodePatch` | Suppresses 3D shake ad sensors, video buffer preloading, and Fresco RAM retention |
| **Performance** | **Disable Push Notifications** | `bytecodePatch` | Neutralizes background push socket polling and persistent wake locks |
| **Performance** | **Live Stream 3D Gift Optimizer** | `bytecodePatch` | Disables 3D gift particle effect engine to eliminate live frame drops |
| **Performance** | **Live Stream SDK & Minigame De-bloat** | `rawResourcePatch` | Strips `liblink_mic_sdk.so` and live stream interactive battle minigames |
| **Slimmer** | **Core Asset De-bloat** | `rawResourcePatch` | Strips Microblink OCR card models, C2PA AI verification libs, non-Latin fonts, V8 engines |
| **Slimmer** | **Studio & Creation De-bloat** | `rawResourcePatch` | Strips AR camera engine (`libeffect_plugin.so`) and video editor SDK (`libttvesdk_plugin.so`) |
| **Slimmer** | **Language Pack Purger** | `rawResourcePatch` | Strips unselected language string bundles from `assets/strings#lang_*` |

---

## 🎬 Usability & Media

### 1. Media Usability & Watermark-Free Downloader (`mediaEnhancementsPatch`)
* **Objective**: Unlock core playback and saving features that creators or the platform restrict.
* **Internal Mechanisms**:
  * **Forced Download Button Unblock**:
    * Hooks `Aweme.isPreventDownload()Z` -> returns `false`.
    * Invokes `TikTokMediaHook` to dynamically flip internal `canDownload` boolean fields via reflection, restoring the "Save video" action in the share modal.
  * **Clean Watermark-Free Downloads**:
    * Hooks `Aweme.getDownloadWithoutWatermark()Z` -> returns `true`.
    * Hooks `Aweme.needTTSWatermarkWhenDownload()Z` -> returns `false` (suppresses text-to-speech audio watermark stamps).
    * Hooks `Video.getDownloadAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;`: Uses `TikTokMediaHook.getWatermarkFreeDownloadUrl()` to extract the clean, original `playAddr` media stream URLs, bypassing the server-side watermark render queue.
  * **Client-side Watermark Neutralization**:
    * Neutralizes `WaterMarkServiceImpl.waterMark()` with immediate `return-void` to prevent client-rendered overlay compositing.

### 2. Playback Speed Persistence (`playbackSpeedPatch`)
* **Objective**: Persist user-selected video playback speed across feed scrolling, video transitions, and app cold restarts.
* **Internal Mechanisms**:
  * **Feed Video Playback Interception**:
    * Hooks `Aweme.getParameterizedSpeed()F` at return points, querying `TikTokSpeedHook.getPlaybackSpeed(Aweme)`.
    * If video speed is default or unadjusted, returns the user's persisted speed preference.
    * Live streams are identified via `TikTokFeedAdFilter.isLiveStream(Aweme)` and constrained to `1.0f` to prevent audio/video desynchronization.
  * **Native Speed Dialog & Selection Sync**:
    * Hooks native speed selection handler (`LX/085j;->LJ(FLcom/ss/android/ugc/aweme/feed/model/Aweme;Ljava/lang/String;Ljava/lang/String;)V`), routing user speed selections to `TikTokSpeedHook.onSpeedSelected(float)`.
    * Intercepts speed querying methods in playback speed dialog (`LIZIZ(Aweme)F` and `LIZJ`) to keep TuxSheet UI radio buttons and bottom-bar badges synchronized with the persisted speed.
  * **Persistent Storage**:
    * Stores current speed in dedicated `SharedPreferences` (`morphe_tiktok_speed_prefs`), restoring preference upon cold launch.

---

## 🛡️ Privacy & Tracker Suppression

### 3. Clean Share URL (`cleanShareUrlPatch`)
* **Objective**: Protect user privacy when sharing video links with friends or third-party apps.
* **Internal Mechanisms**:
  * Hooks `Aweme.getShareUrl()Ljava/lang/String;` at all return points.
  * Passes the raw URL through `TikTokFeedAdFilter.sanitizeShareUrl()`, which preserves video identifiers while stripping:
    * User tracking: `sec_user_id`, `user_id`, `u_code`.
    * App & session tokens: `share_app_id`, `share_item_id`, `share_link_id`, `ug_source`, `sender_device`.
    * Attribution & marketing telemetry: `utm_source`, `utm_campaign`, `utm_medium`, `_r`, `checksum`, `tt_from`.

### 4. Device Privacy Guard (`devicePrivacyGuardPatch`)
* **Objective**: Prevent background snooping on sensitive device APIs and eliminate annoying screenshot popups.
* **Internal Mechanisms**:
  * **Clipboard Snooping Protection**:
    * Neutralizes `IMMessageListClipboardServiceImpl.LIZ()` -> `return-void`.
  * **Screenshot & Screen Recording Detection Suppression**:
    * Neutralizes Lego screenshot task initialization: `ScreenShotTaskHolder$BootFinish`, `ScreenShotFeedbackTaskHolder$BootFinish`, `ScreenShotTask`, `ScreenShotFeedbackTask`, and `ScreenRecordingMonitorInitTask` -> `return-void`.
    * Neutralizes `ScreenShotFeedbackService.onShot()Z` -> returns `false`.
    * Neutralizes `ScreenShotFeedbackService.safelyShowDialog()` -> `return-void` (eliminates modal dialogs prompting for feedback upon taking a screenshot).

### 5. Feed Ad Blocker (`tikTokFeedAdBlockerPatch`)
* **Objective**: Completely clean the For You Page (FYP) and Following feeds from promotional intrusions.
* **Internal Mechanisms**:
  * Bytecode hooks in `FeedApiService.fetchFeedList()`, `FeedItemList.getItems()`, and `FollowFeedList.getItems()`.
  * Delegates feed filtering to runtime helper `TikTokFeedAdFilter`:
    * Evaluates `Aweme.isAd()`, `Aweme.isSoftAd()`, `Aweme.isWithPromotionalMusic()`, and link ad metadata to purge commercial items.

### 6. Hide TikTok Shop Anchors (`hideTikTokShopAnchorsPatch`)
* **Objective**: Remove product tags, shopping showcase cards, and TikTok Shop commercial anchors from video posts in the feed as an independent, modular toggle.
* **Internal Mechanisms**:
  * Bytecode hooks in `FeedApiService.fetchFeedList()`, `FeedItemList.getItems()`, and `FollowFeedList.getItems()`.
  * Delegates shopping anchor stripping to runtime helper `TikTokFeedAdFilter`:
    * Invokes `stripCommercialAnchors()` on video items to clear product anchors (`setAnchors(null)`) and showcase links (`setAnchorInfo(null)`).

### 7. Feed Live Stream Blocker (`feedLiveStreamBlockerPatch`)
* **Objective**: Eliminate live broadcast recommendations and live stream preview cards from the For You Page (FYP) and Following feeds as an independent, modular toggle.
* **Internal Mechanisms**:
  * Bytecode hooks in `FeedApiService.fetchFeedList()`, `FeedItemList.getItems()`, and `FollowFeedList.getItems()`.
  * Delegates live stream filtering to runtime helper `TikTokFeedAdFilter`:
    * Inspects video items for `Aweme.isLive() == true` or `Aweme.getAwemeType() == 101`.
    * Purges matching live broadcast cards from feed lists before UI adapter binding.

### 8. Unified Telemetry & Tracker Silencer (`unifiedTelemetryTrackerSilencerPatch`)
* **Objective**: Cut off background surveillance, user behavior analytics, and diagnostic reporting to ByteDance servers.
* **Internal Mechanisms**:
  * **ByteDance AppLog**: Neutralizes `AppLog.onEvent()` and `AppLog.report()` entrypoints with immediate `return-void`.
  * **Crash Handlers & Telemetry Schedulers**: Neutralizes Lego initialization tasks for Npth crash reporting (`NpthCoreInitTask`), APM metrics (`ApmInit`), and Heimallr performance monitors.
  * **Attribution Trackers**: Neutralizes `InitAppsFlyer` and Firebase analytics startup initialization tasks.

### 9. Update Prompt Suppressor (`disableInAppUpdateNagsPatch`)
* **Objective**: Prevent forced upgrade popups and version enforcement dialogs.
* **Internal Mechanisms**:
  * Neutralizes Lego update check tasks: `CheckUpdateChangeDeviceIDTaskHolder$Background`, `UpdateTaskHolder$Background`, `CheckUpdateChangeDeviceIDTaskHolder$BootFinish`, and `UpdateTaskHolder$BootFinish`.
  * Injects `return-void` into `UpdateHelper.checkUpdate()`.

---

## ⚡ Performance, RAM & Battery

### 10. Instant Launch & Splash Blocker (`instantColdStartPatch`)
* **Objective**: Accelerate application launch time to sub-second speeds (<0.4s) and bypass startup ads.
* **Internal Mechanisms**:
  * Neutralizes Lego splash tasks: `SplashAdManagerPreloadTask.run()` and `SplashAdManagerPreloadTaskEntry.run()`.
  * Forces `SplashSettingServiceImpl.LIZ()` and `LIZIZ()` -> `return false`.
  * Neutralizes `RealTimeSplashManagerImpl.LIZJ()` -> `return false` (eliminates synchronous waiting for remote TopView splash video assets during cold start).
  * Neutralizes `SplashAdServiceImpl.LJ()`, `LJIILJJIL()`, and `LJJIJIIJIL()` -> `return false` (suppresses splash ad presentation and background fetch).

### 11. Resource & Battery Governor (`resourceGovernorPatch`)
* **Objective**: Eliminate battery-draining background operations, sensor polling, and memory leaks.
* **Internal Mechanisms**:
  * **3D Ad Sensors & Gyroscope Polling**:
    * Neutralizes `ShakeEggService.LIZ()` -> returns `false`.
    * Neutralizes `ShakeEggService.LIZIZ()` -> returns `null`.
  * **Network Traffic & Video Buffer Governor**:
    * Forces `PreloadStrategyConfig.isEnableBufferPreload()Z` -> `return false`, preventing aggressive multi-video background buffer downloads on cellular data.
  * **Memory Retention Governor**:
    * Caps animated bitmap frame caching in Facebook Fresco (`FrescoFrameCache.LIZJ()`, `LJFF()`), preventing OOM crashes during long scrolling sessions.

### 12. Disable Push Notifications (`disablePushNotificationsPatch`)
* **Objective**: Neutralize background push notification tasks and persistent socket wake locks to eliminate background battery drain.
* **Internal Mechanisms**:
  * Neutralizes `InitPushTask.run()` -> disables early startup wake lock acquisition and persistent background push sockets.

### 13. Live Stream 3D Gift Optimizer (`liveGiftEffectOptimizerPatch`)
* **Objective**: Disable Live 3D gift particle effect engine and widget rendering lifecycle to eliminate frame drops during live streams.
* **Internal Mechanisms**:
  * Injects `return-void` into `LiveGiftEffectWidget.initView()` and `LiveGiftEffectWidget.onCreate()`.

### 14. Live Stream SDK & Minigame De-bloat (`liveStreamSuiteOptimizerPatch`)
* **Objective**: Strip unneeded native libraries and assets for users who only watch regular videos or standard live streams.
* **Internal Mechanisms**:
  * Zeroes out native interactive streaming binaries: `liblink_mic_sdk.so`.
  * Removes interactive live stream battle minigame assets.

---

## 📦 APK Size & Resource Slimming

### 15. Core Asset De-bloat (`coreAssetDebloatPatch`)
* **Objective**: Strip unneeded third-party SDK models and embedded engines.
* **Internal Mechanisms**:
  * Strips embedded Microblink credit card and document OCR scanning models (`assets/microblink/`).
  * Zeroes out C2PA AI content origin verification libraries (`libtt_c2pa_sdk.so`).
  * Prunes non-Latin and exotic bundled fonts (`assets/fonts/` such as Khmer, Lao, Myanmar, Burmese).
  * Zeroes out embedded V8 JavaScript runtimes for TikTok mini-apps (`libminiapp`, `libv8`).

### 16. Studio & Creation De-bloat (`creatorBloatSlimmerPatch`)
* **Objective**: Massive APK reduction for consumption-only users (>22 MB saved).
* **Internal Mechanisms**:
  * Zeroes out the camera AR effect rendering engine: `libeffect_plugin.so`.
  * Zeroes out the built-in CapCut-derived video editing and encoding suite: `libttvesdk_plugin.so`.
  * Prunes facial mesh and AR tracking models.

### 17. Language Pack Purger (`localeSlimmerPatch`)
* **Objective**: Remove unwanted translations from `assets/strings#lang_*`.
* **Internal Mechanisms**:
  * Parses localized string bundles and deletes unselected language packs.
  * Configurable via the `languages` option (defaults to preserving English: `"en"`).
