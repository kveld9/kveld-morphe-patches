package app.morphe.patches.universal

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.ANDROID_XML_NAMESPACE
import app.morphe.patches.shared.childrenNamed
import app.morphe.patches.shared.removeChildren
import app.morphe.patches.shared.stripPermissionsWhere
import org.w3c.dom.Element

private const val BILLING_PERMISSION = "com.android.vending.BILLING"

private val BILLING_QUERY_ACTIONS = setOf(
    "com.android.vending.billing.InAppBillingService.BIND",
    "com.google.android.apps.play.billingtestcompanion.BillingOverrideService.BIND",
)

private val BILLING_ACTIVITY_NAMES = setOf(
    "com.android.billingclient.api.ProxyBillingActivity",
    "com.android.billingclient.api.ProxyBillingActivityV2",
)

private const val BILLING_METADATA_VERSION = "com.google.android.play.billingclient.version"

/**
 * Universal Play Billing Neutralizer (Manifest Layer).
 *
 * Provides graceful degradation for applications that include the Google Play Billing
 * client library when running on environments or devices without Google Mobile Services (GMS).
 *
 * Architectural Invariant (Option b - Crash Safety):
 * Disabling ProxyBillingActivity/ProxyBillingActivityV2 in AndroidManifest.xml while callers
 * still invoke launchBillingFlow causes a fatal ActivityNotFoundException at runtime. Because
 * a universal patch cannot provide per-app caller stubs (and library implementation methods like
 * BillingClientImpl are minified per-version by R8), the universal patch strips the
 * com.android.vending.BILLING permission and unbinds billing service queries, but intentionally
 * preserves the proxy activities. The billing client then fails closed gracefully (reporting
 * BILLING_UNAVAILABLE / SERVICE_UNAVAILABLE) without crashing the application.
 */
@Suppress("unused")
val universalNeutralizePlayBillingPatch = resourcePatch(
    name = "Neutralize Play Billing",
    description = "Removes Play Billing permission and service query intents from AndroidManifest.xml for graceful degradation on devices without Google Mobile Services.",
    default = false,
) {
    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Neutralize Play Billing] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        var removedPermissions: List<String> = emptyList()
        var prunedQueries = 0
        val surfaceDetected: Boolean

        document(manifestFile.absolutePath).use { doc ->
            val root = doc.documentElement
            val application = root.childrenNamed("application").firstOrNull()

            // 1. Detect whether any Play Billing surface exists in the manifest
            val declaredPermissions = root.childrenNamed("uses-permission", "uses-permission-sdk-23")
                .map { it.getAttribute("android:name").ifBlank { it.getAttributeNS(ANDROID_XML_NAMESPACE, "name") } }
            val hasBillingPermission = declaredPermissions.any { it == BILLING_PERMISSION }

            val queriesElements = root.childrenNamed("queries")
            var hasBillingQuery = false
            for (queries in queriesElements) {
                for (intent in queries.childrenNamed("intent")) {
                    for (action in intent.childrenNamed("action")) {
                        val actionName = action.getAttribute("android:name").ifBlank {
                            action.getAttributeNS(ANDROID_XML_NAMESPACE, "name")
                        }
                        if (actionName in BILLING_QUERY_ACTIONS) {
                            hasBillingQuery = true
                            break
                        }
                    }
                    if (hasBillingQuery) break
                }
                if (hasBillingQuery) break
            }

            val hasBillingActivity = application?.childrenNamed("activity")?.any { activity ->
                val name = activity.getAttribute("android:name").ifBlank {
                    activity.getAttributeNS(ANDROID_XML_NAMESPACE, "name")
                }
                name in BILLING_ACTIVITY_NAMES
            } ?: false

            val hasBillingMetadata = application?.childrenNamed("meta-data")?.any { meta ->
                val name = meta.getAttribute("android:name").ifBlank {
                    meta.getAttributeNS(ANDROID_XML_NAMESPACE, "name")
                }
                name == BILLING_METADATA_VERSION
            } ?: false

            surfaceDetected = hasBillingPermission || hasBillingQuery || hasBillingActivity || hasBillingMetadata

            if (!surfaceDetected) {
                println("[Neutralize Play Billing] Skipped: Play Billing surface not found in AndroidManifest.xml; no changes applied.")
                return@execute
            }

            // 2. Strip billing permission
            removedPermissions = root.stripPermissionsWhere { it == BILLING_PERMISSION }

            // 3. Prune billing query intents
            for (queries in queriesElements) {
                val intentsToRemove = mutableListOf<Element>()
                for (intent in queries.childrenNamed("intent")) {
                    val hasBillingAction = intent.childrenNamed("action").any { action ->
                        val actionName = action.getAttribute("android:name").ifBlank {
                            action.getAttributeNS(ANDROID_XML_NAMESPACE, "name")
                        }
                        actionName in BILLING_QUERY_ACTIONS
                    }
                    if (hasBillingAction) {
                        intentsToRemove.add(intent)
                    }
                }
                if (intentsToRemove.isNotEmpty()) {
                    queries.removeChildren(intentsToRemove)
                    prunedQueries += intentsToRemove.size
                }
            }
        }

        if (removedPermissions.isEmpty() && prunedQueries == 0) {
            println("[Neutralize Play Billing] AndroidManifest.xml billing surface already purged (0 permissions, 0 queries modified; proxy activities preserved).")
            return@execute
        }

        val queryNote = if (prunedQueries > 0) " and pruned $prunedQueries billing query intent(s)" else ""
        println("[Neutralize Play Billing] Stripped ${removedPermissions.size} permission(s)$queryNote in AndroidManifest.xml (proxy activities preserved for crash safety).")
    }
}
