package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardSignatureBypassPatch = bytecodePatch(
    name = "Allow Modified APK",
    description = "Bypasses internal signature check to allow custom APK execution.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        Fingerprint(
            returnType = "V",
            strings = listOf("APK is signed by unrecognized certificates: "),
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
