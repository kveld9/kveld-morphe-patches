package app.morphe.patches.universal

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.ANDROID_XML_NAMESPACE
import app.morphe.patches.shared.PERMISSION_TAGS
import org.w3c.dom.Document
import org.w3c.dom.Element

private val NOTIFICATION_PERMISSIONS = setOf(
    "android.permission.POST_NOTIFICATIONS",
)

private val CAMERA_PERMISSIONS = setOf(
    "android.permission.CAMERA",
)

private val MICROPHONE_PERMISSIONS = setOf(
    "android.permission.RECORD_AUDIO",
    "android.permission.CAPTURE_AUDIO_OUTPUT",
)

private val MEDIA_STORAGE_PERMISSIONS = setOf(
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
    "android.permission.ACCESS_MEDIA_LOCATION",
)

private val LOCATION_PERMISSIONS = setOf(
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_BACKGROUND_LOCATION",
)

private val CONTACTS_PERMISSIONS = setOf(
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
    "android.permission.GET_ACCOUNTS",
)

private val CALENDAR_PERMISSIONS = setOf(
    "android.permission.READ_CALENDAR",
    "android.permission.WRITE_CALENDAR",
)

private val NEARBY_DEVICES_PERMISSIONS = setOf(
    "android.permission.BLUETOOTH_SCAN",
    "android.permission.BLUETOOTH_CONNECT",
    "android.permission.BLUETOOTH_ADVERTISE",
    "android.permission.NEARBY_WIFI_DEVICES",
    "android.permission.UWB_RANGING",
)

private val SENSORS_PERMISSIONS = setOf(
    "android.permission.BODY_SENSORS",
    "android.permission.BODY_SENSORS_BACKGROUND",
    "android.permission.ACTIVITY_RECOGNITION",
)

private val INSTALLED_APPS_PERMISSIONS = setOf(
    "android.permission.QUERY_ALL_PACKAGES",
)

private fun buildBlockedPermissions(
    stripNotifications: Boolean,
    stripCamera: Boolean,
    stripMicrophone: Boolean,
    stripMediaAndStorage: Boolean,
    stripLocation: Boolean,
    stripContacts: Boolean,
    stripCalendar: Boolean,
    stripNearbyDevices: Boolean,
    stripSensors: Boolean,
    stripInstalledApps: Boolean,
): Set<String> {
    val blocked = mutableSetOf<String>()
    if (stripNotifications) blocked.addAll(NOTIFICATION_PERMISSIONS)
    if (stripCamera) blocked.addAll(CAMERA_PERMISSIONS)
    if (stripMicrophone) blocked.addAll(MICROPHONE_PERMISSIONS)
    if (stripMediaAndStorage) blocked.addAll(MEDIA_STORAGE_PERMISSIONS)
    if (stripLocation) blocked.addAll(LOCATION_PERMISSIONS)
    if (stripContacts) blocked.addAll(CONTACTS_PERMISSIONS)
    if (stripCalendar) blocked.addAll(CALENDAR_PERMISSIONS)
    if (stripNearbyDevices) blocked.addAll(NEARBY_DEVICES_PERMISSIONS)
    if (stripSensors) blocked.addAll(SENSORS_PERMISSIONS)
    if (stripInstalledApps) blocked.addAll(INSTALLED_APPS_PERMISSIONS)
    return blocked
}

private fun getPermissionName(element: Element): String {
    val name = element.getAttribute("android:name")
    if (name.isNotBlank()) return name.trim()
    val nameNs = element.getAttributeNS(ANDROID_XML_NAMESPACE, "name")
    if (nameNs.isNotBlank()) return nameNs.trim()
    return element.getAttribute("name").trim()
}

private fun stripPermissionsFromManifest(
    doc: Document,
    blockedPermissions: Set<String>,
): List<String> {
    val removed = mutableListOf<String>()

    for (tag in PERMISSION_TAGS) {
        val nodes = doc.getElementsByTagName(tag)
        val elementsToRemove = mutableListOf<Pair<Element, String>>()

        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? Element ?: continue
            val permissionName = getPermissionName(element)
            if (permissionName in blockedPermissions) {
                elementsToRemove.add(element to permissionName)
            }
        }

        for ((element, name) in elementsToRemove) {
            val parent = element.parentNode
            if (parent != null) {
                parent.removeChild(element)
                removed.add(name)
            }
        }
    }

    return removed
}

