# ⚙️ Patch Configuration & Reference Guide

## 🌐 Locale PAK Slimmer (Brave & Vivaldi)

The **`Locale PAK Slimmer`** patch strips unneeded language resource PAKs from `assets/locales/` to reduce APK size (saving **~10.5 MB in Brave** and **~21.2 MB in Vivaldi**).

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

---

## 🌐 Locale Resource Slimmer (Gboard)

The **`Locale Resource Slimmer`** patch strips unselected language translation directories from Gboard's `res/` (such as `values-*`, `raw-*`, `xml-*`) to reduce APK size (saving **~23.15 MB**).

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

## 📱 DPI Resource Slimmer (Vivaldi, Brave & Gboard)

The **`DPI Resource Slimmer`** patch strips unselected screen density asset directories (such as `drawable-mdpi`, `drawable-hdpi`, `drawable-xhdpi`, `mipmap-mdpi`, etc.) from `res/` to significantly reduce final APK size.

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
