package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val networkTrafficGovernorPatch = bytecodePatch(
    name = "Network & Background Traffic Governor",
    description = "Throttles aggressive video preloading buffer and disables background push polling alarms to save cellular data and battery.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. PreloadStrategyConfig.isEnableBufferPreload() -> return false
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/video/preload/model/PreloadStrategyConfig;",
                name = "isEnableBufferPreload",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
            println("[NetworkTrafficGovernor] Hooked PreloadStrategyConfig.isEnableBufferPreload() -> Aggressive buffer preloading throttled.")
            patched++
        } catch (e: Exception) {
            println("[NetworkTrafficGovernor] PreloadStrategyConfig note: ${e.message}")
        }

        // 2. InitPushTask.run() -> return-void
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
                """,
            )
            println("[NetworkTrafficGovernor] Neutralized InitPushTask.run() -> Periodic background push polling & wake locks disabled.")
            patched++
        } catch (e: Exception) {
            println("[NetworkTrafficGovernor] InitPushTask note: ${e.message}")
        }

        println("[NetworkTrafficGovernor] Applied $patched network preloading and push alarm hooks.")
    }
}
