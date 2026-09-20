# 🎯 Project Scope & Out-of-Scope Philosophy

This document defines the core engineering philosophy, architectural boundaries, and features that are explicitly **out of scope** for the **Morphe Patches** (`com.kveld9.morphe`) repository.

Please read this document thoroughly before submitting a feature request or opening a design discussion.

---

## 1. Core Engineering Philosophy

The design of Morphe Patches is guided by four non-negotiable principles:

1. **Surgical Compile-Time Transformations**:
   Patches operate primarily as compile-time bytecode manipulations (`bytecodePatch`), Android resource transformations (`resourcePatch`), and raw binary/ELF edits (`rawResourcePatch`). We neutralize telemetry, strip background sync daemons, and debloat assets directly at patch-time.
2. **Zero Runtime Overhead**:
   Patched applications must run with zero performance penalty. We eliminate bloat and short-circuit dead paths with `return-void` rather than introducing heavy runtime wrappers, reflection proxies, or persistent background service layers.
3. **Single Target Version Invariant**:
   Every supported application targets strictly **ONE** authoritative version: the latest supported upstream release. When an upstream app updates, the target version is bumped and older versions are retired immediately. We do not maintain legacy fallbacks or multi-version compatibility matrices.
4. **Rapid Upstream Synchronization**:
   Our lightweight, surgical patch design allows us to maintain compatibility with new upstream releases as soon as they drop. Any architectural complexity that threatens our ability to update rapidly is rejected.

---

## 2. Explicitly Out-of-Scope Features

Feature requests that fall into any of the following categories will be closed as **not planned**:

### ❌ In-App Settings Screens & Dynamic UI Panels
* **What is requested**: Injecting custom preference menus, floating overlay panels, or settings screens into target applications (e.g., TikTok, Brave, Gboard) to toggle patches dynamically on the fly.
* **Why it is out of scope**:
  1. **Extreme Fragility**: Applications like TikTok deploy aggressive ProGuard/DexGuard re-obfuscation and publish updates weekly. Hooking into obfuscated Activity, Fragment, or Navigation controller hierarchies breaks continuously across version bumps, making rapid upstream synchronization impossible.
  2. **Contradicts Zero-Overhead Debloating**: Dynamic in-app toggles require keeping unwanted features, telemetry, and background daemons intact inside the APK and wrapping every hook in runtime conditional guards (`if (isFeatureEnabled())`). This adds SharedPreferences disk I/O to performance-critical paths (video decoding, rendering, networking).
  3. **Target Heterogeneity**: The repository targets diverse runtimes—native C++ Chromium (`libchrome.so`), React Native Hermes bytecode, Android IME services, and obfuscated Java/Kotlin. There is no universal UI framework across these targets.
* **Supported Alternative**:
  Configurable patch parameters (such as SIM regions, video quality presets, clipboard retention limits, or package clone suffixes) are configured **at patch-time** via Morphe Manager / CLI options (`stringOption`). Once patched, the app runs cleanly and standalone without runtime management layers.

---

### ❌ Server-Side Bypasses, DRM, and Account Exploits
* **What is requested**: Bypassing server-side subscription paywalls, unlocking cloud-restricted content, accessing private accounts, or defeating DRM protections.
* **Why it is out of scope**:
  Patches operate exclusively on client-side bytecode and local application assets. Features enforced, calculated, or authorized on remote backend servers cannot be manipulated via client APK modification.

---

### ❌ Feature Bloat & In-App Download Managers
* **What is requested**: Embedding complex media download engines, custom UI skins, internal torrent clients, or custom media players inside host applications.
* **Why it is out of scope**:
  Morphe patches focus on unlocking or restoring native capabilities (such as enabling the native watermark-free "Save video" action in TikTok) and stripping unwanted bloat. We do not bloat target APKs with complex third-party subsystems.

---

### ❌ Legacy & Multi-Version Support
* **What is requested**: Maintaining compatibility matrices, legacy fallback code, or bug fixes for older versions of supported applications.
* **Why it is out of scope**:
  Supporting multiple versions multiplies maintenance overhead exponentially and leads to code rot. Users are expected to patch the latest supported version specified in [README.md](../README.md).

---

### ❌ Arbitrary New Target Applications
* **What is requested**: Adding new target applications without architectural alignment or maintainability rationale.
* **Why it is out of scope**:
  Every supported application requires ongoing maintenance, reverse-engineering harnesses, and automated testing pipelines. New targets are added only when they offer clear utility, clean patch boundaries, and sustainable long-term maintenance.
