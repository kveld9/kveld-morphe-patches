# 🛡️ Architecture & Security Notes

## Brave Browser: Privacy Scanner False Positives

> [!NOTE]
> Component scanners such as App Manager or Exodus Privacy may flag Google Play Billing and Google ML Kit components as "trackers" inside Brave. These are false positives caused by generic package name signatures.

When inspecting Brave with package analysis tools, the following components may be highlighted:

| Component | Origin / Library | Actual Function | Privacy & Telemetry Impact |
| :--- | :--- | :--- | :--- |
| `com.android.billingclient.api.ProxyBillingActivity`<br>`com.android.billingclient.api.ProxyBillingActivityV2` | **Google Play Billing** (`billingclient`) | Handles user-initiated in-app subscriptions (Brave Leo AI Premium, Brave VPN). | **None (Transactional only).** These are trampoline UI activities for Google Play checkout sheets. They collect zero browsing analytics or telemetry. Stripping them breaks subscription handling and triggers `ActivityNotFoundException`. |
| `com.google.mlkit.common.internal.MlKitInitProvider`<br>`com.google.mlkit.common.internal.MlKitComponentDiscoveryService` | **Google ML Kit** (`mlkit.common`) | Powers local, on-device OCR and vision features (e.g. camera-based credit card scanning in Autofill and QR code scanning). | **None (On-device execution).** Flagged by Exodus because ML Kit uses the Firebase component dependency injector (`CommonComponentRegistrar`). All model operations run strictly local to the device. |

---

Genuine Brave telemetry is fully neutralized by the **Block Brave Telemetry** patch:
- **P3A (Privacy-Preserving Product Analytics)**: Preference getters forced to return `false` in Dalvik bytecode (`PrefService.e`).
- **Brave Stats & Web Discovery Project (WDP)**: Reporting loops disabled and all 7 telemetry endpoints redirected to `0.0.0.0` in `libchrome.so`.
- **Crashpad & Minidump Uploads**: Upload hooks aborted before dispatch (`MinidumpUploadServiceImpl`, `ChromeMinidumpUploadJobService`) and endpoints zeroed in native binary.
- **Variations Seed Fetching**: Blocked before HTTP socket creation (`IOException("Blocked by Morphe")`).

---

## Brave Browser: Privacy, Debloat & Anti-Fingerprinting Architecture

### 1. Clean New Tab Page (Sponsored Wallpaper & Feed Neutralization)
Official Brave periodically downloads full-screen sponsored advertising wallpapers and updates the Brave News/Today feed in the background. The **`Clean New Tab Page`** patch enforces a lightweight, zero-ad startup state across three layers:
- **Asset Schema Sanitization (`rawResourcePatch`)**: Overwrites `assets/brave/sponsored-images/default.json` with an empty campaigns schema (`{"schemaVersion":1,"campaigns":[]}`). Chromium's sponsored image coordinator finds zero campaigns to download, preventing background bandwidth consumption (~15–30 MB/month) and memory allocations.
- **Preference Defaults Enforcement (`resourcePatch`)**: Sets XML defaults for `show_sponsored_images`, `new_tab_page_show_sponsored_images`, `brave_news_switch`, and `show_brave_news` to `false`.
- **Dalvik Gatekeeper Interception (`bytecodePatch`)**: Intercepts `PrefService.e` for `brave.new_tab_page.show_sponsored_images`, `brave.today.enabled`, `brave.today.opted_in`, and `brave.new_tab_page.show_brave_news`, returning `false` directly at the preference service layer.

### 2. Sensor Privacy Guard (W3C Generic Sensor API Neutralization)
Web applications can fingerprint device hardware variations or infer user input patterns (keystroke acoustic leakage and walking cadence) via high-frequency accelerometer and gyroscope APIs. The **`Sensor Privacy Guard`** patch neutralizes the underlying Chromium sensor providers:
- **`PlatformSensorProvider.hasSensorType(int)`**: Forces return `false` (`0x0`) so hardware sensor existence queries report unsupported.
- **`PlatformSensor.create(PlatformSensorProvider, int, long)`**: Forces return `null` so any low-level or sensor-fusion creation attempts gracefully resolve to `nullptr`.
- **JNI Receiver Stability**: Preserves the `PlatformSensorProvider` Java instance created by `PlatformSensorProvider.create()` because Chromium's native `PlatformSensorProviderAndroid` caches it as a JNI receiver; returning null would trigger a fatal `SIGSEGV` / CheckJNI abort when calling instance methods.
- **Standard Compliant Fallback**: Follows W3C Generic Sensor specifications: sites querying sensors receive standard "NotReadableError" or sensor unavailable responses without crashing web pages.

