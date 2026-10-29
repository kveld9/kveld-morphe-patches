# TikTok Lite Patches Guide

Technical documentation and patch catalog for TikTok Lite on Android.

---

## 1. Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target App** | TikTok Lite |
| **Package Name** | `com.tiktok.lite.go` |
| **Target Version** | `Single pinned version (see README Patch Catalog)` |
| **Min SDK** | `21` |
| **Target Package Format** | APKM bundle (`base.apk` + split config APKs) |
| **Primary Architecture** | `arm64-v8a` / `armeabi-v7a` |
| **Download Target** | [APKMirror](https://www.apkmirror.com/apk/tiktok-pte-ltd/tiktok-lite-faster-tiktok/) |
| **Authoritative Source** | APKMirror |

---

## 2. Supported Patches Catalog

The authoritative patch catalog with descriptions and options is generated in README.md from the built .mpp; see also patches-list.json.

---

## 3. Non-Applicable on Lite

The following patches from the standard TikTok patch suite are excluded on TikTok Lite because the corresponding target classes, components, or assets are absent from the Lite build:

> Re-audit note: every entry below was re-verified with a semantic sweep across all 15 DEX files (exact musically names, related strings, caller XREFs, nearest neighbor with file:line). "Absent" means zero hits everywhere, not just a renamed class. The login-bypass precedent (gate found under mini_account_impl/ with obfuscated names) was the trigger for this second pass, which rescued 8 additional ports.

### Group B: Feature Absent / Inert

- **Always Show Publish Date**: Anchor absent; `VideoItemParams.getAweme()` and `aweme.getCreateTime()` feed item UI hook absent in Lite DEX.
- **Disable Live Gift Effects**: Anchor absent; `LiveGiftPlayer` and `GiftEffectWidget` live gift rendering widgets absent in Lite DEX.
- **Block P2P Video Relay**: Anchor absent; P2P relay binaries (`libns_p2p.so`, `libbytedance_p2p.so`) absent in Lite APK bundle.
- **Remove Creator Tools**: Anchor absent; `CreatorToolsActivity` and creator studio entrypoints absent in Lite DEX.
- **Remove Live Stream Files**: Anchor absent; live stream assets and `LivePlayActivity` stream player absent in Lite DEX.
- **Remove Voice Tools**: Anchor absent; `VoiceConversionService` and audio filter voice engines absent in Lite DEX.
- **Remove Extra Files**: Anchor absent; stripped APK asset layout in Lite APK, no redundant sample files present.
- **Remove Extra Languages**: Anchor absent; Lite APK is already split into trimmed language bundles.

### Group C: Architecture Divergence / Incompatible Targets

- **Auto-Pause First Video**: Anchor absent; `PlayerController` class and first-frame playback interception absent in Lite DEX.
- **Comment Customizer**: Anchor absent; `comment_sort_opt_style` string and comment sorting logic absent in Lite DEX.
- **Custom Share Sheet**: Anchor absent; `isImFunctionOff` method and custom share sheet layout absent in Lite DEX.
- **Customize Search**: Anchor absent; `RecomDataWrapper` class and search feed injection absent in Lite DEX.
- **Declutter Direct Messages**: Anchor absent; DM / IM messaging components and inbox message protocols absent in Lite DEX.
- **Declutter Navigation Bar**: Anchor absent; DEX inventory of the pinned Lite target shows none of `ShopBottomTabProtocol`, `NearbyServiceImpl`, `ExploreFeedServiceImpl`, `ExploreBottomTabProtocol`, `LiveIconGenerator`, `PublishTabProtocol`, `FeedSearchBottomBarAssemTrigger`, `AwemeTrendingBar` or `HotSearchInfo` --
  the Lite main page (MainActivity -> MainRootFragment -> MainPageFragment -> FeedFragment) builds its top strip programmatically from a static single-entry tab list and its bottom navigation, LIVE entrance and feed search pill have no statically resolvable anchors (server/Lynx-driven candidates: lite_bottom_tab_2/4/5, lite_live_add_toplive_entrance, short_drama_inner_feed_search_entry_enabled experiments).
- **Declutter Video Interface**: Anchor absent; `UpvoteVideoTrigger.Qp` and declutter anchor classes absent in Lite DEX.
- **Device Privacy Guard**: Anchor absent; `LX/0OBB` device identifier collector class absent in Lite DEX.
- **Disable On-Device AI**: Anchor absent; `PitayaBootLoader` class and Pitaya AI engine absent in Lite DEX.
- **Enable Profile Banner**: Anchor absent; `ProfileBackgroundExp` class and profile banner customization absent in Lite DEX.
- **Hide Inbox Promos & Alerts**: Anchor absent; `TopBannerWidgetContainerInjector` and promo banner widgets absent in Lite DEX.
- **Hide Inbox Story Tray**: Anchor absent; `StoryGetFeedByPageResponse` class and story tray container absent in Lite DEX.
- **Instant Launch**: Anchor absent; `SplashAdManagerPreloadTask` and splash ad preloader absent in Lite DEX.
- **Offline Video Limit**: Anchor absent; `OfflineModeSheetPageAssem` and user-facing offline videos download sheet absent in Lite DEX.
- **Resume Video After Scroll**: Anchor absent; `ScrollAutoPlayManager` and video resume on scroll handlers absent in Lite DEX.
- **Show Author Region**: Anchor absent; `VideoAuthorInfoVM` and author region metadata badge absent in Lite DEX.
- **Use System Font**: Anchor absent; `TikTokSans-VF.otf` font asset absent in Lite APK bundle.
- **Video Fit Mode**: Anchor absent; `VideoAdaptionResult` class and video aspect ratio adaptation absent in Lite DEX.

---

## 4. Technical Breakdown

### Privacy & Governance
- **Disable History Recording**:
  - *Objective*: Prevent viewed videos from recording in account watch history.
  - *How*: Neutralizes video stats reporting routine (`AwemeStatsApi.L`) with `return-void` when `disableWatchHistory` is enabled.
- **Login Compatibility Fix**:
  - *Objective*: Restore Google account sign-in after patching on modified app signatures.
  - *How*: Stubs `GoogleAuth.isAvailable()Z` to return `false`, which forces `AuthorizeActivity` to fall back to Web-based OAuth (`google_web`).
- **Allow Screenshots**:
  - *Objective*: Removes screenshot and screen recording blocking.
  - *How*: Neutralizes screenshot detection task (`ScreenShotTask.run(Context)`) with early `return-void` to prevent screenshot monitoring and detection telemetry.
- **Block Trackers**:
  - *Objective*: Neutralizes ByteDance AppLog tracking, crash monitors, and analytics.
  - *How*: Stubs `AppLog.onEvent`, `AppLog.onEventV3`, `Npth` crash reporting tasks (`run` and `LCCII`), `Npth.init`, and `UltraliteInitAppsFlyer`, and mocks `AppLogNetworkClient` upload requests with synthetic success responses.
- **Block Trackers (Setup)**:
  - *Objective*: Removes tracking permissions and disables telemetry components.
  - *How*: Strips advertising permissions (`AD_ID`, AdServices), disables analytics receivers and providers (AppLog, AppsFlyer, Google Analytics), removes discovery registrars, and injects opt-out metadata flags into `AndroidManifest.xml`.
- **Bypass Mandatory Login**:
  - *Objective*: Lets you browse videos and feeds without creating an account or logging in.
  - *How*: Forces Lite `MandatoryLoginService` decision methods (`LCC` and `L`) to return `false` and stubs `LBL` with `return-void` to bypass mandatory login barriers.
- **Camera & Microphone Indicator**:
  - *Objective*: Shows a corner mark while TikTok holds camera/mic open.
  - *How*: Installs visual indicator overlay in `MainActivity.onCreate` via `TikTokCameraMicHook.install` and instruments camera and microphone start and release call sites.
- **Clean Share Links**:
  - *Objective*: Removes tracking parameters from shared links.
  - *How*: Sanitizes shared link URLs (supporting `tiktok.com` and `tiktxk.com` domains and Lite tracking parameters) and applies optional custom hostname replacement via `TikTokShareHostHook` in Lite share services `HomePageShareServiceImpl` and `Cem`.
- **Feed Ads & Content Filter**:
  - *Objective*: Filters ads, shopping tags, live cards, and clutter from your feed.
  - *How*: Intercepts feed list getters `FeedItemList.getItems()`, `SearchMix.getAwemeList()`, and `SearchMixFeedList.getAwemeList()` to filter ads, bloat, shop anchors, live streams, AI content, and matching content rules via `TikTokFeedAdFilter` and `TikTokContentFilterHook`.
- **Friends & Suggestions Filter**:
  - *Objective*: Keeps the Friends feed strictly mutual and hides suggested accounts across the app.
  - *How*: Filters non-mutual recommendations in `FriendsFeedResponse.<init>` via `TikTokFeedAdFilter.filterSuggestedVideosInFriendsFeedData` and disables profile recommendation cards by zeroing `ProfileRecUserViewModel.LILII`.
- **In-App Browser Privacy Guard**:
  - *Objective*: Opens web links in your browser and blocks tracking in the remaining web views.
  - *How*: Redirects SparkThird external web links and popup dispatchers to the system browser via `TikTokBrowserHook.redirectSparkThird`.
- **SIM Region Selector**:
  - *Objective*: Locks the app to a chosen country to unlock regional content.
  - *How*: Spoofs `TelephonyManager.getSimCountryIso` return value to configured ISO country code and overrides operator numeric (MCC+MNC) and operator name call sites.
- **Strip Sensitive Permissions**:
  - *Objective*: Removes sensitive hardware, sensor, and tracking permissions.
  - *How*: Selectively strips configured permission groups (notifications, camera, microphone, storage, bluetooth, foreground services, wake lock, OEM signals, and billing) from `AndroidManifest.xml`.

### Usability & Playback
- **Block Popups & Prompts**:
  - *Objective*: Blocks popups, nags and promo dialogs across the app.
  - *How*: Skips first-launch onboarding via `NUJServiceImpl.L` and `LB` (returns `false`), and suppresses notification permission prompts via `minilite/nuj/PushServiceImpl.LB` and system push dialog checks in `01c.LBL`.
- **Download Without Watermark**:
  - *Objective*: Unlocks downloading and saves videos without watermarks.
  - *How*: Unlocks share panel and feed download buttons in `D2M.L(Aweme)` (returns 2 and sanitizes ACL download permissions), forces unwatermarked playAddr stream routing via `7PV.L(Aweme)`, sets `ACLCommonShare` constructors to transcode=1, and injects clean stream URLs via `TikTokMediaHook.patchVideoObject` in `Aweme.getVideo()`.
- **Force Auto-Scroll**:
  - *Objective*: Enables native auto-scrolling to the next video.
  - *How*: Forces native feed auto-scroll experiment flag getter (`fyp_auto_scroll_lite_client`) to return `true`.
- **Remember Clear Display**:
  - *Objective*: Keeps clear-display mode active when scrolling to the next video.
  - *How*: Tracks clear mode intention across `7iG.LC` and long-press toggle `8iD.LC` via `TikTokClearDisplayHook`, and re-triggers clear mode in `FeedFragment.r5` on video switch.
- **Remember Playback Speed**:
  - *Objective*: Keeps your chosen playback speed across videos and restarts.
  - *How*: Persists selected speed via `TikTokSpeedHook` in `Ao6.LFF`, returns stored speed in `Ao6.LBL`, and enforces it across feed scrolls in `FeedFragment.S6`.
- **Show Exact Counts**:
  - *Objective*: Shows exact numbers on likes, comments, views and followers.
  - *How*: Intercepts static count formatters matching `1.0M` to format raw metric values using `TikTokCountsHook.exact(J)`.
- **Show Seekbar**:
  - *Objective*: Restores the video progress seekbar and scrubbing controls.
  - *How*: Forces `VideoControl` progress bar flags (`draftProgressBar=1`, `showProgressBar=2`) in `Aweme.getVideoControl()`, ensures initialization in `FeedFragment`, and forces seekbar visibility active in seekbar controller `LF()`.
- **Stop Video Looping**:
  - *Objective*: Stops videos at the end instead of replaying them in an infinite loop.
  - *How*: Forces looping parameter to `false` in `TTVideoEngineImpl` looping dispatcher and `MediaPlayer.setLooping` delegator.

### Performance & Resource
- **Battery & Resource Saver**:
  - *Objective*: Stops 3D ad sensors and aggressive video preloading to save battery and data.
  - *How*: Forces `PreloadStrategyConfig.isEnableBufferPreload()` to return `false`, throttling aggressive buffer preloading.
- **Disable Push Notifications**:
  - *Objective*: Stops background push notifications.
  - *How*: Neutralizes push initialization task (`PushInitTask.run(Context)`) with early `return-void` to prevent background push polling and wake locks.
- **Disable QUIC**:
  - *Objective*: Loads videos, uploads, and live connections over standard HTTPS instead of QUIC.
  - *How*: Forces TCP/TLS fallback by clearing `mEnableTTQuicHeLoader` in `AVMDLDataLoader.setConfigureInternal` and stubbing `BDUploadUtil.xquicAvalilable()` to return `false`.
- **Playback & Display Governor**:
  - *Objective*: Limits playback resolution and HDR to save battery, and locks the screen refresh rate.
  - *How*: Caps video playback resolution and filters bitrates via `TikTokVideoQualityHook` in `Aweme.getVideo()` and `Video` play address getters, avoids ByteVC2 software decoding, and locks display refresh rate in `MainActivity.onResume` and `onWindowFocusChanged` using `TikTokRefreshRateHook`.

---

## 5. Known Limitations

- **Search History Suppression**: Search history query recording is not supported on TikTok Lite because the search history analytics and recommendation stores (`SuggestWordResponse`, `ManualSearchPvStore`) are absent in the Lite build.
- **Spotify Sign-In**: Spotify login compatibility is not supported on TikTok Lite because the Spotify auth SDK dynamic feature module (`df_music_dsp`) is absent in the Lite build.
- **Offline Video Limit**: TikTok Lite does not feature user-facing offline video limit selection (`OfflineModeSheetPageAssem` is absent); caching is handled automatically based on storage tiers.

### Lite UI anchor map (observed)

Top tab strip = MainPageFragment + C1MA/C08030Ut tab list; top progress ring = C0TT; feed = FeedFragment + player views; bottom 5-tab bar / LIVE button / feed search pill = no static anchors found.

---

## 6. Structural Notes

- **Bundle Format**: TikTok Lite is distributed as an APKM bundle with code split across 15 DEX files (77,111 classes).
- **Native Libraries**: TikTok Lite contains `libtt_lib.so`, `liborbuculum.so`, and `libzoin.so`, but lacks `libchrome.so`.
- **Min SDK**: `21`.

---

## 7. Traffic Audit

Per-flow traffic audit reports live in issues, not in this guide.
