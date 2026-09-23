package app.morphe.patches.shared

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val ANDROID_XML_NAMESPACE = "http://schemas.android.com/apk/res/android"

private val TRACKING_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
)

private val TELEMETRY_PROVIDERS = setOf(
    "com.google.android.gms.measurement.AppMeasurementContentProvider",
    "io.sentry.android.core.SentryInitProvider",
    "io.sentry.android.core.SentryPerformanceProvider",
    "com.facebook.internal.FacebookInitProvider",
    "com.flurry.android.agent.FlurryContentProvider",
    "io.branch.referral.BranchInitProvider",
    "com.appsflyer.internal.platform_extension.PluginInfoContentProvider",
    "com.google.firebase.perf.provider.FirebasePerfProvider",
)

private const val FIREBASE_INIT_PROVIDER = "com.google.firebase.provider.FirebaseInitProvider"

private val TELEMETRY_SERVICES = setOf(
    "com.google.android.gms.measurement.AppMeasurementService",
    "com.google.android.gms.measurement.AppMeasurementJobService",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
    "com.google.android.datatransport.runtime.backends.TransportBackendDiscovery",
    "com.appsflyer.internal.service.AFJobSchedulerService",
)

private val TELEMETRY_RECEIVERS = setOf(
    "com.google.android.gms.measurement.AppMeasurementReceiver",
    "com.google.android.gms.measurement.AppMeasurementInstallReferrerReceiver",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
    "com.adjust.sdk.AdjustReferrerReceiver",
    "com.appsflyer.SingleInstallBroadcastReceiver",
    "com.appsflyer.MultipleInstallBroadcastReceiver",
)

private val OPT_OUT_METADATA = listOf(
    "firebase_analytics_collection_enabled" to "false",
    "firebase_analytics_collection_deactivated" to "true",
    "google_analytics_adid_collection_enabled" to "false",
    "google_analytics_default_allow_ad_personalization_signals" to "false",
    "google_analytics_automatic_screen_reporting_enabled" to "false",
    "firebase_crashlytics_collection_enabled" to "false",
    "firebase_performance_collection_enabled" to "false",
    "firebase_performance_collection_deactivated" to "true",
    "appsflyer_data_collection_enabled" to "false",
    "io.sentry.auto-init" to "false",
)

private fun getAttributeValue(element: Element, attributeName: String): String {
    val attr = element.getAttribute("android:$attributeName")
    if (attr.isNotBlank()) return attr.trim()
    val attrNs = element.getAttributeNS(ANDROID_XML_NAMESPACE, attributeName)
    if (attrNs.isNotBlank()) return attrNs.trim()
    return element.getAttribute(attributeName).trim()
}

private fun setAttributeWithNamespace(element: Element, name: String, value: String) {
    element.removeAttribute("android:$name")
    element.setAttributeNS(ANDROID_XML_NAMESPACE, "android:$name", value)
}

