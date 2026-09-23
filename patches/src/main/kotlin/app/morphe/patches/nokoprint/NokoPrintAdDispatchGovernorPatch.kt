package app.morphe.patches.nokoprint

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Suppress("unused")
val nokoPrintAdDispatchGovernorPatch = bytecodePatch(
    name = "NokoPrint Ad Dispatch Governor",
    description = "Neutralizes in-app banner, interstitial, and rewarded ad loaders, preloading schedulers, and refresh handlers.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

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

        // 6. Rewrite printer driver download protocol in ActivityCore.J to HTTPS
        Fingerprint(
            definingClass = "Lcom/nokoprint/ActivityCore;",
            name = "J",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Z", "Z"),
            returnType = "Z",
        ).method.apply {
            val instructions = implementation?.instructions?.toList() ?: emptyList()
            val urlIdx = instructions.indexOfFirst { ins ->
                (ins.opcode == Opcode.CONST_STRING || ins.opcode == Opcode.CONST_STRING_JUMBO) &&
                    ((ins as? ReferenceInstruction)?.reference as? StringReference)?.string == "://www.nokoprint.com/android_packs/"
            }
            check(urlIdx >= 0) { "Target URL ://www.nokoprint.com/android_packs/ not found in ActivityCore.J" }

            val httpIdx = instructions.subList(0, urlIdx).indexOfLast { ins ->
                (ins.opcode == Opcode.CONST_STRING || ins.opcode == Opcode.CONST_STRING_JUMBO) &&
                    ((ins as? ReferenceInstruction)?.reference as? StringReference)?.string == "http"
            }
            check(httpIdx >= 0) { "Target protocol 'http' not found before driver URL in ActivityCore.J" }

            val reg = (instructions[httpIdx] as OneRegisterInstruction).registerA
            replaceInstruction(httpIdx, "const-string v$reg, \"https\"")
            hookedMethods.add("ActivityCore.J(rewriteDriverUrlToHttps)")
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

        println("[NokoPrint Ad Dispatch Governor] Neutralized ${hookedMethods.size} ad dispatch, promo, and telemetry hooks.")
    }
}
