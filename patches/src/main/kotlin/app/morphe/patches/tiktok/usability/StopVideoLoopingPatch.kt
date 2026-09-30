package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val stopVideoLoopingPatch = bytecodePatch(
    name = "Stop Video Looping",
    description = "Stops videos at the end instead of replaying them in an infinite loop.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0
        val fp = Fingerprint(
            definingClass = "Lcom/ss/ttvideoengine/TTVideoEngine;",
            name = "setLooping",
            returnType = "V",
            parameters = listOf("Z"),
        )
        val method = fp.method
        method.addInstructions(
            0,
            """
                const/4 p1, 0x0
            """.trimIndent(),
        )
        println("[Stop Video Looping] Hooked TTVideoEngine.setLooping(Z)V -> Forced isLooping=false.")
        patched++
        println("[Stop Video Looping] Applied $patched video loop suppression hook(s).")
    }
}
