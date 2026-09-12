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

        // 1. FrescoFrameCache.LIZLLL() -> return null
        try {
            Fingerprint(
                definingClass = "Lcom/facebook/fresco/animation/bitmap/cache/FrescoFrameCache;",
                name = "LIZLLL",
                returnType = "Lcom/facebook/common/references/CloseableReference;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
            println("[RuntimeMemoryGovernor] Capped FrescoFrameCache.LIZLLL() -> Frame bitmap memory retention bounded.")
            patched++
        } catch (e: Exception) {
            println("[RuntimeMemoryGovernor] FrescoFrameCache.LIZLLL note: ${e.message}")
        }

        // 2. FrescoFrameCache.LJ() -> return null
        try {
            Fingerprint(
                definingClass = "Lcom/facebook/fresco/animation/bitmap/cache/FrescoFrameCache;",
                name = "LJ",
                returnType = "Lcom/facebook/common/references/CloseableReference;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
            println("[RuntimeMemoryGovernor] Capped FrescoFrameCache.LJ() -> Frame cache allocation suppressed.")
            patched++
        } catch (e: Exception) {
            println("[RuntimeMemoryGovernor] FrescoFrameCache.LJ note: ${e.message}")
        }

        println("[RuntimeMemoryGovernor] Applied $patched Fresco memory tuning hooks.")
    }
}
