package app.morphe.patches.nokoprint

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

@Suppress("unused")
val nokoPrintStartupPerformancePatch = resourcePatch(
    name = "NokoPrint Startup Performance Optimizer",
    description = "Disables non-essential ad mediation and tracker ContentProviders that auto-initialize before Application.onCreate, accelerating cold startup.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[NokoPrint Startup Performance] AndroidManifest.xml not found - skipping.")
            return@execute
        }

        val providersToDisable = setOf(
            "com.applovin.sdk.AppLovinInitProvider",
            "com.google.android.gms.ads.MobileAdsInitProvider",
            "com.facebook.ads.AudienceNetworkContentProvider",
            "com.facebook.internal.FacebookInitProvider",
            "com.ironsource.lifecycle.IronsourceLifecycleProvider",
            "com.smaato.sdk.core.lifecycle.ProcessLifecycleOwnerInitializer",
            "com.appbrain.AppBrainInitProvider",
            "com.huawei.agconnect.core.provider.AGConnectInitializeProvider",
            "sg.bigo.ads.controller.provider.BigoAdsProvider",
            "com.vungle.ads.VungleProvider",
            "com.mbridge.msdk.config.component.status.MBComponentLifecycleProvider",
            "com.ironsource.lifecycle.LevelPlayActivityLifecycleProvider",
        )

        var disabledCount = 0

        document(manifestFile.absolutePath).use { doc ->
            val providers = doc.getElementsByTagName("provider")
            for (i in 0 until providers.length) {
                val provider = providers.item(i) as? Element ?: continue
                val name = provider.getAttribute("android:name")
                if (name in providersToDisable) {
                    provider.setAttribute("android:enabled", "false")
                    disabledCount++
                }
            }

            // Remove AdsSdkInitializer from androidx.startup.InitializationProvider
            val metaDataNodes = doc.getElementsByTagName("meta-data")
            val metaToRemove = mutableListOf<Element>()
            for (i in 0 until metaDataNodes.length) {
                val elem = metaDataNodes.item(i) as? Element ?: continue
                val name = elem.getAttribute("android:name")
                if (name == "com.unity3d.services.core.configuration.AdsSdkInitializer") {
                    metaToRemove.add(elem)
                }
            }
            metaToRemove.forEach { it.parentNode?.removeChild(it) }
        }

        println("[NokoPrint Startup Performance] Disabled $disabledCount startup ContentProviders to accelerate cold launch.")
    }
}
