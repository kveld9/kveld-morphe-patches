# 🎵 TikTok: Technical Patch Specifications & Deep Breakdown

Comprehensive breakdown of the **31 patches** included in the Morphe TikTok patch suite pinned to target version **`46.9.3`** (supporting both `com.zhiliaoapp.musically` Global and `com.ss.android.ugc.trill` Asia APKs).

---

## 📋 Overview of TikTok Patches

| Category | Patch Name | Type | Key Target / Mechanism |
| :--- | :--- | :--- | :--- |
| **Usability** | **Media Usability & Watermark-Free Downloader** | `bytecodePatch` | Unblock download button for posts & Stories in Share panel, unwatermarked stream routing |
| **Usability** | **Show seekbar** | `bytecodePatch` | Restores video seekbar and scrubbing controls where normally hidden or disabled |
| **Usability** | **Always show publish date** | `bytecodePatch` | Forces video publish and upload timestamps to remain visible across all feed cards |
| **Usability** | **Copy comments without username** | `bytecodePatch` | Sanitizes comment copy actions to exclude the prepended author username |
| **Usability** | **Playback Speed Persistence** | `bytecodePatch` | Persists user-selected video speed across feed scrolling and restarts |
| **Usability** | **Video Quality Governor** | `bytecodePatch` | Enforces independent resolution ceilings for playback (e.g. 480p) and downloads (e.g. 1080p, 720p, or uncapped) |
| **Usability** | **Skip First-Launch Onboarding** | `bytecodePatch` | Bypasses interest pickers, swipe-up tutorial, language prompts, and consent sheets directly to FYP feed |
| **Privacy** | **Fix Google login** | `bytecodePatch` | Restores Google account sign-in via Web OAuth fallback when GMS rejects modified APK signature |
| **Privacy** | **Bypass Mandatory Login** | `bytecodePatch` | Neutralizes mandatory login walls, dynamic regional forced login gates, and guest mode browsing restrictions |
| **Privacy** | **Clean Share URL** | `bytecodePatch` | Strips tracking query parameters, user tokens, and campaign IDs |
| **Privacy** | **Device Privacy Guard** | `bytecodePatch` | Blocks background clipboard inspection, purges 35 invasive permissions, prunes 92 external package query declarations, silences HAR hardware sensors, and bypasses FLAG_SECURE |
| **Privacy** | **Ghost Mode** | `bytecodePatch` | Enables anonymous profile, story, and conversation browsing: suppresses outbound view records, story view pings, and typing indicators |
| **Privacy** | **In-App Browser Privacy Guard** | `bytecodePatch` | Redirects external links to default system browser, neutralizes WebView JS tracking injection and AJAX hookers |
| **Privacy** | **Client-Side AI & Behavioral Profiling Governor** | `bytecodePatch` | Neutralizes Pitaya on-device ML, Tako AI chatbot entries, and AI search clutter |
| **Privacy** | **Region & Geo-Restriction Bypass** | `bytecodePatch` | Spoofs SIM and network country ISO codes to bypass regional restrictions |
| **Privacy** | **Feed Ad Blocker** | `bytecodePatch` | Filters sponsored cards, brand promotions, and commercial audio |
| **Privacy** | **Hide TikTok Shop & Mall** | `bytecodePatch` | Removes product anchors, showcase badges, and bottom/top Shop navigation tabs |
| **Privacy** | **Feed Live Stream Blocker** | `bytecodePatch` | Removes live broadcast cards and live recommendations from FYP and Following |
| **Privacy** | **Feed Bloat & Distraction Blocker** | `bytecodePatch` | Removes friend suggestions, mini-games, CapCut/creation prompts, memories, surveys, mini-dramas, Lemon8 promo |
| **Privacy** | **Unified Telemetry & Tracker Silencer** | `bytecodePatch` | Neutralizes ByteDance AppLog, APM/Npth/Heimdallr crash telemetry, and AppsFlyer |
| **Privacy** | **Update Prompt Suppressor** | `bytecodePatch` | Neutralizes background update polling tasks and device ID check routines |
| **Performance** | **Display Refresh Rate Governor** | `bytecodePatch` | Forces TikTok to run at peak display refresh rate (120Hz/90Hz/60Hz) and neutralizes video playback framerate downclocking |
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
* **Objective**: Unlock core playback, story saving, and media download features that creators or the platform restrict.
* **Internal Mechanisms**:
  * **Forced Download Button Unblock**:
    * Hooks `Aweme.isPreventDownload()Z` -> returns `false`.
    * Hooks `Aweme.getIsCommentPostVideo()Z` -> returns `false`.
    * Invokes `TikTokMediaHook` to dynamically flip internal `canDownload` boolean fields via reflection, restoring the "Save video" action in the share modal.
    * Hooks `AwemeExtKt.isSharedStoryVisible(Aweme;)Z` -> returns `true`.
    * Intercepts Share panel action builder (`LX/0HGG;->LJJI()`) and neutralizes all early `return-void` guards (story type 45, 46, 180, 181, comment video, and restriction checks) prior to action instantiation, ensuring the "Save video" button (`LX/0HJb`) is unconditionally constructed and appended to the Share panel for Stories.
    * Hooks download action `LX/0HJb;->enable()Z` and `now_save` action -> returns `true`, guaranteeing the Save action in the Share panel is always clickable and active rather than grayed out.
  * **Clean Watermark-Free Downloads**:
    * Hooks `Aweme.getDownloadWithoutWatermark()Z` -> returns `true`.
    * Hooks `Aweme.needTTSWatermarkWhenDownload()Z` -> returns `false` (suppresses text-to-speech audio watermark stamps).
    * Hooks `Video.getDownloadAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;`: Uses `TikTokMediaHook.getWatermarkFreeDownloadUrl()` to extract the clean, original `playAddr` media stream URLs, prioritizing uncapped high-resolution streams cached by Video Quality Governor, bypassing the server-side watermark render queue.
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
* **Objective**: Enforce user-configured maximum video playback and download resolution ceilings independently (1080p, 720p, 540p, 480p, or uncapped) to reduce GPU/MediaCodec load, lower GraphicBuffers RAM retention, eliminate scroll stutter, and conserve bandwidth while preserving maximum quality for offline downloads.
* **Internal Mechanisms**:
  * **Opt-In & Decoupled Resolution Ceilings**:
    * Configurable via `stringOption("maxQuality")` (playback ceiling, default: `480`) and `stringOption("maxDownloadQuality")` (download ceiling, default: `1080`), supporting `1080`, `720`, `540`, `480`, or `none`.
    * Stores configuration in runtime helper `TikTokVideoQualityHook` and persists to `morphe_tiktok_quality_prefs`.
  * **Feed Model & Bitrate List Capping**:
    * Before mutating `Video` candidate streams for PlayerKit playback, `TikTokVideoQualityHook.capVideoObject(Video)` caches the best available stream adhering to the download ceiling in `uncappedDownloadAddrs`.
    * Hooks `Video.getBitRate()` and `Video.getRawBitRate()` return points, routing them through `TikTokVideoQualityHook.filterBitrates()`.
    * Discards all rendition ladders exceeding the playback resolution ceiling (`getVideoHeight() > maxAllowedResolution` or parsing `gearName`).
    * Preserves surviving renditions sorted in descending order so the optimal stream within the cap is served first.
  * **Primary Playback Stream Redirection**:
    * Hooks `Aweme.getVideo()`, executing `TikTokVideoQualityHook.capVideoObject(Video)`.
    * Reassigns default playback endpoints (`playAddrValue` and `playAddrBytevc1Value`) to the highest allowed stream within the resolution ceiling.
  * **Decoupled High-Fidelity Downloader Integration**:
    * `TikTokMediaHook` queries `TikTokVideoQualityHook.getBestDownloadPlayAddr(video)` during download URL extraction, ensuring downloaded files maintain full 1080p/720p fidelity even when playback is capped at 480p.
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

