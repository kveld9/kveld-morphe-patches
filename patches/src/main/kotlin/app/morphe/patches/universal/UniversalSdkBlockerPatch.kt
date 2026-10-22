package app.morphe.patches.universal

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val TAG = "[Block Tracking SDKs]"

private data class SdkPrefixRule(
    val prefix: String,
    val label: String,
    val staticMethods: Set<String>,
    val instanceMethods: Set<String>,
    // Item 5: lifecycle-sensitive names require a parameter check, not just a name match.
    // staticParamGuards maps method name -> exact parameter type list (empty = any params).
    val staticParamGuards: Map<String, List<String>> = emptyMap(),
    // Instance method names that only match when the method takes at least one parameter.
    val guardedInstanceMethods: Set<String> = emptySet(),
)

private val APM_STATIC_METHODS = setOf(
    "withApplicationToken",
    "startup",
    "start",
    "init",
    "initialize",
    "register",
    "setup",
    "recordMetric",
    "recordCustomMetric",
    "recordHandledException",
    "recordBreadcrumb",
    "noticeHttpTransaction",
    "noticeNetworkFailure",
    "applyUserPrivacyOptions",
)

private val CRASH_STATIC_METHODS = setOf(
    "init",
    "start",
    "launch",
    "register",
    "setup",
    "configure",
    "initialize",
    "initializeSdk",
    "send",
    "reportException",
    "logException",
)

private val CRASH_INSTANCE_METHODS = setOf(
    "send",
    "reportException",
    "logException",
    "logMessage",
    "start",
)

private val ANALYTICS_STATIC_METHODS = setOf(
    "init",
    "start",
    "integrate",
    "register",
    "setup",
    "configure",
    "initialize",
    "capture",
    "identify",
    "flush",
    "reset",
    "optOut",
    "optIn",
    "close",
)

private val ANALYTICS_INSTANCE_METHODS = setOf(
    "track",
    "trackEvent",
    "tagEvent",
    "tagScreen",
    "recordEvent",
    "dispatch",
    "setUserId",
    "setUserAttributes",
    "openSession",
    "closeSession",
    "upload",
    "capture",
    "identify",
    "screen",
    "alias",
    "flush",
    "reset",
    "setUserAttribute",
    "logout",
)

private val ATTRIBUTION_STATIC_METHODS = setOf(
    "init",
    "start",
    "register",
    "setup",
    "configure",
    "initialize",
)

private val ATTRIBUTION_INSTANCE_METHODS = setOf(
    "track",
    "logEvent",
    "trackEvent",
    "sendEvent",
)

// External audit (issue #162): scoped additions, verified void in reference fingerprints.
// - onCreate is lifecycle-sensitive: scoped to Adjust rule ONLY, never added globally.
//   Adjust v5 renamed it to initSdk(AdjustConfig) (Atlas Earth, Vinted): same scope and guard.
// - pushEvent is CleverTap-specific: scoped to CleverTap rule ONLY.
// - initializeWithGameKey/addDesignEvent verified void: scoped to GameAnalytics ONLY,
//   added to both static+instance sets to cover SDK variants.
// - Transaction under Lcom/unity3d/analytics (distinct package from services/analytics):
//   void per external reference, UNVERIFIED in local APKs. Scoped to new Unity rule ONLY.
// NON-VOID POLICY: object-returning initializers (Branch.init, FirebaseApp.initializeApp)
// are skipped (null would NPE call sites). Exception: chainable instance methods in
// SELF_RETURN_ALLOWLIST whose returnType == defining class get `return-object p0`.
// Item 1: AppsFlyer real entry is AFb1vSDK.start (instance, obfuscated impl class),
// measured in FIFA/CapCut dex. Blocking start also silences install/session dispatch,
// but a deferred-conversion listener never fires: document splash-wait risk in docs.
private val APPSFLYER_INSTANCE_SCOPED = ATTRIBUTION_INSTANCE_METHODS + "start"

private val ADJUST_STATIC_SCOPED = ATTRIBUTION_STATIC_METHODS + "onCreate" + "initSdk"

