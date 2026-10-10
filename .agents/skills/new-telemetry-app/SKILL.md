---
name: new-telemetry-app
description: End-to-end workflow for onboarding a new target app and authoring telemetry-blocking patches: reconnaissance, registration, patch authoring, verification gates, device smoke test, and network traffic auditing.
---

<!-- Canonical shared copy: byte-identical file in kveld-extra-morphe-patches and brave-origin-patches (.agents/skills, hardlinked to .claude/skills inside each repo). Keep copies byte-identical; propagate edits with scripts/sync_shared_skills.sh. Repo-specific deltas live in section 0 below, never in forked copies. -->

# New Target App Onboarding & Telemetry Hardening

## 0. Repository Adaptation Notes (Shared Canonical Copy)

- **Sync invariant**: this file is canonical and byte-identical in both sibling repos. Edit in either repo, then run `scripts/sync_shared_skills.sh` (or `--check` in CI) to propagate. Never create per-repo forks.
- **README**: update only the hand-maintained Supported Targets table above `PATCHES_START`/`PATCHES_END`. Never edit the generated Patch Catalog block, `patches-list.json`, `patches-bundle.json`, or `CHANGELOG.md`; the release pipeline regenerates them. Verify catalog registration from a temporary directory outside the checkout; never run `generatePatchesList` in the checkout.
- **Registry**: register the app in `util/PatchExecutionTest.kt` (`id`, `packageName` from `Constants.kt`, `candidateFilenames`, `filePattern`, `patchDirectoryPart`). Known divergence: id `xiaomi_earbuds` uses `patchDirectoryPart` `xiaomi` and guide `docs/apps/xiaomi-earbuds.md`.
- **Extension wiring**: in kveld-extra link companion runtime via `dependsOn(sharedExtensionPatch)` (direct `extendWith` calls cost redundant `ClassMerger` passes); in brave-origin declare `extendWith("extensions/extension.mpe")` per its `AGENTS.md`. Follow the local repo convention.
- **Universal boundary**: dedicated telemetry suites are a precision superset; never stack a generic telemetry neutralizer on maintained apps. Generic debloat universals (locale/dpi) are unaffected.
- **Commits**: one atomic Conventional Commit per unit (`feat(<app>):` / `fix(<app>):`), direct commit, never push without explicit request. brave-origin develops on `dev` (`main` is the release line) and requires a green `audit-stack` over `origin/dev..HEAD` before any push.

## 1. Scope & Non-Goals

### A. In-Scope Objectives
- Surgical neutralization of tracking, analytics, crash-reporting, attribution, and advertising SDKs for strictly ONE new target application version.
- Multi-tier defense-in-depth suppression:
  1. Manifest-level entrypoints: advertising permissions, analytics background services, install referrer providers, broadcast receivers, and startup initializers.
  2. Declarative SDK opt-out flags: injecting disable flags directly into application `<meta-data>`.
  3. Dalvik bytecode dispatchers: stubbing telemetry loggers, dispatchers, and SDK initialization methods with zero runtime overhead.
  4. Native binary endpoints (where applicable): in-situ redirection of hardcoded native telemetry hosts in `.so` libraries to `0.0.0.0`.
- Neutralization of client-side post-patch compatibility blockers and functioning bypasses: local signature and integrity checks, involuntary store update redirects, sharedUserId conflicts, forced login requirements blocking local utility, and client-side feature/paywall gates.

### B. Explicit Non-Goals & Architectural Boundaries
- **Preserve Core Networking**: Never remove `android.permission.INTERNET` or sever primary application network channels.
- **Preserve Essential App Features**: Never break push notifications (FCM/GCM registration and receivers), deep links, account authentication, session renewal, media playback, or primary user interactions unless explicitly requested by the user.
- **Strict Prohibition of In-App Settings Screens**:
  - Never inject preference screens, settings activities, floating overlays, or dynamic runtime toggles into target applications.
  - *Rationale*: Dynamic UI panels introduce extreme fragility across weekly upstream obfuscation shifts and add disk I/O on performance-critical paths (authoritative boundary: `docs/out-of-scope.md`).
  - *Controlled Exception (Gboard Lite)*: Gboard Lite is the sole exception where in-app settings are permitted because it exposes standard AndroidX `PreferenceScreen` XML resources and standard IME settings activities.
  - *Standard for All Other Targets*: All configurable parameters must be compile-time / patch-time options via Morphe Manager / CLI (`stringOption`, `booleanOption`).
- **Strict Prohibition of Server-Side Bypasses & DRM**:
  - Never attempt to bypass server-side subscription paywalls, unlock cloud-restricted content, access private accounts, or defeat DRM protections (authoritative boundary: `docs/out-of-scope.md`). Patches operate strictly on client-side bytecode and local assets.
