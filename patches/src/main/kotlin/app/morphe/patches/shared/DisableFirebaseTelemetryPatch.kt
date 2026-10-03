package app.morphe.patches.shared

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private val FIREBASE_OPT_OUT_METADATA = mapOf(
    "firebase_analytics_collection_enabled" to "false",
    "firebase_analytics_collection_deactivated" to "true",
    "firebase_crashlytics_collection_enabled" to "false",
    "firebase_performance_collection_enabled" to "false",
    "firebase_performance_collection_deactivated" to "true",
    "firebase_performance_logcat_enabled" to "false",
    "firebase_data_collection_default_enabled" to "false",
    "google_analytics_adid_collection_enabled" to "false",
    "google_analytics_deferred_deep_link_enabled" to "false",
)

private val FIREBASE_COMPONENTS_TO_DISABLE = arrayOf(
    "com.google.android.datatransport.runtime.backends.TransportBackendDiscovery",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
    "com.google.firebase.sessions.SessionLifecycleService",
)

@Suppress("unused")
val disableFirebaseTelemetryPatch = resourcePatch(
    name = "Disable Firebase Telemetry",
    description = "Disables Firebase telemetry, analytics, crashlytics, and performance collection flags, session services, and DataTransport sender entry points.",
    default = false,
) {
    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Disable Firebase Telemetry] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        document(manifestFile.absolutePath).use { document ->
            val application = document.documentElement.childrenNamed("application").firstOrNull() as? Element
            if (application == null) {
                println("[Disable Firebase Telemetry] Skipped: <application> tag not found.")
                return@use
            }

            FIREBASE_OPT_OUT_METADATA.forEach { (name, value) ->
                application.setApplicationMetaData(name, value)
            }

            val disabledComponents = application.disableComponentsByName(*FIREBASE_COMPONENTS_TO_DISABLE)
            val removedRegistrars = application.removeComponentDiscoveryRegistrarsWhere { name ->
                name.contains("Analytics", ignoreCase = true) ||
                    name.contains("Crashlytics", ignoreCase = true) ||
                    name.contains("Perf", ignoreCase = true) ||
                    name.contains("Sessions", ignoreCase = true)
            }

            println("[Disable Firebase Telemetry] Injected ${FIREBASE_OPT_OUT_METADATA.size} opt-out metadata flag(s), disabled $disabledComponents component(s), removed $removedRegistrars discovery registrar(s).")
        }
    }
}
