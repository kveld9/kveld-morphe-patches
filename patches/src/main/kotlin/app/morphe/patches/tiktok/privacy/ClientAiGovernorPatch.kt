package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val clientAiGovernorPatch = bytecodePatch(
    name = "Client-Side AI & Behavioral Profiling Governor",
    description = "Neutralizes on-device machine learning inference, Pitaya behavioral profiling tasks, and on-device ad re-ranking.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Neutralize PitayaBootLoader setup
        try {
            val setupFp = Fingerprint(
                definingClass = "Lcom/bytedance/pitaya/api/PitayaBootLoader;",
                name = "setup",
                returnType = "V",
            )
            setupFp.method.addInstructions(
                0,
                """
                return-void
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] PitayaBootLoader.setup note: ${e.message}")
        }

        // 2. Neutralize PitayaBootLoader.commitBootTaskBySettings
        try {
            val commitFp = Fingerprint(
                definingClass = "Lcom/bytedance/pitaya/api/PitayaBootLoader;",
                name = "commitBootTaskBySettings",
                returnType = "V",
            )
            commitFp.method.addInstructions(
                0,
                """
                return-void
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] commitBootTaskBySettings note: ${e.message}")
        }

        // 3. Neutralize PitayaBootLoader$BootTask.run
        try {
            val bootTaskRunFp = Fingerprint(
                definingClass = "Lcom/bytedance/pitaya/api/PitayaBootLoader\$BootTask;",
                name = "run",
                returnType = "V",
            )
            bootTaskRunFp.method.addInstructions(
                0,
                """
                return-void
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] BootTask.run note: ${e.message}")
        }

        println("[Client-Side AI & Behavioral Profiling Governor] Applied $patched client-side AI and profiling governors.")
    }
}
