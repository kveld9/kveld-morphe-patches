package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gboardEnableAccessPointsMenuRedesignPatch = bytecodePatch(
    name = "Enable Access Points Menu Redesign",
    description = "Enables the redesigned access points menu bar and customization panel (Panel V2).",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val fingerprint = Fingerprint(
            definingClass = "Lpzb;",
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("enable_access_points_menu_redesign")),
        )

        val matchIndex = fingerprint.instructionMatches.first().index
        val reg = fingerprint.method.getInstruction<OneRegisterInstruction>(matchIndex + 1).registerA
        fingerprint.method.addInstructions(
            matchIndex + 2,
            "const/4 v$reg, 0x1",
        )

        val targetClass = app.morphe.patches.shared.LocaleUtils.cleanClassName(fingerprint.originalClassDef.type)
        println("[Access Points Redesign] Injected flag override into $targetClass.<clinit>() at opcode index ${matchIndex + 2}")
    }
}
