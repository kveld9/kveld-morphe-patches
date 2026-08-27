package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val braveSkipFirstRunPatch = bytecodePatch(
    name = "Skip First Run",
    description = "Skips the welcome screen, search engine selection, and onboarding First Run Experience (FRE) on clean installs.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE, Constants.COMPATIBILITY_VIVALDI)

    execute {
        // 1. Force FirstRunStatus.getFirstRunFlowComplete to return true so all app components (sync, promo, telemetry) know FRE is done
        val fp1 = Fingerprint(
            returnType = "Z",
            parameters = listOf(),
            strings = listOf("first_run_flow"),
        )
        fp1.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )

        // 2. Force FirstRunFlowSequencer.checkIfFirstRunIsNecessary to return false to prevent launching FirstRunActivity
        val fp2 = Fingerprint(
            returnType = "Z",
            parameters = listOf("Z", "Z"),
            strings = listOf("disable-fre", "Chrome.FirstRun.SkippedByPolicy"),
        )
        fp2.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )

        val class1 = fp1.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        val class2 = fp2.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        println("[Skip First Run] Forced complete in $class1 & bypassed sequencer in $class2")
    }
}
