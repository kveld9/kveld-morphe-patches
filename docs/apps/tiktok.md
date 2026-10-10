# 🎵 TikTok: Complete Patch, Architecture & Configuration Guide

Comprehensive technical, architectural, and configuration guide for **TikTok** (`com.zhiliaoapp.musically`), pinned to target version **`47.1.4`**.

---

## 🎯 Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target Application** | TikTok |
| **Package Name** | `com.zhiliaoapp.musically` |
| **Supported Target Version** | **`47.1.4`** |
| **Target File Format** | Standalone APK (`APK` - **nodpi**) |
| **Recommended Architecture** | `arm64-v8a` |
| **Official Download Source** | [APKMirror: TikTok (nodpi)](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-47-1-4-release/) |

> [!IMPORTANT]
> Always download the standalone `nodpi` APK variant for `arm64-v8a`. Do not use split APK bundles (`bundle` / `apkm`).

---

## 📋 Applied Patches Catalog

| Category | Patch Name | Type | Key Target / Mechanism |
| :--- | :--- | :--- | :--- |
| **Usability** | **Media Usability & Watermark-Free Downloader** | `bytecodePatch` | Unblocks download button in Share panel, extracts clean original streams without watermark stamps, with saved-video quality preference (`downloadQuality`: high/medium/low or 1080/720/540/480/360 ceiling) and watermark toggle (`removeWatermark`). |
| **Usability** | **Disable Post-Download Share Dialog** | `bytecodePatch` | Suppresses the automatic 'Share to' and friend suggestions bottom sheet that pops up after finishing a download. |
| **Usability** | **[Show Seekbar](#8-show-seekbar)** | `bytecodePatch` | Restores video seekbar and scrubbing controls where hidden or disabled. |
| **Usability** | **Always Show Publish Date** | `bytecodePatch` | Forces video publish and upload timestamps to remain permanently visible on feed cards. |
| **Usability** | **Show Author Region** | `bytecodePatch` | Displays the creator's country or region code next to their username in video author info across feeds and deep-linked detail views. |
| **Usability** | **Skip Content Warnings** | `bytecodePatch` | Bypasses and clears sensitive content warnings, graphic media blur overlays, and age gates on feed videos. |
| **Usability** | **[Comment Customizer](#2-comment-customizer-commentcustomizerpatch)** | `bytecodePatch` | Customizes comment section: native sort controls, clean text copying, disabling suggested emojis bar, hiding comment quick actions, hiding in-comment surveys and feedback cards, hiding profile photo story rings, voice comments, automatic translation with do-not-translate language exclusions (`translationExcludedLanguages`), comment send fix, comment popup ad blocking. |
| **Usability** | **Disable Double Tap to Like** | `bytecodePatch` | Disables the double tap gesture to like videos in the feed, preventing accidental likes while scrolling or pausing. Videos can still be liked using the like button. |
| **Usability** | **Playback Speed Persistence** | `bytecodePatch` | Persists user-selected video speed across feed scrolling and restarts, with optional native hold-and-slide 2x speed lock gesture (`enableSpeedLock`). |
| **Usability** | **[Video Quality Governor](#2-video-quality-governor)** | `bytecodePatch` | Playback resolution ceiling (e.g. 480p). Download quality lives in Media Usability (`downloadQuality`). |
| **Usability** | **Skip First-Launch Onboarding** | `bytecodePatch` | Bypasses interest pickers, swipe-up tutorial, language prompts, and consent sheets directly to FYP. |
| **Usability** | **[Custom Offline Videos Limit](#4-custom-offline-videos-limit)** | `bytecodePatch` | Customizes maximum offline videos download caching limit (~X mins, Y GB/MB). |
| **Usability** | **[Custom Share Sheet](#5-custom-share-sheet)** | `bytecodePatch` | Customizes and cleans the share menu via individual boolean toggles for third-party apps, essential actions, and direct message friend rows. |
| **Usability** | **[Clean Share Panel](#6-clean-share-panel)** | `bytecodePatch` | Removes suggested quick emojis and the 'Send to new group' button from the direct share dialog. |
| **Usability** | **Navigation & Header Declutter** | `bytecodePatch` | Removes clutter from the feed navigation and top header bar, including Nearby and Community tabs, top-left LIVE button, central '+' create content button, in-video bottom search bar, friend profile pictures on the Friends tab, and unread message badges on the Inbox tab. |
| **Usability** | **[Feed Interface Declutter](#7-feed-interface-declutter-feedinterfacedeclutterpatch)** | `bytecodePatch` | Customizes and cleans feed video overlay elements via individual toggles for full screen button, repost pill, interest feedback pills, video descriptions, profile photo follow badges, story rings, playlist bottom bars, save buttons, and music discs. |
| **Usability** | **Disable Profile Photo LIVE Status** | `bytecodePatch` | Removes pulsing LIVE ring/badge from creator avatars in feed and forces clicks directly to user profile. |
| **Usability** | **Force Auto-Scroll** | `bytecodePatch` | Forces the activation of the native video auto-scroll experiment flag for accounts and regions that lack it due to A/B testing. |
| **Usability** | **Hide Popular Lives In Search** | `bytecodePatch` | Removes the Popular LIVEs recommendation card and live stream broadcasts from the search discovery page. |
| **Usability** | **Enable Live Search** | `bytecodePatch` | Shows TikTok's search entry in the Live drawer where supported. |
| **Usability** | **Hide Suggested Searches** | `bytecodePatch` | Removes the suggested search keywords section ('You may like' / 'Search suggestions') from the search discovery page. |
| **Usability** | **Disable Search Video Autoplay** | `bytecodePatch` | Disables automatic video playback in search results. Videos only play when tapped to view in detail. |
| **Usability** | **Auto-Pause First Video** | `bytecodePatch` | Automatically pauses the initial video on startup (frame 0) with center play icon; resumes upon screen tap or feed scroll. |
| **Usability** | **Hide Seen Videos** | `bytecodePatch` | Filters previously watched videos from incoming For You feed batches based on playback progress. |
| **Usability** | **Feed Content Filter** | `bytecodePatch` | Hides stories, photo posts, and videos outside configured view or like ranges from feeds (`minViews`/`maxViews`/`minLikes`/`maxLikes`/`hideStories`/`hidePhotoPosts`). |
| **Usability** | **Resume Video After Scroll** | `bytecodePatch` | Resumes video playback from previous playback position when returning to a video in the feed. |
| **Usability** | **Remember Clear Display** | `bytecodePatch` | Remembers TikTok's clear-display state between videos and re-applies it when new videos start. |
| **Usability** | **Stop Video Looping** | `bytecodePatch` | Prevents videos from looping continuously on playback completion. |
| **Usability** | **[Hide Inbox Promos & Alerts](#9-hide-inbox-promos--alerts)** | `bytecodePatch` | Hides promotional banners, streak mascot cards, contact sync suggestions, friend recommendations, and migration guide tooltips in the inbox and direct messages. |
| **Usability** | **Hide Inbox Story & Status Tray** | `bytecodePatch` | Removes the horizontal story, notes, and status tray (Skylight) displayed at the top of direct messages and the inbox. |
| **Usability** | **[Direct Message Declutter](#10-direct-message-declutter)** | `bytecodePatch` | Cleans direct message conversations and chat list items via individual toggles for chat list camera icons, header voice/video call buttons, message forward buttons, streak mascot and reaction bars, input camera buttons, right-side input action buttons (gallery, emoji, mic), try effect buttons on shared videos, and automatic sticker reply suggestions. |
| **Usability** | **[Disable Feed Long-Press Actions](#7-disable-feed-long-press-actions)** | `bytecodePatch` | Disables long-press action gestures on feed buttons, including Like to repost, Share to quick DMs, and Comment to quick emojis, with optional video-body long-press modes (`longPressVideo`). |
| **Usability** | **Enable Profile Banner** | `bytecodePatch` | Unlocks custom profile banner header cover feature and banner editing tools in Edit Profile. |
| **Usability** | **System Font** | `bytecodePatch` | Forces TikTok to use the Android system font instead of bundled proprietary TikTokSans fonts. |
| **Usability** | **[Popups & Prompts Suppressor](#24-popups--prompts-suppressor-popupsandpromptssuppressorpatch)** | `bytecodePatch` | Suppresses intrusive popups, dialogs, and modal prompts, including 'Follow your friends' dialogs, contacts sync overlays, multi-account notification guides, 2SV security checkup modals, PopLayer promotional sheets, live stream teaser bubbles, sticker recommendations, and DM streak expiration warnings. |
| **Usability** | **Video Fit** | `bytecodePatch` | Adjusts video aspect ratio across feeds and story cells: fit video without cropping or fill screen (`fitMode`). |
| **Privacy** | **Camera & Microphone Indicator** | `bytecodePatch` | Shows an on-screen corner indicator while TikTok holds camera or microphone open. |
| **Privacy** | **Fix Google Login** | `bytecodePatch` | Restores Google account sign-in via Web OAuth fallback when GMS rejects modified APK signature. |
| **Privacy** | **Fix Spotify Login** | `bytecodePatch` | Restores 'Add to Spotify' by intercepting the Spotify SDK SSO intent (rejected by the Spotify app for the re-signed APK) and completing Spotify Web OAuth in a WebView hosted over the SDK `LoginActivity`. |
| **Privacy** | **Bypass Mandatory Login** | `bytecodePatch` | Neutralizes mandatory login walls, dynamic regional forced login gates, and guest browsing restrictions. |
| **Privacy** | **Bypass Screen Capture Detection** | `bytecodePatch` | Clears `FLAG_SECURE` on protected windows, restores Circle to Search / screen translate and recent apps snapshots, and neutralizes screenshot detection listeners and feedback prompts. |
| **Privacy** | **Clean Share URL** | `bytecodePatch` | Strips tracking query parameters, user tokens, and campaign IDs from shared links, with optional custom host redirection (`shareHost`). |
| **Privacy** | **Device Privacy Guard** | `bytecodePatch` | Intercepts runtime permission prompts (contacts, location, nearby devices, AdServices), ContentResolver/PackageManager/LocationManager interception, suppresses in-app permission nag dialogs, settings redirect prompts, and background sync tasks, zeroes Advertising ID, blocks clipboard inspection, isolates package queries, and silences HAR motion sensors. |
| **Privacy** | **In-App Browser Privacy Guard** | `bytecodePatch` | Redirects external links to default system browser, neutralizes WebView JS tracking injection and AJAX hookers. |
| **Privacy** | **Client-Side AI & Behavioral Profiling Governor** | `bytecodePatch` | Neutralizes Pitaya on-device ML, Tako AI chatbot entries, and AI search clutter. |
| **Privacy** | **[SIM Region Selector](#1-sim-region-selector)** | `bytecodePatch` | Spoofs SIM and network country ISO codes, operator numeric codes/names, and cell identity MCC/MNC to bypass regional restrictions. |
| **Privacy** | **Feed Ad Blocker** | `bytecodePatch` | Filters sponsored cards, brand promotions, commercial audio, and search video scroll advertisements across For You, Following, and Search feeds. |
| **Privacy** | **Hide TikTok Shop & Mall** | `bytecodePatch` | Removes product anchors, showcase badges, and bottom/top Shop navigation tabs (configurable via `hideShopTab` and `hideVideoAnchors`). |
| **Privacy** | **Friends Feed Strict Mutuals** | `bytecodePatch` | Filters suggested accounts, recommended videos, and non-mutual profiles (such as 'People you may know') from the Friends feed so it strictly reproduces content from mutual friends. |
| **Privacy** | **Hide Suggested Accounts** | `bytecodePatch` | Removes suggested-account cards from profile headers and inbox/notification surfaces. |
| **Privacy** | **Hide AI-Generated Content** | `bytecodePatch` | Filters and skips videos tagged with native AI-generated metadata, C2PA content credentials, or creator AI disclosure tags across the For You, Following, and Friends feeds. |
| **Privacy** | **Hide Promotional Content** | `bytecodePatch` | Filters and skips videos disclosing branded or paid-promotional content ('Contenido Promocional' / paid partnership tags, StarAtlas orders, branded content accounts) across the For You, Following, and Friends feeds. |
| **Privacy** | **Feed Live Stream Blocker** | `bytecodePatch` | Removes live broadcast cards and live recommendations from FYP and Following. |
| **Privacy** | **Feed Bloat & Distraction Blocker** | `bytecodePatch` | Removes friend suggestions, suggested account carousels, mini-games, CapCut prompts, memories, community/topic cards, post-video evaluation surveys, questionnaires, mini-dramas, Lemon8 promo, in-feed search recommendations/interest cards, and floating rewards pendants across For You, Following, and Friends feeds. |
| **Privacy** | **Unified Telemetry & Tracker Silencer** | `bytecodePatch` | Neutralizes ByteDance AppLog, APM/Npth/Heimdallr crash telemetry, and AppsFlyer. |
| **Privacy** | **Disable Search History Recording** | `bytecodePatch` | Prevents search queries and keywords from being recorded in local history, databases, and analytics stores. |
| **Privacy** | **Non-Personalized Search** | `bytecodePatch` | Forces TikTok's non-personalized search mode instead of the saved account choice. |
| **Privacy** | **Disable Watch History Recording** | `bytecodePatch` | Prevents viewed videos from being recorded in account watch history, playback duration stores, and local history caches. |
| **Privacy** | **Update Prompt Suppressor** | `bytecodePatch` | Neutralizes background update polling tasks and version enforcement dialogs. |
| **Privacy** | **[TikTok Privacy Permissions Stripper](#12-tiktok-privacy-permissions-stripper)** | `resourcePatch` | Selectively strips sensitive privacy, sensor, hardware, and tracking permissions from AndroidManifest.xml via 15 granular opt-in boolean toggles. |
| **Performance** | **[Display Refresh Rate Governor](#3-display-refresh-rate-governor)** | `bytecodePatch` | Locks window to peak hardware refresh rate (120Hz/90Hz) and neutralizes playback downclocking. |
| **Performance** | **[Disable HDR Video Playback](#26-disable-hdr-video-playback-disablehdrvideopatch)** | `bytecodePatch` | Forces the video playback engine to select standard SDR bitrates instead of HDR (HDR10/PQ/HLG). |
| **Performance** | **Instant Launch & Splash Blocker** | `bytecodePatch` | Eliminates cold startup delays, real-time splash advertisements, and background TopView ad preloading. |
| **Performance** | **Resource & Battery Governor** | `bytecodePatch` | Suppresses 3D shake ad sensors and video buffer preloading. |
| **Performance** | **P2P Video Relay & Mesh CDN Blocker** | `rawResourcePatch` | Strips `libavmdlp2pv2.so` and `libp2plivevdp.so` to stop background P2P CDN seeding. |
| **Performance** | **Disable Push Notifications** | `bytecodePatch` | Neutralizes background push socket polling and persistent wake locks. |
| **Performance** | **Live Stream 3D Gift Optimizer** | `bytecodePatch` | Disables 3D gift particle effect engine to eliminate live frame drops. |
| **Performance** | **Live Stream SDK & Minigame De-bloat** | `rawResourcePatch` | Strips `liblink_mic_sdk.so`, Lyrax RTC broadcaster libs, DM call engine (`libvoip.so`), live RTM/base runtimes, and battle minigames. Breaks live viewing/broadcasting and DM voice/video calls. |
| **Slimmer** | **Voice & Speech Engine De-bloat** | `rawResourcePatch` | Strips on-device voice recognition/synthesis engines (`libspeechspg.so`, `libspeechsdk.so`) and their loader stubs (`libspeechengine.so`, `libspeechepg.so`). Breaks voice search, voice input, and editor text-to-speech. |
| **Slimmer** | **Core Asset De-bloat** | `rawResourcePatch` | Strips Microblink OCR models, C2PA AI libs, ByteDance TTWebView engine, non-Latin fonts, Python VM (~77 MB saved). |
| **Slimmer** | **Studio & Creation De-bloat** | `rawResourcePatch` | Strips AR camera engine (`libeffect_plugin.so`), video editor SDK (`libttvesdk_plugin.so`), CapCut NLE libs, CutSame/Davinci template engine, camera dynamic features, upload video encoders, and on-device AI runtimes (LiteRT). |
| **Slimmer** | **Language Pack Purger** | `rawResourcePatch` | Strips unselected language string bundles from `assets/strings#lang_*`. |
| **Universal Patches Suite** | Multiple | Optimization & Privacy | Compatible with universal slimmers and privacy patches (Screen Brightness Governor, Telemetry Neutralizer, Native Binary Trimmer, WebP/PNG Optimizers, DPI/Locale Slimmers). See [Universal Patch Reference](../universal-patches.md). |

---

## ⚙️ Configurable Options in Morphe Manager

### 1. SIM Region Selector

The **`SIM Region Selector`** patch bypasses geographic content restrictions, regional feed filtering, and country-specific catalog locks by spoofing the SIM and network ISO country codes queried by TikTok.

| Option | Key | Type | Default | Range / Format | Description |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Region** | `region` | String | `CH` | 2-letter ISO 3166-1 alpha-2 code | 2-letter ISO country code to spoof for SIM and network country checks. |
| **Operator Numeric** | `operatorNumeric` | String | _(empty)_ | 5-6 digits (MCC+MNC, e.g. `22801`) | Numeric operator code spoofed at every TelephonyManager operator and CellIdentity MCC/MNC call site. Empty keeps stock values. |
| **Operator Name** | `operatorName` | String | _(empty)_ | Carrier display name | Name reported for SIM/network operator queries. Empty keeps the stock name. |

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

The **`Video Quality Governor`** patch enforces a user-configured maximum playback resolution ceiling (`1080p`, `720p`, `540p`, `480p`, `360p`, or unconstrained) across video feeds. While standard TikTok features like "Data Saver" only compress network transfers under cellular conditions without capping hardware decoders, this governor caps the actual rendition ladder (`bitRateList` and `SimBitRate`) parsed by PlayerKit/TTPlayer, reducing hardware MediaCodec load, thermals, GraphicBuffers memory consumption, and frame drops on lower-spec or battery-sensitive devices.

Coverage includes the detail page: hooks on `Video.getPlayAddr()` / `getProperPlayAddr()` (plus codec variants `getPlayAddrBytevc1()`, `getPlayAddrH264()`, `getH264PlayAddr()`) and `SimVideoUrlModel.getRawBitRate()` enforce the cap on directly-read play addresses, which the detail view reaches without passing through `Aweme.getVideo()`. Enforcement resolves the best stream at or below the cap from the video's own ladder and rewrites the returned URL model in place; when the ladder offers no stream below the cap, the lowest available stream is kept.

Download quality is **not** configured here: it lives solely in the Media Usability patch (`downloadQuality`: `high`/`medium`/`low` or a `1080`/`720`/`540`/`480`/`360` ceiling), which always wins for saved files. (The former `maxDownloadQuality` option was retired for exactly this reason: one place for download quality.)

| Option | Key | Type | Default | Supported Ceilings | Description |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Maximum Playback Resolution** | `maxQuality` | String | `480` | `1080`, `720`, `540`, `480`, `360`, `none` | Caps video playback height in vertical pixels. Discards higher rendition profiles in feed. |
| **Avoid ByteVC2 Software Decoding** | `avoidByteVC2` | Boolean | `true` | `true`, `false` | Drops ByteVC2 renditions when an H.264 or ByteVC1 alternative exists, forcing hardware decoding. |
| **Drop Undecodable Video Streams** | `dropUndecodableVideo` | Boolean | `true` | `true`, `false` | When the ladder floor exceeds the hardware decoder, keeps audio-only instead of decoder-reject retry loops. |

ByteVC2 is ByteDance's proprietary codec with no hardware decoder, so TikTok decodes it on the CPU. Dropping it lets H.264/ByteVC1 renditions play through the hardware MediaCodec decoder. As a trade-off, ByteVC1 and H.264 renditions are larger, so mobile data usage can increase slightly; if a video only offers ByteVC2, it is kept.

Measured A/B on a Moto G56 (Dimensity MT6855G, TikTok 47.1.4, 15 feed videos per arm, same build except the flag):

| Metric | avoidByteVC2 on | avoidByteVC2 off |
| --- | --- | --- |
| ByteVC2 renditions dropped | 4 | 0 |
| ByteVC2 software decoder instances created | 0 | 2 |
| Hardware (ByteVC1) decode lines | 24 | 28 |
| Software VDecod threads observed | none | VDecod2-V15/V16 active |
| Crashes | 0 | 0 |

Caveat: per-core CPU delta was not rigorously measured (point samples, different content per arm); the historical ~0.6 CPU cores per video figure comes from a Snapdragon 636 baseline, not from this test.

Undecodable guard: stream dimensions are resolved from bitrate metadata and compared against the largest long side reported by `MediaCodecList` for the stream codec family (`video/hevc` for ByteVC1/HEVC, `video/avc` otherwise). No orientation is assumed: any single known side above the hardware maximum already proves undecodability. When ladder entries hide their own size, the parent `Video` dimensions (authoritative server metadata) are used as fallback. When even the lowest ladder rendition exceeds it (e.g. `2160x3840` on a decoder topped at `2560x1440`, observed as `C2MtkVdec: BAD VALUE: Resolution not supported` retry loops), video streams are dropped and the audio track is preserved so playback fails fast instead of freezing the device. ByteVC2 is excluded (dedicated CPU decoder) and unknown dimensions or hardware fail open (ladder kept).

Single-rendition 4K limitation: when a video exposes only one rendition above both the playback cap and the hardware decoder capability (observed: 2160x3840 HEVC Main 10 HLG on a decoder topped at 2560x1440, C2MtkVdec BAD VALUE retry loop ending in TikTok's couldn't play this video error), no client-side patch can conjure the missing lower renditions. The Governor still protects every other video in feed, search, profile, and detail pages; this ladder shape remains unplayable on the affected device.

#### Supported Resolution Ceilings

| Option String | Resolution Height | Typical Bitrate Band | Target Profile & Resource Rationale |
| :--- | :---: | :--- | :--- |
| `none` | Uncapped | Source bitrates | **Unconstrained**: Preserves the highest bitrate stream provided by TikTok servers without modification. |
| `1080` | 1080p | ~2500–4000 kbps | **ExtremelyHigh**: Uncapped full-HD rendition; recommended default for downloads. |
| `720` | 720p | ~1200–2000 kbps | **SuperHigh**: High-definition baseline balancing sharp visual fidelity with moderate GPU decoding. |
| `540` | 540p | ~800–1200 kbps | **H_High**: Balanced midpoint optimizing fluid 60fps feed scrolling without thermal buildup. |
| `480` *(Playback Default)* | 480p | ~500–800 kbps | **High**: Recommended sweet spot significantly reducing GraphicBuffers RAM allocation and decoding wattage. |
| `360` | 360p | ~300–500 kbps | **Standard**: Ultra-low resource profile minimizing thermal output, battery drain, and cellular data consumption. |

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
| **Custom Offline Videos Limit** | `customLimit` | Integer | `1000` | Any integer `1` to `50000` | Maximum number of offline videos that can be cached for offline playback. |
### 5. Custom Share Sheet

The **`Custom Share Sheet`** patch cleans and customizes TikTok's native sharing bottom sheet via granular boolean toggles. It allows disabling third-party apps (WhatsApp, Facebook, Messenger, Instagram, SMS, Twitter, Telegram, Reddit, etc.), suppressing the direct message friends row, protecting or toggling native sharing capabilities (Repost, QR code, Copy link, System share), and hiding individual utility actions (Promote, Why this video, Duet, Stitch, PiP, etc.).

| Toggle Option | Key | Type | Default | Description |
| :--- | :--- | :---: | :---: | :--- |
| **Hide WhatsApp** | `hideWhatsApp` | Boolean | `true` | Hides WhatsApp and WhatsApp Status from the share sheet. |
| **Hide Instagram** | `hideInstagram` | Boolean | `true` | Hides Instagram and Instagram Stories from the share sheet. |
| **Hide Facebook & Messenger** | `hideFacebook` | Boolean | `true` | Hides Facebook, Facebook Stories, and Messenger from the share sheet. |
| **Hide Telegram** | `hideTelegram` | Boolean | `true` | Hides Telegram from the share sheet. |
| **Hide X / Twitter** | `hideTwitter` | Boolean | `true` | Hides X (Twitter) from the share sheet. |
| **Hide Snapchat** | `hideSnapchat` | Boolean | `true` | Hides Snapchat from the share sheet. |
| **Hide Reddit & Discord** | `hideReddit` | Boolean | `true` | Hides Reddit and Discord from the share sheet. |
| **Hide SMS & Messages** | `hideSms` | Boolean | `true` | Hides SMS and Google Messages from the share sheet. |
| **Hide Secondary Networks** | `hideSecondaryApps` | Boolean | `true` | Hides secondary social apps (Line, Kakao, Viber, VK, Lemon8, etc.). |
| **Hide 'Repost' Button** | `hideRepost` | Boolean | `false` | Hides the native Repost button. Note: also disables the long-press to repost gesture. |
| **Hide QR Code** | `hideQrCode` | Boolean | `false` | Hides the QR code sharing button from the share sheet. |
| **Hide 'Copy Link'** | `hideCopyLink` | Boolean | `false` | Hides the Copy link button from the share sheet. |
| **Hide System Share ('More')** | `hideSystemShare` | Boolean | `false` | Hides the system share dialog ('More') button. |
| **Hide Friends DM Row** | `hideFriendsRow` | Boolean | `false` | Suppresses the top suggested contacts/friends avatar row in the share dialog. |
| **Hide 'Promote' Action** | `hidePromote` | Boolean | `true` | Hides the commercial Promote action from the bottom utilities row. |
| **Hide 'Why This Video'** | `hideWhyThisVideo` | Boolean | `true` | Hides the recommendation explanation action from the bottom utilities row. |
| **Hide 'Create Group' Action** | `hideCreateGroup` | Boolean | `false` | Hides the Create group action from the bottom utilities row. |
| **Hide 'Add to Story'** | `hideAddToStory` | Boolean | `false` | Hides the Add to Story action from the bottom utilities row. |
| **Hide 'Create Sticker'** | `hideCreateSticker` | Boolean | `false` | Hides the sticker creation tool from the bottom utilities row. |
| **Hide 'Duet' Action** | `hideDuet` | Boolean | `false` | Hides the Duet action from the bottom utilities row. |
| **Hide 'Stitch' Action** | `hideStitch` | Boolean | `false` | Hides the Stitch action from the bottom utilities row. |
| **Hide 'Picture-in-Picture'** | `hidePip` | Boolean | `false` | Hides the Picture-in-Picture floating player action. |
| **Hide 'Clear Display'** | `hideClearDisplay` | Boolean | `false` | Hides the Clear display mode action. |
| **Hide 'Background Audio'** | `hideListenAudio` | Boolean | `false` | Hides background audio playback action. |
| **Hide Live Wallpaper & GIF** | `hideWallpaperAndGif` | Boolean | `false` | Hides Live wallpaper and GIF creation actions. |
| **Hide 'Not Interested'** | `hideNotInterested` | Boolean | `false` | Hides the 'Not interested' action. |
| **Hide 'Report'** | `hideReport` | Boolean | `false` | Hides the Report action. |

### 6. Clean Share Panel

The **`Clean Share Panel`** patch removes clutter from the direct message sharing bottom sheet when selecting contacts or composing a message to friends. Governed by two independent toggle switches, it eliminates the suggested horizontal quick emoji bar and the persistent "Send to new group" button.

| Toggle Option | Key | Type | Default | Description |
| :--- | :--- | :---: | :---: | :--- |
| **Hide Quick Emojis** | `hideQuickEmojis` | Boolean | `true` | Removes the horizontal row of suggested quick emojis (🥰, 👍, 😂, etc.) from the direct share panel. |
| **Hide 'Send to New Group'** | `hideSendToNewGroup` | Boolean | `true` | Removes the 'Send to new group' button and hint from the direct share panel. |

### 7. Disable Feed Long-Press Actions

The **`Disable Feed Long-Press Actions`** patch neutralizes long-press gesture detectors on the primary feed action buttons, preventing accidental menu popups while preserving native single-click interactions. It is governed by three independent boolean toggle switches and a configurable video body long-press action.

| Toggle Option | Key | Type | Default | Description |
| :--- | :--- | :---: | :---: | :--- |
| **Disable Long-Press Like (Repost)** | `disableLikeRepost` | Boolean | `true` | Prevents holding the Like button on feed videos from opening the Repost action panel. |
| **Disable Long-Press Share (Quick DMs)** | `disableShareQuickDms` | Boolean | `true` | Prevents holding the Share button on feed videos from opening the quick share recent contacts tray. |
| **Disable Long-Press Comment (Quick Emojis)** | `disableCommentReactions` | Boolean | `true` | Prevents holding the Comment button on feed videos from opening the quick reaction emojis picker. |
| **Long Press Video Action** | `longPressVideo` | String | `nothing` | Action when long pressing feed video body: `nothing` (suppresses menu/repost), `comments`, `copyLink`, or `saveSound`. |

### 8. Show Seekbar

The **`Show Seekbar`** patch restores TikTok's native video seekbar and scrubbing controls on feed videos where they are normally hidden or disabled.

| Toggle Option | Key | Type | Default | Description |
| :--- | :--- | :---: | :---: | :--- |
| **Show Dragging Thumbnail Preview** | `showThumbnail` | Boolean | `true` | Displays real-time video frame thumbnail previews while dragging the seekbar thumb. |

### 9. Hide Inbox Promos & Alerts

The **`Hide Inbox Promos & Alerts`** patch removes promotional and nudge elements from the inbox and direct messages. It is governed by three independent boolean toggles:

| Toggle Option | Key | Type | Default | Description |
| :--- | :--- | :---: | :---: | :--- |
| **Hide Top Promotional Banners** | `hideTopPromosAndBanners` | Boolean | `true` | Hides header banners including the streak pet / mascot, event announcements, top notice banners, and phone link prompts. |
| **Hide Contact & Friend Recommendations** | `hideContactRecommendations` | Boolean | `true` | Hides user recommendation cards (Chat with contacts / Find and chat with them), suggested accounts, and mutual friends modules in the chatlist. |
| **Hide Navigation Notices & Tooltips** | `hideNavigationNoticesAndTooltips` | Boolean | `true` | Hides UI migration tooltips (e.g. New followers has moved), bulletin board guide banners, and Shop migration notices. |

### 10. Direct Message Declutter

The **`Direct Message Declutter`** patch cleans direct message conversations and chat list items via individual boolean toggles:

| Toggle Option | Key | Type | Default | Description |
| :--- | :--- | :---: | :---: | :--- |
| **Hide Chat List Camera Icon** | `hideChatListCamera` | Boolean | `true` | Hides the quick camera action icon on conversation rows in the DM inbox chat list. |
| **Hide Header Call Button** | `hideCallButton` | Boolean | `true` | Hides the audio/video call button (receiver icon) in the 1-on-1 chat header bar. |
| **Hide Message Forward Button** | `hideMessageForwardButton` | Boolean | `true` | Hides the quick forward/share icon displayed beside chat message bubbles. |
| **Hide Reaction & Streak Bar** | `hideReactionTray` | Boolean | `true` | Hides the floating emoji reaction tray and streak mascot bar displayed above the chat text input. |
| **Hide Input Camera Button** | `hideInputCamera` | Boolean | `true` | Hides the camera shortcut icon to the left of the chat message input field. |
| **Hide Gallery Button** | `hideGalleryButton` | Boolean | `true` | Hides the photo album / gallery button on the right side of the chat message input field. |
| **Hide Sticker & Emoji Button** | `hideEmojiButton` | Boolean | `true` | Hides the sticker and emoji selector button on the right side of the chat message input field. |
| **Hide Voice Record Button** | `hideVoiceRecordButton` | Boolean | `true` | Hides the microphone / voice recording button on the right side of the chat message input field. |
| **Hide Try Effect Button** | `hideTryEffectButton` | Boolean | `true` | Removes the 'Try effect' camera button shown on shared videos that use an effect in direct messages. |
| **Hide Sticker Reply Suggestions** | `hideStickerReplySuggestions` | Boolean | `true` | Removes the automatic 'Tap a sticker to reply' suggestion panel and the typing-triggered sticker/GIF strip above the input bar. The manual sticker reply button keeps working. |

### 11. Popups & Prompts Suppressor

The **`Popups & Prompts Suppressor`** patch suppresses intrusive dialogs, bottom sheets, full-screen takeover prompts, and inline nudge banners across TikTok. Governed by 5 independent boolean toggle switches, it eliminates recurring account/contact sync nags, personalized sticker popups, annoying PopLayer promotion sheets, floating live stream teaser bubbles, and DM streak expiration warnings.

| Toggle Option | Key | Type | Default | Description |
| :--- | :--- | :---: | :---: | :--- |
| **Suppress Account & Permission Nags** | `suppressAccountPrompts` | Boolean | `true` | Suppresses 'Follow your friends' modals, 'Find contacts' Friends tab sync overlays, multi-account notification guides, and 'Security checkup 2SV' upsells. |
| **Suppress Sticker Recommendations** | `suppressStickerRecommendations` | Boolean | `true` | Disables personalized sticker suggestion popups and typing recommendations in direct messages. |
| **Filter PopLayer Prompts & Nags** | `filterPopLayerPrompts` | Boolean | `true` | Suppresses repetitive PopLayer prompts including favorites collection guides, launcher shortcut dialogs, repost newbie sheets, STEM feed prompts, campus education sheets, creator inbox guides, app review dialogs, marketing opt-ins, FYP surveys, CapCut/Lemon8 upsells, profile visitor prompts, profile view history sheets, message push guides, and story intro sheets. |
| **Suppress Live Teaser Bubbles** | `suppressLiveTeaserBubble` | Boolean | `true` | Disables floating live stream preview teasers and popup windows from appearing over the video feed. |
| **Suppress DM Streak Reminders** | `suppressStreakReminders` | Boolean | `true` | Suppresses direct message streak expiration warning banners and inline urgency reminders. |

### 12. TikTok Privacy Permissions Stripper

The **`TikTok Privacy Permissions Stripper`** patch selectively strips sensitive privacy, sensor, hardware, and tracking permissions from `AndroidManifest.xml` via 15 granular boolean toggles. Five toggles default to `true` (`nfc`, `screenshotDetection`, `oemSignals`, `bluetooth`, `biometric`), while the remaining 10 toggles default to `false`.

> [!WARNING]
> Stripping permissions at the manifest level completely revokes the capability for the application at the OS package level. While notification, NFC, and OEM signal removals are safe, stripping hardware or media permissions will disable respective features (camera, microphone, gallery, background sync) or cause crashes if components lack graceful permission guards.

| Toggle Option | Key | Type | Default | Risk Level | Description |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Strip Notification Permission** | `stripNotifications` | Boolean | `false` | **Safe** | Remove `POST_NOTIFICATIONS` permission (Android 13+). Notification channels cannot dispatch push alerts. |
| **Strip Camera Permission** | `stripCamera` | Boolean | `false` | **Risk** | Remove `CAMERA`. WARNING: Breaks camera recording, photo capturing, QR scanning, LIVE broadcasting, and video creation. |
| **Strip Microphone Permissions** | `stripMicrophone` | Boolean | `false` | **Risk** | Remove `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE`, and `FOREGROUND_SERVICE_CAMERA`. WARNING: Breaks video voice recording, LIVE audio broadcasting, audio comments, and voice/video calling. |
| **Strip Storage & Media Permissions** | `stripStorageMedia` | Boolean | `false` | **Risk** | Remove external storage (`READ`/`WRITE_EXTERNAL_STORAGE`) and media permissions (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `ACCESS_MEDIA_LOCATION`). WARNING: Breaks local gallery picker, drafts, and video/photo saving. |
| **Strip Bluetooth Permissions** | `stripBluetooth` | Boolean | `true` | **Risk** | Remove `BLUETOOTH`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, and `BLUETOOTH_ADVERTISE`. WARNING: Breaks Bluetooth audio accessories, wireless headphones low-latency sync, Cast devices, and external remote controls. |
| **Strip NFC Permission** | `stripNfc` | Boolean | `true` | **Safe\*** | Remove `NFC`. WARNING: Disables NFC tag interactions and NFC-based hardware authentication tokens. Safe unless using NFC hardware keys/login. |
| **Strip Biometric Permissions** | `stripBiometric` | Boolean | `true` | **Risk** | Remove `USE_BIOMETRIC` and `USE_FINGERPRINT`. WARNING: Breaks fingerprint/face biometric unlocking, passkeys, and biometric payment authorization. |
| **Strip Foreground Service Permissions** | `stripForegroundServices` | Boolean | `false` | **High Risk** | Remove generic `FOREGROUND_SERVICE` and specialized types (`DATA_SYNC`, `MEDIA_PLAYBACK`, `MEDIA_PROJECTION`, `PHONE_CALL`). WARNING: HIGH RISK. Breaks background video uploads, offline caching, media playback notification services, screen sharing, and background VoIP calls. |
| **Strip System Alert Window Permission** | `stripSystemAlertWindow` | Boolean | `false` | **Risk** | Remove `SYSTEM_ALERT_WINDOW`. WARNING: Breaks Picture-in-Picture overlay window outside the app, floating mini-player, and overlay notification heads. |
| **Strip Wake Lock Permission** | `stripWakeLock` | Boolean | `false` | **Risk** | Remove `WAKE_LOCK`. WARNING: Device CPU may sleep during media playback or long video uploads/downloads when screen turns off, suspending progress. |
| **Strip Screenshot Detection Permissions** | `stripScreenshotDetection` | Boolean | `true` | **Safe** | Remove `DETECT_SCREEN_CAPTURE` and `DETECT_SCREEN_RECORDING` to neutralize OS-level capture detection callbacks. Complements Universal Screenshot Protection Bypass. |
| **Strip Miscellaneous Hardware Permissions** | `stripMiscHardware` | Boolean | `false` | **Low/Med** | Remove `VIBRATE`, `MODIFY_AUDIO_SETTINGS`, `MANAGE_OWN_CALLS`, `REORDER_TASKS`, `SET_WALLPAPER`, and `USE_FULL_SCREEN_INTENT`. WARNING: Disables haptic feedback vibration, volume adjustments, alarm priority intents, and live wallpaper export. |
| **Strip OEM Signals & Telemetry** | `stripOemSignals` | Boolean | `true` | **Safe\*** | Remove vendor diagnostic/attribution tokens (Huawei, Oppo, Orange, Samsung MapsAgent), launcher badge providers, AICore service binding, and internal TikTok IPC permissions. WARNING: Removing `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` may impact dynamic broadcast receivers registered at runtime. |
| **Strip Push Delivery Permissions** | `stripPushDelivery` | Boolean | `false` | **Risk** | Remove Google C2DM/FCM (`com.google.android.c2dm.permission.RECEIVE`) and Amazon ADM (`com.amazon.device.messaging.permission.RECEIVE`). WARNING: Breaks background push notification reception. |
| **Strip In-App Billing Permission** | `stripBilling` | Boolean | `false` | **Risk** | Remove Google Play In-App Billing (`com.android.vending.BILLING`). WARNING: Breaks coin purchases and in-app monetization transactions. |

> [!NOTE]
> `stripScreenshotDetection` provides OS-layer defense-in-depth alongside Universal Screenshot Protection Bypass: while the runtime bypass clears `FLAG_SECURE` and stubs `registerScreenCaptureCallback`, it does not revoke manifest declarations. Stripping `DETECT_*` ensures the OS never dispatches capture callbacks regardless of runtime state. Both can coexist.

#### Non-Negotiables Excluded (Managed by Dedicated Patches)

Certain permissions are intentionally excluded from this manifest stripper because specialized patches handle them at runtime via bytecode hooks without breaking Android manifest contracts:

| Excluded Permissions | Authoritative Handler | Handling Mechanism & Rationale |
| :--- | :--- | :--- |
| `com.google.android.gms.permission.AD_ID`<br>`android.permission.ACCESS_ADSERVICES_AD_ID`<br>`android.permission.ACCESS_ADSERVICES_ATTRIBUTION`<br>`com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE` | **Universal Telemetry Neutralizer** + **Device Privacy Guard** | Bytecode hook zeroes GAID (`00000000-0000-0000-0000-000000000000`), drops AdServices attribution tokens, and neutralizes Play Store install referrer receivers in runtime memory. |
| `android.permission.ACCESS_FINE_LOCATION`<br>`android.permission.ACCESS_COARSE_LOCATION` | **Device Privacy Guard** | Intercepts `LocationManager` and `PowerPermissions` headless dispatcher at the Dalvik layer, returning `PERMISSION_DENIED` and clearing location caches while avoiding manifest-level XML parse shifts. |
| `android.permission.READ_CONTACTS` | **Device Privacy Guard** | Intercepts `ContentResolver.query` and BPEA contacts reader trampolines (`LX/0OFU`, `LX/0OFw`), returning empty cursors and neutralizing background sync Lego tasks without breaking caller state. |
| `android.permission.INTERNET` | **Universal Offline Mode** *(Optional)* | **Total Exclusion**: Revoking `INTERNET` at the manifest level causes Linux kernel socket allocation denials (`EPERM` / `socket failed: EACCES`), crashing the process at frame 0. Users requiring total offline isolation should use [Universal Offline Mode](../universal-patches.md#5-universal-offline-mode-universalofflinepatch). |

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

### 2. Comment Customizer (`commentCustomizerPatch`)
- Consolidates comment section usability enhancements and decluttering options via compile/patch-time toggles:
  - **`commentSortControls` (default: true)**: Unlocks TikTok's internal native comment sorting controls sheet (Most Relevant, Newest, Creator Only, Media Only) by overriding `comment_sort_opt_style` configuration getter (`2`) and Aweme comment sort eligibility checks.
  - **`copyWithoutUsername` (default: true)**: Copies clean comment text without prepending the author username (`@username: `). Hooks `ClipData` builder `LIZ(String, String, List)` and BPEA clipboard helper callers.
  - **`disableSuggestedEmojis` (default: true)**: Removes the horizontal bar of suggested quick emojis above the comment input box across active keyboard and passive comment views (`ExposedEmojiPanelTrigger`, `CommentPanelFakeInput`, `CommentKeyboardModel`, `PersonalizedEmojiExperiment`).
  - **`hideCommentQuickActions` (default: true)**: Hides the quick action buttons (photo/gallery, emoji/sticker, and mention `@`) inside the comment input bar (`BaseInputAssem`, `CommentKeyboardModel`, and `TikTokCommentHook.hideCommentQuickActions`).
  - **`hideCommentSurveys` (default: true)**: Hides surveys, opinion questionnaires, and feedback cards embedded within comment lists. Neutralizes survey data item providers (`CommentSurveyDataItem`), display eligibility checks (`shouldShow`), pre-layout Lynx gates, and stubs `CommentLynxCell` view creation and binding with a GONE view.
  - **`hideStoryRings` (default: true)**: Removes profile photo story rings from avatars in the comment section. Stubs `AvatarRing.setMode` with `return-void`, rewrites `AvatarRing.draw` to pass through to `FrameLayout.draw` without rendering ring arcs, and neutralizes `AvatarRing.onInterceptTouchEvent` -> `false`.
  - **`enableVoiceComments` (default: true)**: Forces native voice comment recording buttons in comment input bars, bypassing regional rollout restrictions and remote server blocks (`audio_comment_publish`, `comment_audio_publish_entry_forbidden`, `VEAudioRecorder`).
  - **`autoTranslate` (default: false)**: Automatically dispatches batch translations for incoming comments via TikTok's native engine (`BaseCommentCell`, `CommentList.onLoaded`).
  - **`commentSendFix` (default: true)**: Fixes silently dropped comments by substituting the null top-page screen with the owning panel screen (`CommentPublishViewModel.kJ1`, event `click_comment_send`).
  - **`hideCommentPopupAds` (default: true)**: Blocks brand surprise animations over comments via `CommentSurpriseStruct` with path tags (`page/publish/milestone`, self-celebrations are preserved).
  - **`showFlaggedComments` (default: true)**: Automatically expands community-flagged comments inline without requiring manual taps or displaying the collapsed warning banner. Hooks `FoldFooterCell.onViewAttachedToWindow` to trigger debounced auto-expansion on the cell entrance (`TikTokCommentHook.autoExpandFoldEntrance`), dispatching native `QU` expansion without re-implementing viewmodel logic.

### 3. Device Privacy Guard (`devicePrivacyGuardPatch`)
> [!NOTE]
> **Bytecode-Only Privacy Architecture (`ResourceMode.RAW`)**:
> Unlike apps with standard resource structures, TikTok's entire patch suite strictly avoids resource decoding (`resourcePatch`). Re-encoding TikTok's obfuscated resource tree via `arsclib` drops launcher icon drawables (`res/a/aq2.xml`, `res/a/aq3.xml`). Privacy is enforced at the Dalvik bytecode execution layer via runtime permission interception, in-app nag suppression, sensor silencing, and telemetry neutralization while keeping APK resources intact.

- **Runtime Permission Interception & Denial Handling**:
  - Intercepts ByteDance Helios static dispatcher (`LX/02z2;->LLJ`) for `Activity.requestPermissions`.
  - Intercepts PowerPermissions headless engine (`FakeFragment;->cY` / `jT`) to immediately dispatch denials (`PackageManager.PERMISSION_DENIED`) for invasive permissions (`READ_CONTACTS`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `ACCESS_LOCAL_NETWORK`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `ACTIVITY_RECOGNITION`, `AD_ID`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`).
  - Resets permanent denial flags in Keva stores (`FriendsSharePreferences -> read_contact_denied = false`, `permission_store -> <perm> = false`) to prevent persistent denial profiling.
  - Hooks permission cache check (`LX/04CN;->LIZ`) returning `false` for blocked permissions to suppress permanently-denied flags.
- **In-App Permission Dialog, Settings Redirect & Location Popup Suppression**:
  - Neutralizes permanently denied system settings redirect dialogs (`LX/06WV;->LJI(Activity, String, boolean)Z` -> returns `false` for blocked permissions), eliminating persistent in-app prompts nudging users to open device Application Details Settings.
  - Suppresses relation onboarding and permission dialog triggers (`LX/16rQ.LIZJ` -> `false` for contacts, `LX/16rP.LIZJ` -> `false` for Facebook, and `LX/16rO.LIZIZ` -> `false` for permission popups).
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
- **Clipboard Protection**: Blocks programmatic and BPEA background clipboard reading methods (`LX/01ZZ.LIZ`, `LX/0jUy.LIZIZ`).
- **Framework Call-Site Filtering (insert-only)**:
  - `ContentResolver.query` (3 overloads) filtered by URI via `noteUri`/`filterNotedResult` (contacts -> empty cursor).
  - `queryIntentActivities` filtered via `noteIntent`/`filterNotedIntentResult` (explicit intents pass through, broad queries -> empty list).
  - `getLastKnownLocation` -> `null`.
  - `requestSingleUpdate` canceled via `removeUpdates`.
  - `isProviderEnabled` / `isLocationEnabled` -> `false`.

### 4. Bypass Screen Capture Detection (`bypassScreenCapturePatch`)
- Clears `FLAG_SECURE` (`0x2000`) on window layouts via `AntiScreenRecordController.applyFlag(enabled=false)`, `makeScreenProtection(enable=false)`, and neutralizes global Activity `FLAG_SECURE` enforcement (`setFlags(8192, 8192)`), enabling screenshots and screen recordings across restricted views, live courses, and ephemeral chats.
- Neutralizes dynamic Circle to Search and recent apps blocking: stubs `MainContentSecurityAssem` (`dispatchTouchEvent`, `rq`), neutralizes `ContentSecurityHelper` bottom navigation touch listeners (`LIZ(MotionEvent) -> return-void`, `LIZJ() -> false`), and neutralizes the `circle_search_block` experiment to prevent bottom touch interception from injecting `FLAG_SECURE` at navigation gestures (restoring Circle to Search, screen translate, music search, and recent apps preview snapshots).
- Neutralizes screenshot detection listeners, floating feedback prompts, and telemetry (`ScreenShotFeedbackService` and startup Lego tasks).

### 5. Feed Bloat & Distraction Blocker (`feedBloatBlockerPatch`)
- **Rewards Pendants Suppression**: Injects `return-void` into `SpecActWidget.bind(ViewGroup)` to prevent floating Rewards widgets (countdown coins, soccer ball stickers) from attaching to feed views.
- **Sticker & Card Stripping**: Nullifies `Aweme.getActivityPendant()`, `getCommerceStickerInfo()`, `getFloatingCardInfo()`, and `getBannerTip()`.
- **Feed Stream Purge**: Filters suggested accounts, mini-games, CapCut creation prompts, memories recaps, mini-dramas, in-feed search interest cards (card type `38`), and trending search recommendation cards (card types `34`, `35`) before UI adapter binding via `TikTokFeedAdFilter.isFeedBloat()`.
- **Community & Lynx Cards Suppression**: Neutralizes all inserted non-video Lynx cards and topic cards via `TikTokFeedAdFilter.isFeedBloat()` (`CardInsertInfo != null`, `recommendCardType > 0`), stubs `Aweme.getExploreCommunityCommentShowType()` -> `null`, and sanitizes community comment headers via `stripSurveyBloat()`.
- **Post-Video Safety Surveys & Questionnaires Neutralization**: Suppresses in-feed safety evaluation surveys ("¿Valió la pena ver este video?", feedback questionnaires, push surveys, and bottom survey buttons). Injects `return-void` into `CellSurveyComponent` (`onViewCreated`, `ur`, `wr`) and `FeedBottomSurveyButtonComponent.onParentViewCreated()`, forces `AwemeExtKt.isWithSurvey()` -> `false`, `PNSSurveyService.LIZIZ()` -> `false`, `PushSurveyAssemTrigger.yr()` -> `false`, nullifies `Aweme` survey getters (`getWithSurvey() -> false`, `getSurveyInfo() -> null`, `getSurveyInfos() -> null`, `getPersonalizedSurveyUI() -> null`, `getOnboardingSurvey() -> null`, `getPersonalizedOnboardingSurvey() -> null`), prunes standalone survey cards without media content (`hasMediaContent == false`), and strips residual survey bloat metadata on retained videos and photo mode posts (`TikTokFeedAdFilter.stripSurveyBloat()`).
- **Search Recommendation Request Neutralization**: Stubs `AbsSearchService.u()Ljava/util/List;` to return an empty list (`Collections.emptyList()`), preventing the search module from registering card insert request generators into the feed pipeline.
- **Friends Feed Rec User Cards & Bloat Neutralization**: Injects `p1 = false, p2 = false` into `FriendsV3RecUserConfig.<init>(ZZ)V` to prevent `FriendsV3FeedListViewModel` from instantiating suggested friend cards (`FriendsV3RecUserItem`) or bottom recommendation lists (`FriendsV3BottomRecListItem`). Intercepts `FriendsV3FeedResponse.<init>` to filter in-feed bloat and nullify suggested friends (`newlyShownMafIds = null`), and hooks `FriendsFeedResponse.<init>` (V2) to prune inserted card results (`cardInsertResults = null`, `insertedResults = null`). Because network responses are deserialized through the no-arg constructors, the same filtering and nulling also runs at the network boundary via `FriendsV3FeedNetworkSource.LJ` (V3) and the return points of the obfuscated friend feed request `LX/06CX;->LIZLLL` (V2); the constructor hooks remain for cached and copied responses. Injects immediate `View.GONE` and dimensions contraction (`0x0`) into `FriendsV3HorizontalRecUserCardCell.onItemViewCreated` and `FriendsV3BottomRecUserListCell.onItemViewCreated` as a fallback UI defense.

### 6. Navigation & Header Declutter (`feedNavigationDeclutterPatch`)
- Consolidates clutter removal across the feed navigation strip, top toolbar, and video bottom bars via compile/patch-time toggles:
  - **`hideNearbyTab` (default: true)**: Removes the Nearby (local city or region) feed tab from the top navigation strip. Hooks `NearbyServiceImpl.LJIIZILJ()` tab provider `LJ()` -> `null`, `NearbyTabProtocol.enable()` -> `false`, and service boolean gates.
  - **`hideCommunityTab` (default: true)**: Removes the Community (Explore) feed tab from the top navigation strip and bottom navigation bar. Hooks `ExploreFeedServiceImpl.LIZ()` -> `false`, tab provider `LJ()` / `LIZ()` -> `null`, and `ExploreBottomTabProtocol.enable()` -> `false`.
  - **`hideTopLiveEntrance` (default: false)**: Removes the top-left LIVE broadcast button and tab entry point from the top navigation bar. Hooks `LiveIconGenerator.enabled()` -> `false`, view method -> `null`, and `LiveTabProtocol.enable()` -> `false`.
  - **`hideFeedSearchBar` (default: true)**: Removes the in-video bottom search suggestion and trending query bar. Hooks `FeedSearchBottomBarAssem` and `FeedSearchItemViewModel` trigger/binding methods to suppress bottom query pills.
  - **`hidePublishTab` (default: false)**: Removes the central `+` create / publish content button from the bottom navigation bar. Hooks `PublishTabProtocol.enable()` -> `false` and forces `View.GONE` (`0x8`) in `PublishBottomTabViewFactory.LIZ`, seamlessly redistributing the remaining 4 navigation tabs (Home, Friends, Inbox, Profile).
  - **`hideFriendsAvatarPreview` (default: true)**: Prevents friend profile pictures from replacing the Friends icon on the bottom navigation bar. Suppresses avatar fetching and ability attachment via `ISocial2TabRedDotService` (`enableTabAvatar` -> `false`, `loadAvatarAbility` -> `return-void`, `dealWithFriendsAvatar` -> `return-void`), neutralizes `BaseBottomTabAvatarAbility` and `FriendBottomTabAvatarAbility` (`isShowing` -> `false`, `cr2` -> `false`), stubs bottom tab manager view attachment and binding (`LJI`, `LJII`, `LJJJI` -> `return-void`, `LJJIIZ` -> `false`), and neutralizes the new-user exemption/reminder setting (`tt_friends_tab_avatar_exemption_new_user_days` -> `false`), ensuring the standard Friends icon remains consistently visible.
  - **`hideInboxBadge` (default: true)**: Removes the unread message counter badge and notification red dot from the bottom Messages (Inbox) tab, while preserving the unread states inside the Inbox list. Injects early-return and zero-count guards exclusively for the `NOTIFICATION` tab branch within the bottom Tab Manager (`LJIIL`, `LJJL`, `LJIIJJI`, `LJJIIJ`), leaving the shared `NoticeCountTabBadgePresentServiceImpl` and `C0BTZ` interior data pipeline fully intact.

### 7. Feed Interface Declutter (`feedInterfaceDeclutterPatch`)
- Consolidates clutter removal across feed video cell overlay elements via compile/patch-time toggles:
  - **`hideRepostBadge` (default: true)**: Hides the repost and shared-by pill badge ('Shared by' / 'Reposted') above creator details on feed videos. Hooks `UpvoteVideoTrigger.yr(VideoItemParams)Z` -> returns `false`, preventing `UpvoteVideoAssemNew` from being attached or activated. Stubs `UpvoteVideoAssemNew` methods (`tr`/`hb` -> `false`, `LLLLIILL`/`LLILZ`/`z4` -> `return-void`, `onViewCreated` -> `View.GONE`).
  - **`hideVideoDescriptions` (default: true)**: Hides video descriptions, captions, hashtags, "more" expansion buttons, and the "See translation" interactive button across feed videos while keeping creator and author info intact. Enforces `View.GONE` (`0x8`) at return points in `VideoDescAssem.onViewCreated`/`z4`/`js` (preserving state initialization to prevent lifecycle NPEs) and redirects every desc visibility call inside the feed-only delegate `LX/09Ce` to extension wrappers that force GONE (same invoke shape, no control-flow disturbance), neutralizes `FriendsV3DescAssem`, and neutralizes `TranslationControlsAssem` and `TranslationStatusAssem` to eliminate leftover translation action buttons.
  - **`hideAvatarFollowButton` (default: true)**: Hides the red plus (`+`) follow badge on creator profile avatars in the feed and eliminates accidental follow touches. Hooks `FeedAvatarDefaultAssem.Qr(ViewGroup, int, Object)` -> permanently sets visibility to `View.GONE` (`0x8`) and disables clickability via `TikTokMediaHook.hideFollowButton`. Injects immediate `View.GONE` into `FeedAvatarDefaultAssem.onViewCreated` right after `LLLIILIL` (`follow_view_container`) is assigned.
  - **`disableStoryRings` (default: true)**: Removes creator profile photo story rings from feed videos, ensuring avatar photos remain clean without blue story rings. Hooks `User.getStoryStatus()I` -> `0`, stubs `FeedAvatarSocialPublishAssem` lifecycle methods (`onViewCreated`, `onBind`, `tr`) with `return-void` to remove story indicators and click interceptors, and hooks `SocPubDistributeServiceImpl` -> `false`.
  - **`hidePlaylistBar` (default: true)**: Hides the playlist indicator bar displayed above bottom navigation when a video is part of a playlist. Hooks `PlayListBottomBarAssemTrigger.yr(...)Z` -> `false`, forces `InteractPlayListBottomBarAssem.onViewCreated` -> `View.GONE` (`0x8`), stubs `z4`, and hooks `Aweme.getPlaylist_info()` -> `null`.
  - **`hideSaveButton` (default: false)**: Hides the bookmark/favorite save button on the right-side action rail of feed videos across standard and landscape modes (`VideoFavoriteAssem`, `LandscapeVideoFavoriteAssem`), enforcing `View.GONE` (`0x8`) in `onViewCreated` and stubbing `z4`.
  - **`hideMusicCover` (default: false)**: Hides the rotating vinyl music album cover disc at the bottom right corner of feed videos. Enforces `View.GONE` (`0x8`) in `VideoMusicCoverAssem.onViewCreated`, stubs `z4`, and neutralizes rotation animators (`Tr`, `Wr`).
  - **`hideFullscreenButton` (default: false)**: Hides the floating 'Full screen' button on horizontal/landscape feed videos. Hooks `LandscapeEntranceAssem.Kr()Z` -> `false`, `Aa()Z` -> `false`, `bf()Z` -> `false`, enforces `View.GONE` (`0x8`) in `LandscapeEntranceAssem.onViewCreated`, stubs `z4` to neutralize binding, and stubs `LLLLIILL`/`LLILZ` with `return-void` to prevent visibility synchronization and auto-rotation triggers.
  - **`hideFeedbackButtons` (default: true)**: Hides the 'Not interested' / 'Interested' feedback pills (`bottom_button_early_feedback`) above the bottom navigation on feed videos. Hooks `EarlyFeedbackButtonTrigger.yr(VideoItemParams)Z` -> returns `false`, enforces `View.GONE` (`0x8`) in `EarlyFeedbackButtonAssem` and `EarlyFeedbackStandardButtonAssem` `onViewCreated`, stubs `z4`, and stubs `EarlyFeedbackStandardButtonAssem.Mr` with `return-void`.

### 8. Disable Profile Photo LIVE Status (`disableAvatarLiveStatusPatch`)
- Removes the pulsing LIVE ring animation and LIVE badge from creator avatars in the feed and ensures avatar taps route strictly to the creator's user profile instead of launching the live stream broadcast.
- Dynamically locates and hooks the author live validator (`LX/09A7;->LIZIZ(Aweme, User)Z`) -> returns `false`.
- Hooks `FeedAvatarAssemWrap.Yr()Z` -> returns `false`, preventing the attachment and lifecycle execution of `FeedAvatarLiveAssem`.
- Hooks `FeedAvatarLiveAssem.ur()Z` -> returns `false`.
- Stubs `FeedAvatarLiveAssem.Ar(ZZ)V` and `FeedAvatarLiveAssem.onBind(Object)V` with `return-void` to eliminate live streaming UI bindings and animations.
- Preserves `FeedAvatarDefaultAssem`'s default avatar click handler (`LX/0BIx`), routing taps directly to `//user/profile`.

### 9. Force Auto-Scroll (`forceAutoScrollPatch`)
- Forces the activation of TikTok's native video auto-scroll experiment flag for accounts and regions where it is withheld by server-side A/B testing experiments.
- **Feed Auto-Scroll A/B Experiment Flag**: Hooks the core experiment evaluator referencing `"fyp_auto_scroll"` -> returns `true`.
- **FypAutoScrollServiceImpl Capability Bridge**: Forces `FypAutoScrollServiceImpl.LJIILJJIL()` -> returns `true`, granting the feed panel full auto-scroll capabilities.
- **Tablet and Search Auto-Scroll**: Forces tablet/foldable (`"tablet_fyp_auto_scroll"`) and search feed (`"search_auto_scroll"`) experiment flags to return `true`.
- **FeedBottomBarFacade Capability Hook**: Forces `FeedBottomBarFacadeImpl.LJIJJLI()` referencing `"panel_auto_scroll"` to return `true`.
- **Long-Press Menu Item Constructor**: Overrides `createAutoScrollItem` in `LX/0oWb;->LJII`, neutralizing the `"panel_auto_scroll"` guard and the `isLogin()` requirement so that the Auto-scroll toggle option is permanently available and visible in the video long-press / share menu.

### 10. Disable Search History Recording (`disableSearchHistoryRecordingPatch`)
- Prevents search queries, keywords, and search interactions from being written to persistent local history or device storage.
- **Search History Manager Hook**: Stubs `LX/0D7Z;->LIZ(SearchHistory, String)V` with `return-void` to neutralize search history record persistence.
- **Manual Search PV Tracking Hook**: Stubs `ManualSearchPvStore.LJIIJ(String, String)V` with `return-void` to prevent manual search pageview and query history accumulation.
- **Top History Recommendation Suppression**: Hooks `SuggestWordResponse.getTopHistoryWords()` -> returns `null` to neutralize server-pushed search history suggestions.

### 11. Hide Suggested Searches (`hideSuggestedSearchesPatch`)
- Removes the suggested search keywords section ('You may like' / 'Search suggestions') from the search discovery screen.
- **Search Intermediate Raw Payload & Model Filtering**: Intercepts `RecomDataWrapper.<init>(String, SuggestWordResponse)` to filter out `"recom_search"`, `"recom_search_pic"`, `"recom_search_under_bar"`, and `"guess_search"` card items from the raw JSON payload and parsed response model before Lynx rendering.
- **Cached Guess Search Preload Neutralization**: Hooks `LX/0HMZ;->LIZ()Lorg/json/JSONObject;` and `LX/0HMZ;->LIZIZ()Ljava/lang/String;` to return `null`, eliminating cached suggestion preloading on startup.
- **Native Guess Search Fallback Override**: Forces `DynamicSingleIntermediateFragmentNew.yU()Z` -> returns `false` to disable native guess search fallback rendering.
- **Lynx AB Parameters & Evaluator Suppression**: Overrides AB evaluator `LX/0HL2;->LIZ` -> returns `0` and stubs `LX/0HL2;<clinit>()` to disable `show_suggest_search_words`. Intercepts `SparkHostApiImpl.LJLJI` to sanitize Lynx `abParams` (`show_suggest_search_words = 0`, `sbp_not_login_disable_guess_search = 1`, `disable_suggest_guide = 1`), and sanitizes schema URLs in `LX/0HLB;->LIZ` to strip suggestion queries before template evaluation.

### 12. Hide Popular Lives In Search (`hideSearchPopularLivesPatch`)
- Removes the Popular LIVEs recommendation card and live stream broadcasts from the search discovery screen.
- **Search Intermediate Raw Payload & Model Filtering**: Intercepts `RecomDataWrapper.<init>(String, SuggestWordResponse)` to filter out `"trending_rank_live"` and `"live_popular"` card items from the raw JSON payload and parsed response model before Lynx rendering.
- **Lynx AB Parameters & Schema Sanitization**: Intercepts `SparkHostApiImpl.LJLJI` to sanitize Lynx `abParams` (`has_transfer_tab_live = 0`, clears `transfer_tab_live_url`, `intermediate_show_trending_billboard = 0`), and strips `intermediate_show_trending_billboard` from Lynx schema URLs in `LX/0HLB;->LIZ`.

### 13. Hide AI-Generated Content (`hideAiTaggedContentPatch`)
- Filters and skips videos tagged with native AI-generated metadata, C2PA content credentials, or creator AI disclosure tags across the For You, Following, and Friends feeds.
- **Feed API Response Interception**: Hooks `FeedApiService.fetchFeedList` to filter incoming items at the network response boundary before model mapping.
- **Feed Item Model Interception**: Hooks `FeedItemList.getItems()` and `FollowFeedList.getItems()` to sanitize feed collections in-situ.
- **Friends Feed Network Interception**: Hooks `FriendsV3FeedNetworkSource.LJ` (V3 response handler) and the obfuscated friend feed request `LX/06CX;->LIZLLL` (`/tiktok/v1/friend/friend_feed`, V2) return points to filter `friendsV3Feeds` / `friendFeedData` after deserialization.
- **Multi-Vector AI Metadata Inspection**: Inspects `Aweme` for:
  - `AIGCInfo` (`AIGCLabelType != 0`, `createByAI == true`).
  - `ModerationAigcInfo` (`moderationAigcLabelType != 0`, `moderationUserLabelStatus != 0`, `creatorGuidanceStatus != 0`, `moderationCreatorSegment` populated).
  - `C2PAInfo` (`aigcSrc`, `firstAigcSrc`, `lastAigcSrc` populated).
  - Specific AI sub-structures (`aiAliveInfo`, `aiPortraitInfo`, `aiRemixInfo`, `aiTheaterInfo`, `aiChatEditorInfo`).
  - Native AI banners and anchors (`ANCHOR_AIGC`, Lynx AI disclosure templates).
  - Video description and tag regex matching for creator-disclosed AI markers (`#aigenerated`, `#ai`, `#generadoporIA`, etc.).

### 14. Friends Feed Strict Mutuals (`friendsFeedStrictMutualsPatch`)
- Enforces strict mutual friendship verification across the TikTok Friends tab, eliminating non-mutual suggested accounts, recommended videos, and "People you may know" (`Personas que quizás conozcas`) cards.
- **Friends V3 & V2 Network Interception**: Hooks `FriendsV3FeedResponse.<init>` and `FriendsV3FeedNetworkSource.LJ` for Friends V3 payloads, as well as `FriendsFeedResponse.<init>` and `LX/06CX;->LIZLLL` (`/tiktok/v1/friend/friend_feed`) return points for Friends V2 payloads.
- **Mutual Follow & Suggested Tag Inspection**:
  - Validates `author.getFollowStatus() == 2` (mutual friends who follow each other). If the author is not a mutual friend (`followStatus != 2`), the video is pruned from playback.
  - Detects and prunes suggested accounts via `User.getMatchedFriendStruct()`, `User.isMatchedFriendAvailable()`, and `User.getRecType()`.
  - Prunes videos tagged with recommendation relation labels (`Aweme.getFeedRelationLabel()`, `getRelationLabel()`, `getRelationRecommendInfo()`, and `getRecReasonsStruct()`).
  - For reposts (`FriendsV3RepostModel`), inspects the `reposter` user profile: ensures `reposter.getFollowStatus() == 2` and filters out suggested reposter accounts.
  - Automatically preserves user's own uploads and self-reposts by verifying `author.getUid()` / `reposter.getUid()` against the logged-in user (`IUserService.getCurrentUserID()`).

### 15. Disable Search Video Autoplay (`disableSearchVideoAutoplayPatch`)
- Disables automatic video and media playback in TikTok search results, preserving bandwidth and preventing unwanted audio or distraction while browsing search cards.
- **Search List Autoplay Calculation Loop Suppression**: Injects `return-void` at index 0 of `SearchListAutoplayHelper.LIZIZ(Z LX/0JHH;)V` (fingerprinted by string `"checkLogic() is not called on main thread"`), halting the recurring scroll and idle candidate evaluation cycle.
- **Card AutoPlay Ability Inactivation**: Injects `const/4 v0, 0` / `return v0` into `SearchCardVideoPlayerAssem$autoPlayAbility$2$1.l2()Z`, `SearchVideoForLynx$ability$1.l2()Z`, and `SearchCardPhotoPlayerAssem$autoPlayAbility$2$1.l2()Z`, asserting `false` for card autoplay eligibility.
- **Playback Execution Guard**: Injects `return-void` into `r()V` on all search card `AutoPlayAbility` implementations, preventing any direct invocation from triggering video playback or hiding cover thumbnails. Detail view playback when opening a video remains fully functional via `PlayerController`.

### 16. Resume Video After Scroll (`resumeVideoAfterScrollPatch`)
- Persists and restores playback timestamp when scrolling away and returning to feed videos.
- **Configuration Gate Activation**: Hooks `FeedPlayProgressContinueConfig` gate (`invoke()`), forcing `enable = true`.
- **Feed Type Restriction Bypass**: Intercepts the event type check matching `landscape_change_keep_tag`, replacing the `MOVE_RESULT` register with `1` to allow timestamp restoration across standard vertical portrait feeds.

### 17. Stop Video Looping (`stopVideoLoopingPatch`)
- Prevents videos from repeating in an infinite loop upon playback completion.
- **Native Player Looping Suppression**: Injects `const/4 p1, 0x0` at instruction offset 0 of `Lcom/ss/ttvideoengine/TTVideoEngine;->setLooping(Z)V`, ensuring `isLooping` remains disabled for the underlying media session.

### 18. Hide Seen Videos (`hideSeenVideosPatch`)
- Automatically filters previously watched videos from incoming For You feed batches, preventing repeat content during the session while preserving active viewing history.
- **Playback Tracking**: Hooks `PlayerController.onPlayProgressChange(String, long, long)` (recording videos viewed for >= 5s or >= 70% duration) and `PlayerController.onPlayCompleted(String)`.
- **Network Ingestion Filtering**: Hooks `FeedApiService.fetchFeedList()` return points, pruning seen video entries directly from deserialized `FeedItemList` payloads before they are delivered to the UI layer, preventing adapter desynchronization and frame drops.

### 19. Disable Post-Download Share Dialog (`disablePostDownloadDialogPatch`)
- Suppresses the automatic 'Share to' and friend suggestions bottom sheet that pops up after finishing a video or media download.
- **Bottom Sheet Presentation Neutralization**: Stubs the popup display launcher in `DownloadAndShareFragment` (fingerprinted by `definingClass = DownloadAndShareFragment` and string `"after_video_saved_share_to_nscreen"`) with `return-void` at instruction offset 0, preventing the creation and display of the `TuxSheet` bottom sheet dialog while preserving download completion toasts and saved file integrity.

### 20. Hide Inbox Story & Status Tray (`hideInboxStoryTrayPatch`)
- Removes the horizontal story carousel, status notes, and creation bubbles (Skylight) displayed at the top of the direct messages inbox.
- **Skylight Widget Injector Suppression**: Hooks `InboxSkylightWidgetV2Injector.enable()Z` -> returns `false`, preventing the Skylight container from registering or injecting into the inbox multi-pod recycler.
- **Combine Pod Provider Neutralization**: Stubs `InboxSkylightWidgetV2.Sq()Ljava/util/List;` -> returns `emptyList()`, neutralizing story and thought combine pod creation.
- **Eligibility Gate Neutralization**: Hooks `InboxSkylightWidgetV2.er(List)Z` -> returns `false`, ensuring display eligibility checks evaluate to empty.

### 21. Disable Feed Long-Press Actions (`disableFeedLongPressActionsPatch`)
- Disables long-press action gestures on feed buttons, eliminating unwanted menu popups while preserving standard single-tap actions.
- **Configurable Options**:
  - `disableLikeRepost` (default: `true`): Prevents long-pressing the Like (heart) button from opening TikTok's Repost action panel. Hooks `VideoDiggAssem.Sr(View)Z` to consume the long-press gesture (`return true`) without triggering the repost panel or falling through to click. Single tap to like or unlike remains fully functional.
  - `disableShareQuickDms` (default: `true`): Prevents holding the Share button from launching the quick-share recent contacts tray. Overrides the `im_long_press_share_button_to_quick_share` configuration lambda to return `0` (`Integer.valueOf(0)`) and neutralizes the `ShareUnreadVideoQuickDMTrigger` eligibility check -> returns `false`. Single tap to open the full share sheet remains fully functional.
  - `disableCommentReactions` (default: `true`): Prevents long-pressing the Comment button from opening the quick emoji reaction picker. Overrides the `long_press_quick_comment` configuration lambda to return `0` (`Integer.valueOf(0)`), causing `VideoCommentAssem.Kr()` and `Lr()` to attach only the native single-tap `OnClickListener` without long-press touch listeners. Single tap to open comments remains fully functional.

### 22. Enable Profile Banner (`profileBannerPatch`)
- Unlocks the custom profile banner (background header cover) feature on user profiles and enables the banner selection, cropping, and editing tools in Edit Profile.
- **ProfileBackgroundExp Gate Activation**: Hooks the main feature evaluation gate in `ProfileBackgroundExp` (`(Z)Z`) -> returns `true`, allowing `MusProfileEditFragment` to attach `ProfileBgEditHelper` (`LX/0axG`) and `ProfileRootBaseComponent` to assemble the `ProfileBackgroundComponent`.

### 23. Direct Message Declutter (`directMessageDeclutterPatch`)
- Declutters direct message chat rooms and the inbox conversation list via modular boolean toggles:
  - **Chat List Camera Icon (`hideChatListCamera`)**: Hooks the serialized view configuration model `LX/0CbL;` in its `<init>` constructors and `setShowCameraIcon(Z)V` to enforce `showCameraIcon = false`, removing the camera shortcut on conversation list rows.
  - **Header Call Button (`hideCallButton`)**: In `BaseSingleChatTitleBarRightAssem.onViewCreated(View)V`, sets the call icon view (`0x7f0a39ae` / `icon_call_container`) to `View.GONE` and sets field `LLLFF` to `null` to neutralize async observer callbacks without crashing.
  - **Message Forward Button (`hideMessageForwardButton`)**: In `SideMessageStatusReusedSkeletonUISlot;->vs(LX/0BVS;)LX/0pxs;`, substitutes return value `LX/0pxs;->FORWARD` with `LX/0pxs;->NOTHING`, suppressing the forward arrow button beside individual chat messages while keeping the reply button functional.
  - **Reaction & Streak Mascot Bar (`hideReactionTray`)**: Hooks `ActionBarServiceImpl;->LJI()Z` to return `false` (neutralizing feature eligibility) and injects `View.GONE` into `ActionBarUIAssem.onViewCreated(View)V` immediately prior to `return-void`, hiding the emoji reaction bar and streak mascot.
  - **Input Camera Shortcut (`hideInputCamera`)**: Hooks `DMCameraFeatureImpl;->LJII(...)` to return `null` and suppresses container `0x7f0a479c` (`input_camera_container`) with `View.GONE` in `IMInputAssem.onViewCreated(View)V`.
  - **Right Input Action Buttons (`hideGalleryButton`, `hideEmojiButton`, `hideVoiceRecordButton`)**:
    - **Photo Album / Gallery**: Sets `0x7f0a3d4f` (`im_photo_btn`) to `View.GONE` and returns early in `IMImageBtnViewAssem.Wr(View)V`.
    - **Stickers & Emojis**: Sets `0x7f0a3ce0` (`im_emoji_btn`) to `View.GONE` and returns early in `InputEmojiButtonUIAssem.onViewCreated(View)V`. For the redesigned input bar layout (`ui_slot_input_layout_redesign`), forces the `InputIconBtn` `EMOJI_BTN` slot view (`0x7f0a3aba` / `kwq`) to 0x0 dimensions (`width = 0`, `height = 0`), strips margins (`setMarginEnd(0)`), disables click and touch interactions (`setClickable(false)`, `setEnabled(false)`), scaled to 0 (`setScaleX(0f)`, `setScaleY(0f)`), and sets `View.GONE` directly in its setup lambda built by `LX/0YEc;->LIZ`.
    - **Voice Record**: Sets `0x7f0a4980` (`record_btn`) to `View.GONE` and returns early in `RecordBtnAssem.onViewCreated(View)V`.
    - *Preservation Invariant*: The parent actions container `0x7f0a3dd3` (`im_input_right_container`) is intentionally left intact so that `SendButtonAssemV2` appears seamlessly when typing text.
  - **Try Effect CTA Button (`hideTryEffectButton`)**: Hooks both static eligibility gate methods in obfuscated gate class `LX/0qRQ;` (`LIZIZ(...)Z` called by `AwemeCardAssem` and `LIZJ(...)Z` called by CTA list builders `LX/0qPi`/`LX/0qPj` before instantiating `TryEffectCTAButtonType` / `LX/0YK6;`) to return `false`, neutralizing the 'Try effect' camera shortcut button on shared videos in direct messages.
  - **Sticker Reply Suggestions (`hideStickerReplySuggestions`)**: Neutralizes sticker suggestion popups and reply banners in direct messages:
    - **Incoming Reply Suggestions**: In `ReplyToStickerRecommendationViewModel`, hooks the static synthetic default-args dispatcher method (`y83(...)V`) by injecting a bitmask check on register `p3` at instruction offset 0 (`and-int/lit8 v0, p3, 0x2`). When called by automatic triggers without a message argument (mask contains bit `0x2`), it returns early (`return-void`), suppressing the automatic 'Tap a sticker to reply' suggestion panel above the text input bar while preserving manual sticker replies initiated via the sticker reply button (mask `0x1`).
    - **Preshown Reply Banner**: Hooks `PreshownStickerBannerProtocol.isEnabled()Z` -> `false` and converts `PreshownStickerBannerProtocol.intercept(List)List` to passthrough `return-object p1`, preventing the bottom conversation banner from instantiating.
    - **Typing Recommendations**: Hooks `TypingRecommendationPanelAssem.Kq()Z` -> `false` and stubs the typing strip pipeline `Aq(LX/0XIS;)V`, `Rq(LX/0pPl;)V`, `Sq(List;)V`, `rq(LX/0pPl;)V` and `uq(LX/0pPl;)V` with `return-void`, suppressing the typing-triggered sticker/GIF strip above the input bar.

### 24. Popups & Prompts Suppressor (`popupsAndPromptsSuppressorPatch`)
- Suppresses intrusive modal popups, bottom sheets, overlay takeovers, and nudge reminders via modular boolean toggles:
  - **Account & Permission Nags (`suppressAccountPrompts`)**:
    - Stubs `RecUserPopupInMainActivityController.LIZLLL()V` with `return-void` to prevent the "Follow your friends" recommendation dialog on startup and navigation.
    - Stubs `LX/0YL4;->LJII(...)V` with `return-void` to suppress "Get notifications from other accounts" prompts when switching accounts.
    - Suppresses the "Find contacts" sync overlay on the Friends tab by stubbing `LX/0v6A;->canShow()Z` -> `false`, `LX/0v6A;->LJII(...)V` -> `return-void`, and `RelationAuthDialogControl;->LJFF(...)V` -> `return-void`. Note: The secondary Find Contacts sync overlay remains pending runtime flow analysis.
    - Suppresses 2-Step Verification security checkup popups and message push guides by intercepting `LocalCampaignManager.showLocalCampaign` -> `false` and `PopSuiteManagerService.shouldShowPopSuitePopup` -> `false` (generalized to block `UPSELL_2SV_POPUP`, `MESSAGE_REQUEST_PUSH_GUIDE_POPUP`, and `GPPPA` 2SV fullsheet/profile variants).
  - **Sticker Recommendations (`suppressStickerRecommendations`)**:
    - Disables sticker typing recommendations in direct messages by intercepting `ChatFeatureListConf.featureEnable` -> `false` when queried for `TYPING_RECOMMEND`.
  - **PopLayer Prompts & Nags (`filterPopLayerPrompts`)**:
    - Hooks `LX/07Q5;->canShow()Z` (`PopLayerBaseFragment.canShow`) to check against companion extension hook `TikTokPopupHook.shouldSuppressPopLayer()`.
    - Blocks targeted PopLayer labels and triggers: favorites collections guide, add shortcut nag, repost newbie guide, STEM feed prompt, campus education sheet, creator inbox guide, in-app review prompt, marketing/email opt-ins, FYP survey dialogs, CapCut upsell sheets, Lemon8 promo modals, profile visitor prompts, message push guides, and story introduction sheets.
    - Stubs `LX/0P2r;->LIZ()V` with `return-void` to strictly block the "Profile view history turn-on" PopLayer sheet before it reaches the trigger evaluation.
  - **Live Stream Teaser Bubble (`suppressLiveTeaserBubble`)**:
    - Suppresses floating live stream preview teaser bubbles over feed videos by stubbing `LiveBubbleUtil.LIZ` -> `return-void` and forcing `LiveBubbleUtil.LJIIIIZZ` -> `false`.
  - **DM Streak Reminders (`suppressStreakReminders`)**:
    - Neutralizes streak expiration urgency alerts and inline reminder messages in direct messages by forcing `LX/0O2v;->LIZ` (`has_streak_reminder_inline_msg`) -> `false`.

### 25. Update Prompt Suppressor (`disableInAppUpdateNagsPatch`)
- Neutralizes background update polling tasks and device ID check routines to prevent forced update popups.
- Stubs `run()V` on `CheckUpdateChangeDeviceIDTaskHolder$Background`, `CheckUpdateChangeDeviceIDTaskHolder$BootFinish`, and cold startup task `CheckUpdateChangeDeviceIDTask` with `return-void`.

### 26. Disable HDR Video Playback (`disableHdrVideoPatch`)
- Forces the video playback engine to select standard SDR bitrates (BT.709/sRGB) instead of HDR (HDR10/PQ/HLG), preventing blinding screen brightness spikes and display thermal throttling while preserving smooth playback.
- Stubs `isForceHdrOff()Z` -> `true` across all `ISimPlayerConfig` and `PlayerConfigImpl` implementations to trigger PlayerKit's native HDR rendition filter.
- Stubs `SimVideoUrlModel.isHaveHdr()Z` -> `false` and `SimBitRate.isHdr()Z` -> `false` to ensure player models report streams strictly as standard dynamic range.

### 27. TikTok Privacy Permissions Stripper (`tikTokPrivacyPermissionsStripperPatch`)
- **Manifest DOM Transformation**:
  - Implemented as a clean `resourcePatch` executing directly against `AndroidManifest.xml`.
  - Uses `Element.stripPermissionsWhere` to query and remove direct children matching `uses-permission` and `uses-permission-sdk-23` without altering unrelated manifest metadata or application attributes.
  - Guarantees zero resource re-encoding regressions (unlike full ARSC recompilation).
- **Comprehensive Manifest Coverage & Pruning Discipline**:
  - Covers all 64 unique permissions extracted from TikTok 47.1.4 standalone manifest (`tiktok_47.1.4_orig.apk`).
  - 51 permissions mapped into 15 categorized boolean toggle switches (5 default `true`: `nfc`, `screenshotDetection`, `oemSignals`, `bluetooth`, `biometric`; 10 default `false`).
  - 7 non-negotiable permissions pruned from manifest stripping and delegated to specialized runtime governors (**Universal Telemetry Neutralizer** and **Device Privacy Guard**).
  - 1 kernel-critical permission (`android.permission.INTERNET`) explicitly excluded to prevent cold startup socket allocation aborts.
- **Dynamic Mutation & Telemetry Summary**:
  - Validates manifest existence before execution.
  - Aggregates enabled toggles into a single pass predicate to prune elements efficiently.
  - Emits concise ASCII telemetry (`[TikTok Privacy Permissions Stripper] Stripped N permission(s) from AndroidManifest.xml: ...`) with clean short names.

### 28. Hide Promotional Content (`hidePromotionalContentPatch`)
- Filters and skips videos disclosing branded or paid-promotional content ("Contenido Promocional" / paid partnership disclosure tags) across the For You, Following, and Friends feeds.
- **Feed API Response Interception**: Hooks `FeedApiService.fetchFeedList` to filter incoming items at the network response boundary before model mapping.
- **Feed Item Model Interception**: Hooks `FeedItemList.getItems()` and `FollowFeedList.getItems()` to sanitize feed collections in-situ.
- **Friends Feed Network Interception**: Hooks `FriendsV3FeedNetworkSource.LJ` (V3 response handler) and the friend feed request `LX/06CX;->LIZLLL` (`/tiktok/v1/friend/friend_feed`, V2) return points to filter `friendsV3Feeds` / `friendFeedData` after deserialization.
- **Multi-Vector Commercial & Branded Metadata Inspection**: Inspects `Aweme` for:
  - `AwemeCommerceStruct` (`bcHashtag` tag disclosure text, `isBrandedContent` / `brandedContentType > 0`, `isBrandOrganicContent` / `brandOrganicType > 0`, `CommerceLabelInfo.bcLabelDisplayType == 1`, `ecSearchBoBcLabelText`).
  - `brandContentAccounts` (tagged sponsor accounts list).
  - `starAtlasOrderId` (ByteDance Star Atlas commercial order ID).
  - `commercialVideoInfo` (commercial video payload marker).
  - `promoteModel` and `promoteIconText` (native in-feed promotion triggers).
  - Multi-locale description, caption, banner, and anchor fallback pattern matching (`#paidpartnership`, `#brandedcontent`, `#contenidopromocional`, `[Contenido Promocional]`, `[Paid partnership]`, `[Contenu sponsorisé]`, `[Colaboración pagada]`, `[Parceria paga]`, etc.).
