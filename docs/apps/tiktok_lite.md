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

- **Allow Screenshots**: Anchor absent; `ScreenShotTask` was located in an optional dynamic feature split absent from the distribution bundle.
- **Always Show Publish Date**: Anchor absent; `VideoItemParams.getAweme()` and `aweme.getCreateTime()` feed item UI hook absent in Lite DEX.
- **Bypass Mandatory Login**: Anchor absent; `MandatoryLoginService` was located in an optional dynamic feature split absent from the distribution bundle.
- **Camera & Microphone Indicator**: Anchor absent; `MainActivity` hook and camera/mic call sites absent in Lite DEX.
- **Disable History Recording**: Anchor absent; `AwemeStatsApi` was located in an optional dynamic feature split absent from the distribution bundle.
- **Disable Live Gift Effects**: Anchor absent; `LiveGiftPlayer` and `GiftEffectWidget` live gift rendering widgets absent in Lite DEX.
- **Block P2P Video Relay**: Anchor absent; P2P relay binaries (`libns_p2p.so`, `libbytedance_p2p.so`) absent in Lite APK bundle.
- **Download Without Watermark**: Anchor absent; Aweme ACL share and download components (`AwemeACLShare`, `ACLCommonShare`) absent in Lite DEX.
- **Force Auto-Scroll**: Anchor absent; feed auto-scroll experiment flag getter absent in Lite DEX.
- **Friends & Suggestions Filter**: Anchor absent; `FriendsFeedResponse` was located in an optional dynamic feature split absent from the distribution bundle.
- **In-App Browser Privacy Guard**: Anchor absent; `SparkThird` browser engine absent in Lite DEX.
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
- **Login Compatibility Fix**: Not applicable on Lite; the patch is compiled for `com.zhiliaoapp.musically` only. TikTok Lite relies exclusively on native Google Play Services auth (`play-services-auth` / `SignInHubActivity`) and lacks the browser-based Web OAuth fallback (`GoogleWebAuth`) found in standard TikTok's Lobby. On re-signed APKs, native GMS token acquisition is rejected server-side by Google due to signing certificate SHA-1 mismatch. Device registration transport is preserved separately by Block Trackers Setup.
- **Offline Video Limit**: Anchor absent; `OfflineModeSheetPageAssem` and user-facing offline videos download sheet absent in Lite DEX.
- **Resume Video After Scroll**: Anchor absent; `ScrollAutoPlayManager` and video resume on scroll handlers absent in Lite DEX.
- **Show Author Region**: Anchor absent; `VideoAuthorInfoVM` and author region metadata badge absent in Lite DEX.
- **Use System Font**: Anchor absent; `TikTokSans-VF.otf` font asset absent in Lite APK bundle.
- **Video Fit Mode**: Anchor absent; `VideoAdaptionResult` class and video aspect ratio adaptation absent in Lite DEX.

---

## 4. Technical Breakdown

### Privacy & Governance
- **Block Trackers**:
  - *Objective*: Neutralizes ByteDance AppLog tracking, crash monitors, and analytics.
  - *How*: Stubs `AppLog.onEvent` and every overload of `Npth.init`, suppressing telemetry and crash reporting while preserving device registration transport for account login.
- **Block Trackers (Setup)**:
  - *Objective*: Removes tracking permissions and disables telemetry components.
  - *How*: Strips advertising permissions (`AD_ID`, AdServices), disables analytics receivers and providers (AppLog, AppsFlyer, Google Analytics), removes analytics discovery registrars, and injects opt-out metadata flags into `AndroidManifest.xml`.
- **Clean Share Links**:
  - *Objective*: Removes tracking parameters and promotional tail text from shared links.
  - *How*: Intercepts `minilite.feed.model.Aweme.getShareUrl()` to sanitize shared link URLs, strips tracking parameters, and applies custom hostname replacement via `TikTokShareHostHook`.
- **Feed Ads & Content Filter**:
  - *Objective*: Filters ads, shopping tags, live cards, clutter, and previously watched videos from your feed.
  - *How*: Intercepts feed list getters `FeedItemList.getItems()` and `FeedItemList.getAwemeList()` to filter ads, bloat, shop anchors, live streams, matching content rules, and previously seen videos (`hideSeenVideos`) via `TikTokFeedAdFilter` and `TikTokSeenVideoHook`. Tracks viewed videos on playback preparation via `Aweme.getVideo()`.
- **SIM Region Selector**:
  - *Objective*: Locks the app to a chosen country to unlock regional content.
  - *How*: Spoofs `TelephonyManager.getSimCountryIso` return value to configured ISO country code and overrides operator numeric (MCC+MNC) and operator name call sites.