- **Controlled Scope & Client-Side Verification Test**:
  - Any client-side compatibility or entitlement patch must pass the three-question verification test before implementation:
    1. Is the final decision executed locally by the APK's own bytecode or assets?
    2. Does the complete user flow execute entirely on-device?
    3. Does the patch operate without forging network credentials, session tokens, purchase receipts, or remote attestations?
  - **Three-Yes Invariant**: All three questions MUST answer YES. If any question is NO, the patch is strictly out of scope and must not be written.
  - **Truthful Documentation Invariant**: If backend servers continue to reject, restrict, or deny the feature or cloud data, NEVER document or describe the patch as an "unlock" or "subscription bypass". Document it strictly as a client-side UI suppression or local feature gate bypass.
  - **Social & Messaging Guest-Mode Prohibition**: Guest-mode or login-bypass patches are strictly prohibited on social networks and messaging platforms (e.g. Discord, Twitter/X) where client-side sessions are intrinsically tied to server state. When implementing guest mode in permitted local utilities, implement mandatory null-identity guards to prevent crashes.
  - **Default Setting Policy**: Set `default = true` ONLY when the complete user flow is verified on a physical device. Any doubt or lack of hardware validation requires `default = false` (opt-in).
- **Strict Prohibition of Feature Bloat & Download Managers**:
  - Never embed third-party media download engines, torrent clients, or custom UI skins inside host applications.
- **Single Target Version Invariant**:
  - Every target application must target strictly ONE active upstream version.
  - When upstream ships distinct 32-bit and 64-bit builds (e.g. `armeabi-v7a` and `arm64-v8a`), configure a SINGLE `AppTarget` entry for that upstream version with `versionCodes = mapOf(...)` keyed by ABI variant when applicable in `targets = listOf(...)`.
  - Never retain legacy fallback code or multi-version compatibility matrices for older versions. When upstream updates, bump the target version and retire the old version immediately.

---

## 2. Architecture & Compatibility Policy

### A. CPU Architecture Support Matrix
- **`arm64-v8a` (Primary / First-Class)**: Standard 64-bit target for all modern Android devices. All patches, native transforms, and validation runs must target `arm64-v8a` first.
- **`armeabi-v7a` (Legacy 32-bit)**: Supported only when the application provides official 32-bit builds and patches operate strictly on Dalvik bytecode/resources without 64-bit-exclusive native dependencies.
- **`x86` / `x86_64` (Out of Scope)**: Desktop emulator architectures are not supported.

### B. APK Variant Selection
- **Standalone nodpi APK**: Preferred target format whenever available upstream.
- **Split APK Bundles (`.apkm` / `.xapk`)**: Used when upstream distributes only split bundles. Morphe Patcher and the test runner fuse split DEXes and assets into a unified base APK during patching.
- Target compatibility must declare the exact format: `apkFileType = ApkFileType.APK`, `ApkFileType.APKM`, or `ApkFileType.XAPK`.

### C. Universal vs Dedicated Patch Boundary
- **Precision Superset Rule**: Dedicated application patches are a precision superset for their respective targets.
- **Prohibition on Stacking Universals**: Never stack `Universal Telemetry Neutralizer` or `Universal SDK Blocker` on maintained apps. Stacking adds execution time, runs expensive full-DEX scans, and risks breaking features like push notifications or login if generic toggles collide.
- If a newly discovered generic SDK appears in an onboarded app, author the rule directly inside that app's dedicated patch suite.

### D. Core Patch Typologies
- **`bytecodePatch`**: High-level Dalvik AST transforms via dexlib2 fingerprints and instruction injection (`addInstructions(0, "return-void")`).
- **`resourcePatch`**: XML DOM manipulation (`AndroidManifest.xml`, `res/xml/*.xml`) executed prior to DEX assembly.
- **`rawResourcePatch`**: Deterministic byte-level ELF string redirection, companion `.so` zeroing, or asset binary patching.
- Universal patches are ordinary patches that omit `compatibleWith(...)`, making them applicable across any target APK without app-specific obfuscation dependencies. There is no special `universalPatch` typology in the patcher engine.

---

## 3. Recon Procedure

### A. Artifact Placement
Place candidate APK, APKM, or XAPK files into `candidate_apks/` (the Morphe patch runner specifically resolves `candidate_apks/` when running without an explicit `-Papk` parameter). Never commit raw binary APKs to version control.

### B. Badging & Version Metadata Extraction
Extract target package name, version name, version code, and SDK requirements:

```bash
# Derive aapt2 dynamically from local.properties sdk.dir if not in PATH:
AAPT2="$(grep '^sdk.dir=' local.properties | cut -d= -f2)/build-tools/$(ls $(grep '^sdk.dir=' local.properties | cut -d= -f2)/build-tools | sort -V | tail -n 1)/aapt2"
"${AAPT2:-aapt2}" dump badging candidate_apks/<app>.apk | grep -E "(package: name=|versionCode=|versionName=|sdkVersion:)"
```

Record the following metadata:
- Package name (e.g. `com.example.android`)
- Version name (e.g. `1.2.3.4`)
- Version code (e.g. `12345678`)
- Minimum SDK (e.g. `26`)
- Supported ABIs (e.g. `arm64-v8a`)

### C. Manifest XML Tree Dump
Dump the decoded manifest tree to a scratch path outside git tracking:

```bash
mkdir -p scratch
"${AAPT2:-aapt2}" dump xmltree --file AndroidManifest.xml candidate_apks/<app>.apk > scratch/manifest_tree.txt
```

### D. Telemetry Component Inventory
Grep the manifest tree for tracking, advertising, and diagnostic entries:

1. **Permissions (`uses-permission`)**:
   - Advertising ID: `com.google.android.gms.permission.AD_ID`
   - Privacy Sandbox / AdServices:
     - `android.permission.ACCESS_ADSERVICES_ATTRIBUTION`
     - `android.permission.ACCESS_ADSERVICES_AD_ID`
     - `android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE`
     - `android.permission.ACCESS_ADSERVICES_TOPICS`
   - Install Referrer: `com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE`
   - AppHub / Partner services: `com.applovin.array.apphub.permission.BIND_APPHUB_SERVICE`

