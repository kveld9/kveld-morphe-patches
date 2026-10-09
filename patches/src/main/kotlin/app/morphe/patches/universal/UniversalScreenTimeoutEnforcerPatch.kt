package app.morphe.patches.universal

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

// Mask to clear FLAG_KEEP_SCREEN_ON (0x00000080): ~0x80 = -0x81 (0xFF7F)
private const val MASK_CLEAR_FLAG_KEEP_SCREEN_ON = "-0x81"

private sealed class ScreenTimeoutMutation(val instructionIndex: Int) {
    class PrependInstructions(index: Int, val smali: String) : ScreenTimeoutMutation(index)
    class ReplaceWithNop(index: Int) : ScreenTimeoutMutation(index)
}

private enum class MutationCategory {
    VIEW,
    WINDOW,
    LAYOUT_PARAMS,
}

@Suppress("unused")
val universalScreenTimeoutEnforcerPatch = bytecodePatch(
    name = "Universal Screen Timeout Enforcer",
    description = "Forces the target application to respect system screen timeout and sleep timers by neutralizing keepScreenOn view calls and stripping FLAG_KEEP_SCREEN_ON from windows and layout parameters.",
    default = false,
) {
    execute {
        var viewHooks = 0
        var windowHooks = 0
        var layoutParamHooks = 0
        var skippedFlags = 0
        var touchedClasses = 0

        classDefForEach { classDef ->
            if (!hasAnyTargetInstruction(classDef)) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            var classModified = false

            mutableClass.methods.forEach { method ->
                val mutations = scanMethodMutations(
                    method = method,
                    onCategory = { category ->
                        when (category) {
                            MutationCategory.VIEW -> viewHooks++
                            MutationCategory.WINDOW -> windowHooks++
                            MutationCategory.LAYOUT_PARAMS -> layoutParamHooks++
                        }
                    },
                    onSkippedFlag = { skippedFlags++ },
                )

                if (mutations.isNotEmpty()) {
                    classModified = true
                    applyMutations(method, mutations)
                }
            }

            if (classModified) {
                touchedClasses++
            }
        }

        val totalHooks = viewHooks + windowHooks + layoutParamHooks
        if (totalHooks == 0) {
            println("[Universal Screen Timeout Enforcer] Target APK does not invoke keepScreenOn or FLAG_KEEP_SCREEN_ON APIs (0 components found).")
            return@execute
        }

        println(
            "[Universal Screen Timeout Enforcer] Applied $totalHooks screen timeout hook(s) across $touchedClasses class(es) " +
                "($viewHooks View/SurfaceHolder setKeepScreenOn, $windowHooks Window addFlags/setFlags, $layoutParamHooks LayoutParams flags)."
        )
        if (skippedFlags > 0) {
            println("[Universal Screen Timeout Enforcer] Skipped $skippedFlags flag site(s) with registers >= 16 (assembler addressing limit).")
        }
    }
}

private fun hasAnyTargetInstruction(classDef: ClassDef): Boolean {
    for (method in classDef.methods) {
        val instructions = method.instructionsOrNull ?: continue
        for (inst in instructions) {
            if (isTargetInstruction(inst)) return true
        }
    }
    return false
}

private fun isTargetInstruction(inst: Instruction): Boolean {
    return isSetKeepScreenOn(inst) ||
        isWindowAddFlags(inst) ||
        isWindowSetFlags(inst) ||
        isLayoutParamsFlagsPut(inst)
}

private fun isSetKeepScreenOn(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_VIRTUAL &&
        inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE &&
        inst.opcode != Opcode.INVOKE_INTERFACE &&
        inst.opcode != Opcode.INVOKE_INTERFACE_RANGE &&
        inst.opcode != Opcode.INVOKE_SUPER &&
        inst.opcode != Opcode.INVOKE_SUPER_RANGE
    ) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return methodRef.name == "setKeepScreenOn" &&
        methodRef.parameterTypes.size == 1 &&
        methodRef.parameterTypes[0] == "Z" &&
        methodRef.returnType == "V"
}

private fun isWindowAddFlags(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return methodRef.definingClass == "Landroid/view/Window;" &&
        methodRef.name == "addFlags" &&
        methodRef.parameterTypes.size == 1 &&
        methodRef.parameterTypes[0] == "I"
}

