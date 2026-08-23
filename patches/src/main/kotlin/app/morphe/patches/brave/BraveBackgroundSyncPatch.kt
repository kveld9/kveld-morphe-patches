package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val braveBackgroundSyncPatch = bytecodePatch(
    name = "Disable Background Sync & Periodic Sync",
    description = "Eliminates background wakeups, radio modem activity, and battery drain by forcing GooglePlayServicesChecker.shouldDisableBackgroundSync() -> true and neutralizing wakeup tasks.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        // 1. Force GooglePlayServicesChecker.shouldDisableBackgroundSync() -> true (1)
        // Causes Chromium core to permanently disable Background Sync across all tabs.
        Fingerprint(
            returnType = "Z",
            parameters = emptyList(),
            strings = listOf("BackgroundSync.LaunchTask.PlayServicesAvailable"),
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )

        // 2. Neutralize PeriodicBackgroundSync wakeup task runner
        Fingerprint(
            returnType = "V",
            parameters = listOf("Landroid/content/Context;", "Lfjj;", "Lu5c;"),
            strings = listOf("BackgroundSync.Periodic.Wakeup.DelayTime"),
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        // 3. Neutralize OneShot BackgroundSync wakeup task runner
        Fingerprint(
            returnType = "V",
            parameters = listOf("Landroid/content/Context;", "Lfjj;", "Lu5c;"),
            strings = listOf("BackgroundSync.Wakeup.DelayTime"),
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
