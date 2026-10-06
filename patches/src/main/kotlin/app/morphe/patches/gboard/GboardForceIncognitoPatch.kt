package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.cleanClassName
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gboardForceIncognitoPatch: BytecodePatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(sharedExtensionPatch)

    execute {
        val fp1 = Fingerprint(
            definingClass = "Lsmn;",
            name = "z",
            parameters = listOf("Landroid/view/inputmethod/EditorInfo;"),
            returnType = "Z",
        )
        fp1.method.addInstructions(
            0,
            """
                invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isForceIncognitoEnabled()Z
                move-result v0
                if-eqz v0, :cond_skip_morphe_incognito_smn
                const/4 v0, 0x1
                return v0
                :cond_skip_morphe_incognito_smn
            """.trimIndent(),
        )

        val fp2 = Fingerprint(
            definingClass = "Lfoy;",
            name = "G",
            parameters = emptyList(),
            returnType = "Z",
        )
        fp2.method.addInstructions(
            0,
            """
                invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isForceIncognitoEnabled()Z
                move-result v0
                if-eqz v0, :cond_skip_morphe_incognito_foy
                invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isHideIncognitoIconEnabled()Z
                move-result v0
                if-eqz v0, :cond_show_incognito_mask
                const/4 v0, 0
                return v0
                :cond_show_incognito_mask
                const/4 v0, 1
                return v0
                :cond_skip_morphe_incognito_foy
            """.trimIndent(),
        )

        val onPrimaryClipChangedFingerprint = Fingerprint(
            name = "onPrimaryClipChanged",
            returnType = "V",
            parameters = emptyList(),
            filters = OpcodesFilter.opcodesToFilters(
                Opcode.INVOKE_STATIC,
                Opcode.MOVE_RESULT,
                Opcode.IF_EQZ,
                Opcode.RETURN_VOID,
            ),
            strings = listOf("clipboard_primary_uri", ""),
        )

        onPrimaryClipChangedFingerprint.method.apply {
            val patternMatch = onPrimaryClipChangedFingerprint.instructionMatches
            val moveResultIndex = patternMatch[1].index
            val reg = getInstruction<OneRegisterInstruction>(moveResultIndex).registerA
            addInstructions(
                moveResultIndex + 1,
                """
                    invoke-static {v$reg}, ${Constants.GBOARD_EXTENSION_CLASS}->overrideClipboardIncognito(Z)Z
                    move-result v$reg
                """.trimIndent(),
            )
        }

        val c1 = cleanClassName(fp1.originalClassDef.type)
        val c2 = cleanClassName(fp2.originalClassDef.type)
        println("[Force Incognito Mode] Hooked incognito predicates in $c1, $c2 & clipboard guard controlled by preference")
    }
}