2. **Components (`service`, `receiver`, `provider`, `activity`)**:
   - Facebook Analytics: `com.facebook.analytics2.*` (upload services, alarm receivers)
   - Google Measurement / Firebase: `com.google.android.gms.measurement.*`, `com.google.android.gms.analytics.*`
   - Google DataTransport: `com.google.android.datatransport.runtime.*`
   - Device & Install Identifiers: `*FDIDLiteProvider`, `*PhoneIdProvider`, `*InstallReferrerProvider`
   - Firebase Component Discovery: `com.google.firebase.components.ComponentDiscoveryService`
   - MLKit Discovery: `com.google.mlkit.common.internal.MlKitComponentDiscoveryService`

3. **Startup Initializers (`androidx.startup.InitializationProvider`)**:
   - Ad SDK initializers: `com.unity3d.services.core.configuration.AdsSdkInitializer`, `com.google.android.gms.ads.MobileAdsInitProvider`

### E. ML Kit Analysis & Scanner False-Positive Rule
Component scanners (App Manager, Exodus Privacy) frequently flag Google ML Kit components (`MlKitInitProvider`, `MlKitComponentDiscoveryService`) as "trackers" because ML Kit uses the Firebase dependency injection framework (`CommonComponentRegistrar`).
- **Empirical Reality**: ML Kit models (OCR, barcode scanning, Autofill vision) execute strictly locally on-device and send zero telemetry.
- **Rule**: Never strip ML Kit components in default telemetry patches. Disabling ML Kit breaks in-app barcode and QR scanning. If an ML Kit slimmer is desired, author it as a separate opt-in patch with `default = false` and an explicit description warning.

### F. Native Binary Reconnaissance (`lib/<abi>/*.so`)
When the target application bundles native libraries:
1. Inspect bundled native libraries:
   ```bash
   unzip -l candidate_apks/<app>.apk "lib/*"
   ```
2. Scan for embedded telemetry endpoints in native binaries:
   ```bash
   strings -a lib/arm64-v8a/libnative.so | grep -E "(telemetry|analytics|crash|stats|log|metrics)"
   ```
3. Check for standalone crash reporter or profiler `.so` files suitable for companion bloat zeroing (e.g. `libcrashlytics.so`, `libsentry.so`, `libgwp-asan.so`).

When maintaining native binary host redirections across application updates, enforce a structured native ELF audit contract generalized from single-app scanners. Maintain a catalog of known telemetry endpoints with expected occurrence counts per target ABI (e.g. `arm64-v8a`, `armeabi-v7a`). During an update audit, classify findings into offset-change (the domain string persists but shifted position in `.rodata`), vanished (the string was removed or refactored upstream, requiring hook pruning to avoid false assertion failures), and new-candidate discovery (strings matching domain heuristics such as `*telemetry*`, `*collector*`, `*crash*`, `*metrics*` that emerged in the new build). All ELF audit reports and candidate offset tables must be written to `scratch/` (e.g. `scratch/elf_audit_<app>.txt`) and never committed to version control.

### G. Bytecode Scan for Stable SDK Signatures
Scan DEX files with `androguard` to locate stable SDK entrypoints and verify exact Smali descriptors:

```python
# Command run via python virtual environment
./venv/bin/python -c '
from androguard.core.apk import APK
from androguard.core.dex import DEX
apk = APK("candidate_apks/<app>.apk")
for dex_bytes in apk.get_all_dex():
    dex = DEX(dex_bytes)
    for cls in dex.get_classes():
        name = cls.get_name()
        if "analytics" in name.lower() or "measurement" in name.lower():
            for m in cls.get_methods():
                print(f"{name}->{m.get_name()}{m.get_descriptor()}")
' > scratch/sdk_methods.txt
```

Identify stable SDK dispatch points:
- `FirebaseAnalytics.logEvent`: `(Ljava/lang/String; Landroid/os/Bundle;)V` or obfuscated equivalent
- `AppMeasurement.logEventInternal`: `(Ljava/lang/String; Ljava/lang/String; Landroid/os/Bundle;)V`
- App-specific uploader dispatchers: e.g. `IgAnalytics2TaskBasedUploader.HZG` in Instagram

Record exact Smali descriptors:
- Defining class: `Lcom/target/Uploader;`
- Method name: `dispatch`
- Parameter types: `listOf("Ljava/lang/String;", "Landroid/os/Bundle;")`
- Return type: `V` (void) or `Z` (boolean)

### H. Triage Tooling
When signatures shift or cannot be identified via simple grep:
- **`jadx-gui` (Static Triage)**: Open APK in jadx, follow Xrefs from string constants or log messages, and identify the shifted method signature.
- **`frida` / `jnitrace` (Dynamic Lab Triage)**: Attach to the target process on the lab device to confirm whether candidate methods execute during user interactions before writing hooks.

Always store intermediate recon dumps in `scratch/` (ensuring `mkdir -p scratch` first) or `<appDataDir>/brain/<conversation-id>/scratch/`. Never commit raw scan outputs.