// Identity entry points emit profile data even when event tracking is blocked.
// Only public entry points are listed; generic names such as setUserId, set, increment
// are deliberately excluded because the same names exist on internal SDK state classes
// (measured in Amplitude Kotlin: State, EventOptions, IdentityCoordinator, AnalyticsEvent
// use setUserId) and stubbing them would corrupt SDK state.
// Amplitude identify/groupIdentify/setGroup verified as self-returning on
// Lcom/amplitude/core/Amplitude in a real APK; Mixpanel identify/alias and CleverTap
// onUserLogin/pushProfile are void in the public API but UNVERIFIED in local APKs.
private val AMPLITUDE_INSTANCE_SCOPED =
    ATTRIBUTION_INSTANCE_METHODS + "identify" + "groupIdentify" + "setGroup"

private val MIXPANEL_INSTANCE_SCOPED = ATTRIBUTION_INSTANCE_METHODS + "identify" + "alias"

private val CLEVERTAP_INSTANCE_SCOPED =
    ATTRIBUTION_INSTANCE_METHODS + "pushEvent" + "onUserLogin" + "pushProfile"

private val GAMEANALYTICS_STATIC_SCOPED =
    ATTRIBUTION_STATIC_METHODS + "initializeWithGameKey" + "addDesignEvent"

private val GAMEANALYTICS_INSTANCE_SCOPED =
    ATTRIBUTION_INSTANCE_METHODS + "initializeWithGameKey" + "addDesignEvent"

private val UNITY_ANALYTICS_STATIC_SCOPED = ATTRIBUTION_STATIC_METHODS + "Transaction"

// Item 6: instance methods in this allowlist that return their own defining class
// (chainable APIs, e.g. Amplitude Kotlin track) are hooked with `return-object p0`
// instead of being skipped. Amplitude Kotlin identify/groupIdentify/setGroup return the
// Amplitude instance and get `return-object p0`. Type-correct by construction
// (returnType == class type), never null. "init" is not listed: it only appears in static
// method sets, and this path requires an instance method. Classes renamed by R8 fall
// outside the package prefix and are not covered. Boolean/int returns stay out of scope:
// each needs a hand-reviewed per-method value.
private val SELF_RETURN_ALLOWLIST = setOf("track", "identify", "groupIdentify", "setGroup")

// MANDATORY exclusion: NEVER block consent or clear setters (setConsent, setAdStorage,
// setAnalyticsStorage, setAdPersonalization, setAdUserData, clearConditionalUserProperty,
// resetAnalyticsData). Stubbing them would freeze granted consent or preserve stored data.
private val FIREBASE_ANALYTICS_METHODS = setOf(
    "logEvent",
    "setUserProperty",
    "onEvent",
    "registerEventNames",
    "unregisterEventNames",
    "setConditionalUserProperty",
)

// MANDATORY exclusion: NEVER block setCrashlyticsCollectionEnabled (direction-sensitive:
// stubbing a disable call keeps collection ON) nor deleteUnsentReports (it discards
// queued reports; stubbing preserves them for later upload).
private val CRASHLYTICS_STATIC = setOf(
    "recordException",
    "setCustomKeys",
)

private val CRASHLYTICS_INSTANCE = setOf(
    "recordException",
    "log",
    "setCustomKey",
    "setCustomKeys",
    "setUserId",
    "sendUnsentReports",
)

// MANDATORY exclusions: public API event/revenue/customRevenue/init return Z (non-void:
// out of scope for void-only early-return design); generic worker run->V (32x, class roles
// unverified) and callbacks onSuccess/onFailure (stubbing could stall retry state machines
// and increase network traffic) are deliberately excluded.
private val SINGULAR_INSTANCE_METHODS = setOf(
    "fetchReferrer",
    "onInstallReferrerReceived",
    "setup",
)

private val ATTRIBUTION_EXTRA_STATIC = setOf(
    "init",
    "initialize",
    "configure",
    "start",
    "register",
    "setup",
)

