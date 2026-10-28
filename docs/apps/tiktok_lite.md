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
| **Download Target** | [APKMirror](https://www.apkmirror.com/apk/tiktok-pte-ltd/tiktok-lite-android-go/) |
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
- **Declutter Navigation Bar**: Anchor absent; `ShopBottomTabProtocol` and shop bottom navigation tab absent in Lite DEX.
- **Declutter Video Interface**: Anchor absent; `UpvoteVideoTrigger.Qp` and declutter anchor classes absent in Lite DEX.
- **Device Privacy Guard**: Anchor absent; `LX/0OBB` device identifier collector class absent in Lite DEX.
- **Disable On-Device AI**: Anchor absent; `PitayaBootLoader` class and Pitaya AI engine absent in Lite DEX.
- **Enable Profile Banner**: Anchor absent; `ProfileBackgroundExp` class and profile banner customization absent in Lite DEX.
- **Hide Inbox Promos & Alerts**: Anchor absent; `TopBannerWidgetContainerInjector` and promo banner widgets absent in Lite DEX.
- **Hide Inbox Story Tray**: Anchor absent; `StoryGetFeedByPageResponse` class and story tray container absent in Lite DEX.
- **Instant Launch**: Anchor absent; `SplashAdManagerPreloadTask` and splash ad preloader absent in Lite DEX.
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

---

## 5. Known Limitations

- **Search History Suppression**: Search history query recording is not supported on TikTok Lite because the search history analytics and recommendation stores (`SuggestWordResponse`, `ManualSearchPvStore`) are absent in the Lite build.
- **Spotify Sign-In**: Spotify login compatibility is not supported on TikTok Lite because the Spotify auth SDK dynamic feature module (`df_music_dsp`) is absent in the Lite build.

---

## 6. Structural Notes

- **Bundle Format**: TikTok Lite is distributed as an APKM bundle with code split across 15 DEX files (77,111 classes).
- **Native Libraries**: TikTok Lite contains `libtt_lib.so`, `liborbuculum.so`, and `libzoin.so`, but lacks `libchrome.so`.
- **Min SDK**: `21`.

---

## 7. Traffic Audit

Per-flow traffic audit reports live in issues, not in this guide.