private fun stripTrackingPermissions(doc: Document): List<String> {
    val removed = mutableListOf<String>()
    val permissionTags = listOf("uses-permission", "uses-permission-sdk-23")
    for (tag in permissionTags) {
        val nodes = doc.getElementsByTagName(tag)
        val toRemove = mutableListOf<Pair<Element, String>>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? Element ?: continue
            val name = getAttributeValue(element, "name")
            if (name in TRACKING_PERMISSIONS) {
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

private fun disableManifestComponents(doc: Document, tag: String, targetNames: Set<String>): Int {
    var disabled = 0
    val elements = doc.getElementsByTagName(tag)
    for (i in 0 until elements.length) {
        val elem = elements.item(i) as? Element ?: continue
        val name = getAttributeValue(elem, "name")
        if (name in targetNames) {
            val currentEnabled = getAttributeValue(elem, "enabled")
            if (!currentEnabled.equals("false", ignoreCase = true)) {
                setAttributeWithNamespace(elem, "enabled", "false")
                disabled++
            }
        }
    }
    return disabled
}

private fun collectExistingMetaData(appElement: Element): MutableMap<String, Element> {
    val existingMeta = mutableMapOf<String, Element>()
    val metaNodes = appElement.getElementsByTagName("meta-data")
    for (i in 0 until metaNodes.length) {
        val meta = metaNodes.item(i) as? Element ?: continue
        val name = getAttributeValue(meta, "name")
        if (name.isNotBlank()) {
            existingMeta[name] = meta
        }
    }
    return existingMeta
}

private fun injectOptOutMetadata(doc: Document): Int {
    val appNodes = doc.getElementsByTagName("application")
    if (appNodes.length == 0) return 0
    val appElement = appNodes.item(0) as? Element ?: return 0

    val existingMeta = collectExistingMetaData(appElement)
    var injectedOrUpdated = 0

    for ((name, value) in OPT_OUT_METADATA) {
        val existing = existingMeta[name]
        if (existing != null) {
            val currentValue = getAttributeValue(existing, "value")
            if (currentValue != value) {
                setAttributeWithNamespace(existing, "value", value)
                injectedOrUpdated++
            }
        } else {
            val newMeta = doc.createElement("meta-data")
            setAttributeWithNamespace(newMeta, "name", name)
            setAttributeWithNamespace(newMeta, "value", value)
            appElement.appendChild(newMeta)
            injectedOrUpdated++
        }
    }
    return injectedOrUpdated
}

@Suppress("unused")
val universalTelemetryNeutralizerPatch = resourcePatch(
    name = "Universal Telemetry Neutralizer",
    description = "Strips advertising and Privacy Sandbox permissions, disables analytics ContentProviders and telemetry background services (Firebase, Sentry, Adjust, AppsFlyer, DataTransport), and injects telemetry opt-out metadata.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val revokePermissions by booleanOption(
        key = "revokePermissions",
        default = true,
        title = "Revoke Advertising & Tracking Permissions",
        description = "Remove AD_ID, Privacy Sandbox attribution/topics/audiences, and Play Install Referrer permissions from AndroidManifest.xml.",
        required = false,
    )

    val disableProviders by booleanOption(
        key = "disableProviders",
        default = true,
        title = "Disable Telemetry ContentProviders",
        description = "Disable analytics ContentProviders (Google Measurement, Sentry, Facebook, Flurry, Branch, AppsFlyer).",
        required = false,
    )

    val disableServices by booleanOption(
        key = "disableServices",
        default = true,
        title = "Disable Telemetry Background Services",
        description = "Disable Google Measurement, Google DataTransport, and AppsFlyer background job services.",
        required = false,
    )

    val disableReceivers by booleanOption(
        key = "disableReceivers",
        default = true,
        title = "Disable Telemetry Receivers",
        description = "Disable install referrer and analytics measurement broadcast receivers (Adjust, AppsFlyer, Google Measurement).",
        required = false,
    )

    val injectOptOutFlags by booleanOption(
        key = "injectOptOutFlags",
        default = true,
        title = "Inject Telemetry Opt-Out Flags",
        description = "Inject meta-data opt-out entries into application tag (Firebase Analytics, Crashlytics, Performance, AppsFlyer, Sentry).",
        required = false,
    )

    val disableFirebaseInit by booleanOption(
        key = "disableFirebaseInit",
        default = false,
        title = "Disable Firebase Init Provider",
        description = "Disable com.google.firebase.provider.FirebaseInitProvider. Default is false to prevent issues in apps that depend on Firebase Auth or push notifications.",
        required = false,
    )

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Universal Telemetry Neutralizer] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        val shouldRevoke = revokePermissions ?: true
        val shouldDisableProviders = disableProviders ?: true
        val shouldDisableServices = disableServices ?: true
        val shouldDisableReceivers = disableReceivers ?: true
        val shouldInjectOptOut = injectOptOutFlags ?: true
        val shouldDisableFirebase = disableFirebaseInit ?: false

        var removedPerms: List<String> = emptyList()
        var disabledProvidersCount = 0
        var disabledServicesCount = 0
        var disabledReceiversCount = 0
        var injectedFlagsCount = 0

        document(manifestFile.absolutePath).use { doc ->
            if (shouldRevoke) {
                removedPerms = stripTrackingPermissions(doc)
            }

            if (shouldDisableProviders) {
                val targetProviders = if (shouldDisableFirebase) {
                    TELEMETRY_PROVIDERS + FIREBASE_INIT_PROVIDER
                } else {
                    TELEMETRY_PROVIDERS
                }
                disabledProvidersCount = disableManifestComponents(doc, "provider", targetProviders)
            }

            if (shouldDisableServices) {
                disabledServicesCount = disableManifestComponents(doc, "service", TELEMETRY_SERVICES)
            }

            if (shouldDisableReceivers) {
                disabledReceiversCount = disableManifestComponents(doc, "receiver", TELEMETRY_RECEIVERS)
            }

            if (shouldInjectOptOut) {
                injectedFlagsCount = injectOptOutMetadata(doc)
            }
        }

        val totalDisabled = disabledProvidersCount + disabledServicesCount + disabledReceiversCount
        if (removedPerms.isEmpty() && totalDisabled == 0 && injectedFlagsCount == 0) {
            println("[Universal Telemetry Neutralizer] AndroidManifest.xml is already clean (0 tracking elements found).")
            return@execute
        }

        val permNames = removedPerms.map { it.substringAfterLast('.') }.distinct()
        val permNote = if (removedPerms.isNotEmpty()) "revoked ${removedPerms.size} permission(s) (${permNames.joinToString(", ")})" else "0 permissions revoked"
        println("[Universal Telemetry Neutralizer] $permNote, disabled $totalDisabled component(s), injected $injectedFlagsCount opt-out flag(s).")
    }
}
