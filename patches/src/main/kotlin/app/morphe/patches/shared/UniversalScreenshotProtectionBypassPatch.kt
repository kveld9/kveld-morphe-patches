package app.morphe.patches.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val MASK_CLEAR_FLAG_SECURE = "-0x2001"

private sealed class PendingMutation(val instructionIndex: Int) {
    class PrependInstructions(index: Int, val smali: String) : PendingMutation(index)
    class ReplaceInstruction(index: Int, val smali: String) : PendingMutation(index)
    class ReplaceWithNop(index: Int) : PendingMutation(index)
}

private val screenshotProtectionBypassResourcePatch = resourcePatch(
    name = "Screenshot Protection Bypass Resource",
    description = "Enables allowAudioPlaybackCapture on the application tag in AndroidManifest.xml.",
    default = false,
) {
    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            return@execute
        }

        document(manifestFile.absolutePath).use { doc ->
            val appNodes = doc.getElementsByTagName("application")
            if (appNodes.length > 0) {
                val appElem = appNodes.item(0) as? Element ?: return@use
                val currentSetting = if (appElem.hasAttributeNS(ANDROID_XML_NAMESPACE, "allowAudioPlaybackCapture")) {
                    appElem.getAttributeNS(ANDROID_XML_NAMESPACE, "allowAudioPlaybackCapture")
                } else if (appElem.hasAttribute("android:allowAudioPlaybackCapture")) {
                    appElem.getAttribute("android:allowAudioPlaybackCapture")
                } else {
                    null
                }
                if (currentSetting != "true") {
                    appElem.removeAttribute("android:allowAudioPlaybackCapture")
                    appElem.setAttributeNS(ANDROID_XML_NAMESPACE, "android:allowAudioPlaybackCapture", "true")
                    println("[Universal Screenshot Protection Bypass] Set android:allowAudioPlaybackCapture=\"true\" in AndroidManifest.xml.")
                }
            }
        }
    }
}

@Suppress("unused")
val universalScreenshotProtectionBypassPatch = bytecodePatch(
    name = "Universal Screenshot Protection Bypass",
    description = "Neutralizes FLAG_SECURE on windows, layout params, and SurfaceViews, unlocks audio playback capture, and suppresses Android 14+ screenshot and screen recording detection callbacks.",
    default = false,
) {
    dependsOn(screenshotProtectionBypassResourcePatch)

    execute {
        var windowHooks = 0
        var surfaceHooks = 0
        var audioHooks = 0
        var detectionHooks = 0
        var touchedClasses = 0

        classDefForEach { classDef ->
            val methods = classDef.methods
            var classHasMatch = false

            for (method in methods) {
                val instructions = method.instructionsOrNull ?: continue
                for (inst in instructions) {
                    if (isTargetInstruction(inst)) {
                        classHasMatch = true
                        break
                    }
                }
                if (classHasMatch) break
            }

            if (!classHasMatch) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            var classModified = false

            mutableClass.methods.forEach { method ->
                val instructions = method.instructionsOrNull?.toList() ?: return@forEach
                val mutations = mutableListOf<PendingMutation>()

                for ((index, inst) in instructions.withIndex()) {
                    val windowMutation = checkWindowInstruction(inst, index)
                    if (windowMutation != null) {
                        mutations.add(windowMutation)
                        windowHooks++
                        continue
                    }

                    val surfaceMutation = checkSurfaceInstruction(inst, index)
                    if (surfaceMutation != null) {
                        mutations.add(surfaceMutation)
                        surfaceHooks++
                        continue
                    }

                    val audioMutation = checkAudioInstruction(inst, index)
                    if (audioMutation != null) {
                        mutations.add(audioMutation)
                        audioHooks++
                        continue
                    }

                    val detectionMutations = checkDetectionInstruction(instructions, index)
                    if (detectionMutations.isNotEmpty()) {
                        mutations.addAll(detectionMutations)
                        detectionHooks++
                        continue
                    }
                }

                if (mutations.isNotEmpty()) {
                    classModified = true
                    // Apply in descending index order so prepending instructions does not invalidate prior indices
                    mutations.sortByDescending { it.instructionIndex }
                    for (mutation in mutations) {
                        when (mutation) {
                            is PendingMutation.PrependInstructions -> {
                                method.addInstructions(mutation.instructionIndex, mutation.smali)
                            }
                            is PendingMutation.ReplaceInstruction -> {
                                method.replaceInstruction(mutation.instructionIndex, mutation.smali)
                            }
                            is PendingMutation.ReplaceWithNop -> {
                                method.replaceInstruction(mutation.instructionIndex, "nop")
                            }
                        }
                    }
                }
            }

            if (classModified) {
                touchedClasses++
            }
        }

        val totalHooks = windowHooks + surfaceHooks + audioHooks + detectionHooks
        if (totalHooks == 0) {
            println("[Universal Screenshot Protection Bypass] Target APK does not invoke FLAG_SECURE, audio capture, or screenshot detection APIs (0 components found).")
            return@execute
        }

        println(
            "[Universal Screenshot Protection Bypass] Applied $totalHooks bypass hook(s) across $touchedClasses class(es) " +
                "($windowHooks window/layout flags, $surfaceHooks SurfaceView, $audioHooks audio capture, $detectionHooks detection/recents)."
        )
    }
}

