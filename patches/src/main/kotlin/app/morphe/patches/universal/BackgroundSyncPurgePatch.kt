package app.morphe.patches.universal

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.ANDROID_XML_NAMESPACE
import app.morphe.patches.shared.getAttributeValue
import org.w3c.dom.Document
import org.w3c.dom.Element

private val BOOT_AND_WAKEUP_ACTIONS = setOf(
    "android.intent.action.BOOT_COMPLETED",
    "android.intent.action.LOCKED_BOOT_COMPLETED",
    "android.intent.action.QUICKBOOT_POWERON",
    "com.htc.intent.action.QUICKBOOT_POWERON",
    "android.intent.action.REBOOT",
    "android.intent.action.MY_PACKAGE_REPLACED",
    "android.intent.action.PACKAGE_REPLACED",
    "android.intent.action.PACKAGE_RESTARTED",
)

private val BACKGROUND_SYNC_SERVICES = setOf(
    "androidx.work.impl.background.systemalarm.SystemAlarmService",
    "androidx.work.impl.background.systemjob.SystemJobService",
    "androidx.work.impl.foreground.SystemForegroundService",
    "androidx.work.multiprocess.RemoteWorkManagerService",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
    "com.google.android.datatransport.runtime.backends.TransportBackendDiscovery",
)

private val BACKGROUND_SYNC_RECEIVERS = setOf(
    "androidx.work.impl.utils.ForceStopRunnable\$BroadcastReceiver",
    "androidx.work.impl.background.systemalarm.ConstraintProxy\$BatteryChargingProxy",
    "androidx.work.impl.background.systemalarm.ConstraintProxy\$BatteryNotLowProxy",
    "androidx.work.impl.background.systemalarm.ConstraintProxy\$StorageNotLowProxy",
    "androidx.work.impl.background.systemalarm.ConstraintProxy\$NetworkStateProxy",
    "androidx.work.impl.background.systemalarm.RescheduleReceiver",
    "androidx.work.impl.background.systemalarm.ConstraintProxyUpdateReceiver",
    "androidx.work.impl.diagnostics.DiagnosticsReceiver",
    "androidx.profileinstaller.ProfileInstallReceiver",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
)

private fun disableElement(element: Element): Boolean {
    val current = getAttributeValue(element, "enabled")
    if (current.equals("false", ignoreCase = true)) return false
    element.removeAttribute("android:enabled")
    element.setAttributeNS(ANDROID_XML_NAMESPACE, "android:enabled", "false")
    return true
}

private fun hasBootAction(receiver: Element): Boolean {
    val intentFilters = receiver.getElementsByTagName("intent-filter")
    for (i in 0 until intentFilters.length) {
        val filter = intentFilters.item(i) as? Element ?: continue
        val actions = filter.getElementsByTagName("action")
        for (j in 0 until actions.length) {
            val action = actions.item(j) as? Element ?: continue
            val actionName = getAttributeValue(action, "name")
            if (actionName in BOOT_AND_WAKEUP_ACTIONS) {
                return true
            }
        }
    }
    return false
}

private fun stripPermissions(doc: Document, permissionsToStrip: Set<String>): List<String> {
    val removed = mutableListOf<String>()
    val permissionTags = listOf("uses-permission", "uses-permission-sdk-23")
    for (tag in permissionTags) {
        val nodes = doc.getElementsByTagName(tag)
        val toRemove = mutableListOf<Pair<Element, String>>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? Element ?: continue
            val name = getAttributeValue(element, "name")
            if (name in permissionsToStrip) {
                toRemove.add(element to name)
            }
        }
        for ((element, name) in toRemove) {
            element.parentNode?.removeChild(element)
            removed.add(name)
        }
    }
    return removed
}

