# ⚙️ Patch Configuration & Reference Guide

## 🌐 Locale PAK Slimmer (Brave & Vivaldi)

The **`Locale PAK Slimmer`** patch strips unneeded language resource PAKs from `assets/locales/` to reduce APK size (saving **~10.5 MB in Brave** and **~21.2 MB in Vivaldi**).

> [!NOTE]
> ### Why Chromium Browsers Require a Dedicated PAK Slimmer
> Standard Android applications (such as Gboard Lite or Hevy) store localized strings in the standard Android resource hierarchy (`res/values-<lang>/strings.xml`). Android's `AssetManager` automatically falls back to base `res/values/` when a localized folder is deleted, making universal directory removal safe via **`Locale Resource Slimmer`**.
>
> In contrast, Chromium-based browsers (Brave, Vivaldi) compile over 95% of their browser UI strings, Omnibox text, Brave Shields, and core Chromium components into **Chromium DataPack v5 binary archives** located in `assets/locales/<locale>.pak`.
> - **Native C++ Loader Invariant**: Chromium's native C++ resource loader (`ui::ResourceBundle`) expects a valid binary DataPack file for the active device locale. Deleting `<locale>.pak` or replacing it with an empty (0-byte) stub triggers a native segmentation fault or assertion failure on startup (`Check failed: file_is_valid`) whenever the device is set to an unselected language.
> - **Zero-Crash Fallback Substitution**: Instead of deleting files, `Locale PAK Slimmer` safely replaces stripped `.pak` files with the binary table of `en-US.pak` (or 18-byte minimal valid DataPack headers for Vivaldi grammatical gender variants). This satisfies the native C++ loader while reclaiming 10 to 22 MB of storage.
> - **Complementary Operation**: For maximum slimming in Brave and Vivaldi, both `Locale PAK Slimmer` (targeting native `assets/locales/*.pak`) and `Locale Resource Slimmer` (targeting Android wrapper `res/values-*`) can be applied together without conflict.

### Configuration in Morphe Manager

When configuring the **`Locales to keep`** option, specify a comma-separated list of locale codes (e.g. `es-419, es, en-US, pt-BR`).
- English (`en-US`) is always preserved automatically as an essential Chromium fallback.
- To prevent Chromium startup crashes on devices configured with unselected system languages, stripped language PAKs are safely populated with the base `en-US` resource table fallback rather than empty stubs.
- In Vivaldi, corresponding grammatical gender variants (e.g. `es-419_FEMININE`) are preserved automatically.

### Popular Language Codes

| Language | Locale Code(s) |
| :--- | :--- |
| **Spanish** | `es` (Spain / Global), `es-419` (Latin America) |
| **English** | `en-US` (US - *Always kept*), `en-GB` (UK) |
| **Portuguese** | `pt-BR` (Brazil), `pt-PT` (Portugal) |
| **French** | `fr` (France), `fr-CA` (Canada) |
| **German / Italian / Dutch** | `de` (German), `it` (Italian), `nl` (Dutch) |
| **Russian / Ukrainian / Polish** | `ru`, `uk`, `pl` |
| **Japanese / Korean / Chinese** | `ja`, `ko`, `zh-CN` (Simplified), `zh-TW` (Traditional), `zh-HK` (Hong Kong) |
| **Nordic Languages** | `sv` (Swedish), `da` (Danish), `fi` (Finnish), `nb` (Norwegian), `is` (Icelandic) |
| **Regional Languages of Spain** | `ca` (Catalan), `gl` (Galician), `eu` (Basque) |
| **Arabic / Turkish / Hebrew** | `ar`, `tr`, `he` |

<details>
<summary><b>🔍 View all 81 available base locale codes in Brave & Vivaldi</b></summary>
<br>

```text
af, am, ar, as, az, be, bg, bn, bs, ca, cs, da, de, el, en-GB, en-US, es, es-419,
et, eu, fa, fi, fil, fr, fr-CA, gl, gu, he, hi, hr, hu, hy, id, is, it, ja, ka,
kk, km, kn, ko, ky, lo, lt, lv, mk, ml, mn, mr, ms, my, nb, ne, nl, or, pa, pl,
pt-BR, pt-PT, ro, ru, si, sk, sl, sq, sr, sr-Latn, sv, sw, ta, te, th, tr, uk,
ur, uz, vi, zh-CN, zh-HK, zh-TW, zu
```

</details>

