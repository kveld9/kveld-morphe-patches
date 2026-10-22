# Gboard Lite Patches Guide

Technical documentation and patch catalog for Gboard Lite on Android.

---

## 1. Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target App** | Gboard Lite |
| **Package Name** | `com.google.android.inputmethod.latin` |
| **Target Version** | `18.5.1.993608717-lite_beta-arm64-v8a` / `18.5.1.993608717-lite_beta-armeabi-v7a` |
| **Target Package Format** | standalone APK (APK nodpi) |
| **Primary Architecture** | `arm64-v8a` / `armeabi-v7a` |
| **Download Target** | [APKMirror (arm64-v8a)](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-1-993608717-beta-release/gboard-the-google-keyboard-18-5-1-993608717-lite_beta-arm64-v8a-android-apk-download/) |
| **Alternative Variant** | `18.5.1.993608717-lite_beta-armeabi-v7a` (alternative 32-bit ARM variant on APKMirror) |
| **Authoritative Source** | APKMirror |

---

## 2. Applied Patches Catalog

| Patch Name | Typology | Default State | Dependencies | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Keyboard Settings** | `resourcePatch` | `true` (Enabled) | AMOLED, Inset, Incognito, Clipboard, Feature Flags, Toolbar, Seekbar, Core Integrity, Haptics | Adds a settings screen inside Gboard: theme, vibration, clipboard and more. |
| **Block Trackers** | `bytecodePatch` | `true` (Enabled) | None | Stops background metrics, event logging and crash reporting. |
| **Install a Copy** | `resourcePatch` | `true` (Enabled) | `gboardCoreIntegrityPatch` | Installs this Gboard next to the original one. |
| **Stop Background Sync** | `bytecodePatch` | `false` (Opt-in) | None | Stops background downloads and sync to save battery. |
| **Disable Cloud Backup** | `resourcePatch` | `true` (Enabled) | None | Keeps keyboard data off Google Drive backups. |
| **Stop Remote Updates** | `bytecodePatch` | `true` (Enabled) | None | Stops Google from changing settings remotely. |
| **Disable Play Services** | `bytecodePatch` | `false` (Opt-in) | None | Runs without Google Play services. Also turns off inline Translate. |
| **Block App Hijacking** | `bytecodePatch` | `true` (Enabled) | `gboardRemoveWebDebugBridgePatch` | Protects against other apps hijacking Gboard and removes debug tools. |
| **Go Offline (Setup)** | `resourcePatch` | `false` (Opt-in) | None | Removes network permissions. |
| **Offline Only** | `bytecodePatch` | `false` (Opt-in) | `Go Offline (Setup)` | Fully disconnects Gboard. Download dictionaries first. |
| **Remove Extra Files** | `resourcePatch` | `true` (Enabled) | None | Deletes licenses, tutorial animations and junk. |
| **Remove Permissions** | `resourcePatch` | `false` (Opt-in) | None | Removes permissions you pick below. |
| **Remove Translate Limit** | `bytecodePatch` | `true` (Enabled) | None | Removes the character limit for inline translation. |

---

## 3. Technical Breakdown

### A. Modular Settings Sub-Patches
The `Keyboard Settings` patch incorporates several internal bytecode and resource layers:
- **Pure Black (AMOLED)**:
  - *Objective*: Provide a true black keyboard theme for OLED screens to conserve battery and reduce glare.
  - *How*: Replaces default dark theme background definitions with pure black color values.
- **Zero Bottom Inset & Custom Padding**:
  - *Objective*: Eliminate unnecessary blank space below the keyboard when using gesture navigation and allow adjusting bottom padding.
  - *How*: Bypasses system navigation bar insets and injects custom bottom padding controls.
- **Force Incognito Mode**:
  - *Objective*: Prevent learning from typed text and protect private sessions.
  - *How*: Enforces incognito keyboard input mode, hides the incognito indicator icon, and prevents clipboard capture during private sessions.
- **Voice Typing Incognito**:
  - *Objective*: Dictate text without sending voice transcripts to remote servers.
  - *How*: Configures speech recognition to operate in private mode without remote transcript logging.
- **Clipboard Enhancements**:
  - *Objective*: Keep copied snippets longer, save more unpinned clips, and display clips in a grid.
  - *How*: Extends clipboard retention time limits up to 30 days (720 hours), raises unpinned item count caps, enables grid layout mode, and expands character length limits.
- **Modern Haptics & Decoupled Feedback**:
  - *Objective*: Customize vibration strength independently of system settings.
  - *How*: Decouples keyboard haptics from system touch vibration toggles and enables modern haptic patterns.
- **Top Toolbar Customization**:
  - *Objective*: Adjust the number of visible utility icons on the top toolbar.
  - *How*: Overrides the maximum item count cap on the top access points bar.

### B. Telemetry & Remote Control Suppression
- **Block Trackers**:
  - *Objective*: Stop background metric uploads and crash telemetry.
  - *How*: Disables event logging, Clearcut metrics, and crash reporting services.
- **Stop Remote Updates**:
  - *Objective*: Prevent server-side experiment flags from changing keyboard configuration remotely.
  - *How*: Neutralizes Google Phenotype configuration synchronization and broadcast receivers.
- **Disable Cloud Backup**:
  - *Objective*: Keep dictionary and keyboard history strictly on the local device.
  - *How*: Removes cloud backup transport agents and attributes from the application manifest.
- **Block App Hijacking**:
  - *Objective*: Protect keyboard input from inter-app interception and remove debugging interfaces.
  - *How*: Restricts exported broadcast receivers and purges web debugging bridge components.

### C. Offline Operation & Permission Stripping
- **Offline Only**:
  - *Objective*: Run Gboard completely isolated from the internet.
  - *How*: Strips network permissions from the manifest, disables background sync services, and forces connectivity monitors to report offline state.
- **Disable Play Services**:
  - *Objective*: Allow Gboard to function on devices without Google Play services installed.
  - *How*: Stubs Google Play services availability checks to report service disabled without retry loops.
- **Remove Permissions**:
  - *Objective*: Reduce attack surface by dropping unused sensitive device permissions.
  - *How*: Provides configurable options to remove Contacts, Microphone, Storage, and System Dictionary permissions.
- **Remove Extra Files**:
  - *Objective*: Reduce application size.
  - *How*: Purges build properties, tutorial animations, uncompressed GIFs, and embedded open-source license texts.

### D. Usability & Input Enhancements
- **Remove Translate Limit**:
  - *Objective*: Allow entering longer text in inline translation without character restrictions.
  - *How*: Strips length filter caps from the inline translation input field upon keyboard layout inflation while preserving other input filters.

---

## 4. Known Limitations

- **Offline Mode Prerequisites**: Offline patches completely sever network access. Language packs, layout models, and dictionaries must be installed prior to enabling offline enforcement.
- **Play Services Decoupling**: Disabling Google Play Services breaks cloud-dependent features such as inline translation.
- **Permission Removal Effects**: Stripping optional permissions directly disables corresponding features (e.g. removing Contacts disables contact name autocompletion suggestions).
