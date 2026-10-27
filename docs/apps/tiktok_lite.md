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
---

## 3. Non-Applicable on Lite (35 Dropped Patches)

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
- **Bypass Mandatory Login**: Anchor absent; `MandatoryLoginService` class and mandatory login enforcement absent in Lite DEX.
- **Comment Customizer**: Anchor absent; `comment_sort_opt_style` string and comment sorting logic absent in Lite DEX.
- **Custom Share Sheet**: Anchor absent; `isImFunctionOff` method and custom share sheet layout absent in Lite DEX.
- **Customize Search**: Anchor absent; `RecomDataWrapper` class and search feed injection absent in Lite DEX.
- **Declutter Direct Messages**: Anchor absent; DM / IM messaging components and inbox message protocols absent in Lite DEX.
- **Declutter Navigation Bar**: Anchor absent; `ShopBottomTabProtocol` and shop bottom navigation tab absent in Lite DEX.
- **Declutter Video Interface**: Anchor absent; `UpvoteVideoTrigger.Qp` and declutter anchor classes absent in Lite DEX.
- **Device Privacy Guard**: Anchor absent; `LX/0OBB` device identifier collector class absent in Lite DEX.
- **Disable History Recording**: Anchor absent; `AwemeStatsApi` class and batch watch history reporting absent in Lite DEX.
- **Disable On-Device AI**: Anchor absent; `PitayaBootLoader` class and Pitaya AI engine absent in Lite DEX.
- **Disable QUIC**: Anchor absent; `CronetFrontierConnection.covertTransportMode` and cronet QUIC transport absent in Lite DEX.
- **Enable Profile Banner**: Anchor absent; `ProfileBackgroundExp` class and profile banner customization absent in Lite DEX.
- **Feed Ads & Content Filter**: Anchor absent; `FeedApiService.fetchFeedList` and commercial ad injection absent in Lite DEX.
- **Friends & Suggestions Filter**: Anchor absent; `LX/064b` friend recommendation filter class absent in Lite DEX.
- **Hide Inbox Promos & Alerts**: Anchor absent; `TopBannerWidgetContainerInjector` and promo banner widgets absent in Lite DEX.
- **Hide Inbox Story Tray**: Anchor absent; `StoryGetFeedByPageResponse` class and story tray container absent in Lite DEX.
- **Instant Launch**: Anchor absent; `SplashAdManagerPreloadTask` and splash ad preloader absent in Lite DEX.
- **Login Compatibility Fix**: Anchor absent; `WebLoginActivity` and third-party login compatibility wrappers absent in Lite DEX.
- **Playback & Display Governor**: Anchor absent; `Video.getRawBitRate` and video bitrate governor absent in Lite DEX.
- **Resume Video After Scroll**: Anchor absent; `ScrollAutoPlayManager` and video resume on scroll handlers absent in Lite DEX.
- **Show Author Region**: Anchor absent; `VideoAuthorInfoVM` and author region metadata badge absent in Lite DEX.
- **Use System Font**: Anchor absent; `TikTokSans-VF.otf` font asset absent in Lite APK bundle.
- **Video Fit Mode**: Anchor absent; `VideoAdaptionResult` class and video aspect ratio adaptation absent in Lite DEX.

---

## 4. Structural Notes

- **Bundle Format**: TikTok Lite is distributed as an APKM bundle with code split across 15 DEX files (77,111 classes).
- **Native Libraries**: TikTok Lite contains `libtt_lib.so`, `liborbuculum.so`, and `libzoin.so`, but lacks `libchrome.so`.
- **Target Pinning**: TikTok Lite pins exclusively version `47.5.52` (`versionCode: 470552`, `minSdk: 21`).
