# Gboard Lite Patches Guide

Technical documentation and patch catalog for Gboard Lite on Android.

---

## 1. Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target App** | Gboard Lite |
| **Package Name** | `com.google.android.inputmethod.latin` |
| **Target Version** | `Single pinned version -- see Supported Targets table in README.md` |
| **Target Package Format** | standalone APK (APK nodpi) |
| **Primary Architecture** | `arm64-v8a` / `armeabi-v7a` |
| **Download Target** | [APKMirror](https://www.apkmirror.com/apk/google-inc/gboard/) |
| **Alternative Variant** | alternative 32-bit ARM variant on APKMirror |
| **Authoritative Source** | APKMirror |

---

## 2. Applied Patches Catalog

The authoritative patch catalog with descriptions and options is generated in README.md from the built .mpp; see also patches-list.json.

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