### 5. Show Seekbar (`showSeekbarPatch`)
* **Objective**: Restores TikTok's native video seekbar and scrubbing controls where creators or the platform have hidden or disabled them.
* **Internal Mechanisms**:
  * **Global Feed Seekbar Visibility Predicate**:
    * Hooks the feed `ShouldShowProgressBar(Aweme)Z` method.
    * Checks if `Aweme != null` and returns `true`, ensuring the seekbar container is mounted and visible across feed videos.
  * **Seekbar State Machine Override**:
    * Intercepts `setSeekBarShowType(I)V` (matching string literal `"seekbar show type change, change to:"`).
    * Rewrites hidden states (`3` and `4`) to `0` (visible and scrubbing enabled), restoring full progress track display and dragging functionality.

### 6. Always Show Publish Date (`alwaysShowPublishDatePatch`)
* **Objective**: Forces video publish and upload timestamps to remain permanently visible in video author metadata across all feed types.
* **Internal Mechanisms**:
  * **Author Info State Synchronization**:
    * Targets `VideoAuthorInfoVM.paramSync2StateAccept` taking `VideoItemParams`.
    * Locates the post-time visibility region bounded by `"v3"` and `getCreateTime()J`.
    * Traverses static gate predicate invocations (`INVOKE_STATIC ... -> (Ljava/lang/String;)Z`) and injects `const/4 vX, 0x1` immediately after their `move-result`, guaranteeing publish timestamps are rendered.

