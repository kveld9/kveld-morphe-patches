# Brave Patches Guide

Technical documentation and patch catalog for Brave Private Web Browser on Android.

---

## 1. Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target App** | Brave Private Web Browser, VPN |
| **Package Name** | `com.brave.browser` |
| **Target Version** | `1.98.60` |
| **Target Package Format** | standalone APK (`Bravemonoarm64.apk` or `BraveMonoarm.apk`) |
| **Primary Architecture** | `arm64-v8a` / `armeabi-v7a` |
| **Download Target** | [APKMirror](https://www.apkmirror.com/apk/brave-software/brave-browser/brave-browser-1-98-60-release/brave-private-web-browser-vpn-1-98-60-arm64-v8a-android-12l-apk/) |
| **Authoritative Source** | github.com/brave/brave-browser/releases |

---

## 2. Applied Patches Catalog

| Patch Name | Typology | Default State | Dependencies | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Stop Background Sync** | `bytecodePatch` | `true` (Enabled) | None | Stops background wakeups and battery drain. |
| **Block Trackers (Setup)** | `resourcePatch` | `false` (Opt-in) | None | Removes the Play install referrer permission. |
| **Block Tracking Servers** | `rawResourcePatch` | `false` (Opt-in) | None | Redirects tracking servers to 0.0.0.0. |
| **Block Trackers** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch`, `Block Trackers (Setup)`, `Block Tracking Servers` | Blocks usage analytics, crash uploads and experiment downloads. |
| **Clean Start Page** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Removes sponsored wallpapers, news feed and promo cards. |
| **Keep Tabs Open** | `bytecodePatch` | `true` (Enabled) | None | Stops Brave from minimizing your tabs in the background. |
| **Remove Extra Files** | `rawResourcePatch` | `false` (Opt-in) | None | Deletes unused AR and XR files to shrink the app. |
| **Silence Promo Notifications** | `bytecodePatch` | `true` (Enabled) | None | Stops tips, reward promos and marketing notifications. |
| **Brave Origin (Setup)** | `resourcePatch` | `false` (Opt-in) | None | Turns on Origin preference switches by default. |
| **Brave Origin** | `bytecodePatch` | `true` (Enabled) | `Brave Origin (Setup)`, `braveNativeExtractionPatch` | Unlocks Brave Origin feature switches. |
| **Block Popups & Rating Requests** | `bytecodePatch` | `true` (Enabled) | None | Blocks rating popups, review requests and promo dialogs. |
| **Select Previous Tab on Close** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Selects the previous tab in the tab group when closing a tab. |

---

## 3. Technical Breakdown

### A. Telemetry & Background Sync Neutralization
- **Stop Background Sync**:
  - *Objective*: Stop periodic background wakeups and conserve battery.
  - *How*: Modifies the background sync check to return true, preventing scheduled sync tasks from running.
- **Block Trackers & Hosts**:
  - *Objective*: Prevent transmission of telemetry, crash reports, and remote experiment metrics.
  - *How*: Strips the Google Play install referrer permission from the manifest, neutralizes crash dump uploads, experiment synchronization, and Google Play install referrer attribution queries, and redirects telemetry server endpoints to 0.0.0.0. Provides the option to also block shopping offer servers.

### B. Interface & Promo Suppression
- **Clean Start Page**:
  - *Objective*: Provide a distraction-free start page free of ads, sponsored cards, and unwanted news.
  - *How*: Disables sponsored background wallpapers, Brave News feeds, and marketing cards by intercepting preference checks and feed view models. Includes an option to hide top site tiles.
- **Silence Promo Notifications**:
  - *Objective*: Eliminate unsolicited marketing reminders, tips, and rewards onboarding notifications.
  - *How*: Neutralizes tips notification scheduling, rewards onboarding alarm enqueue, and retention marketing broadcast dispatch.
- **Block Popups & Rating Requests**:
  - *Objective*: Prevent rating prompts, review nag dialogs, and promotional modal interruptions.
  - *How*: Disables in-app review and promotional sign-up eligibility checks, and drops promotional dialog tags before display. Hides the Rate Brave entry and its Support category in Settings.

### C. Browser Navigation & Performance
- **Keep Tabs Open**:
  - *Objective*: Prevent the browser from discarding or minimizing background tabs unexpectedly.
  - *How*: Disables inactive tab suspension and automatic tab minimization logic.
- **Select Previous Tab on Close**:
  - *Objective*: Return directly to the previously viewed tab inside a tab group when closing the active tab.
  - *How*: Alters tab closure selection logic to preselect the predecessor tab within the active tab group.
- **Brave Origin**:
  - *Objective*: Provide access to internal Brave Origin feature switches and experimental settings.
  - *How*: Unlocks Origin preference toggles and bypasses internal feature gatekeepers across Rewards, News, Wallet, and VPN settings.

### D. Resource Debloating
- **Remove Extra Files**:
  - *Objective*: Reduce overall application size on device storage.
  - *How*: Overwrites unused companion native libraries in place with empty stubs, targeting augmented reality and extended reality binaries. Includes an option to also remove the WireGuard VPN library.

---

## 4. Known Limitations

- **Binary Stripping**: Applying native file removal zeroes selected components in-situ; features depending on those libraries (e.g. AR, XR, or Brave VPN when its library is removed) will become unavailable.
- **Standalone Builds**: Patches target official standalone release APKs (`Bravemonoarm64.apk` or `BraveMonoarm.apk`); split or bundle variants are unsupported.
