package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val braveDisablePullToRefreshPatch = bytecodePatch(
    name = "Disable Pull To Refresh",
    description = "Completely disables the pull-to-refresh overscroll gesture and animation to prevent accidental page reloads.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        // 1. Force start(II)Z -> false (0) in SwipeRefreshHandler
        val fp1 = Fingerprint(
            returnType = "Z",
            parameters = listOf("I", "I"),
            strings = listOf("brave_pull_to_refresh", "Android.OverscrollFromBottom.CanStart"),
        )
        fp1.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )

        // 2. Neutralize pull(FF)V to prevent any visual overscroll animation or glow
        val fp2 = Fingerprint(
            returnType = "V",
            parameters = listOf("F", "F"),
            strings = listOf("SwipeRefreshHandler.pull"),
        )
        fp2.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        val targetClass = fp1.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        println("[Disable Pull To Refresh] Hooked start() & pull() in $targetClass to eliminate overscroll gesture")
    }
}
