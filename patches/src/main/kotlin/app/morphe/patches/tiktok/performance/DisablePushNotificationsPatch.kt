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
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_LITE)

    execute {
        var patched = 0
        val isLite = packageMetadata.packageName == Constants.TIKTOK_LITE_PACKAGE_NAME

        val targetClass = if (isLite) {
            "Lcom/ss/android/ugc/aweme/legoImpl/task/PushInitTask;"
        } else {
            "Lcom/ss/android/ugc/aweme/legoImp/task/InitPushTask;"
        }

        Fingerprint(
            definingClass = targetClass,
            name = "run",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        println("[Disable Push Notifications] Neutralized $targetClass.run() -> Background push polling & wake locks disabled.")
        patched++

        if (patched == 0) {
            println("[Disable Push Notifications] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Disable Push Notifications] Applied $patched push notification suppression hook(s).")
        }
    }
}
