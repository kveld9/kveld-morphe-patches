package app.morphe.patches.vivaldi

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val vivaldiBackgroundSyncPatch = bytecodePatch(
    name = "Disable Background Sync & Periodic Sync",
    description = "Eliminates background wakeups, radio modem activity, and battery drain by neutralizing periodic and one-shot background sync tasks.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Neutralize PeriodicBackgroundSync wakeup task runner
        val fp1 = Fingerprint(
            returnType = "V",
            strings = listOf("BackgroundSync.Periodic.Wakeup.DelayTime"),
        )
        fp1.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        val c1 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp1.originalClassDef.type)
        hookedMethods.add("$c1.PeriodicSync")

        // 2. Neutralize OneShot BackgroundSync wakeup task runner
        val fp2 = Fingerprint(
            returnType = "V",
            strings = listOf("BackgroundSync.Wakeup.DelayTime"),
        )
        fp2.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        val c2 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp2.originalClassDef.type)
        hookedMethods.add("$c2.OneShotSync")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Disable Background Sync] Neutralized ${hookedMethods.size} background sync runners in ${targetClasses.joinToString(", ")}")
    }
}