### 3. Clean Share URL (Link Tracking Sanitization)
When sharing links via Android's native share sheet or copying URLs from the address bar and context menus, sites frequently attach tracking and attribution query tokens (`utm_*`, `fbclid`, `gclid`, `igshid`, `si`, `msclkid`, etc.). The **`Clean Share URL`** patch intercepts shared and copied data:
- **Android Share Sheet Dispatch (`Lcch.a`)**: Intercepts the generated `android.content.Intent` across all share entrypoints, sanitizing `Intent.EXTRA_TEXT`, `Intent.getData()`, and `ClipData` via [`BraveExtension.cleanShareIntent`](../extensions/extension/src/main/java/com/kveld9/morphe/extension/BraveExtension.java).
- **Clipboard Sanitization (`Clipboard.setText`)**: Cleans single URLs and embedded URLs in composite text before strings enter the system clipboard.
- **Invariant Preservation**: Functional parameters such as video identifiers (`v`), navigation anchors (`t`), and search queries (`q`) are strictly preserved while stripping profiling tokens.

#### Scope & False-Positive Prevention Strategy
The parameter sanitizer does **not** attempt to match every ad network parameter across the entire web. It is intentionally designed as a **domain-agnostic deterministic filter**:
- **Why not strip all unknown parameters?** Broad or generic query keys (such as `ref`, `source`, `token`, `id`, `session`, `click`, `campaign`) cannot be purged universally without domain context, as doing so breaks legitimate web navigation, e-commerce checkouts, faceted search filters, and one-time authentication links.
- **Comparison with Domain-Conditional Engines**: Projects like ClearURLs, AdGuard URL Tracking Protection, or Brave's upstream C++ `url_cleaner` maintain hundreds of rules scoped to specific domains (e.g. stripping `tag` only on `amazon.com` or `rdt_cid` only on `reddit.com`). Morphe's lightweight companion runtime prioritizes high-confidence global tokens that can be removed with zero risk of site breakage.

