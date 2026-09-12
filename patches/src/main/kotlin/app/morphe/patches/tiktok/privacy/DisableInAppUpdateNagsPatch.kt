package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val disableInAppUpdateNagsPatch = bytecodePatch(
    name = "Update Prompt Suppressor",
    description = "Neutralizes background update polling tasks and device ID check routines to prevent forced update popups.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. CheckUpdateChangeDeviceIDTaskHolder$Background
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/CheckUpdateChangeDeviceIDTaskHolder\$Background;",
                name = "run",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[DisableInAppUpdateNags] Neutralized CheckUpdateChangeDeviceIDTaskHolder\$Background.run().")
            patched++
        } catch (e: Exception) {
            println("[DisableInAppUpdateNags] Background task note: ${e.message}")
        }

        // 2. CheckUpdateChangeDeviceIDTaskHolder$BootFinish
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/CheckUpdateChangeDeviceIDTaskHolder\$BootFinish;",
                name = "run",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[DisableInAppUpdateNags] Neutralized CheckUpdateChangeDeviceIDTaskHolder\$BootFinish.run().")
            patched++
        } catch (e: Exception) {
            println("[DisableInAppUpdateNags] BootFinish task note: ${e.message}")
        }

        println("[DisableInAppUpdateNags] Disabled $patched update check tasks -> In-app update nag dialogs blocked.")
    }
}