private val ATTRIBUTION_EXTRA_INSTANCE = setOf(
    "connect",
    "eventWithName",
    "track",
    "trackEvent",
    "logEvent",
    "sendEvent",
)

private val SNOWPLOW_STATIC = setOf(
    "init",
    "createTracker",
    "register",
    "setup",
    "start",
)

private val SNOWPLOW_INSTANCE = setOf(
    "track",
    "addContext",
    "setUserId",
    "setSubject",
)

private val PIPELINE_STATIC = setOf(
    "start",
    "init",
    "initializeSharedInstance",
    "register",
    "setup",
)

private val PIPELINE_INSTANCE = setOf(
    "logEvent",
    "logScreenEvent",
    "logError",
    "logException",
    "logNetworkPerformance",
    "addEvent",
    "uploadEvents",
)

// MANDATORY exclusion: NEVER add setAnalyticsEnabled (direction-sensitive consent setter)
// nor clearCachedData (discards stored data; stubbing preserves it).
private val HUAWEI_STATIC = setOf(
    "init",
    "setup",
    "register",
    "enableLog",
)

private val HUAWEI_INSTANCE = setOf(
    "onEvent",
    "setUserId",
    "setUserProfile",
)

private val METRICA_STATIC = setOf(
    "activate",
    "activateReporter",
    "reportEvent",
    "reportError",
    "reportRevenue",
    "resumeSession",
    "pauseSession",
    "sendEventsBuffer",
    "register",
)

private val METRICA_INSTANCE = setOf(
    "reportEvent",
    "reportError",
    "resumeSession",
    "pauseSession",
    "sendEventsBuffer",
)

// Scoped to appevents subpackage only (never broad com/facebook); flush verified
// against AppEventQueue.flush, logEvent is the stable public API entry.
private val APPEVENTS_INSTANCE = setOf(
    "flush",
    "logEvent",
)

// MANDATORY exclusion: NEVER add run (86x generic workers, roles unverified),
// onCreate/onActivity*/onNewIntent (component lifecycles, may belong to SDK UI),
// onResponse and callback-style methods (stall risk), access$* synthetics,
// loadUrl/showDialog/showOpenHeatMapDialog (functional UI), ignoreView and
// disableAutoTrack (DIRECTION INVERSION: stubbing keeps collection ON),
// handleSchemeUrl/mergeJSONObject/d/i (ambiguous internals), and any non-void
// overloads (out of void-only scope).
// logout/resetAnonymousId/removeExposureView/remove excluded (stubbing preserves identity/tracking state: direction inversion).
private val SENSORS_STATIC = setOf(
    "init",
    "track",
    "trackMenuItem",
    "trackViewOnClick",
    "trackDialog",
    "trackDrawerClosed",
    "trackDrawerOpened",
    "trackExpandableListViewOnChildClick",
    "trackExpandableListViewOnGroupClick",
    "trackListView",
    "trackRadioGroup",
    "trackTabHost",
    "trackTabLayoutSelected",
)

private val SENSORS_INSTANCE = setOf(
    "track",
    "trackEvent",
    "trackViewScreen",
    "trackViewAppClick",
    "trackAppInstall",
    "trackInstallation",
    "trackDeepLinkLaunch",
    "login",
    "loginWithKey",
    "identify",
    "showUpWebView",
    "showUpX5WebView",
    "onClick",
    "addExposureView",
    "setViewID",
)

private val REPLAY_STATIC = setOf(
    "init",
    "start",
    "startWithKey",
    "startWithConfiguration",
    "startNewSession",
    "setup",
    "setupAndStartRecording",
    "register",
    "initialize",
    "initializeSdk",
    "begin",
    "launch",
    "configure",
)

private val REPLAY_INSTANCE = setOf(
    "startRecording",
    "startNewSession",
    "startSession",
    "event",
    "identify",
)

private val LOCATION_STATIC = setOf(
    "initialize",
    "init",
    "start",
    "register",
    "setup",
    "configure",
)

private val LOCATION_INSTANCE = setOf(
    "start",
    "startTracking",
    "track",
    "sendLocation",
    "onLocationChanged",
)

