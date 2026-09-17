package app.morphe.patches.vivaldi

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import app.morphe.patches.shared.ensureRegisterCount

@Suppress("unused")
val vivaldiDisablePromptsPatch = bytecodePatch(
    name = "Disable Vivaldi Prompts & In-App Popups",
    description = "Disables in-app donation prompts, search engine switch promos, rate-app popups, background privacy report alarms, default browser promo handlers, and hides the Vivaldia game from the main menu.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Rate Vivaldi In-App Promo Dialog: dismiss immediately on view creation
        try {
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
        } catch (e: Exception) {
            println("[Disable Prompts] Rate Vivaldi dialog hook note: ${e.message}")
        }

        // 2. Search Engine Switch & Donation BottomSheet Launcher: neutralize display helper
        try {
            val fpLauncher = Fingerprint(
                returnType = "V",
                strings = listOf("DIALOG_TYPE"),
                custom = { method, _ ->
                    method.parameterTypes.size == 4 &&
                        method.parameterTypes.first() == "Lcom/google/android/material/bottomsheet/BottomSheetDialogFragment;"
                },
            )
            fpLauncher.method.addInstructions(0, "return-void")
            val cLauncher = app.morphe.patches.shared.LocaleUtils.cleanClassName(fpLauncher.originalClassDef.type)
            hookedMethods.add("$cLauncher.showBottomSheetPrompt")
        } catch (e: Exception) {
            println("[Disable Prompts] Search switch bottomsheet hook note: ${e.message}")
        }

        // 3. Donation Promotional Click Handler: neutralize click action
        try {
            val fp3 = Fingerprint(
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
                strings = listOf("https://login.vivaldi.net/profile/donations#mtm_campaign=Android-Donate-SearchEngineSwitchDialog"),
            )
            fp3.method.addInstructions(0, "return-void")
            val c3 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp3.originalClassDef.type)
            hookedMethods.add("$c3.onClick")
        } catch (e: Exception) {
            println("[Disable Prompts] Donation click handler hook note: ${e.message}")
        }

        // 4. Privacy Report Notification Receiver: neutralize onReceive handler
        try {
            Fingerprint(
                definingClass = "Lorg/vivaldi/browser/prompts/PrivacyReportNotificationReceiver;",
                name = "onReceive",
                returnType = "V",
                parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            ).method.apply {
                addInstructions(0, "return-void")
                hookedMethods.add("PrivacyReportNotificationReceiver.onReceive")
            }
        } catch (e: Exception) {
            println("[Disable Prompts] PrivacyReport onReceive hook note: ${e.message}")
        }

        // 5. Privacy Report Notification Receiver: neutralize periodic alarm scheduler
        try {
            Fingerprint(
                definingClass = "Lorg/vivaldi/browser/prompts/PrivacyReportNotificationReceiver;",
                returnType = "V",
                parameters = listOf("Landroid/content/Context;"),
            ).method.apply {
                addInstructions(0, "return-void")
                hookedMethods.add("PrivacyReportNotificationReceiver.alarmScheduler")
            }
        } catch (e: Exception) {
            println("[Disable Prompts] PrivacyReport scheduler hook note: ${e.message}")
        }

        // 6. Default Browser Promo Notification Receiver: neutralize onReceive handler
        try {
            Fingerprint(
                definingClass = "Lorg/vivaldi/browser/prompts/DefaultBrowserNotificationReceiver;",
                name = "onReceive",
                returnType = "V",
                parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            ).method.apply {
                addInstructions(0, "return-void")
                hookedMethods.add("DefaultBrowserNotificationReceiver.onReceive")
            }
        } catch (e: Exception) {
            println("[Disable Prompts] DefaultBrowser onReceive hook note: ${e.message}")
        }

        // 7. Hide Vivaldia game from App Menu (default to false)
        try {
            val fpGameMenu = Fingerprint(
                strings = listOf("show_vivaldi_game_in_menu"),
                custom = { method, _ -> method.parameterTypes.isEmpty() },
            )
            val patched = replaceNarrowLiteralNearString(
                method = fpGameMenu.method,
                targetString = "show_vivaldi_game_in_menu",
                searchOffsets = listOf(1, 2, 3),
                expectedLiteral = 1,
            ) { reg -> "const/4 v$reg, 0x0" }
            if (patched) {
                hookedMethods.add("AppMenuPropertiesDelegate.hideVivaldiaGame")
            }
        } catch (e: Exception) {
            println("[Disable Prompts] Vivaldia menu hook note: ${e.message}")
        }

        // 8. Vivaldia Game Preference: set default switch state to false
        try {
            val fpGamePref = Fingerprint(
                definingClass = "Lorg/vivaldi/browser/preferences/VivaldiGamePreference;",
                strings = listOf("show_vivaldi_game_in_menu"),
            )
            val patched = replaceNarrowLiteralNearString(
                method = fpGamePref.method,
                targetString = "show_vivaldi_game_in_menu",
                searchOffsets = listOf(-1, -2, -3),
                expectedLiteral = 1,
            ) { reg -> "const/4 v$reg, 0x0" }
            if (patched) {
                hookedMethods.add("VivaldiGamePreference.defaultOff")
            }
        } catch (e: Exception) {
            println("[Disable Prompts] Vivaldia game pref hook note: ${e.message}")
        }

        // 9. MainSettings: Unconditionally strip Rate Vivaldi and Default Browser promo cards in onStart
        try {
            val fpMainSettings = Fingerprint(
                definingClass = "Lorg/chromium/chrome/browser/settings/MainSettings;",
                strings = listOf("rate_vivaldi", "default_browser_promo"),
            )
            val removePrefMethod = fpMainSettings.originalClassDef.methods.firstOrNull {
                it.parameterTypes == listOf("Ljava/lang/String;") && it.returnType == "V"
            }?.name
            if (removePrefMethod != null) {
                val fpOnStart = Fingerprint(
                    definingClass = "Lorg/chromium/chrome/browser/settings/MainSettings;",
                    name = "onStart",
                    parameters = emptyList(),
                    returnType = "V",
                )
                fpOnStart.method.ensureRegisterCount(2)
                fpOnStart.method.addInstructions(
                    0,
                    """
                        const-string v0, "rate_vivaldi"
                        invoke-virtual {p0, v0}, Lorg/chromium/chrome/browser/settings/MainSettings;->$removePrefMethod(Ljava/lang/String;)V
                        const-string v0, "default_browser_promo"
                        invoke-virtual {p0, v0}, Lorg/chromium/chrome/browser/settings/MainSettings;->$removePrefMethod(Ljava/lang/String;)V
                    """,
                )
                val cMain = app.morphe.patches.shared.LocaleUtils.cleanClassName(fpMainSettings.originalClassDef.type)
                hookedMethods.add("$cMain.onStartStripCards")
            } else {
                println("[Disable Prompts] MainSettings: removePreference method not found")
            }
        } catch (e: Exception) {
            println("[Disable Prompts] MainSettings promo cleanup hook note: ${e.message}")
        }

        // 10. DefaultBrowserPromoPreference: force setVisible(false) on attach
        try {
            val fpPromoPref = Fingerprint(
                definingClass = "Lorg/vivaldi/browser/preferences/DefaultBrowserPromoPreference;",
                parameters = emptyList(),
                returnType = "V",
                custom = { method, _ -> method.name != "<init>" },
            )
            fpPromoPref.method.ensureRegisterCount(2)
            fpPromoPref.method.addInstructions(
                0,
                """
                    invoke-super {p0}, Landroidx/preference/Preference;->u()V
                    const/4 v0, 0x0
                    invoke-virtual {p0, v0}, Landroidx/preference/Preference;->b0(Z)V
                    return-void
                """,
            )
            hookedMethods.add("DefaultBrowserPromoPreference.uHide")
        } catch (e: Exception) {
            println("[Disable Prompts] DefaultBrowserPromoPreference hook note: ${e.message}")
        }

        // 11. Vivaldi Default Browser Role Helper: force isDefaultBrowser -> true and neutralize role request prompt
        try {
            val fpRoleHelper = Fingerprint(
                returnType = "Z",
                parameters = listOf("Landroid/content/Context;"),
                strings = listOf("android.app.role.BROWSER"),
                custom = { _, classDef ->
                    classDef.methods.any {
                        it.parameterTypes == listOf("Landroid/app/Activity;") && it.returnType == "V"
                    }
                },
            )
            fpRoleHelper.method.ensureRegisterCount(1)
            fpRoleHelper.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )
            val roleClass = fpRoleHelper.originalClassDef.type
            val cleanRoleClass = app.morphe.patches.shared.LocaleUtils.cleanClassName(roleClass)
            hookedMethods.add("$cleanRoleClass.isDefaultBrowserTrue")

            val fpRolePrompt = Fingerprint(
                definingClass = roleClass,
                returnType = "V",
                parameters = listOf("Landroid/app/Activity;"),
            )
            fpRolePrompt.method.addInstructions(0, "return-void")
            hookedMethods.add("$cleanRoleClass.rolePromptNeutralized")
        } catch (e: Exception) {
            println("[Disable Prompts] Default browser role helper hook note: ${e.message}")
        }

        // 12. Chromium Upstream Default Browser Promo Utils: neutralize promo eligibility checks
        try {
            val fpChromiumPromo = Fingerprint(
                returnType = "Z",
                strings = listOf("disable-default-browser-promo", "android.app.role.BROWSER"),
                parameters = listOf("Landroid/content/Context;"),
            )
            fpChromiumPromo.method.ensureRegisterCount(1)
            fpChromiumPromo.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
            val promoClass = fpChromiumPromo.originalClassDef.type
            val cleanPromoClass = app.morphe.patches.shared.LocaleUtils.cleanClassName(promoClass)
            hookedMethods.add("$cleanPromoClass.promoEligibilityFalse")

            val fpChromiumPromo2 = Fingerprint(
                definingClass = promoClass,
                returnType = "Z",
                parameters = listOf("Landroid/content/Context;", "I"),
            )
            fpChromiumPromo2.method.ensureRegisterCount(1)
            fpChromiumPromo2.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
            hookedMethods.add("$cleanPromoClass.promoEligibilityWithRoleFalse")
        } catch (e: Exception) {
            println("[Disable Prompts] Chromium DefaultBrowserPromoUtils hook note: ${e.message}")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Disable Prompts] Neutralized ${hookedMethods.size} promo handlers across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}

private fun replaceNarrowLiteralNearString(
    method: MutableMethod,
    targetString: String,
    searchOffsets: List<Int>,
    expectedLiteral: Int,
    replacementSmali: (Int) -> String,
): Boolean {
    val instructions = method.implementation?.instructions?.toList() ?: return false
    val strIdx = instructions.indexOfFirst {
        it.opcode == Opcode.CONST_STRING &&
            ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == targetString
    }
    if (strIdx < 0) return false

    for (offset in searchOffsets) {
        val targetIdx = strIdx + offset
        if (targetIdx < 0 || targetIdx >= instructions.size) continue
        val ins = instructions[targetIdx]
        if (ins.opcode != Opcode.CONST_4) continue
        val narrow = ins as? NarrowLiteralInstruction ?: continue
        if (narrow.narrowLiteral != expectedLiteral) continue
        val reg = (ins as OneRegisterInstruction).registerA
        method.replaceInstruction(targetIdx, replacementSmali(reg))
        return true
    }
    return false
}
