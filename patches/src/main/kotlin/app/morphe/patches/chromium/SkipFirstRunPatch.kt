package app.morphe.patches.chromium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val skipFirstRunPatch = bytecodePatch(
    name = "Skip Welcome Screens",
    description = "Skips welcome, search choice and onboarding on fresh installs.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

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

        val class1 = fp1.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        println("[Skip Welcome Screens] Forced complete in $class1")
    }
}
