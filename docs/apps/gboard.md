# ⌨️ Gboard Lite: Complete Patch & Configuration Guide

Comprehensive technical, setup, and configuration guide for **Gboard Lite** (`com.google.android.inputmethod.latin`), covering target requirements, setup workflows for offline dictionaries and Glide Typing, applied patches, and clipboard manager customizations.

---

## 🎯 Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target Application** | Gboard Lite |
| **Package Name** | `com.google.android.inputmethod.latin` |
| **Supported Target Version (ARM64)** | **`18.4.1.985164140-lite_beta-arm64-v8a`** |
| **Supported Target Version (ARMv7a)** | **`18.4.1.985164140-lite_beta-armeabi-v7a`** |
| **Target File Format** | Standalone APK (`APK` - **Do NOT download split bundles**) |
| **Screen Density** | `nodpi` |
| **Official Download Source** | [APKMirror: Gboard - the Google Keyboard](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-4-1-985164140-beta/) |

> [!IMPORTANT]
> Always download the standalone `lite` / `lite_beta` APK (nodpi). Do not download multi-split APKM / APK bundles.

---

## 🛠️ Predictive Text & Glide Typing on Fresh Installations

> [!IMPORTANT]
> **Gboard Lite does not bundle language dictionaries, predictive text models, or gesture/glide typing decoding models inside the APK.**
> Unlike the full 80+ MB Gboard APK, Gboard Lite downloads language models on-demand upon first launch via Google's **MDD (Mobile Data Download)** and **Superpacks** subsystems.

If you perform a clean install of Gboard Lite with background sync debloat patches enabled, Gboard will be prevented from downloading the initial dictionary and gesture model pack for your language. This results in an empty suggestion bar, no predictive text, and **Glide Typing (swipe to type) not functioning**.

### Setup Procedure:

1. **When patching for a clean install (or when adding new languages):**
   - **Leave unchecked (default):**
     - ❌ `Disable Background Sync`
   - *(Also ensure `Force Incognito Mode` is unchecked if you want personalized learning and history).*

2. **Open Gboard once with an active Internet connection:**
   - Type a few words, test a swipe gesture, or navigate to *Gboard Settings > Languages* so it downloads your language dictionary and gesture pack into local storage (`/data/data/com.google.android.inputmethod.latin/...`).

3. **Re-apply debloat patches (Optional):**
   - Once your language packs are cached locally on device, you can optionally re-patch with `Disable Background Sync` enabled to freeze background network traffic, WorkManager schedulers, MDD, and Superpacks polling permanently.

> [!NOTE]
> **Inline Google Translate Tool & Play Services:**
> Gboard's inline translation tool (*Translate* / *Traductor* on the top toolbar) does not bundle a standalone HTTP stack; it relies on Google Play Services to provide its Cronet network client dynamically (`PlayServicesCronetProvider`).
> When **`Disable Play Services Integration`** is enabled, GMS availability is reported as `SERVICE_DISABLED`, skipping GMS background sync and telemetry but also preventing Play Services Cronet from initializing, which deactivates inline translation.
> If you rely on Gboard's inline translation tool, leave **`Disable Play Services Integration`** unselected when patching.

---

## 📋 Applied Patches Catalog

