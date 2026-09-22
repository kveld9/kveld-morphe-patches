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
| **Usability** | **Playback Speed Persistence** | `bytecodePatch` | Persists user-selected video speed across feed scrolling and restarts. |
| **Usability** | **[Video Quality Governor](#2-video-quality-governor)** | `bytecodePatch` | Decoupled resolution ceilings for playback (e.g. 480p) and downloads (e.g. 1080p). |
| **Usability** | **Skip First-Launch Onboarding** | `bytecodePatch` | Bypasses interest pickers, swipe-up tutorial, language prompts, and consent sheets directly to FYP. |
| **Usability** | **[Custom Offline Videos Limit](#4-custom-offline-videos-limit)** | `bytecodePatch` | Customizes maximum offline videos download caching limit (~X mins, Y GB/MB). |
| **Usability** | **Auto-translate comments** | `bytecodePatch` | Automatically dispatches batch translations via TikTok's native engine. |
| **Privacy** | **Fix Google login** | `bytecodePatch` | Restores Google account sign-in via Web OAuth fallback when GMS rejects modified APK signature. |
| **Privacy** | **Bypass Mandatory Login** | `bytecodePatch` | Neutralizes mandatory login walls, dynamic regional forced login gates, and guest browsing restrictions. |
| **Privacy** | **Clean Share URL** | `bytecodePatch` | Strips tracking query parameters, user tokens, and campaign IDs from shared links. |
| **Privacy** | **Device Privacy Guard** | `bytecodePatch` | Blocks background clipboard inspection, isolates package queries, silences HAR hardware sensors, and clears `FLAG_SECURE`. |
| **Privacy** | **In-App Browser Privacy Guard** | `bytecodePatch` | Redirects external links to default system browser, neutralizes WebView JS tracking injection and AJAX hookers. |
| **Privacy** | **Client-Side AI & Behavioral Profiling Governor** | `bytecodePatch` | Neutralizes Pitaya on-device ML, Tako AI chatbot entries, and AI search clutter. |
| **Privacy** | **[SIM Region Selector](#1-sim-region-selector)** | `bytecodePatch` | Spoofs SIM and network country ISO codes to bypass regional restrictions. |
| **Privacy** | **Feed Ad Blocker** | `bytecodePatch` | Filters sponsored cards, brand promotions, and commercial audio. |
| **Privacy** | **Hide TikTok Shop & Mall** | `bytecodePatch` | Removes product anchors, showcase badges, and bottom/top Shop navigation tabs. |
| **Privacy** | **Feed Live Stream Blocker** | `bytecodePatch` | Removes live broadcast cards and live recommendations from FYP and Following. |
| **Privacy** | **Feed Bloat & Distraction Blocker** | `bytecodePatch` | Removes friend suggestions, mini-games, CapCut prompts, memories, surveys, mini-dramas, Lemon8 promo, and floating rewards pendants. |
| **Privacy** | **Unified Telemetry & Tracker Silencer** | `bytecodePatch` | Neutralizes ByteDance AppLog, APM/Npth/Heimdallr crash telemetry, and AppsFlyer. |
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
| **Universal Slimmers** | `resourcePatch` + `rawResourcePatch` | Optimization | `Locale Resource Slimmer`, `DPI Resource Slimmer`, `PNG Asset Optimizer`, and `APK Junk Cleaner`. |

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
> Unlike apps with standard resource structures, TikTok's entire patch suite strictly avoids resource decoding (`resourcePatch`). Re-encoding TikTok's obfuscated resource tree via `arsclib` drops launcher icon drawables (`res/a/aq2.xml`, `res/a/aq3.xml`). Privacy is enforced at the Dalvik bytecode execution layer (intercepting BPEA clipboard, silencing motion sensors, trapping package queries, and neutralizing analytics trackers via `Unified Telemetry & Tracker Silencer`), while dangerous permissions remain governed by Android's runtime permission model.

- **Universal Package Query Isolation**: Intercepts `PackageManager` query trampolines (`LX/00m8.U3`, `LX/00m8.R3`) and throws a controlled `NameNotFoundException` when external installed apps are queried, isolating app visibility without requiring resource decoding.
- **Contacts Isolation**: Neutralizes BPEA contacts reader `LX/0OFU.LIZ()` to return empty list and `LX/0OFw.LIZ()` to return a null cursor safely.
- **HAR Motion Sensor Silencing**: Injects `return -1` into `HarSensorManager` init and stubs `onSensorChanged` to stop physical movement fingerprinting.
- **FLAG_SECURE Bypass**: Permanently clears `FLAG_SECURE` (`0x2000`) on window layouts via `AntiScreenRecordController.applyFlag`, enabling screenshots and screen recordings in all views.
- **Clipboard Protection**: Blocks programmatic and BPEA background clipboard reading methods (`LX/01ZZ.LIZ`, `LX/0jUy.LIZIZ`).

### 4. Feed Bloat & Distraction Blocker (`feedBloatBlockerPatch`)
- **Rewards Pendants Suppression**: Injects `return-void` into `SpecActWidget.bind(ViewGroup)` to prevent floating Rewards widgets (countdown coins, soccer ball stickers) from attaching to feed views.
- **Sticker & Card Stripping**: Nullifies `Aweme.getActivityPendant()`, `getCommerceStickerInfo()`, `getFloatingCardInfo()`, and `getBannerTip()`.
- **Feed Stream Purge**: Filters suggested accounts, mini-games, CapCut creation prompts, memories recaps, mini-dramas, and surveys before UI adapter binding.
