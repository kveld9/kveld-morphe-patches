# 🎵 TikTok: Complete Patch, Architecture & Configuration Guide

Comprehensive technical, architectural, and configuration guide for **TikTok** (supporting both Global `com.zhiliaoapp.musically` and Asia `com.ss.android.ugc.trill`), pinned to target version **`47.0.3`**.

---

## 🎯 Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target Application** | TikTok |
| **Package Names** | `com.zhiliaoapp.musically` (Global) · `com.ss.android.ugc.trill` (Asia) |
| **Supported Target Version** | **`47.0.3`** |
| **Target File Format** | Standalone APK (`APK` - **nodpi**) |
| **Recommended Architecture** | `arm64-v8a` |
| **Official Download Source** | [APKMirror: Global (musical.ly)](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-47-0-3-release/) · [APKMirror: Asia (trill)](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/tiktok-47-0-3-2-release/) |

> [!IMPORTANT]
> Always download the standalone `nodpi` APK variant for `arm64-v8a`. Do not use split APK bundles (`bundle` / `apkm`).

---

## 📋 Applied Patches Catalog

| Category | Patch Name | Type | Key Target / Mechanism |
| :--- | :--- | :--- | :--- |
| **Usability** | **Media Usability & Watermark-Free Downloader** | `bytecodePatch` | Unblocks download button in Share panel, extracts clean original streams without watermark stamps. |
| **Usability** | **Show seekbar** | `bytecodePatch` | Restores video seekbar and scrubbing controls where hidden or disabled. |
| **Usability** | **Always show publish date** | `bytecodePatch` | Forces video publish and upload timestamps to remain permanently visible on feed cards. |
| **Usability** | **Copy comments without username** | `bytecodePatch` | Sanitizes comment copy actions to exclude the prepended author username. |
| **Usability** | **Disable Comment Suggested Emojis** | `bytecodePatch` | Removes the horizontal bar of suggested quick emojis displayed above the comment input box. |
| **Usability** | **Playback Speed Persistence** | `bytecodePatch` | Persists user-selected video speed across feed scrolling and restarts. |
| **Usability** | **[Video Quality Governor](#2-video-quality-governor)** | `bytecodePatch` | Decoupled resolution ceilings for playback (e.g. 480p) and downloads (e.g. 1080p). |
| **Usability** | **Skip First-Launch Onboarding** | `bytecodePatch` | Bypasses interest pickers, swipe-up tutorial, language prompts, and consent sheets directly to FYP. |
| **Usability** | **[Custom Offline Videos Limit](#4-custom-offline-videos-limit)** | `bytecodePatch` | Customizes maximum offline videos download caching limit (~X mins, Y GB/MB). |
| **Usability** | **Auto-translate comments** | `bytecodePatch` | Automatically dispatches batch translations via TikTok's native engine. |
| **Usability** | **Hide Top-Left LIVE Button** | `bytecodePatch` | Removes the top-left LIVE broadcast button and tab entry point from the top navigation bar. |
| **Usability** | **Hide STEM and Community Tabs** | `bytecodePatch` | Removes STEM and Comunidad (Explore / Topics) tabs from the top navigation feed strip. |
| **Usability** | **Hide Profile Photo Follow Button** | `bytecodePatch` | Hides the plus (+) follow badge on creator profile avatars in the feed and disables its touch interaction. |
| **Usability** | **Disable Profile Photo LIVE Status** | `bytecodePatch` | Removes pulsing LIVE ring/badge from creator avatars in feed and forces clicks directly to user profile. |
| **Usability** | **Disable Story Feed Indicators** | `bytecodePatch` | Removes the top-center story drop-down indicator pill (e.g. '1 Story') and creator profile photo story rings from feed videos, ensuring avatar photos remain clean. |
| **Usability** | **Force auto-scroll** | `bytecodePatch` | Forces the activation of the native video auto-scroll experiment flag for accounts and regions that lack it due to A/B testing. |
| **Usability** | **Hide Feed Search Bar** | `bytecodePatch` | Removes the search suggestion pill and trending bar ('Search · <keyword>') from the bottom of feed videos. |
| **Usability** | **Hide Popular Lives In Search** | `bytecodePatch` | Removes the popular LIVE broadcasts section ('LIVE populares') and live stream recommendations from the search intermediate page. |
| **Usability** | **Hide Suggested Searches** | `bytecodePatch` | Removes the suggested searches section ('Podría interesarte' / Guess Search) from the search intermediate page. |
| **Usability** | **Auto-pause first video** | `bytecodePatch` | Automatically pauses the initial video on startup (frame 0) with center play icon; resumes instantly upon screen tap or feed scroll. |
| **Privacy** | **Fix Google login** | `bytecodePatch` | Restores Google account sign-in via Web OAuth fallback when GMS rejects modified APK signature. |
| **Privacy** | **Bypass Mandatory Login** | `bytecodePatch` | Neutralizes mandatory login walls, dynamic regional forced login gates, and guest browsing restrictions. |
| **Privacy** | **Clean Share URL** | `bytecodePatch` | Strips tracking query parameters, user tokens, and campaign IDs from shared links. |
| **Privacy** | **Device Privacy Guard** | `bytecodePatch` | Intercepts runtime permission prompts (contacts, location), suppresses in-app permission nag dialogs and background sync tasks, zeroes Advertising ID, blocks clipboard inspection, isolates package queries, silences HAR motion sensors, and clears `FLAG_SECURE`. |
| **Privacy** | **In-App Browser Privacy Guard** | `bytecodePatch` | Redirects external links to default system browser, neutralizes WebView JS tracking injection and AJAX hookers. |
| **Privacy** | **Client-Side AI & Behavioral Profiling Governor** | `bytecodePatch` | Neutralizes Pitaya on-device ML, Tako AI chatbot entries, and AI search clutter. |
| **Privacy** | **[SIM Region Selector](#1-sim-region-selector)** | `bytecodePatch` | Spoofs SIM and network country ISO codes to bypass regional restrictions. |
| **Privacy** | **Feed Ad Blocker** | `bytecodePatch` | Filters sponsored cards, brand promotions, and commercial audio. |
| **Privacy** | **Hide TikTok Shop & Mall** | `bytecodePatch` | Removes product anchors, showcase badges, and bottom/top Shop navigation tabs. |
| **Privacy** | **Feed Live Stream Blocker** | `bytecodePatch` | Removes live broadcast cards and live recommendations from FYP and Following. |
| **Privacy** | **Feed Bloat & Distraction Blocker** | `bytecodePatch` | Removes friend suggestions, suggested account carousels, mini-games, CapCut prompts, memories, surveys, mini-dramas, Lemon8 promo, and floating rewards pendants across For You, Following, and Friends feeds. |
| **Privacy** | **Unified Telemetry & Tracker Silencer** | `bytecodePatch` | Neutralizes ByteDance AppLog, APM/Npth/Heimdallr crash telemetry, and AppsFlyer. |
| **Privacy** | **Disable Search History Recording** | `bytecodePatch` | Prevents search queries and keywords from being recorded in local history, databases, and analytics stores. |
| **Privacy** | **Update Prompt Suppressor** | `bytecodePatch` | Neutralizes background update polling tasks and version enforcement dialogs. |
| **Performance** | **[Display Refresh Rate Governor](#3-display-refresh-rate-governor)** | `bytecodePatch` | Locks window to peak hardware refresh rate (120Hz/90Hz) and neutralizes playback downclocking. |
| **Performance** | **Instant Launch & Splash Blocker** | `bytecodePatch` | Eliminates cold start delays, splash ad tasks, and TopView preload waits (<0.4s). |
| **Performance** | **Resource & Battery Governor** | `bytecodePatch` | Suppresses 3D shake ad sensors and video buffer preloading. |
| **Performance** | **P2P Video Relay Blocker** | `rawResourcePatch` | Strips `libavmdlp2pv2.so` and `libp2plivevdp.so` to stop background P2P CDN seeding. |
| **Performance** | **Disable Push Notifications** | `bytecodePatch` | Neutralizes background push socket polling and persistent wake locks. |
| **Performance** | **Live Stream 3D Gift Optimizer** | `bytecodePatch` | Disables 3D gift particle effect engine to eliminate live frame drops. |
| **Performance** | **Live Stream Suite Optimizer** | `rawResourcePatch` | Strips `liblink_mic_sdk.so`, Lyrax RTC broadcaster libs, and battle minigames (~50 MB saved). |
| **Slimmer** | **Core Asset De-bloat** | `rawResourcePatch` | Strips Microblink OCR models, C2PA AI libs, ByteDance TTWebView engine, non-Latin fonts, Python VM (~77 MB saved). |
| **Slimmer** | **Studio & Creation De-bloat** | `rawResourcePatch` | Strips AR camera engine (`libeffect_plugin.so`) and video editor SDK (`libttvesdk_plugin.so`) (~101 MB saved). |
| **Slimmer** | **Language Pack Purger** | `rawResourcePatch` | Strips unselected language string bundles from `assets/strings#lang_*`. |
| **Universal Patches Suite** | Multiple | Optimization & Privacy | Compatible with universal slimmers and privacy patches (Telemetry Neutralizer, Native Binary Trimmer, WebP/PNG Optimizers, DPI/Locale Slimmers). See [Universal Patch Reference](../universal-patches.md). |

---

## ⚙️ Configurable Options in Morphe Manager

### 1. SIM Region Selector

The **`SIM Region Selector`** patch bypasses geographic content restrictions, regional feed filtering, and country-specific catalog locks by spoofing the SIM and network ISO country codes queried by TikTok.

| Option | Key | Type | Default | Range / Format | Description |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Region** | `region` | String | `CH` | 2-letter ISO 3166-1 alpha-2 code | 2-letter ISO country code to spoof for SIM and network country checks. |

#### Region Selection Guide & Operational Trade-Offs

When selecting a region code, balance **e-commerce bloat (TikTok Shop / Mall / Live selling)** against **audio licensing and content availability**:

##### A. Optimal Baseline: `CH` (Switzerland — Default)
- **Zero E-Commerce Bloat**: TikTok Shop, shopping tabs, and affiliate product showcases are not deployed.
- **Full Music Catalog**: Complete access to commercial and international audio without local licensing mutes.
- **No EU Regulatory Overhead**: Being outside the European Union, it avoids recurring Digital Markets Act (DMA) consent dialogs and cookie barriers.
- **Unrestricted Global Feed**: Clean international feed with full upload and viewing availability.
- **Critical Distinction**: `CH` is the ISO code for Switzerland (*Confoederatio Helvetica*). Do **not** confuse with `CN` (China).

##### B. Anglo-American Trends & North American Catalog: `CA`, `AU`, or `US`
- **`CA` (Canada) / `AU` (Australia)**: Full access to North American and global trending audios with significantly less commercial push and fewer affiliate streams than the US.
- **`US` (United States)**: Maximum creator and audio catalog, but carries the heaviest native deployment of TikTok Shop, live shopping cards, and commercial anchors.
  - *Recommendation*: When using `US`, ensure `Hide TikTok Shop & Mall` and `Feed Live Stream Blocker` are activated.

##### C. Problematic Regions to Avoid

| Region | Code | Operational Issue / Rationale |
| :--- | :---: | :--- |
| **China** | `CN` | **Total Lockout**: Mainland China uses the dedicated *Douyin* client. Spoofing `CN` causes the global TikTok client to fail server authentication and reject feed requests. |
| **India** | `IN` | **Government Ban**: TikTok services are blocked nationwide; API requests will fail to resolve. |
| **Russia** | `RU` | **Frozen Feed**: International uploads and global recommendation feeds remain suspended since 2022. |
| **Japan** | `JP` | **Muted Audios**: Stringent domestic copyright regulations (JASRAC) silence a high percentage of international and commercial tracks. |
| **Germany** | `DE` | **Audio Licensing Restrictions**: Strict music rights enforcement (GEMA) silences popular audio tracks, alongside EU regulatory consent dialogs. |
| **Southeast Asia** (`ID`, `TH`, `VN`, `MY`, `PH`) | — | **Heavy Commercial Saturation**: Primary testing ground for live commerce, floating shopping baskets, and affiliate showcases. |

##### D. Supported Region Codes (ISO 3166-1 alpha-2)

The patch accepts any valid **2-letter ISO 3166-1 alpha-2** country code. Inputs are case-insensitive (e.g. `us`, `US`, and `Us` resolve identically).

| Region | ISO Code | Description / Feed Scope |
| :--- | :---: | :--- |
| **Switzerland** | `CH` *(Default)* | **Recommended**: Cleanest interface, zero TikTok Shop bloat, full audio catalog, no EU DMA modals |
| **United States** | `US` | Global catalog, unrestricted English feed, US creator content (pair with Shop & Live debloat patches) |
| **Canada** | `CA` | Canadian feed & North American audio catalog (less commercial bloat than US) |
| **Australia** | `AU` | Australian & Oceania feed and trending catalog |
| **United Kingdom** | `GB` | UK feed & European creator catalog |
| **Spain** | `ES` | Spanish domestic feed & European audio library |
| **Mexico** | `MX` | Mexican & North Latin American Spanish feed |
| **Argentina** | `AR` | Southern Cone Spanish feed |
| **Brazil** | `BR` | Brazilian Portuguese feed & Latin American trends |
| **France** | `FR` | French feed & Francophone catalog |
| **Italy** | `IT` | Italian domestic feed |
| **South Korea** | `KR` | Korean feed, K-Pop trends, local live streams |
| **Singapore** | `SG` | Southeast Asian English & regional catalog |
| **Japan** | `JP` | Japanese localized feed (subject to JASRAC audio restrictions) |
| **Germany** | `DE` | German feed & Central European catalog (subject to GEMA audio restrictions) |

<details>
<summary><b>🔍 View comprehensive list of 50+ ISO 3166-1 alpha-2 country codes</b></summary>
<br>

```text
AR, AT, AU, BE, BR, CA, CH, CL, CO, CZ, DE, DK, ES, FI, FR, GB, GR, HK, HU, ID,
IE, IL, IN, IS, IT, JP, KR, MX, MY, NL, NO, NZ, PE, PH, PL, PT, RO, SA, SE, SG,
TH, TR, TW, UA, US, UY, VN, ZA
```

</details>

---

### 2. Video Quality Governor

The **`Video Quality Governor`** patch enforces user-configured maximum resolution ceilings (`1080p`, `720p`, `540p`, `480p`, or unconstrained) across video feeds while allowing independent configuration of download quality. While standard TikTok features like "Data Saver" only compress network transfers under cellular conditions without capping hardware decoders, this governor caps the actual rendition ladder (`bitRateList` and `SimBitRate`) parsed by PlayerKit/TTPlayer, reducing hardware MediaCodec load, thermals, GraphicBuffers memory consumption, and frame drops on lower-spec or battery-sensitive devices.

Crucially, **playback quality and download quality are decoupled**: users can browse their feed in battery-efficient 480p while downloading clean videos in full 1080p.

| Option | Key | Type | Default | Supported Ceilings | Description |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Maximum Playback Resolution** | `maxQuality` | String | `480` | `1080`, `720`, `540`, `480`, `none` | Caps video playback height in vertical pixels. Discards higher rendition profiles in feed. |
| **Maximum Download Resolution** | `maxDownloadQuality` | String | `1080` | `1080`, `720`, `540`, `480`, `none` | Sets download resolution ceiling independently of playback, allowing high-fidelity saving. |

#### Supported Resolution Ceilings

| Option String | Resolution Height | Typical Bitrate Band | Target Profile & Resource Rationale |
| :--- | :---: | :--- | :--- |
| `none` | Uncapped | Source bitrates | **Unconstrained**: Preserves the highest bitrate stream provided by TikTok servers without modification. |
| `1080` | 1080p | ~2500–4000 kbps | **ExtremelyHigh**: Uncapped full-HD rendition; recommended default for downloads. |
| `720` | 720p | ~1200–2000 kbps | **SuperHigh**: High-definition baseline balancing sharp visual fidelity with moderate GPU decoding. |
| `540` | 540p | ~800–1200 kbps | **H_High**: Balanced midpoint optimizing fluid 60fps feed scrolling without thermal buildup. |
| `480` *(Playback Default)* | 480p | ~500–800 kbps | **High**: Recommended sweet spot significantly reducing GraphicBuffers RAM allocation and decoding wattage. |

---

### 3. Display Refresh Rate Governor

The **`Display Refresh Rate Governor`** patch locks TikTok's window rendering frequency to peak hardware refresh rates (120Hz/90Hz) or a user-selected ceiling, neutralizing TikTok's internal refresh rate downclocking mechanisms. Under standard execution, PlayerKit lowers the window refresh rate to match video fps (typically 24–30fps or 60fps), which creates perceptible UI stutter when interacting with comments, scrolling feeds, or viewing overlays.

| Option | Key | Type | Default | Supported Values | Description |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Target Refresh Rate** | `targetRate` | String | `max` | `max`, `120`, `90`, `60` | Select target display refresh rate. `max` queries display hardware for highest supported rate. Any value exceeding physical screen capability is automatically clamped. |

#### Hardware Query & Boundary Clamping
In `TikTokRefreshRateHook.resolveTargetRate()`, queries `Display.getSupportedModes()` (Android M+) with fallback to `Display.getSupportedRefreshRates()` to detect physical display capabilities. If a configured rate exceeds physical panel frequency (e.g. selecting 120Hz on a 90Hz or 60Hz display), it automatically clamps to `maxSupportedRate` to prevent fatal WindowManager mode switch aborts.

---

### 4. Custom Offline Videos Limit

The **`Custom Offline Videos Limit`** patch customizes the maximum video caching limit available in TikTok's native Offline Mode bottom sheet. While stock TikTok restricts offline download caching to fixed tiers (e.g. 50, 100, 150), this patch injects a user-configurable count into the options list while preserving native Keva persistence and the Auto-adjust option.

| Option | Key | Type | Default | Supported Values | Description |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Custom Offline Videos Limit** | `customLimit` | Integer | `200` | Any integer `1` to `50000` | Maximum number of offline videos that can be cached for offline playback. |

---

## 🔒 Deep Technical Patch Breakdown

### 1. Media Usability & Watermark-Free Downloader (`mediaEnhancementsPatch`)
- **Forced Download Button Unblock**:
  - Hooks `Aweme.isPreventDownload()Z` -> returns `false`.
  - Hooks `Aweme.getIsCommentPostVideo()Z` -> returns `false`.
  - Flips internal `canDownload` boolean fields via reflection in `TikTokMediaHook`.
  - Hooks download action `LX/0HJb;->enable()Z` -> returns `true`, guaranteeing the Save action in the Share panel is clickable and never grayed out.
- **Clean Watermark-Free Downloads**:
  - Hooks `Aweme.getDownloadWithoutWatermark()Z` -> returns `true`.
  - Hooks `Aweme.needTTSWatermarkWhenDownload()Z` -> returns `false`.
  - Extracts clean, original `playAddr` media stream URLs, bypassing server-side watermark render queues.
- **Client-Side Watermark Neutralization**:
  - Stubs `WaterMarkServiceImpl.waterMark()` with `return-void` to prevent client-rendered overlay compositing.

### 2. Auto-Translate Comments (`commentAutoTranslatePatch`)
- Automatically translates incoming comments using TikTok's native translation engine.
- Targets `BaseCommentCell` and injects `TikTokCommentTranslateHook.registerCommentCell`.
- Captures deserialized comment lists (`commentListLoadedFingerprint`) and queues comments for batch translation.
- Deduplicates translation requests using composite keys (`requestKey + ":lang:" + currentLanguagePolicyKey`).
- Respects native "Do not translate" language preferences from `TranslationLangKevaServiceImpl`.

### 3. Device Privacy Guard (`devicePrivacyGuardPatch`)
> [!NOTE]
> **Bytecode-Only Privacy Architecture (`ResourceMode.RAW`)**:
> Unlike apps with standard resource structures, TikTok's entire patch suite strictly avoids resource decoding (`resourcePatch`). Re-encoding TikTok's obfuscated resource tree via `arsclib` drops launcher icon drawables (`res/a/aq2.xml`, `res/a/aq3.xml`). Privacy is enforced at the Dalvik bytecode execution layer via runtime permission interception, in-app nag suppression, sensor silencing, and telemetry neutralization while keeping APK resources intact.

- **Runtime Permission Interception & Denial Caching**:
  - Intercepts ByteDance Helios static dispatcher (`LX/02z2;->LLJ`) for `Activity.requestPermissions`.
  - Intercepts PowerPermissions headless engine (`FakeFragment;->jT`) to immediately dispatch permanent denials (`PackageManager.PERMISSION_DENIED`) for invasive permissions (`READ_CONTACTS`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `ACCESS_LOCAL_NETWORK`).
  - Records permanent user denials into Keva stores (`FriendsSharePreferences -> read_contact_denied = true`, `permission_store -> <perm> = true`) so the app treats permissions as permanently denied and suppresses repeated prompts.
  - Hooks permission cache check (`LX/04DS;->LIZ`) to report blocked permissions as permanently denied.
- **In-App Permission Dialog & Location Popup Suppression**:
  - Suppresses relation/contacts synchronization auth dialogs (`RelationAuthDialogControl.LJIIIIZZ` -> `false`, `RelationAuthDialogControl.LJI` -> `false`).
  - Suppresses location popups and scenes (`LX/0BK7` popup checks and `LocationServiceImpl.LJIIZILJ`, `LJIJ`).
- **Background Sync Lego Task Neutralization**:
  - Stubs background contacts sync tasks (`ContactsUploadRequest.run`, `PermissionRequestAndUploadLegoTask.run`, `IMContactInitTask.run`, `MafFollowBackBootRequest.run`).
  - Stubs background location init tasks (`InitLocationTask.run`, `InitLocationTaskHolder$Background.run`, `InitLocationTaskHolder$Main.run`).
- **Google Advertising ID (GAID) Zeroing**:
  - Hooks GAID providers (`LX/02z2;->LLLLIIL` -> `null`, `LLLLIIIILLL` -> `"00000000-0000-0000-0000-000000000000"`).
- **Universal Package Query Isolation**: Intercepts `PackageManager` query trampolines (`LX/00m8.U3`, `LX/00m8.R3`) and throws a controlled `NameNotFoundException` when external installed apps are queried, isolating app visibility without requiring resource decoding.
- **Contacts Isolation**: Neutralizes BPEA contacts reader `LX/0OFU.LIZ()` to return empty list and `LX/0OFw.LIZ()` to return a null cursor safely.
- **HAR Motion Sensor Silencing**: Injects `return -1` into `HarSensorManager` init and stubs `onSensorChanged` to stop physical movement fingerprinting.
- **FLAG_SECURE Bypass**: Permanently clears `FLAG_SECURE` (`0x2000`) on window layouts via `AntiScreenRecordController.applyFlag`, enabling screenshots and screen recordings in all views.
- **Clipboard Protection**: Blocks programmatic and BPEA background clipboard reading methods (`LX/01ZZ.LIZ`, `LX/0jUy.LIZIZ`).

### 4. Feed Bloat & Distraction Blocker (`feedBloatBlockerPatch`)
- **Rewards Pendants Suppression**: Injects `return-void` into `SpecActWidget.bind(ViewGroup)` to prevent floating Rewards widgets (countdown coins, soccer ball stickers) from attaching to feed views.
- **Sticker & Card Stripping**: Nullifies `Aweme.getActivityPendant()`, `getCommerceStickerInfo()`, `getFloatingCardInfo()`, and `getBannerTip()`.
- **Feed Stream Purge**: Filters suggested accounts, mini-games, CapCut creation prompts, memories recaps, mini-dramas, and surveys before UI adapter binding.
- **Friends Feed Rec User Cards & Bloat Neutralization**: Injects `p1 = false, p2 = false` into `FriendsV3RecUserConfig.<init>(ZZ)V` to prevent `FriendsV3FeedListViewModel` from instantiating suggested friend cards (`FriendsV3RecUserItem`) or bottom recommendation lists (`FriendsV3BottomRecListItem`). Intercepts `FriendsV3FeedResponse.<init>` to filter in-feed bloat and nullify suggested friends (`newlyShownMafIds = null`), and hooks `FriendsFeedResponse.<init>` (V2) to prune inserted card results (`cardInsertResults = null`, `insertedResults = null`). Injects immediate `View.GONE` and dimensions contraction (`0x0`) into `FriendsV3HorizontalRecUserCardCell.onItemViewCreated` and `FriendsV3BottomRecUserListCell.onItemViewCreated` as a fallback UI defense.

### 5. Hide Top-Left LIVE Button (`hideTopLiveEntrancePatch`)
- Removes the top-left LIVE broadcast button and tab entry point from the top navigation toolbar.
- Hooks `LiveIconGenerator.enabled()Z` -> returns `false`.
- Hooks `LiveIconGenerator.LIZLLL()Z` -> returns `false`.
- Hooks `LiveIconGenerator.b2(Context)View` -> returns `null` to prevent view inflation and attachment.
- Hooks `LiveTabProtocol.enable()Z` -> returns `false` to suppress top live tab variants.

### 6. Hide STEM & Community Tabs (`hideStemAndCommunityTabsPatch`)
- Removes the STEM and Comunidad (Explore / Topics) tabs from the top navigation feed strip, leaving only the primary Following and For You feeds.
- Hooks `StemTabProtocol.enable()Z` -> returns `false`.
- Hooks `ExploreXTabProtocol.enable()Z` -> returns `false`.
- Hooks `BaseTopicTabProtocol.enable()Z` -> returns `false` (disabling all inherited topic tabs such as Fashion, Food, Gaming, and Sports).
- Hooks `BaseTopicFCPTabProtocol.enable()Z` -> returns `false`.
- Hooks `BasePersonalizedTabProtocol.enable()Z` -> returns `false`.

### 7. Hide Profile Photo Follow Button (`hideAvatarFollowButtonPatch`)
- Hides the red plus (`+`) follow badge on creator profile avatars in the feed and eliminates accidental follow touches.
- Hooks `FeedAvatarDefaultAssem.cs(ViewGroup, int, Object)` -> permanently sets visibility to `View.GONE` (`0x8`) and disables clickability.
- Injects immediate `View.GONE` (`0x8`) and `setClickable(false)` into `FeedAvatarDefaultAssem.onViewCreated` right after `LLLIILIL` (`follow_view_container`) is assigned, preventing view flash on cell recycling.

### 8. Disable Profile Photo LIVE Status (`disableAvatarLiveStatusPatch`)
- Removes the pulsing LIVE ring animation and LIVE badge from creator avatars in the feed and ensures avatar taps route strictly to the creator's user profile instead of launching the live stream broadcast.
- Dynamically locates and hooks the author live validator (`LX/09A7;->LIZIZ(Aweme, User)Z`) -> returns `false`.
- Hooks `FeedAvatarAssemWrap.Yr()Z` -> returns `false`, preventing the attachment and lifecycle execution of `FeedAvatarLiveAssem`.
- Hooks `FeedAvatarLiveAssem.ur()Z` -> returns `false`.
- Stubs `FeedAvatarLiveAssem.Ar(ZZ)V` and `FeedAvatarLiveAssem.onBind(Object)V` with `return-void` to eliminate live streaming UI bindings and animations.
- Preserves `FeedAvatarDefaultAssem`'s default avatar click handler (`LX/0BIx`), routing taps directly to `//user/profile`.

### 9. Disable Comment Suggested Emojis (`disableCommentSuggestedEmojisPatch`)
- Removes the horizontal bar of suggested quick emojis displayed above the comment input box in active keyboard mode and passive comment views.
- **Active Keyboard Trigger Neutralization**: Hooks `ExposedEmojiPanelTrigger.wr(CommentContextSource, ...)Z` -> returns `false`, preventing the trigger manager from mounting `HorizontalEmojiMiniPanelAssemForKeyboard` into the active comment keyboard view hierarchy.
- **Passive Feed Input Trigger Neutralization**: Hooks `CommentPanelFakeInput.Gt()Z` -> returns `false`, preventing `HorizontalEmojiMiniPanelAssem` from attaching to the passive/feed comment bar prior to keyboard expansion.
- **ViewModel Model Flag Enforcement**: Hooks `CommentKeyboardModel.getForceDisableExposedEmoji()Z` -> returns `true`, enforcing TikTok's native internal model flag across comment view models and cells.
- **Internal Experiment Flag Enforcement**: Hooks `PersonalizedEmojiExperiment.LIZ()Z` -> returns `true` (`hideExposeEmoji`), suppressing emoji resource preloading, layout spacing allocation, and telemetry events.

### 10. Disable Story Feed Indicators (`disableStoryFeedIndicatorsPatch`)
- Removes the top-center story drop-down indicator pill (e.g. "1 Story ▼") and creator profile photo story rings from feed videos, ensuring avatar photos remain permanently clean.
- **User Story Status Neutralization**: Hooks `User.getStoryStatus()I` -> returns `0`, preventing feed wrappers from detecting active author stories.
- **Feed Avatar Story Ring & Click Neutralization**: Stubs `FeedAvatarSocialPublishAssem.onViewCreated(View)V`, `FeedAvatarSocialPublishAssem.onBind(Object)V`, and `FeedAvatarSocialPublishAssem.tr(VideoItemParams)V` with `return-void` to prevent inflating/animating the cyan story ring and remove the `CLICK_TAG_FEED_AVATAR_SOCIAL` click interceptor, keeping the avatar clean and routing taps strictly to the creator profile.
- **Social Publish Distributor**: Hooks `SocPubDistributeServiceImpl.LJII(User)Z` -> returns `false`.
- **Feed Story Tag Trigger & Predicate Neutralization**: Dynamically locates and hooks the story tag visibility evaluator (`LX/0AZy;->LIZ(Context, Aweme, String)Z`) -> returns `false`, and forces `FeedStoryTagTrigger.Kr()Z` and `FeedStoryTagTriggerV2.Kr()Z` to return `false`.
- **Story Tag Assem & Canvas Neutralization**: Stubs `FeedStoryTagAssem.Sr(VideoItemParams)V`, `FeedStoryTagAssem.onBind(Object)V`, `FeedStoryTagAssemV2.Sr(VideoItemParams)V`, `FeedStoryTagAssemV2.onBind(Object)V`, and `StoryTag.onDraw(Canvas)V` with `return-void`.

### 11. Hide Feed Search Bar (`hideFeedSearchBarPatch`)
- Removes the search suggestion pill and trending bar (e.g. "Search · <keyword> >") displayed directly above the bottom navigation bar on feed videos, eliminating search clutter and distraction.
- **Trigger Component Suppression**: Hooks `Kr(VideoItemParams)Z` -> returns `false` across `FeedSearchBottomBarAssemTrigger`, `FeedSearchBottomBarAssemTriggerV2`, `TrendingBottomBarAssemTrigger`, `AdFeedSearchBottomBarAssemTrigger`, and `FeedEcSearchBottomBarAssemTrigger`, preventing the bottom bar from ever mounting into the feed cell.
- **Assem Lifecycle Neutralization**: Stubs `onViewCreated(View)V` and `onBind(Object)V` across `FeedSearchBottomBarAssem`, `FeedSearchBottomBarAssemV2`, `TrendingBottomBarAssem`, `AdFeedSearchBottomBarAssem`, and `FeedEcSearchBottomBarAssem` with `return-void`. Also stubs `FeedSearchBottomBarAssem.Sr()V` to prevent layout inflation and view binding.
- **Aweme Model Overrides**: Forces `Aweme.isDisableSearchTrendingBar()Z` to return `true`, `Aweme.hasTrendingBar()Z` and `Aweme.hasTrendingBarFYP()Z` to return `false`, and nulls out `getTrendingBar()`, `getTrendingBarFYP()`, and `getHotSearchInfo()`.

### 12. Force auto-scroll (`forceAutoScrollPatch`)
- Forces the activation of TikTok's native video auto-scroll experiment flag for accounts and regions where it is withheld by server-side A/B testing experiments.
- **Feed Auto-Scroll A/B Experiment Flag**: Hooks the core experiment evaluator referencing `"fyp_auto_scroll"` -> returns `true`.
- **FypAutoScrollServiceImpl Capability Bridge**: Forces `FypAutoScrollServiceImpl.LJIILJJIL()` -> returns `true`, granting the feed panel full auto-scroll capabilities.
- **Tablet and Search Auto-Scroll**: Forces tablet/foldable (`"tablet_fyp_auto_scroll"`) and search feed (`"search_auto_scroll"`) experiment flags to return `true`.
- **FeedBottomBarFacade Capability Hook**: Forces `FeedBottomBarFacadeImpl.LJIJJLI()` referencing `"panel_auto_scroll"` to return `true`.
- **Long-Press Menu Item Constructor**: Overrides `createAutoScrollItem` in `LX/0oWb;->LJII`, neutralizing the `"panel_auto_scroll"` guard and the `isLogin()` requirement so that the Auto-scroll toggle option is permanently available and visible in the video long-press / share menu.

### 13. Disable Search History Recording (`disableSearchHistoryRecordingPatch`)
- Prevents search queries, keywords, and search interactions from being written to persistent local history or device storage.
- **Search History Manager Hook**: Stubs `LX/0D7Z;->LIZ(SearchHistory, String)V` with `return-void` to neutralize search history record persistence.
- **Manual Search PV Tracking Hook**: Stubs `ManualSearchPvStore.LJIIJ(String, String)V` with `return-void` to prevent manual search pageview and query history accumulation.
- **Top History Recommendation Suppression**: Hooks `SuggestWordResponse.getTopHistoryWords()` -> returns `null` to neutralize server-pushed search history suggestions.

### 14. Hide Suggested Searches (`hideSuggestedSearchesPatch`)
- Removes the 'Podría interesarte' (You may like / Guess Search) suggested keyword recommendation card from the search intermediate discovery screen.
- **Search Intermediate Raw Payload & Model Filtering**: Intercepts `RecomDataWrapper.<init>(String, SuggestWordResponse)` to filter out `"guess_search"` card items from the raw JSON payload and parsed response model before Lynx rendering.
- **Cached Guess Search Preload Neutralization**: Hooks `LX/0HMZ;->LIZ()Lorg/json/JSONObject;` and `LX/0HMZ;->LIZIZ()Ljava/lang/String;` to return `null`, eliminating cached suggestion preloading on startup.
- **Native Guess Search Fallback Override**: Forces `DynamicSingleIntermediateFragmentNew.yU()Z` -> returns `false` to disable native guess search fallback rendering.

### 15. Hide Popular Lives In Search (`hideSearchPopularLivesPatch`)
- Removes the 'LIVE populares' (Popular LIVEs) recommendation card and stream broadcasts from the search intermediate discovery screen.
- **Search Intermediate Raw Payload & Model Filtering**: Intercepts `RecomDataWrapper.<init>(String, SuggestWordResponse)` to filter out `"live_popular"` card items from the raw JSON payload and parsed response model before Lynx rendering.

