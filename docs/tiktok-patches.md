# 🎵 TikTok: Technical Patch Specifications & Deep Breakdown

Comprehensive breakdown of the **25 patches** included in the Morphe TikTok patch suite pinned to target version **`46.9.3`** (supporting both `com.zhiliaoapp.musically` Global and `com.ss.android.ugc.trill` Asia APKs).

---

## 📋 Overview of TikTok Patches

| Category | Patch Name | Type | Key Target / Mechanism |
| :--- | :--- | :--- | :--- |
| **Usability** | **Media Usability & Watermark-Free Downloader** | `bytecodePatch` | Forced seekbar scrubbing, unblock download button, unwatermarked stream routing |
| **Usability** | **Playback Speed Persistence** | `bytecodePatch` | Persists user-selected video speed across feed scrolling and restarts |
| **Usability** | **Video Quality Governor** | `bytecodePatch` | Caps maximum video playback resolution (1080p, 720p, 540p, 480p, 360p) to reduce GPU/MediaCodec load and memory retention |
| **Usability** | **Skip First-Launch Onboarding** | `bytecodePatch` | Bypasses interest pickers, swipe-up tutorial, language prompts, and consent sheets directly to FYP feed |
| **Privacy** | **Bypass Mandatory Login** | `bytecodePatch` | Neutralizes mandatory login walls, dynamic regional forced login gates, and guest mode browsing restrictions |
| **Privacy** | **Clean Share URL** | `bytecodePatch` | Strips tracking query parameters, user tokens, and campaign IDs |
| **Privacy** | **Device Privacy Guard** | `bytecodePatch` | Blocks background clipboard inspection, purges 33 invasive permissions, suppresses screenshot/recording triggers, and bypasses FLAG_SECURE |
| **Privacy** | **In-App Browser Privacy Guard** | `bytecodePatch` | Redirects external links to default system browser, neutralizes WebView JS tracking injection and AJAX hookers |
| **Privacy** | **Client-Side AI & Behavioral Profiling Governor** | `bytecodePatch` | Neutralizes Pitaya on-device ML, Tako AI chatbot entries, and AI search clutter |
| **Privacy** | **Region & Geo-Restriction Bypass** | `bytecodePatch` | Spoofs SIM and network country ISO codes to bypass regional restrictions |
| **Privacy** | **Feed Ad Blocker** | `bytecodePatch` | Filters sponsored cards, brand promotions, and commercial audio |
| **Privacy** | **Hide TikTok Shop & Mall** | `bytecodePatch` | Removes product anchors, showcase badges, and bottom/top Shop navigation tabs |
| **Privacy** | **Feed Live Stream Blocker** | `bytecodePatch` | Removes live broadcast cards and live recommendations from FYP and Following |
| **Privacy** | **Feed Bloat & Distraction Blocker** | `bytecodePatch` | Removes friend suggestions, mini-games, CapCut/creation prompts, memories, surveys, mini-dramas, Lemon8 promo |
| **Privacy** | **Unified Telemetry & Tracker Silencer** | `bytecodePatch` | Neutralizes ByteDance AppLog, APM/Npth/Heimdallr crash telemetry, and AppsFlyer |
| **Privacy** | **Update Prompt Suppressor** | `bytecodePatch` | Neutralizes background update polling tasks and device ID check routines |
| **Performance** | **Instant Launch & Splash Blocker** | `bytecodePatch` | Eliminates cold start delays, splash ad tasks, and TopView preload waits (<0.4s) |
| **Performance** | **Resource & Battery Governor** | `bytecodePatch` | Suppresses 3D shake ad sensors, video buffer preloading, and Fresco RAM retention |
| **Performance** | **P2P Video Relay Blocker** | `rawResourcePatch` | Strips `libavmdlp2pv2.so` and `libp2plivevdp.so` to stop background P2P CDN seeding |
| **Performance** | **Disable Push Notifications** | `bytecodePatch` | Neutralizes background push socket polling and persistent wake locks |
| **Performance** | **Live Stream 3D Gift Optimizer** | `bytecodePatch` | Disables 3D gift particle effect engine to eliminate live frame drops |
| **Performance** | **Live Stream Suite Optimizer** | `rawResourcePatch` | Strips `liblink_mic_sdk.so`, Lyrax RTC broadcaster libs, and battle minigames |
| **Slimmer** | **Core Asset De-bloat** | `rawResourcePatch` | Strips Microblink OCR models, C2PA AI libs, ByteDance TTWebView engine, non-Latin fonts, V8 |
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