private fun isTargetInstruction(inst: Instruction): Boolean {
    if (isWindowSetFlags(inst) || isWindowAddFlags(inst) || isLayoutParamsFlagsPut(inst)) return true
    if (isSurfaceViewSetSecure(inst)) return true
    if (isAudioCapturePolicyInstruction(inst)) return true
    if (isRegisterScreenCaptureCallback(inst) ||
        isUnregisterScreenCaptureCallback(inst) ||
        isSetRecentsScreenshotEnabled(inst) ||
        isAddScreenRecordingCallback(inst) ||
        isRemoveScreenRecordingCallback(inst)
    ) return true
    return false
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

private fun isSurfaceViewSetSecure(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return methodRef.definingClass == "Landroid/view/SurfaceView;" &&
        methodRef.name == "setSecure" &&
        methodRef.parameterTypes.size == 1 &&
        methodRef.parameterTypes[0] == "Z"
}

private fun isAudioCapturePolicyInstruction(inst: Instruction): Boolean {
    return isAudioAttributesSetAllowedCapturePolicy(inst) || isAudioManagerSetAllowedCapturePolicy(inst)
}

private fun isAudioAttributesSetAllowedCapturePolicy(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return methodRef.definingClass == "Landroid/media/AudioAttributes\$Builder;" &&
        methodRef.name == "setAllowedCapturePolicy" &&
        methodRef.parameterTypes.size == 1 &&
        methodRef.parameterTypes[0] == "I"
}

private fun isAudioManagerSetAllowedCapturePolicy(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return methodRef.definingClass == "Landroid/media/AudioManager;" &&
        methodRef.name == "setAllowedCapturePolicy" &&
        methodRef.parameterTypes.size == 1 &&
        methodRef.parameterTypes[0] == "I"
}

private fun isRegisterScreenCaptureCallback(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return methodRef.name == "registerScreenCaptureCallback" &&
        methodRef.parameterTypes.size == 2 &&
        (methodRef.definingClass == "Landroid/app/Activity;" ||
            methodRef.definingClass.endsWith("Activity;") ||
            methodRef.parameterTypes[1].contains("ScreenCaptureCallback"))
}

private fun isUnregisterScreenCaptureCallback(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return methodRef.name == "unregisterScreenCaptureCallback" &&
        methodRef.parameterTypes.size == 1 &&
        (methodRef.definingClass == "Landroid/app/Activity;" ||
            methodRef.definingClass.endsWith("Activity;") ||
            methodRef.parameterTypes[0].contains("ScreenCaptureCallback"))
}

private fun isSetRecentsScreenshotEnabled(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return methodRef.name == "setRecentsScreenshotEnabled" &&
        methodRef.parameterTypes.size == 1 &&
        methodRef.parameterTypes[0] == "Z" &&
        (methodRef.definingClass == "Landroid/app/Activity;" || methodRef.definingClass.endsWith("Activity;"))
}

private fun isAddScreenRecordingCallback(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_INTERFACE &&
        inst.opcode != Opcode.INVOKE_INTERFACE_RANGE &&
        inst.opcode != Opcode.INVOKE_VIRTUAL &&
        inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE
    ) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return methodRef.name == "addScreenRecordingCallback" &&
        methodRef.parameterTypes.size == 2 &&
        methodRef.returnType == "I"
}

private fun isRemoveScreenRecordingCallback(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.INVOKE_INTERFACE &&
        inst.opcode != Opcode.INVOKE_INTERFACE_RANGE &&
        inst.opcode != Opcode.INVOKE_VIRTUAL &&
        inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE
    ) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return methodRef.name == "removeScreenRecordingCallback" &&
        methodRef.parameterTypes.size == 1 &&
        methodRef.returnType == "V"
}

