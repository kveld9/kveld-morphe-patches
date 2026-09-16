package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val disablePushNotificationsPatch = bytecodePatch(
    name = "Disable Push Notifications",
    description = "Neutralizes background push notification tasks and persistent socket wake locks to eliminate background battery drain.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

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

        println("[Disable Push Notifications] Applied $patched push notification suppression hook(s).")
    }
}
