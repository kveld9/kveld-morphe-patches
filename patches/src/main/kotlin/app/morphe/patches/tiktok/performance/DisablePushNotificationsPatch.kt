package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val disablePushNotificationsPatch = bytecodePatch(
    name = "Disable Push Notifications",
    description = "Stops background push notifications.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/InitPushTask;",
                name = "run",
                parameters = listOf("Landroid/content/Context;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Disable Push Notifications] Neutralized InitPushTask.run() -> Background push polling & wake locks disabled.")
            patched++
        } catch (e: Exception) {
            println("[Disable Push Notifications] InitPushTask note: ${e.message}")
        }

        if (patched == 0) {
            println("[Disable Push Notifications] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Disable Push Notifications] Applied $patched push notification suppression hook(s).")
        }
    }
}
