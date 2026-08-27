package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val braveBatteryOptimizationPatch = bytecodePatch(
    name = "Disable Battery Status API & OS Listener",
    description = "Neutralizes the Android BatteryStatusManager broadcast listener to prevent continuous OS battery wakeups.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE, Constants.COMPATIBILITY_VIVALDI)

    execute {
        val fp = Fingerprint(
            returnType = "V",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            strings = listOf("android.intent.action.BATTERY_CHANGED", "cr_BatteryStatusManager"),
        )
        fp.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        val targetClass = fp.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        println("[Disable Battery API] Hooked onReceive in $targetClass to drop BATTERY_CHANGED broadcast events")
    }
}