| Patch Name | Type | Category | Default | Primary Mechanism |
| :--- | :--- | :--- | :---: | :--- |
| **Gboard Enhancements** | `resourcePatch` + `bytecodePatch` | Customization & Suite | ✅ Yes | Master customization suite bundling in-app toggleable features (Pure AMOLED Theme, Zero Bottom Inset, Independent Keyboard Vibration, Force Incognito, Voice Typing in Incognito, Clipboard Enhancements, Toolbar Item Count, Feature Flags, Onboarding status, and Core Integrity) managed directly from *Settings > Morphe Patches*. |
| **Block Telemetry** | `bytecodePatch` | Privacy & Security | ✅ Yes | Disables background metrics dispatch, event logging, daily pings, Google Primes profiling, crash reporting, AppDoctor diagnostics, and Tenor share tracking. |
| **Clone Gboard** | `bytecodePatch` + `resourcePatch` | Utility & Modding | ✅ Yes | Appends a custom suffix to the package name to allow installing Gboard alongside the original application. |
| **Disable Background Sync** | `bytecodePatch` | Battery & Debloat | ❌ No | Neutralizes AndroidX WorkManager schedulers, MDD (Mobile Data Download) periodic sync, and Superpacks eager asset synchronization (opt-in to preserve initial dictionary downloads). |
| **Disable Cloud Backup** | `resourcePatch` | Privacy & Security | ✅ Yes | Disables Android backup for Gboard (allowBackup=false and backup agent removed) so keyboard settings, learned words, and personal dictionary data are never uploaded to Google Drive backups or copied by device-to-device transfer. Trade-off: Gboard data no longer migrates to a new device through Android backup or restore. |
| **Disable Remote Configuration** | `bytecodePatch` | Privacy & Stability | ✅ Yes | Disables periodic remote experiment flag synchronization and background updates. |
| **Disable Play Services Integration** | `bytecodePatch` | Privacy & Battery | ❌ No | Makes Gboard's Google Play services availability check always report SERVICE_DISABLED, so GMS-backed code paths (Clearcut logging, Phenotype, account sync, Google Help feedback) are skipped at the source instead of being attempted. SERVICE_DISABLED is used instead of SERVICE_MISSING because GoogleApiAvailability remaps SERVICE_MISSING to SERVICE_UPDATING when the GMS package is installed, which makes GoogleApiManager retry every few seconds. Trade-off: Disables Gboard's inline Google Translate tool, which routes network requests through Play Services' dynamic Cronet provider (`PlayServicesCronetProvider`). If you need inline translation, leave this patch unselected. |
| **Hardened Intent Security** | `bytecodePatch` + `resourcePatch` | Security & Integrity | ✅ Yes | Enables Gboard internal external intent protection against unauthorized intent hijacking and removes the exported, permissionless web debug bridge content provider. |
| **Offline Only** | `bytecodePatch` + `resourcePatch` | Privacy & Security | ❌ No | Completely isolates Gboard from network access by purging manifest permissions, disabling foreground sync services, neutralizing HTTP clients (Cronet, OkHttp, Superpacks), and spoofing offline status. |
| **Resource Slimmer** | `bytecodePatch` | Optimization | ✅ Yes | Strips embedded third-party license text, onboarding tutorial Lottie animations, promotional GIFs, and APK root metadata/junk files. |
| **Strip Permissions** | `resourcePatch` | Privacy & Security | ❌ No | Selectively revokes sensitive hardware, privacy, and system permissions from AndroidManifest.xml. |
| **Universal Slimmers** | `resourcePatch` + `rawResourcePatch` | Optimization | ✅ Yes | `Locale Resource Slimmer`, `DPI Resource Slimmer`, `PNG Asset Optimizer`, and `APK Junk Cleaner`. |

---

## ⚙️ Gboard Enhancements: In-App Customization Suite

The **`Gboard Enhancements`** patch injects a top-level **Morphe Patches** category directly into Gboard's main settings screen (*Settings > Morphe Patches*). All runtime-configurable features are consolidated here, eliminating the need to re-patch the APK to adjust settings. All preference titles, summaries, category headers, status cards, and live slider units dynamically adapt to the active device/app language (supporting Spanish on `es` locales with English fallback).

### 🌐 Multi-Language Support & Community Contributions
The settings UI automatically detects the active device system language (`LocaleList` on Android 7+ and legacy `locale`) and routes strings to the corresponding language pack:
- **Currently Supported**: English (`en`, base fallback) and Spanish (`es`).
- **Granular Fallback**: If a key is omitted in a regional language pack, it falls back to the English string without breaking or showing empty/null values.
- **Contributing Translations**: The localization engine is modular and designed for easy community contributions. Anyone can submit a new language pack via a single Pull Request. For step-by-step instructions and a template, refer to the [Gboard i18n Contribution Guide](../../extensions/extension/src/main/java/com/kveld9/morphe/extension/gboard/i18n/README.md).

### 1. Actions & Status
- **Enable Gboard in System Settings**: Dynamic warning card shown when Gboard is installed but disabled in Android settings (`Settings > System > Languages & input > Manage keyboards`). Tapping the card opens the system keyboard manager directly.
- **Select Gboard as Active Keyboard**: Dynamic warning card shown when Gboard is enabled but not set as the default input method. Tapping opens the input method picker.
- **Restart Gboard Process**: Dedicated one-tap action card to restart the Gboard process immediately via `AlarmManager` and apply changed settings without requiring manual force-stop or device reboot. Shows live status: *(Restart Pending)* in red when preferences are modified.
- **Pending Restart Feedback**: When any toggle or slider is modified, an inline notification toast (*Restart Gboard to apply changes*) alerts the user that a restart is required for the change to take effect.