private fun isWindowSetFlags(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return methodRef.definingClass == "Landroid/view/Window;" &&
        methodRef.name == "setFlags" &&
        methodRef.parameterTypes.size == 2 &&
        methodRef.parameterTypes[0] == "I" &&
        methodRef.parameterTypes[1] == "I"
}

private fun isLayoutParamsFlagsPut(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.IPUT) return false
    val fieldRef = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return false
    return fieldRef.definingClass == "Landroid/view/WindowManager\$LayoutParams;" &&
        fieldRef.name == "flags" &&
        fieldRef.type == "I"
}

private fun checkViewInstruction(inst: Instruction, index: Int): Pair<ScreenTimeoutMutation, MutationCategory>? {
    if (!isSetKeepScreenOn(inst)) return null
    return ScreenTimeoutMutation.ReplaceWithNop(index) to MutationCategory.VIEW
}

private fun checkWindowInstruction(
    inst: Instruction,
    index: Int,
    onSkippedFlag: () -> Unit = {},
): Pair<ScreenTimeoutMutation, MutationCategory>? {
    if (!isWindowAddFlags(inst) && !isWindowSetFlags(inst)) return null
    val flagsReg = extractRegisterAt(inst, argIndex = 1) ?: return null
    if (flagsReg >= 16) {
        onSkippedFlag()
        return null
    }
    return ScreenTimeoutMutation.PrependInstructions(
        index,
        "and-int/lit16 v$flagsReg, v$flagsReg, $MASK_CLEAR_FLAG_KEEP_SCREEN_ON"
    ) to MutationCategory.WINDOW
}

private fun checkLayoutParamsInstruction(
    inst: Instruction,
    index: Int,
    onSkippedFlag: () -> Unit = {},
): Pair<ScreenTimeoutMutation, MutationCategory>? {
    if (!isLayoutParamsFlagsPut(inst)) return null
    val twoReg = inst as? TwoRegisterInstruction ?: return null
    val flagsReg = twoReg.registerA
    if (flagsReg >= 16) {
        onSkippedFlag()
        return null
    }
    return ScreenTimeoutMutation.PrependInstructions(
        index,
        "and-int/lit16 v$flagsReg, v$flagsReg, $MASK_CLEAR_FLAG_KEEP_SCREEN_ON"
    ) to MutationCategory.LAYOUT_PARAMS
}

private fun scanMethodMutations(
    method: Method,
    onCategory: (MutationCategory) -> Unit,
    onSkippedFlag: () -> Unit = {},
): List<ScreenTimeoutMutation> {
    val instructions = method.instructionsOrNull?.toList() ?: return emptyList()
    val mutations = mutableListOf<ScreenTimeoutMutation>()

    for ((index, inst) in instructions.withIndex()) {
        val match = checkViewInstruction(inst, index)
            ?: checkWindowInstruction(inst, index, onSkippedFlag)
            ?: checkLayoutParamsInstruction(inst, index, onSkippedFlag)
            ?: continue

        mutations.add(match.first)
        onCategory(match.second)
    }

    return mutations
}

private fun applyMutations(method: MutableMethod, mutations: List<ScreenTimeoutMutation>) {
    // Sort descending by instruction index to preserve preceding instruction indices
    val sortedMutations = mutations.sortedByDescending { it.instructionIndex }
    for (mutation in sortedMutations) {
        when (mutation) {
            is ScreenTimeoutMutation.PrependInstructions -> {
                method.addInstructions(mutation.instructionIndex, mutation.smali)
            }
            is ScreenTimeoutMutation.ReplaceWithNop -> {
                method.replaceInstruction(mutation.instructionIndex, "nop")
            }
        }
    }
}

private fun extractRegisterAt(inst: Instruction, argIndex: Int): Int? {
    return when (inst) {
        is FiveRegisterInstruction -> when (argIndex) {
            0 -> inst.registerC
            1 -> inst.registerD
            2 -> inst.registerE
            3 -> inst.registerF
            4 -> inst.registerG
            else -> null
        }
        is RegisterRangeInstruction -> inst.startRegister + argIndex
        else -> null
    }
}