## 🌐 Locale Resource Slimmer (Universal)

The **`Locale Resource Slimmer`** patch strips unselected language translation directories from `res/` (such as `values-*`, `raw-*`, `xml-*`) across any supported target APK (e.g. Gboard Lite, Hevy, Brave, Vivaldi) to reduce APK size.

> [!TIP]
> **Chromium Browsers (Brave & Vivaldi)**: While `Locale Resource Slimmer` trims standard Android wrapper resources in `res/values-*`, Chromium browsers store over 95% of their strings (~10–22 MB) in native binary `.pak` files inside `assets/locales/`. For complete multilingual slimming in Brave and Vivaldi, combine this patch with the specialized **`Locale PAK Slimmer`**.

### Configuration in Morphe Manager

When configuring the **`Locales to keep`** option (`locales`), specify a comma-separated list of language codes to preserve (e.g. `es, es-419, pt-BR, fr, de`).
- **Default**: `en` (English `en` and `en-US` are always retained).
- **Base Fallback Safety**: Resource directories without language qualifiers (e.g. `res/values/`, `res/xml/`) are strictly preserved.
- **Prefix Matching**: Specifying a base code like `es` automatically preserves both global Spanish and regional variants (`es-rUS`, `es-rES`, `es-r419`).

### Popular Language Codes

| Language | Locale Code(s) |
| :--- | :--- |
| **English** | `en` (*Always kept by default*), `en-GB`, `en-CA`, `en-AU`, `en-IN` |
| **Spanish** | `es` (Spain / Global), `es-419` / `es-US` (Latin America / US) |
| **Portuguese** | `pt` (Global), `pt-BR` (Brazil), `pt-PT` (Portugal) |
| **French** | `fr` (France / Global), `fr-CA` (Canada) |
| **German / Italian / Dutch** | `de` (German), `it` (Italian), `nl` (Dutch) |
| **Russian / Ukrainian / Polish** | `ru`, `uk`, `pl` |
| **Japanese / Korean / Chinese** | `ja`, `ko`, `zh` (Global), `zh-CN` (Simplified), `zh-TW` (Traditional), `zh-HK` (Hong Kong) |
| **Nordic Languages** | `sv` (Swedish), `da` (Danish), `fi` (Finnish), `nb` (Norwegian), `is` (Icelandic) |
| **Regional Languages of Spain** | `ca` (Catalan), `gl` (Galician), `eu` (Basque) |
| **Arabic / Turkish / Hebrew** | `ar`, `tr`, `iw` (Hebrew) |

<details>
<summary><b>🔍 View all 100 available locale codes in Gboard Lite</b></summary>
<br>

```text
af, ak, am, ar, as, az, be, bg, bn, bo, bs, ca, cs, da, de, el, en, en-rAU,
en-rCA, en-rGB, en-rIN, en-rXC, es, es-r419, es-rES, es-rUS, et, eu, fa, ff,
fi, fr, fr-rCA, gl, gu, ha, hi, hr, hu, hy, id, ig, in, is, it, iw, ja, ka,
kk, km, kn, ko, ky, lo, lt, lv, mk, ml, mn, mr, ms, my, my-rZG, nb, ne, nl,
nod, or, pa, pl, pt, pt-rBR, pt-rPT, ro, ru, se, si, sk, sl, sou, sq, sr, sv,
sw, ta, te, th, tl, tr, uk, ur, uz, vi, yo, zh, zh-rCN, zh-rHK, zh-rTW, zu
```

</details>

---

## 📱 DPI Resource Slimmer (Universal)

The **`DPI Resource Slimmer`** patch is universal and strips unselected screen density asset directories (such as `drawable-mdpi`, `drawable-hdpi`, `drawable-xhdpi`, `mipmap-mdpi`, etc.) from `res/` across all supported target APKs (Vivaldi, Brave, Gboard, Hevy) to significantly reduce final APK size.

### Configuration in Morphe Manager

Specify a comma-separated list of densities to retain:
- **Default value**: `xxhdpi` (corresponds to standard 1080p displays, ~480 dpi, the most common smartphone resolution).
- **Single density (maximum space savings)**: e.g. `xxhdpi` for 1080p devices, or `xxxhdpi` for 1440p / 2K devices.
- **Multiple densities (broad device compatibility)**: e.g. `xhdpi, xxhdpi`.
- **Friendly aliases**: Resolution aliases such as `1080p` (`xxhdpi`), `720p` (`xhdpi`), or `1440p` / `2k` (`xxxhdpi`) are supported.