### 2. UI & Appearance
- **Pure AMOLED Theme**: Injects a native pure black (`#000000`) theme package into Gboard's theme selector without altering standard Light, Dark, System Auto, or Dynamic Color themes.
- **Key Border Shapes**: Unlocks key shape border selection (Default, Semi-rounded, Round) in theme customization.

### 3. Layout & Ergonomics
- **Zero Bottom Inset**: Eliminates or customizes the navigation bar bottom inset padding (bottom chin/blank space) under the keyboard in gesture navigation mode. On Android 13+ it can also hide the system-drawn IME navigation bar (hide-keyboard chevron and IME switcher) that the framework reserves under the keyboard, so the keyboard sits flush with the screen edge; use the Bottom Padding slider to add space back.
- **Bottom Padding (px)**: Live slider (0 to 150 px, default: `0 px`) to fine-tune the bottom margin. Formatted with live unit display during slider drag.
- **Hide IME Navigation Bar**: Toggle (default: off, requires Zero Bottom Inset) controlling the Android 13+ system IME bar hiding. Turn it on for a fully flush keyboard; leave it off to keep the keyboard switcher and collapse buttons visible while retaining Gboard's internal bottom offset removal.
- **Top Toolbar Item Count**: Live slider (4 to 8, default: `5`) controlling the maximum number of access point icons displayed on the top toolbar before collapsing into the overflow menu.
- **Dismiss Suggestions Button**: Renders a close button (`X`) on proactive suggestion strips to quickly dismiss recommendations.
- **Cursor Trackpad Mode**: Unlocks 2D trackpad cursor navigation and cursor lock mode by holding and sliding across the spacebar.

