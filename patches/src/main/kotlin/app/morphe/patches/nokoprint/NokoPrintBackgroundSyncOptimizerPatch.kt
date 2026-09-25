package app.morphe.patches.nokoprint

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val nokoPrintBackgroundSyncResourcePatch = resourcePatch(
    name = "Background Sync Manifest Hardening",
    description = "Disables WorkManager alarms, constraint proxies, diagnostic receivers, and non-essential background schedulers.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Background Sync Optimizer] AndroidManifest.xml not found - skipping.")
            return@execute
        }

        val disabledComponents = setOf(
            "androidx.work.impl.background.systemalarm.SystemAlarmService",
            "androidx.work.impl.background.systemalarm.ConstraintProxy\$BatteryChargingProxy",
            "androidx.work.impl.background.systemalarm.ConstraintProxy\$BatteryNotLowProxy",
            "androidx.work.impl.background.systemalarm.ConstraintProxy\$StorageNotLowProxy",
            "androidx.work.impl.background.systemalarm.ConstraintProxy\$NetworkStateProxy",
            "androidx.work.impl.background.systemalarm.RescheduleReceiver",
            "androidx.work.impl.background.systemalarm.ConstraintProxyUpdateReceiver",
            "androidx.work.impl.diagnostics.DiagnosticsReceiver",
            "androidx.profileinstaller.ProfileInstallReceiver",
            "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
            "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
            "androidx.work.impl.foreground.SystemForegroundService",
            "androidx.work.impl.background.systemjob.SystemJobService",
            "androidx.work.impl.utils.ForceStopRunnable\$BroadcastReceiver",
            "com.google.android.datatransport.runtime.backends.TransportBackendDiscovery",
        )

        var neutralizedCount = 0

        document(manifestFile.absolutePath).use { doc ->
            val tags = listOf("service", "receiver")
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

            // Remove WorkManagerInitializer from androidx.startup.InitializationProvider
            val metaDataNodes = doc.getElementsByTagName("meta-data")
            val metaToRemove = mutableListOf<Element>()
            for (i in 0 until metaDataNodes.length) {
                val elem = metaDataNodes.item(i) as? Element ?: continue
                val name = elem.getAttribute("android:name")
                if (name == "androidx.work.WorkManagerInitializer") {
                    metaToRemove.add(elem)
                }
            }
            metaToRemove.forEach { it.parentNode?.removeChild(it) }
        }

        println("[Background Sync Optimizer] Neutralized $neutralizedCount background sync components.")
    }
}

@Suppress("unused")
val nokoPrintBackgroundSyncOptimizerPatch = bytecodePatch(
    name = "Background Sync Optimizer",
    description = "Neutralizes background WorkManager constraint tasks and diagnostic wakelocks.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)
    dependsOn(nokoPrintBackgroundSyncResourcePatch)

    execute {
        // Neutralize WorkManagerInitializer create(Context)Object safely returning null
        Fingerprint(
            definingClass = "Landroidx/work/WorkManagerInitializer;",
            name = "create",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Ljava/lang/Object;",
        ).method.apply {
            addInstructions(0, "const/4 v0, 0\nreturn-object v0")
            println("[Background Sync Optimizer] Neutralized WorkManagerInitializer.create")
        }
    }
}
