package app.morphe.patches.vivaldi

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val vivaldiDisablePromptsPatch = bytecodePatch(
    name = "Disable Vivaldi Prompts & In-App Popups",
    description = "Disables in-app donation prompts, search engine switch promos, rate-app popups, background privacy report alarms, and default browser promotional handlers.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Rate Vivaldi In-App Promo Dialog: dismiss immediately on view creation
        val fp1 = Fingerprint(
            returnType = "V",
            parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
            strings = listOf("prompt_shown_count", "skip_until_time"),
        )
        fp1.method.addInstructions(
            0,
            """
                invoke-virtual {p0}, Lcom/google/android/material/bottomsheet/BottomSheetDialogFragment;->dismissAllowingStateLoss()V
                return-void
            """,
        )
        val c1 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp1.originalClassDef.type)
        hookedMethods.add("$c1.onViewCreated")

        // 2. Search Engine Switch BottomSheet Dialog: dismiss immediately on view creation
        val fp2 = Fingerprint(
            returnType = "V",
            parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
            strings = listOf("MULTICHOICE_PROMPT", "DONATE_AND_MULTICHOICE_PROMPT"),
        )
        fp2.method.addInstructions(
            0,
            """
                invoke-virtual {p0}, Lcom/google/android/material/bottomsheet/BottomSheetDialogFragment;->dismissAllowingStateLoss()V
                return-void
            """,
        )
        val c2 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp2.originalClassDef.type)
        hookedMethods.add("$c2.onViewCreated")

        // 3. Donation Promotional Click Handler: neutralize click action
        val fp3 = Fingerprint(
            returnType = "V",
            parameters = listOf("Landroid/view/View;"),
            strings = listOf("https://login.vivaldi.net/profile/donations#mtm_campaign=Android-Donate-SearchEngineSwitchDialog"),
        )
        fp3.method.addInstructions(
            0,
            """
                return-void
            """,
        )
        val c3 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp3.originalClassDef.type)
        hookedMethods.add("$c3.onClick")

        // 4. Privacy Report Notification Receiver: neutralize onReceive handler
        Fingerprint(
            definingClass = "Lorg/vivaldi/browser/prompts/PrivacyReportNotificationReceiver;",
            name = "onReceive",
            returnType = "V",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("PrivacyReportNotificationReceiver.onReceive")
        }

        // 5. Privacy Report Notification Receiver: neutralize periodic alarm scheduler
        Fingerprint(
            definingClass = "Lorg/vivaldi/browser/prompts/PrivacyReportNotificationReceiver;",
            name = "a",
            returnType = "V",
            parameters = listOf("Landroid/content/Context;"),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("PrivacyReportNotificationReceiver.a")
        }

        // 6. Default Browser Promo Notification Receiver: neutralize onReceive handler
        Fingerprint(
            definingClass = "Lorg/vivaldi/browser/prompts/DefaultBrowserNotificationReceiver;",
            name = "onReceive",
            returnType = "V",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("DefaultBrowserNotificationReceiver.onReceive")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Disable Prompts] Neutralized ${hookedMethods.size} promo handlers across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}