### 3. Video Quality Governor (`videoQualityGovernorPatch`)
* **Objective**: Enforce user-configured maximum video playback resolution (1080p, 720p, 540p, 480p, 360p) to reduce GPU/MediaCodec load, lower GraphicBuffers RAM retention, eliminate scroll stutter, and control bandwidth.
* **Internal Mechanisms**:
  * **Opt-In & Configurable Resolution Ceiling**:
    * Set to `default = false` (opt-in) with `stringOption("maxQuality")` allowing selection of `1080`, `720`, `540`, `480`, or `360`.
    * Stores configuration in runtime helper `TikTokVideoQualityHook` and persists to `morphe_tiktok_quality_prefs`.
  * **Feed Model & Bitrate List Capping**:
    * Hooks `Video.getBitRate()` and `Video.getRawBitRate()` return points, routing them through `TikTokVideoQualityHook.filterBitrates()`.
    * Discards all rendition ladders exceeding the configured resolution ceiling (`getVideoHeight() > maxAllowedResolution` or parsing `gearName`).
    * Preserves surviving renditions sorted in descending order so the optimal stream within the cap is served first.
  * **Primary Playback Stream Redirection**:
    * Hooks `Aweme.getVideo()`, executing `TikTokVideoQualityHook.capVideoObject(Video)`.
    * Reassigns default playback endpoints (`playAddrValue` and `playAddrBytevc1Value`) to the highest allowed stream within the resolution ceiling.
  * **PlayerKit Engine Bitrate Filtering**:
    * Hooks `SimVideoUrlModel.getBitRate()`, delivering only capped `SimBitRate` candidates to the underlying `TTPlayer` playback engine.
  * **Live Stream Invariant**:
    * Live streams are exempted to prevent audio/video desynchronization on real-time RTMP/WebRTC broadcasts.

### 4. Skip First-Launch Onboarding (`skipFirstLaunchOnboardingPatch`)
* **Objective**: Bypass the entire first-run introduction funnel (interest pickers, swipe-up tutorial, language prompts, and consent sheets) directly to the main feed upon first install.
* **Internal Mechanisms**:
  * **Direct Main Page Assembly**:
    * Hooks `MainPageFragment.JS()Z` -> returns `false`.
    * Ensures `MainPageFragment` directly executes `zS()`, immediately mounting feed video player, navigation, and page assems without entering the deferred onboarding waiting state.
  * **New User Journey Completion & Suppression**:
    * Hooks `NewUserJourneyService` method referencing `did_finish_nuj` (`LIZJ()Z`) -> returns `true` (`did_finish_nuj = true` across all service callers).
    * Hooks `NewUserJourneyService` method referencing `new_user_journey` (`LJJJI(Activity)Z`) -> returns `false` (suppresses evaluating whether to present NUJ).
    * Hooks `NewUserJourneyService` taking `BaseActivity` (`LJIJJLI(BaseActivity)Z`) -> returns `false` (suppresses launching NUJ container activity).
    * Neutralizes `NewUserJourneyService` methods referencing `deeplink_intent_about_welcome_screen` (`LJIJI`) and `reorder_new_journey_front` (`LJJJ`) with immediate `return-void` to prevent launching or reordering the onboarding activity container.

---


## 🛡️ Privacy & Tracker Suppression

