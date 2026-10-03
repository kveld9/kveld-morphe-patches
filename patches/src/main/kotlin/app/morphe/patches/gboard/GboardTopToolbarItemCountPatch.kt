package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

val gboardTopToolbarItemCountPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    extendWith("extensions/extension.mpe")

    execute {
        val defaultCount = Constants.GboardPrefs.DEFAULT_TOOLBAR_ITEM_COUNT.toLong()
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
            fpMax.method.replaceInstruction(matchMax + 1, "const-wide/16 v$regMax, $defaultCount")
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
            fpDefault.method.replaceInstruction(matchDefault + 1, "const-wide/16 v$regDefault, $defaultCount")
            patched++
        }

        // 3. Dynamic runtime limit hook on AccessPointsBar.i()
        val fpBar = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/accesspoint/widget/AccessPointsBar;",
            name = "i",
            parameters = emptyList(),
            returnType = "I",
        )
        fpBar.method.apply {
            clearTryBlocks()
            val insnCount = implementation?.instructions?.count() ?: 0
            if (insnCount > 0) {
                removeInstructions(0, insnCount)
            }
            ensureRegisterCount(1)
            addInstructions(
                0,
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->getToolbarItemCount()I
                    move-result v0
                    return v0
                """.trimIndent(),
            )
        }
        patched++

        // 4. Initialize field k in AccessPointsBar.<init> to dynamic runtime preference
        val fpBarInit = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/accesspoint/widget/AccessPointsBar;",
            name = "<init>",
            parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;"),
        )
        val kInsnIndex = fpBarInit.method.instructions.indexOfFirst {
            it.opcode.name.startsWith("iput") && ((it as? ReferenceInstruction)?.reference as? FieldReference)?.name == "k"
        }
        if (kInsnIndex != -1) {
            fpBarInit.method.addInstructions(
                kInsnIndex + 1,
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->getToolbarItemCount()I
                    move-result v4
                    iput v4, p0, Lcom/google/android/libraries/inputmethod/accesspoint/widget/AccessPointsBar;->k:I
                """.trimIndent(),
            )
            patched++
        }

        val targetClass = LocaleUtils.cleanClassName(fpMax.originalClassDef.type)
        println("[Top Toolbar Item Count] Injected $patched toolbar limit override(s) in $targetClass -> dynamic preference enabled.")
    }
}
