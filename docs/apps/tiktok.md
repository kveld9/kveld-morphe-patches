# TikTok Patches Guide

Technical documentation and patch catalog for TikTok on Android.

---

## 1. Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target App** | TikTok |
| **Package Name** | `com.zhiliaoapp.musically` |
| **Target Version** | `47.1.4` |
| **Target Package Format** | standalone APK (nodpi APK) |
| **Primary Architecture** | `arm64-v8a` / `armeabi-v7a` |
| **Download Target** | [APKMirror](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-47-1-4-release/tiktok-47-1-4-2-android-apk-download/) |
| **Authoritative Source** | APKMirror |

---

## 2. Applied Patches Catalog

| Patch Name | Typology | Default State | Dependencies | Description |
| :--- | :--- | :--- | :--- | :--- |
| **No Push Notifications** | `bytecodePatch` | `false` (Opt-in) | None | Stops background push notifications. |
| **Instant Launch** | `bytecodePatch` | `true` (Enabled) | None | Opens the app immediately without splash screens or ad delays. |
| **Disable Live Gift Effects** | `bytecodePatch` | `false` (Opt-in) | None | Disables 3D gift animations in live streams to prevent frame drops. |
| **Remove Live Stream Files** | `rawResourcePatch` | `false` (Opt-in) | None | Removes live streaming and minigame files. Breaks lives and video calls. |
| **Block P2P Video Relay** | `rawResourcePatch` | `true` (Enabled) | None | Stops your device from uploading videos to other users. |
| **Playback & Display Governor** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Limits playback resolution and HDR to save battery, and locks the screen refresh rate. |
| **Battery & Resource Saver** | `bytecodePatch` | `true` (Enabled) | None | Stops 3D ad sensors and aggressive video preloading to save battery and data. |
| **Allow Screenshots** | `bytecodePatch` | `true` (Enabled) | None | Removes screenshot and screen recording blocking. |
| **Camera & Mic Dot** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Shows a corner mark while TikTok holds camera/mic open. |
| **Clean Share Links** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Removes tracking parameters from shared links. |
| **Disable On-Device AI** | `bytecodePatch` | `true` (Enabled) | None | Turns off on-device AI, the chatbot and AI search. |
| **Block Data Access** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Blocks access to your contacts, location, apps and sensors. |
| **Feed Ads & Content Filter** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Filters ads, shopping tags, live cards, and clutter from your feed. |
| **Friends & Suggestions Filter** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Keeps the Friends feed strictly mutual and hides suggested accounts across the app. |
| **Disable History Recording** | `bytecodePatch` | `false` (Opt-in) | None | Prevents TikTok from recording your watch history and search queries. |
| **Open Links In Browser** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Opens web links in your browser and blocks tracking in the remaining web views. |
| **Login Compatibility Fix** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Restores Google and Spotify sign-in on modified app builds. |
| **Browse Without Login** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Lets you browse videos and feeds without creating an account or logging in. |
| **Set App Region** | `bytecodePatch` | `false` (Opt-in) | None | Locks the app to a chosen country to unlock regional content. |
| **Block Trackers (Setup)** | `resourcePatch` | `true` (Enabled) | None | Removes tracking permissions and disables telemetry components. |
| **Strip Sensitive Permissions** | `resourcePatch` | `false` (Opt-in) | None | Removes sensitive hardware, sensor, and tracking permissions. |
| **Block Trackers** | `bytecodePatch` | `true` (Enabled) | None | Neutralizes ByteDance AppLog tracking, crash monitors, and analytics. |
| **Remove Extra Files** | `rawResourcePatch` | `true` (Enabled) | None | Removes card scanners, AI engines and extra fonts to save space. |
| **Remove Creator Tools** | `rawResourcePatch` | `false` (Opt-in) | None | Removes video editing and effects tools to free up space. |
| **Remove Extra Languages** | `rawResourcePatch` | `false` (Opt-in) | None | Deletes unselected language packs. |
| **Remove Voice Tools** | `rawResourcePatch` | `false` (Opt-in) | None | Removes voice search and text-to-speech to save space. Breaks voice features. |
| **Show Video Dates** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Shows the upload date on all videos. |
| **Start First Video Paused** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Pauses the first video on launch so it plays without lag. |
| **Better Comments** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Takes control of comments: sort, copy, translate and hide clutter. |
| **Offline Video Limit** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Customizes the maximum number of videos saved for offline viewing. |
| **Clean Share Menu** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Lets you hide share menu entries you do not use. |
| **Declutter Direct Messages** | `bytecodePatch` | `false` (Opt-in) | None | Cleans up chat and DM screens by hiding extra buttons. |
| **Declutter Video Interface** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Cleans up the buttons and overlays on top of videos. |
| **Declutter Navigation Bar** | `bytecodePatch` | `true` (Enabled) | None | Removes extra tabs and buttons from navigation bars. |
| **Auto-Scroll Videos** | `bytecodePatch` | `true` (Enabled) | None | Enables native auto-scrolling to the next video. |
| **Clean Inbox** | `bytecodePatch` | `true` (Enabled) | None | Hides promo banners, streak cards and suggestions in your inbox. |
| **Hide Inbox Story Tray** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Hides the stories and status tray at the top of the inbox. |
| **Download Without Watermark** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Unlocks downloading and saves videos without watermarks. |
| **Remember Playback Speed** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Keeps your chosen playback speed across videos and restarts. |
| **Block Popups & Prompts** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Blocks popups, nags and promo dialogs across the app. |
| **Unlock Profile Covers** | `bytecodePatch` | `true` (Enabled) | None | Unlocks the profile banner and its editing tools. |
| **Keep Clear View** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Keeps clear-display mode active when scrolling to the next video. |
| **Resume Where You Left Off** | `bytecodePatch` | `false` (Opt-in) | None | Resumes playback from where you left off when returning to a video. |
| **Customize Search** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Customizes search suggestions, live recommendations, and autoplay. |
| **Show Creator Country** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Displays the creator's country code next to their username. |
| **Video Seekbar** | `bytecodePatch` | `true` (Enabled) | `sharedExtensionPatch` | Restores the video progress seekbar and scrubbing controls. |
| **No Auto-Replay** | `bytecodePatch` | `false` (Opt-in) | None | Stops videos at the end instead of replaying them in an infinite loop. |
| **Use System Font** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Uses the system font instead of bundled TikTok fonts. |
| **Video Fit Mode** | `bytecodePatch` | `false` (Opt-in) | `sharedExtensionPatch` | Lets you choose between fitting the whole video or filling the screen. |