private fun checkWindowInstruction(inst: Instruction, index: Int): PendingMutation? {
    if (isWindowAddFlags(inst)) {
        val flagsReg = extractRegisterAt(inst, argIndex = 1) ?: return null
        if (flagsReg < 16) {
            return PendingMutation.PrependInstructions(index, "and-int/lit16 v$flagsReg, v$flagsReg, $MASK_CLEAR_FLAG_SECURE")
        }
    } else if (isWindowSetFlags(inst)) {
        val flagsReg = extractRegisterAt(inst, argIndex = 1) ?: return null
        if (flagsReg < 16) {
            return PendingMutation.PrependInstructions(index, "and-int/lit16 v$flagsReg, v$flagsReg, $MASK_CLEAR_FLAG_SECURE")
        }
    } else if (isLayoutParamsFlagsPut(inst)) {
        val twoReg = inst as? TwoRegisterInstruction ?: return null
        val flagsReg = twoReg.registerA
        if (flagsReg < 16) {
            return PendingMutation.PrependInstructions(index, "and-int/lit16 v$flagsReg, v$flagsReg, $MASK_CLEAR_FLAG_SECURE")
        }
    }
    return null
}

private fun checkSurfaceInstruction(inst: Instruction, index: Int): PendingMutation? {
    if (isSurfaceViewSetSecure(inst)) {
        val secureReg = extractRegisterAt(inst, argIndex = 1) ?: return null
        if (secureReg >= 256) return null
        val smali = if (secureReg < 16) "const/4 v$secureReg, 0x0" else "const/16 v$secureReg, 0x0"
        return PendingMutation.PrependInstructions(index, smali)
    }
    return null
}

private fun checkAudioInstruction(inst: Instruction, index: Int): PendingMutation? {
    if (isAudioCapturePolicyInstruction(inst)) {
        val policyReg = extractRegisterAt(inst, argIndex = 1) ?: return null
        if (policyReg >= 256) return null
        val smali = if (policyReg < 16) "const/4 v$policyReg, 0x1" else "const/16 v$policyReg, 0x1"
        return PendingMutation.PrependInstructions(index, smali)
    }
    return null
}

private fun checkDetectionInstruction(instructions: List<Instruction>, index: Int): List<PendingMutation> {
    val inst = instructions[index]
    if (isRegisterScreenCaptureCallback(inst) ||
        isUnregisterScreenCaptureCallback(inst) ||
        isRemoveScreenRecordingCallback(inst)
    ) {
        return listOf(PendingMutation.ReplaceWithNop(index))
    }

    if (isSetRecentsScreenshotEnabled(inst)) {
        val enabledReg = extractRegisterAt(inst, argIndex = 1) ?: return emptyList()
        if (enabledReg >= 256) return emptyList()
        val smali = if (enabledReg < 16) "const/4 v$enabledReg, 0x1" else "const/16 v$enabledReg, 0x1"
        return listOf(PendingMutation.PrependInstructions(index, smali))
    }

    if (isAddScreenRecordingCallback(inst)) {
        val mutations = mutableListOf<PendingMutation>()
        var canNeutralize = true
        if (index + 1 < instructions.size) {
            val next = instructions[index + 1]
            if (next.opcode == Opcode.MOVE_RESULT) {
                val reg = (next as? OneRegisterInstruction)?.registerA
                if (reg != null && reg < 256) {
                    val smaliConst = if (reg < 16) "const/4 v$reg, 0x0" else "const/16 v$reg, 0x0"
                    mutations.add(PendingMutation.ReplaceInstruction(index + 1, smaliConst))
                } else {
                    canNeutralize = false
                }
            }
        }
        if (canNeutralize) {
            mutations.add(PendingMutation.ReplaceWithNop(index))
            return mutations
        }
        return emptyList()
    }

    return emptyList()
}

private fun extractRegisterAt(inst: Instruction, argIndex: Int): Int? {
    return when (inst) {
        is FiveRegisterInstruction -> {
            when (argIndex) {
                0 -> inst.registerC
                1 -> inst.registerD
                2 -> inst.registerE
                3 -> inst.registerF
                4 -> inst.registerG
                else -> null
            }
        }
        is RegisterRangeInstruction -> inst.startRegister + argIndex
        else -> null
    }
}