@Suppress("unused")
val universalPrivacyPermissionsPatch = resourcePatch(
    name = "Strip Sensitive Permissions (Universal)",
    description = "Removes sensitive privacy and hardware permissions you select.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)

    val stripNotifications by booleanOption(
        key = "stripNotifications",
        default = false,
        title = "Notifications",
        description = "Removes notification permissions (Android 13+).",
        required = false,
    )

    val stripCamera by booleanOption(
        key = "stripCamera",
        default = false,
        title = "Camera",
        description = "Removes camera access permissions.",
        required = false,
    )

    val stripMicrophone by booleanOption(
        key = "stripMicrophone",
        default = false,
        title = "Microphone",
        description = "Removes audio recording and microphone permissions.",
        required = false,
    )

    val stripMediaAndStorage by booleanOption(
        key = "stripMediaAndStorage",
        default = false,
        title = "Photos & files",
        description = "Removes photo, video, and storage access permissions.",
        required = false,
    )

    val stripLocation by booleanOption(
        key = "stripLocation",
        default = false,
        title = "Location",
        description = "Removes fine, coarse, and background location permissions.",
        required = false,
    )

    val stripContacts by booleanOption(
        key = "stripContacts",
        default = false,
        title = "Contacts",
        description = "Removes contacts and account reading permissions.",
        required = false,
    )

    val stripCalendar by booleanOption(
        key = "stripCalendar",
        default = false,
        title = "Calendar",
        description = "Removes calendar reading and writing permissions.",
        required = false,
    )

    val stripNearbyDevices by booleanOption(
        key = "stripNearbyDevices",
        default = false,
        title = "Nearby devices",
        description = "Removes Bluetooth scanning and nearby device permissions.",
        required = false,
    )

    val stripSensors by booleanOption(
        key = "stripSensors",
        default = false,
        title = "Body sensors",
        description = "Removes body sensor and activity recognition permissions.",
        required = false,
    )

    val stripInstalledApps by booleanOption(
        key = "stripInstalledApps",
        default = false,
        title = "Installed apps",
        description = "Hides other installed apps from this app (QUERY_ALL_PACKAGES).",
        required = false,
    )

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Strip Sensitive Permissions (Universal)] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        val removeNotifications = stripNotifications ?: false
        val removeCamera = stripCamera ?: false
        val removeMicrophone = stripMicrophone ?: false
        val removeMediaAndStorage = stripMediaAndStorage ?: false
        val removeLocation = stripLocation ?: false
        val removeContacts = stripContacts ?: false
        val removeCalendar = stripCalendar ?: false
        val removeNearbyDevices = stripNearbyDevices ?: false
        val removeSensors = stripSensors ?: false
        val removeInstalledApps = stripInstalledApps ?: false

        val blockedPermissions = buildBlockedPermissions(
            stripNotifications = removeNotifications,
            stripCamera = removeCamera,
            stripMicrophone = removeMicrophone,
            stripMediaAndStorage = removeMediaAndStorage,
            stripLocation = removeLocation,
            stripContacts = removeContacts,
            stripCalendar = removeCalendar,
            stripNearbyDevices = removeNearbyDevices,
            stripSensors = removeSensors,
            stripInstalledApps = removeInstalledApps,
        )

        if (blockedPermissions.isEmpty()) {
            println("[Strip Sensitive Permissions (Universal)] Skipped: No privacy permission categories selected to strip.")
            return@execute
        }

        var removedList: List<String> = emptyList()
        document(manifestFile.absolutePath).use { doc ->
            removedList = stripPermissionsFromManifest(doc, blockedPermissions)
        }

        if (removedList.isEmpty()) {
            println("[Strip Sensitive Permissions (Universal)] No matching permissions found in AndroidManifest.xml.")
            return@execute
        }

        val shortNames = removedList.map { it.substringAfterLast('.') }.distinct()
        println("[Strip Sensitive Permissions (Universal)] Stripped ${removedList.size} permission(s) from AndroidManifest.xml: ${shortNames.joinToString(", ")}.")
    }
}