#### Complete Catalog of Filtered Parameters (Chromium Extension)
Implemented in [`ChromiumExtension.java`](../extensions/extension/src/main/java/com/kveld9/morphe/extension/ChromiumExtension.java#L34-L94) and [`isTrackingParam`](../extensions/extension/src/main/java/com/kveld9/morphe/extension/ChromiumExtension.java#L214-L235):
- **Prefix Families (Global Dynamic Matching)**:
  - `utm_*`: Urchin Tracking Module / Google Analytics marketing attribution (`utm_source`, `utm_medium`, `utm_campaign`, `utm_term`, `utm_content`).
  - `ga_*`: Google Analytics client parameters.
  - `pk_*`: Piwik / Matomo campaign tracking.
  - `matomo_*`: Modern Matomo campaign tracking tokens.
- **Global Tokens (Unambiguous AdTech Identifiers)**:
  - *Meta / Facebook / Instagram*: `fbclid`, `igshid`
  - *Google Ads / DoubleClick*: `gclid`, `gbraid`, `wbraid`, `dclid`
  - *Microsoft / Bing*: `msclkid`
  - *Twitter / X*: `twclid`
  - *Yandex*: `yclid`
  - *HubSpot*: `_hsenc`, `_hsmi`
  - *Mailchimp*: `mc_cid`, `mc_eid`
  - *Adobe Analytics / Omniture*: `s_kwcid`
  - *Marketo & Wicked Reports*: `mkt_tok`, `wickedid`
  - *Affiliate & Advertising Networks*: `sc_channel`, `sc_campaign`, `sc_geo`, `zanpid`, `vero_id`, `vero_conv`
- **Domain-Scoped Tokens (Authoritative Context Matching)**:
  - *YouTube* (`youtube.com`, `youtu.be`): `si` (share source identifier)
  - *Spotify* (`spotify.com`): `si` (share source identifier)
  - *Twitter / X* (`x.com`, `twitter.com`): `ref_src`, `ref_url`
  - *LinkedIn* (`linkedin.com`): `trk`
  - *TikTok* (`tiktok.com`): `tt_medium`, `tt_content`

#### Platform-Specific Companion: TikTok URL Sanitization
In contrast to the browser implementation, the TikTok companion filter ([`TikTokFeedAdFilter.sanitizeShareUrl`](../extensions/extension/src/main/java/com/kveld9/morphe/extension/tiktok/TikTokFeedAdFilter.java#L305-L328)) is domain-scoped (`tiktok.com`) and purges ByteDance-specific user tracking and device fingerprinting keys:
- `user_id`, `sec_user_id`, `u_code` (sender user identification)
- `sender_device`, `checksum` (device telemetry and verification)
- `share_link_id`, `share_item_id`, `share_app_id`, `ug_source`, `tt_from`, `timestamp`, `_r`, `source` (viral loop and graph correlation)


---

## Chromium DataPack v5 vs. Android Resource Architecture

### Why Chromium Requires Specialized PAK Slimming
Standard Android applications (such as Gboard Lite or Hevy) store localized strings, layouts, and XML assets inside the compiled Android resource table (`resources.arsc`) and directory hierarchy (`res/values-<locale>/`). When a locale qualifier directory is removed, Android's runtime `AssetManager` automatically resolves missing strings using the unquantified `res/values/` fallback directory.

Chromium browsers (such as Brave), however, bypass Android's resource pipeline for the browser UI, Omnibox, navigation, Shields, settings, and native rendering engine. Instead, they utilize Chromium's native **DataPack v5** binary format:
1. **Binary DataPack v5 Structure**: Locales are stored as discrete binary files in `assets/locales/<locale>.pak`, consisting of a 12-byte header (`uint32 version`, `uint8 encoding`, `uint24 resource_count`, `uint16 alias_count`), followed by an index of resource IDs and offsets, and raw string payloads.
2. **Native Loader Failure Modes**:
   - Deleting `<locale>.pak` from the APK causes Chromium's native C++ resource loader (`ui::ResourceBundle::LoadLocaleResources`) to fail startup initialization when the host device is set to that language.
   - Truncating `<locale>.pak` to an empty (0-byte) file causes Chromium to trigger an assertion crash (`Check failed: file_is_valid`).
3. **Morphe's Safe Substitution Strategy**:
   - `BraveLocaleSlimmerPatch`: Copies `en-US.pak` byte payloads into all stripped locale paths, satisfying the C++ bundle loader with zero native crashes while freeing ~10.5 MB.
4. **Separation of Concerns**:
   - `Locale Resource Slimmer` operates universally across all Android apps on `res/values-*`.
   - `Locale PAK Slimmer` operates specifically on Chromium's native `assets/locales/*.pak`.
   - In Brave, both patches can be combined to achieve complete language stripping at both the C++ native engine and Android wrapper layers.

---

## Hevy: React Native & Hermes Bytecode Architecture

### Why Dynamic Hermes (HBC96) Parsing is Required:
Unlike traditional Android apps whose logic lives exclusively in Dalvik `.dex` files, Hevy compiles its core React Native UI and business state into **Hermes Bytecode** (`assets/index.android.bundle`).
- **The Pitfall of Hardcoded Signatures**: Brittle 32-byte pattern scanners fail across minor Hevy updates because the global string table shifts, changing 16-bit property IDs (e.g. `'subscription'`, `'active_subscription'`).
- **Morphe's AST Resolution**: Morphe's `HevyUnlockProPatch` parses the 128-byte HBC96 file header, dynamically locates `isWithinProOfflineGracePeriod` within the string storage table, resolves its dynamic String ID, and pinpoints the exact function bytecode offset and size in the function headers table.
- **Safe Offline-Pro State**: The injected prologue (`78 00 5C 00` -> `LoadConstTrue r0; Ret r0`) places the app into Hevy's native, supported offline grace period, preserving local database routines and offline analytics without server-side fraud risks.

### Telemetry Neutralization in Hevy:
- **Advertising Permissions**: `AD_ID`, `ACCESS_ADSERVICES_*`, and `BIND_GET_INSTALL_REFERRER_SERVICE` are stripped from `AndroidManifest.xml`, forcing attribution frameworks to receive all-zero identifiers (`00000000-0000-0000-0000-000000000000`).
- **Sentry Crash Reporting**: Content providers disabled in manifest and native bridge methods in `classes6.dex` (`initNativeSdk`) stubbed to resolve boolean true and return immediately.
- **Adjust, Facebook & Amplitude**: Native package dispatch and event queues (`AppEventQueue.flush`, `PackageHandler.addPackage`, `AndroidContextProvider.prefetch`) are neutralized at bytecode level without crashing React Native modules.

---

## Xiaomi Earbuds: Hardware OEM Gating & Catalog Architecture

### 1. Model Catalog Filtering Bypass (`DeviceInfoListCache.isShowProduct`)
- **Upstream Gating Mechanism**: Xiaomi Earbuds evaluates each earphone model against the installed application version using `DeviceInfoListCache.isShowProduct(String model)`. In upstream code, channel checks and strict version regexes can cause supported hardware models to be silently excluded from discovery caches, rejecting device models during Bluetooth LE scanning.
- **Surgical Bytecode Override**: The `Xiaomi Earbuds Model Catalog Unlock` patch hooks `DeviceInfoListCache.isShowProduct(Ljava/lang/String;)Z` to return `true` unconditionally (`const/4 v0, 1; return v0`). This guarantees all device models defined in the internal catalog are recognized, retained in `DeviceInfoListCache`, and discoverable during manual pairing and BLE scanning.

### 2. OEM Feature Gating & Audio Codec Unlock
- **SuperAivs OEM Gating (`SurgicalOEMUnlockPatch`)**: Xiaomi restricts specific integrations (such as SuperAivs, function `5009`) to MIUI/HyperOS devices. The patch intercepts the hardware brand check to unlock full feature parity on non-Xiaomi Android devices (Samsung, Google Pixel, Motorola, OnePlus).
- **Spatial Audio & aptX Adaptive 96kHz (`SoundFeaturesUnlockPatch`)**: Xiaomi restricts Spatial Audio when using 96kHz aptX Adaptive connections. The patch removes this restriction and server-side capability locks, enabling Spatial Audio and hearing enhancements across high-resolution Bluetooth connections.

