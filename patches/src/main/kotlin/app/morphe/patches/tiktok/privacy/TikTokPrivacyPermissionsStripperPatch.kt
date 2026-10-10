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
    name = "TikTok Privacy Permissions Stripper",
    description = "Selectively strips sensitive privacy, sensor, hardware, and tracking permissions from AndroidManifest.xml via configurable boolean toggles.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    val stripNotifications by booleanOption(
        key = "stripNotifications",
        default = false,
        title = "Strip Notification Permission",
        description = "Remove POST_NOTIFICATIONS permission (Android 13+). Notification channels cannot dispatch push alerts.",
        required = false,
    )

    val stripCamera by booleanOption(
        key = "stripCamera",
        default = false,
        title = "Strip Camera Permission",
        description = "Remove CAMERA permission. WARNING: Breaks camera recording, photo capturing, QR scanning, LIVE broadcasting, and video creation.",
        required = false,
    )

    val stripMicrophone by booleanOption(
        key = "stripMicrophone",
        default = false,
        title = "Strip Microphone Permissions",
        description = "Remove RECORD_AUDIO, FOREGROUND_SERVICE_MICROPHONE, and FOREGROUND_SERVICE_CAMERA permissions. WARNING: Breaks video voice recording, LIVE audio broadcasting, audio comments, and voice/video calling.",
        required = false,
    )

    val stripStorageMedia by booleanOption(
        key = "stripStorageMedia",
        default = false,
        title = "Strip Storage & Media Permissions",
        description = "Remove external storage (READ/WRITE_EXTERNAL_STORAGE) and media permissions (READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO, READ_MEDIA_VISUAL_USER_SELECTED, ACCESS_MEDIA_LOCATION). WARNING: Breaks local gallery picker, drafts, and video/photo saving.",
        required = false,
    )

    val stripBluetooth by booleanOption(
        key = "stripBluetooth",
        default = true,
        title = "Strip Bluetooth Permissions",
        description = "Remove BLUETOOTH, BLUETOOTH_SCAN, BLUETOOTH_CONNECT, and BLUETOOTH_ADVERTISE permissions. WARNING: Breaks Bluetooth audio accessories, wireless headphones low-latency sync, Cast devices, and external remote controls.",
        required = false,
    )

    val stripNfc by booleanOption(
        key = "stripNfc",
        default = true,
        title = "Strip NFC Permission",
        description = "Remove NFC permission. WARNING: Disables NFC tag interactions and NFC-based hardware authentication tokens.",
        required = false,
    )

    val stripBiometric by booleanOption(
        key = "stripBiometric",
        default = true,
        title = "Strip Biometric Permissions",
        description = "Remove USE_BIOMETRIC and USE_FINGERPRINT permissions. WARNING: Breaks fingerprint/face biometric unlocking, passkeys, and biometric payment authorization.",
        required = false,
    )

    val stripForegroundServices by booleanOption(
        key = "stripForegroundServices",
        default = false,
        title = "Strip Foreground Service Permissions",
        description = "Remove generic FOREGROUND_SERVICE and specialized types (DATA_SYNC, MEDIA_PLAYBACK, MEDIA_PROJECTION, PHONE_CALL). WARNING: HIGH RISK. Breaks background video uploads, offline caching, media playback notification services, screen sharing, and VoIP calls in background.",
        required = false,
    )

    val stripSystemAlertWindow by booleanOption(
        key = "stripSystemAlertWindow",
        default = false,
        title = "Strip System Alert Window Permission",
        description = "Remove SYSTEM_ALERT_WINDOW permission. WARNING: Breaks Picture-in-Picture overlay window outside the app, floating mini-player, and overlay notification heads.",
        required = false,
    )

    val stripWakeLock by booleanOption(
        key = "stripWakeLock",
        default = false,
        title = "Strip Wake Lock Permission",
        description = "Remove WAKE_LOCK permission. WARNING: Device CPU may sleep during media playback or long video uploads/downloads when screen turns off, suspending progress.",
        required = false,
    )

    val stripScreenshotDetection by booleanOption(
        key = "stripScreenshotDetection",
        default = true,
        title = "Strip Screenshot & Recording Detection Permissions",
        description = "Remove DETECT_SCREEN_CAPTURE and DETECT_SCREEN_RECORDING permissions to neutralize system-level capture callbacks.",
        required = false,
    )

    val stripMiscHardware by booleanOption(
        key = "stripMiscHardware",
        default = false,
        title = "Strip Miscellaneous Hardware & System Permissions",
        description = "Remove VIBRATE, MODIFY_AUDIO_SETTINGS, MANAGE_OWN_CALLS, REORDER_TASKS, SET_WALLPAPER, and USE_FULL_SCREEN_INTENT permissions. WARNING: Disables haptic feedback vibration, volume adjustments, alarm priority intents, and live wallpaper export.",
        required = false,
    )

    val stripOemSignals by booleanOption(
        key = "stripOemSignals",
        default = true,
        title = "Strip OEM Diagnostic & Background Signals",
        description = "Remove OEM attribution, launcher badges, AICore bindings, and proprietary TikTok internal IPC permissions. WARNING: Removing DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION may impact runtime dynamic broadcast receivers.",
        required = false,
    )

    val stripPushDelivery by booleanOption(
        key = "stripPushDelivery",
        default = false,
        title = "Strip Push Delivery Permissions",
        description = "Remove Google C2DM/FCM and Amazon ADM push receiver permissions. WARNING: Breaks push notification delivery.",
        required = false,
    )

    val stripBilling by booleanOption(
        key = "stripBilling",
        default = false,
        title = "Strip In-App Billing Permission",
        description = "Remove Google Play in-app billing permission (com.android.vending.BILLING). WARNING: Breaks coin purchases and in-app transactions.",
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
