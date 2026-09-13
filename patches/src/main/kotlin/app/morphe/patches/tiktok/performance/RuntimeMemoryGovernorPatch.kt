package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val runtimeMemoryGovernorPatch = bytecodePatch(
    name = "Runtime Memory Governor",
    description = "Optimizes Fresco bitmap memory pool retention and suppresses aggressive animated frame allocations to reduce memory pressure.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. FrescoFrameCache frame getter (LIZJ in modern / LIZLLL in legacy) -> return null
        try {
            val frameGetter = try {
                Fingerprint(
                    definingClass = "Lcom/facebook/fresco/animation/bitmap/cache/FrescoFrameCache;",
                    name = "LIZJ",
                    custom = { method, _ -> method.returnType.startsWith("L") },
                ).method
            } catch (e: Exception) {
                Fingerprint(
                    definingClass = "Lcom/facebook/fresco/animation/bitmap/cache/FrescoFrameCache;",
                    name = "LIZLLL",
                    custom = { method, _ -> method.returnType.startsWith("L") },
                ).method
            }

            frameGetter.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
            println("[RuntimeMemoryGovernor] Capped FrescoFrameCache frame getter -> Frame bitmap memory retention bounded.")
            patched++
        } catch (e: Exception) {
            println("[RuntimeMemoryGovernor] FrescoFrameCache frame getter note: ${e.message}")
        }

        // 2. FrescoFrameCache parameterized frame getter (LJFF in modern / LJ in legacy) -> return null
        try {
            val frameCacheGetter = try {
                Fingerprint(
                    definingClass = "Lcom/facebook/fresco/animation/bitmap/cache/FrescoFrameCache;",
                    name = "LJFF",
                    custom = { method, _ -> method.returnType.startsWith("L") },
                ).method
            } catch (e: Exception) {
                Fingerprint(
                    definingClass = "Lcom/facebook/fresco/animation/bitmap/cache/FrescoFrameCache;",
                    name = "LJ",
                    custom = { method, _ -> method.returnType.startsWith("L") },
                ).method
            }

            frameCacheGetter.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
            println("[RuntimeMemoryGovernor] Capped FrescoFrameCache parameterized frame getter -> Frame cache allocation suppressed.")
            patched++
        } catch (e: Exception) {
            println("[RuntimeMemoryGovernor] FrescoFrameCache parameterized frame getter note: ${e.message}")
        }

        println("[RuntimeMemoryGovernor] Applied $patched Fresco memory tuning hooks.")
    }
}
