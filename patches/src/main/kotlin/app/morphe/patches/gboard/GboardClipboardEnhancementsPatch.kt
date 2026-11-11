package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import app.morphe.patches.shared.clearTryBlocks
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

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

        // 2. Customize clipboard grid columns
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

        // 3. Per-item text clip character limit (text_clip_item_char_limit Phenotype long flag)
        val fpCharLimit = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("text_clip_item_char_limit")),
        )
        fpCharLimit.method.apply {
            val body = instructions.toList()
            val defaultIndex = fpCharLimit.instructionMatches.first().index + 1
            val default = body[defaultIndex]
            if (default.opcode !in setOf(Opcode.CONST_WIDE_16, Opcode.CONST_WIDE_32, Opcode.CONST_WIDE) ||
                body[defaultIndex + 1].opcode != Opcode.INVOKE_STATIC
            ) {
                throw PatchException("[Clipboard Enhancements] Unexpected text_clip_item_char_limit declaration shape")
            }
            val reg = (default as OneRegisterInstruction).registerA
            addInstructions(
                defaultIndex + 1,
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->getClipboardCharLimit()J
                    move-result-wide v$reg
                """.trimIndent(),
            )
        }
        println("[Clipboard Enhancements] Hooked per-clip character limit to dynamic Morphe Patches preference.")
        patched++

        // 4. Raise the clipboard adapter prune cap: pinning an item runs this path,
        //    which otherwise trims the unpinned list back to 5 timestamps.
        val fpAdapter = Fingerprint(
            strings = listOf("Incompatible type for view holder."),
        )
        val adapterClass = fpAdapter.matchAll().firstOrNull()?.method?.definingClass
            ?: throw PatchException(
                "[Clipboard Enhancements] Clipboard adapter anchor not found on this APK.",
            )

        val fpPrune = Fingerprint(
            definingClass = adapterClass,
            returnType = "V",
            parameters = emptyList(),
            custom = { method, _ ->
                method.implementation?.instructions?.let { sequence ->
                    sequence.any {
                        it.opcode == Opcode.CONST_4 &&
                            (it as? NarrowLiteralInstruction)?.narrowLiteral == 5
                    } && sequence.any {
                        (it as? ReferenceInstruction)?.reference?.toString() == "Ljava/util/Set;->size()I"
                    }
                } == true
            },
        )
        val pruneMethod = try {
            fpPrune.method
        } catch (e: Exception) {
            throw PatchException(
                "[Clipboard Enhancements] Unpinned prune method not found in $adapterClass: ${e.message}",
            )
        }
        pruneMethod.apply {
            val body = instructions.toList()
            val pruneIndex = body.indexOfFirst {
                it.opcode == Opcode.CONST_4 &&
                    (it as? NarrowLiteralInstruction)?.narrowLiteral == 5
            }
            if (pruneIndex < 0) {
                throw PatchException("[Clipboard Enhancements] Prune limit constant not found in $definingClass.")
            }
            val pruneReg = (body[pruneIndex] as OneRegisterInstruction).registerA
            replaceInstruction(
                pruneIndex,
                "invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->getClipboardUnpinnedLimit()I",
            )
            addInstructions(pruneIndex + 1, "move-result v$pruneReg")
        }
        println("[Clipboard Enhancements] Hooked unpinned prune cap in clipboard adapter -> dynamic preference.")
        patched++

        // 5. Dynamic unpinned caps and budget in clipboard loader
        val fpLoaderCaps = Fingerprint(
            strings = listOf("timestamp DESC limit %d", "(%s & %d) = 0 AND (%s & %d) = 0 AND %s >= ?"),
            returnType = "Ljava/lang/Object;",
            parameters = emptyList(),
        )
        val loaderMethod = try {
            fpLoaderCaps.method
        } catch (e: Exception) {
            throw PatchException(
                "[Clipboard Enhancements] Clipboard loader method not found: ${e.message}",
            )
        }

        val targetSites = loaderMethod.implementation?.instructions?.withIndex()
            ?.filter { iv ->
                val inst = iv.value
                val isConst100 = inst.opcode == Opcode.CONST_16 &&
                    (inst as? NarrowLiteralInstruction)?.narrowLiteral == 100
                val isRsub100 = inst.opcode == Opcode.RSUB_INT_LIT8 &&
                    (inst as? NarrowLiteralInstruction)?.narrowLiteral == 100
                isConst100 || isRsub100
            }
            ?.map { it.index to it.value }
            ?.toList() ?: emptyList()

        if (targetSites.size != 6) {
            throw PatchException(
                "[Clipboard Enhancements] Expected 6 residual 100 cap sites in clipboard loader, found ${targetSites.size}.",
            )
        }

        targetSites.asReversed().forEach { (idx, inst) ->
            when (inst.opcode) {
                Opcode.CONST_16 -> {
                    val reg = (inst as OneRegisterInstruction).registerA
                    loaderMethod.replaceInstruction(
                        idx,
                        "invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->getClipboardUnpinnedLimit()I",
                    )
                    loaderMethod.addInstructions(
                        idx + 1,
                        "move-result v$reg",
                    )
                }
                Opcode.RSUB_INT_LIT8 -> {
                    val twoReg = inst as TwoRegisterInstruction
                    val regA = twoReg.registerA
                    val regB = twoReg.registerB
                    loaderMethod.replaceInstruction(
                        idx,
                        "invoke-static {v$regB}, ${Constants.GBOARD_EXTENSION_CLASS}->getClipboardRemainingBudget(I)I",
                    )
                    loaderMethod.addInstructions(
                        idx + 1,
                        "move-result v$regA",
                    )
                }
                else -> {}
            }
        }
        println("[Clipboard Enhancements] Hooked remaining budget across ${targetSites.size} site(s).")
        patched++

        // 6. Remove residual 100 hardcoded cap in clipboard adapter prune path
        val fpAdapterPrune = Fingerprint(
            definingClass = adapterClass,
            returnType = "V",
            parameters = emptyList(),
            custom = { method, _ ->
                method.implementation?.instructions?.any {
                    it.opcode == Opcode.RSUB_INT_LIT8 &&
                        (it as? NarrowLiteralInstruction)?.narrowLiteral == 100
                } == true
            },
        )
        val adapterPruneMethod = try {
            fpAdapterPrune.method
        } catch (e: Exception) {
            throw PatchException(
                "[Clipboard Enhancements] Adapter prune method with residual 100 cap not found in $adapterClass: ${e.message}",
            )
        }

        val rsubSites = adapterPruneMethod.implementation?.instructions?.withIndex()
            ?.filter { iv ->
                val inst = iv.value
                inst.opcode == Opcode.RSUB_INT_LIT8 &&
                    (inst as? NarrowLiteralInstruction)?.narrowLiteral == 100
            }
            ?.map { it.index to it.value }
            ?.toList() ?: emptyList()

        if (rsubSites.isEmpty()) {
            throw PatchException("[Clipboard Enhancements] Residual 100 cap not found in adapter prune method.")
        }

        rsubSites.asReversed().forEach { (idx, inst) ->
            val twoReg = inst as TwoRegisterInstruction
            val regA = twoReg.registerA
            val regB = twoReg.registerB
            adapterPruneMethod.replaceInstruction(
                idx,
                "invoke-static {v$regB}, ${Constants.GBOARD_EXTENSION_CLASS}->getClipboardRemainingBudget(I)I",
            )
            adapterPruneMethod.addInstructions(
                idx + 1,
                "move-result v$regA",
            )
        }
        println("[Clipboard Enhancements] Hooked remaining budget in clipboard adapter.")
        patched++

        if (patched == 0) {
            println("[Clipboard Enhancements] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Clipboard Enhancements] Applied $patched dynamic clipboard enhancement hook(s).")
        }
    }
}
