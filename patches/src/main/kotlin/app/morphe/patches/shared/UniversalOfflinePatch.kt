package app.morphe.patches.shared

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val ANDROID_XML_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val PERMISSION_INTERNET = "android.permission.INTERNET"

private val NETWORK_STATE_PERMISSIONS = setOf(
    "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.ACCESS_WIFI_STATE",
)

private val WIFI_CONTROL_PERMISSIONS = setOf(
    "android.permission.CHANGE_NETWORK_STATE",
    "android.permission.CHANGE_WIFI_STATE",
    "android.permission.CHANGE_WIFI_MULTICAST_STATE",
    "android.permission.NEARBY_WIFI_DEVICES",
    "android.permission.OVERRIDE_WIFI_CONFIG",
)

private val PUSH_PERMISSIONS = setOf(
    "com.google.android.c2dm.permission.RECEIVE",
)

private val GOOGLE_SERVICES_PERMISSIONS = setOf(
    "com.google.android.providers.gsf.permission.READ_GSERVICES",
    "android.permission.GET_ACCOUNTS",
)

private val PERMISSION_TAGS = listOf("uses-permission", "uses-permission-sdk-23")

private fun buildBlockedPermissions(
    stripNetworkState: Boolean,
    stripWifiControls: Boolean,
    stripPush: Boolean,
    stripGoogleServices: Boolean,
): Set<String> {
    val blocked = mutableSetOf(PERMISSION_INTERNET)
    if (stripNetworkState) blocked.addAll(NETWORK_STATE_PERMISSIONS)
    if (stripWifiControls) blocked.addAll(WIFI_CONTROL_PERMISSIONS)
    if (stripPush) blocked.addAll(PUSH_PERMISSIONS)
    if (stripGoogleServices) blocked.addAll(GOOGLE_SERVICES_PERMISSIONS)
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

private fun enforceCleartextBlock(doc: Document): Boolean {
    val appNodes = doc.getElementsByTagName("application")
    if (appNodes.length == 0) return false

    val appElement = appNodes.item(0) as? Element ?: return false
    val currentSetting = appElement.getAttribute("android:usesCleartextTraffic").trim()
    val currentSettingNs = appElement.getAttributeNS(ANDROID_XML_NAMESPACE, "usesCleartextTraffic").trim()
    if (currentSetting.equals("false", ignoreCase = true) || currentSettingNs.equals("false", ignoreCase = true)) {
        return false
    }

    appElement.setAttributeNS(ANDROID_XML_NAMESPACE, "android:usesCleartextTraffic", "false")
    return true
}

@Suppress("unused")
val universalOfflinePatch = resourcePatch(
    name = "Universal Offline Mode",
    description = "Forces offline execution across any application by revoking INTERNET and network permissions from AndroidManifest.xml and blocking cleartext HTTP traffic at the OS level.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val stripNetworkState by booleanOption(
        key = "stripNetworkState",
        default = false,
        title = "Strip Network State Permissions",
        description = "Also remove ACCESS_NETWORK_STATE and ACCESS_WIFI_STATE permissions. Default is false to prevent SecurityException crashes in apps that query network status without error handling.",
        required = false,
    )

    val stripWifiControls by booleanOption(
        key = "stripWifiControls",
        default = true,
        title = "Strip Wi-Fi Control Permissions",
        description = "Remove CHANGE_NETWORK_STATE, CHANGE_WIFI_STATE, CHANGE_WIFI_MULTICAST_STATE, and NEARBY_WIFI_DEVICES permissions.",
        required = false,
    )

    val stripPush by booleanOption(
        key = "stripPush",
        default = false,
        title = "Strip Push Notification Permissions",
        description = "Remove Google Cloud Messaging and Firebase Cloud Messaging push receiver permissions (com.google.android.c2dm.permission.RECEIVE).",
        required = false,
    )

    val stripGoogleServices by booleanOption(
        key = "stripGoogleServices",
        default = false,
        title = "Strip Google Services Sync Permissions",
        description = "Remove Google Services Framework and account sync permissions (com.google.android.providers.gsf.permission.READ_GSERVICES, android.permission.GET_ACCOUNTS).",
        required = false,
    )

    val blockCleartext by booleanOption(
        key = "blockCleartext",
        default = true,
        title = "Block Cleartext Traffic",
        description = "Enforce android:usesCleartextTraffic='false' on the application tag in AndroidManifest.xml.",
        required = false,
    )

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Universal Offline Mode] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        val removeNetworkState = stripNetworkState ?: false
        val removeWifiControls = stripWifiControls ?: true
        val removePush = stripPush ?: false
        val removeGoogleServices = stripGoogleServices ?: false
        val shouldBlockCleartext = blockCleartext ?: true

        val blockedPermissions = buildBlockedPermissions(
            stripNetworkState = removeNetworkState,
            stripWifiControls = removeWifiControls,
            stripPush = removePush,
            stripGoogleServices = removeGoogleServices,
        )

        var removedList: List<String> = emptyList()
        var cleartextBlocked = false

        document(manifestFile.absolutePath).use { doc ->
            removedList = stripPermissionsFromManifest(doc, blockedPermissions)
            if (shouldBlockCleartext) {
                cleartextBlocked = enforceCleartextBlock(doc)
            }
        }

        if (removedList.isEmpty() && !cleartextBlocked) {
            println("[Universal Offline Mode] No target network permissions found in AndroidManifest.xml (already offline).")
            return@execute
        }

        if (removedList.isEmpty()) {
            println("[Universal Offline Mode] Blocked cleartext HTTP traffic (0 network permissions present).")
            return@execute
        }

        val shortNames = removedList.map { it.substringAfterLast('.') }.distinct()
        val cleartextNote = if (cleartextBlocked) " + blocked cleartext HTTP" else ""
        println("[Universal Offline Mode] Stripped ${removedList.size} permission(s) (${shortNames.joinToString(", ")})$cleartextNote.")
    }
}