---

## 3. Technical Breakdown

### A. Privacy, Ad Filtering & Tracking
- **Feed Ads & Content Filter**:
  - *Objective*: Provide a clean feed without promotional interruptions, sponsored product links, or commerce anchors.
  - *How*: Filters sponsored feed items, live cards, and shopping anchors during feed model parsing.
- **Block Trackers**:
  - *Objective*: Prevent analytics tracking, event logging, and advertising profiling.
  - *How*: Neutralizes internal event dispatchers and strips tracking permissions from the manifest.
- **Device Privacy Guard**:
  - *Objective*: Prevent device fingerprinting and unauthorized hardware or clipboard access.
  - *How*: Blocks queries for installed packages, SIM identifiers, sensor monitoring, and clipboard snooping.
- **In-App Browser Privacy Guard**:
  - *Objective*: Avoid tracking inside embedded web views and open web links in the user's preferred browser.
  - *How*: Intercepts link navigation to launch the external browser and neutralizes script injection in remaining web views.
- **Login Compatibility Fix & Browse Without Login**:
  - *Objective*: Enable video browsing without an account and fix third-party login providers on modified packages.
  - *How*: Bypasses mandatory login interceptors and adjusts authentication callbacks for external providers.

### B. Usability & Playback Enhancements
- **Playback Controls**:
  - *Objective*: Give full control over video scrubbing, auto-scrolling, launch behavior, and playback speed.
  - *How*: Enables seekbar display across all video posts, auto-scrolling to subsequent items, and persistent playback speed settings.
- **Interface Decluttering**:
  - *Objective*: Maximize viewing area by removing non-essential UI buttons, prompts, and carousels.
  - *How*: Hides feed overlay widgets, story trays, direct message banners, and navigation bar tabs.
- **Media Enhancements & Screenshot Protection Bypass**:
  - *Objective*: Save media cleanly and allow screen capture in restricted views.
  - *How*: Removes watermark overlays from download flows and clears secure window flags to permit screenshots.

### C. Resource Governance & Slimming
- **Block P2P Video Relay**:
  - *Objective*: Prevent TikTok from using local device bandwidth to serve content to other users.
  - *How*: Stubs native peer-to-peer relay libraries to halt upload relaying.
- **Battery & Resource Saver**:
  - *Objective*: Save battery life and reduce mobile data consumption.
  - *How*: Neutralizes 3D ad sensor listeners and limits aggressive background video preloading.
- **Slimmer Axes**:
  - *Objective*: Shrink application size on device storage.
  - *How*: Provides opt-in stripping of creator tools, live broadcast engines, voice synthesis models, and unused language packs.

---

## 4. Known Limitations

- **Binary Debloating Side Effects**: Zeroing creator or live streaming libraries disables video recording, camera effects, and live broadcast features.
- **Voice Tools**: Stripping speech engine binaries ceases text-to-speech narration and voice search functionality.
- **Server Enforcement**: Region selection spoofs client carrier and locale indicators; server-side geoblocks or IP-level enforcement require corresponding network routing.