### I. Compatibility Blocker Reconnaissance
After analyzing telemetry, investigate client-side post-patch compatibility blockers and functioning gates that prevent the modified application from installing, launching, or operating properly:

1. **Manifest-Level Blocker Scan (`aapt2 dump xmltree`)**:
   - Dump the manifest as shown in Section 3.C and inspect for:
     - Shared User ID conflicts: `android:sharedUserId` (triggers `INSTALL_FAILED_SHARED_USER_INCOMPATIBLE` on modern Android distributions).
     - Store redirect and update activities: activities launching store intents or orphan update services (e.g. `AppUpdateActivity`, `MarketRedirectActivity`).
     - Orphan billing activities: vendor store billing components that fail without host OEM stores. *Rule*: Never disable `com.android.billingclient.api.ProxyBillingActivity` or `ProxyBillingActivityV2`.
     - Intent filter handlers: confirm `android.intent.action.VIEW` filters for web schemes and OAuth redirects remain intact.

2. **Bytecode-Level Gate & Integrity Scan (DEX Scan & jadx)**:
   - Scan DEX bytecodes (using the method scanner in Section 3.G or jadx) for:
     - **Signature & Anti-Tamper Verification**: APK signature hash checks, package manager signature comparisons, emulator/root/Xposed detection classes, and code transparency callback interfaces (`CodeTransparencyCheckCallback`).
     - **Involuntary Store & Market Redirects**: String constants `market://details?id=`, `play.google.com/store/apps/details`, or store intent builders executed on launch or repackage detection.
     - **Mandatory Login & FTUX Gates**: First-Time User Experience (FTUX) wizards or compulsory login listeners that block offline or standalone utility functions.
     - **Client-Side Paywalls & Local Feature Gates**: Boolean getters (`isPro`, `isPaying`, `isSubscribed`, `hasFamilyPlan`), licensing state enums (`LicensingState`), or local paywall activities (`BlockPaywallActivity`).

### J. Behavioral Privacy Recon Checklist (Non-SDK Hardening)
Standard telemetry reconnaissance (Exodus signatures, manifest components, and uploader dispatchers) does not discover first-party behavioral tracking or invasive client features that operate outside commercial SDKs. During application onboarding, audit the decompiled application for the following non-SDK privacy hardening candidates:
- **Sensors & Content-Capture Indicators**: Camera, microphone, and ambient sensor background listeners, motion sensors, screen capture callbacks, or visual content analysis hooks.
- **In-App Browser Guards**: WebViews that inject custom JavaScript bridges, track external link browsing history, or override third-party cookie/storage isolation.
- **Share-URL Tracking Parameters**: Outgoing share intent builders that append tracking identifiers (e.g. `utm_*`, `igshid`, `si`, `fbclid`, or custom attribution query parameters) to shared URLs.
- **Clipboard Access**: Background or unexpected foreground reads from `ClipboardManager` on launch or input focus.
- **GMS / Phenotype Decoupling Candidates**: Hard couplings to Google Play Services Experiment/Phenotype configuration flags that force remote feature rollouts or telemetry overrides.
- **Local History Recording**: Internal SQLite/Room databases or SharedPreferences storing local watch, search, browse, or query histories without user consent.
- **Personalized Search & Feed Algorithms**: Client-side ranking models, behavioral interaction counters, and recommendation logging.
- **P2P Relay & Mesh Networking**: Background peer-to-peer data sharing, local network discovery (mDNS, SSDP), or distributed caching services.
- **On-Device AI Governors**: On-device machine learning models indexing local user data, photos, audio, or text for client-side profiling.
- **Incognito & Private Input Modes**: Keyboard/IME flags, voice typing transmission, or lack of `IME_FLAG_NO_PERSONALIZED_LEARNING` when handling sensitive text fields.

*Scope Invariant*: These behavioral privacy vectors are NOT discovered by the automated Exodus-plus-dispatchers model. Each identified vector represents a separate, dedicated privacy hardening item (often requiring its own independent opt-in patch) and must NEVER be bundled into the primary telemetry suppression patch.

---

## 4. Registration & Documentation Deliverables

Onboarding a new target app requires synchronized deliverables, all committed in the SAME atomic commit as the app's first patch:
1. `Constants.kt` entry inside `object Constants { ... }`
2. `PatchExecutionTest.kt` entry in `TargetApp`
3. `README.md` Supported Apps table update (adhering strictly to repository-specific column schema)
4. `docs/apps/<app_id>.md` comprehensive application guide (including mandatory Behavioral Hazards Warning)
5. `docs/compatibility.md` synchronization when that file exists in the repository

### A. Centralized Constants (`Constants.kt`)
File: `patches/src/main/kotlin/app/morphe/patches/shared/Constants.kt`

Add package name, target version, and `Compatibility` contract inside `object Constants`:

```kotlin
object Constants {
    // ... existing constants ...

    const val <APP>_PACKAGE_NAME = "com.example.android"
    const val <APP>_TARGET_VERSION = "1.2.3.4"

    val COMPATIBILITY_<APP> = Compatibility(
        name = "<App Name>",
        packageName = <APP>_PACKAGE_NAME,
        apkFileType = ApkFileType.APK, // Use ApkFileType.APKM or ApkFileType.XAPK for bundles
        appIconColor = 0x123456,
        targets = listOf(
            AppTarget(
                version = <APP>_TARGET_VERSION,
                description = "Download com.example.android v$<APP>_TARGET_VERSION (APK) from APKMirror",
            )
        )
    )
}
```

