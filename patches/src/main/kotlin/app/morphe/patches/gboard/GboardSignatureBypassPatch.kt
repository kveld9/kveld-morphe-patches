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
        val fp = Fingerprint(
            returnType = "V",
            strings = listOf("APK is signed by unrecognized certificates: "),
        )
        fp.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        val targetClass = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp.originalClassDef.type)
        println("[Allow Modified APK] Neutralized signature validation check in $targetClass.${fp.method.name}()")
    }
}
