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

## Chromium DataPack v5 vs. Android Resource Architecture

### Why Chromium Requires Specialized PAK Slimming
Standard Android applications (such as Gboard Lite or Hevy) store localized strings, layouts, and XML assets inside the compiled Android resource table (`resources.arsc`) and directory hierarchy (`res/values-<locale>/`). When a locale qualifier directory is removed, Android's runtime `AssetManager` automatically resolves missing strings using the unquantified `res/values/` fallback directory.

Chromium browsers (Brave, Vivaldi), however, bypass Android's resource pipeline for the browser UI, Omnibox, navigation, Shields, settings, and native rendering engine. Instead, they utilize Chromium's native **DataPack v5** binary format:
1. **Binary DataPack v5 Structure**: Locales are stored as discrete binary files in `assets/locales/<locale>.pak`, consisting of a 12-byte header (`uint32 version`, `uint8 encoding`, `uint24 resource_count`, `uint16 alias_count`), followed by an index of resource IDs and offsets, and raw string payloads.
2. **Native Loader Failure Modes**:
   - Deleting `<locale>.pak` from the APK causes Chromium's native C++ resource loader (`ui::ResourceBundle::LoadLocaleResources`) to fail startup initialization when the host device is set to that language.
   - Truncating `<locale>.pak` to an empty (0-byte) file causes Chromium to trigger an assertion crash (`Check failed: file_is_valid`).
3. **Morphe's Safe Substitution Strategy**:
   - `BraveLocaleSlimmerPatch`: Copies `en-US.pak` byte payloads into all stripped locale paths, satisfying the C++ bundle loader with zero native crashes while freeing ~10.5 MB.
   - `VivaldiLocaleSlimmerPatch`: Distinguishes base locales (substituted with `en-US.pak`) from grammatical gender variants (`*_FEMININE`, `*_MASCULINE`), which receive 18-byte minimal valid DataPack headers to avoid redundant byte duplication, freeing ~21.2 MB.
4. **Separation of Concerns**:
   - `Locale Resource Slimmer` operates universally across all Android apps on `res/values-*`.
   - `Locale PAK Slimmer` operates specifically on Chromium's native `assets/locales/*.pak`.
   - In Brave and Vivaldi, both patches can be combined to achieve complete language stripping at both the C++ native engine and Android wrapper layers.

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

