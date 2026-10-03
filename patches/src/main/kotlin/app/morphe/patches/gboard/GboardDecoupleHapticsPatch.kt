package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val gboardDecoupleHapticsPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Hook SystemHapticSettingsHelper.c(Context) -> overrideSystemHapticStatus
        // Locate caller from PressEffectPlayerModuleProvider$Module.b (updateVibrationPreference)
        val fpModule = Fingerprint(
            strings = listOf("PressEffectPlayerModuleProvider.java", "updateVibrationPreference"),
        )
        val methodUpdatePref = fpModule.method
        val helperCall = methodUpdatePref.implementation?.instructions?.firstOrNull { ins ->
            ins.opcode == Opcode.INVOKE_STATIC &&
                ((ins as? ReferenceInstruction)?.reference as? MethodReference)?.let { ref ->
                    ref.parameterTypes.size == 1 &&
                        ref.parameterTypes[0] == "Landroid/content/Context;" &&
                        ref.returnType == "I"
                } == true
        } as? ReferenceInstruction

        if (helperCall != null) {
            val helperRef = helperCall.reference as MethodReference
            val fpHapticHelper = Fingerprint(
                definingClass = helperRef.definingClass,
                name = helperRef.name,
                parameters = listOf("Landroid/content/Context;"),
                returnType = "I",
            )
            fpHapticHelper.method.apply {
                ensureRegisterCount(2)
                addInstructions(
                    0,
                    """
                        invoke-static {p0}, ${Constants.GBOARD_EXTENSION_CLASS}->overrideSystemHapticStatus(Landroid/content/Context;)I
                        move-result v0
                        if-gez v0, :cond_continue_original_status
                        return v0
                        :cond_continue_original_status
                    """.trimIndent(),
                )
            }
            patched++
        }

        // 2. Prevent updateVibrationPreference from resetting enable_vibrate_on_keypress
        methodUpdatePref.apply {
            ensureRegisterCount(1)
            addInstructions(
                0,
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isDecoupleTouchFeedbackEnabled()Z
                    move-result v0
                    if-eqz v0, :cond_skip_morphe_update_pref
                    return-void
                    :cond_skip_morphe_update_pref
                """.trimIndent(),
            )
        }
        patched++

        // 3. Hook PressEffectPlayerModuleProvider$Module.a() -> overrideHapticFeedbackSetting
        val methodSyncSetting = fpModule.classDef.methods.firstOrNull { m ->
            m.name == "a" && m.returnType == "V" && m.parameters.isEmpty()
        }
        if (methodSyncSetting != null) {
            val boolIndices = methodSyncSetting.implementation?.instructions?.withIndex()
                ?.filter { ins ->
                    ins.value.opcode == Opcode.INVOKE_VIRTUAL &&
                        ((ins.value as? ReferenceInstruction)?.reference as? MethodReference)?.let { ref ->
                            ref.definingClass == "Ljava/lang/Boolean;" &&
                                ref.name == "booleanValue" &&
                                ref.returnType == "Z"
                        } == true
                }
                ?.map { it.index }
                ?.toList() ?: emptyList()

            boolIndices.asReversed().forEach { idx ->
                val nextIns = methodSyncSetting.implementation?.instructions?.elementAtOrNull(idx + 1)
                val reg = (nextIns as? OneRegisterInstruction)?.registerA ?: 2
                methodSyncSetting.addInstructions(
                    idx + 2,
                    """
                        invoke-static {v$reg}, ${Constants.GBOARD_EXTENSION_CLASS}->overrideHapticFeedbackSetting(Z)Z
                        move-result v$reg
                    """.trimIndent(),
                )
                patched++
            }
        }

        // 4. Hook PressEffectPlayerImpl:
        //    a) Replace performHapticFeedback(int) with GboardExtension.performHapticFeedback(View, int)
        //    b) In f(int), replace USAGE_TOUCH (18) with GboardExtension.getVibrationUsage(18)
        val fpPlayer = Fingerprint(
            strings = listOf(
                "PressEffectPlayerImpl.performBasicTapEffect",
                "PressEffectPlayerImpl.performKeyReleaseEffect",
            ),
        )
        val playerClass = fpPlayer.classDef
        for (m in playerClass.methods) {
            val impl = m.implementation ?: continue
            val hapticIndices = impl.instructions.withIndex()
                .filter { ins ->
                    ins.value.opcode == Opcode.INVOKE_VIRTUAL &&
                        ((ins.value as? ReferenceInstruction)?.reference as? MethodReference)?.let { ref ->
                            ref.definingClass == "Landroid/view/View;" &&
                                ref.name == "performHapticFeedback" &&
                                ref.parameterTypes.size == 1 &&
                                ref.parameterTypes[0] == "I" &&
                                ref.returnType == "Z"
                        } == true
                }
                .map {
                    val ins35c = it.value as Instruction35c
                    it.index to (ins35c.registerC to ins35c.registerD)
                }
                .toList()

            hapticIndices.asReversed().forEach { (idx, regs) ->
                val (regC, regD) = regs
                m.replaceInstruction(
                    idx,
                    """
                        invoke-static {v$regC, v$regD}, ${Constants.GBOARD_EXTENSION_CLASS}->performHapticFeedback(Landroid/view/View;I)Z
                    """.trimIndent(),
                )
                patched++
            }

            if (m.name == "f") {
                val usageIndices = impl.instructions.withIndex()
                    .filter { ins ->
                        (ins.value.opcode == Opcode.CONST_16 || ins.value.opcode == Opcode.CONST_4) &&
                            (ins.value as? NarrowLiteralInstruction)?.narrowLiteral == 18
                    }
                    .map { it.index to (it.value as OneRegisterInstruction).registerA }
                    .toList()

                usageIndices.asReversed().forEach { (idx, reg) ->
                    m.addInstructions(
                        idx + 1,
                        """
                            invoke-static {v$reg}, ${Constants.GBOARD_EXTENSION_CLASS}->getVibrationUsage(I)I
                            move-result v$reg
                        """.trimIndent(),
                    )
                    patched++
                }
            }
        }

        // 5. Hook VibrationDurationPreference.an(int) -> overrideHapticFeedbackSetting
        val fpPref = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/preferencewidgets/VibrationDurationPreference;",
            name = "an",
            parameters = listOf("I"),
            returnType = "Ljava/lang/String;",
        )
        val methodPref = fpPref.method
        val prefBoolIndices = methodPref.implementation?.instructions?.withIndex()
            ?.filter { ins ->
                ins.value.opcode == Opcode.INVOKE_VIRTUAL &&
                    ((ins.value as? ReferenceInstruction)?.reference as? MethodReference)?.let { ref ->
                        ref.definingClass == "Ljava/lang/Boolean;" &&
                            ref.name == "booleanValue" &&
                            ref.returnType == "Z"
                    } == true
            }
            ?.map { it.index }
            ?.toList() ?: emptyList()

        prefBoolIndices.asReversed().forEach { idx ->
            val nextIns = methodPref.implementation?.instructions?.elementAtOrNull(idx + 1)
            val reg = (nextIns as? OneRegisterInstruction)?.registerA ?: 2
            methodPref.addInstructions(
                idx + 2,
                """
                    invoke-static {v$reg}, ${Constants.GBOARD_EXTENSION_CLASS}->overrideHapticFeedbackSetting(Z)Z
                    move-result v$reg
                """.trimIndent(),
            )
            patched++
        }

        println("[Decouple Haptics] Injected $patched independent haptics and touch feedback decoupling hook(s).")
    }
}