**Single Version / ABI Variant Rule**: Maintain strictly ONE upstream version. If upstream ships separate 32-bit and 64-bit APKs, declare a single `AppTarget` entry with `versionCodes = mapOf(...)` keyed by ABI variant under that upstream version.

### B. Runner Registry (`PatchExecutionTest.kt`) & Mapping Invariant
File: `patches/src/main/kotlin/util/PatchExecutionTest.kt`

Every target app establishes a 1:1:1:1 mapping:
- `TargetApp.id`: CLI/runner identifier (e.g. `instagram`, `xiaomi_earbuds`)
- `TargetApp.patchDirectoryPart`: Subdirectory under `patches/src/main/kotlin/app/morphe/patches/` (usually matches `id`, but can diverge, e.g. `xiaomi_earbuds` id pointing to `xiaomi` directory)
- Guide filename: `docs/apps/<id>.md`
- README table row: Target entry matching the target repo's table columns

Add the target to `enum class TargetApp`:

```kotlin
<APP>(
    id = "<app_id>",
    appName = "<App Name>",
    packageName = Constants.<APP>_PACKAGE_NAME,
    candidateFilenames = listOf(
        "<app>_${Constants.<APP>_TARGET_VERSION}.apk",
        "com.example.android_${Constants.<APP>_TARGET_VERSION}.apkm",
    ),
    filePattern = Regex("(?i).*<app_id>.*\\.(?:apk|apkm|xapk)$"),
    patchDirectoryPart = "<app_id>", // e.g. "xiaomi" when id is "xiaomi_earbuds"
),
```

### C. Supported Apps Table (`README.md`)
File: `README.md`

Follow the repository's existing Supported Apps table schema (6 columns):
`| App | Package | Target Version | Variant | Download Source | Guide |`

Example for this repository:
```markdown
| <App Name> | `<package_name>` | <target_version> | APKM bundle (`arm64-v8a`) | [APKMirror](url) | [<App Name> Guide](docs/apps/<app_id>.md) |
```

### D. Application Guide Deliverable (`docs/apps/<app_id>.md`)
File: `docs/apps/<app_id>.md`

Every new application onboarding MUST produce a dedicated guide with this exact structure, including a mandatory **Behavioral Hazards & Warnings** section:

```markdown
# <App Name>: Complete Patch & Architecture Guide

Comprehensive technical, architecture, and patch guide for **<App Name>** (`<package_name>`), covering target requirements, telemetry neutralization, and architectural invariants.

---

## Target & Compatibility

| Property | Value |
| :--- | :--- |
| **Target Application** | <App Name> |
| **Package Name** | `<package_name>` |
| **Supported Target Version** | **`<target_version>`** |
| **Target File Format** | Standalone APK (`APK`) or Bundle (`APKM`/`XAPK`) |
| **Recommended Architecture** | `arm64-v8a` |
| **Official Download Source** | [APKMirror / APKPure](url) |

---

## Applied Patches Catalog

| Patch Name | Type | Category | Default | Primary Mechanism |
| :--- | :--- | :--- | :---: | :--- |
| **Block Telemetry & Trackers** | `bytecodePatch` + `resourcePatch` | Privacy & Telemetry | Yes | Strips advertising permissions, disables analytics services/providers in AndroidManifest.xml, and stubs Dalvik telemetry dispatchers. |

---

## Behavioral Hazards & Warnings

> [!WARNING]
> Document critical user-facing trade-offs, potential upstream breaking points, preserved services (e.g. push tokens, billing), or components that must NOT be removed (e.g. MLKit breakage for QR/barcodes).
> - **Client-Side vs Server-Side Entitlements**: Client-side gates, local license checks, or login requirement screens are neutralized locally on-device. No server-side subscriptions or cloud entitlements are granted or claimed; cloud-synced features and server-backed assets remain subject to backend authorization.

---

## Deep Technical Patch Breakdown

### 1. Block Telemetry & Trackers (`<appId>BlockTelemetryPatch`)
- **Objective**: Neutralize tracking SDKs, analytics dispatchers, and advertising permissions.
- **Manifest Purge**: Strips `AD_ID` and advertising permissions, disables measurement and analytics components, and removes discovery registrars.
- **Bytecode Hooks**: Stubs telemetry dispatch methods with early `return-void`.
```

### E. Package Directory & Extension Payloads
Create the target package directory:
`patches/src/main/kotlin/app/morphe/patches/<patchDirectoryPart>/`

**Extension Payload Invariant**: When bytecode hooks require companion Java/Kotlin runtime logic, always link via `dependsOn(sharedExtensionPatch)` rather than calling `extendWith("extensions/extension.mpe")` directly. Direct calls invoke redundant `ClassMerger` passes and heavily degrade build performance.

**Release Catalog Protection**: Never execute `./gradlew generatePatchesList` in the repository checkout: it rewrites the tracked `patches-list.json`, which is managed strictly by the CI release pipeline.

---

## 5. Implementation Skill Delegation

Telemetry blocking and asset debloating implementations are partitioned into dedicated specialized skills. Do not duplicate implementation logic or code snippets across skill definitions.

