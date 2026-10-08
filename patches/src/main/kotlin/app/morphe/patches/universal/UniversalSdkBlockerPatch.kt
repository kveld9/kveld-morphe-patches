package app.morphe.patches.universal

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patches.shared.findMutableMethodOf
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method

private const val TAG = "[Universal SDK Blocker]"

private data class SdkPrefixRule(
    val prefix: String,
    val label: String,
    val staticMethods: Set<String>,
    val instanceMethods: Set<String>,
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
    SdkPrefixRule("Lio/fabric/sdk", "Fabric", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/instabug", "Instabug", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lly/count/android", "Countly", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
    SdkPrefixRule("Lnet/hockeyapp", "HockeyApp", CRASH_STATIC_METHODS, CRASH_INSTANCE_METHODS),
)

private val ANALYTICS_RULES = listOf(
    SdkPrefixRule("Lorg/matomo", "Matomo", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/leanplum", "Leanplum", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/localytics", "Localytics", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/webengage", "WebEngage", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/posthog", "PostHog", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/moengage", "MoEngage", ANALYTICS_STATIC_METHODS, ANALYTICS_INSTANCE_METHODS),
)

private val ATTRIBUTION_RULES = listOf(
    SdkPrefixRule("Lcom/appsflyer", "AppsFlyer", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/adjust", "Adjust", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/amplitude", "Amplitude", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/mixpanel", "Mixpanel", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/clevertap", "CleverTap", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/segment", "Segment", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lio/branch", "Branch", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/branch", "Branch", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/unity3d/services/analytics", "Unity Analytics", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/flurry", "Flurry", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
    SdkPrefixRule("Lcom/gameanalytics", "GameAnalytics", ATTRIBUTION_STATIC_METHODS, ATTRIBUTION_INSTANCE_METHODS),
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

@Suppress("unused")
val universalSdkBlockerPatch = bytecodePatch(
    name = "Universal SDK Blocker",
    description = "Neutralizes third-party APM, crash reporting, analytics, attribution, and push engagement SDK init and event methods at DEX level via early return-void; companion runtime layer to Universal Telemetry Neutralizer (manifest layer).",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val blockApm by booleanOption(
        key = "blockApm",
        default = true,
        title = "Block APM & Performance Monitoring SDKs",
        description = "Neutralize New Relic, Datadog, and Dynatrace monitoring, metric reporting, and HTTP transaction tracing methods.",
        required = false,
    )

    val blockCrashReporters by booleanOption(
        key = "blockCrashReporters",
        default = true,
        title = "Block Crash Reporting SDKs",
        description = "Neutralize Raygun, Shake, Embrace, Splunk Mint, App Center, OpenTelemetry, ACRA, Sentry, Bugsnag, Crashlytics, Fabric, Instabug, Countly, and HockeyApp crash reporters.",
        required = false,
    )

    val blockAnalytics by booleanOption(
        key = "blockAnalytics",
        default = true,
        title = "Block Analytics SDKs",
        description = "Neutralize Matomo, Leanplum, Localytics, WebEngage, PostHog, and MoEngage event tracking, capture, and session logging methods.",
        required = false,
    )

    val blockAttribution by booleanOption(
        key = "blockAttribution",
        default = true,
        title = "Block Attribution & Engagement SDKs",
        description = "Neutralize AppsFlyer, Adjust, Amplitude, Mixpanel, CleverTap, Segment, Branch, Unity Analytics, Flurry, and GameAnalytics conversion, tracking, and attribution SDKs.",
        required = false,
    )

    val blockLegacyAnalytics by booleanOption(
        key = "blockLegacyAnalytics",
        default = true,
        title = "Block Legacy Google Analytics",
        description = "Neutralize pre-Firebase Google Analytics tracking, hit dispatching, and activity reporting methods (com.google.analytics and com.google.android.gms.analytics).",
        required = false,
    )

    val blockPushEngagement by booleanOption(
        key = "blockPushEngagement",
        default = false,
        title = "Block Push Engagement SDKs (WARNING: Breaks Push)",
        description = "Neutralize OneSignal, Airship, and Braze push engagement and tagging SDKs. WARNING: this breaks push notification delivery; enable only to fully silence push engagement SDK runtimes.",
        required = false,
    )

    execute {
        val activeRules = buildActiveRules(
            blockApm = blockApm ?: true,
            blockCrash = blockCrashReporters ?: true,
            blockAnalytics = blockAnalytics ?: true,
            blockAttribution = blockAttribution ?: true,
            blockLegacy = blockLegacyAnalytics ?: true,
            blockPush = blockPushEngagement ?: false,
        )

        if (activeRules.isEmpty()) {
            println("$TAG Skipped: all SDK blocking options are disabled.")
            return@execute
        }

        var totalHooked = 0
        var touchedClasses = 0
        val sdkHookCounts = mutableMapOf<String, Int>()

        classDefForEach { classDef ->
            val rule = matchSdkPrefix(classDef.type, activeRules) ?: return@classDefForEach
            val targetMethods = findBlockableMethods(classDef, rule)
            if (targetMethods.isEmpty()) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            totalHooked += applyBlockHooks(mutableClass, targetMethods, rule, sdkHookCounts)
            touchedClasses++
        }

        if (totalHooked == 0) {
            println("$TAG Target APK does not contain targeted SDK classes or methods (0 methods hooked).")
            return@execute
        }

        val summary = formatSummary(sdkHookCounts)
        println("$TAG Applied $totalHooked hook(s) across $touchedClasses class(es) ($summary).")
    }
}

private fun buildActiveRules(
    blockApm: Boolean,
    blockCrash: Boolean,
    blockAnalytics: Boolean,
    blockAttribution: Boolean,
    blockLegacy: Boolean,
    blockPush: Boolean,
): List<SdkPrefixRule> {
    val categories = listOf(
        blockApm to APM_RULES,
        blockCrash to CRASH_RULES,
        blockAnalytics to ANALYTICS_RULES,
        blockAttribution to ATTRIBUTION_RULES,
        blockLegacy to LEGACY_RULES,
        blockPush to PUSH_RULES,
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

private fun isBlockableMethod(method: Method, rule: SdkPrefixRule): Boolean {
    if (method.implementation == null) return false
    val name = method.name
    if (isSpecialMethodName(name)) return false
    if (method.returnType != "V") return false
    return if (AccessFlags.STATIC.isSet(method.accessFlags)) {
        name in rule.staticMethods
    } else {
        name in rule.instanceMethods
    }
}

private fun findBlockableMethods(classDef: ClassDef, rule: SdkPrefixRule): List<Method> {
    val matches = mutableListOf<Method>()
    for (method in classDef.methods) {
        if (isBlockableMethod(method, rule)) {
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
): Int {
    var hooked = 0
    for (targetMethod in targetMethods) {
        val mutableMethod = mutableClass.findMutableMethodOf(targetMethod)
        mutableMethod.addInstruction(0, "return-void")
        counts[rule.label] = (counts[rule.label] ?: 0) + 1
        hooked++
    }
    return hooked
}

private fun formatSummary(counts: Map<String, Int>): String {
    return counts.entries
        .sortedByDescending { it.value }
        .joinToString(", ") { "${it.key}: ${it.value}" }
}