private val LEGACY_METHODS = setOf(
    "send",
    "activityStart",
    "activityStop",
    "reportActivityStart",
    "reportActivityStop",
    "dispatchLocalHits",
    "enableAutoActivityReports",
    "setDryRun",
)

private val PUSH_STATIC_METHODS = setOf(
    "initWithContext",
    "startInit",
    "setAppId",
    "sendTag",
    "sendTags",
    "sendOutcome",
    "postNotification",
    "login",
    "logout",
    "takeOff",
    "configure",
    "init",
    "register",
    "setup",
    "setEmail",
    "setSMSNumber",
)

private val APM_RULES = listOf(
    SdkPrefixRule("Lcom/newrelic", "New Relic", APM_STATIC_METHODS, emptySet()),
    SdkPrefixRule("Lcom/datadog", "Datadog", APM_STATIC_METHODS, emptySet()),
    SdkPrefixRule("Lcom/dynatrace", "Dynatrace", APM_STATIC_METHODS, emptySet()),
)

private val CRASH_RULES = listOf(
    SdkPrefixRule("Lcom/mindscapehq", "Raygun", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/shakebugs", "Shake", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lio/embrace", "Embrace", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/splunk", "Splunk Mint", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/microsoft/appcenter", "App Center", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lio/opentelemetry", "OpenTelemetry", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lorg/acra", "ACRA", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lio/sentry", "Sentry", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/bugsnag", "Bugsnag", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/crashlytics/android", "Crashlytics", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/google/firebase/crashlytics", "Crashlytics", CRASHLYTICS_STATIC, CRASHLYTICS_INSTANCE),
    SdkPrefixRule("Lio/fabric/sdk", "Fabric", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/instabug", "Instabug", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lly/count/android", "Countly", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lnet/hockeyapp", "HockeyApp", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
)

private val ANALYTICS_RULES = listOf(
    SdkPrefixRule("Lcom/google/firebase/analytics", "Firebase Analytics", FIREBASE_ANALYTICS_METHODS, FIREBASE_ANALYTICS_METHODS),
    SdkPrefixRule("Lorg/matomo", "Matomo", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/leanplum", "Leanplum", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/localytics", "Localytics", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/webengage", "WebEngage", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/posthog", "PostHog", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/moengage", "MoEngage", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/snowplowanalytics", "Snowplow", SNOWPLOW_STATIC, SNOWPLOW_INSTANCE),
    SdkPrefixRule("Lcom/mparticle", "mParticle", PIPELINE_STATIC, PIPELINE_INSTANCE),
    SdkPrefixRule("Lcom/treasuredata", "Treasure Data", PIPELINE_STATIC, PIPELINE_INSTANCE),
    SdkPrefixRule("Lcom/huawei/hms/analytics", "Huawei Analytics", HUAWEI_STATIC, HUAWEI_INSTANCE),
    SdkPrefixRule("Lcom/yandex/metrica", "Yandex Metrica", METRICA_STATIC, METRICA_INSTANCE),
    SdkPrefixRule("Lcom/facebook/appevents", "Facebook AppEvents", emptySet(), APPEVENTS_INSTANCE),
    SdkPrefixRule("Lcom/sensorsdata", "Sensors Analytics", SENSORS_STATIC, SENSORS_INSTANCE),
)

private val ATTRIBUTION_RULES = listOf(
    SdkPrefixRule(
        "Lcom/appsflyer",
        "AppsFlyer",
        ATTRIBUTION_STATIC_METHODS,
        APPSFLYER_INSTANCE_SCOPED,
        guardedInstanceMethods = setOf("start"),
    ),
    SdkPrefixRule(
        "Lcom/adjust",
        "Adjust",
        ADJUST_STATIC_SCOPED,
        ATTRIBUTION_INSTANCE_METHODS,
        staticParamGuards = mapOf(
            "onCreate" to listOf("Lcom/adjust/sdk/AdjustConfig;"),
            "initSdk" to listOf("Lcom/adjust/sdk/AdjustConfig;"),
        ),
    ),
    SdkPrefixRule("Lcom/amplitude", "Amplitude", ATTRIBUTION_STATIC_METHODS, AMPLITUDE_INSTANCE_SCOPED),
    SdkPrefixRule("Lcom/mixpanel", "Mixpanel", ATTRIBUTION_STATIC_METHODS, MIXPANEL_INSTANCE_SCOPED),
    SdkPrefixRule("Lcom/clevertap", "CleverTap", ATTRIBUTION_STATIC_METHODS, CLEVERTAP_INSTANCE_SCOPED),
    SdkPrefixRule("Lcom/segment", "Segment", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lio/branch", "Branch", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/branch", "Branch", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/singular", "Singular", emptySet(), SINGULAR_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/kochava", "Kochava", ATTRIBUTION_EXTRA_STATIC, ATTRIBUTION_EXTRA_INSTANCE),
    SdkPrefixRule("Lcom/tenjin", "Tenjin", ATTRIBUTION_EXTRA_STATIC, ATTRIBUTION_EXTRA_INSTANCE),
    SdkPrefixRule("Lcom/unity3d/services/analytics", "Unity Analytics", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/unity3d/analytics", "Unity Analytics", UNITY_ANALYTICS_STATIC_SCOPED, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/flurry", "Flurry", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/gameanalytics", "GameAnalytics", GAMEANALYTICS_STATIC_SCOPED, GAMEANALYTICS_INSTANCE_SCOPED),
)

private val SESSION_REPLAY_RULES = listOf(
    SdkPrefixRule("Lcom/uxcam", "UXCam", REPLAY_STATIC, REPLAY_INSTANCE),
    SdkPrefixRule("Lcom/smartlook", "Smartlook", REPLAY_STATIC, REPLAY_INSTANCE),
    SdkPrefixRule("Lcom/fullstory", "FullStory", REPLAY_STATIC, REPLAY_INSTANCE),
    SdkPrefixRule("Lcom/contentsquare", "Contentsquare", REPLAY_STATIC, REPLAY_INSTANCE),
    SdkPrefixRule("Lcom/bugsee", "Bugsee", REPLAY_STATIC, REPLAY_INSTANCE),
)

private val LOCATION_RULES = listOf(
    SdkPrefixRule("Lio/radar", "Radar", LOCATION_STATIC, LOCATION_INSTANCE),
    SdkPrefixRule("Lcom/gimbal", "Gimbal", LOCATION_STATIC, LOCATION_INSTANCE),
    SdkPrefixRule("Lcom/estimote", "Estimote", LOCATION_STATIC, LOCATION_INSTANCE),
)

private val LEGACY_RULES = listOf(
    SdkPrefixRule("Lcom/google/analytics", "Legacy Analytics", LEGACY_METHODS, LEGACY_METHODS),
    SdkPrefixRule("Lcom/google/android/gms/analytics", "Legacy Analytics", LEGACY_METHODS, LEGACY_METHODS),
)

private val PUSH_RULES = listOf(
    SdkPrefixRule("Lcom/onesignal", "OneSignal", PUSH_STATIC_METHODS, emptySet()),
    SdkPrefixRule("Lcom/urbanairship", "Airship", PUSH_STATIC_METHODS, emptySet()),
    SdkPrefixRule("Lcom/braze", "Braze", PUSH_STATIC_METHODS, emptySet()),
    SdkPrefixRule("Lcom/appboy", "Braze", PUSH_STATIC_METHODS, emptySet()),
)

// MANDATORY DIRECTION-SAFETY EXCLUSIONS (Honeygain, Appsalt & EarnFM bandwidth monetization):
// Manifest component deactivation is the primary kill; bytecode hooks provide defense-in-depth
// on cold-start entry points only.
// NEVER hook:
// - Honeygain: create/getInstance/builder/build (non-void object returns; null risks call-site NPE),
//   isConsentAgreed/imsdkClassPresent/start()Z (non-void boolean returns),
//   stop/cancleAsyncJob (direction inversion: stubbing prevents cleanup/stop),
//   resetConsent/setConsent/reportUserConsent (direction inversion: stubbing freezes consent),
//   showConsent/showConsentNow/startWithConsent (consent dialog UI),
//   setCountry/setUid (config setters), getters (non-void).
//   With zero safe void methods surviving, Honeygain carries empty method sets to prevent unsafe stubbing.
// - Appsalt: stop/optOut (direction inversion: stubbing keeps SDK running or prevents opt-out),
//   setBackground/setLaunchOnBoot/setEnableLogging/setNotification/setNotificationId/setOnError (config setters),
//   isOptedIn/isRunning/isBackground/getId/getLastError/getNotification (non-void getters).
//   Instance methods are internal/companion delegates; only safe void static entry points are hooked.
// - EarnFM: FleetSdk(...) constructor (never hook constructors),
//   initialize/startSdk/stopSdk/destroyServer/storeConsent/isInitialized (non-void booleans -> excluded by void-only policy),
//   getActivePort (non-void int getter), setStoreLogs (config setter),
//   _performVersionCheck (internal scheduler callback; stubbing halts update checker only, not running server -> no defense value).
//   With zero safe void methods surviving, EarnFM carries empty method sets to prevent unsafe stubbing.
private val APPSALT_STATIC_METHODS = setOf(
    "initialize",
    "start",
    "optIn",
    "requestConsent",
)

private val BANDWIDTH_SHARING_RULES = listOf(
    SdkPrefixRule("Lio/hopmonsdk", "Honeygain", emptySet(), emptySet()),
    SdkPrefixRule("Lcom/appsalt", "Appsalt", APPSALT_STATIC_METHODS, emptySet()),
    SdkPrefixRule("Lcom/fleet", "EarnFM", emptySet(), emptySet()),
)

@Suppress("unused")
val universalSdkBlockerPatch = bytecodePatch(
    name = "Block Tracking SDKs",
    description = "Disables analytics, crash and ad SDKs inside the app.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val blockApm by booleanOption(
        key = "blockApm",
        default = true,
        title = "Performance monitors",
        description = "Neutralizes New Relic, Datadog, and Dynatrace monitoring methods.",
        required = false,
    )

    val blockCrashReporters by booleanOption(
        key = "blockCrashReporters",
        default = true,
        title = "Crash reporters",
        description = "Neutralizes Sentry, Crashlytics, Bugsnag, and common crash reporters.",
        required = false,
    )

    val blockAnalytics by booleanOption(
        key = "blockAnalytics",
        default = true,
        title = "Analytics",
        description = "Neutralizes Firebase, Matomo, PostHog, and common analytics SDKs.",
        required = false,
    )

    val blockAttribution by booleanOption(
        key = "blockAttribution",
        default = true,
        title = "Ad attribution",
        description = "Neutralizes AppsFlyer, Adjust, Amplitude, Branch, and attribution SDKs.",
        required = false,
    )

    val blockLegacyAnalytics by booleanOption(
        key = "blockLegacyAnalytics",
        default = true,
        title = "Old Google Analytics",
        description = "Neutralizes pre-Firebase Google Analytics tracking methods.",
        required = false,
    )

    val blockPushEngagement by booleanOption(
        key = "blockPushEngagement",
        default = false,
        title = "Push SDKs (breaks push!)",
        description = "Neutralizes OneSignal, Airship, and Braze push engagement SDKs.",
        required = false,
    )

    val blockSessionReplay by booleanOption(
        key = "blockSessionReplay",
        default = true,
        title = "Screen recorders",
        description = "Neutralizes UXCam, Smartlook, FullStory, and session recording SDKs.",
        required = false,
    )

    val blockLocationTrackers by booleanOption(
        key = "blockLocationTrackers",
        default = true,
        title = "Location trackers",
        description = "Neutralizes Radar, Gimbal, and Estimote location tracking SDKs.",
        required = false,
    )

    val blockBandwidthSharing by booleanOption(
        key = "blockBandwidthSharing",
        default = true,
        title = "Bandwidth-sharing SDKs",
        description = "Neutralizes Honeygain and Appsalt bandwidth monetization SDKs.",
        required = false,
    )

    val excludedSdks by stringOption(
        key = "excludedSdks",
        default = "",
        title = "SDKs to keep",
        description = "Comma-separated SDK names to leave untouched (e.g. 'Adjust, Branch').",
        required = false,
    )

    execute {
        val excludedLabels = (excludedSdks ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        var activeRules = buildActiveRules(
            blockApm = blockApm ?: true,
            blockCrash = blockCrashReporters ?: true,
            blockAnalytics = blockAnalytics ?: true,
            blockAttribution = blockAttribution ?: true,
            blockLegacy = blockLegacyAnalytics ?: true,
            blockPush = blockPushEngagement ?: false,
            blockSessionReplay = blockSessionReplay ?: true,
            blockLocationTrackers = blockLocationTrackers ?: true,
            blockBandwidthSharing = blockBandwidthSharing ?: true,
        )

        if (excludedLabels.isNotEmpty()) {
            val knownLabels = activeRules.map { it.label }.toSet()
            val unknown = excludedLabels - knownLabels
            if (unknown.isNotEmpty()) {
                println("$TAG Unknown SDK label(s): ${unknown.sorted().joinToString(", ")}.")
            }
            activeRules = activeRules.filter { it.label !in excludedLabels }
        }

        if (activeRules.isEmpty()) {
            println("$TAG Skipped: all SDK blocking options are disabled.")
            return@execute
        }

        var totalHooked = 0
        var totalSkipped = 0
        var touchedClasses = 0
        val sdkHookCounts = mutableMapOf<String, Int>()
        val sdkHookedMethods = mutableMapOf<String, MutableSet<String>>()

        classDefForEach { classDef ->
            val rule = matchSdkPrefix(classDef.type, activeRules) ?: return@classDefForEach
            val targetMethods = findBlockableMethods(classDef, rule)
            if (targetMethods.isEmpty()) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            val (hooked, skipped) = applyBlockHooks(mutableClass, targetMethods, rule, sdkHookCounts, sdkHookedMethods)
            totalHooked += hooked
            totalSkipped += skipped
            touchedClasses++
        }

        if (totalHooked == 0) {
            println("$TAG Target APK does not contain targeted SDK classes or methods (0 methods hooked).")
            return@execute
        }

        val summary = formatSummary(sdkHookCounts)
        val skippedSuffix = if (totalSkipped > 0) " ($totalSkipped skipped)" else ""
        println("$TAG Applied $totalHooked hook(s) across $touchedClasses class(es) ($summary)$skippedSuffix.")
        formatMethodLines(sdkHookedMethods).forEach { println("$TAG Hooked methods -> $it.") }
    }
}

private fun buildActiveRules(
    blockApm: Boolean,
    blockCrash: Boolean,
    blockAnalytics: Boolean,
    blockAttribution: Boolean,
    blockLegacy: Boolean,
    blockPush: Boolean,
    blockSessionReplay: Boolean,
    blockLocationTrackers: Boolean,
    blockBandwidthSharing: Boolean,
): List<SdkPrefixRule> {
    val categories = listOf(
        blockApm to APM_RULES,
        blockCrash to CRASH_RULES,
        blockAnalytics to ANALYTICS_RULES,
        blockAttribution to ATTRIBUTION_RULES,
        blockLegacy to LEGACY_RULES,
        blockPush to PUSH_RULES,
        blockSessionReplay to SESSION_REPLAY_RULES,
        blockLocationTrackers to LOCATION_RULES,
        blockBandwidthSharing to BANDWIDTH_SHARING_RULES,
    )
    return categories.filter { it.first }.flatMap { it.second }
}

private fun matchSdkPrefix(type: String, rules: List<SdkPrefixRule>): SdkPrefixRule? {
    for (rule in rules) {
        if (isPrefixMatch(type, rule.prefix)) return rule
    }
    return null
}

private fun isPrefixMatch(type: String, prefix: String): Boolean {
    if (!type.startsWith(prefix)) return false
    val len = prefix.length
    if (type.length == len) return true
    val delimiter = type[len]
    return delimiter == '/' || delimiter == ';' || delimiter == '$'
}

private fun isSpecialMethodName(name: String): Boolean =
    name == "<init>" || name == "<clinit>"

// Kotlin compiles interface default methods into static bridges in $DefaultImpls classes;
// they are shared plugin/lifecycle plumbing, not SDK entry points (Segment Plugin$DefaultImpls.setup
// only calls setAnalytics; stubbing it crashed Ground News at launch with an uninitialized lateinit).
// Public SDK entry points live on the SDK classes themselves and stay covered.
private fun isKotlinDefaultImplsClass(type: String): Boolean =
    type.endsWith("\$DefaultImpls;")

private fun isVoidBlockable(method: Method, rule: SdkPrefixRule): Boolean {
    val name = method.name
    return if (AccessFlags.STATIC.isSet(method.accessFlags)) {
        val guard = rule.staticParamGuards[name]
        if (guard != null) {
            method.parameters.map { it.type } == guard
        } else {
            name in rule.staticMethods
        }
    } else {
        if (name in rule.guardedInstanceMethods) {
            name in rule.instanceMethods && method.parameters.isNotEmpty()
        } else {
            name in rule.instanceMethods
        }
    }
}

private fun isSelfReturnBlockable(method: Method, classType: String, rule: SdkPrefixRule): Boolean {
    if (AccessFlags.STATIC.isSet(method.accessFlags)) return false
    val name = method.name
    if (isSpecialMethodName(name)) return false
    if (name !in SELF_RETURN_ALLOWLIST) return false
    if (name !in rule.instanceMethods && name !in rule.guardedInstanceMethods) return false
    if (method.returnType != classType) return false
    return (method.implementation?.registerCount ?: 0) >= 1
}

private fun isBlockableMethod(method: Method, classType: String, rule: SdkPrefixRule): Boolean {
    if (method.implementation == null) return false
    val name = method.name
    if (isSpecialMethodName(name)) return false
    if (method.returnType == "V") return isVoidBlockable(method, rule)
    return isSelfReturnBlockable(method, classType, rule)
}

private fun findBlockableMethods(classDef: ClassDef, rule: SdkPrefixRule): List<Method> {
    if (isKotlinDefaultImplsClass(classDef.type)) return emptyList()
    val matches = mutableListOf<Method>()
    for (method in classDef.methods) {
        if (isBlockableMethod(method, classDef.type, rule)) {
            matches.add(method)
        }
    }
    return matches
}

private fun applyBlockHooks(
    mutableClass: MutableClass,
    targetMethods: List<Method>,
    rule: SdkPrefixRule,
    counts: MutableMap<String, Int>,
    methodNames: MutableMap<String, MutableSet<String>>,
): Pair<Int, Int> {
    var hooked = 0
    var skipped = 0
    for (targetMethod in targetMethods) {
        val mutableMethod = mutableClass.methods.firstOrNull { MethodUtil.methodSignaturesMatch(it, targetMethod) }
        if (mutableMethod == null) {
            skipped++
            continue
        }
        if (targetMethod.returnType == "V") {
            mutableMethod.addInstruction(0, "return-void")
        } else {
            mutableMethod.addInstruction(0, "return-object p0")
        }
        counts[rule.label] = (counts[rule.label] ?: 0) + 1
        methodNames.getOrPut(rule.label) { mutableSetOf() }.add(targetMethod.name)
        hooked++
    }
    return Pair(hooked, skipped)
}

private fun formatSummary(counts: Map<String, Int>): String {
    return counts.entries
        .sortedByDescending { it.value }
        .joinToString(", ") { "${it.key}: ${it.value}" }
}

private fun formatMethodLines(methodNames: Map<String, Set<String>>): List<String> =
    methodNames.entries
        .sortedBy { it.key }
        .map { (label, names) -> "$label: ${names.sorted().joinToString(", ")}" }
