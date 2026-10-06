package app.morphe.patches.nokoprint

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
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

        val acceptInstructions = acceptHandlerMethod.implementation?.instructions
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

private fun deriveSharedPreferencesField(instructions: Iterable<*>): FieldReference {
    for (inst in instructions) {
        val refInst = inst as? ReferenceInstruction ?: continue
        if (refInst.opcode != Opcode.IGET_OBJECT) continue

        val fieldRef = refInst.reference as? FieldReference ?: continue
        if (fieldRef.type == "Landroid/content/SharedPreferences;") {
            return fieldRef
        }
    }
    error("[Skip Welcome Dialog] Failed to derive SharedPreferences FieldReference from accept handler.")
}

private fun derivePostConsentMethod(instructions: Iterable<*>): MethodReference {
    var seenApply = false
    for (inst in instructions) {
        val refInst = inst as? ReferenceInstruction ?: continue
        if (!seenApply) {
            if (isSharedPreferencesApply(refInst)) {
                seenApply = true
            }
            continue
        }

        if (refInst.opcode == Opcode.INVOKE_VIRTUAL) {
            val methodRef = refInst.reference as? MethodReference
            if (methodRef != null) {
                return methodRef
            }
        }
    }
    error("[Skip Welcome Dialog] Failed to derive post-consent MethodReference from accept handler.")
}

private fun isSharedPreferencesApply(refInst: ReferenceInstruction): Boolean {
    val methodRef = refInst.reference as? MethodReference ?: return false
    return methodRef.definingClass == "Landroid/content/SharedPreferences\$Editor;" &&
        methodRef.name == "apply" &&
        methodRef.returnType == "V"
}
