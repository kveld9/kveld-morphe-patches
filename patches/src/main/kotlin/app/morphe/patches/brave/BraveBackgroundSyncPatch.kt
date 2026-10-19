package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val braveBackgroundSyncPatch = bytecodePatch(
    name = "Stop Background Sync",
    description = "Stops background wakeups and battery drain.",
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

        if (hookedMethods.isEmpty()) {
            println("[Stop Background Sync] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Stop Background Sync] Injected Smali hooks into ${hookedMethods.size} methods (${hookedMethods.joinToString(", ")})")
        }
    }
}