### 7. Copy Comments Without Username (`copyCommentsWithoutUsernamePatch`)
* **Objective**: Copies only the comment text to the Android clipboard without prepending the author's username (e.g. `Author: Comment text` -> `Comment text`).
* **Internal Mechanisms**:
  * **Clipboard Helper Discovery**:
    * Dynamically locates the BPEA clipboard helper method calling `ClipData.newPlainText(CharSequence, CharSequence)`.
  * **Comment Copy Builder Interception**:
    * Matches methods referencing `Comment.getText()` and invoking the clipboard helper.
    * Hooks `Comment.getText()` to capture the original unformatted comment text into `TikTokCommentHook.captureCommentText(String)`.
    * Hooks the clipboard helper invocation call site directly, sanitizing the text to be copied via `TikTokCommentHook.sanitizeCopiedComment(String)` to strip the author prefix with zero stack frame perturbation.

---


## 🛡️ Privacy & Tracker Suppression

### 1. Fix Google Login (`fixGoogleLoginPatch`)
* **Objective**: Restores Google account sign-in functionality after APK modification and re-signing by forcing TikTok to fall back to Web-based OAuth authentication.
* **Internal Mechanisms**:
  * **Play Services Signature Verification Bypass**:
    * Modified APKs fail Google Play Services SHA-256 fingerprint validation when attempting native GMS login (`GoogleAuth`).
    * Hooks `com.bytedance.lobby.google.GoogleAuth.isAvailable()Z` -> returns `false`.
    * Hooks `com.bytedance.lobby.google.GoogleOneTapAuth.isAvailable()Z` -> returns `false`.
    * Forces TikTok's Lobby authentication dispatcher to automatically route Google sign-in requests through the secure Custom Tabs / Web OAuth flow, which succeeds regardless of the APK's signing certificate.

### 2. Bypass Mandatory Login (`mandatoryLoginBypassPatch`)
* **Objective**: Neutralize mandatory login walls, dynamic regional forced login gates, and guest browsing restrictions.
* **Internal Mechanisms**:
  * **Forced Login Evaluation Suppression**:
    * Hooks `MandatoryLoginService.shouldShowForcedLogin(Z)Z` -> returns `false`.
    * Hooks `MandatoryLoginService.enableForcedLogin(Z)Z` -> returns `false`.
    * Hooks `MandatoryLoginService.shouldShowLoginTabFirst()Z` -> returns `false` (prevents auto-focusing on login tabs).
  * **Fullscreen Login Wall Neutralization & Playback Resumption**:
    * Hooks `MandatoryLoginService.tryShowMandatoryLoginPage(...)V` to dispatch `TikTokLoginHook.notifyLoginResult(listener)` with `onResult(1, 2, null)` on the main Looper before `return-void`. This dismisses login wait states immediately and signals `VideoViewComponent` to resume video playback without stalling for watchdog timeouts when switching tabs.
  * **Authoritative Guest Mode State Resolution**:
    * Hooks `GuestModeServiceImpl.isGuestMode()Z` to delegate to `TikTokLoginHook.isGuestMode()`, querying `AccountUserService.isLogin()` via reflection. This decouples guest browsing validity from skipped first-launch onboarding / Keva age gate consent flags.

### 3. Clean Share URL (`cleanShareUrlPatch`)
* **Objective**: Protect user privacy when sharing video links with friends or third-party apps.
* **Internal Mechanisms**:
  * Hooks `Aweme.getShareUrl()Ljava/lang/String;` at all return points.
  * Passes the raw URL through `TikTokFeedAdFilter.sanitizeShareUrl()`, which preserves video identifiers while stripping:
    * User tracking: `sec_user_id`, `user_id`, `u_code`.
    * App & session tokens: `share_app_id`, `share_item_id`, `share_link_id`, `ug_source`, `sender_device`.
    * Attribution & marketing telemetry: `utm_source`, `utm_campaign`, `utm_medium`, `_r`, `checksum`, `tt_from`.

