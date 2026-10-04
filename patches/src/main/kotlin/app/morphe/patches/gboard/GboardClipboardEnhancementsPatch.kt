package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import app.morphe.patches.shared.clearTryBlocks
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gboardClipboardEnhancementsPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        // 1. Extend SQLite retention TTL & UI query cutoff window
        val fpTtl = Fingerprint(
            strings = listOf("getUnpinnedItemTimeLimitInMilliSeconds"),
            returnType = "J",
            parameters = listOf("Landroid/content/Context;"),
        )
        fpTtl.method.apply {
            clearTryBlocks()
            removeInstructions(0, implementation!!.instructions.count())
            addInstructions(
                0,
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->getClipboardRetentionMillis()J
                    move-result-wide v0
                    return-wide v0
                """.trimIndent(),
            )
        }
        println("[Clipboard Enhancements] Hooked retention limit to dynamic Morphe Patches preference.")
        patched++

        // 2. Raise UI unpinned clips throttle from 5 to dynamic limit
        val fpLoader = Fingerprint(
            strings = listOf("timestamp DESC limit %d", "(%s & %d) = 0 AND (%s & %d) = 0 AND %s >= ?"),
            returnType = "Ljava/lang/Object;",
            parameters = emptyList(),
        )
        val method = fpLoader.method
        val targetIndices = method.implementation?.instructions?.withIndex()
            ?.filter {
                it.value.opcode == Opcode.CONST_4 &&
                    (it.value as? NarrowLiteralInstruction)?.narrowLiteral == 5
            }
            ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
            ?.toList() ?: emptyList()

        targetIndices.asReversed().forEach { (idx, reg) ->
            method.replaceInstruction(
                idx,
                "invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->getClipboardUnpinnedLimit()I",
            )
            method.addInstructions(
                idx + 1,
                "move-result v$reg",
            )
        }
        if (targetIndices.isNotEmpty()) {
            println("[Clipboard Enhancements] Injected dynamic unpinned clips limit hook across ${targetIndices.size} opcode site(s).")
            patched++
        }

        // 3. Customize clipboard grid columns
        val fpKeyboard = Fingerprint(
            definingClass = "Lcom/google/android/apps/inputmethod/libs/clipboard/ClipboardKeyboard;",
            returnType = "I",
            parameters = emptyList(),
        )
        fpKeyboard.method.apply {
            clearTryBlocks()
            removeInstructions(0, implementation!!.instructions.count())
            addInstructions(
                0,
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->getClipboardGridColumns()I
                    move-result v0
                    return v0
                """.trimIndent(),
            )
        }
        println("[Clipboard Enhancements] Hooked clipboard grid columns to dynamic Morphe Patches preference.")
        patched++

        println("[Clipboard Enhancements] Applied $patched dynamic clipboard enhancement hook(s).")
    }
}
