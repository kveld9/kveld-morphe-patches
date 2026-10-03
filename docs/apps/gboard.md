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

---

## 📋 Applied Patches Catalog

| Patch Name | Type | Category | Default | Primary Mechanism |
| :--- | :--- | :--- | :---: | :--- |
| **AAPT Resource Workaround** | `resourcePatch` | Stability & Tooling | ✅ Yes | Removes unsupported Android 15 DP2+ attributes (`android:supportsConnectionlessStylusHandwriting`) from input method XML resources to prevent AAPT linking failures. |
| **Gboard Enhancements** | `resourcePatch` + `bytecodePatch` | Customization & Suite | ✅ Yes | Master customization suite bundling in-app toggleable features (Pure AMOLED Theme, Zero Bottom Inset, Force Incognito, Clipboard Enhancements, Toolbar Item Count, Feature Flags, Onboarding status, and Core Integrity) managed directly from *Ajustes > Morphe Patches*. |
| **Block Telemetry** | `bytecodePatch` | Privacy & Security | ✅ Yes | Disables background metrics dispatch, event logging, daily pings, Google Primes profiling, crash reporting, AppDoctor diagnostics, and Tenor share tracking. |
| **Clone Gboard** | `bytecodePatch` + `resourcePatch` | Utility & Modding | ✅ Yes | Appends a custom suffix to the package name to allow installing Gboard alongside the original application. |
| **Disable Background Sync** | `bytecodePatch` | Battery & Debloat | ❌ No | Neutralizes AndroidX WorkManager schedulers, MDD (Mobile Data Download) periodic sync, and Superpacks eager asset synchronization (opt-in to preserve initial dictionary downloads). |
| **Disable Remote Configuration** | `bytecodePatch` | Privacy & Stability | ✅ Yes | Disables periodic remote experiment flag synchronization and background updates. |
| **Hardened Intent Security** | `bytecodePatch` | Security & Integrity | ✅ Yes | Enables Gboard internal external intent protection against unauthorized intent hijacking. |
| **Offline Only** | `bytecodePatch` + `resourcePatch` | Privacy & Security | ❌ No | Completely isolates Gboard from network access by purging manifest permissions, disabling foreground sync services, neutralizing HTTP clients (Cronet, OkHttp, Superpacks), and spoofing offline status. |
| **Resource Slimmer** | `bytecodePatch` | Optimization | ✅ Yes | Strips embedded third-party license text, onboarding tutorial Lottie animations, promotional GIFs, and APK root metadata/junk files. |
| **Strip Permissions** | `resourcePatch` | Privacy & Security | ❌ No | Selectively revokes sensitive hardware, privacy, and system permissions from AndroidManifest.xml. |
| **Universal Slimmers** | `resourcePatch` + `rawResourcePatch` | Optimization | ✅ Yes | `Locale Resource Slimmer`, `DPI Resource Slimmer`, `PNG Asset Optimizer`, and `APK Junk Cleaner`. |

---

## ⚙️ Gboard Enhancements: In-App Customization Suite

The **`Gboard Enhancements`** patch injects a top-level **Morphe Patches** category directly into Gboard's main settings screen (*Ajustes > Morphe Patches*). All runtime-configurable features are consolidated here, eliminating the need to re-patch the APK to adjust settings.

### 1. Actions & Status
- **Enable Gboard in System Settings**: Dynamic warning card shown when Gboard is installed but disabled in Android settings (`Settings > System > Languages & input > Manage keyboards`). Tapping the card opens the system keyboard manager directly.
- **Select Gboard as Active Keyboard**: Dynamic warning card shown when Gboard is enabled but not set as the default input method. Tapping opens the input method picker.
- **Restart Gboard Process**: Dedicated one-tap action card to restart the Gboard process immediately via `AlarmManager` and apply changed settings without requiring manual force-stop or device reboot. Shows live status: *(Restart Pending)* in red when preferences are modified.
- **Pending Restart Feedback**: When any toggle or slider is modified, an inline notification toast (*Restart Gboard to apply changes*) alerts the user that a restart is required for the change to take effect.

### 2. UI & Appearance
- **Pure AMOLED Theme**: Injects a native pure black (`#000000`) theme package into Gboard's theme selector without altering standard Light, Dark, System Auto, or Dynamic Color themes.
- **Key Border Shapes**: Unlocks key shape border selection (Default, Semi-rounded, Round) in theme customization.

### 3. Layout & Ergonomics
- **Zero Bottom Inset**: Eliminates or customizes the navigation bar bottom inset padding (bottom chin/blank space) under the keyboard in gesture navigation mode.
- **Bottom Padding (px)**: Live slider (0 to 150 px, default: `0 px`) to fine-tune the bottom margin. Formatted with live unit display during slider drag.
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

### 5. Smart Features & Voice
- **Grammar Checker & Smart Compose**: Unlocks inline grammar review and Smart Compose predictions under *Correcciones y sugerencias*.
- **Bluetooth Microphone**: Unlocks Bluetooth microphone audio input for voice typing under *Dictado por voz*.

### 6. Privacy & Security
- **Force Incognito Mode**: Always operates in incognito mode (disables personalized learning and persistent input logging) while preserving clipboard functionality.
- **Hide Incognito Icon**: Hides the incognito mask icon on the top toolbar when Force Incognito is active.

### 7. Core Integrity & Startup Resilience
- Neutralizes internal signature validation checks in modified APKs.
- Redirects `LauncherActivity` to verify onboarding/IME status and trampoline directly to `SettingsActivity`.
- Neutralizes Phenotype default flag reset assertion crashes.

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

## ⚙️ Configurable Options: Clipboard Enhancements

