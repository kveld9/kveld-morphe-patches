# Brave Patches Guide

Technical documentation and patch catalog for Brave Private Web Browser on Android.

---

## 1. Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target App** | Brave Private Web Browser, VPN |
| **Package Name** | `com.brave.browser` |
| **Target Version** | `1.97.59` |
| **Target Package Format** | standalone APK (`Bravemonoarm64.apk` or `BraveMonoarm.apk`) |
| **Primary Architecture** | `arm64-v8a` / `armeabi-v7a` |
| **Download Target** | [APKMirror](https://www.apkmirror.com/apk/brave-software/brave-browser/brave-browser-1-97-59-release/brave-private-web-browser-vpn-1-97-59-arm64-v8a-android-12l-apk/) |
| **Authoritative Source** | github.com/brave/brave-browser/releases |

---

## 2. Applied Patches Catalog

| Patch Name | Typology | Default State | Dependencies | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Stop Background Wakeups** | `bytecodePatch` | `true` (Enabled) | None | Stops background wakeups and battery drain. |
| **Block Telemetry (Setup)** | `resourcePatch` | `false` (Opt-in) | None | Turns off analytics switches by default. |
| **Block Tracking Servers** | `rawResourcePatch` | `false` (Opt-in) | None | Redirects tracking servers to 0.0.0.0. |
| **Block Telemetry** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch`, `Block Telemetry (Setup)`, `Block Tracking Servers` | Blocks usage analytics, crash uploads and experiment downloads. |
| **Clean New Tab (Setup)** | `resourcePatch` | `true` (Enabled) | None | Turns off ads and news switches by default. |
| **Clean New Tab** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch`, `Clean New Tab (Setup)` | Removes sponsored wallpapers, news feed and promo cards. |
| **Keep All Tabs Loaded** | `bytecodePatch` | `true` (Enabled) | None | Stops Brave from minimizing your tabs in the background. |
| **Remove Unused AI & VPN** | `rawResourcePatch` | `false` (Opt-in) | None | Deletes unused AI, VPN and AR files to shrink the app. |
| **Stop Promo Notifications** | `bytecodePatch` | `true` (Enabled) | None | Stops tips, reward promos and marketing notifications. |
| **Brave Rewards (Setup)** | `resourcePatch` | `false` (Opt-in) | None | Turns on Rewards preference switches by default. |
| **Brave Rewards** | `bytecodePatch` | `true` (Enabled) | `Brave Rewards (Setup)`, `Faster Launch` (native extraction and BTI patches) | Unlocks Brave Rewards feature switches. |
| **Faster Launch** | `bytecodePatch` | `true` (Enabled) | `braveNativeExtractionPatch`, `braveBtiCompatibilityPatch` | Speeds up launch by disabling carrier customizations. |
| **Block Popups & Ratings** | `bytecodePatch` | `true` (Enabled) | None | Blocks rating popups, review requests and promo dialogs. |
| **Close Tab In Group Order** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Selects the previous tab in the tab group when closing a tab. |

---

## 3. Technical Breakdown

### A. Telemetry & Background Sync Neutralization
- **Stop Background Wakeups**:
  - *Objective*: Stop periodic background wakeups and conserve battery.
  - *How*: Modifies the background sync check to return true, preventing scheduled sync tasks from running.
- **Block Telemetry & Hosts**:
  - *Objective*: Prevent transmission of telemetry, crash reports, and remote experiment metrics.
  - *How*: Disables analytics preference toggles in settings resources, strips Google Play referrer and ad attribution declarations from the manifest, neutralizes crash dump uploads, experiment synchronization, and Google Play install referrer attribution queries, and redirects telemetry server endpoints to 0.0.0.0. Provides the option to also block shopping offer servers.

### B. Interface & Promo Suppression
- **Clean New Tab**:
  - *Objective*: Provide a distraction-free start page free of ads, sponsored cards, and unwanted news.
  - *How*: Disables sponsored background wallpapers, Brave News feeds, and marketing cards by updating default preference resources and feed view models. Includes an option to hide top site tiles.
- **Stop Promo Notifications**:
  - *Objective*: Eliminate unsolicited marketing reminders, rewards tips, and onboarding notifications.
  - *How*: Intercepts and neutralizes the scheduling and dispatch routines for promotional alerts and rewards onboarding tips.
- **Block Popups & Ratings**:
  - *Objective*: Prevent rating prompts, review nag dialogs, and promotional modal interruptions.
  - *How*: Disables in-app review eligibility checks, Play Store rating dialogs, and promotional sign-up modals.

### C. Browser Navigation & Performance
- **Keep All Tabs Loaded**:
  - *Objective*: Prevent the browser from discarding or minimizing background tabs unexpectedly.
  - *How*: Disables inactive tab suspension and automatic tab minimization logic.
- **Close Tab In Group Order**:
  - *Objective*: Return directly to the previously viewed tab inside a tab group when closing the active tab.
  - *How*: Alters tab closure selection logic to preselect the predecessor tab within the active tab group.
- **Faster Launch**:
  - *Objective*: Accelerate cold browser startup and ensure smooth native execution.
  - *How*: Disables partner customization checks during startup, forces native library extraction in the app manifest, and applies branch target identification adjustments to native binaries.
- **Brave Rewards**:
  - *Objective*: Provide access to internal Brave Rewards feature switches and experimental settings.
  - *How*: Unlocks Rewards preference toggles and bypasses internal feature gatekeepers across Rewards, News, Wallet, and VPN settings.

### D. Resource Debloating
- **Remove Unused AI & VPN**:
  - *Objective*: Reduce overall application size on device storage.
  - *How*: Overwrites unused companion native libraries in place with empty stubs, targeting AI assistants, WireGuard VPN, and augmented reality binaries.

---

## 4. Known Limitations

- **Binary Stripping**: Applying native file removal zeroes selected components in-situ; features depending on those libraries (e.g. Leo AI or VPN) will become unavailable.
- **Standalone Builds**: Patches target official standalone release APKs (`Bravemonoarm64.apk` or `BraveMonoarm.apk`); split or bundle variants are unsupported.
