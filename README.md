<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Target-Brave_Browser_v1.93.136-FF4500?style=for-the-badge&logo=brave&logoColor=white" />
  <img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge" />
</p>

<h1 align="center">🦁 Brave Patches</h1>

<p align="center">
  Custom Morphe patch suite for <b>Brave Browser</b> on Android — Brave Origin feature unlock, multi-layer telemetry blocking, and background/startup performance optimizations.
</p>

<p align="center">
  <a href="https://morphe.software/add-source?github=kveld9/brave-patches"><img src="https://img.shields.io/badge/Morphe_Manager-Add_Patch_Source-8A2BE2?style=for-the-badge&logo=android" /></a>
  <a href="https://github.com/kveld9/brave-patches/releases"><img src="https://img.shields.io/badge/Releases-Latest-green?style=for-the-badge&logo=github" /></a>
</p>

---

## 📖 About

This repository provides custom, community-developed bytecode, native, and resource patches for **Brave Browser** using the **[Morphe](https://morphe.software)** patcher framework.

### 📲 Add to Morphe Manager

Click the button below or add `kveld9/brave-patches` in Morphe Manager patch sources:

👉 **[Add Brave Patches to Morphe](https://morphe.software/add-source?github=kveld9/brave-patches)**

---

## ✨ Features & Patches

<table>
<tr>
<td width="50%" valign="top">

### 🔓 Brave Origin Unlock
* **Feature Policy Gatekeepers** — Unlocks Brave Origin and enables local feature controls for Rewards, News, Wallet, VPN, and Leo AI.
* **Persistent Preferences** — Saves toggle states locally in SharedPreferences.
* **Clean UI Defaults** — Enables Origin switches by default in layout XMLs.

### 🛡️ Block Brave Telemetry (Multi-Layer)
* **Resource Defaults** — Defaults P3A, Stats, and WDP switches to `false` in preferences XML.
* **Native Hardening (`libchrome.so` ARM64)** — Neutralizes P3A scheduler init, Brave Stats upload branches, and WDP search host extractor.
* **DNS / Hosts Redirection** — Redirects 10 diagnostic/telemetry domains to `0.0.0.0` (BSG, WDP, Crashpad, Variations, CR).
* **Bytecode Blocking** — Aborts Crashpad minidump uploads, Variations seed network connections, and forces PrefService telemetry checks to `false`.

</td>
<td width="50%" valign="top">

### ⚡ Startup Performance Optimization
* **Bypass Carrier / Partner Customizations** — Neutralizes `PartnerBrowserCustomizations.initializeAsync`.
* **Zero Overhead** — Eliminates main-thread SharedPreferences reads, background ThreadPool tasks, ContentResolver queries, and 10s timeout handlers during cold start.

### 🔕 In-Product Notification Scheduler Optimization
* **Neutralize Job ID 105** — Prevents Chromium C++ tips/promo scheduler from scheduling background jobs in Android `JobScheduler`.
* **Prevent Unnecessary Wakeups** — Immediately cancels existing persisted tasks to avoid background native library loading (`libmonochrome_64.so`).

</td>
</tr>
</table>

---

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0](https://github.com/kveld9/brave-patches/releases/tag/v1.0.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;4 patches total
<details open>
<summary>📦 Brave Browser&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 1.93.136 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Block Brave Telemetry](#block-brave-telemetry) | Blocks P3A product analytics, Brave Stats usage pings, crash dump uploads, WDP, and Variations seed fetching. |  |
| [Brave In-Product Notification Scheduler Optimization](#brave-in-product-notification-scheduler-optimization) | Eliminates periodic background wakeups and native library loading caused by Chromium in-product tips/promo scheduler (Job ID 105). |  |
| [Brave Origin](#brave-origin) | Unlocks Brave Origin and enables local feature toggle controls. |  |
| [Brave Startup Performance Optimization](#brave-startup-performance-optimization) | Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations. |  |

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

Apply the `.mpp` file using [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) or Morphe Manager.

---

## 📜 License

Brave Patches are licensed under the [GNU General Public License v3.0](LICENSE).
