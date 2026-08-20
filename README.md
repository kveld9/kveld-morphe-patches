<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Target-Brave_Browser_v1.93.137-FF4500?style=for-the-badge&logo=brave&logoColor=white" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge" />
</p>

<h1 align="center">🦁 Brave Patches</h1>

<p align="center">
  Custom Morphe patch suite for <b>Brave Browser</b> on Android — Brave Origin feature unlock, multi-layer telemetry blocking, and background/startup performance optimizations.
</p>

<p align="center">
  <a href="https://morphe.software/add-source?github=kveld9/brave-patches"><img src="https://img.shields.io/badge/Morphe_Manager-Add_Patch_Source-8A2BE2?style=for-the-badge&logo=android" /></a>
</p>

---

## 📖 About

This repository provides custom, community-developed bytecode, native, and resource patches for **Brave Browser** using the **[Morphe](https://morphe.software)** patcher framework.

### 📲 Add to Morphe Manager

Click the button below or add `kveld9/brave-patches` in Morphe Manager patch sources:

👉 **[Add Brave Patches to Morphe](https://morphe.software/add-source?github=kveld9/brave-patches)**

---

## ✨ Features & Technical Details

### 🔓 1. Brave Origin (`BraveOriginPatch.kt`)
Unlocks Brave Origin features and implements a decoupled local toggle system:
* **Subscription & Token Mocking**:
  * Bytecode hooks on `isSubscriptionActive(Profile)` and `hasValidSubscriptionTokens(Profile)` force return `true` (`const/4 v0, 0x1`).
  * Mocks `isCredentialSummaryCached()` to `true` and intercepts `requestCredentialSummary(Profile, Callback)` to immediately invoke `Callback.onResult(Boolean.TRUE)` without querying remote Google Play Billing / Brave licensing servers.
  * Stubs `syncOriginPackageProduct()` and `showOriginSettingsForRestart()` into `return-void`.
* **Local Preference Routing (`BraveOriginPreferences`)**:
  * Intercepts `onPreferenceChange()` in the Origin settings UI fragment to persist toggle states directly into Android `SharedPreferences` under the key prefix `brave_origin_off_<feature>`.
  * Dynamically refreshes UI preference view states via `e5()`.
* **Declarative Policy Gatekeepers**:
  * Hooks individual feature policy evaluators (`brave.rewards.disabled_by_policy`, `brave.news.disabled_by_policy`, `brave.wallet.disabled_by_policy`, `brave.brave_vpn.disabled_by_policy`, `brave.ai_chat.enabled_by_policy`).
  * Redirects policy verification to local `SharedPreferences` checks (`ActivityThread.currentApplication()`) instead of remote server policies.
* **Resource Defaults**:
  * Sets `android:defaultValue="true"` on Origin switches (`rewards_switch`, `vpn_switch`, `leo_ai_switch`, etc.) in layout XMLs.

---

### 🛡️ 2. Block Brave Telemetry (`BraveBlockTelemetryPatch.kt`)
Implements a **4-layer defense in depth** architecture against tracking and diagnostic dispatch:
* **Layer 1 (Resource Defaults)**:
  * Scans layout XMLs to force `defaultValue="false"` on `privacy_preserving_analytics_switch`, `statistics_reporting_switch`, and `web_discovery_project_switch`.
* **Layer 2 (Native ARM64 `libchrome.so` Binary Patching)**:
  * **P3A Scheduler**: Overwrites conditional branch at `0x0ab8abe0` with an unconditional branch (`b 0x0ab8aaac`), immediately triggering the scheduler abort routine in `P3AService::InitScheduler`.
  * **Brave Stats**: Replaces the upload trigger branch at `0x0c3aa0e8` with an ARM64 `nop` (`0x1f 0x20 0x03 0xd5`), silencing `BraveStatsUpdater::Start`.
  * **Web Discovery Project (WDP)**: Patches branch at `0x0c3e9388` (`b 0x0c3e9394`) to bypass search host/query extraction in `BraveSearchDefaultHostExtractor`.
* **Layer 3 (DNS / Host Redirection to `0.0.0.0`)**:
  * Replaces 10 ASCII telemetry endpoint strings directly in `libchrome.so` with null-padded `0.0.0.0`:
    * `star-randsrv.bsg.brave.com`, `collector.bsg.brave.com`, `usage-ping.brave.com`
    * `patterns.wdp.brave.com`, `collector.wdp.brave.com`, `star.wdp.brave.com`, `quorum.wdp.brave.com`
    * `cr.brave.com`, `variations.brave.com`, `crashpad.chromium.org`.
* **Layer 4 (Bytecode / Smali Interception)**:
  * **Crashpad Minidump Uploads**: Hooks `MinidumpUploadServiceImpl.tryUploadCrashDumpWithLocalId` (`return-void`) and `ChromeMinidumpUploadJobService.onStartJob` (`return false`).
  * **Variations Seed Fetching**: Intercepts `HttpURLConnection` for `https://variations.brave.com/seed` and throws an `IOException("Blocked by Morphe")` before any network socket is opened.
  * **PrefService Query Blocker**: Hooks `PrefService.e(String)` to return `false` on any checks for `brave.p3a.enabled`, `brave.stats.reporting_enabled`, and `brave.web_discovery_enabled`.

---

### ⚡ 3. Brave Startup Performance Optimization (`BraveStartupPerformancePatch.kt`)
* **Mechanism**:
  * Intercepts `PartnerBrowserCustomizations.initializeAsync(Context)` and immediately sets the initialized state flag `b = Boolean.TRUE` before returning (`return-void`).
* **Technical Benefit**:
  * Eliminates OEM carrier content provider queries (`ContentResolver`), main-thread `SharedPreferences` disk I/O, background thread pool execution, and 10-second watchdog handlers during cold app launch.

---

### 🔕 4. In-Product Notification Scheduler Optimization (`BraveNotificationSchedulerOptimizationPatch.kt`)
* **Mechanism**:
  * **Prevent Scheduling**: Stubs `NotificationSchedulerTask.schedule(long, long)` to `return-void`, preventing Chromium C++ from registering background tasks with Android `JobScheduler` (Job ID 105).
  * **Cancel Persisted Jobs**: In `NotificationSchedulerTask.c(...)` (`onStartTask`), immediately calls `NotificationSchedulerTask.cancel()` and returns `1` (`STOP_TASK`).
* **Technical Benefit**:
  * Prevents periodic background wakeups while the browser is closed and stops Android OS from loading heavy native Chromium binaries (`libmonochrome_64.so`) into memory.

---

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **v1.0.0**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;4 patches total
<details open>
<summary>📦 Brave Browser&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 1.93.137 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| [Block Brave Telemetry](#block-brave-telemetry) | Blocks P3A product analytics, Brave Stats usage pings, crash dump uploads, WDP, and Variations seed fetching. |
| [Brave In-Product Notification Scheduler Optimization](#brave-in-product-notification-scheduler-optimization) | Eliminates periodic background wakeups and native library loading caused by Chromium in-product tips/promo scheduler (Job ID 105). |
| [Brave Origin](#brave-origin) | Unlocks Brave Origin and enables local feature toggle controls. |
| [Brave Startup Performance Optimization](#brave-startup-performance-optimization) | Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations. |

</details>

<!-- PATCHES_END -->

---

### 🛠️ Building locally

To compile the patches bundle locally:

```bash
./gradlew buildAndroid
```

The compiled patch bundle (`.mpp`) will be generated at:
```
patches/build/libs/patches-*.mpp
```

Apply the `.mpp` file using [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) or [Morphe Manager](https://github.com/MorpheApp/morphe-manager).

---

## 📜 License

Brave Patches are licensed under the [GNU General Public License v3.0](LICENSE).