- **Strip Sensitive Permissions**:
  - *Objective*: Removes sensitive hardware, sensor, and tracking permissions.
  - *How*: Selectively strips configured permission groups (notifications, camera, microphone, storage, bluetooth, foreground services, wake lock, OEM signals, and billing) from `AndroidManifest.xml`.

### Usability & Playback
- **Block Popups & Prompts**:
  - *Objective*: Blocks popups, nags and promo dialogs across the app.
  - *How*: Suppresses notification permission prompts via `PushServiceImpl.LB` and system push dialog checks reading `count_system` (`0Co.LBL()Z`), preserving onboarding completion state and login callback execution.
- **Remember Clear Display**:
  - *Objective*: Keeps clear-display mode active when scrolling to the next video.
  - *How*: Intercepts Lite clear-mode dispatcher matching `change_clear_mode_status` (`LX/7t3.LC`) to suppress exit on `switch_video`, and neutralizes `FeedFragment.r5` video switch dismissal.
- **Remember Playback Speed**:
  - *Objective*: Keeps your chosen playback speed across videos and restarts.
  - *How*: Persists selected speed via `TikTokSpeedHook` in `SimplifyPlayerImpl` (`LFF(F)V`) and enforces saved speed on new video playback in `SimplifyPlayerImpl.render()V`.
- **Show Exact Counts**:
  - *Objective*: Shows exact numbers on likes, comments, views and followers.
  - *How*: Intercepts static count formatters matching `1.0M` to format raw metric values using `TikTokCountsHook.exact(J)`.
- **Show Seekbar**:
  - *Objective*: Restores the video progress seekbar and scrubbing controls.
  - *How*: Forces `VideoControl` progress bar flags (`draftProgressBar=1`, `showProgressBar=2`) in `minilite.feed.model.Aweme.getVideoControl()`.
- **Stop Video Looping**:
  - *Objective*: Stops videos at the end instead of replaying them in an infinite loop.
  - *How*: Forces looping parameter to `false` in `TTVideoEngineImpl` looping dispatcher and `MediaPlayer.setLooping` delegator.

### Performance & Resource
- **Battery & Resource Saver**:
  - *Objective*: Stops 3D ad sensors and aggressive video preloading to save battery and data.
  - *How*: Forces `PreloadStrategyConfig.isEnableBufferPreload()` to return `false`, throttling aggressive buffer preloading.
- **Disable Push Notifications**:
  - *Objective*: Stops background push notifications.
  - *How*: Neutralizes push service initialization (`com.ss.android.ugc.aweme.minilite.remotepush.interaction.PushService.init(Context)`) with early `return-void` to prevent background push polling and wake locks without breaking Play Feature Delivery (`SplitCompat.install`).
- **Disable QUIC**:
  - *Objective*: Loads videos, uploads, and live connections over standard HTTPS instead of QUIC.
  - *How*: Forces TCP/TLS fallback by clearing `mEnableTTQuicHeLoader` in `AVMDLDataLoader.setConfigureInternal`.
- **Playback & Display Governor**:
  - *Objective*: Limits playback resolution and HDR to save battery, and locks the screen refresh rate.
  - *How*: Caps video playback resolution and filters bitrates via `TikTokVideoQualityHook` in `minilite.feed.model.Aweme.getVideo()` and `minilite.feed.model.Video` play address getters, avoids ByteVC2 software decoding, and locks display refresh rate in `MainActivity.onResume` and `onWindowFocusChanged` using `TikTokRefreshRateHook`.

---

## 5. Known Limitations

- **Incomplete Distribution Bundles**: TikTok Lite ships most of its feature code in separate modules that Google Play installs on demand, and some public bundle uploads are captured before those modules are fetched, so they arrive without them. On such a bundle the app stays in its reduced mode and asks for the full resources, and that download cannot complete on a patched build because Play only serves the modules to apps it installed itself with the original signature. Import a bundle that already contains every module; a bundle missing them cannot be completed on the device.

- **Google Sign-In on Re-signed Builds**: Native Google sign-in fails with a "Couldn't log in" toast on patched TikTok Lite APKs. Google Play Services validates the APK certificate SHA-1 against ByteDance's production OAuth 2.0 Client ID registration on Google servers. Because TikTok Lite replaced ByteDance's Lobby with `mini_lobby` that completely strips browser-based Web OAuth fallback (`GoogleWebAuth`), native Google sign-in cannot succeed on non-rooted environments. Users must sign in via username/email and password, or use standard TikTok if Google authentication is required.
- **Search History Suppression**: Search history query recording is not supported on TikTok Lite because the search history analytics and recommendation stores (`SuggestWordResponse`, `ManualSearchPvStore`) are absent in the Lite build.
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
