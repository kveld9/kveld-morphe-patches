package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.cleanClassName
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

val gboardTranslateLimitPatch = bytecodePatch(
    name = "Remove Translate Limit",
    description = "Removes the character limit for inline translation.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        val fp = Fingerprint(
            definingClass = "Lcom/google/android/apps/inputmethod/libs/translate/TranslateKeyboard;",
            name = "dE",
            parameters = listOf(
                "Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;",
                "Lcom/google/android/libraries/inputmethod/metadata/KeyboardViewDef;",
            ),
            returnType = "V",
        )

        val aInsnIndex = fp.method.instructions.indexOfFirst { insn ->
            insn.opcode.name.startsWith("iput") &&
                ((insn as? ReferenceInstruction)?.reference as? FieldReference)?.let { field ->
                    field.name == "a" && field.type == "Lcom/google/android/libraries/inputmethod/widgets/EditTextOnKeyboard;"
                } == true
        }

        if (aInsnIndex != -1) {
            val insn = fp.method.instructions.elementAt(aInsnIndex)
            val reg = (insn as? TwoRegisterInstruction)?.registerA ?: 4
            fp.method.addInstructions(
                aInsnIndex + 1,
                """
                    invoke-static/range {v$reg .. v$reg}, ${Constants.GBOARD_EXTENSION_CLASS}->removeTranslateLimit(Landroid/widget/EditText;)V
                """.trimIndent(),
            )
            patched++
        }

        val targetClass = cleanClassName(fp.originalClassDef.type)
        if (patched == 0) {
            println("[Translate Limit] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Translate Limit] Injected $patched translate limit override(s) in $targetClass -> 200-character cap removed.")
        }
    }
}
