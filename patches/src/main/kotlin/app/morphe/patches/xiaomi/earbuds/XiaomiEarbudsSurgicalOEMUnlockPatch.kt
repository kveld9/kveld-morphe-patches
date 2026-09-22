package app.morphe.patches.xiaomi.earbuds

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val xiaomiEarbudsSurgicalOEMUnlockPatch = bytecodePatch(
    name = "Xiaomi Earbuds Surgical OEM Unlock",
    description = "Bypasses Xiaomi OEM hardware gating for SuperAivs (function 5009) and suppresses MIUI-specific spatial audio system dialog nags.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Bypass Xiaomi OEM device check for function 5009 in DeviceSettingsFragment.initFunctionItem
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;",
            name = "initFunctionItem",
            parameters = listOf(),
            returnType = "V",
        ).method.apply {
            val instructions = implementation?.instructions?.toList() ?: emptyList()
            val targetIdx = instructions.indexOfFirst { ins ->
                val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                ref?.definingClass == "Lcom/xiaomi/fitness/common/utils/RomUtils;" && ref.name == "isXiaomi"
            }
            check(targetIdx >= 0) { "Target invocation RomUtils.isXiaomi not found in DeviceSettingsFragment.initFunctionItem" }

            val moveResultIns = instructions[targetIdx + 1]
            val reg = (moveResultIns as OneRegisterInstruction).registerA
            replaceInstruction(targetIdx, "nop")
            replaceInstruction(targetIdx + 1, "const/4 v$reg, 0")
            hookedMethods.add("DeviceSettingsFragment.initFunctionItem(unlockFunction5009)")
        }

        // 2. Suppress SpatialAudioVM isShowDialog notification nag
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/ui/spatialaudio/SpatialAudioVM;",
            name = "isShowDialog",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("SpatialAudioVM.isShowDialog")
        }

        // 3. Bypass Xiaomi account requirement when clicking SuperAivs (function 5009) in DeviceSettingsFragment
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;",
            name = "initFunctionListener\$lambda\$24",
            parameters = listOf("Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;", "Landroid/view/View;"),
            returnType = "V",
        ).method.apply {
            val instructions = implementation?.instructions?.toList() ?: emptyList()
            val targetIdx = instructions.indexOfFirst { ins ->
                val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                ref?.definingClass == "Lcom/xiaomi/fitness/account/manager/AccountManager;" && ref.name == "isLogin"
            }
            check(targetIdx >= 0) { "Target invocation AccountManager.isLogin not found in initFunctionListener\$lambda\$24" }

            val moveResultIns = instructions[targetIdx + 1]
            val reg = (moveResultIns as OneRegisterInstruction).registerA
            replaceInstruction(targetIdx, "nop")
            replaceInstruction(targetIdx + 1, "const/4 v$reg, 1")
            hookedMethods.add("DeviceSettingsFragment.initFunctionListener\$lambda\$24(SuperAivs)")
        }

        println("[Xiaomi Earbuds Surgical OEM Unlock] Neutralized Xiaomi OEM restrictions across ${hookedMethods.size} targets (SuperAivs 5009 item, voice settings, and SpatialAudio dialog).")
    }
}
