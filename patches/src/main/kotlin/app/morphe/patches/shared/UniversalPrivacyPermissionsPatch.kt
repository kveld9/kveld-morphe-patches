package app.morphe.patches.shared

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val ANDROID_XML_NAMESPACE = "http://schemas.android.com/apk/res/android"
private val PERMISSION_TAGS = listOf("uses-permission", "uses-permission-sdk-23")

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
    name = "Universal Privacy Permissions Stripper",
    description = "Selectively strips sensitive privacy, sensor, and hardware permissions from AndroidManifest.xml via configurable boolean toggles.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)

    val stripNotifications by booleanOption(
        key = "stripNotifications",
        default = false,
        title = "Strip Notification Permission",
        description = "Remove POST_NOTIFICATIONS permission (Android 13+) from AndroidManifest.xml.",
        required = false,
    )

    val stripCamera by booleanOption(
        key = "stripCamera",
        default = false,
        title = "Strip Camera Permission",
        description = "Remove CAMERA permission from AndroidManifest.xml.",
        required = false,
    )

    val stripMicrophone by booleanOption(
        key = "stripMicrophone",
        default = false,
        title = "Strip Microphone Permissions",
        description = "Remove RECORD_AUDIO and audio capture permissions from AndroidManifest.xml.",
        required = false,
    )

    val stripMediaAndStorage by booleanOption(
        key = "stripMediaAndStorage",
        default = false,
        title = "Strip Storage & Media Permissions",
        description = "Remove external storage (READ/WRITE/MANAGE_EXTERNAL_STORAGE) and media permissions (READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO, ACCESS_MEDIA_LOCATION) from AndroidManifest.xml.",
        required = false,
    )

    val stripLocation by booleanOption(
        key = "stripLocation",
        default = false,
        title = "Strip Location Permissions",
        description = "Remove ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, and ACCESS_BACKGROUND_LOCATION permissions from AndroidManifest.xml.",
        required = false,
    )

    val stripContacts by booleanOption(
        key = "stripContacts",
        default = false,
        title = "Strip Contacts & Accounts Permissions",
        description = "Remove READ_CONTACTS, WRITE_CONTACTS, and GET_ACCOUNTS permissions from AndroidManifest.xml.",
        required = false,
    )

    val stripCalendar by booleanOption(
        key = "stripCalendar",
        default = false,
        title = "Strip Calendar Permissions",
        description = "Remove READ_CALENDAR and WRITE_CALENDAR permissions from AndroidManifest.xml.",
        required = false,
    )

    val stripNearbyDevices by booleanOption(
        key = "stripNearbyDevices",
        default = false,
        title = "Strip Nearby Devices Permissions",
        description = "Remove Bluetooth scanning/connection and nearby Wi-Fi permissions (BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE, NEARBY_WIFI_DEVICES, UWB_RANGING) from AndroidManifest.xml.",
        required = false,
    )

    val stripSensors by booleanOption(
        key = "stripSensors",
        default = false,
        title = "Strip Body Sensors Permissions",
        description = "Remove BODY_SENSORS, BODY_SENSORS_BACKGROUND, and ACTIVITY_RECOGNITION permissions from AndroidManifest.xml.",
        required = false,
    )

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Universal Privacy Permissions Stripper] Skipped: AndroidManifest.xml not found.")
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
        )

        if (blockedPermissions.isEmpty()) {
            println("[Universal Privacy Permissions Stripper] Skipped: No privacy permission categories selected to strip.")
            return@execute
        }

        var removedList: List<String> = emptyList()
        document(manifestFile.absolutePath).use { doc ->
            removedList = stripPermissionsFromManifest(doc, blockedPermissions)
        }

        if (removedList.isEmpty()) {
            println("[Universal Privacy Permissions Stripper] No matching permissions found in AndroidManifest.xml.")
            return@execute
        }

        val shortNames = removedList.map { it.substringAfterLast('.') }.distinct()
        println("[Universal Privacy Permissions Stripper] Stripped ${removedList.size} permission(s) from AndroidManifest.xml: ${shortNames.joinToString(", ")}.")
    }
}
