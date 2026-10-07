package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.cleanClassName
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val gboardHideNumberHintsPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(sharedExtensionPatch)

    dependsOn(gboardCoreIntegrityPatch)

    execute {
        // The small number hints (1-0 above the letter row) ship in an alternate
        // keyboard def that AdditionalImeDefCache loads only when the Phenotype
        // flag "enable_number_row" resolves true (Luee.c). Binding that flag
        // default to the Morphe preference selects the base def without hints.
        // Long-press input handling lives elsewhere, so long-press symbols keep
        // working while only the hint labels disappear.
        val fp = Fingerprint(
            definingClass = "Luee;",
            name = "c",
            parameters = listOf("Luet;", "Z", "Lahcy;"),
            returnType = "Lahcv;",
            filters = listOf(string("enable_number_row")),
        )
        val body = fp.method.instructions.toList()
        val stringIndex = fp.instructionMatches.first().index
        val invokeIndex = (stringIndex + 1 until minOf(stringIndex + 8, body.size)).firstOrNull { i ->
            val insn = body[i]
            insn.opcode == Opcode.INVOKE_DIRECT &&
                ((insn as? ReferenceInstruction)?.reference as? MethodReference)?.let {
                    it.definingClass == "Laaxb;" && it.name == "<init>"
                } == true
        } ?: throw PatchException("[Hide Number Hints] Laaxb.<init> not found after \"enable_number_row\"")
        val reg = (body[invokeIndex] as FiveRegisterInstruction).registerE
        fp.method.addInstructions(
            invokeIndex,
            """
                invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isNumberHintsEnabled()Z
                move-result v$reg
            """.trimIndent(),
        )

        val targetClass = cleanClassName(fp.originalClassDef.type)
        println("[Hide Number Hints] Flag enable_number_row in $targetClass.c() -> base keyboard def controlled by preference.")
    }
}
