# Brave Patches Guide

Technical documentation and patch catalog for Brave Private Web Browser on Android.

---

## 1. Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target App** | Brave Private Web Browser, VPN |
| **Package Name** | `com.brave.browser` |
| **Target Version** | `Single pinned version (see README Patch Catalog)` |
| **Target Package Format** | standalone APK (`Bravemonoarm64.apk` or `BraveMonoarm.apk`) |
| **Primary Architecture** | `arm64-v8a` / `armeabi-v7a` |
| **Download Target** | [APKMirror](https://www.apkmirror.com/apk/brave-software/brave-browser/) |
| **Authoritative Source** | github.com/brave/brave-browser/releases |

---

## 2. Applied Patches Catalog

The authoritative patch catalog with descriptions and options is generated in README.md from the built .mpp; see also patches-list.json.

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
  - *How*: Disables sponsored background wallpapers, Brave News feeds, and marketing cards by intercepting preference queries and layout initialization guards (custom NTP wallpaper is unsupported on current Brave). Includes an option to hide shortcut tiles:
    - `hideTopSites`: Removes most-visited shortcut tiles for a completely blank new tab page.
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
  - *Objective*: Provide fully customizable styling for Brave dark and light chrome, replacing standard charcoal grey and white surfaces with custom colors. Defaults preserve pure-black AMOLED styling (#000000) while allowing granular customization of backgrounds, elevated containers, text ink, highlights, and Material You system role mappings across both dark and light modes.
  - *Light Theme Coverage*: Symmetrically skins daytime chrome so that switching to light theme or system day mode does not revert toolbars, the address bar, or the omnibox back to stock white:
    - Base backgrounds (`backgroundColor`) apply to daytime window chrome, toolbar backgrounds, and light system neutral/surface base roles (`system_surface_light`, `system_neutral1_50`).
    - Elevated surfaces (`surfaceColor`) apply to daytime search containers, omnibox backgrounds, and light system container roles (`system_surface_container_light`, `system_surface_container_high_light`).
    - Text ink (`textColor`) applies to daytime text labels, address bar text, and tab titles, inverted from dark ink with luminance scaling preserved.
    - Accent highlights (`accentColor`) apply to daytime toggles, active switches, and interactive highlights.
  - *Two-Tier Surfaces*: Distinguishes between flat base backgrounds (toolbars, window roots, web view containers) and elevated surfaces (dialog boxes, modal bottom sheets, popover menus, and settings cards).
    - When `surfaceUsesBackground` is enabled (`true`, default), all surfaces share the base background color, providing a uniform true-black AMOLED appearance. Defaults are OLED pure-black out of the box, with #1e2029 as the suggested value for tier depth.
    - When `surfaceUsesBackground` is disabled (`false`), elevated cards, omnibox containers, and dialogs use the dedicated `surfaceColor` (default `#000000`), creating clear visual separation when set to a distinct tier color such as `#1e2029`.
  - *Material You Dynamic Palette Opt-Out*: On Android 12 and higher, system-wide wallpaper palette extraction dynamically tints app surfaces. When `disableDynamicColors` is enabled (`true`, default), the patch intercepts internal dynamic color readers and rewrites Android system neutral/container color definitions, preventing dynamic system wallpaper colors from overriding the chosen custom theme across dark and light modes. Users wanting Android wallpaper-derived tints can toggle this setting to `false`.
  - *Customization Options*:

| Option Key | Typology | Default | Title & Visible Effect | Description & Example |
| :--- | :--- | :--- | :--- | :--- |
| `backgroundColor` | `colorOption` | `#000000` | Background color | Primary background for toolbars, window chrome, and base surfaces across dark and light modes. Example: `#000000` for pure OLED black, or `#0d1117` for deep navy. |
| `surfaceColor` | `colorOption` | `#000000` | Surface / container color | Dedicated color for raised containers, modal sheets, cards, and omnibox surfaces. Defaults to OLED pure-black (#000000) out of the box; use #1e2029 for visible tier depth. |
| `textColor` | `colorOption` | `#f0f2ff` | Text ink color | Primary text ink for labels, addresses, tab headers, and menus across dark and light chrome. Defaults to Brave slate ink (`#f0f2ff`). |
| `accentColor` | `colorOption` | `#737ade` | Accent highlight color | Interactive highlights, active toggles, links, and action buttons across dark and light chrome. Defaults to Brave purple-blue (`#737ade`). |
| `windowBackgroundColor` | `colorOption` | `#000000` | Window background color | Root window background applied behind decor views in two-tier mode. Defaults to `#000000`. |
| `cardColor` | `colorOption` | `#000000` | Preference card color | Fill color for settings and preference rows in two-tier mode. Defaults to OLED pure-black (#000000) out of the box; use #1e2029 for visible tier depth. |
| `surfaceUsesBackground` | `booleanOption` | `true` | Surfaces use background color | When true, elevated dialogs, omnibox containers, and cards share the base background color. When false, elevated surfaces use `surfaceColor`. |
| `disableDynamicColors` | `booleanOption` | `true` | Disable Material You dynamic colors | Prevents wallpaper extraction on Android 12+ from overriding custom theme surfaces. |
| `enablePreferenceCards` | `booleanOption` | `true` | Style preference rows as cards | Groups Settings and preference rows into rounded cards with subtle margins in two-tier mode. |
| `modulateTextLuminance` | `booleanOption` | `true` | Modulate text tiers by luminance | Dynamically scales secondary (81%) and disabled (62%) text brightness relative to the primary text ink color. |
| `patchMaterialYouRoles` | `booleanOption` | `true` | Patch Material You system color roles | Rewrites Android S+ system neutral and container color roles in `values-v31+` directories to theme colors across dark and light modes. |
| `patchLStarSelectors` | `booleanOption` | `true` | Patch color state list lStar selectors | Rewrites color state list `lStar` luminance attributes in `res/color*` to match background and container surfaces across dark and light modes. |
| `patchVectorDrawables` | `booleanOption` | `true` | Patch vector drawable surface fills | Rewrites background and container fills inside vector drawables in `res/drawable*` to match theme colors. |

  - *How*: Rewrites color values, vector fills, window background references, and lStar selector resources at patch time across day, night, and version-specific resource directories (`res/values*`, `res/color*`, `res/drawable*`, `res/layout*`, `res/xml*`), updates preference XML defaults, and injects bytecode prologues into dynamic color reader methods in DEX bytecode to return false.

---

## 4. Known Limitations

- **Binary Stripping**: Applying native file removal zeroes selected components in-situ; features depending on those libraries (e.g. AR, XR, or Brave VPN when its library is removed) will become unavailable.
- **Standalone Builds**: Patches target official standalone release APKs (`Bravemonoarm64.apk` or `BraveMonoarm.apk`); split or bundle variants are unsupported.
