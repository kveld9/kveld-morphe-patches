package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.cleanClassName

val gboardDisablePhenotypeSyncPatch = bytecodePatch(
    name = "Stop Remote Updates",
    description = "Stops Google from changing settings remotely.",
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
            definingClass = "Lwzh;",
            name = "dB",
            parameters = listOf("Landroid/content/Context;", "Lwjf;"),
            returnType = "V",
        )
        fp3.method.addInstructions(0, "return-void")
        val c3 = cleanClassName(fp3.originalClassDef.type)
        hookedMethods.add("$c3.dB")

        val fp4 = Fingerprint(
            definingClass = "Lwzh;",
            name = "e",
            parameters = emptyList(),
            returnType = "V",
        )
        fp4.method.addInstructions(0, "return-void")
        hookedMethods.add("$c3.e")

        val fp5 = Fingerprint(
            definingClass = "Lwzh;",
            name = "g",
            parameters = emptyList(),
            returnType = "V",
        )
        fp5.method.addInstructions(0, "return-void")
        hookedMethods.add("$c3.g")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        if (hookedMethods.isEmpty()) {
            println("[Stop Remote Updates] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Stop Remote Updates] Neutralized ${hookedMethods.size} Phenotype sync methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
        }
    }
}