The **`Clipboard Enhancements`** patch modernizes Gboard Lite's local clipboard manager by removing artificial limits imposed on history retention, clip capacity, and keyboard layout:

| Option | Key | Type | Default | Range / Format | Description |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Unpinned clip limit** | `unpinnedClipLimit` | String | `50` | `5` to `100` | Maximum number of unpinned clipboard items loaded and displayed in the UI. |
| **Retention time limit (hours)** | `retentionHours` | String | `24` | Integer $\ge 1$ | Duration in hours to retain unpinned clips in SQLite storage and UI before automatic cleanup (e.g. `6`, `12`, `24`, `48`, `168`). |
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

## 🎛️ Configurable Options: Top Toolbar Item Count

The **`Top Toolbar Item Count`** patch allows customizing the maximum number of access point icons displayed directly in Gboard's top toolbar:

| Option | Key | Type | Default | Range / Format | Description |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Toolbar item count** | `itemCount` | String | `5` | `4` to `8` | Maximum number of access point icons displayed on the top toolbar without collapsing into the overflow menu. |
 
---

## 🎛️ Configurable Options: Force Incognito Mode

The **`Force Incognito Mode`** patch includes an opt-in toggle to hide the incognito mask icon from the keyboard toolbar:

| Option | Key | Type | Default | Description |
| :--- | :--- | :--- | :---: | :--- |
| **Hide Incognito Icon** | `hideIncognitoIcon` | Boolean | `false` | Hides the incognito mask icon on the toolbar by replacing it with the standard access points grid icon (`res/6qJ.xml` -> `res/BVL.xml`). |

---

## 📐 Configurable Options: Zero Bottom Inset

The **`Zero Bottom Inset`** patch eliminates the forced empty navigation bar spacer (bottom chin) introduced by gesture navigation in Android 10+:

| Option | Key | Type | Default | Range / Format | Description |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Bottom padding (px)** | `bottomPadding` | String | `0` | `0` to `150` | Forced bottom margin padding in pixels (`0` for completely flush with the bottom edge of the display, or custom value for edge curvature). |

### Technical Architecture:
- Intercepts `KeyboardModeUtils.getKeyboardBottomOffset(Context, int, int, boolean)` (`Lves;->d`) to neutralize Google's internal physical ergonomic margin (`inch * ydpi`), forcing it directly to the configured padding (`0` px by default).
- Intercepts `WindowMetricsNotification.getNavigationBarBottomInset()` (`Laatt;->a`), collapsing the calculated navigation bar bottom offset to the configured pixel value across `KeyboardModeManager`, layout controllers, and touchable regions.
- Eliminates the blank chin space under the spacebar in gesture navigation mode without root, Magisk, or system overlays.

---

## 🎛️ Configurable Options: Feature Flags

The **`Feature Flags`** patch unlocks hidden Google feature flags and experimental UI capabilities via compile/patch-time toggles:

| Option | Key | Type | Default | Description |
| :--- | :--- | :--- | :---: | :--- |
| **Access Points Menu Redesign** | `enableAccessPointsRedesign` | Boolean | `true` | Enables the redesigned access points menu bar and customization panel (Panel V2). |
| **Key Shape Selection** | `enableKeyShapeSelection` | Boolean | `true` | Enables key border shape selection UI (Default, Semi-rounded, Round) in theme customization. |
| **Cursor Trackpad** | `enableCursorTrackpad` | Boolean | `false` | Enables 2D trackpad cursor navigation and cursor lock mode by holding the spacebar (experimental). |
| **Grammar Checker & Smart Compose** | `enableGrammarChecker` | Boolean | `true` | Unlocks Grammar check and Smart Compose / inline suggestions under Text correction preferences. |
| **Dismiss Suggestions Button** | `enableDismissSuggestionsButton` | Boolean | `true` | Adds a close button (X) to dismiss proactive suggestions on the suggestion bar. |
| **Emoji Scale Setting** | `enableEmojiScale` | Boolean | `true` | Unlocks the emoji size scaling setting in Gboard appearance preferences. |
| **Bluetooth Microphone** | `enableBluetoothMicrophone` | Boolean | `true` | Unlocks the 'Use Bluetooth microphone' setting under Voice typing preferences. |

### Technical Architecture & Unlocks:
1. **Cursor Trackpad Mode** (Disabled by default): Long-pressing and swiping across the spacebar enters full 2D cursor navigation mode (moving horizontally and vertically) with haptic feedback. Holding until locked enters sticky cursor mode. Disabled by default due to input connection flickering in web views (such as Firefox/GeckoView). Phenotype resilience is handled automatically via dependency on **Core Integrity**.
2. **Bluetooth Microphone**: Unlocks the dedicated "Usar micrófono Bluetooth" (Use Bluetooth microphone) toggle under *Gboard Settings > Dictado por voz* (Voice typing).
3. **Grammar Checker & Smart Compose**: Unlocks "Revisión gramatical" (Grammar check with blue squiggly underlines) and client-side inline smart suggestions under *Gboard Settings > Correcciones y sugerencias*.
4. **Emoji Scale Setting**: Unlocks the "Tamaño de los emojis" (Emoji size) slider under *Gboard Settings > Preferencias > Apariencia*.
5. **Dismiss Suggestions Button**: Renders a dedicated dismiss button (`X`) on the proactive suggestion bar, allowing quick hiding of proactive recommendations.
6. **Key Shape Selection**: Forces `xsj.i()` to return true, unblocking the key shape border radius selector in custom themes.
7. **Access Points Menu Redesign**: Forces `enable_access_points_menu_redesign` to true, activating Panel V2 toolbar customization.
