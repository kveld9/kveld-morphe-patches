package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.childrenNamed
import app.morphe.patches.shared.disableComponentsWhere
import app.morphe.patches.shared.removeComponentDiscoveryRegistrarsWhere
import app.morphe.patches.shared.setApplicationMetaData
import app.morphe.patches.shared.stripPermissionsWhere

private val TRACKING_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
)

private val BLOCKED_COMPONENTS = setOf(
    "com.ss.android.common.applog.HotsoonReceiver",
    "com.appsflyer.MultipleInstallBroadcastReceiver",
    "com.appsflyer.SingleInstallBroadcastReceiver",
    "com.bytedance.applog.collector.Collector",
    "com.google.android.gms.measurement.AppMeasurementReceiver",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
    "com.google.android.gms.measurement.AppMeasurementService",
    "com.google.android.gms.measurement.AppMeasurementJobService",
    "com.google.android.datatransport.runtime.backends.TransportBackendDiscovery",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
    "com.appsflyer.referrer.INSTALL_PROVIDER",
    "com.appsflyer.internal.platform_extension.PluginInfoContentProvider",
    "com.google.android.gms.measurement.AppMeasurementContentProvider",
)

private val OPT_OUT_METADATA = listOf(
    "firebase_analytics_collection_enabled" to "false",
    "firebase_analytics_collection_deactivated" to "true",
    "google_analytics_default_allow_analytics_storage" to "false",
    "google_analytics_default_allow_ad_storage" to "false",
    "google_analytics_default_allow_ad_user_data" to "false",
    "google_analytics_default_allow_ad_personalization_signals" to "false",
    "google_analytics_adid_collection_enabled" to "false",
    "appsflyer_data_collection_enabled" to "false",
    "com.facebook.sdk.AutoInitEnabled" to "false",
    "com.facebook.sdk.AutoLogAppEventsEnabled" to "false",
    "com.facebook.sdk.AdvertiserIDCollectionEnabled" to "false",
)

val telemetryManifestPurgePatch = resourcePatch(
    name = "Block Trackers (Setup)",
    description = "Removes tracking permissions and components.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Block Trackers (Setup)] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        var strippedPermsCount = 0
        var disabledComponentsCount = 0
        var removedRegistrarsCount = 0
        var injectedFlagsCount = 0

        document(manifestFile.absolutePath).use { doc ->
            val root = doc.documentElement
            val application = root.childrenNamed("application").firstOrNull()

            val strippedList = root.stripPermissionsWhere { it in TRACKING_PERMISSIONS }
            strippedPermsCount = strippedList.size

            if (application != null) {
                disabledComponentsCount = application.disableComponentsWhere { name ->
                    name in BLOCKED_COMPONENTS
                }

                removedRegistrarsCount = application.removeComponentDiscoveryRegistrarsWhere { name ->
                    name.contains("Analytics", ignoreCase = true) || name.contains("Transport", ignoreCase = true)
                }

                OPT_OUT_METADATA.forEach { (name, value) ->
                    application.setApplicationMetaData(name, value)
                    injectedFlagsCount++
                }
            }
        }

        println("[Block Trackers (Setup)] Stripped $strippedPermsCount permission(s), disabled $disabledComponentsCount component(s), removed $removedRegistrarsCount discovery registrar(s), injected $injectedFlagsCount opt-out metadata flag(s).")
    }
}
