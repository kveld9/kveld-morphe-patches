package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.stripPermissionsWhere

private val NOTIFICATIONS_PERMISSIONS = setOf(
    "android.permission.POST_NOTIFICATIONS",
)

private val CAMERA_PERMISSIONS = setOf(
    "android.permission.CAMERA",
)

private val MICROPHONE_PERMISSIONS = setOf(
    "android.permission.RECORD_AUDIO",
    "android.permission.FOREGROUND_SERVICE_MICROPHONE",
    "android.permission.FOREGROUND_SERVICE_CAMERA",
)

private val STORAGE_MEDIA_PERMISSIONS = setOf(
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
    "android.permission.ACCESS_MEDIA_LOCATION",
)

private val BLUETOOTH_PERMISSIONS = setOf(
    "android.permission.BLUETOOTH",
    "android.permission.BLUETOOTH_SCAN",
    "android.permission.BLUETOOTH_CONNECT",
    "android.permission.BLUETOOTH_ADVERTISE",
)

private val NFC_PERMISSIONS = setOf(
    "android.permission.NFC",
)

private val BIOMETRIC_PERMISSIONS = setOf(
    "android.permission.USE_BIOMETRIC",
    "android.permission.USE_FINGERPRINT",
)

private val FOREGROUND_SERVICES_PERMISSIONS = setOf(
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
    "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
    "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION",
    "android.permission.FOREGROUND_SERVICE_PHONE_CALL",
)

private val SYSTEM_ALERT_WINDOW_PERMISSIONS = setOf(
    "android.permission.SYSTEM_ALERT_WINDOW",
)

private val WAKE_LOCK_PERMISSIONS = setOf(
    "android.permission.WAKE_LOCK",
)

private val SCREENSHOT_DETECTION_PERMISSIONS = setOf(
    "android.permission.DETECT_SCREEN_CAPTURE",
    "android.permission.DETECT_SCREEN_RECORDING",
)

private val MISC_HARDWARE_PERMISSIONS = setOf(
    "android.permission.VIBRATE",
    "android.permission.MODIFY_AUDIO_SETTINGS",
    "android.permission.MANAGE_OWN_CALLS",
    "android.permission.REORDER_TASKS",
    "android.permission.SET_WALLPAPER",
    "android.permission.USE_FULL_SCREEN_INTENT",
)

private val OEM_SIGNALS_PERMISSIONS = setOf(
    "com.huawei.appmarket.service.commondata.permission.GET_COMMON_DATA",
    "com.oplus.ocs.permission.third",
    "com.orange.update.permission.READ_ATTRIBUTION",
    "com.samsung.android.mapsagent.permission.READ_APP_INFO",
    "com.sec.android.provider.badge.permission.READ",
    "com.sec.android.provider.badge.permission.WRITE",
    "com.android.launcher.permission.READ_SETTINGS",
    "com.google.android.apps.aicore.service.BIND_SERVICE",
    "com.tiktok.manager.SYS_START_PERMISSION",
    "com.tiktok.preload.permission.IDENTIFY",
    "com.zhiliaoapp.musically.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
    "com.zhiliaoapp.musically.permission.PRF_INTERNAL",
    "com.zhiliaoapp.musically.permission.RECEIVE_ADM_MESSAGE",
    "com.zhiliao.musically.livewallpaper.permission.wallpaperplugin",
)

private val PUSH_DELIVERY_PERMISSIONS = setOf(
    "com.google.android.c2dm.permission.RECEIVE",
    "com.amazon.device.messaging.permission.RECEIVE",
)

private val BILLING_PERMISSIONS = setOf(
    "com.android.vending.BILLING",
)

private val PERMISSION_GROUPS = mapOf(
    "notifications" to NOTIFICATIONS_PERMISSIONS,
    "camera" to CAMERA_PERMISSIONS,
    "microphone" to MICROPHONE_PERMISSIONS,
    "storageMedia" to STORAGE_MEDIA_PERMISSIONS,
    "bluetooth" to BLUETOOTH_PERMISSIONS,
    "nfc" to NFC_PERMISSIONS,
    "biometric" to BIOMETRIC_PERMISSIONS,
    "foregroundServices" to FOREGROUND_SERVICES_PERMISSIONS,
    "systemAlertWindow" to SYSTEM_ALERT_WINDOW_PERMISSIONS,
    "wakeLock" to WAKE_LOCK_PERMISSIONS,
    "screenshotDetection" to SCREENSHOT_DETECTION_PERMISSIONS,
    "miscHardware" to MISC_HARDWARE_PERMISSIONS,
    "oemSignals" to OEM_SIGNALS_PERMISSIONS,
    "pushDelivery" to PUSH_DELIVERY_PERMISSIONS,
    "billing" to BILLING_PERMISSIONS,
)

private fun buildBlockedPermissions(toggleStates: Map<String, Boolean>): Set<String> {
    return toggleStates.filterValues { it }
        .flatMap { (key, _) -> PERMISSION_GROUPS[key] ?: emptySet() }
        .toSet()
}

