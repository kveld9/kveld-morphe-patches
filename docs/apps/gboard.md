# ⌨️ Gboard Lite: Complete Patch & Configuration Guide

Comprehensive technical, setup, and configuration guide for **Gboard Lite** (`com.google.android.inputmethod.latin`), covering target requirements, setup workflows for offline dictionaries and Glide Typing, applied patches, and clipboard manager customizations.

---

## 🎯 Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target Application** | Gboard Lite |
| **Package Name** | `com.google.android.inputmethod.latin` |
| **Supported Target Version (ARM64)** | **`18.2.4.969776716-lite_beta-arm64-v8a`** |
| **Supported Target Version (ARMv7a)** | **`18.2.4.969776716-lite_beta-armeabi-v7a`** |
| **Target File Format** | Standalone APK (`APK` - **Do NOT download split bundles**) |
| **Screen Density** | `nodpi` |
| **Official Download Source** | [APKMirror: Gboard - the Google Keyboard](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-2-4-969776716-release/) |

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
   - **Temporarily uncheck:**
     - ❌ `Disable MDD Background Sync`
     - ❌ `Disable Superpacks Eager Sync`
     - ❌ `Disable WorkManager`
   - *(Also ensure `Force Incognito Mode` is unchecked if you want personalized learning and history).*

2. **Open Gboard once with an active Internet connection:**
   - Type a few words, test a swipe gesture, or navigate to *Gboard Settings > Languages* so it downloads your language dictionary and gesture pack into local storage (`/data/data/com.google.android.inputmethod.latin/...`).

3. **Re-apply debloat patches (Optional):**
   - Once your language packs are cached locally on device, you can re-patch with `Disable MDD Background Sync`, `Disable Superpacks Eager Sync`, and `Disable WorkManager` to freeze background network traffic and disk polling permanently.

---

## 📋 Applied Patches Catalog

