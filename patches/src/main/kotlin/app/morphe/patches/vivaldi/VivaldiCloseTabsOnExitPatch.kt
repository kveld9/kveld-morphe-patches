package app.morphe.patches.vivaldi

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val vivaldiCloseTabsOnExitPatch = bytecodePatch(
    name = "Close Tabs on Exit",
    description = "Prevents tab restoration on startup, ensuring Vivaldi always opens with a clean Start Page session regardless of how the app was terminated.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        // 1. Locate TabStateFileManager helper method via unique string constants
        val tabStateHelperMethod = Fingerprint(
            returnType = "Z",
            parameters = listOf("Ljava/lang/String;"),
            strings = listOf("tab_state", ".bak", ".new"),
        ).method

        // 2. Neutralize readTabState (parse stream) method in TabStateFileManager to return 0 restored tabs
        Fingerprint(
            definingClass = tabStateHelperMethod.definingClass,
            returnType = "I",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        println("[Close Tabs on Exit] TabState restoration neutralized (clean startup state)")
    }
}
