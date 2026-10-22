package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Suppress("unused")
val braveSuppressInAppPromosPatch = bytecodePatch(
    name = "Block Popups & Rating Requests",
    description = "Blocks rating popups, review requests and promo dialogs.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Force the rate eligibility check (Context) to return false
        Fingerprint(
            returnType = "Z",
            strings = listOf("brave_rate_dont_show_again", "qa_force_rate_dialog"),
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            hookedMethods.add("RateEligibilityGate.d")
        }

        // 2. BraveAdsSignupDialog: Force eligibility check to false
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/dialogs/BraveAdsSignupDialog;",
            name = "a",
            returnType = "Z",
            parameters = emptyList(),
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            hookedMethods.add("BraveAdsSignupDialog.a")
        }

        // 3. BraveDialogFragment: Drop promotional dialog tags before display
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

        // 4. Hide the Rate Brave settings entry and its Support category
        val helperMethod = Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/settings/BraveMainPreferencesBase;",
            returnType = "V",
            parameters = listOf("Ljava/lang/String;", "Z"),
        ).method
        val helperSmali = "${helperMethod.definingClass}->${helperMethod.name}(Ljava/lang/String;Z)V"

        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/settings/BraveMainPreferencesBase;",
            returnType = "V",
            parameters = listOf("Landroid/os/Bundle;"),
            strings = listOf("rate_brave", "home_screen_widget"),
        ).method.apply {
            hideRateBravePreference(helperSmali)
            hookedMethods.add("BraveMainPreferencesBase.rateBrave")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        if (hookedMethods.isEmpty()) {
            println("[Block Popups & Rating Requests] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Block Popups & Rating Requests] Applied ${hookedMethods.size} hooks across ${targetClasses.size} classes -> in-app surveys & promo dialogs disabled.")
        }
    }
}

private data class HookRegisters(
    val fragReg: Int,
    val prefReg: Int,
    val tmpReg: Int,
)

private fun requireInvokeFragReg(
    instruction: Instruction,
    index: Int,
): Int {
    if (instruction.opcode != Opcode.INVOKE_VIRTUAL || instruction !is FiveRegisterInstruction) {
        throw PatchException("Expected INVOKE_VIRTUAL at index $index, found: ${instruction.opcode.name}")
    }
    return instruction.registerC
}

private fun requireRegister(
    instruction: Instruction,
    expectedOpcode: Opcode,
    index: Int,
): Int {
    if (instruction.opcode != expectedOpcode || instruction !is OneRegisterInstruction) {
        throw PatchException("Expected ${expectedOpcode.name} at index $index, found: ${instruction.opcode.name}")
    }
    return instruction.registerA
}

private fun resolveRegisters(
    instructions: List<Instruction>,
    stringIndex: Int,
): HookRegisters {
    val fragReg = requireInvokeFragReg(instructions[stringIndex + 1], stringIndex + 1)
    val prefReg = requireRegister(instructions[stringIndex + 2], Opcode.MOVE_RESULT_OBJECT, stringIndex + 2)
    val tmpReg = requireRegister(instructions[stringIndex + 3], Opcode.NEW_INSTANCE, stringIndex + 3)

    if (fragReg > 15 || prefReg > 15 || tmpReg > 15) {
        throw PatchException("Register out of 4-bit range: fragReg=$fragReg, prefReg=$prefReg, tmpReg=$tmpReg")
    }
    return HookRegisters(fragReg, prefReg, tmpReg)
}

private fun findNextConstStringIndex(
    instructions: List<Instruction>,
    startIndex: Int,
    targetReg: Int,
): Int {
    for (k in startIndex until instructions.size) {
        val inst = instructions[k] as? OneRegisterInstruction ?: continue
        if (inst.opcode == Opcode.CONST_STRING && inst.registerA == targetReg) {
            return k
        }
    }
    throw PatchException("Could not find CONST_STRING for register v$targetReg after index $startIndex")
}

private fun MutableMethod.hideRateBravePreference(helperSmali: String) {
    val instructions = implementation?.instructions?.toList()
        ?: throw PatchException("BraveMainPreferencesBase method has no implementation")

    val stringIndex = instructions.indexOfFirst { instruction ->
        instruction is ReferenceInstruction &&
            (instruction.reference as? StringReference)?.string == "rate_brave"
    }
    if (stringIndex == -1 || stringIndex + 3 >= instructions.size) {
        throw PatchException("Could not find 'rate_brave' sequence in BraveMainPreferencesBase")
    }

    val (fragReg, prefReg, tmpReg) = resolveRegisters(instructions, stringIndex)
    val insertIndex = findNextConstStringIndex(instructions, stringIndex + 4, prefReg)

    addInstructions(
        insertIndex,
        """
            const-string v$prefReg, "rate_brave"
            const/4 v$tmpReg, 0x0
            invoke-virtual {v$fragReg, v$prefReg, v$tmpReg}, $helperSmali
            const-string v$prefReg, "support_section"
            invoke-virtual {v$fragReg, v$prefReg, v$tmpReg}, $helperSmali
        """.trimIndent(),
    )
}
