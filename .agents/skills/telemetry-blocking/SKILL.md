---
name: telemetry-blocking
description: Technical methodology and implementation patterns for telemetry, tracking, analytics, and crash-reporting blocking in Morphe patches.
---

<!-- Canonical shared copy: byte-identical file in kveld-extra-morphe-patches and brave-origin-patches (.agents/skills, hardlinked to .claude/skills inside each repo). Keep copies byte-identical; propagate edits with scripts/sync_shared_skills.sh. -->

# Telemetry Blocking Implementation Guidelines

## 0. Repository Adaptation Notes (Shared Canonical Copy)

- **Sync invariant**: this file is canonical and byte-identical in both sibling repos. Edit in either repo, then run `scripts/sync_shared_skills.sh` (or `--check` in CI) to propagate. Never create per-repo forks.
- **Helpers**: manifestening helpers live in `app.morphe.patches.shared` (`ManifestXml.kt`, `BytecodeUtils.kt`) in both repos with the same API (`stripPermissionsWhere`, `disableComponentsByName`, `setApplicationMetaData`, `replaceWithReturnVoid`, `clearTryBlocks`).
- **Extension wiring**: companion runtime filters (e.g. preference-gate filters) follow the local repo convention: `dependsOn(sharedExtensionPatch)` in kveld-extra, `extendWith("extensions/extension.mpe")` in brave-origin.

## 1. Analysis Methodology (Hybrid Discovery Model)

Telemetry blocking enforces defense-in-depth neutralization of tracking, analytics, attribution, advertising, and crash-reporting frameworks without breaking core application functionality.