### 1. Bypass Mandatory Login (`mandatoryLoginBypassPatch`)
* **Objective**: Neutralize mandatory login walls, dynamic regional forced login gates, and guest browsing restrictions.
* **Internal Mechanisms**:
  * **Forced Login Evaluation Suppression**:
    * Hooks `MandatoryLoginService.shouldShowForcedLogin(Z)Z` -> returns `false`.
    * Hooks `MandatoryLoginService.enableForcedLogin(Z)Z` -> returns `false`.
    * Hooks `MandatoryLoginService.shouldShowLoginTabFirst()Z` -> returns `false` (prevents auto-focusing on login tabs).
  * **Fullscreen Login Wall Neutralization**:
    * Hooks `MandatoryLoginService.tryShowMandatoryLoginPage(...)V` with immediate `return-void` to prevent invoking `SignUpOrLoginActivity` over the feed.

### 2. Clean Share URL (`cleanShareUrlPatch`)
* **Objective**: Protect user privacy when sharing video links with friends or third-party apps.
* **Internal Mechanisms**:
  * Hooks `Aweme.getShareUrl()Ljava/lang/String;` at all return points.
  * Passes the raw URL through `TikTokFeedAdFilter.sanitizeShareUrl()`, which preserves video identifiers while stripping:
    * User tracking: `sec_user_id`, `user_id`, `u_code`.
    * App & session tokens: `share_app_id`, `share_item_id`, `share_link_id`, `ug_source`, `sender_device`.
    * Attribution & marketing telemetry: `utm_source`, `utm_campaign`, `utm_medium`, `_r`, `checksum`, `tt_from`.

