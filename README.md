# 🦁 Brave Origin Patches

Custom Morphe patch suite for Brave Browser on Android.

## ❓ About

This repository provides patches for **Brave Browser** focused on unlocking features, privacy hardening, and performance improvements:

* **Brave Origin**: Unlocks Brave Origin features and enables local feature policy toggle controls.
* **Block Brave Telemetry**: Disables P3A product analytics, Brave Stats usage pings, crash dump uploads (Crashpad), WDP, and Variations seed network fetching.
* **Brave Startup Performance Optimization**: Eliminates background CPU and disk overhead during startup by bypassing unused OEM carrier partner customizations.
* **Brave In-Product Notification Scheduler Optimization**: Prevents periodic background wakeups and native library loading triggered by in-product tips/promo tasks (Job ID 105).

### 📲 How to use these patches in Morphe

Click the link below to add this repository directly to Morphe Manager:

👉 [Add Brave Origin Patches to Morphe](https://morphe.software/add-source?github=kveld9/brave-origin-patches)

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

You can then apply the `.mpp` file using [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) or Morphe Manager.

---

## 📜 License

Brave Origin Patches are licensed under the [GNU General Public License v3.0](LICENSE).
