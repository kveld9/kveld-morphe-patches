package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val braveSuppressInAppPromosPatch = bytecodePatch(
    name = "Suppress In-App Promos & Surveys",
    description = "Suppresses intrusive in-app rating surveys, Play Store review prompts, and marketing promo popups (YouTube promo, ad-free callouts, and Brave Ads onboarding).",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Rate Eligibility Gate: Force Lsjf.d(Context) to return false
        Fingerprint(
            returnType = "Z",
            parameters = listOf("Landroid/content/Context;"),
            strings = listOf("brave_rate_dont_show_again", "qa_force_rate_dialog"),
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            hookedMethods.add("RateEligibilityGate.d")
        }

        // 2. BraveRateDialogFragment: Neutralize show(FragmentManager, String)
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/rate/BraveRateDialogFragment;",
            strings = listOf("brave_rating_dialog_tag"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("BraveRateDialogFragment.show")
        }

        // 3. BraveAskPlayStoreRatingDialog: Neutralize show(FragmentManager, String)
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/rate/BraveAskPlayStoreRatingDialog;",
            strings = listOf("brave_ask_play_store_rating_dialog_tag"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("BraveAskPlayStoreRatingDialog.show")
        }

        // 4. BraveRateThanksFeedbackDialog: Neutralize show(FragmentManager, String)
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/rate/BraveRateThanksFeedbackDialog;",
            strings = listOf("brave_rate_thanks_feedback_dialog_tag"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("BraveRateThanksFeedbackDialog.show")
        }

        // 5. BraveAdsSignupDialog: Neutralize in-app signup prompt builder
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/dialogs/BraveAdsSignupDialog;",
            name = "b",
            returnType = "V",
            parameters = listOf("Landroid/content/Context;"),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("BraveAdsSignupDialog.b")
        }

        // 6. BraveAdsSignupDialog: Force eligibility check to false
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/dialogs/BraveAdsSignupDialog;",
            name = "a",
            returnType = "Z",
            parameters = emptyList(),
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            hookedMethods.add("BraveAdsSignupDialog.a")
        }

        // 7. BraveDialogFragment: Drop promotional dialog tags before display
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/BraveDialogFragment;",
            returnType = "V",
            custom = { method, _ ->
                method.parameterTypes.size == 2 && method.parameterTypes[1] == "Ljava/lang/String;"
            },
        ).method.apply {
            addInstructionsWithLabels(
                0,
                """
                    const-string v0, "OpenYtInBraveDialogFragment"
                    invoke-virtual {v0, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :not_yt
                    return-void
                    :not_yt
                    const-string v0, "BraveAdFreeCalloutDialogFragment"
                    invoke-virtual {v0, p2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :not_adfree
                    return-void
                    :not_adfree
                    nop
                """.trimIndent(),
            )
            hookedMethods.add("BraveDialogFragment.show")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Suppress In-App Promos] Applied ${hookedMethods.size} hooks across ${targetClasses.size} classes -> in-app surveys & promo dialogs disabled.")
    }
}