### 3. Device Privacy Guard (`devicePrivacyGuardPatch`)
* **Objective**: Prevent background snooping on sensitive device APIs, eliminate annoying screenshot popups, purge invasive Android manifest permissions, block local network scanning, and bypass restrictive `FLAG_SECURE` screen recording blocks.
* **Internal Mechanisms**:
  * **Manifest Permission Purge**:
    * Strips 33 invasive permissions from `AndroidManifest.xml` via companion resource patch, eliminating advertising IDs (`com.google.android.gms.permission.AD_ID`), Google Play Billing (`com.android.vending.BILLING`), precise/coarse GPS location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`), NFC, Bluetooth scanning, audio recording (`RECORD_AUDIO`), fingerprint sensor access, and OEM hardware tracking (Huawei/Xiaomi/Oppo push and analytics tokens).
    * Strips `android.permission.ACCESS_LOCAL_NETWORK` to prevent unauthorized local subnet scanning.
    * Preserves `android.permission.DETECT_SCREEN_CAPTURE` to maintain crash-free compatibility with Android 14+ platform callback registrations.
  * **FLAG_SECURE Bypass (Unrestricted Screen Capture & Recording)**:
    * Neutralizes `LivePcsCourseVideoAntiScreenshotSetting.getValue()Z` -> returns `false`, preventing live courses and paywalled video sessions from enforcing anti-screenshot restrictions.
    * Injects `const/4 p1, 0x0` into `AntiScreenRecordController.applyFlag(Z)V` to permanently clear the window secure flag via native `Window.clearFlags(FLAG_SECURE / 0x2000)`.
    * Injects `const/4 p2, 0x0` into `makeScreenProtection(Landroid/view/Window;Z)V` to force `enableScreenProtection=false`, ensuring `canRecordScreen` evaluates to `true` and clearing secure flags during broadcasts.
  * **Clipboard Snooping Protection**:
    * Neutralizes `IMMessageListClipboardServiceImpl.LIZ()` -> `return-void`.
  * **Screenshot & Screen Recording Detection & Telemetry Suppression**:
    * Neutralizes Lego screenshot task initialization: `ScreenShotTaskHolder$BootFinish`, `ScreenShotFeedbackTaskHolder$BootFinish`, `ScreenShotTask`, `ScreenShotFeedbackTask`, `ScreenRecordingMonitorInitTask`, `InternalShareScreenshotTask`, and `InternalShareScreenshotTaskHolder$BootFinish` -> `return-void`.
    * Neutralizes `ScreenShotFeedbackService.onShot()Z` -> returns `false`.
    * Neutralizes `ScreenShotFeedbackService.safelyShowDialog()` -> `return-void` (eliminates modal dialogs prompting for feedback upon taking a screenshot).
    * Neutralizes `ScreenShotFeedbackService.sendShareFeedbackEvent()` -> `return-void` (suppresses screenshot telemetry reporting).
    * Neutralizes `ScreenShotFeedbackService.isFeedbackEnable()` and `tryShowScreenShotFloatingView()` -> returns `false`.
    * Suppresses quick-share popup modals and Tako AI screenshot triggers.

### 4. In-App Browser Privacy Guard (`inAppBrowserPrivacyGuardPatch`)
* **Objective**: Protect user privacy by redirecting external and third-party web links directly to the user's default system browser (Brave, Firefox, Chrome), preventing third-party browsing sessions from ever running inside TikTok's process, and neutralizing residual tracking inside essential internal WebViews.
* **Internal Mechanisms**:
  * **External Web Navigation Redirection**:
    * Intercepts `SparkThird` container dispatcher (`LX/042u;->LIZIZ()`) at entry, extracting the destination URL and launching an independent `Intent.ACTION_VIEW` with `FLAG_ACTIVITY_NEW_TASK` straight to the system default browser.
    * Intercepts `SparkThirdPopUp` modal dispatcher (`LX/042u;->LIZJ()`) to prevent third-party links from rendering in half-screen web modals, redirecting them externally.
    * Forces `AnchorInfoStruct.getOpenSystemBrowser()` -> returns `true` across commercial and rich ad models (`com.ss.android.ugc.aweme.commercialize.model.feed.anchor` and `com.bytedance.ies.ugc.aweme.rich.model.commercialize.feed.anchor`), delegating ad links to the system browser.
  * **Residual WebView Tracking Neutralization**:
    * Forces the inline JS injection predicate (`webview_inline_inject_js`) -> returns `false` to prevent TikTok from injecting tracking scripts into web views.
    * Neutralizes `WebViewAjaxHooker.onPageStarted()` with `return-void` to block TikTok from hooking into `XMLHttpRequest` and `fetch()` calls inside residual in-app WebViews.
  * **Proprietary Web Engine De-bloat**:
    * Strips ByteDance's proprietary TTWebView engine binary (`libdex_df_ttwebview.so`) and dynamic feature manifest (`df_ttwebview.json`) via `coreAssetDebloatPatch`, forcing any residual hybrid views to rely on the clean, sandboxed system AOSP WebView.

### 5. Client-Side AI & Behavioral Profiling Governor (`clientAiGovernorPatch`)
* **Objective**: Neutralize ByteDance's "Pitaya" on-device machine learning engine, suppress the Tako AI chatbot across feeds and comments, and eradicate AI smart search and summary clutter.
* **Internal Mechanisms**:
  * **On-Device Inference & Behavioral Profiling**:
    * Injects `return-void` into `PitayaBootLoader.setup()` to neutralize on-device ML model initialization.
    * Injects `return-void` into `PitayaBootLoader.commitBootTaskBySettings()` and `BootTask.run()` to halt model downloads and inference tasks.
  * **Tako AI Chatbot Neutralization**:
    * Forces `TakoLaunchServiceImpl.LJ()Z` -> returns `false` to globally kill the Tako framework at launch.
    * Neutralizes `TakoFeedIconServiceImpl.LIZIZ()` -> returns `null` to eliminate the floating Tako icon in the feed.
    * Forces `TakoCommentTopBarServiceImpl.canShow()` -> returns `false` to prevent AI questions and Tako prompts from rendering at the top of comments.
  * **AI Search & Summary Clutter Removal**:
    * Forces `SearchMixFeed.isTako()` -> returns `false` and `getBot()` -> returns `null` to eliminate AI bot result cards.
    * Neutralizes `SearchMixFeed.getAiAdCard()` -> returns `null` to remove AI-generated commerce summary cards.
    * Forces `SearchTakoSugListAssem.bb()` -> returns `false` to suppress Tako suggestion lists.
    * Forces `SearchTakoCardProtocol.zX()`, `SearchTakoNewBotCardProtocol.zX()`, and `SearchAdAISummaryCardProtocol.zX()` -> returns `false`.

### 6. Region & Geo-Restriction Bypass (`regionBypassPatch`)
* **Objective**: Bypass regional content restrictions, geo-blocked feeds, and country-specific account barriers without requiring physical SIM ejection.
* **Internal Mechanisms**:
  * Hooks BPEA telephony abstraction wrapper `LX/067c;->LIZJ(Landroid/telephony/TelephonyManager;LX/019X;)Ljava/lang/String;` (`getSimCountryIso`) to return the user-configured ISO country code.
  * Hooks BPEA telephony abstraction wrapper `LX/067c;->LIZ(Landroid/telephony/TelephonyManager;LX/019X;)Ljava/lang/String;` (`getNetworkCountryIso`) to return the user-configured ISO country code.
  * Configurable via the `region` patch option (defaults to `"CH"`).
  * **Region Selection & Recommendations**: For detailed guidance on picking the best region (e.g. `CH` for minimal e-commerce bloat and full audio catalogs vs `CA`/`US` for North American trends) and avoiding problematic country codes (such as `CN`, `IN`, `RU`, `JP`, or `DE`), see the [SIM Region Selection Guide](patch-configuration.md#sim-region-selector).

### 7. Feed Ad Blocker (`tikTokFeedAdBlockerPatch`)
* **Objective**: Completely clean the For You Page (FYP) and Following feeds from promotional intrusions.
* **Internal Mechanisms**:
  * Bytecode hooks in `FeedApiService.fetchFeedList()`, `FeedItemList.getItems()`, and `FollowFeedList.getItems()`.
  * Delegates feed filtering to runtime helper `TikTokFeedAdFilter`:
    * Evaluates `Aweme.isAd()`, `Aweme.isSoftAd()`, `Aweme.isWithPromotionalMusic()`, and link ad metadata to purge commercial items.

### 8. Hide TikTok Shop & Mall (`hideTikTokShopAnchorsPatch`)
* **Objective**: Completely eliminate shopping distractions by removing product tags, commercial anchors, and the dedicated Shop tab from top and bottom navigation bars.
* **Internal Mechanisms**:
  * **Video Feed Anchor Stripping**: Hooks `FeedApiService.fetchFeedList()`, `FeedItemList.getItems()`, and `FollowFeedList.getItems()` to invoke `TikTokFeedAdFilter.stripCommercialAnchors()`, clearing product tags (`setAnchors(null)`) and showcase links (`setAnchorInfo(null)`).
  * **Shop Bottom Tab Neutralization**: Hooks `ShopBottomTabProtocol.enable()Z` -> returns `false`.
  * **Shop Top Tab Neutralization**: Hooks `ShopTopTabProtocol.enable()Z` -> returns `false`.
  * **Shop Icon & Entry Service Neutralization**: Hooks `ShopIconServiceImpl.rw()Z` -> returns `false`.

### 9. Feed Live Stream Blocker (`feedLiveStreamBlockerPatch`)
* **Objective**: Eliminate live broadcast recommendations and live stream preview cards from the For You Page (FYP) and Following feeds as an independent, modular toggle.
* **Internal Mechanisms**:
  * Bytecode hooks in `FeedApiService.fetchFeedList()`, `FeedItemList.getItems()`, and `FollowFeedList.getItems()`.
  * Delegates live stream filtering to runtime helper `TikTokFeedAdFilter`:
    * Multi-signal detection inspecting `Room`, `RoomFeedCellStruct`, `liveId > 0`, `StreamUrlModel`, `authorLive`, and live aweme types (`101`, `68`, `102`, `69`) as well as Following feed live broadcasts (`feedType == 2`).
    * Purges matching live broadcast cards from feed lists before UI adapter binding.

### 10. Feed Bloat & Distraction Blocker (`feedBloatBlockerPatch`)
* **Objective**: Eliminate non-video clutter, intrusive recommendation cards, Touchpoint Rewards pendants, floating ad stickers, mini-games, creation prompts, surveys, and promotional distraction cards from the For You and Following feeds.
* **Internal Mechanisms**:
  * **Touchpoint Rewards & Ad Pendant Neutralization (Issue #33)**:
    * Injects `return-void` into `SpecActWidget.bind(ViewGroup)` to prevent dynamic floating Rewards activity widgets (such as the floating soccer ball, gift box, or coin countdown sticker) from inflating or attaching to the feed view hierarchy.
    * Injects `return-void` into `SpecActWidget.showOrHidePendant(ZZ)` and `SpecActWidget.showNormalPendant()`.
    * Neutralizes real-time pendant dispatchers `FeedPendantService.LIZ(String)` and `AdPendantService.LIZ(String)` with `return-void`.
  * **In-Video Floating Bloat, Commercial Stickers & Activity Pendants**:
    * Forces `Aweme.getActivityPendant()` -> returns `null`.
    * Forces `Aweme.getCommerceStickerInfo()` -> returns `null` (removes sponsored interactive stickers over video playback).
    * Forces `Aweme.getSpecialSticker()` -> returns `null`.
    * Forces `Aweme.getFloatingCardInfo()` -> returns `null` (blocks floating product/action cards hovering over videos).
    * Forces `Aweme.getBannerTip()` -> returns `null`.
    * Forces `Aweme.getStandardComponentInfo()` -> returns `null`.
  * **Feed Stream & Card Filtering**:
    * Hooks `FeedApiService.fetchFeedList()`, `FeedItemList.getItems()`, and `FollowFeedList.getItems()`.
    * Neutralizes Lego cross-promotion task `Lemon8ServiceInitTask.run(Context)`.
    * Delegates to `TikTokFeedAdFilter.isFeedBloat(Aweme)`:
      * **Suggested Accounts**: `Aweme.getAwemeType() == 4004` (`TTRecUserBigCardViewHolder`), `CardInsertInfo.getCardType() == 49`, `Aweme.isFriendsTabFakeAweme() == true`, `Aweme.getRecommendCardType() > 0`.
      * **Mini-Games**: `Aweme.getAwemeType() == 104` or `CardInsertInfo.getCardType() == 120` (`MiniGameInstantPlayCardVH`).
      * **Creation & CapCut Prompts**: `CardInsertInfo.getCardType() in 188..191` (`CreationFeedCardViewHolder`).
      * **Memories ("On This Day")**: `CardInsertInfo.getCardType() == 127` (`OnThisDayCreationCardViewHolder`).
      * **EOY Recaps & Inspiration**: `CardInsertInfo.getCardType() == 84`, `CardInsertInfo.getCardType() == 176`.
      * **AI Remix & Effects**: `CardInsertInfo.getCardType() == 113`, `CardInsertInfo.getCardType() == 2`.
      * **Surveys & Feedback**: `CardInsertInfo.getCardType() == 4` or `16` (`BottomSurveyAssem`).
      * **Mini-Dramas & Series Promos**: `Aweme.getAwemeType() == 110` (`MiniDramaCard`).
      * **Lynx In-Feed Promos**: `Aweme.getAwemeType() == 106`.
    * Prunes matching cards from list iterators in-situ with zero crashes or UI gaps.

### 11. Unified Telemetry & Tracker Silencer (`unifiedTelemetryTrackerSilencerPatch`)
* **Objective**: Cut off background surveillance, user behavior analytics, and diagnostic reporting to ByteDance servers.
* **Internal Mechanisms**:
  * **ByteDance AppLog**: Neutralizes `AppLog.onEvent()` and `AppLog.report()` entrypoints with immediate `return-void`.
  * **Crash Handlers & Telemetry Schedulers**: Neutralizes Lego initialization tasks for Npth crash reporting (`NpthCoreInitTask`), APM metrics (`ApmInit`), and Heimallr performance monitors.
  * **Attribution Trackers**: Neutralizes `InitAppsFlyer` and Firebase analytics startup initialization tasks.

### 12. Update Prompt Suppressor (`disableInAppUpdateNagsPatch`)
* **Objective**: Prevent forced upgrade popups and version enforcement dialogs.
* **Internal Mechanisms**:
  * Neutralizes Lego update check tasks: `CheckUpdateChangeDeviceIDTaskHolder$Background`, `UpdateTaskHolder$Background`, `CheckUpdateChangeDeviceIDTaskHolder$BootFinish`, and `UpdateTaskHolder$BootFinish`.
  * Injects `return-void` into `UpdateHelper.checkUpdate()`.

---

## ⚡ Performance, RAM & Battery

### 14. Instant Launch & Splash Blocker (`instantColdStartPatch`)
* **Objective**: Accelerate application launch time to sub-second speeds (<0.4s) and bypass startup ads.
* **Internal Mechanisms**:
  * Neutralizes Lego splash tasks: `SplashAdManagerPreloadTask.run()` and `SplashAdManagerPreloadTaskEntry.run()`.
  * Forces `SplashSettingServiceImpl.LIZ()` and `LIZIZ()` -> `return false`.
  * Neutralizes `RealTimeSplashManagerImpl.LIZJ()` -> `return false` (eliminates synchronous waiting for remote TopView splash video assets during cold start).
  * Neutralizes `SplashAdServiceImpl.LJ()`, `LJIILJJIL()`, and `LJJIJIIJIL()` -> `return false` (suppresses splash ad presentation and background fetch).

### 15. Resource & Battery Governor (`resourceGovernorPatch`)
* **Objective**: Eliminate battery-draining background operations, sensor polling, and memory leaks.
* **Internal Mechanisms**:
  * **3D Ad Sensors & Gyroscope Polling**:
    * Neutralizes `ShakeEggService.LIZ()` -> returns `false`.
    * Neutralizes `ShakeEggService.LIZIZ()` -> returns `null`.
  * **Network Traffic & Video Buffer Governor**:
    * Forces `PreloadStrategyConfig.isEnableBufferPreload()Z` -> `return false`, preventing aggressive multi-video background buffer downloads on cellular data.
  * **Memory Retention Governor**:
    * Caps animated bitmap frame caching in Facebook Fresco (`FrescoFrameCache.LIZJ()`, `LJFF()`), preventing OOM crashes during long scrolling sessions.

### 16. P2P Video Relay Blocker (`p2pVideoRelayBlockerPatch`)
* **Objective**: Prevent TikTok from utilizing user device battery, CPU, and cellular data as a distributed P2P edge CDN relay for other users' video streams.
* **Internal Mechanisms**:
  * Zeroes out native P2P video delivery engine: `libavmdlp2pv2.so` (~7.9 MB).
  * Zeroes out native P2P live stream distribution module: `libp2plivevdp.so` (~1.9 MB).
  * Reclaims ~9.83 MB of storage while completely eliminating unmetered peer-to-peer relay network activity.

### 17. Disable Push Notifications (`disablePushNotificationsPatch`)
* **Objective**: Neutralize background push notification tasks and persistent socket wake locks to eliminate background battery drain.
* **Internal Mechanisms**:
  * Neutralizes `InitPushTask.run()` -> disables early startup wake lock acquisition and persistent background push sockets.

### 18. Live Stream 3D Gift Optimizer (`liveGiftEffectOptimizerPatch`)
* **Objective**: Disable Live 3D gift particle effect engine and widget rendering lifecycle to eliminate frame drops during live streams.
* **Internal Mechanisms**:
  * Injects `return-void` into `LiveGiftEffectWidget.initView()` and `LiveGiftEffectWidget.onCreate()`.

### 19. Live Stream Suite Optimizer (`liveStreamSuiteOptimizerPatch`)
* **Objective**: Strip unneeded interactive streaming libraries, live RTC broadcaster SDKs, and minigame assets for users who only watch regular videos or standard live streams.
* **Internal Mechanisms**:
  * Zeroes out native interactive streaming binaries: `liblink_mic_sdk.so`.
  * Zeroes out native Lyrax live streaming and RTC broadcaster engines: `liblyrax.so` (~48 MB) and `liblyrax_plugin.so` (~2.5 MB).
  * Removes interactive live stream battle minigame assets.
  * Reclaims over 50.45 MB of storage.

---

## 📦 APK Size & Resource Slimming

### 20. Core Asset De-bloat (`coreAssetDebloatPatch`)
* **Objective**: Strip unneeded third-party SDK models, embedded engines, FinTech/card scanners, and client AI runtime binaries across both `arm64-v8a` and `armeabi-v7a`.
* **Internal Mechanisms**:
  * Strips embedded Microblink credit card OCR models (`assets/microblink/`).
  * Zeroes out TikTok Shop FinTech & Card Scanner libraries (`libBlinkCard.so`, `libdex_df_pipo_bnpl.so`, `libdex_df_ccdc_impl_ocr.so`, `libdex_df_pipo_external_payments.so`, `libpipo-security-sdk.so`) and checkout UI models (`assets/pipo_ui_default_checkout.json`, `assets/pipo_ui_default_components.json`).
  * Zeroes out Pitaya AI native libraries (`libAndroidPitayaCore.so`, `libAndroidPitayaProxy.so`, `libPitayaBdComponent.so`, `libPitayaTTPPolicy.so`, `libdex_df_pitaya.so`).
  * Zeroes out ByteDance Local LLM and Gemini Nano libraries (`libbytennllm.so`, `libbytennllm-jni.so`, `libdex_df_gemini_nano.so`), while preserving core `libbytenn.so` and `libbytennwrapper.so` required for native audio effects (`libaudioeffect.so`) and Starship SDK (`libstarship_sdk.so`).
  * Zeroes out SSDP/DLNA local network scanner binary (`libdex_df_live_cast.so`).
  * Zeroes out C2PA AI content origin verification libraries (`libtt_c2pa_sdk.so`, `libtt_c2pa_sdk_d.so`).
  * Zeroes out internal diagnostic, profiling, and Python runtime libraries (`libreschecker.so`, `libpy-cv-numpycv.so`, `libpythonA.so`, `libpy-numpy.so`, `libBDMicroPythonVM.so`, `libBDPythonVM.so`).
  * Prunes non-Latin and exotic bundled fonts (`assets/fonts/` such as Greek, Hebrew, Armenian, Georgian, Khmer, Lao, Myanmar, Thai).
  * Reclaims over 77 MB of uncompressed APK storage.

### 21. Studio & Creation De-bloat (`creatorBloatSlimmerPatch`)
* **Objective**: Strip camera AR face filters, CapCut NLE video editing SDKs, and creation plugins across both `arm64-v8a` and `armeabi-v7a` for feed-only users.
* **Internal Mechanisms**:
  * Zeroes out camera AR effects engine: `libeffect_plugin.so`, `libEffectCreatorJni.so`.
  * Zeroes out CapCut-like video editor SDK: `libttvesdk_plugin.so`.
  * Zeroes out ILA Material SDK: `libILAMaterialSDK.so`.
  * Dynamically detects and zeroes out all bundled CapCut NLE native libraries (`libNLEMediaPublicJni.so`, `libNLEEditorJni.so`, `libNLETemplateModelJni.so`, `libNLEMediaJni.so`, etc.).
  * Purges bundled face mesh and facial landmark tracking models (`assets/model/ttfacemodel`).
  * Reclaims over 101 MB of uncompressed APK storage.

### 22. Language Pack Purger (`localeSlimmerPatch`)
* **Objective**: Reclaim substantial APK storage by stripping unused localized strings while preserving user-selected languages (default: English).
* **Internal Mechanisms**:
  * Scans `assets/strings#lang_*` bundles and removes unselected translation files.
  * Preserves default app language to ensure zero missing resource exceptions.
  * Configurable via the `locales` option (defaults to preserving English: `"en"`).