### A. Telemetry Blocking Implementation (`telemetry-blocking`)
- **When to Invoke**: Invoke when implementing the primary telemetry suppression suite for a target application, including manifest-level permission stripping, component disabling, opt-out metadata injection, and Dalvik bytecode dispatcher stubs.
- **What It Delivers**:
  - Manifest purge implementation using shared helpers in `app.morphe.patches.shared.ManifestXml.kt`: `stripPermissionsWhere`, `disableComponentsWhere`, `disableComponentsByName`, and `setApplicationMetaData`.
  - Bytecode stubbing via `app.morphe.patches.shared.BytecodeUtils.kt`: `replaceWithReturnVoid()`, `replaceWithReturnBoolean(value)`, and `clearTryBlocks()` to eliminate dangling exception blocks that trigger Dalvik/ART `VerifyError`.
  - Direction-inversion trap awareness: gatekeeper methods named `isTrackingDisabled`, `isLimitAdTrackingEnabled`, or `areNotificationsOptedOut` must return `true` (0x1) to disable tracking, whereas methods like `isAnalyticsEnabled` must return `false` (0x0).
  - Bytecode return type validation: assert `returnType != "V"` before attempting return-value injections, and verify method signature return types.
  - Component discovery registrar pruning patterns and the ML Kit scanner false-positive caveat.
  - Reverse traversal multi-return hook insertion and register stability invariants.
  - The zero-zombie fingerprint contract and `[Patch Name]` diagnostic telemetry logging standard.
  - Verification assertions against the compiled APK manifest tree via `aapt2`.
  - Reference: `.agents/skills/telemetry-blocking/SKILL.md`.
  - DEX hosts-rewrite follows the telemetry-blocking hosts-rewrite pattern, and all stub selections must pass its mandatory exclusion list.

### B. Application Debloating & Asset Slimming (`app-debloat`)
- **When to Invoke**: Invoke when analyzing or stripping companion native libraries, non-essential asset bundles, multi-language string tables, high-density screen graphics, onboarding videos, editor assets, or background sync schedulers.
- **What It Delivers**:
  - Native binary in-situ zeroing (`file.writeBytes(byteArrayOf())`) for companion AI, VPN, XR, and crash-reporting shared libraries.
  - Localization slimming for Android `res/values-*/` and Chromium DataPack v5 `assets/locales/*.pak` with safe fallback preservation.
  - Screen density (`drawable-*dpi`, `mipmap-*dpi`) and non-phone UI mode trimming with launcher icon protection and orphan asset preservation.
  - Replacement of heavy onboarding media and editor stickers with minimal container headers and transparent PNG stubs.
  - Background wakeup and periodic sync elimination via `BackgroundSyncPurgePatch` patterns.
  - Guidelines for structuring one independent opt-in patch per debloat axis (`default = false`).
  - Reference: `.agents/skills/app-debloat/SKILL.md`.

### C. Compatibility and Functioning Bypass (`compat-bypass`)
- **When to Invoke**: Invoke when the patched application fails to install, launch, or function properly due to client-side integrity/signature checks, mandatory login requirements blocking local utility, involuntary store redirects, sharedUserId conflicts, or client-side feature/paywall gates.
- **What It Delivers**:
  - Local signature and integrity check neutralization patterns (anti-tamper bypass, code transparency callback stubs, signature spoofing to Google Play Services).
  - Involuntary store-redirect killer patterns without affecting standard `VIEW` deep links or OAuth browser flows.
  - Client-side paywall and local feature gate neutralization patterns (boolean flags, licensing state enums, FTUX dismissals).
  - Conditional guest-mode and login bypass patterns for standalone utilities, with strict exclusion of social and messaging applications (e.g. Discord, Twitter/X) and mandatory null-identity crash guards.
  - Manifest compatibility fixes (stripping `android:sharedUserId`).
  - Strict preservation invariants: never disable `ProxyBillingActivity`, push notifications, or `android.permission.INTERNET`.
  - Default policy guidance: `default = true` strictly requires physical device verification of the full user flow; any doubt or unverified state defaults to `false` (opt-in).
  - Reference: `.agents/skills/compat-bypass/SKILL.md`.

---

## 6. Verification Gates

### A. Full-Suite In-Situ Patching Gate
Execute Morphe Patcher against the target APK with all patches active:

```bash
# Execute patch test by registered target app id and candidate APK
./gradlew runPatchTest -Papp=<app_id> -Papk=candidate_apks/<app>.apk

# Force all boolean patch options on (covers opt-in toggles)
./gradlew runPatchTest -Papp=<app_id> -Papk=candidate_apks/<app>.apk -PallOptions=true

# Byte-identical DEX proof for performance or refactor changes
./gradlew runPatchTest -Papp=<app_id> -Papk=candidate_apks/<app>.apk -PdexDigest=build/digest-before.txt
```

#### Quiet Gate Invocation (Agent Standard)
Always redirect full output to a git-ignored file in `build/` and inspect the verdict:
```bash
./gradlew runPatchTest -Papp=<app_id> -Papk=candidate_apks/<app>.apk -PallOptions=true --console=plain > build/patchtest-<app_id>.log 2>&1; rc=$?; sed -n '/FINAL PATCHING RESULT/,$p' build/patchtest-<app_id>.log | grep -v '^\s*at ' || tail -40 build/patchtest-<app_id>.log; echo "exit=$rc"
```

