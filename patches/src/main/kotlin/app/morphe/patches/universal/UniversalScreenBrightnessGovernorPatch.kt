package app.morphe.patches.universal

import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val universalScreenBrightnessGovernorPatch = bytecodePatch(
    name = "Universal Screen Brightness Governor",
    description = "Prevents applications from overriding display brightness (such as HDR video playback brightness boosts, in-app brightness sliders, or blinding screens) by neutralizing all direct writes to WindowManager.LayoutParams.screenBrightness.",
    default = false,
) {
    execute {
        var brightnessHooks = 0
        var touchedClasses = 0

        classDefForEach { classDef ->
            if (!hasAnyTargetInstruction(classDef)) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            var classModified = false

            mutableClass.methods.forEach { method ->
                val mutations = scanMethodMutations(method) {
                    brightnessHooks++
                }

                if (mutations.isNotEmpty()) {
                    classModified = true
                    applyMutations(method, mutations)
                }
            }

            if (classModified) {
                touchedClasses++
            }
        }

        if (brightnessHooks == 0) {
            println("[Universal Screen Brightness Governor] Target APK does not write to screenBrightness (0 components found).")
            return@execute
        }

        println(
            "[Universal Screen Brightness Governor] Applied $brightnessHooks screen brightness hook(s) across $touchedClasses class(es)."
        )
    }
}

private fun hasAnyTargetInstruction(classDef: ClassDef): Boolean {
    for (method in classDef.methods) {
        val instructions = method.instructionsOrNull ?: continue
        for (inst in instructions) {
            if (isLayoutParamsScreenBrightnessPut(inst)) return true
        }
    }
    return false
}

private fun isLayoutParamsScreenBrightnessPut(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.IPUT) return false
    val fieldRef = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return false
    return fieldRef.definingClass == "Landroid/view/WindowManager\$LayoutParams;" &&
        fieldRef.name == "screenBrightness" &&
        fieldRef.type == "F"
}

private fun scanMethodMutations(
    method: Method,
    onMatch: () -> Unit,
): List<Int> {
    val instructions = method.instructionsOrNull?.toList() ?: return emptyList()
    val mutations = mutableListOf<Int>()

    for ((index, inst) in instructions.withIndex()) {
        if (isLayoutParamsScreenBrightnessPut(inst)) {
            mutations.add(index)
            onMatch()
        }
    }

    return mutations
}

private fun applyMutations(method: MutableMethod, mutationIndices: List<Int>) {
    // Sort descending by instruction index to preserve preceding instruction indices
    val sortedIndices = mutationIndices.sortedDescending()
    for (index in sortedIndices) {
        method.replaceInstruction(index, "nop")
    }
}