To achieve exhaustive coverage across both commercial SDKs and in-house proprietary pipelines, apply a **Hybrid Discovery Model**:
1. **Automated Third-Party Discovery (Exodus Privacy Database)**:
   - Query the [Exodus Privacy Trackers API](https://reports.exodus-privacy.eu.org/api/trackers) (`etip` / `trackers.json`) to enumerate known commercial tracker package signatures (e.g. AppLovin, IronSource, Unity Ads, Adjust, AppsFlyer, Vungle, InMobi, Kochava, Singular) present in the target APK's DEX and Manifest.
   - Categorize findings into `analytics`, `advertisement`, `identification`, and `profiling`.
2. **First-Party & Proprietary Telemetry Reversing**:
   - Manually audit vendor-specific in-house analytics, ad measurement, and telemetry dispatchers (e.g. Microsoft OneDS/Aria/MUTSDK/AdMeasurement, Meta Analytics2, ByteDance AppLog) that are not distributed as third-party SDKs and therefore absent from public tracker catalogs.

### A. Manifest Inventory
Audit the decompiled `AndroidManifest.xml` tree to identify tracking entrypoints across six distinct structural categories:

1. **Permissions (`uses-permission`)**:
   - Advertising ID: `com.google.android.gms.permission.AD_ID`
   - Privacy Sandbox / AdServices:
     - `android.permission.ACCESS_ADSERVICES_ATTRIBUTION`
     - `android.permission.ACCESS_ADSERVICES_AD_ID`
     - `android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE`
     - `android.permission.ACCESS_ADSERVICES_TOPICS`
   - Play Install Referrer: `com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE`
   - Partner / Ad Store bindings: `com.applovin.array.apphub.permission.BIND_APPHUB_SERVICE`

2. **Content Providers (`<provider>`)**:
   - Measurement: `com.google.android.gms.measurement.AppMeasurementContentProvider`
   - Crash reporting: `io.sentry.android.core.SentryInitProvider`, `io.sentry.android.core.SentryPerformanceProvider`
   - Attribution & SDK initializers: `com.facebook.internal.FacebookInitProvider`, `io.branch.referral.BranchInitProvider`, `com.appsflyer.internal.platform_extension.PluginInfoContentProvider`
   - Device identifiers: `*FDIDLiteProvider`, `*PhoneIdProvider`, `*InstallReferrerProvider`, `*UsdidValuesProvider`
   - Startup Initializers: `androidx.startup.InitializationProvider` entries hosting ad initializers (`com.unity3d.services.core.configuration.AdsSdkInitializer`)

3. **Background Services (`<service>`)**:
   - Google Measurement: `com.google.android.gms.measurement.AppMeasurementService`, `com.google.android.gms.measurement.AppMeasurementJobService`
   - Google DataTransport: `com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService`, `com.google.android.datatransport.runtime.backends.TransportBackendDiscovery`
   - Firebase Sessions: `com.google.firebase.sessions.SessionLifecycleService`
   - First-party upload pipelines: `com.facebook.analytics2.logger.*UploadService`, `com.facebook.analytics2.fabric.onefabric.FFAlarmUploadJobService`
   - Attribution services: `com.appsflyer.internal.service.AFJobSchedulerService`

4. **Broadcast Receivers & Activities (`<receiver>`, `<activity>`)**:
   - Referrer receivers: `com.adjust.sdk.AdjustReferrerReceiver`, `com.appsflyer.SingleInstallBroadcastReceiver`, `com.appsflyer.MultipleInstallBroadcastReceiver`
   - Measurement receivers: `com.google.android.gms.measurement.AppMeasurementReceiver`, `com.google.android.gms.measurement.AppMeasurementInstallReferrerReceiver`
   - Alarm dispatchers: `com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver`, `com.facebook.analytics2.fabric.onefabric.OneFabricUploadAlarmReceiver`
   - Cross-Sell & Campaign Targeting: in-app promotion receivers/activities (e.g. `*CrossSellReceiver`, `*CrossSellHandlerActivity`, `*CampaignReceiver`, `*FloodgateDynamicUxActivity`, `*InstallBroadcastReceiver`).

5. **Component Discovery Registrars (`<meta-data>`)**:
   - Firebase/DI component discovery: child `<meta-data>` entries inside `com.google.firebase.components.ComponentDiscoveryService` declaring registrars (e.g. `AnalyticsRegistrar`, `CrashlyticsRegistrar`, `PerfRegistrar`).

6. **Extended Component Taxonomy (Universal Telemetry Neutralizer)**:
   - Device-ID and Cross-App Identity Providers: Content providers exposing persistent hardware/device identifiers, family attribution, and install referrer tokens across sibling apps (`*FDIDLiteProvider`, `*PhoneIdProvider`, `*UsdidValuesProvider`, `*FamilyAppsUserValuesProvider`, `*AttributionIdProvider`, `*InstallReferrerProvider`, `AccessLibraryContentProvider`).
   - Device-ID and Cross-Signing Services/Receivers: Services and broadcast receivers handling background cross-signing or install referrer fetch routines (`CrossSigningService`, `CrossSigningBroadcastReceiver`, `InstallReferrerFetchJobIntentService`, `PhoneIdRequestReceiver`).
   - Meta Analytics2 and OneFabric Upload Infrastructure: First-party upload services and alarm receivers orchestrating scheduled telemetry flushes (`FFAlarmUploadJobService`, `OneFabricUploadAlarmReceiver`, `GooglePlayUploadService`, `Analytics2UploadService`, `HighPriUploadRetryReceiver`, `DelayedWorkerService`).
   - Lacrima Crash Detectors and Dumper Upload Services: Background crash dump uploaders (`DumperUploadService`, `ExceptionsUploadService`, `ProfiloUploadService`) and system event broadcast receivers monitoring device shutdown or lock-screen transitions to detect dirty exits (`ProtectedLockScreenBroadcastReceiver`, `SystemShutdownBootBroadcastReceiver`, `CrashLoop$LastState`).
   - ML Kit Component Discovery: `MlKitComponentDiscoveryService` and `MlKitInitProvider` (flagged by scanners but subject to the mandatory preservation rule in Section 2.B).
   - Ad SDK Startup Initializers: `<meta-data>` initializers nested under `androidx.startup.InitializationProvider` that boot ad network SDKs on process launch (`com.unity3d.services.core.configuration.AdsSdkInitializer`, `MobileAdsInitProvider`).

### B. DEX Scan for Stable SDK Classes
Inspect DEX bytecodes to locate stable public entrypoints, ad measurement classes, and first-party upload dispatchers:

1. **Third-Party Analytics & Attribution SDKs (Cross-referenced with Exodus signatures)**:
   - Firebase Analytics: `Lcom/google/firebase/analytics/FirebaseAnalytics;->logEvent(Ljava/lang/String;Landroid/os/Bundle;)V`
   - Google AppMeasurement: `Lcom/google/android/gms/measurement/AppMeasurement;->logEventInternal(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;)V`
   - Adjust SDK: `Lcom/adjust/sdk/PackageHandler;->addPackage(Lcom/adjust/sdk/ActivityPackage;)V`, `sendFirstPackage()V`
   - Sentry: `Lio/sentry/react/RNSentryModuleImpl;->initNativeSdk(Lcom/facebook/react/bridge/ReadableMap;Lcom/facebook/react/bridge/Promise;)V`
   - AppsFlyer: `Lcom/appsflyer/AppsFlyerLib;->start(Landroid/content/Context;)V`, `logEvent(...)V`
   - Branch Metrics: `Lio/branch/referral/Branch;->isTrackingDisabled()Z` (getter stubbed to return 1)
   - Amplitude: `Lcom/amplitude/reactnative/AndroidContextProvider;->isLimitAdTrackingEnabled()Z` (getter stubbed to return 1)

2. **First-Party Analytics & Dispatchers**:
   - Meta / Instagram: `Lcom/instagram/analytics/analytics2/IgAnalytics2TaskBasedUploader;->HZG(LX/KpT;LX/ArQ;LX/Av0;)V`, `Lcom/instagram/analytics/analytics2/IGAnalytics2SimpleUploader;->HZG(...)V`, `Lcom/facebook/analytics2/logger/legacy/uploader/PrivacyControlledUploader;->HZG(...)V`
   - ByteDance / TikTok: `Lcom/ss/android/common/applog/AppLog;->onEvent(...)V`, `onEventV3(...)V`, `sendEvent(...)V`, `Lcom/bytedance/applog/AppLog;->onEventV3(...)V`
   - Microsoft OneDS / Aria / MUTSDK: `LifecycleHandler`, `AggregatedMetric`, `SendAggregationTimerTask`, `HardwareInformationReceiver`, `PowerInfoReceiver`
   - Brave Browser: `Lorg/chromium/components/prefs/PrefService;->e(Ljava/lang/String;)Z` (telemetry preference gatekeeper)

3. **Ad Measurement Platforms & Advertising Identifiers**:
   - Scan for classes matching `*admeasurement*`, `*admobile*`, `*adpartner*`, `*attribution*`, `*advertisingid*`, `*appsetid*`.
   - Scan for methods retrieving advertising IDs: `getAIFA()`, `getAppSetId()`, `getAdvertisingId()`, `getGaid()`, `getAdvertisingIdInfo()`.
   - Target: `Lcom/microsoft/office/adsmobile_admeasurementpartner/admeasurement/AdMeasurementPlatformData;->getAIFA()Ljava/lang/String;`, `->getAppSetId()Ljava/lang/String;`.

### C. Preservation Rules (Mandatory Invariants)
Telemetry patches must preserve all functional application systems:
1. **Push Notifications**:
   - NEVER disable push notification services: `com.google.firebase.messaging.FirebaseMessagingService`, `com.facebook.pushlite.*`, `com.facebook.rti.push.*`.
   - NEVER disable push receivers: `com.google.firebase.iid.FirebaseInstanceIdReceiver`, `com.braze.push.BrazePushReceiver`.
   - NEVER disable `com.google.firebase.provider.FirebaseInitProvider` by default, as Firebase Auth and Cloud Messaging rely on it.
2. **Deep Links & Intent Filters**:
   - NEVER strip activity intent filters handling `android.intent.action.VIEW` with `http`, `https`, or custom application URL schemes.
3. **Core Network Access**:
   - NEVER strip `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE` in default telemetry patches.

### D. DEX const-string Hosts Rewrite (Hosts-File Driven)
For tracking domains and telemetry endpoints that cannot be completely neutralized via method-level stubs or where native binary (ELF) editing is out of scope or undesirable, apply a Dalvik bytecode literal rewrite driven by an adblock/hosts blocklist:
- **Literal Rewriting Mechanism**: Traverse Dalvik instructions across candidate classes and rewrite `const-string` and `const-string/jumbo` URL and hostname literals matching a user-supplied hosts blocklist to a sink IP address (`0.0.0.0` by default).
- **Subdomain Matching**: Support matching both exact hostnames and subdomains (e.g. an entry for `example.com` matches `analytics.example.com`).
- **Reserved-Host Exclusions**: Explicitly exclude loopback, local network, and wildcard addresses (`localhost`, `localhost6`, `localhost.localdomain`, `0.0.0.0`, `127.0.0.1`, `::1`) from candidate matching to prevent corrupting local IPC or loopback server bindings.
- **Large-Blocklist Memory Warning**: Loading blocklists exceeding 100,000 rules incurs high heap memory usage and noticeable patching latency on memory-constrained devices. Emit an explicit diagnostic warning when large lists are parsed.
- **Architectural Role (Second-Layer Defense)**: Serves as a second layer of defense against commercial offers, tracking endpoints, or dynamic ping targets (e.g. `offers.brave.com`) directly in DEX without requiring ELF string redirection in `libchrome.so` or companion native libraries.

### E. Mandatory SDK Method Exclusions
When authoring Dalvik bytecode stubs for analytics and telemetry SDKs, never stub methods whose omission or naive replacement inverts tracking state, preserves stale telemetry queues, or breaks application lifecycles. When in doubt about polarity or method side effects, exclude the method from stubbing:
- **Consent and Direction-Sensitive Setters (Never Stub)**: Never stub consent management or opt-out setters with `return-void`. Calling `setConsent`, `setAdStorage`, `setAnalyticsStorage`, `setAdPersonalization`, `setAdUserData`, `clearConditionalUserProperty`, `resetAnalyticsData`, `setAnalyticsEnabled`, `disableAutoTrack`, or `ignoreView` signals that the user or system is opting out or revoking permissions. Stubbing them with an early return prevents the opt-out signal from propagating and freezes tracking in an enabled state.
- **Report-Queue Mutators (Never Stub Purges)**: Never stub queue-clearing or collection-disabling routines such as `setCrashlyticsCollectionEnabled` (direction-sensitive) or `deleteUnsentReports` / `clearCachedData`. `deleteUnsentReports` discards pending queued crash reports; stubbing it keeps reports queued on disk for future upload attempts. Only explicit dispatch triggers (such as `sendUnsentReports`) are safe to stub.
- **Identity Preservers (Never Stub Teardown)**: Never stub identity and session teardown methods (`logout`, `resetAnonymousId`, `removeExposureView`, `remove`). Stubbing identity clearance preserves user identity and tracking state across sessions.
- **Generic Workers, Lifecycles, and Callbacks (Never Stub)**: Never stub generic asynchronous entrypoints like `run` (generic `Runnable` workers whose class roles are unverified), Android component lifecycles (`onCreate`, `onActivityCreated`, `onActivityStarted`, `onNewIntent` which may govern essential SDK UI or Activity bindings), asynchronous network callbacks (`onResponse`, `onSuccess`, `onFailure` where stubbing stalls retry state machines and exacerbates retry traffic), or synthetic bridge methods (`access$*`).
- **Functional UI (Never Stub)**: Never stub methods that render interactive dialogs, webviews, or core user flows (`loadUrl`, `showDialog`, `showOpenHeatMapDialog`).
- **Non-Void Overloads**: Public SDK methods returning non-void types (e.g. `boolean`, integer status codes, or instance references) must not be stubbed with void returns (`return-void`). These methods are outside the scope of void-only early returns; attempting to inject `return-void` causes bytecode verification errors.
- **Core Rule**: When in doubt about method polarity or runtime necessity, exclude the method from stubbing.

### F. Out of Scope (What is OUT)
- **Functional Deep Links**: Authentication redirects, universal links, and external app integrations are strictly preserved.
- **Wearable Companions**: WearOS listeners (e.g. `com.hevy.services.WearListenerService`, `com.meta.wearable.acdc.sdk.service.ACDCRegistrationService`) are disabled only when they function solely as telemetry or unrequested background sync. Functional companion features are left intact unless debloating is requested.
- **Google Play Billing**: `com.android.billingclient.api.ProxyBillingActivity` and `ProxyBillingActivityV2` must NEVER be removed or disabled. Stripping them triggers `ActivityNotFoundException` during user checkouts.
- **In-App Settings UI**: Dynamic configuration toggles or preference activities inside the host app are strictly prohibited. Options must be compile-time / patch-time options in Morphe Manager / CLI.

---

## 2. Implementation Patterns

Telemetry blocking is implemented through a two-layer architecture: a manifest-level `resourcePatch` executed prior to bytecode assembly, and a primary `bytecodePatch` that depends on it.

### A. Manifest Purge (`resourcePatch`)
Use centralized helper extensions from `app.morphe.patches.shared` to manipulate `AndroidManifest.xml` cleanly:

```kotlin
private val exampleTelemetryResourcePatch = resourcePatch(
    name = "Telemetry Manifest Purge",
    description = "Strips advertising permissions, disables analytics components, and injects opt-out metadata in AndroidManifest.xml.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_<APP>)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[<App> Telemetry] AndroidManifest.xml not found - skipping manifest purge.")
            return@execute
        }

        val blockedPermissions = setOf(
            "com.google.android.gms.permission.AD_ID",
            "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
            "android.permission.ACCESS_ADSERVICES_AD_ID",
            "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE",
            "android.permission.ACCESS_ADSERVICES_TOPICS",
            "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
        )

        val blockedComponents = setOf(
            "com.google.android.gms.measurement.AppMeasurementService",
            "com.google.android.gms.measurement.AppMeasurementJobService",
            "com.google.android.gms.measurement.AppMeasurementReceiver",
            "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
            "com.google.android.datatransport.runtime.backends.TransportBackendDiscovery",
            "com.example.analytics.AsyncInstallReferrerProvider",
        )

        val optOutMetadata = listOf(
            "firebase_analytics_collection_enabled" to "false",
            "firebase_analytics_collection_deactivated" to "true",
            "firebase_crashlytics_collection_enabled" to "false",
            "firebase_performance_collection_enabled" to "false",
            "firebase_performance_collection_deactivated" to "true",
            "google_analytics_adid_collection_enabled" to "false",
            "google_analytics_default_allow_ad_personalization_signals" to "false",
        )

        var removedPermissions = 0
        var disabledComponents = 0
        var removedRegistrars = 0
        var injectedMetadata = 0

        document(manifestFile.absolutePath).use { doc ->
            val root = doc.documentElement
            val application = root.getElementsByTagName("application").item(0) as? org.w3c.dom.Element

            // 1. Strip tracking and ad permissions
            removedPermissions = root.stripPermissionsWhere { it in blockedPermissions }.size

            if (application != null) {
                // 2. Disable components (sets android:enabled="false" and android:exported="false")
                disabledComponents = application.disableComponentsByName(*blockedComponents.toTypedArray())

                // 3. Remove discovery meta-data registrars (exclude MLKit)
                removedRegistrars = application.removeComponentDiscoveryRegistrarsWhere { name ->
                    (name.contains("analytics", ignoreCase = true) ||
                        name.contains("measurement", ignoreCase = true) ||
                        name.contains("crashlytics", ignoreCase = true)) &&
                        !name.contains("mlkit", ignoreCase = true)
                }

                // 4. Inject declarative SDK opt-out metadata flags
                optOutMetadata.forEach { (name, value) ->
                    application.setApplicationMetaData(name, value)
                    injectedMetadata++
                }
            }
        }

        println("[<App> Telemetry] Stripped $removedPermissions permissions, disabled $disabledComponents tracking components, removed $removedRegistrars discovery registrars, and injected $injectedMetadata opt-out flags in AndroidManifest.xml.")
    }
}
```

### B. ML Kit Component Discovery Caveat & Scanner Warning
Exodus Privacy and App Manager flag Google ML Kit components (`MlKitInitProvider`, `MlKitComponentDiscoveryService`) as trackers because ML Kit uses Firebase's `CommonComponentRegistrar` dependency injection mechanism.
- **Operational Reality**: ML Kit models (OCR, barcode scanning, text recognition) execute strictly locally on-device and transmit zero network telemetry.
- **Rule**: NEVER strip ML Kit components in default telemetry patches. Disabling ML Kit breaks in-app camera barcode scanning, QR scanning, and text recognition.
- **Opt-In Exception**: If ML Kit removal is specifically required, author it as an isolated, standalone patch with `default = false` and an explicit description warning:
  ```kotlin
  val exampleMlKitSlimmerPatch = resourcePatch(
      name = "MLKit Vision Slimmer",
      description = "Disables MLKit component discovery and registrars. WARNING: this breaks in-app QR and barcode scanning.",
      default = false,
  )
  ```

### C. Bytecode Dispatcher Neutralization (`bytecodePatch`)
Neutralize event dispatchers at Dalvik bytecode level using verified dexlib2 fingerprints:

1. **Void Dispatcher Stubbing**:
   Inject an immediate `return-void` at instruction index 0:
   ```kotlin
   Fingerprint(
       definingClass = "Lcom/target/analytics/Uploader;",
       name = "dispatch",
       parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
       returnType = "V",
   ).method.addInstructions(0, "return-void")
   ```

2. **Boolean Getter / Gatekeeper Stubbing**:
   Force return `false` (`const/4 v0, 0x0; return v0`) for tracking enabled checks, or `true` (`const/4 v0, 0x1; return v0`) for tracking disabled / limit ad tracking checks:
   ```kotlin
   Fingerprint(
       definingClass = "Lio/branch/referral/Branch;",
       name = "isTrackingDisabled",
       returnType = "Z",
   ).method.addInstructions(
       0,
       """
           const/4 v0, 0x1
           return v0
       """.trimIndent(),
   )
   ```

3. **Promise Resolvers (React Native / Hybrid Bridges)**:
   When an async SDK init returns a `Promise`, resolve the promise before returning to prevent hanging React Native bridges:
   ```kotlin
   Fingerprint(
       definingClass = "Lio/sentry/react/RNSentryModuleImpl;",
       name = "initNativeSdk",
   ).method.addInstructions(
       0,
       """
           sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
           invoke-interface {p2, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
           return-void
       """.trimIndent(),
   )
   ```

4. **Multi-Return Reverse Traversal**:
   When wrapping or filtering methods at their return points (e.g. `PrefService.e`), locate all return instructions and iterate in reverse order:
   ```kotlin
   val returnIndices = method.implementation?.instructions?.withIndex()
       ?.filter { it.value.opcode == Opcode.RETURN }
       ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
       ?.toList() ?: emptyList()

   returnIndices.asReversed().forEach { (returnIndex, reg) ->
       method.addInstructions(
           returnIndex,
           """
               invoke-static {p1, v$reg}, ${Constants.BRAVE_EXTENSION_CLASS}->filterTelemetryPref(Ljava/lang/String;Z)Z
               move-result v$reg
           """.trimIndent(),
       )
   }
   ```

5. **Advertising Identifier & Attribution Nullification**:
   Force return empty string `""` or `null` for advertising identifier getters (e.g. `getAIFA`, `getAppSetId`, `getAdvertisingId`) to break tracking payloads if background requests survive:
   ```kotlin
   Fingerprint(
       definingClass = "Lcom/microsoft/office/adsmobile_admeasurementpartner/admeasurement/AdMeasurementPlatformData;",
       name = "getAIFA",
       returnType = "Ljava/lang/String;",
       parameters = emptyList(),
   ).method.apply {
       clearTryBlocks()
       ensureRegisterCount(1)
       implementation?.let { removeInstructions(0, it.instructions.count()) }
       addInstructions(0, """
           const-string v0, ""
           return-object v0
       """)
   }
   ```

6. **Register Stability**:
   Standard non-range invokes (`invoke-*`) can only reference registers `v0`-`v15`. In methods with high register counts (`.registers 18`), parameter registers `p0`, `p1`, etc., map to high indices (`v16`, `v17`). To pass parameters safely, use `invoke-*/range` or copy high registers into low temporary registers first.

7. **Preference-Gate Filtering (Brave Pattern)**:
   For applications that route multiple telemetry, diagnostic, and feature flags through a unified preference query method (e.g. `PrefService.e(String key)` returning `boolean`), replacing the entire method with a static `return false` breaks non-telemetry application preferences. Instead, combine a resource patch (`resourcePatch`) that sets `android:defaultValue="false"` on telemetry preference switches in XML layout/preference files with bytecode hooking that wraps every return point using multi-return reverse traversal (as shown in item 4 above). Each return instruction passes the preference key and existing boolean value to a companion runtime extension function (e.g. `Extension.filterTelemetryPref(key, value)`), which selectively forces known telemetry preference keys (such as P3A analytics, usage metrics, or discovery experiments) to `false` while preserving all functional preferences untouched.

### D. Zero-Zombie Rule (Definitive Gate Invariant)
- **Zero Fingerprint Mismatches**: Every fingerprint in committed code MUST resolve cleanly against the target APK.
- **No Masking via try-catch**: Wrapping hooks in `try-catch` blocks is permitted strictly as a temporary diagnostic aid during local triage to isolate shifted targets. It is strictly prohibited to leave failing fingerprints caught by `try-catch` in committed code.
- **Prune Obsolete Hooks**: When an upstream app update removes a class, method, or tracking pipeline, prune the obsolete fingerprint entirely instead of retaining dead code.

### E. Diagnostic Telemetry Contract
Every patch execution must emit clean, high-signal diagnostic output complying with the following invariants:
- **Prefix**: Every log line begins with the bracketed patch name: `println("[<Patch Name>] ...")`.
- **Dynamic Mutation Counter**: Track applied hooks and components with local counters (`var patched = 0`).
- **Single Consolidated Summary**: Emit a single concluding summary line:
  `println("[<Patch Name>] Neutralized $patched telemetry dispatch methods.")`
- **Guard Transparency**: If preconditions are unmet or an optional component is absent, log an explicit reason:
  `println("[<Patch Name>] Skipped: AndroidManifest.xml not found.")`
- **Zero Loop Spam**: Never log inside traversal loops. Aggregate metrics and emit single totals.
- **Plain ASCII Only**: Never use emojis, unicode pictographs, or non-ASCII characters in log output.

---

## 3. Verification

Static analysis alone is insufficient. Verify telemetry suppression through direct artifact inspection and automated runner gates.

### A. Output APK Manifest Inspection
Inspect the patched APK manifest using `aapt2` to prove components and permissions were modified:

```bash
# Verify advertising and attribution permissions are completely absent
aapt2 dump xmltree --file AndroidManifest.xml build/test-<app>.apk | grep -E "E: uses-permission.*android:name.*(AD_ID|ACCESS_ADSERVICES)"

# Verify tracking services are explicitly disabled (android:enabled="false" -> 0x0)
aapt2 dump xmltree --file AndroidManifest.xml build/test-<app>.apk | grep -B 2 -A 5 "AppMeasurementService"

# Verify opt-out metadata flags are injected
aapt2 dump xmltree --file AndroidManifest.xml build/test-<app>.apk | grep -B 2 -A 5 "firebase_analytics_collection_enabled"
```

Pass criteria:
1. Blocked permissions return zero lines in the `aapt2` output.
2. Disabled components contain `A: android:enabled(0x0101000e)=0x0`.
3. Injected metadata entries exist with `A: android:value(0x01010024)="false"`.

### B. Morphe Patcher Gate Commands
Run the official patching test harness across the target application. (Commands named for reference; do not run unprompted):

```bash
# Execute patch test suite for target app
./gradlew runPatchTest -Papp=<app_id>

# Run with all options enabled
./gradlew runPatchTest -Papp=<app_id> -PallOptions=true

# Quiet execution for automated evaluation
./gradlew runPatchTest -Papp=<app_id> --console=plain > build/patchtest-<app_id>.log 2>&1
```

A patch task is complete only when the runner reports:
- Failed patches: `0`
- Detected Fingerprint Failures: `0`
- Detected Smali Compile Errors: `0`
- Exit code: `0`
