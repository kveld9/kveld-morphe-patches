package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gboardEnableGrammarCheckerPatch = bytecodePatch(
    name = "Enable Grammar Checker",
    description = "Unlocks Grammar check and Smart Compose / inline suggestions settings under Text correction preferences.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    dependsOn(gboardPhenotypeResiliencePatch)

    execute {
        var patched = 0

        // 1. Grammar check flag
        val fpGrammar = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("enable_grammar_checker")),
        )
        val matchGrammar = fpGrammar.instructionMatches.first().index
        val nextInsnGrammar = fpGrammar.method.getInstruction<Instruction>(matchGrammar + 1)
        val regGrammar = when (nextInsnGrammar) {
            is OneRegisterInstruction -> nextInsnGrammar.registerA
            is FiveRegisterInstruction -> nextInsnGrammar.registerD
            else -> 1
        }
        val insertGrammar = if (nextInsnGrammar is OneRegisterInstruction) matchGrammar + 2 else matchGrammar + 1
        fpGrammar.method.addInstructions(insertGrammar, "const/4 v$regGrammar, 0x1")
        patched++

        // 2. Smart Compose / Inline suggestions flag
        val fpInline = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("enable_inline_suggestions_on_client_side")),
        )
        val matchInline = fpInline.instructionMatches.first().index
        val nextInsnInline = fpInline.method.getInstruction<Instruction>(matchInline + 1)
        val regInline = when (nextInsnInline) {
            is OneRegisterInstruction -> nextInsnInline.registerA
            is FiveRegisterInstruction -> nextInsnInline.registerD
            else -> 1
        }
        val insertInline = if (nextInsnInline is OneRegisterInstruction) matchInline + 2 else matchInline + 1
        fpInline.method.addInstructions(insertInline, "const/4 v$regInline, 0x1")
        patched++

        val targetClass = LocaleUtils.cleanClassName(fpGrammar.originalClassDef.type)
        println("[Grammar Checker] Injected $patched flag override(s) into $targetClass -> Grammar Check & Smart Compose unlocked.")
    }
}
