package app.morphe.patches.hevy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val hevyBatteryResourcePatch = resourcePatch(
    name = "Hevy Battery & Memory Resource Hardening",
    description = "Disables background wakeups, Play Billing components, removes largeHeap, and strips billing queries in AndroidManifest.xml.",
    default = false,
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

            // 3. Google Play & Amazon Billing Activities
            "com.android.billingclient.api.ProxyBillingActivity",
            "com.android.billingclient.api.ProxyBillingActivityV2",
            "com.revenuecat.purchases.amazon.purchasing.ProxyAmazonBillingActivity",
        )

        var neutralizedCount = 0
        var removedBillingQueries = 0
        var removedBillingPermissions = 0
        var removedLargeHeap = false

        document(manifestFile.absolutePath).use { doc ->
            // 1. Disable background services, receivers, providers, and billing activities
            val tags = listOf("service", "receiver", "provider", "activity")
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

            // 2. Strip com.android.vending.BILLING permission
            val usesPerms = doc.getElementsByTagName("uses-permission")
            val permsToRemove = mutableListOf<Element>()
            for (i in 0 until usesPerms.length) {
                val elem = usesPerms.item(i) as? Element ?: continue
                if (elem.getAttribute("android:name") == "com.android.vending.BILLING") {
                    permsToRemove.add(elem)
                }
            }
            permsToRemove.forEach {
                it.parentNode?.removeChild(it)
                removedBillingPermissions++
            }

            // 3. Strip billing queries intent actions from <queries>
            val queriesElements = doc.getElementsByTagName("queries")
            for (i in 0 until queriesElements.length) {
                val qElem = queriesElements.item(i) as? Element ?: continue
                val intents = qElem.getElementsByTagName("intent")
                val intentsToRemove = mutableListOf<Element>()
                for (j in 0 until intents.length) {
                    val intentElem = intents.item(j) as? Element ?: continue
                    val actions = intentElem.getElementsByTagName("action")
                    for (k in 0 until actions.length) {
                        val actionElem = actions.item(k) as? Element ?: continue
                        val actionName = actionElem.getAttribute("android:name")
                        if (actionName.contains("billing", ignoreCase = true) ||
                            actionName.contains("InAppBillingService", ignoreCase = true)
                        ) {
                            intentsToRemove.add(intentElem)
                            break
                        }
                    }
                }
                intentsToRemove.forEach {
                    it.parentNode?.removeChild(it)
                    removedBillingQueries++
                }
            }

            // 4. Remove android:largeHeap="true" to enforce regular ART Garbage Collection
            val appElements = doc.getElementsByTagName("application")
            for (i in 0 until appElements.length) {
                val appElem = appElements.item(i) as? Element ?: continue
                if (appElem.hasAttribute("android:largeHeap")) {
                    appElem.removeAttribute("android:largeHeap")
                    removedLargeHeap = true
                }
            }
        }

        val largeHeapMsg = if (removedLargeHeap) ", removed android:largeHeap" else ""
        println("[Hevy Battery Optimization] Neutralized $neutralizedCount components, stripped $removedBillingPermissions billing perms, pruned $removedBillingQueries billing queries$largeHeapMsg in AndroidManifest.xml")
    }
}

@Suppress("unused")
val hevyBatteryOptimizationPatch = bytecodePatch(
    name = "Hevy Battery Optimization & Background Sync Killer",
    description = "Disables background WorkManager alarms, periodic job schedulers, Google Play Billing IPC (~89 MB RAM), and removes largeHeap to force aggressive Garbage Collection.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_HEVY)
    dependsOn(hevyBatteryResourcePatch)

    execute {
        // Stub BillingClientImpl.startConnection to eliminate Play Store IPC / bound service
        val billingFp = Fingerprint(
            definingClass = "Lcom/android/billingclient/api/BillingClientImpl;",
            name = "startConnection",
            parameters = listOf("Lcom/android/billingclient/api/BillingClientStateListener;"),
        )
        billingFp.method.addInstructions(0, "return-void")
        println("[Hevy Battery Optimization] Stubbed BillingClientImpl.startConnection to eliminate Play Store background IPC.")
    }
}
