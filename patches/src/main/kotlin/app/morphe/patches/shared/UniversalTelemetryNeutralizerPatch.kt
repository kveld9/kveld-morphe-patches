package app.morphe.patches.shared

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch

private val TRACKING_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
)

private val TELEMETRY_PROVIDERS = setOf(
    "com.google.android.gms.measurement.AppMeasurementContentProvider",
    "io.sentry.android.core.SentryInitProvider",
    "io.sentry.android.core.SentryPerformanceProvider",
    "com.facebook.internal.FacebookInitProvider",
    "com.flurry.android.agent.FlurryContentProvider",
    "io.branch.referral.BranchInitProvider",
    "com.appsflyer.internal.platform_extension.PluginInfoContentProvider",
    "com.google.firebase.perf.provider.FirebasePerfProvider",
)

private const val FIREBASE_INIT_PROVIDER = "com.google.firebase.provider.FirebaseInitProvider"

private val TELEMETRY_SERVICES = setOf(
    "com.google.android.gms.measurement.AppMeasurementService",
    "com.google.android.gms.measurement.AppMeasurementJobService",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
    "com.google.android.datatransport.runtime.backends.TransportBackendDiscovery",
    "com.google.firebase.sessions.SessionLifecycleService",
    "com.appsflyer.internal.service.AFJobSchedulerService",
)

private val TELEMETRY_RECEIVERS = setOf(
    "com.google.android.gms.measurement.AppMeasurementReceiver",
    "com.google.android.gms.measurement.AppMeasurementInstallReferrerReceiver",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
    "com.adjust.sdk.AdjustReferrerReceiver",
    "com.appsflyer.SingleInstallBroadcastReceiver",
    "com.appsflyer.MultipleInstallBroadcastReceiver",
)

private val OPT_OUT_METADATA = listOf(
    "firebase_analytics_collection_enabled" to "false",
    "firebase_analytics_collection_deactivated" to "true",
    "firebase_crashlytics_collection_enabled" to "false",
    "firebase_performance_collection_enabled" to "false",
    "firebase_performance_collection_deactivated" to "true",
    "firebase_performance_logcat_enabled" to "false",
    "firebase_data_collection_default_enabled" to "false",
    "google_analytics_adid_collection_enabled" to "false",
    "google_analytics_deferred_deep_link_enabled" to "false",
    "google_analytics_default_allow_ad_personalization_signals" to "false",
    "google_analytics_automatic_screen_reporting_enabled" to "false",
    "appsflyer_data_collection_enabled" to "false",
    "io.sentry.auto-init" to "false",
)