### Screen Density Reference Guide

| Density Qualifier | Screen DPI Range | Typical Screen Resolution | Example Devices |
| :--- | :--- | :--- | :--- |
| **`mdpi`** | ~160 dpi (1.0x baseline) | 320x480 / 480x800 | Legacy / ultra low-end devices |
| **`hdpi`** | ~240 dpi (1.5x) | 480x854 / 540x960 | Budget entry-level phones |
| **`xhdpi`** | ~320 dpi (2.0x) | 720x1280 (720p HD) | Entry-level / older 720p phones |
| **`xxhdpi`** *(Default)* | ~480 dpi (3.0x) | 1080x1920 / 1080x2400 (1080p FHD+) | **Most modern smartphones** |
| **`xxxhdpi`** | ~640 dpi (4.0x) | 1440x2560 / 1440x3120 (1440p QHD+) | Premium flagships (Galaxy Ultra, Pixel Pro) |

### 🛡️ Zero-Crash Safety Invariants

1. **Protected Density Qualifiers**: Density-independent directories (`drawable-nodpi`, `drawable-anydpi`, `mipmap-anydpi-v26` for vector drawables and adaptive icons) and unquantified base directories (`drawable`, `mipmap`, `values`, `layout`, etc.) are **strictly preserved and never removed**.
2. **Orphan Asset Preservation**: If a graphical asset exists *exclusively* in a directory marked for deletion, it is automatically copied forward to the target preserved directory before deletion. This prevents runtime `Resources$NotFoundException`.
3. **Empty Folder Pruning**: All empty directories left behind by the removal process are cleaned up bottom-up.

---

## 🖼️ PNG Asset Optimizer (Universal)

The **`PNG Asset Optimizer`** is a universal patch that losslessly recompresses PNG assets inside `res/` and `assets/` with maximum zlib compression (`BEST_COMPRESSION`, level 9) and strips non-rendering metadata chunks (`pHYs`, `tEXt`, `tIME`).

### 🛡️ Pixel Safety & 9-Patch Invariants
- **9-Patch Protection**: Files named `*.9.png` or files containing the Android compiled 9-patch chunk `npTc` are **strictly preserved** to prevent UI stretching distortion.
- **Decompression Verification**: Every recompressed PNG stream is inflated and compared in-memory against original raw RGBA pixel buffers prior to writing to disk, guaranteeing zero visual degradation.
- **Anti-Bloat Guard**: If recompression yields a larger file or saves less than 64 bytes, the original file is preserved untouched.

---

## 🧹 APK Junk Cleaner (Universal)

The **`APK Junk Cleaner`** is a universal patch that strips non-functional build metadata, compiler properties, Kotlin coroutines debug tables, and duplicate license notices from `META-INF` and APK root.

### 🛡️ Protected Core Invariants
- **Critical Extensions**: `.dex`, `.arsc`, `.xml`, `.so`, `.rsa`, `.sf`, `.dsa` are strictly protected.
- **Service Loader Integrations**: `META-INF/services/` and `META-INF/MANIFEST.MF` are strictly preserved to maintain dynamic dependency injection.
- **Root Whitelist**: Core root directories (`assets`, `res`, `lib`, `smali`) are protected from accidental pruning.

---

## 🏋️ Hevy: Unlock Pro & Telemetry Hardening

### Unlock Pro (Hermes Bytecode HBC96)
- **Mechanism**: Dynamically parses Hermes Bytecode (HBC96) tables inside `assets/index.android.bundle`, locates property descriptor getters for `isPro`, `isPaying`, `isInGracePeriod`, and `isWithinProOfflineGracePeriod`, and injects a 4-byte prologue (`78 00 5C 00` -> `LoadConstTrue r0; Ret r0`).
- **Features Unlocked**: Activates client-side Pro status directly in `HevyProStore.isPro` and secondary store getters, unlocking unlimited workout routine templates, routine folders, advanced graphs, and local workout analytics.
- **Idempotency**: Detects existing patches and safely exits without double-writing.

### Battery Optimization & Background Sync Killer
- **Mechanism**: Neutralizes 17 background services, alarm proxies, and job schedulers in `AndroidManifest.xml` (including AndroidX WorkManager alarm services, Firebase messaging wakeups, and DataTransport schedulers).
- **Result**: Eliminates battery drain in sleep with 0 active JobScheduler tasks and 0 AlarmManager wakeups.

