# TikTok Patches Guide

Technical documentation and patch catalog for TikTok on Android.

---

## 1. Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target App** | TikTok |
| **Package Name** | `com.zhiliaoapp.musically` |
| **Target Version** | `Single pinned version (see README Patch Catalog)` |
| **Target Package Format** | standalone APK (nodpi APK) |
| **Primary Architecture** | `arm64-v8a` / `armeabi-v7a` |
| **Download Target** | [APKMirror](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/) |
| **Authoritative Source** | APKMirror |

TikTok Lite is a separate target covered in [TikTok Lite Guide](tiktok_lite.md).

---

## 2. Applied Patches Catalog

The authoritative patch catalog with descriptions and options is generated in README.md from the built .mpp; see also patches-list.json.

---

## 3. Technical Breakdown

### A. Privacy, Ad Filtering & Tracking
- **Clean Share Links**:
  - *Objective*: Removes tracking parameters from shared links and supports optional custom share host replacement.
  - *How*: Purges tracking tokens, device telemetry, and marketing campaign parameters from shared links across `tiktok.com` and `tiktxk.com` domains with optional custom hostname replacement via `TikTokShareHostHook`.
- **Feed Ads & Content Filter**:
  - *Objective*: Provide a clean feed without promotional interruptions, sponsored product links, or commerce anchors.
  - *How*: Filters sponsored feed items, live cards, and shopping anchors during feed model parsing, removes ads inserted between a profile's videos, hides posts from chosen creators, with blocked hashtags, matching blocked keywords, or from hidden regions, and filters videos outside configured duration limits, exceeding post age, or outside configured view or like thresholds across For You, Following, and search feeds.
- **Block Trackers**:
  - *Objective*: Prevent analytics tracking, event logging, and advertising profiling.
  - *How*: Neutralizes internal event dispatchers and strips tracking permissions from the manifest.
- **Device Privacy Guard**:
  - *Objective*: Prevent device fingerprinting and unauthorized hardware or clipboard access.
  - *How*: Blocks queries for installed packages, SIM identifiers, sensor monitoring, and clipboard snooping.
- **In-App Browser Privacy Guard**:
  - *Objective*: Avoid tracking inside embedded web views and open web links in the user's preferred browser.
  - *How*: Intercepts link navigation to launch the external browser and neutralizes script injection in remaining web views.
- **Login Compatibility Fix & Mandatory Login Bypass**:
  - *Objective*: Enable video browsing without an account and fix third-party login providers on modified packages.
  - *How*: Bypasses mandatory login interceptors and adjusts authentication callbacks for external providers.

### B. Usability & Playback Enhancements
- **Playback Controls**:
  - *Objective*: Give full control over video scrubbing, auto-scrolling, launch behavior, and playback speed.
  - *How*: Enables seekbar display across all video posts, auto-scrolling to subsequent items, and persistent playback speed settings.
- **Interface Decluttering**:
  - *Objective*: Maximize viewing area by removing non-essential UI buttons, prompts, and carousels.
  - *How*: Hides feed overlay widgets, story trays, direct message banners, and navigation bar tabs.
- **Block Popups & Prompts**:
  - *Objective*: Eliminate modal dialogs, permission nags, and promo sheets across the app.
  - *How*: Suppresses push notification permission prompts, account and contact sync overlays, new user onboarding, PopLayer dialogs, update nags, and sticker recommendations.
- **Media Enhancements & Screenshot Protection Bypass**:
  - *Objective*: Save media cleanly and allow screen capture in restricted views.
  - *How*: Removes watermark overlays from download flows and clears secure window flags to permit screenshots.
- **Comment Customizer**:
  - *Objective*: Provide control over comment sorting, translation, copying, and poll visibility.
  - *How*: Enables comment sorting, clean comment copying, auto-translation, auto-expanding flagged comments, and displaying poll results without voting first.
- **Exact Counts**:
  - *Objective*: Display exact numerical counts instead of rounded metric abbreviations on likes, comments, views, and followers.
  - *How*: Intercepts compact count formatters and ICU notation options to format raw metric values using locale-aware integer formatting.

### C. Resource Governance & Slimming
- **Block P2P Video Relay**:
  - *Objective*: Prevent TikTok from using local device bandwidth to serve content to other users.
  - *How*: Stubs native peer-to-peer relay libraries to halt upload relaying.
- **Battery & Resource Saver**:
  - *Objective*: Save battery life and reduce mobile data consumption.
  - *How*: Neutralizes 3D ad sensor listeners and limits aggressive background video preloading.
- **Disable QUIC**:
  - *Objective*: Fallback to standard HTTPS (TCP/TLS) when UDP or QUIC connections are blocked or throttled by network firewalls or VPNs.
  - *How*: Forces video media loading, upload utilities, and live stream connections to use TCP/TLS instead of QUIC.
- **Slimmer Axes**:
  - *Objective*: Shrink application size on device storage.
  - *How*: Provides opt-in stripping of creator tools, live broadcast engines, voice synthesis models, and unused language packs.

---

## 4. Known Limitations

- **Binary Debloating Side Effects**: Zeroing creator or live streaming libraries disables video recording, camera effects, and live broadcast features.
- **Voice Tools**: Stripping speech engine binaries ceases text-to-speech narration and voice search functionality.
- **Server Enforcement**: Region selection spoofs client carrier and locale indicators; server-side geoblocks or IP-level enforcement require corresponding network routing.
