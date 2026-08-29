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
        val hookedMethods = mutableListOf<String>()

        // 1. Force GooglePlayServicesChecker.shouldDisableBackgroundSync() -> true (1)
        val fp1 = Fingerprint(
            returnType = "Z",
            parameters = emptyList(),
            strings = listOf("BackgroundSync.LaunchTask.PlayServicesAvailable"),
        )
        fp1.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )
        val c1 = fp1.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        hookedMethods.add("$c1.${fp1.method.name}()")

        // 2. Neutralize PeriodicBackgroundSync wakeup task runner
        val fp2 = Fingerprint(
            returnType = "V",
            parameters = listOf("Landroid/content/Context;", "Lwtj;", "Lpcc;"),
            strings = listOf("BackgroundSync.Periodic.Wakeup.DelayTime"),
        )
        fp2.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        val c2 = fp2.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        hookedMethods.add("$c2.${fp2.method.name}()")

        // 3. Neutralize OneShot BackgroundSync wakeup task runner
        val fp3 = Fingerprint(
            returnType = "V",
            parameters = listOf("Landroid/content/Context;", "Lwtj;", "Lpcc;"),
            strings = listOf("BackgroundSync.Wakeup.DelayTime"),
        )
        fp3.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        val c3 = fp3.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        hookedMethods.add("$c3.${fp3.method.name}()")

        println("[Disable Background Sync] Injected Smali hooks into ${hookedMethods.size} methods (${hookedMethods.joinToString(", ")})")
    }
}
