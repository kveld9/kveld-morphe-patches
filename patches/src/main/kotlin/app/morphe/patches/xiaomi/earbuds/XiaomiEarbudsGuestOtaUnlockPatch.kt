package app.morphe.patches.xiaomi.earbuds

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val xiaomiEarbudsGuestOtaUnlockPatch = bytecodePatch(
    name = "Xiaomi Earbuds Guest OTA Unlock",
    description = "Bypasses mandatory Xiaomi account login checks for firmware update queries, allowing guest users to check and perform device OTA updates.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. SettingListItemModule$provideUpgradeSettingItem$1.invoke(Context)Boolean
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/di/SettingListItemModule\$provideUpgradeSettingItem\$1;",
            name = "invoke",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Ljava/lang/Boolean;",
        ).method.apply {
            val instructions = implementation?.instructions?.toList() ?: emptyList()
            val targetIdx = instructions.indexOfFirst { ins ->
                val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                ref?.definingClass == "Lcom/xiaomi/fitness/account/manager/AccountManager;" && ref.name == "isLogin"
            }
            check(targetIdx >= 0) { "Target invocation AccountManager.isLogin not found in provideUpgradeSettingItem" }

            val moveResultIns = instructions[targetIdx + 1]
            val reg = (moveResultIns as OneRegisterInstruction).registerA
            replaceInstruction(targetIdx, "nop")
            replaceInstruction(targetIdx + 1, "const/4 v$reg, 1")
            hookedMethods.add("provideUpgradeSettingItem.invoke")
        }

        // 2. DeviceSettingsFragment.initFunctionListener$lambda$26(DeviceSettingsFragment, View)V (Bluetooth OTA check)
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;",
            name = "initFunctionListener\$lambda\$26",
            parameters = listOf("Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;", "Landroid/view/View;"),
            returnType = "V",
        ).method.apply {
            val instructions = implementation?.instructions?.toList() ?: emptyList()
            val targetIdx = instructions.indexOfFirst { ins ->
                val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                ref?.definingClass == "Lcom/xiaomi/fitness/account/manager/AccountManager;" && ref.name == "isLogin"
            }
            check(targetIdx >= 0) { "Target invocation AccountManager.isLogin not found in initFunctionListener\$lambda\$26" }

            val moveResultIns = instructions[targetIdx + 1]
            val reg = (moveResultIns as OneRegisterInstruction).registerA
            replaceInstruction(targetIdx, "nop")
            replaceInstruction(targetIdx + 1, "const/4 v$reg, 1")
            hookedMethods.add("DeviceSettingsFragment.initFunctionListener\$lambda\$26")
        }

        // 3. DeviceSettingsFragment.initFunctionListener$lambda$30(DeviceSettingsFragment, View)V (USB OTA check)
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;",
            name = "initFunctionListener\$lambda\$30",
            parameters = listOf("Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;", "Landroid/view/View;"),
            returnType = "V",
        ).method.apply {
            val instructions = implementation?.instructions?.toList() ?: emptyList()
            val targetIdx = instructions.indexOfFirst { ins ->
                val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                ref?.definingClass == "Lcom/xiaomi/fitness/account/manager/AccountManager;" && ref.name == "isLogin"
            }
            check(targetIdx >= 0) { "Target invocation AccountManager.isLogin not found in initFunctionListener\$lambda\$30" }

            val moveResultIns = instructions[targetIdx + 1]
            val reg = (moveResultIns as OneRegisterInstruction).registerA
            replaceInstruction(targetIdx, "nop")
            replaceInstruction(targetIdx + 1, "const/4 v$reg, 1")
            hookedMethods.add("DeviceSettingsFragment.initFunctionListener\$lambda\$30")
        }

        println("[Xiaomi Earbuds Guest OTA Unlock] Unlocked guest firmware update checks across ${hookedMethods.size} targets.")
    }
}
