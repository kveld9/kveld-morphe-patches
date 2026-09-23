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
| **Clipboard Enhancements** | `bytecodePatch` | Usability & Storage | ✅ Yes | Removes hardcoded 1-hour TTL and 5-clip UI throttling, extending retention up to user-configured hours/items. |
| **Disable MDD Background Sync** | `bytecodePatch` + `resourcePatch` | Battery & Debloat | ✅ Yes | Neutralizes Google Mobile Data Download periodic network polling and sync tasks. |
| **Disable Superpacks Eager Sync** | `bytecodePatch` | Battery & Debloat | ✅ Yes | Prevents background Superpacks language model sync scheduling. |
| **Disable WorkManager** | `resourcePatch` | Battery & Optimization | ✅ Yes | Neutralizes AndroidX WorkManager background schedulers in `AndroidManifest.xml`. |
| **Offline Only** | `bytecodePatch` + `resourcePatch` | Privacy & Security | ❌ No | Completely isolates Gboard from network access by purging manifest permissions, disabling foreground sync services, neutralizing HTTP clients (Cronet, OkHttp, Superpacks), and spoofing offline status. |
| **Universal Slimmers** | `resourcePatch` + `rawResourcePatch` | Optimization | ✅ Yes | `Locale Resource Slimmer`, `DPI Resource Slimmer`, `PNG Asset Optimizer`, and `APK Junk Cleaner`. |

---

## 🔒 Configurable Options: Offline Only

The **`Offline Only`** patch provides complete network isolation for privacy-focused setups:

| Option | Key | Type | Default | Description |
| :--- | :--- | :--- | :---: | :--- |
| **Strip Contacts Permission** | `stripContacts` | Boolean | `false` | When enabled, additionally revokes `android.permission.READ_CONTACTS` from `AndroidManifest.xml` for complete device isolation. |

### Technical Architecture:
1. **Manifest Purge**: Strips 9 network/tracking permissions (`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `GET_ACCOUNTS`, `READ_GSERVICES`, `GET_PACKAGE_SIZE`, `FOREGROUND_SERVICE`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`), sets `android:usesCleartextTraffic="false"`, and disables network foreground services (`SuperpacksForegroundTaskService`, `SystemForegroundService`).
2. **Bytecode Neutralization**: Spoofs `DeviceStatusMonitor` to `NO_CONNECTION`, mocks `NetworkInfoNotification` offline predicates, redirects central HTTP clients (Cronet, OkHttp, Superpacks) to immediate `IOException("Offline mode")` exceptions, neutralizes language download queues, and disconnects Glide/WorkManager connectivity listeners.


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