#### Gate Pass Criteria
- Patcher exit code: `0`
- Failed patches: `0`
- Detected Fingerprint Failures: `0`
- Detected Smali Compile Errors: `0`
- Exceptions: `0`

### B. Lint, Unit Tests and Compiler Checks
Run standard project checks and harness unit tests:

```bash
# Unit tests for RE update harness
./venv/bin/python -m unittest discover harness/tests

# Static lint and compilation check (quiet to build/)
./gradlew check --console=plain > build/check.log 2>&1; rc=$?; tail -25 build/check.log; echo "exit=$rc"
```

### C. Injection Proof via Output APK Inspection
Verify modifications on the actual output APK artifacts rather than trusting stdout counters alone:

```bash
# Generate signed and aligned output APK
./gradlew runPatchTest -Papp=<app_id> -Papk=candidate_apks/<app>.apk -Pout=build/test-<app_id>.apk
```

*Note*: `-Pout` specifies the output APK path. Without `-Pout`, the test runner will not write the output APK to disk. For APKM/XAPK split targets, the runner writes the base APK to `build/test-<app_id>.apk` and separate signed splits to `build/test-<app_id>.<split>.apk`.

#### Verification Commands
```bash
AAPT2="$(grep '^sdk.dir=' local.properties | cut -d= -f2)/build-tools/$(ls $(grep '^sdk.dir=' local.properties | cut -d= -f2)/build-tools | sort -V | tail -n 1)/aapt2"

# 1. Verify stripped permissions in output APK
"${AAPT2:-aapt2}" dump xmltree --file AndroidManifest.xml build/test-<app_id>.apk | grep -E "E: uses-permission.*android:name.*(AD_ID|ACCESS_ADSERVICES)"

# 2. Verify disabled components have android:enabled="false" (0x0)
"${AAPT2:-aapt2}" dump xmltree --file AndroidManifest.xml build/test-<app_id>.apk | grep -B 2 -A 5 "AppMeasurementService"

# 3. Verify injected opt-out metadata
"${AAPT2:-aapt2}" dump xmltree --file AndroidManifest.xml build/test-<app_id>.apk | grep -B 2 -A 5 "firebase_analytics_collection_enabled"
```

Assert that:
- Blocked permissions are completely absent from the dump.
- Blocked components contain `A: android:enabled(0x0101000e)=0x0`.
- Injected metadata flags are present.

### D. Reverse Engineering Harness Usage (Version Bumps)
For target applications integrated with the automated update harness (`harness/update.py`):
```bash
# Run preflight environment check
./venv/bin/python harness/update.py --doctor

# Non-destructive audit of a new target APK version
./venv/bin/python harness/update.py candidate_apks/<new-apk> --audit
```
*Note*: Applications without a dedicated harness pipeline are audited manually and validated via `./gradlew runPatchTest -Papp=<app_id>`.

---

## 7. Device Validation & Traffic Audit

### A. Automated ADB Smoke Install Gate
Use `validation/smoke_install.py` against a connected Android device matching the target ABI (`arm64-v8a`) and minimum SDK. Install the generated APK(s) in `build/`: for standalone APKs, pass the single `build/test-<app_id>.apk`; for APKM/XAPK targets where split APKs were produced (`build/test-<app_id>.<split>.apk`), pass both base and all generated splits (`build/test-<app_id>*.apk`) so `smoke_install.py` performs an `install-multiple`:

```bash
# Single standalone APK or base plus splits
./venv/bin/python validation/smoke_install.py build/test-<app_id>.apk --uninstall-on-conflict

# Multiple APKs (base + splits for bundle targets)
./venv/bin/python validation/smoke_install.py build/test-<app_id>*.apk --uninstall-on-conflict

# Explicit device serial if multiple devices are attached
./venv/bin/python validation/smoke_install.py build/test-<app_id>*.apk --serial <serial> --uninstall-on-conflict
```

#### Post-Test Cleanup
Uninstall the test package to leave the device in a clean state:

```bash
adb -s <serial> uninstall <package_name>
```

### B. Physical Device Comparative Validation
For non-trivial telemetry, background sync, and compatibility bypass modifications, perform a comparative run (Vanilla vs Patched) on the physical lab device:
1. Run vanilla APK: exercise user flows, measure background jobs and telemetry dispatch, and record default gate/login/license behavior.
2. Run patched APK: confirm telemetry endpoints remain silent, bypassed gates function as intended, and the application functions with zero crashes.
3. **State Persistence & Cold Restart Verification**: For compatibility and bypass modifications, verify that bypassed gates, local entitlements, or guest session states persist across:
   - Cold app restarts (`am force-stop` followed by launcher restart).
   - Task eviction (swiping away from recents).
   - Cache clearance and device reboot.
   Assert that the app does not regress to a locked state, show unhandled `NullPointerException`, or attempt invalid background synchronizations upon reopening.

### C. Runtime Traffic Audit (Optional Tooling)
Runtime packet capture audits require `tcpdump` on the device and optional analysis tools (`tshark`). Note that `tcpdump` and `tshark` are optional tools that may not be available in standard PATH environments.

