package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

import app.morphe.patches.shared.replaceWithReturnVoid

val disableInAppUpdateNagsPatch = bytecodePatch(
    name = "Update Prompt Suppressor",
    description = "Neutralizes background update polling tasks and device ID check routines to prevent forced update popups.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. CheckUpdateChangeDeviceIDTaskHolder$Background
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/CheckUpdateChangeDeviceIDTaskHolder\$Background;",
            name = "run",
            returnType = "V",
        ).method.replaceWithReturnVoid()
        println("[Update Prompt Suppressor] Neutralized CheckUpdateChangeDeviceIDTaskHolder\$Background.run().")
        patched++

        // 2. CheckUpdateChangeDeviceIDTaskHolder$BootFinish
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/CheckUpdateChangeDeviceIDTaskHolder\$BootFinish;",
            name = "run",
            returnType = "V",
        ).method.replaceWithReturnVoid()
        println("[Update Prompt Suppressor] Neutralized CheckUpdateChangeDeviceIDTaskHolder\$BootFinish.run().")
        patched++

        // 3. CheckUpdateChangeDeviceIDTask (cold startup update check)
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/app/application/task/CheckUpdateChangeDeviceIDTask;",
            name = "run",
            returnType = "V",
        ).method.replaceWithReturnVoid()
        println("[Update Prompt Suppressor] Neutralized CheckUpdateChangeDeviceIDTask.run().")
        patched++

        println("[Update Prompt Suppressor] Disabled $patched update check tasks -> In-app update nag dialogs blocked.")
    }
}
