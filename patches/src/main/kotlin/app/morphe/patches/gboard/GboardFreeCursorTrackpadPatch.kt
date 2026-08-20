package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardFreeCursorTrackpadPatch = bytecodePatch(
    name = "Enable Free Cursor 2D Trackpad",
    description = "Unlocks Gboard's 2D Free Cursor navigation through the spacebar, including vertical cursor movement and the magnifier HUD.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        Fingerprint(
            definingClass = "Ltaw;",
            name = "f",
            parameters = listOf("Landroid/view/inputmethod/EditorInfo;"),
            returnType = "Z",
        ).method.addInstructions(
            0,
            "const/4 v0, 0x1\nreturn v0",
        )

        val moduleDef1Fingerprint = Fingerprint(
            definingClass = "Lszm;",
            name = "getModuleDef",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Lvsp;",
            filters = listOf(
                methodCall(
                    definingClass = "Lvsd;",
                    name = "k",
                    parameters = listOf("Lsrt;"),
                    returnType = "V",
                ),
            ),
        )
        val matchIndex1 = moduleDef1Fingerprint.instructionMatches.first().index
        moduleDef1Fingerprint.method.removeInstruction(matchIndex1)

        val moduleDef2Fingerprint = Fingerprint(
            definingClass = "Lszw;",
            name = "getModuleDef",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Lvsp;",
            filters = listOf(
                methodCall(
                    definingClass = "Lvsd;",
                    name = "k",
                    parameters = listOf("Lsrt;"),
                    returnType = "V",
                ),
            ),
        )
        val matchIndex2 = moduleDef2Fingerprint.instructionMatches.first().index
        moduleDef2Fingerprint.method.removeInstruction(matchIndex2)
    }
}