@Suppress("unused")
val tikTokPrivacyPermissionsStripperPatch = resourcePatch(
    name = "Remove Permissions",
    description = "Removes permissions you pick below.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    val stripNotifications by booleanOption(
        key = "stripNotifications",
        default = false,
        title = "Notifications",
        description = "No push alerts (POST_NOTIFICATIONS, Android 13+).",
        required = false,
    )

    val stripCamera by booleanOption(
        key = "stripCamera",
        default = false,
        title = "Camera",
        description = "WARNING: breaks recording, photos, QR scan, lives and video creation.",
        required = false,
    )

    val stripMicrophone by booleanOption(
        key = "stripMicrophone",
        default = false,
        title = "Microphone",
        description = "WARNING: breaks voice recording, lives audio, comments and calls.",
        required = false,
    )

    val stripStorageMedia by booleanOption(
        key = "stripStorageMedia",
        default = false,
        title = "Photos & files",
        description = "WARNING: breaks gallery, drafts and saving photos/videos.",
        required = false,
    )

    val stripBluetooth by booleanOption(
        key = "stripBluetooth",
        default = true,
        title = "Bluetooth",
        description = "WARNING: breaks headphones, casting and remote controls.",
        required = false,
    )

    val stripNfc by booleanOption(
        key = "stripNfc",
        default = true,
        title = "NFC",
        description = "Disables tap-to-pay tags.",
        required = false,
    )

    val stripBiometric by booleanOption(
        key = "stripBiometric",
        default = true,
        title = "Fingerprint login",
        description = "WARNING: breaks fingerprint/face unlock, passkeys and payments.",
        required = false,
    )

    val stripForegroundServices by booleanOption(
        key = "stripForegroundServices",
        default = false,
        title = "Background services",
        description = "WARNING: HIGH RISK. Breaks background uploads, offline caching, playback services and calls.",
        required = false,
    )

    val stripSystemAlertWindow by booleanOption(
        key = "stripSystemAlertWindow",
        default = false,
        title = "Popups overlay",
        description = "WARNING: breaks mini-player, popups and overlay windows.",
        required = false,
    )

    val stripWakeLock by booleanOption(
        key = "stripWakeLock",
        default = false,
        title = "Keep awake",
        description = "WARNING: CPU may sleep during playback or long uploads with screen off.",
        required = false,
    )

    val stripScreenshotDetection by booleanOption(
        key = "stripScreenshotDetection",
        default = true,
        title = "Screenshot detection",
        description = "System will not report captures.",
        required = false,
    )

    val stripMiscHardware by booleanOption(
        key = "stripMiscHardware",
        default = false,
        title = "Vibration & more",
        description = "WARNING: disables vibration, volume adjustments and live wallpapers.",
        required = false,
    )

    val stripOemSignals by booleanOption(
        key = "stripOemSignals",
        default = true,
        title = "Phone signals",
        description = "Removes maker signals. WARNING: may affect dynamic receivers.",
        required = false,
    )

    val stripPushDelivery by booleanOption(
        key = "stripPushDelivery",
        default = false,
        title = "Push delivery",
        description = "WARNING: breaks push notifications.",
        required = false,
    )

    val stripBilling by booleanOption(
        key = "stripBilling",
        default = false,
        title = "Purchases",
        description = "WARNING: breaks coin purchases.",
        required = false,
    )

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[TikTok Privacy Permissions Stripper] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        val toggleStates = mapOf(
            "notifications" to (stripNotifications ?: false),
            "camera" to (stripCamera ?: false),
            "microphone" to (stripMicrophone ?: false),
            "storageMedia" to (stripStorageMedia ?: false),
            "bluetooth" to (stripBluetooth ?: true),
            "nfc" to (stripNfc ?: true),
            "biometric" to (stripBiometric ?: true),
            "foregroundServices" to (stripForegroundServices ?: false),
            "systemAlertWindow" to (stripSystemAlertWindow ?: false),
            "wakeLock" to (stripWakeLock ?: false),
            "screenshotDetection" to (stripScreenshotDetection ?: true),
            "miscHardware" to (stripMiscHardware ?: false),
            "oemSignals" to (stripOemSignals ?: true),
            "pushDelivery" to (stripPushDelivery ?: false),
            "billing" to (stripBilling ?: false),
        )

        val blockedPermissions = buildBlockedPermissions(toggleStates)
        if (blockedPermissions.isEmpty()) {
            println("[TikTok Privacy Permissions Stripper] Skipped: No permission categories selected to strip.")
            return@execute
        }

        var removedList: List<String> = emptyList()
        document(manifestFile.absolutePath).use { doc ->
            val root = doc.documentElement
            removedList = root.stripPermissionsWhere { it in blockedPermissions }
        }

        if (removedList.isEmpty()) {
            println("[TikTok Privacy Permissions Stripper] AndroidManifest.xml is already clean (0 matching permissions found).")
            return@execute
        }

        val shortNames = removedList.map { it.substringAfterLast('.') }.distinct()
        println("[TikTok Privacy Permissions Stripper] Stripped ${removedList.size} permission(s) from AndroidManifest.xml: ${shortNames.joinToString(", ")}.")
    }
}