```bash
# 1. Verify tcpdump binary exists on device first
adb -s <serial> shell "which tcpdump || ls /data/local/tmp/tcpdump" || { echo "tcpdump not found on device; skipping traffic capture"; exit 0; }

# 2. Start packet capture on device with packet limit or bound to app UID/IPs
adb -s <serial> shell su -c "tcpdump -i any -s 0 -c 1000 -w /data/local/tmp/traffic.pcap" &
TCPDUMP_PID=$!

# 3. Launch patched app and exercise standard user flows
adb -s <serial> shell monkey -p <package_name> -c android.intent.category.LAUNCHER 1
sleep 30

# 4. Stop capture with specific process pattern and pull pcap
adb -s <serial> shell su -c "pkill -f 'tcpdump -i any'"
adb -s <serial> pull /data/local/tmp/traffic.pcap scratch/traffic.pcap
adb -s <serial> shell su -c "rm -f /data/local/tmp/traffic.pcap"

# 5. Assert pcap contains packets (>0 bytes and non-empty). Never report "silent telemetry" if capture was empty!
if [ ! -s scratch/traffic.pcap ]; then
    echo "ERROR: Packet capture empty or missing. Aborting traffic analysis."
fi
```

**Never Commit PCAPs**: `.pcap` capture files must stay in `scratch/` or git-ignored paths and must never be committed.

#### Protocol & Host Inventory Extraction (when tshark is available)
```bash
if command -v tshark >/dev/null 2>&1; then
    # Extract TLS Server Name Indication (SNI) hostnames
    tshark -r scratch/traffic.pcap -Y "tls.handshake.type == 1" -T fields -e tls.handshake.extensions_server_name | sort -u

    # Extract QUIC Initial packet server names (HTTP/3 traffic)
    tshark -r scratch/traffic.pcap -Y "quic" -T fields -e quic.tls.handshake.extensions_server_name | sort -u
fi
```

#### Traffic Audit Caveats & Blind Spots
When reporting network audit results, always document technical blind spots:
1. **Empty Capture Hazard**: Never interpret an empty pcap file or failed capture run as proof that telemetry is silent. Abort report if capture failed.
2. **QUIC / HTTP/3 Traffic**: UDP port 443 traffic bypasses standard HTTP/HTTPS forward proxies unless UDP 443 is blocked or intercepted at the firewall. Inspect QUIC Initial frames directly.
3. **Encrypted Client Hello (ECH)**: TLS 1.3 connections negotiating ECH encrypt the inner SNI, exposing only outer provider hostnames.
4. **DNS-over-TLS (DoT) / DNS-over-HTTPS (DoH)**: Android Private DNS encrypts DNS resolution over port 853 or port 443. Queries do not appear in standard plaintext UDP 53 captures.
5. **Evidence Standard**: Report observed hostnames and explicit caveats. Never claim "100% telemetry blocked" beyond the empirical evidence collected from traffic dumps.

---

## 8. Closing Protocol

### A. Mandatory Atomic Commits & Vertical Slices
Every completed unit of work must be committed immediately as an isolated, independent commit.

1. **Initial App Onboarding Commit (Vertical Slice)**:
   - Scope: Target app registration (`Constants.kt`, `PatchExecutionTest.kt`, `README.md`) + dedicated app guide (`docs/apps/<app_id>.md`) + initial telemetry patch implementation (`patches/.../<app_id>/...`).
   - Format: `feat(<app_id>): add block telemetry patch`
2. **Subsequent Feature / Opt-In Patch Commits**:
   - Scope: Standalone opt-in patch (e.g. MLKit slimmer) + accompanying documentation updates in `docs/apps/<app_id>.md`.
   - Format: `feat(<app_id>): add mlkit vision slimmer patch`
3. **Bugfix Commits**:
   - Scope: Standalone fix for a shifted fingerprint or broken component.
   - Format: `fix(<app_id>): update uploader fingerprint for <version>`

### B. Conventional Commits Standards
- Written strictly in English.
- Imperative mood, concise summary line.
- Plain ASCII only: no emojis, unicode pictographs, or non-ASCII symbols.
- Zero AI attribution, zero `Co-Authored-By` lines.

### C. Direct Commit & No-Push Invariant
- **Direct Commit**: Commit completed units autonomously upon satisfying verification gates.
- **Strict No-Push**: NEVER execute `git push` autonomously. Pushing is reserved strictly for explicit user instructions.
- **Strict No-PR**: Never generate PR titles, PR descriptions, or suggest opening PRs.

### D. Mandatory Post-Close Adversarial Audit (audit-stack)
After all commits in A are landed, automatically run the `audit-stack` skill against the onboarding changeset before closing the task (auditing the pending range, e.g. `origin/<base>..HEAD`). Never run per-commit auto-audits.
1. **Scope**: Strictly the onboarding scope -- `patches/.../<app_id>/`, `docs/apps/<app_id>.md`, and the app's hunks in `Constants.kt` / `PatchExecutionTest.kt` / `README.md`. Explicitly exclude `FOREIGN_IN_PROGRESS_WORK` (uncommitted files and directories outside the app scope); never stage, modify, or report foreign work as part of this audit.
2. **Remediation precedence**: `audit-stack`'s autonomous fix loop runs through the AGY delegation in force -- fixes are applied by the same AGY worker conversations (steered with concrete findings), gates are executed by the orchestrator. Never fix project files directly from the orchestrator; never use another harness's subagents for implementation.
3. **Fix commits**: Each confirmed fix lands as its own `fix(<app_id>): ...` commit after re-running the Section 6 gates. Loop until the audit reports zero blocking findings.
4. No push / no PR (per C).
