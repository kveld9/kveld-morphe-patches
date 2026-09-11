package app.morphe.patches.hevy

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

@Suppress("unused")
val hevyBatteryOptimizationPatch = resourcePatch(
    name = "Hevy Battery Optimization & Background Sync Killer",
    description = "Disables background WorkManager alarms, periodic job schedulers, Firebase messaging wakeups, and DataTransport schedulers to eliminate battery drain in sleep.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_HEVY)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Hevy Battery Optimization] AndroidManifest.xml not found - skipping component neutralization.")
            return@execute
        }

        val disabledComponents = setOf(
            // 1. AndroidX WorkManager Alarm & Job Services
            "androidx.work.impl.background.systemalarm.SystemAlarmService",
            "androidx.work.impl.background.systemjob.SystemJobService",
            "androidx.work.impl.foreground.SystemForegroundService",
            "androidx.work.impl.utils.ForceStopRunnable\$BroadcastReceiver",
            "androidx.work.impl.background.systemalarm.ConstraintProxy\$BatteryChargingProxy",
            "androidx.work.impl.background.systemalarm.ConstraintProxy\$BatteryNotLowProxy",
            "androidx.work.impl.background.systemalarm.ConstraintProxy\$StorageNotLowProxy",
            "androidx.work.impl.background.systemalarm.ConstraintProxy\$NetworkStateProxy",
            "androidx.work.impl.background.systemalarm.RescheduleReceiver",
            "androidx.work.impl.background.systemalarm.ConstraintProxyUpdateReceiver",
            "androidx.work.impl.diagnostics.DiagnosticsReceiver",

            // 2. Firebase & DataTransport Background Wakeups
            "com.hevy.notifications.HevyFirebaseMessagingReceiver",
            "com.hevy.notifications.HevyFirebaseMessagingService",
            "com.google.firebase.iid.FirebaseInstanceIdReceiver",
            "com.google.firebase.messaging.FirebaseMessagingService",
            "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
            "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
            "androidx.profileinstaller.ProfileInstallReceiver",
        )

        var neutralizedCount = 0

        document(manifestFile.absolutePath).use { doc ->
            val tags = listOf("service", "receiver", "provider")
            for (tag in tags) {
                val elements = doc.getElementsByTagName(tag)
                for (i in 0 until elements.length) {
                    val elem = elements.item(i) as? Element ?: continue
                    val name = elem.getAttribute("android:name")
                    if (name in disabledComponents) {
                        elem.setAttribute("android:enabled", "false")
                        neutralizedCount++
                    }
                }
            }
        }

        println("[Hevy Battery Optimization] Neutralized $neutralizedCount background services, receivers, and wakeups in AndroidManifest.xml")
    }
}
