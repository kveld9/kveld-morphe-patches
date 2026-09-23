package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gboardTopToolbarItemCountPatch = bytecodePatch(
    name = "Top Toolbar Item Count",
    description = "Customizes the maximum number of access point icons displayed on the top toolbar.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    val itemCount by stringOption(
        key = "itemCount",
        title = "Toolbar item count",
        description = "Maximum number of access point icons displayed on the top toolbar (default: 5, range: 4..8).",
        default = "5",
        required = false,
    )

    execute {
        val parsedCount = itemCount?.trim()?.toLongOrNull()?.coerceIn(4L, 8L) ?: 5L
        var patched = 0

        // 1. Max access points on bar (AccessPointsBar.<clinit>)
        val fpMax = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("config_max_access_points")),
        )
        val matchMax = fpMax.instructionMatches.first().index
        val insnMax = fpMax.method.getInstruction<Instruction>(matchMax + 1)
        if (insnMax is OneRegisterInstruction && insnMax.opcode.name.lowercase().contains("const-wide")) {
            val regMax = insnMax.registerA
            fpMax.method.replaceInstruction(matchMax + 1, "const-wide/16 v$regMax, $parsedCount")
            patched++
        }

        // 2. Default access points on bar (qam.<clinit>)
        val fpDefault = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("config_default_access_points_num_on_bar")),
        )
        val matchDefault = fpDefault.instructionMatches.first().index
        val insnDefault = fpDefault.method.getInstruction<Instruction>(matchDefault + 1)
        if (insnDefault is OneRegisterInstruction && insnDefault.opcode.name.lowercase().contains("const-wide")) {
            val regDefault = insnDefault.registerA
            fpDefault.method.replaceInstruction(matchDefault + 1, "const-wide/16 v$regDefault, $parsedCount")
            patched++
        }

        val targetClass = LocaleUtils.cleanClassName(fpMax.originalClassDef.type)
        println("[Top Toolbar Item Count] Injected $patched toolbar limit override(s) in $targetClass -> max $parsedCount items.")
    }
}
