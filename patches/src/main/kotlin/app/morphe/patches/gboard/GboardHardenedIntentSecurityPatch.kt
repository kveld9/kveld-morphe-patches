package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants

val gboardHardenedIntentSecurityPatch = bytecodePatch(
    name = "Hardened Intent Security",
    description = "Enables Gboard internal external intent protection against unauthorized intent hijacking.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val fingerprint = Fingerprint(
            definingClass = "Luev;",
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("prevent_external_intents")),
        )

        val matchIndex = fingerprint.instructionMatches.first().index
        fingerprint.method.addInstructions(
            matchIndex + 2,
            "const/4 v1, 0x1",
        )

        val targetClass = app.morphe.patches.shared.LocaleUtils.cleanClassName(fingerprint.originalClassDef.type)
        println("[Hardened Intent Security] Injected flag override into $targetClass.<clinit>() at opcode index ${matchIndex + 2}")
    }
}