### 4. Device Privacy Guard (`devicePrivacyGuardPatch`)
* **Objective**: Prevent background snooping on sensitive device APIs, eliminate annoying screenshot popups, purge invasive Android manifest permissions, block local network scanning, prune cross-app package inspection, isolate contact queries, silence continuous physical motion sensors, and bypass restrictive `FLAG_SECURE` screen recording blocks.
* **Internal Mechanisms**:
  * **Manifest Permission Purge & Package Query Pruning**:
    * Strips 35 invasive permissions from `AndroidManifest.xml` via companion resource patch, eliminating advertising IDs (`com.google.android.gms.permission.AD_ID`), Google Play Billing (`com.android.vending.BILLING`), precise/coarse GPS location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`), NFC, Bluetooth advertising and scanning (`BLUETOOTH_ADVERTISE`, `BLUETOOTH_SCAN`), audio recording (`RECORD_AUDIO`), fingerprint sensor access, and OEM hardware tracking.
    * Strips `android.permission.ACCESS_LOCAL_NETWORK` to prevent unauthorized local subnet scanning.
    * Prunes 92 external third-party `<package>` declarations inside `<queries>`, restricting package visibility exclusively to TikTok internal packages (`com.zhiliaoapp.musically`, `com.ss.android.ugc.trill`, `com.zhiliao.musically.livewallpaper`) to block cross-app package inventory scanning.
    * Preserves `android.permission.DETECT_SCREEN_CAPTURE` to maintain crash-free compatibility with Android 14+ platform callback registrations.
  * **Universal Package Query Trampoline Isolation**:
    * Intercepts `LX/00m8.U3` (`PackageManager.getPackageInfo` trampoline) and `LX/00m8.R3` (`PackageManager.getApplicationInfo` trampoline).
    * Routes package queries through `TikTokPrivacyHook.checkPackageAllowed()`. If an external app is queried, immediately throws a controlled `PackageManager.NameNotFoundException`, making third-party installed apps invisible.
  * **Contacts Access Isolation**:
    * Neutralizes BPEA contacts reader `LX/0OFU.LIZ()` -> returns `Collections.emptyList()`.
    * Neutralizes BPEA ContentResolver query `LX/0OFw.LIZ()` -> returns `null` cursor, safely reporting 0 contacts without triggering `SecurityException`.
  * **HAR (Human Activity Recognition) Sensor Silencing**:
    * Neutralizes `LX/18eO.LIZIZ()` (HarSensorManager init) -> returns `-1` to abort sensor registration.
    * Neutralizes `LX/18eO.onSensorChanged()` -> immediate `return-void` to prevent continuous accelerometer and gyroscope sampling used for physical movement fingerprinting.
    * Neutralizes `SmartHARServiceImpl.enable()` -> returns `false`, and `checkAndInit()` -> `return-void`.
  * **FLAG_SECURE Bypass (Unrestricted Screen Capture & Recording)**:
    * Neutralizes `LivePcsCourseVideoAntiScreenshotSetting.getValue()Z` -> returns `false`, preventing live courses and paywalled video sessions from enforcing anti-screenshot restrictions.
    * Injects `const/4 p1, 0x0` into `AntiScreenRecordController.applyFlag(Z)V` to permanently clear the window secure flag via native `Window.clearFlags(FLAG_SECURE / 0x2000)`.
    * Injects `const/4 p2, 0x0` into `makeScreenProtection(Landroid/view/Window;Z)V` to force `enableScreenProtection=false`, ensuring `canRecordScreen` evaluates to `true` and clearing secure flags during broadcasts.
  * **Clipboard Snooping Protection**:
    * Neutralizes programmatic clipboard reading `LX/01ZZ.LIZ()` -> returns `null`.
    * Neutralizes BPEA clipboard reading `LX/0jUy.LIZIZ()` -> returns `null`.
    * Neutralizes `IMMessageListClipboardServiceImpl.LIZ()` -> `return-void`.
  * **Screenshot & Screen Recording Detection & Telemetry Suppression**:
    * Neutralizes Lego screenshot task initialization: `ScreenShotTaskHolder$BootFinish`, `ScreenShotFeedbackTaskHolder$BootFinish`, `ScreenShotTask`, `ScreenShotFeedbackTask`, `ScreenRecordingMonitorInitTask`, `InternalShareScreenshotTask`, and `InternalShareScreenshotTaskHolder$BootFinish` -> `return-void`.
    * Neutralizes `ScreenShotFeedbackService.onShot()Z` -> returns `false`.
    * Neutralizes `ScreenShotFeedbackService.safelyShowDialog()` -> `return-void` (eliminates modal dialogs prompting for feedback upon taking a screenshot).
    * Neutralizes `ScreenShotFeedbackService.sendShareFeedbackEvent()` -> `return-void` (suppresses screenshot telemetry reporting).
    * Neutralizes `ScreenShotFeedbackService.isFeedbackEnable()` and `tryShowScreenShotFloatingView()` -> returns `false`.
    * Suppresses quick-share popup modals and Tako AI screenshot triggers.

### 5. Ghost Mode (`ghostModePatch`)
* **Objective**: Enable fully anonymous profile, story, and conversation browsing: view profiles and stories without alerting creators or leaving view history traces, and browse conversations without sending typing indicators, while preserving your ability to view follower counts and your own viewer list.
* **Internal Mechanisms**:
  * **Reactive Call-Site Skipping (Profile & Story View Suppression)**:
    * Dynamically identifies callers of `ProfileViewerApiService.reportView` and `StoryApi` methods (`reportStoryViewed`, `reportUserInteraction`, `reportStoryReveal`).
    * Traces Dalvik execution chains from invocation through `.subscribeOn(...)` to terminal `.subscribe()` / `.enqueue()` dispatch points.
    * Injects conditional branches (`skipReportAtCallSite`) backed by backward control-flow graph register liveness analysis (`RegisterLiveness`), safely jumping over the entire dispatch pipeline when `TikTokGhostModeHook` is active. This avoids passing invalid/dummy objects into RxJava or Kotlin coroutine state machines, completely preventing `NullPointerException` crashes and preserved follower counts.
  * **Secondary Story View Dispatch Suppression**:
    * Directly guards `LX/0x6G;->run()V` (`StoryFeedService.reportStoryViewed$3`) with `TikTokGhostModeHook.shouldBlockStoryView()` and exits `LX/07Rx;->LIZIZ` early, preventing background dispatch of delayed story view notifications.
  * **Profile Visit Telemetry Neutralization**:
    * Suppresses outbound `profile_request_response` analytics emission sites in `ProfilePlatformViewModel;->J53`, preventing recipient user identifiers (`to_user_id`) from leaking through AppLog telemetry channels.
  * **Outbound Typing Status Suppression**:
    * Guards `TypingStatusSenderTimer.LIZ(String)` and `LIZIZ(String)` entrypoints with early `return-void` via `TikTokGhostModeHook.shouldBlockTypingStatus()`.
  * **Companion Runtime Hook**:
    * Bridges hooks through `TikTokGhostModeHook` in `extensions/extension.mpe` with granular diagnostic telemetry.

### 6. In-App Browser Privacy Guard (`inAppBrowserPrivacyGuardPatch`)
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

### 6. Client-Side AI & Behavioral Profiling Governor (`clientAiGovernorPatch`)
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

### 7. Region & Geo-Restriction Bypass (`regionBypassPatch`)
* **Objective**: Bypass regional content restrictions, geo-blocked feeds, and country-specific account barriers without requiring physical SIM ejection.
* **Internal Mechanisms**:
  * Hooks BPEA telephony abstraction wrapper `LX/067c;->LIZJ(Landroid/telephony/TelephonyManager;LX/019X;)Ljava/lang/String;` (`getSimCountryIso`) to return the user-configured ISO country code.
  * Hooks BPEA telephony abstraction wrapper `LX/067c;->LIZ(Landroid/telephony/TelephonyManager;LX/019X;)Ljava/lang/String;` (`getNetworkCountryIso`) to return the user-configured ISO country code.
  * Configurable via the `region` patch option (defaults to `"CH"`).
  * **Region Selection & Recommendations**: For detailed guidance on picking the best region (e.g. `CH` for minimal e-commerce bloat and full audio catalogs vs `CA`/`US` for North American trends) and avoiding problematic country codes (such as `CN`, `IN`, `RU`, `JP`, or `DE`), see the [SIM Region Selection Guide](patch-configuration.md#sim-region-selector).

### 8. Feed Ad Blocker (`tikTokFeedAdBlockerPatch`)
* **Objective**: Completely clean the For You Page (FYP) and Following feeds from promotional intrusions.
* **Internal Mechanisms**:
  * Bytecode hooks in `FeedApiService.fetchFeedList()`, `FeedItemList.getItems()`, and `FollowFeedList.getItems()`.
  * Delegates feed filtering to runtime helper `TikTokFeedAdFilter`:
    * Evaluates `Aweme.isAd()`, `Aweme.isSoftAd()`, `Aweme.isWithPromotionalMusic()`, and link ad metadata to purge commercial items.

### 9. Hide TikTok Shop & Mall (`hideTikTokShopAnchorsPatch`)
* **Objective**: Completely eliminate shopping distractions by removing product tags, commercial anchors, and the dedicated Shop tab from top and bottom navigation bars.
* **Internal Mechanisms**:
  * **Video Feed Anchor Stripping**: Hooks `FeedApiService.fetchFeedList()`, `FeedItemList.getItems()`, and `FollowFeedList.getItems()` to invoke `TikTokFeedAdFilter.stripCommercialAnchors()`, clearing product tags (`setAnchors(null)`) and showcase links (`setAnchorInfo(null)`).
  * **Shop Bottom Tab Neutralization**: Hooks `ShopBottomTabProtocol.enable()Z` -> returns `false`.
  * **Shop Top Tab Neutralization**: Hooks `ShopTopTabProtocol.enable()Z` -> returns `false`.
  * **Shop Icon & Entry Service Neutralization**: Hooks `ShopIconServiceImpl.rw()Z` -> returns `false`.

### 10. Feed Live Stream Blocker (`feedLiveStreamBlockerPatch`)
* **Objective**: Eliminate live broadcast recommendations and live stream preview cards from the For You Page (FYP) and Following feeds as an independent, modular toggle.
* **Internal Mechanisms**:
  * Bytecode hooks in `FeedApiService.fetchFeedList()`, `FeedItemList.getItems()`, and `FollowFeedList.getItems()`.
  * Delegates live stream filtering to runtime helper `TikTokFeedAdFilter`:
    * Multi-signal detection inspecting `Room`, `RoomFeedCellStruct`, `liveId > 0`, `StreamUrlModel`, `authorLive`, and live aweme types (`101`, `68`, `102`, `69`) as well as Following feed live broadcasts (`feedType == 2`).
    * Purges matching live broadcast cards from feed lists before UI adapter binding.

### 11. Feed Bloat & Distraction Blocker (`feedBloatBlockerPatch`)
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
      * **Suggested Accounts**: `Aweme.getAwemeType() == 4004` (`TTRecUserBigCardViewHolder`), `CardInsertInfo.getCardType() == 49`, `Aweme.isFriendsTabFakeAweme() == true`.
      * **Mini-Games**: `Aweme.getAwemeType() == 104` or `CardInsertInfo.getCardType() == 120` (`MiniGameInstantPlayCardVH`).
      * **Creation & CapCut Prompts**: `CardInsertInfo.getCardType() in 188..191` (`CreationFeedCardViewHolder`).
      * **Memories ("On This Day")**: `CardInsertInfo.getCardType() == 127` (`OnThisDayCreationCardViewHolder`).
      * **EOY Recaps & Inspiration**: `CardInsertInfo.getCardType() == 84`, `CardInsertInfo.getCardType() == 176`.
      * **AI Remix & Effects**: `CardInsertInfo.getCardType() == 113`, `CardInsertInfo.getCardType() == 2`.
      * **Surveys & Feedback**: `CardInsertInfo.getCardType() == 4` or `16` (`BottomSurveyAssem`).
      * **Mini-Dramas & Series Promos**: `Aweme.getAwemeType() == 110` (`MiniDramaCard`).
      * **Lynx In-Feed Promos**: `Aweme.getAwemeType() == 106`.
    * **Following Feed Invariants**: Preserves essential watch history and unread markers (`feedType == 65280`, `65465`, `65298`, `lastViewData`) to prevent presenter index out-of-bounds crashes, while safely isolating and pruning confirmed recommendation cards (`feedType == 3`, `62`, `recommendUser`).
    * Prunes matching cards from list iterators in-situ with zero crashes or UI gaps.

### 13. Unified Telemetry & Tracker Silencer (`unifiedTelemetryTrackerSilencerPatch`)
* **Objective**: Cut off background surveillance, user behavior analytics, and diagnostic reporting to ByteDance servers.
* **Internal Mechanisms**:
  * **ByteDance AppLog**: Neutralizes `AppLog.onEvent()` and `AppLog.report()` entrypoints with immediate `return-void`.
  * **Crash Handlers & Telemetry Schedulers**: Neutralizes Lego initialization tasks for Npth crash reporting (`NpthCoreInitTask`), APM metrics (`ApmInit`), and Heimallr performance monitors.
  * **Attribution Trackers**: Neutralizes `InitAppsFlyer` and Firebase analytics startup initialization tasks.

### 14. Update Prompt Suppressor (`disableInAppUpdateNagsPatch`)
* **Objective**: Prevent forced upgrade popups and version enforcement dialogs.
* **Internal Mechanisms**:
  * Neutralizes Lego update check tasks: `CheckUpdateChangeDeviceIDTaskHolder$Background`, `UpdateTaskHolder$Background`, `CheckUpdateChangeDeviceIDTaskHolder$BootFinish`, and `UpdateTaskHolder$BootFinish`.
  * Injects `return-void` into `UpdateHelper.checkUpdate()`.

---

## ⚡ Performance, RAM & Battery

### 15. Display Refresh Rate Governor (`displayRefreshRateGovernorPatch`)
* **Objective**: Eliminate micro-stutter and forced refresh rate drops, locking TikTok's window display rate to peak hardware frequency (120Hz/90Hz) or a user-selected target, while neutralizing internal framerate downclocking routines during feed playback.
* **Internal Mechanisms**:
  * **Opt-In & Configurable Frequency with Hardware Clamping**:
    * Configurable via `targetRate` option: `max` (peak display rate detected dynamically via `Display.getSupportedModes()` with fallback to `Display.getSupportedRefreshRates()`), `120` (120 Hz), `90` (90 Hz), or `60` (60 Hz).
    * **Hardware Capability Boundary Protection**: `TikTokRefreshRateHook` queries actual physical display modes via `Display.getSupportedModes()` / `Display.getSupportedRefreshRates()`. If a requested target rate exceeds the panel's maximum physical frequency (e.g., selecting 120Hz on a 90Hz or 60Hz screen), it automatically clamps to the screen's peak supported rate instead of attempting an out-of-bounds mode switch that crashes the app or display pipeline.
    * Activity lifecycle guards verify the `Activity` is active and not finishing/destroyed before applying LayoutParams.
  * **Safe Video Playback Downclocking Neutralization**:
    * Hooks `LX/09YB.invoke()` (`ui_video_frame_rate_opt`) to return `Boolean.TRUE`, causing `PlayerController.LJJZZIII` to branch past internal downclocking instructions without aborting `onRenderFirstFrame` callbacks.
    * Neutralizes `LX/07tH.invoke()` (`setRefreshRateIfNeeded`) -> returns `Unit.LIZ`.
  * **Touch & Drag Release Frequency Enforcement**:
    * Overrides `LX/0JOJ.LIZ()` to immediately re-apply the target refresh rate to `Window.LayoutParams.preferredRefreshRate` whenever drag gestures stop.
    * Overrides `LX/1PFE.LIZ()` (instance) and `LX/1PFE.LIZIZ()` (static) (`RefreshFrequencyTutor`) passing the target `Activity` instance to prevent resetting the display back to 60Hz.
  * **Activity Lifecycle Lock**:
    * Hooks `MainActivity.onResume()` and `MainActivity.onWindowFocusChanged(boolean)` to ensure window parameters remain strictly locked to target refresh rate across focus switches and app switching.

### 16. Instant Launch & Splash Blocker (`instantColdStartPatch`)
* **Objective**: Accelerate application launch time to sub-second speeds (<0.4s) and bypass startup ads.
* **Internal Mechanisms**:
  * Neutralizes Lego splash tasks: `SplashAdManagerPreloadTask.run()` and `SplashAdManagerPreloadTaskEntry.run()`.
  * Forces `SplashSettingServiceImpl.LIZ()` and `LIZIZ()` -> `return false`.
  * Neutralizes `RealTimeSplashManagerImpl.LIZJ()` -> `return false` (eliminates synchronous waiting for remote TopView splash video assets during cold start).
  * Neutralizes `SplashAdServiceImpl.LJ()`, `LJIILJJIL()`, and `LJJIJIIJIL()` -> `return false` (suppresses splash ad presentation and background fetch).

### 17. Resource & Battery Governor (`resourceGovernorPatch`)
* **Objective**: Eliminate battery-draining background operations, sensor polling, and memory leaks.
* **Internal Mechanisms**:
  * **3D Ad Sensors & Gyroscope Polling**:
    * Neutralizes `ShakeEggService.LIZ()` -> returns `false`.
    * Neutralizes `ShakeEggService.LIZIZ()` -> returns `null`.
  * **Network Traffic & Video Buffer Governor**:
    * Forces `PreloadStrategyConfig.isEnableBufferPreload()Z` -> `return false`, preventing aggressive multi-video background buffer downloads on cellular data.
  * **Memory Retention Governor**:
    * Caps animated bitmap frame caching in Facebook Fresco (`FrescoFrameCache.LIZJ()`, `LJFF()`), preventing OOM crashes during long scrolling sessions.

### 18. P2P Video Relay Blocker (`p2pVideoRelayBlockerPatch`)
* **Objective**: Prevent TikTok from utilizing user device battery, CPU, and cellular data as a distributed P2P edge CDN relay for other users' video streams.
* **Internal Mechanisms**:
  * Zeroes out native P2P video delivery engine: `libavmdlp2pv2.so` (~7.9 MB).
  * Zeroes out native P2P live stream distribution module: `libp2plivevdp.so` (~1.9 MB).
  * Reclaims ~9.83 MB of storage while completely eliminating unmetered peer-to-peer relay network activity.

### 19. Disable Push Notifications (`disablePushNotificationsPatch`)
* **Objective**: Neutralize background push notification tasks and persistent socket wake locks to eliminate background battery drain.
* **Internal Mechanisms**:
  * Neutralizes `InitPushTask.run()` -> disables early startup wake lock acquisition and persistent background push sockets.

### 20. Live Stream 3D Gift Optimizer (`liveGiftEffectOptimizerPatch`)
* **Objective**: Disable Live 3D gift particle effect engine and widget rendering lifecycle to eliminate frame drops during live streams.
* **Internal Mechanisms**:
  * Injects `return-void` into `LiveGiftEffectWidget.initView()` and `LiveGiftEffectWidget.onCreate()`.

### 21. Live Stream Suite Optimizer (`liveStreamSuiteOptimizerPatch`)
* **Objective**: Strip unneeded interactive streaming libraries, live RTC broadcaster SDKs, and minigame assets for users who only watch regular videos or standard live streams.
* **Internal Mechanisms**:
  * Zeroes out native interactive streaming binaries: `liblink_mic_sdk.so`.
  * Zeroes out native Lyrax live streaming and RTC broadcaster engines: `liblyrax.so` (~48 MB) and `liblyrax_plugin.so` (~2.5 MB).
  * Removes interactive live stream battle minigame assets.
  * Reclaims over 50.45 MB of storage.

---

## 📦 APK Size & Resource Slimming

### 22. Core Asset De-bloat (`coreAssetDebloatPatch`)
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

### 23. Studio & Creation De-bloat (`creatorBloatSlimmerPatch`)
* **Objective**: Strip camera AR face filters, CapCut NLE video editing SDKs, and creation plugins across both `arm64-v8a` and `armeabi-v7a` for feed-only users.
* **Internal Mechanisms**:
  * Zeroes out camera AR effects engine: `libeffect_plugin.so`, `libEffectCreatorJni.so`.
  * Zeroes out CapCut-like video editor SDK: `libttvesdk_plugin.so`.
  * Zeroes out ILA Material SDK: `libILAMaterialSDK.so`.
  * Dynamically detects and zeroes out all bundled CapCut NLE native libraries (`libNLEMediaPublicJni.so`, `libNLEEditorJni.so`, `libNLETemplateModelJni.so`, `libNLEMediaJni.so`, etc.).
  * Purges bundled face mesh and facial landmark tracking models (`assets/model/ttfacemodel`).
  * Reclaims over 101 MB of uncompressed APK storage.

### 24. Language Pack Purger (`localeSlimmerPatch`)
* **Objective**: Reclaim substantial APK storage by stripping unused localized strings while preserving user-selected languages (default: English).
* **Internal Mechanisms**:
  * Scans `assets/strings#lang_*` bundles and removes unselected translation files.
  * Preserves default app language to ensure zero missing resource exceptions.
  * Configurable via the `locales` option (defaults to preserving English: `"en"`).