private fun disableReceivers(
    doc: Document,
    targetBootActions: Boolean,
    targetSyncReceivers: Boolean,
): Int {
    var disabledCount = 0
    val receivers = doc.getElementsByTagName("receiver")
    for (i in 0 until receivers.length) {
        val receiver = receivers.item(i) as? Element ?: continue
        val name = getAttributeValue(receiver, "name")

        val matchSync = targetSyncReceivers && name in BACKGROUND_SYNC_RECEIVERS
        val matchBoot = targetBootActions && hasBootAction(receiver)

        if (matchSync || matchBoot) {
            if (disableElement(receiver)) {
                disabledCount++
            }
        }
    }
    return disabledCount
}

private fun disableServices(doc: Document): Int {
    var disabledCount = 0
    val services = doc.getElementsByTagName("service")
    for (i in 0 until services.length) {
        val service = services.item(i) as? Element ?: continue
        val name = getAttributeValue(service, "name")
        if (name in BACKGROUND_SYNC_SERVICES) {
            if (disableElement(service)) {
                disabledCount++
            }
        }
    }
    return disabledCount
}

@Suppress("unused")
val backgroundSyncPurgePatch = resourcePatch(
    name = "Stop Background Sync (Universal)",
    description = "Stops background sync schedulers and boot wakeups.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val stripBootPermission by booleanOption(
        key = "stripBootPermission",
        default = true,
        title = "Strip Boot Permission",
        description = "Removes startup boot permissions from the manifest.",
        required = false,
    )

    val disableBootReceivers by booleanOption(
        key = "disableBootReceivers",
        default = true,
        title = "Disable Boot Receivers",
        description = "Disables broadcast receivers that launch the app on device startup (BOOT_COMPLETED, LOCKED_BOOT_COMPLETED, QUICKBOOT_POWERON, REBOOT, MY_PACKAGE_REPLACED and PACKAGE_REPLACED/RESTARTED). Disables the whole receiver, including its non-boot filters.",
        required = false,
    )

    val disableWorkManager by booleanOption(
        key = "disableWorkManager",
        default = true,
        title = "Disable Background Schedulers",
        description = "Disables WorkManager and background job scheduler services.",
        required = false,
    )

    val stripWakeLock by booleanOption(
        key = "stripWakeLock",
        default = false,
        title = "Strip Wake Lock Permission",
        description = "Removes wake-lock permissions (may interrupt background downloads).",
        required = false,
    )

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Stop Background Sync (Universal)] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        val removeBootPerm = stripBootPermission ?: true
        val removeWakeLock = stripWakeLock ?: false
        val shouldDisableBoot = disableBootReceivers ?: true
        val shouldDisableWorkManager = disableWorkManager ?: true

        val permissionsToStrip = mutableSetOf<String>()
        if (removeBootPerm) permissionsToStrip.add("android.permission.RECEIVE_BOOT_COMPLETED")
        if (removeWakeLock) permissionsToStrip.add("android.permission.WAKE_LOCK")

        var removedPerms: List<String> = emptyList()
        var disabledReceiversCount = 0
        var disabledServicesCount = 0

        document(manifestFile.absolutePath).use { doc ->
            if (permissionsToStrip.isNotEmpty()) {
                removedPerms = stripPermissions(doc, permissionsToStrip)
            }
            disabledReceiversCount = disableReceivers(
                doc = doc,
                targetBootActions = shouldDisableBoot,
                targetSyncReceivers = shouldDisableWorkManager,
            )
            if (shouldDisableWorkManager) {
                disabledServicesCount = disableServices(doc)
            }
        }

        if (removedPerms.isEmpty() && disabledReceiversCount == 0 && disabledServicesCount == 0) {
            println("[Stop Background Sync (Universal)] AndroidManifest.xml has no background wakeups or boot receivers.")
            return@execute
        }

        val permNames = removedPerms.map { it.substringAfterLast('.') }.distinct()
        val permNote = if (removedPerms.isNotEmpty()) "revoked ${removedPerms.size} permission(s) (${permNames.joinToString(", ")})" else "0 permissions revoked"
        println("[Stop Background Sync (Universal)] $permNote, disabled $disabledReceiversCount receiver(s) and $disabledServicesCount service(s).")
    }
}
