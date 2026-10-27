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
| **Custom Brave theme** | `bytecodePatch` | `false` (Opt-in) | `Custom Brave theme resources` | Applies custom colors to Brave dark mode chrome, defaulting to pure OLED black for battery saving on OLED screens. |

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

### E. Theming & Interface Customization
- **Custom Brave theme**:
  - *Objective*: Provide fully customizable styling for Brave dark mode chrome, replacing standard charcoal grey surfaces with custom colors. Defaults preserve pure-black AMOLED styling (#000000) while allowing granular customization of backgrounds, elevated containers, text ink, highlights, and Material You system role mappings.
  - *Two-Tier Surfaces*: Distinguishes between flat base backgrounds (toolbars, window roots, web view containers) and elevated surfaces (dialog boxes, modal bottom sheets, popover menus, and settings cards).
    - When `surfaceUsesBackground` is enabled (`true`, default), all surfaces share the base background color, providing a seamless, uniform true-black AMOLED appearance. Defaults are OLED pure-black out of the box, with #1e2029 as the suggested value for tier depth.
    - When `surfaceUsesBackground` is disabled (`false`), elevated cards and dialogs use the dedicated `surfaceColor` (default `#000000`), creating clear visual separation when set to a distinct tier color such as `#1e2029`.
  - *Material You Dynamic Palette Opt-Out*: On Android 12 and higher, system-wide wallpaper palette extraction dynamically tints app surfaces. When `disableDynamicColors` is enabled (`true`, default), the patch intercepts internal dynamic color readers and rewrites Android system neutral/container color definitions, preventing dynamic system wallpaper colors from overriding the chosen dark theme. Users wanting Android wallpaper-derived tints can toggle this setting to `false`.
  - *Customization Options*:

| Option Key | Typology | Default | Title & Visible Effect | Description & Example |
| :--- | :--- | :--- | :--- | :--- |
| `backgroundColor` | `colorOption` | `#000000` | Background color | Primary background for toolbars, window chrome, and base surfaces. Example: `#000000` for pure OLED black, or `#0d1117` for deep navy. |
| `surfaceUsesBackground` | `booleanOption` | `true` | Surfaces use background color | When true, elevated dialogs and cards share the base background color. When false, elevated surfaces use `surfaceColor`. |
| `surfaceColor` | `colorOption` | `#000000` | Surface / container color | Dedicated color for raised containers, modal sheets, and cards. Defaults to OLED pure-black (#000000) out of the box; use #1e2029 for visible tier depth. |
| `textColor` | `colorOption` | `#f0f2ff` | Text ink color | Primary text ink for labels, addresses, tab headers, and menus. Defaults to Brave slate ink (`#f0f2ff`). |
| `accentColor` | `colorOption` | `#737ade` | Accent highlight color | Interactive highlights, active toggles, links, and action buttons. Defaults to Brave purple-blue (`#737ade`). |
| `disableDynamicColors` | `booleanOption` | `true` | Disable Material You dynamic colors | Prevents wallpaper extraction on Android 12+ from overriding custom dark surfaces. |
| `windowBackgroundColor` | `colorOption` | `#000000` | Window background color | Root window background applied behind decor views in two-tier mode. Defaults to `#000000`. |
| `cardColor` | `colorOption` | `#000000` | Preference card color | Fill color for settings and preference rows in two-tier mode. Defaults to OLED pure-black (#000000) out of the box; use #1e2029 for visible tier depth. |
| `enablePreferenceCards` | `booleanOption` | `true` | Style preference rows as cards | Groups Settings and preference rows into rounded cards with subtle margins in two-tier mode. |
| `modulateTextLuminance` | `booleanOption` | `true` | Modulate text tiers by luminance | Dynamically scales secondary (81%) and disabled (62%) text brightness relative to the primary text ink color. |
| `patchMaterialYouRoles` | `booleanOption` | `true` | Patch Material You system color roles | Rewrites Android S+ system neutral and container color roles in `values-v31+` directories to theme colors. |
| `patchLStarSelectors` | `booleanOption` | `true` | Patch color state list lStar selectors | Rewrites color state list `lStar` luminance attributes in `res/color*` to match background and container surfaces. |
| `patchVectorDrawables` | `booleanOption` | `true` | Patch vector drawable surface fills | Rewrites dark surface fills inside vector drawables in `res/drawable*` to match background and container colors. |

  - *How*: Rewrites color values, vector fills, window background references, and lStar selector resources at patch time across day, night, and version-specific resource directories (`res/values*`, `res/color*`, `res/drawable*`, `res/layout*`, `res/xml*`), updates preference XML defaults, and injects bytecode prologues into dynamic color reader methods in DEX bytecode to return false.

---

## 4. Known Limitations

- **Binary Stripping**: Applying native file removal zeroes selected components in-situ; features depending on those libraries (e.g. AR, XR, or Brave VPN when its library is removed) will become unavailable.
- **Standalone Builds**: Patches target official standalone release APKs (`Bravemonoarm64.apk` or `BraveMonoarm.apk`); split or bundle variants are unsupported.
