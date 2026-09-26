package app.morphe.patches.nokoprint

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val nokoPrintAdManifestResourcePatch = resourcePatch(
    name = "Ad Manifest Purge",
    description = "Disables third-party ad mediation activities, internal web browsers, debuggers, and ad startup ContentProviders in AndroidManifest.xml.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Ad Dispatch Governor] AndroidManifest.xml not found - skipping manifest purge.")
            return@execute
        }

        val adComponentPrefixes = listOf(
            "com.applovin.",
            "com.mbridge.msdk.",
            "com.facebook.",
            "com.unity3d.",
            "com.ironsource.",
            "com.fyber.inneractive.",
            "com.vungle.ads.",
            "com.chartboost.sdk.",
            "com.inmobi.ads.",
            "com.amazon.device.ads.",
            "com.amazon.aps.",
            "com.appbrain.",
            "net.pubnative.lite.",
            "com.smaato.sdk.",
            "com.bytedance.sdk.",
            "sg.bigo.ads.",
            "com.ogury.",
            "com.moloco.sdk.",
            "com.pubmatic.sdk.",
            "com.tappx.sdk.",
        )

        val providersToDisable = setOf(
            "com.applovin.sdk.AppLovinInitProvider",
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

        var disabledComponents = 0
        var disabledProviders = 0

        document(manifestFile.absolutePath).use { doc ->
            val tags = listOf("activity", "service", "receiver")
            for (tag in tags) {
                val elements = doc.getElementsByTagName(tag)
                for (i in 0 until elements.length) {
                    val comp = elements.item(i) as? Element ?: continue
                    val name = comp.getAttribute("android:name")
                    if (adComponentPrefixes.any { name.startsWith(it) }) {
                        comp.setAttribute("android:enabled", "false")
                        disabledComponents++
                    }
                }
            }

            val providers = doc.getElementsByTagName("provider")
            for (i in 0 until providers.length) {
                val provider = providers.item(i) as? Element ?: continue
                val name = provider.getAttribute("android:name")
                if (name in providersToDisable) {
                    provider.setAttribute("android:enabled", "false")
                    disabledProviders++
                }
            }

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

        println("[Ad Dispatch Governor] Disabled $disabledComponents ad components, $disabledProviders startup providers.")
    }
}

@Suppress("unused")
val nokoPrintAdDispatchGovernorPatch = bytecodePatch(
    name = "Ad Dispatch Governor",
    description = "Neutralizes ad loaders, unlocks ad-free status, and strips mediation components & startup providers.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)
    dependsOn(nokoPrintAdManifestResourcePatch)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Force ActivityRoot.g(Z)Z to return true (is_no_ads active across entire activity hierarchy)
        Fingerprint(
            definingClass = "Lcom/nokoprint/ActivityRoot;",
            name = "g",
            parameters = listOf("Z"),
            returnType = "Z",
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            hookedMethods.add("ActivityRoot.g(isNoAds)")
        }

        // 2. Stub ActivityRoot.w(Z)V (banner container initialization & ad dispatching)
        Fingerprint(
            definingClass = "Lcom/nokoprint/ActivityRoot;",
            name = "w",
            parameters = listOf("Z"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("ActivityRoot.w(bannerDispatcher)")
        }

        // 3. Stub ActivityRoot.r (AdMob ad revenue & impression callback)
        Fingerprint(
            definingClass = "Lcom/nokoprint/ActivityRoot;",
            name = "r",
            parameters = listOf("Ljava/lang/String;", "Lcom/google/android/gms/ads/AdValue;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("ActivityRoot.r(adMobRevenue)")
        }

        // 4. Stub ActivityRoot.s (AppLovin MAX ad revenue & attribution callback)
        Fingerprint(
            definingClass = "Lcom/nokoprint/ActivityRoot;",
            name = "s",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Lcom/applovin/mediation/MaxAd;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("ActivityRoot.s(appLovinAdCallback)")
        }

        // 5. Stub com.nokoprint.f4.b (interstitial ad preloading for AdMob / AppLovin)
        Fingerprint(
            definingClass = "Lcom/nokoprint/f4;",
            name = "b",
            parameters = listOf("Lcom/google/android/material/carousel/d;", "Lcom/nokoprint/ActivityRoot;", "Ljava/util/Hashtable;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("f4.b(preloadInterstitial)")
        }

        // 6. Bypass rewarded ad loader in j4.b and execute target callback immediately
        Fingerprint(
            definingClass = "Lcom/nokoprint/j4;",
            name = "b",
            parameters = listOf("Lcom/nokoprint/ActivityRoot;", "Ljava/util/Hashtable;", "Landroidx/compose/runtime/b1;"),
            returnType = "V",
        ).method.apply {
            val count = implementation?.instructions?.count() ?: 0
            if (count > 0) {
                removeInstructions(0, count)
            }
            addInstructionsWithLabels(
                0,
                """
                if-eqz p0, :cond_skip_h
                invoke-virtual {p0}, Lcom/nokoprint/ActivityRoot;->h()V
                :cond_skip_h
                if-eqz p2, :cond_skip_run
                iget-object v0, p2, Landroidx/compose/runtime/b1;->d:Ljava/lang/Object;
                check-cast v0, Ljava/lang/Runnable;
                if-eqz v0, :cond_skip_run
                invoke-interface {v0}, Ljava/lang/Runnable;->run()V
                :cond_skip_run
                return-void
                """.trimIndent(),
            )
            hookedMethods.add("j4.b(bypassRewardedAd)")
        }

        // 7. Stub com.pairip.licensecheck.LicenseClient.checkLicense to bypass Google Play anti-tamper exit
        Fingerprint(
            definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
            name = "checkLicense",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("LicenseClient.checkLicense")
        }

        // 8. Stub ActivityRoot.i()Z (MobileAds.initialize) to return false
        Fingerprint(
            definingClass = "Lcom/nokoprint/ActivityRoot;",
            name = "i",
            parameters = emptyList(),
            returnType = "Z",
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            hookedMethods.add("ActivityRoot.i(initMobileAds)")
        }

        // 9. Stub ActivityRoot.k()Z (AppLovinSdk.initialize) to return false
        Fingerprint(
            definingClass = "Lcom/nokoprint/ActivityRoot;",
            name = "k",
            parameters = emptyList(),
            returnType = "Z",
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            hookedMethods.add("ActivityRoot.k(initAppLovinSdk)")
        }

        // 10. Stub ActivityRoot.a(J, Z, String)V (ad revenue tracking to Facebook & TikTok)
        Fingerprint(
            definingClass = "Lcom/nokoprint/ActivityRoot;",
            name = "a",
            parameters = listOf("J", "Z", "Ljava/lang/String;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("ActivityRoot.a(trackAdRevenue)")
        }

        println("[Ad Dispatch Governor] Neutralized ${hookedMethods.size} ad dispatch, promo, and telemetry hooks.")
    }
}