---

## ⌨️ Gboard: Clipboard Enhancements

The **`Clipboard Enhancements`** patch modernizes Gboard Lite's local clipboard manager by removing artificial limits imposed on history retention, clip capacity, and keyboard layout.

### Configuration in Morphe Manager

| Option | Key | Type | Default | Range / Format | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Unpinned clip limit** | `unpinnedClipLimit` | String | `50` | `5` to `100` | Maximum number of unpinned clipboard items loaded and displayed in the UI. |
| **Retention time limit (hours)** | `retentionHours` | String | `24` | Integer $\ge 1$ | Duration in hours to retain unpinned clips in SQLite storage and UI before automatic cleanup (e.g. `6`, `12`, `24` for 1 day, `48` for 2 days, `168` for 7 days). |
| **Clipboard grid columns** | `gridColumns` | String | `2` | `1`, `2`, or `3` | Number of columns in the clipboard keyboard layout. |

### Technical Architecture & Synchronization

1. **Synchronized TTL Override (`Lgju;->a(Landroid/content/Context;)J`)**:
   - Stock Gboard restricts unpinned clip retention to approximately 1 hour using a hardcoded cutoff calculation.
   - The patch overrides this method to return the user-configured hours converted to milliseconds ($\text{retentionHours} \times 3600 \times 1000 \text{ ms}$).
   - Because `Lgju;->a` is the single source of truth consumed by both the **Background SQLite Pruner (`Lgkr;->g()V`)** and the **UI History Loader (`Lght;->call()`)**, clips are neither deleted from disk nor hidden from the suggestion/clipboard view until the configured duration expires.

2. **In-situ Opcode Throttling Removal (`Lght;->call()`)**:
   - Gboard stock queries clamp unpinned clips using three separate `const/4 ..., 5` opcodes in Dalvik bytecode.
   - The patch detects each target register and rewrites these instructions to `const/16 v$reg, $parsedLimit`, allowing up to 100 recent unpinned items to be fetched from `clipboards.db`.

3. **Custom Grid Span (`ClipboardKeyboard->b()I`)**:
   - Overrides the `StaggeredGridLayoutManager` span count to render 1, 2, or 3 columns cleanly across phones, foldables, and tablets.

---

<a id="sim-region-selector"></a>
## 🎵 TikTok: SIM Region Selector

The **`SIM Region Selector`** patch bypasses geographic content restrictions, regional feed filtering, and country-specific catalog locks by spoofing the SIM and network ISO country codes queried by TikTok.

### Configuration in Morphe Manager

| Option | Key | Type | Default | Range / Format | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Region** | `region` | String | `CH` | 2-letter ISO 3166-1 alpha-2 code | 2-letter ISO country code to spoof for SIM and network country checks. |

### Region Selection Guide & Operational Trade-Offs

When selecting a region code, balance **e-commerce bloat (TikTok Shop / Mall / Live selling)** against **audio licensing and content availability**:

#### 1. Optimal Baseline: `CH` (Switzerland — Default)
* **Zero E-Commerce Bloat**: TikTok Shop, shopping tabs, and affiliate product showcases are not deployed.
* **Full Music Catalog**: Complete access to commercial and international audio without local licensing mutes.
* **No EU Regulatory Overhead**: Being outside the European Union, it avoids recurring Digital Markets Act (DMA) consent dialogs and cookie barriers.
* **Unrestricted Global Feed**: Clean international feed with full upload and viewing availability.
* **Critical Distinction**: `CH` is the ISO code for Switzerland (*Confoederatio Helvetica*). Do **not** confuse with `CN` (China).

