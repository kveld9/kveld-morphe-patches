package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

val gboardUnlockWritingToolsPatch = bytecodePatch(
    name = "Unlock Writing Tools & Proofread AI",
    description = "Unlocks the full Writing Tools and Proofread AI suite in the suggestion strip, toolbar shortcuts, and Text correction settings across all languages.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    dependsOn(gboardCoreIntegrityPatch)

    val supportedLanguages by stringOption(
        key = "supportedLanguages",
        title = "Supported Language Tags",
        description = "Language tags allowed for Writing Tools and Proofread ('*' allows all languages universally, or specify comma-separated BCP-47 tags like 'en-US,es-ES').",
        default = "*",
    )

    execute {
        val targetLanguages = supportedLanguages?.takeIf { it.isNotBlank() } ?: "*"
        var patched = 0
        val targetClasses = mutableSetOf<String>()

        // 1. Core Writing Tools & Proofread Phenotype flags (Ljlw)
        val fpJlw = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("writing_helper")),
        )

        targetClasses.add(LocaleUtils.cleanClassName(fpJlw.originalClassDef.type))

        val jlwTargets = setOf(
            "writing_helper",
            "config_proofread",
            "writing_tools",
            "writing_tools_enable_stable_entrance",
            "enable_writing_tools_replace_button",
            "enable_writing_tools_suggest_style",
            "writing_helper_supported_language_tags",
            "proofread_supported_apps",
        )

        val jlwInstructions = fpJlw.method.implementation?.instructions?.toList() ?: emptyList()
        val jlwMatches = mutableListOf<Pair<Int, String>>()
        for ((idx, insn) in jlwInstructions.withIndex()) {
            val str = ((insn as? ReferenceInstruction)?.reference as? StringReference)?.string ?: continue
            if (str in jlwTargets) {
                jlwMatches.add(idx to str)
            }
        }

        // Process in descending index order so insertions never invalidate offsets of preceding matches
        for ((matchIndex, strRef) in jlwMatches.sortedByDescending { it.first }) {
            val nextInsn = fpJlw.method.getInstruction<Instruction>(matchIndex + 1)
            when (strRef) {
                "writing_helper_supported_language_tags" -> {
                    val reg = (nextInsn as? OneRegisterInstruction)?.registerA ?: 2
                    fpJlw.method.addInstructions(matchIndex + 2, "const-string v$reg, \"$targetLanguages\"")
                    patched++
                }
                "proofread_supported_apps" -> {
                    val reg = (nextInsn as? OneRegisterInstruction)?.registerA ?: 3
                    fpJlw.method.addInstructions(matchIndex + 2, "const-string v$reg, \"*\"")
                    patched++
                }
                else -> {
                    val reg = when (nextInsn) {
                        is OneRegisterInstruction -> nextInsn.registerA
                        is FiveRegisterInstruction -> nextInsn.registerD
                        else -> 1
                    }
                    val insertIndex = if (nextInsn is OneRegisterInstruction) matchIndex + 2 else matchIndex + 1
                    fpJlw.method.addInstructions(insertIndex, "const/4 v$reg, 0x1")
                    patched++
                }
            }
        }

        // 2. On-Device Proofread & AI Core LLM Phenotype flags (Lkrg)
        val fpKrg = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("enable_on_device_proofread")),
        )

        targetClasses.add(LocaleUtils.cleanClassName(fpKrg.originalClassDef.type))

        val krgTargets = setOf(
            "enable_on_device_proofread",
            "debug_service_enable_on_device_gen_ai",
            "enable_ai_core_llm",
        )

        val krgInstructions = fpKrg.method.implementation?.instructions?.toList() ?: emptyList()
        val krgMatches = mutableListOf<Pair<Int, String>>()
        for ((idx, insn) in krgInstructions.withIndex()) {
            val str = ((insn as? ReferenceInstruction)?.reference as? StringReference)?.string ?: continue
            if (str in krgTargets) {
                krgMatches.add(idx to str)
            }
        }

        for ((matchIndex, _) in krgMatches.sortedByDescending { it.first }) {
            val nextInsn = fpKrg.method.getInstruction<Instruction>(matchIndex + 1)
            val reg = when (nextInsn) {
                is OneRegisterInstruction -> nextInsn.registerA
                is FiveRegisterInstruction -> nextInsn.registerD
                else -> 1
            }
            val insertIndex = if (nextInsn is OneRegisterInstruction) matchIndex + 2 else matchIndex + 1
            fpKrg.method.addInstructions(insertIndex, "const/4 v$reg, 0x1")
            patched++
        }

        // 3. Toolbar Entrance V2 flag (Lveo)
        val fpVeo = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("enable_writing_tools_v2_on_toolbar")),
        )

        targetClasses.add(LocaleUtils.cleanClassName(fpVeo.originalClassDef.type))

        val matchVeo = fpVeo.instructionMatches.first().index
        val nextInsnVeo = fpVeo.method.getInstruction<Instruction>(matchVeo + 1)
        val regVeo = when (nextInsnVeo) {
            is OneRegisterInstruction -> nextInsnVeo.registerA
            is FiveRegisterInstruction -> nextInsnVeo.registerD
            else -> 1
        }
        val insertVeo = if (nextInsnVeo is OneRegisterInstruction) matchVeo + 2 else matchVeo + 1
        fpVeo.method.addInstructions(insertVeo, "const/4 v$regVeo, 0x1")
        patched++

        // 4. Writing Helper Chip in Spellchecker (Lxjj)
        val fpXjj = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("writing_helper_chip_in_spellchecker")),
        )

        targetClasses.add(LocaleUtils.cleanClassName(fpXjj.originalClassDef.type))

        val matchXjj = fpXjj.instructionMatches.first().index
        val nextInsnXjj = fpXjj.method.getInstruction<Instruction>(matchXjj + 1)
        val regXjj = when (nextInsnXjj) {
            is OneRegisterInstruction -> nextInsnXjj.registerA
            is FiveRegisterInstruction -> nextInsnXjj.registerD
            else -> 1
        }
        val insertXjj = if (nextInsnXjj is OneRegisterInstruction) matchXjj + 2 else matchXjj + 1
        fpXjj.method.addInstructions(insertXjj, "const/4 v$regXjj, 0x1")
        patched++

        println("[Writing Tools] Injected $patched flag override(s) across ${targetClasses.size} classes -> Writing Tools & Proofread AI unlocked.")
    }
}