@Suppress("unused")
val universalTelemetryNeutralizerPatch = resourcePatch(
    name = "Universal Telemetry Neutralizer",
    description = "Strips advertising and Privacy Sandbox permissions, disables analytics ContentProviders and telemetry background services (Firebase, Sentry, Adjust, AppsFlyer, DataTransport), prunes ComponentDiscovery registrars, and injects telemetry opt-out metadata.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val revokePermissions by booleanOption(
        key = "revokePermissions",
        default = true,
        title = "Revoke Advertising & Tracking Permissions",
        description = "Remove AD_ID, Privacy Sandbox attribution/topics/audiences, and Play Install Referrer permissions from AndroidManifest.xml.",
        required = false,
    )

    val disableProviders by booleanOption(
        key = "disableProviders",
        default = true,
        title = "Disable Telemetry ContentProviders",
        description = "Disable analytics ContentProviders (Google Measurement, Sentry, Facebook, Flurry, Branch, AppsFlyer).",
        required = false,
    )

    val disableServices by booleanOption(
        key = "disableServices",
        default = true,
        title = "Disable Telemetry Background Services",
        description = "Disable Google Measurement, Google DataTransport, Firebase Sessions, and AppsFlyer background job services.",
        required = false,
    )

    val disableReceivers by booleanOption(
        key = "disableReceivers",
        default = true,
        title = "Disable Telemetry Receivers",
        description = "Disable install referrer and analytics measurement broadcast receivers (Adjust, AppsFlyer, Google Measurement).",
        required = false,
    )

    val injectOptOutFlags by booleanOption(
        key = "injectOptOutFlags",
        default = true,
        title = "Inject Telemetry Opt-Out Flags & Prune Registrars",
        description = "Inject meta-data opt-out entries into application tag (Firebase Analytics, Crashlytics, Performance, AppsFlyer, Sentry) and prune Firebase discovery registrars.",
        required = false,
    )

    val disableFirebaseInit by booleanOption(
        key = "disableFirebaseInit",
        default = false,
        title = "Disable Firebase Init Provider",
        description = "Disable com.google.firebase.provider.FirebaseInitProvider. Default is false to prevent issues in apps that depend on Firebase Auth or push notifications.",
        required = false,
    )

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Universal Telemetry Neutralizer] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        val shouldRevoke = revokePermissions ?: true
        val shouldDisableProviders = disableProviders ?: true
        val shouldDisableServices = disableServices ?: true
        val shouldDisableReceivers = disableReceivers ?: true
        val shouldInjectOptOut = injectOptOutFlags ?: true
        val shouldDisableFirebase = disableFirebaseInit ?: false

        var removedPerms: List<String> = emptyList()
        var disabledProvidersCount = 0
        var disabledServicesCount = 0
        var disabledReceiversCount = 0
        var injectedFlagsCount = 0
        var removedRegistrarsCount = 0

        document(manifestFile.absolutePath).use { doc ->
            val root = doc.documentElement
            val application = root.childrenNamed("application").firstOrNull()

            if (shouldRevoke) {
                removedPerms = root.stripPermissionsWhere { it in TRACKING_PERMISSIONS }
            }

            if (application != null) {
                if (shouldDisableProviders) {
                    val targetProviders = if (shouldDisableFirebase) {
                        TELEMETRY_PROVIDERS + FIREBASE_INIT_PROVIDER
                    } else {
                        TELEMETRY_PROVIDERS
                    }
                    disabledProvidersCount = application.disableComponentsWhere("provider") { it in targetProviders }
                }

                if (shouldDisableServices) {
                    disabledServicesCount = application.disableComponentsWhere("service") { it in TELEMETRY_SERVICES }
                }

                if (shouldDisableReceivers) {
                    disabledReceiversCount = application.disableComponentsWhere("receiver") { it in TELEMETRY_RECEIVERS }
                }

                if (shouldInjectOptOut) {
                    OPT_OUT_METADATA.forEach { (name, value) ->
                        application.setApplicationMetaData(name, value)
                    }
                    injectedFlagsCount = OPT_OUT_METADATA.size

                    removedRegistrarsCount = application.removeComponentDiscoveryRegistrarsWhere { name ->
                        name.contains("Analytics", ignoreCase = true) ||
                            name.contains("Crashlytics", ignoreCase = true) ||
                            name.contains("Perf", ignoreCase = true) ||
                            name.contains("Sessions", ignoreCase = true)
                    }
                }
            }
        }

        val totalDisabled = disabledProvidersCount + disabledServicesCount + disabledReceiversCount
        if (removedPerms.isEmpty() && totalDisabled == 0 && injectedFlagsCount == 0 && removedRegistrarsCount == 0) {
            println("[Universal Telemetry Neutralizer] AndroidManifest.xml is already clean (0 tracking elements found).")
            return@execute
        }

        val permNames = removedPerms.map { it.substringAfterLast('.') }.distinct()
        val permNote = if (removedPerms.isNotEmpty()) "revoked ${removedPerms.size} permission(s) (${permNames.joinToString(", ")})" else "0 permissions revoked"
        val regNote = if (removedRegistrarsCount > 0) ", removed $removedRegistrarsCount discovery registrar(s)" else ""
        println("[Universal Telemetry Neutralizer] $permNote, disabled $totalDisabled component(s), injected $injectedFlagsCount opt-out flag(s)$regNote.")
    }
}
