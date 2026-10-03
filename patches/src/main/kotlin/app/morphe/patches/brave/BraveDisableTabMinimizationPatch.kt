package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val braveDisableTabMinimizationPatch = bytecodePatch(
    name = "Disable Tab Auto-Minimization",
    description = "Prevents Brave from minimizing active tabs to the background and forcing a New Tab Page when returning to the browser after inactivity.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        // Target: ChromeTabbedActivity.k6 ()Z (or obfuscated name across versions)
        // Evaluates whether the opening screen should override tab restoration:
        // - Checks "brave_new_tab_page_opening_screen" (defaults to 1 = after inactivity)
        // - Checks background elapsed time against 12h threshold
        // - Checks "brave_foreground_session_ends_triggered"
        // - If true, triggers NTP opening, backgrounds the active tab, and sets "brave_show_recent_tabs_snackbar".
        // Injecting 'const/4 v0, 0x0; return v0' at index 0 guarantees that Brave always restores
        // and preserves the active tab right where the user left it, completely bypassing the opening screen override.
        val openingScreenFp = Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/ChromeTabbedActivity;",
            returnType = "Z",
            parameters = emptyList(),
            strings = listOf(
                "brave_new_tab_page_opening_screen",
                "brave_show_recent_tabs_snackbar",
            ),
        )

        openingScreenFp.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )

        println("[Disable Tab Auto-Minimization] Applied 1 hook in ChromeTabbedActivity.${openingScreenFp.method.name} -> opening screen tab backgrounding disabled.")
    }
}
