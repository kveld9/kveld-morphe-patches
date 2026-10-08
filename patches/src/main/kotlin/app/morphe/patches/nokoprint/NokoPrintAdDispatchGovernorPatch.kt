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

        val tagCounts = mutableMapOf("activity" to 0, "service" to 0, "receiver" to 0)
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
                        tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
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

        val disabledComponents = tagCounts.values.sum()
        val categoryBreakdown = tagCounts.entries
            .filter { it.value > 0 }
            .joinToString(", ") { "${it.key}=${it.value}" }
        val componentDetails = if (categoryBreakdown.isNotEmpty()) " ($categoryBreakdown)" else ""

        println("[Ad Dispatch Governor] Disabled $disabledComponents ad components$componentDetails, $disabledProviders startup providers.")
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

        // 1. Force m4.g(Z)Z to return true (is_no_ads active across entire activity hierarchy)
        Fingerprint(
            definingClass = "Lcom/nokoprint/m4;",
            name = "g",
            parameters = listOf("Z"),
            returnType = "Z",
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            hookedMethods.add("m4.g(isNoAds)")
        }

        // 2. Stub m4.w(Z)V (banner container initialization & ad dispatching)
        Fingerprint(
            definingClass = "Lcom/nokoprint/m4;",
            name = "w",
            parameters = listOf("Z"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("m4.w(bannerDispatcher)")
        }

        // 3. Stub m4.r (AdMob ad revenue & impression callback)
        Fingerprint(
            definingClass = "Lcom/nokoprint/m4;",
            name = "r",
            parameters = listOf("Ljava/lang/String;", "Lcom/google/android/gms/ads/AdValue;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("m4.r(adMobRevenue)")
        }

        // 4. Stub m4.s (AppLovin MAX ad revenue & attribution callback)
        Fingerprint(
            definingClass = "Lcom/nokoprint/m4;",
            name = "s",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Lcom/applovin/mediation/MaxAd;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("m4.s(appLovinAdCallback)")
        }

        // 5. Bypass interstitial ad loader in h4.b and invoke target callback immediately
        Fingerprint(
            definingClass = "Lcom/nokoprint/h4;",
            name = "b",
            parameters = listOf("Lcom/google/android/material/carousel/d;", "Lcom/nokoprint/m4;", "Ljava/util/Hashtable;"),
            returnType = "V",
        ).method.apply {
            val count = implementation?.instructions?.count() ?: 0
            if (count > 0) {
                removeInstructions(0, count)
            }
            addInstructionsWithLabels(
                0,
                """
                if-eqz p1, :cond_skip_h
                invoke-virtual {p1}, Lcom/nokoprint/m4;->h()V
                :cond_skip_h
                if-eqz p0, :cond_skip_cb
                const/4 v0, 0x0
                invoke-virtual {p0, v0}, Lcom/google/android/material/carousel/d;->a(Lcom/nokoprint/h4;)V
                :cond_skip_cb
                return-void
                """.trimIndent(),
            )
            hookedMethods.add("h4.b(bypassInterstitial)")
        }

        // 6. Bypass interstitial ad display in h4.c and execute completion callback immediately
        Fingerprint(
            definingClass = "Lcom/nokoprint/h4;",
            name = "c",
            parameters = listOf("Lcom/nokoprint/m4;", "Lcom/nokoprint/m;"),
            returnType = "V",
        ).method.apply {
            val count = implementation?.instructions?.count() ?: 0
            if (count > 0) {
                removeInstructions(0, count)
            }
            addInstructionsWithLabels(
                0,
                """
                if-eqz p1, :cond_skip_h
                invoke-virtual {p1}, Lcom/nokoprint/m4;->h()V
                :cond_skip_h
                if-eqz p2, :cond_skip_run
                invoke-virtual {p2}, Lcom/nokoprint/m;->run()V
                :cond_skip_run
                return-void
                """.trimIndent(),
            )
            hookedMethods.add("h4.c(bypassInterstitialDisplay)")
        }

        // 7. Bypass rewarded ad loader in l4.b and execute target callback immediately
        Fingerprint(
            definingClass = "Lcom/nokoprint/l4;",
            name = "b",
            parameters = listOf("Lcom/nokoprint/m4;", "Ljava/util/Hashtable;", "Landroidx/compose/runtime/b1;"),
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
                invoke-virtual {p0}, Lcom/nokoprint/m4;->h()V
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
            hookedMethods.add("l4.b(bypassRewardedAd)")
        }

        // 8. Stub com.pairip.licensecheck.LicenseClient.checkLicense to bypass Google Play anti-tamper exit
        Fingerprint(
            definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
            name = "checkLicense",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("LicenseClient.checkLicense")
        }

        // 9. Stub m4.i()Z (MobileAds.initialize) to return false
        Fingerprint(
            definingClass = "Lcom/nokoprint/m4;",
            name = "i",
            parameters = emptyList(),
            returnType = "Z",
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            hookedMethods.add("m4.i(initMobileAds)")
        }

        // 10. Stub m4.k()Z (AppLovinSdk.initialize) to return false
        Fingerprint(
            definingClass = "Lcom/nokoprint/m4;",
            name = "k",
            parameters = emptyList(),
            returnType = "Z",
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            hookedMethods.add("m4.k(initAppLovinSdk)")
        }

        // 11. Stub m4.a(J, Z, String)V (ad revenue tracking to Facebook & TikTok)
        Fingerprint(
            definingClass = "Lcom/nokoprint/m4;",
            name = "a",
            parameters = listOf("J", "Z", "Ljava/lang/String;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("m4.a(trackAdRevenue)")
        }

        println("[Ad Dispatch Governor] Neutralized ${hookedMethods.size} ad dispatch, promo, and telemetry hooks.")
    }
}