| Patch Name | Type | Category | Default | Primary Mechanism |
| :--- | :--- | :--- | :---: | :--- |
| **Add AMOLED Theme** | `bytecodePatch` + `resourcePatch` | UI & Appearance | ✅ Yes | Adds a selectable Pure Black AMOLED theme using Gboard's native color_black theme package, without replacing standard themes. |
| **Allow Modified APK** | `bytecodePatch` | Security & Integrity | ✅ Yes | Bypasses internal signature check to allow custom APK execution. |
| **Block Telemetry** | `bytecodePatch` | Privacy & Security | ✅ Yes | Disables background metrics dispatch, event logging, daily pings, Google Primes profiling, crash reporting, AppDoctor diagnostics, and Tenor share tracking. |
| **Clipboard Enhancements** | `bytecodePatch` | Usability & Storage | ✅ Yes | Removes hardcoded 1-hour TTL and 5-clip UI throttling, extending retention up to user-configured hours/items. |
| **Clone Gboard** | `bytecodePatch` + `resourcePatch` | Utility & Modding | ✅ Yes | Appends a custom suffix to the package name to allow installing Gboard alongside the original application. |
| **Disable Background Sync** | `bytecodePatch` | Battery & Debloat | ❌ No | Neutralizes AndroidX WorkManager schedulers, MDD (Mobile Data Download) periodic sync, and Superpacks eager asset synchronization (opt-in to preserve initial dictionary downloads). |
| **Disable Remote Configuration** | `bytecodePatch` | Privacy & Stability | ✅ Yes | Disables periodic remote experiment flag synchronization and background updates. |
| **Enable Access Points Menu Redesign** | `bytecodePatch` | UI & Appearance | ✅ Yes | Enables the redesigned access points menu bar and customization panel (Panel V2). |
| **Enable Bluetooth Microphone** | `bytecodePatch` | Usability & Audio | ✅ Yes | Unlocks Bluetooth microphone recording toggle under Voice typing settings. |
| **Enable Cursor Trackpad** | `bytecodePatch` | Navigation & Control | ✅ Yes | Enables 2D trackpad cursor navigation and cursor lock mode by holding the spacebar, neutralizing Phenotype flag reset conflicts. |
| **Enable Dismiss Suggestions Button** | `bytecodePatch` | UI & Usability | ✅ Yes | Adds a close button (X) to dismiss proactive suggestions on the suggestion bar. |
| **Enable Emoji Scale Setting** | `bytecodePatch` | UI & Appearance | ✅ Yes | Unlocks the emoji size scaling setting in Gboard appearance preferences. |
| **Enable Grammar Checker** | `bytecodePatch` | Usability & Typing | ✅ Yes | Unlocks Grammar check and Smart Compose / inline suggestions under Text correction preferences. |
| **Enable Key Shape Selection** | `bytecodePatch` | UI & Appearance | ✅ Yes | Enables the key border shape selection UI (Default, Semi-rounded, Round) in theme customization. |
| **Force Incognito Mode** | `bytecodePatch` | Privacy & Security | ❌ No | Forces Gboard to always operate in incognito mode (disabling personalized learning and persistent input logging). |
| **Hardened Intent Security** | `bytecodePatch` | Security & Integrity | ✅ Yes | Enables Gboard internal external intent protection against unauthorized intent hijacking. |
| **Offline Only** | `bytecodePatch` + `resourcePatch` | Privacy & Security | ❌ No | Completely isolates Gboard from network access by purging manifest permissions, disabling foreground sync services, neutralizing HTTP clients (Cronet, OkHttp, Superpacks), and spoofing offline status. |
| **Phenotype Flag Resilience** | `bytecodePatch` | Stability & Resilience | ✅ Yes | Neutralizes Phenotype flag registration conflicts to allow runtime flag overrides without crashes. |
| **Resource Slimmer** | `bytecodePatch` | Optimization | ✅ Yes | Strips embedded third-party license text, onboarding tutorial Lottie animations, promotional GIFs, and APK root metadata/junk files. |
| **Strip Permissions** | `resourcePatch` | Privacy & Security | ❌ No | Selectively revokes sensitive hardware, privacy, and system permissions from AndroidManifest.xml. |
| **Top Toolbar Item Count** | `bytecodePatch` | UI & Customization | ✅ Yes | Expands and customizes the maximum number of access point icons displayed directly on the top toolbar (default: 5, range: 4..8). |
| **Universal Slimmers** | `resourcePatch` + `rawResourcePatch` | Optimization | ✅ Yes | `Locale Resource Slimmer`, `DPI Resource Slimmer`, `PNG Asset Optimizer`, and `APK Junk Cleaner`. |

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

## 🚀 Productivity & Usability Unlocks

### 1. Cursor Trackpad Mode (`Enable Cursor Trackpad`)
- **Behavior**: Long-pressing and swiping across the spacebar enters full 2D cursor navigation mode (moving horizontally and vertically) with haptic feedback. Holding until locked enters sticky cursor mode.
- **Phenotype Resilience**: Neutralizes internal Google Phenotype flag assertions (`svl.n`) that previously triggered `IllegalStateException: Resetting default value is disallowed` when XML resource defaults conflicted with patched compile-time defaults.

### 2. Bluetooth Microphone (`Enable Bluetooth Microphone`)
- **Behavior**: Unlocks the dedicated "Usar micrófono Bluetooth" (Use Bluetooth microphone) toggle under *Gboard Settings > Dictado por voz* (Voice typing).
- **Function**: Enables audio capture directly from connected Bluetooth headsets and external wireless microphones during voice input.

### 3. Grammar Checker & Smart Compose (`Enable Grammar Checker`)
- **Behavior**: Unlocks "Revisión gramatical" (Grammar check with blue squiggly underlines) and client-side inline smart suggestions under *Gboard Settings > Correcciones y sugerencias*.

### 4. Emoji Scale Setting (`Enable Emoji Scale Setting`)
- **Behavior**: Unlocks the "Tamaño de los emojis" (Emoji size) slider under *Gboard Settings > Preferencias > Apariencia*, enabling granular scaling of emoji keys independently of system font sizing.

### 5. Dismiss Suggestions Button (`Enable Dismiss Suggestions Button`)
- **Behavior**: Renders a dedicated dismiss button (`X`) on the proactive suggestion bar, allowing quick hiding of proactive recommendations without opening menus.
