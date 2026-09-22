package app.morphe.patches.nokoprint

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
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

        // 1. Stub com.nokoprint.ads.a.i (preload banner)
        Fingerprint(
            definingClass = "Lcom/nokoprint/ads/a;",
            name = "i",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;", "Lcom/nokoprint/ads/a\$e;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("a.i(preloadBanner)")
        }

        // 2. Stub com.nokoprint.ads.a.j (preload interstitial)
        Fingerprint(
            definingClass = "Lcom/nokoprint/ads/a;",
            name = "j",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("a.j(preloadInterstitial)")
        }

        // 3. Stub com.nokoprint.ads.a.k (preload rewarded)
        Fingerprint(
            definingClass = "Lcom/nokoprint/ads/a;",
            name = "k",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("a.k(preloadRewarded)")
        }

        // 4. Stub com.nokoprint.ads.b.i (banner adapter preload)
        Fingerprint(
            definingClass = "Lcom/nokoprint/ads/b;",
            name = "i",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Lcom/nokoprint/ads/b\$f;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("b.i(bannerPreload)")
        }

        // 5. Stub com.nokoprint.ads.b.j (interstitial adapter preload)
        Fingerprint(
            definingClass = "Lcom/nokoprint/ads/b;",
            name = "j",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("b.j(interstitialPreload)")
        }

        // 6. Stub com.nokoprint.ads.c.i (mediation adapter preload)
        Fingerprint(
            definingClass = "Lcom/nokoprint/ads/c;",
            name = "i",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Lcom/nokoprint/ads/c\$g;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("c.i(mediationPreload)")
        }

        // 7. Force com.nokoprint.n.A(Z)Z to return true (is_no_ads active)
        Fingerprint(
            definingClass = "Lcom/nokoprint/n;",
            name = "A",
            parameters = listOf("Z"),
            returnType = "Z",
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            hookedMethods.add("n.A(isNoAds)")
        }

        // 8. Stub com.nokoprint.c methods (update check, exit interstitial preload, show interstitial, subscription prompt)
        Fingerprint(
            definingClass = "Lcom/nokoprint/c;",
            name = "y0",
            parameters = emptyList(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("c.y0(checkUpdate)")
        }

        Fingerprint(
            definingClass = "Lcom/nokoprint/c;",
            name = "T",
            parameters = emptyList(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("c.T(preloadExitInterstitial)")
        }

        Fingerprint(
            definingClass = "Lcom/nokoprint/c;",
            name = "z0",
            parameters = emptyList(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("c.z0(showInterstitial)")
        }

        Fingerprint(
            definingClass = "Lcom/nokoprint/c;",
            name = "C0",
            parameters = emptyList(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("c.C0(subscriptionPrompt)")
        }

        // 9. Neutralize com.nokoprint.o.k(com.nokoprint.c)ArrayList (return empty list of store adapters)
        Fingerprint(
            definingClass = "Lcom/nokoprint/o;",
            name = "k",
            parameters = listOf("Lcom/nokoprint/c;"),
            returnType = "Ljava/util/ArrayList;",
        ).method.apply {
            addInstructions(
                0,
                """
                new-instance v0, Ljava/util/ArrayList;
                invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("o.k(emptyStoreAdapters)")
        }

        // 10. Silence com.nokoprint.h$t$c.b review prompts
        Fingerprint(
            definingClass = "Lcom/nokoprint/h\$t\$c;",
            name = "b",
            parameters = listOf("Lcom/nokoprint/n\$p;"),
            returnType = "V",
        ).method.apply {
            val instructions = implementation?.instructions?.toList() ?: emptyList()
            val targetIdx = instructions.indexOfFirst { ins ->
                val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                ref?.definingClass == "Lcom/nokoprint/h;" && ref.name == "h1"
            }
            check(targetIdx >= 0) { "Target invocation Lcom/nokoprint/h;->h1 not found in com.nokoprint.h\$t\$c.b" }
            addInstructions(targetIdx + 1, "return-void")
            hookedMethods.add("h\$t\$c.b(silenceReviewPrompt)")
        }

        // 11. Rewrite printer driver download URL in com.nokoprint.d.r0 to HTTPS
        Fingerprint(
            definingClass = "Lcom/nokoprint/d;",
            name = "r0",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Z", "Z"),
            returnType = "Z",
        ).method.apply {
            val instructions = implementation?.instructions?.toList() ?: emptyList()
            val targetIdx = instructions.indexOfFirst { ins ->
                (ins.opcode == Opcode.CONST_STRING || ins.opcode == Opcode.CONST_STRING_JUMBO) &&
                    ((ins as? ReferenceInstruction)?.reference as? StringReference)?.string == "http://nokoprint.com/android_packs/"
            }
            check(targetIdx >= 0) { "Target URL http://nokoprint.com/android_packs/ not found in com.nokoprint.d.r0" }
            val reg = (instructions[targetIdx] as OneRegisterInstruction).registerA
            replaceInstruction(targetIdx, "const-string v$reg, \"https://nokoprint.com/android_packs/\"")
            hookedMethods.add("d.r0(rewriteDriverUrlToHttps)")
        }

        println("[NokoPrint Ad Dispatch Governor] Neutralized ${hookedMethods.size} ad dispatch, promo, and telemetry hooks.")
    }
}
