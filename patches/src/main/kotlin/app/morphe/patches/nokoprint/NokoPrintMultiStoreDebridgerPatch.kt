package app.morphe.patches.nokoprint

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

@Suppress("unused")
val nokoPrintMultiStoreDebridgerPatch = resourcePatch(
    name = "NokoPrint Multi-Store Debridger",
    description = "Disables orphan billing activities, background services, and permissions for alternative OEM stores (Huawei HMS, Xiaomi, Samsung, CafeBazaar, OneStore).",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[NokoPrint Multi-Store Debridger] AndroidManifest.xml not found - skipping.")
            return@execute
        }

        val orphanComponents = setOf(
            // Huawei HMS, AGConnect & OTA
            "com.huawei.hms.activity.BridgeActivity",
            "com.huawei.hms.activity.EnableServiceActivity",
            "com.huawei.agconnect.core.ServiceDiscovery",
            "com.huawei.agconnect.core.provider.AGConnectInitializeProvider",
            "com.huawei.updatesdk.service.otaupdate.AppUpdateActivity",
            "com.huawei.updatesdk.support.pm.PackageInstallerActivity",
            "com.huawei.updatesdk.fileprovider.UpdateSdkFileProvider",
            "com.huawei.hms.account.internal.ui.activity.AccountSignInHubActivity",
            "com.huawei.hms.hwid.internal.ui.activity.HwIdSignInHubActivity",
            // Xiaomi Billing & Common IAP
            "com.xiaomi.billingclient.ui.ProxyBillingActivity",
            "com.xiaomi.billingclient.ui.ClientPaymentWebActivity",
            "com.xiaomi.billingclient.ui.ClientExtraWebActivity",
            "com.xiaomi.billingclient.floating.WebActivity",
            "com.iap.common.floating.WebActivity",
            "com.iap.common.ui.ClientExtraWebActivity",
            "com.iap.common.ui.ClientPaymentWebActivity",
            "com.iap.common.ui.ProxyBillingActivity",
            // Samsung IAP
            "com.samsung.android.sdk.iap.lib.activity.DialogActivity",
            "com.samsung.android.sdk.iap.lib.activity.CheckPackageActivity",
            "com.samsung.android.sdk.iap.lib.activity.AccountActivity",
            "com.samsung.android.sdk.iap.lib.activity.PaymentActivity",
            "com.samsung.android.sdk.iap.lib.activity.ChangeSubscriptionPlanActivity",
            // RuStore
            "com.nokoprint.AppStoreRustoreActivity",
            "ru.rustore.sdk.pay.internal.presentation.ui.PayActivity",
            "ru.rustore.sdk.pay.RuStorePayContentProvider",
            "ru.rustore.sdk.imaging.ImageLoaderContentProvider",
            "ru.rustore.sdk.metrics.internal.presentation.SendMetricsEventJobService",
            // OneStore
            "com.gaa.sdk.iap.ProxyActivity",
            "com.gaa.sdk.auth.SignInActivity",
            // CafeBazaar
            "com.farsitel.bazaar.billing.IABReceiver",
            // Amazon IAP
            "com.amazon.device.iap.ResponseReceiver",
            // Google Play Billing & Core
            "com.android.billingclient.api.ProxyBillingActivity",
            "com.android.billingclient.api.ProxyBillingActivityV2",
            "com.google.android.play.core.common.PlayCoreDialogWrapperActivity",
            "com.google.android.play.core.assetpacks.AssetPackExtractionService",
            "com.google.android.play.core.assetpacks.ExtractionForegroundService",
            "com.pairip.licensecheck.LicenseActivity",
            // Internal Store Webpage
            "com.fyber.inneractive.sdk.activities.InternalStoreWebpageActivity",
        )

        val orphanPermissions = setOf(
            // Store / Billing permissions
            "com.samsung.android.iap.permission.BILLING",
            "com.farsitel.bazaar.permission.PAY_THROUGH_BAZAAR",
            "com.android.vending.BILLING",
            "com.android.vending.CHECK_LICENSE",
            "com.huawei.appmarket.service.commondata.permission.GET_COMMON_DATA",
            // Ad & Attribution permissions
            "android.permission.ACCESS_ADSERVICES_AD_ID",
            "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
            "android.permission.ACCESS_ADSERVICES_TOPICS",
            "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE",
            "com.google.android.gms.permission.AD_ID",
            "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
            "com.applovin.array.apphub.permission.BIND_APPHUB_SERVICE",
            "com.amazon.privacypass.ATTEST",
            // Unused background sync / foreground service permissions
            "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
        )

        var disabledComponents = 0
        var removedPermissions = 0

        document(manifestFile.absolutePath).use { doc ->
            // 1. Disable orphan components
            val tags = listOf("activity", "service", "receiver", "provider")
            for (tag in tags) {
                val elements = doc.getElementsByTagName(tag)
                for (i in 0 until elements.length) {
                    val elem = elements.item(i) as? Element ?: continue
                    val name = elem.getAttribute("android:name")
                    if (name in orphanComponents) {
                        elem.setAttribute("android:enabled", "false")
                        disabledComponents++
                    }
                }
            }

            // 2. Remove orphan store permissions
            val usesPerms = doc.getElementsByTagName("uses-permission")
            val permsToRemove = mutableListOf<Element>()
            for (i in 0 until usesPerms.length) {
                val elem = usesPerms.item(i) as? Element ?: continue
                val name = elem.getAttribute("android:name")
                if (name in orphanPermissions) {
                    permsToRemove.add(elem)
                }
            }
            permsToRemove.forEach {
                it.parentNode?.removeChild(it)
                removedPermissions++
            }
        }

        println("[NokoPrint Multi-Store Debridger] Disabled $disabledComponents OEM store components, stripped $removedPermissions permissions.")
    }
}
