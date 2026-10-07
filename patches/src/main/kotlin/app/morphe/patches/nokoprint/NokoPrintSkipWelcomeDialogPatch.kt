package app.morphe.patches.nokoprint

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val PRIVACY_ACCEPTED_KEY = "privacy_accepted"

@Suppress("unused")
val nokoPrintSkipWelcomeDialogPatch = bytecodePatch(
    name = "Skip Welcome Dialog",
    description = "Suppresses the first-launch About/privacy dialog by applying the app's own accept action silently. The About entry in the menu keeps working.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

    execute {
        val acceptHandlerMethod = Fingerprint(
            name = "onClick",
            returnType = "V",
            parameters = listOf("Landroid/content/DialogInterface;", "I"),
            strings = listOf(PRIVACY_ACCEPTED_KEY),
        ).method

        val acceptInstructions = acceptHandlerMethod.implementation?.instructions?.toList()
            ?: error("[Skip Welcome Dialog] Accept handler has no instructions.")

        val prefsField = deriveSharedPreferencesField(acceptInstructions)
        val acceptMethod = derivePostConsentMethod(acceptInstructions)

        val methodParams = acceptMethod.parameterTypes.joinToString("")
        val methodRefStr = "${acceptMethod.definingClass}->${acceptMethod.name}($methodParams)${acceptMethod.returnType}"
        val fieldRefStr = "${prefsField.definingClass}->${prefsField.name}:${prefsField.type}"

        val builderMethod = Fingerprint(
            definingClass = "Lcom/nokoprint/ActivityHome;",
            returnType = "V",
            parameters = listOf("Z"),
            strings = listOf("purchase_sku", "purchase_store"),
        ).method

        requireLocalRegisters(builderMethod, 3)

        builderMethod.addInstructionsWithLabels(
            0,
            """
            if-eqz p1, :show_dialog
            iget-object v0, p0, $fieldRefStr
            invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
            move-result-object v0
            const-string v1, "$PRIVACY_ACCEPTED_KEY"
            const/4 v2, 0x1
            invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
            move-result-object v0
            invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
            invoke-virtual {p0}, $methodRefStr
            return-void
            """.trimIndent(),
            ExternalLabel("show_dialog", builderMethod.getInstruction(0)),
        )

        println("[Skip Welcome Dialog] Hooked ActivityHome first-launch dialog -> auto-accepts privacy notice via ${acceptMethod.name}().")
    }
}

private fun requireLocalRegisters(method: Method, minLocalRegisters: Int) {
    val impl = method.implementation
        ?: error("[Skip Welcome Dialog] Dialog builder has no implementation.")
    val parameterRegisters = 1 + method.parameterTypes.sumOf {
        if (it.toString() == "J" || it.toString() == "D") 2 else 1
    }
    val localRegisters = impl.registerCount - parameterRegisters
    check(localRegisters >= minLocalRegisters) {
        "[Skip Welcome Dialog] Dialog builder has only $localRegisters local registers; the hook needs v0-v2."
    }
}

private fun deriveSharedPreferencesField(instructions: List<Instruction>): FieldReference =
    instructions.firstNotNullOfOrNull { instruction ->
        if (instruction.opcode == Opcode.IGET_OBJECT) {
            instruction.getReference<FieldReference>()
                ?.takeIf { it.type == "Landroid/content/SharedPreferences;" }
        } else {
            null
        }
    } ?: error("[Skip Welcome Dialog] Failed to derive SharedPreferences FieldReference from accept handler.")

private fun derivePostConsentMethod(instructions: List<Instruction>): MethodReference {
    val applyIndex = instructions.indexOfFirst { isSharedPreferencesApply(it) }
    if (applyIndex == -1) {
        error("[Skip Welcome Dialog] Failed to derive post-consent MethodReference from accept handler.")
    }

    return instructions.drop(applyIndex + 1).firstNotNullOfOrNull { instruction ->
        if (instruction.opcode == Opcode.INVOKE_VIRTUAL) {
            instruction.getReference<MethodReference>()
        } else {
            null
        }
    } ?: error("[Skip Welcome Dialog] Failed to derive post-consent MethodReference from accept handler.")
}

private fun isSharedPreferencesApply(instruction: Instruction): Boolean {
    val methodRef = instruction.getReference<MethodReference>() ?: return false
    return methodRef.definingClass == "Landroid/content/SharedPreferences\$Editor;" &&
        methodRef.name == "apply" &&
        methodRef.returnType == "V"
}