### 4. Clipboard Manager
- **Extended History Retention**: Enables custom retention duration limit for unpinned clips in history.
- **Retention Time Limit (Hours)**: Live slider (1 to 168 hours, default: `24h`) controlling unpinned clip expiration in SQLite database and UI.
- **Raise Unpinned Clips Limit**: Enables custom limit for unpinned clipboard history items.
- **Unpinned Clips Limit**: Live slider (5 to 100 items, default: `50`) controlling the maximum unpinned items displayed in the clipboard panel.
- **Clipboard Grid Layout**: Enables multi-column layout for clipboard clips.
- **Clipboard Grid Columns**: Live slider (1, 2, or 3 columns, default: `2`) controlling clipboard grid columns across phones, foldables, and tablets.
- **Clip Character Limit**: Live slider (5k to 200k characters, default: `20k`, Gboard's stock value) overriding the `text_clip_item_char_limit` flag that caps how many characters each text clip stores. Restart Gboard after changing it, since the flag is read once at class initialization.

### 5. Haptics & Vibration
- **Independent Keyboard Vibration**: Decouples Gboard keypress vibration from Android's system-wide Touch feedback and gesture navigation haptics setting. When enabled (default: on), Gboard maintains its own vibration response even if system-wide touch feedback is disabled in Android settings, preventing unwanted gesture haptics elsewhere in the OS.
- **Modern Keypress Haptics**: Gboard ships its haptic-primitive keypress path (`VibrationEffect.Composition`, the crisp system tick) disabled behind `vibration_effect_min_sdk = 1024`, an API level no device reports. When enabled (default: on), the minimum is lowered to API 30 so keypresses use the primitive tick instead of a plain one-shot buzz. Gboard's own `areAllEffectsSupported()` hardware check is untouched, so vibrators without primitive support keep the legacy path. On the primitive path the vibration strength slider maps to intensity rather than milliseconds. Restart Gboard after toggling, since the flag is read once at class initialization.

### 6. Smart Features & Voice
- **Grammar Checker & Smart Compose**: Unlocks inline grammar review and Smart Compose predictions under *Text correction* (*Correcciones y sugerencias*).
- **Bluetooth Microphone**: Unlocks Bluetooth microphone audio input for voice typing under *Voice typing* (*Dictado por voz*).

### 7. Privacy & Security
- **Force Incognito Mode**: Always operates in incognito mode (disables personalized learning and persistent input logging).
- **Hide Incognito Icon**: Hides the incognito mask icon on the top toolbar when Force Incognito is active.
- **Voice Typing in Incognito**: Unlocks speech dictation and voice typing microphone input in incognito mode and private input fields (toggleable switch under Morphe Patches > Privacy & Security, default: enabled).
- **Clipboard in Incognito**: Unlocks clipboard history and paste in incognito mode and private input fields (toggleable switch under Morphe Patches > Privacy & Security, default: enabled).

### 8. Core Integrity & Startup Resilience
- Neutralizes internal signature validation checks in modified APKs.
- Redirects `LauncherActivity` to verify onboarding/IME status and trampoline directly to `SettingsActivity`.
- Neutralizes Phenotype default flag reset assertion crashes.

### 9. Root Settings Declutter
- **Privacy Opt-In Removal**: Strips the *Privacy* header (*Privacidad* / `PrivacySettingsFragment`) which previously contained telemetry opt-ins and personalized statistics.
- **Legal & Terms Removal**: Purges the *About* header (*Información* / `AboutSettingsFragment`) which only routed to external Google open-source license and terms URLs.
- **Feedback & Share Dispatchers Removal**: Removes *Help & feedback* (*Ayuda y comentarios* / `help_and_feedback`), *Share Gboard* (*Compartir Gboard* / `sharing`), `RateUsPreference`, and `FooterPreference` from root settings screens (`settings.xml` and `settings_legacy.xml`).
- **Clean Container Pruning**: Automatically prunes empty `PreferenceCategory` containers left after child removal, presenting a streamlined, distraction-free root settings menu.

---

## 🔒 Network Isolation: Offline Only

The **`Offline Only`** patch provides complete network isolation for privacy-focused setups.

### Technical Architecture:
1. **Manifest Purge**: Strips 8 network/tracking permissions (`INTERNET`, `ACCESS_WIFI_STATE`, `GET_ACCOUNTS`, `READ_GSERVICES`, `GET_PACKAGE_SIZE`, `FOREGROUND_SERVICE`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`), retains `ACCESS_NETWORK_STATE` to prevent GMS Cronet runtime `SecurityException` crashes while blocking actual network traffic at the socket/HTTP layer, sets `android:usesCleartextTraffic="false"`, and disables network foreground services (`SuperpacksForegroundTaskService`, `SystemForegroundService`).
2. **Bytecode Neutralization**: Spoofs `DeviceStatusMonitor` to `NO_CONNECTION`, mocks `NetworkInfoNotification` offline predicates, redirects central HTTP clients (Cronet, OkHttp, Superpacks) to immediate `IOException("Offline mode")` exceptions, neutralizes language download queues, and disconnects Glide/WorkManager connectivity listeners.

---

## 🛡️ Configurable Options: Strip Permissions

The **`Strip Permissions`** patch provides granular control over sensitive hardware, privacy, and system permissions declared in `AndroidManifest.xml`:

| Option | Key | Type | Default | Description |
| :--- | :--- | :--- | :---: | :--- |
| **Strip Contacts Permission** | `stripContacts` | Boolean | `false` | Revokes `android.permission.READ_CONTACTS` from `AndroidManifest.xml` (disables contact name suggestions). |
| **Strip Microphone Permission** | `stripAudio` | Boolean | `false` | Revokes `android.permission.RECORD_AUDIO` from `AndroidManifest.xml` (disables voice dictation). |
| **Strip Media & Storage Permissions** | `stripMedia` | Boolean | `false` | Revokes `READ_MEDIA_IMAGES`, `READ_MEDIA_VISUAL_USER_SELECTED`, and `READ_EXTERNAL_STORAGE` from `AndroidManifest.xml` (disables custom image background themes). |
| **Strip System Dictionary Permissions** | `stripUserDictionary` | Boolean | `false` | Revokes `READ_USER_DICTIONARY` and `WRITE_USER_DICTIONARY` from `AndroidManifest.xml`. |
| **Strip Cross-Profile Permission** | `stripCrossProfile` | Boolean | `false` | Revokes `INTERACT_ACROSS_PROFILES` from `AndroidManifest.xml` to isolate work and personal profiles. |

---

## 🏷️ Configurable Options: Clone Gboard

The **`Clone Gboard`** patch allows running a patched build alongside stock Gboard by modifying the application package identifier:

| Option | Key | Type | Default | Description |
| :--- | :--- | :--- | :---: | :--- |
| **Package Suffix** | `packageSuffix` | String | `clone` | Suffix appended after the original package name (e.g., `clone` -> `com.google.android.inputmethod.latin.clone`). |

