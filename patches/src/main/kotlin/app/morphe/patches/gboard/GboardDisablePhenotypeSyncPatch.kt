package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardDisablePhenotypeSyncPatch = bytecodePatch(
    name = "Disable Remote Configuration",
    description = "Disables periodic remote experiment flag synchronization and background updates.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val hookedMethods = mutableListOf<String>()

        val fp1 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/phenotype/client/stable/PhenotypeUpdateBackgroundBroadcastReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fp1.method.addInstructions(0, "return-void")
        hookedMethods.add("PhenotypeUpdateBackgroundBroadcastReceiver.onReceive")

        val fp2 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/phenotype/client/stable/AccountRemovedBroadcastReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fp2.method.addInstructions(0, "return-void")
        hookedMethods.add("AccountRemovedBroadcastReceiver.onReceive")

        val fp3 = Fingerprint(
            definingClass = "Lwjp;",
            name = "dC",
            parameters = listOf("Landroid/content/Context;", "Lvul;"),
            returnType = "V",
        )
        fp3.method.addInstructions(0, "return-void")
        val c3 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp3.originalClassDef.type)
        hookedMethods.add("$c3.dC")

        val fp4 = Fingerprint(
            definingClass = "Lwjp;",
            name = "e",
            parameters = emptyList(),
            returnType = "V",
        )
        fp4.method.addInstructions(0, "return-void")
        hookedMethods.add("$c3.e")

        val fp5 = Fingerprint(
            definingClass = "Lwjp;",
            name = "g",
            parameters = emptyList(),
            returnType = "V",
        )
        fp5.method.addInstructions(0, "return-void")
        hookedMethods.add("$c3.g")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Disable Remote Config] Neutralized ${hookedMethods.size} Phenotype sync methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}
