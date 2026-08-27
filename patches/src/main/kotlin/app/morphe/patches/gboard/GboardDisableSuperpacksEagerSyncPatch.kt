package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardDisableSuperpacksEagerSyncPatch = bytecodePatch(
    name = "Disable Superpacks Eager Sync",
    description = "Neutralizes eager background Superpacks synchronization during application startup, eliminating unnecessary disk sweeps and network polling while preserving on-demand pack downloads.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val hookedMethods = mutableListOf<String>()

        val fp1 = Fingerprint(
            definingClass = "Lgvk;",
            name = "n",
            parameters = emptyList(),
            returnType = "V",
        )
        fp1.method.addInstructions(0, "return-void")
        val c1 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp1.originalClassDef.type)
        hookedMethods.add("$c1.n")

        val fp2 = Fingerprint(
            definingClass = "Lgrp;",
            name = "n",
            parameters = emptyList(),
            returnType = "V",
        )
        fp2.method.addInstructions(0, "return-void")
        val c2 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp2.originalClassDef.type)
        hookedMethods.add("$c2.n")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Disable Superpacks Eager Sync] Neutralized ${hookedMethods.size} Superpacks sync methods in ${targetClasses.joinToString(", ")}")
    }
}