#### 2. Anglo-American Trends & North American Catalog: `CA`, `AU`, or `US`
* **`CA` (Canada) / `AU` (Australia)**: Full access to North American and global trending audios with significantly less commercial push and fewer affiliate streams than the US.
* **`US` (United States)**: Maximum creator and audio catalog, but carries the heaviest native deployment of TikTok Shop, live shopping cards, and commercial anchors.
  * *Recommendation*: When using `US`, ensure [Hide TikTok Shop & Mall](tiktok-patches.md#9-hide-tiktok-shop--mall-hidetiktokshopanchorspatch) and [Feed Live Stream Blocker](tiktok-patches.md#10-feed-live-stream-blocker-feedlivestreamblockerpatch) are activated.

#### 3. Problematic Regions to Avoid

| Region | Code | Operational Issue / Rationale |
| :--- | :--- | :--- |
| **China** | `CN` | **Total Lockout**: Mainland China uses the dedicated *Douyin* client. Spoofing `CN` causes the global TikTok client to fail server authentication and reject feed requests. |
| **India** | `IN` | **Government Ban**: TikTok services are blocked nationwide; API requests will fail to resolve. |
| **Russia** | `RU` | **Frozen Feed**: International uploads and global recommendation feeds remain suspended since 2022. |
| **Japan** | `JP` | **Muted Audios**: Stringent domestic copyright regulations (JASRAC) silence a high percentage of international and commercial tracks. |
| **Germany** | `DE` | **Audio Licensing Restrictions**: Strict music rights enforcement (GEMA) silences popular audio tracks, alongside EU regulatory consent dialogs. |
| **Southeast Asia** (`ID`, `TH`, `VN`, `MY`, `PH`) | — | **Heavy Commercial Saturation**: Primary testing ground for live commerce, floating shopping baskets, and affiliate showcases. |

### Supported Region Codes (ISO 3166-1 alpha-2)

The patch accepts any valid **2-letter ISO 3166-1 alpha-2** country code. Inputs are case-insensitive (e.g. `us`, `US`, and `Us` resolve identically).

| Region | ISO Code | Description / Feed Scope |
| :--- | :--- | :--- |
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

### Technical Architecture
- Hooks BPEA telephony wrapper methods `LX/067c;->LIZJ` (`getSimCountryIso`) and `LX/067c;->LIZ` (`getNetworkCountryIso`).
- Safely intercepts queries before they reach the OS `TelephonyManager`, returning the configured region without requiring physical SIM ejection or airplane mode.

---

## 🎵 TikTok: Video Quality Governor

The **`Video Quality Governor`** patch enforces user-configured maximum resolution ceilings (`1080p`, `720p`, `540p`, `480p`, `360p`) across video feeds. While standard TikTok features like "Data Saver" only compress network transfers under cellular conditions without capping hardware decoders, this governor caps the actual rendition ladder (`bitRateList` and `SimBitRate`) parsed by PlayerKit/TTPlayer, reducing hardware MediaCodec load, thermals, GraphicBuffers memory consumption, and frame drops on lower-spec or battery-sensitive devices.

### Configuration in Morphe Manager

| Option | Key | Type | Default | Supported Ceilings | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Maximum Video Resolution** | `maxQuality` | String | `480` | `1080`, `720`, `540`, `480`, `360` | Maximum video playback height in vertical pixels. Discards higher rendition profiles. |

### Supported Resolution Ceilings

| Option String | Resolution Height | Typical Bitrate Band | Target Profile & Resource Rationale |
| :--- | :--- | :--- | :--- |
| `1080` | 1080p | ~2500–4000 kbps | **ExtremelyHigh**: Uncapped full-HD playback for high-end devices and unmetered Wi-Fi. |
| `720` | 720p | ~1200–2000 kbps | **SuperHigh**: High-definition baseline balancing sharp visual fidelity with moderate GPU decoding. |
| `540` | 540p | ~800–1200 kbps | **H_High**: Balanced midpoint optimizing fluid 60fps feed scrolling without thermal buildup. |
| `480` *(Default)* | 480p | ~500–800 kbps | **High**: Recommended sweet spot significantly reducing GraphicBuffers RAM allocation and decoding wattage. |
| `360` | 360p | ~300–500 kbps | **Standard**: Maximum resource and battery conservation; ideal for background listening or weak connections. |

### Technical Architecture
- **Dalvik Hooking**: Injects hooks into `Aweme.getVideo()` (return object synchronization), `Video.getBitRate()` & `Video.getRawBitRate()` (candidate ladder filtering), and `SimVideoUrlModel.getBitRate()` (PlayerKit engine filtering).
- **In-Situ Synchronization**: Invokes `TikTokVideoQualityHook.capVideoObject(Video)` and `TikTokVideoQualityHook.filterBitrates(List)`. Discards streams exceeding the cap and prioritizes the highest valid stream within the ceiling.
- **Fail-Safe Fallback**: If an uploaded video only provides renditions exceeding the ceiling, the governor preserves the lowest available stream rather than black-screening or stalling playback.
- **Preference Persistence**: User selection is saved to `morphe_tiktok_quality_prefs` SharedPreferences, maintaining state across restarts.




