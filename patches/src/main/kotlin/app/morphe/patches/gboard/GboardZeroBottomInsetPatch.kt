package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import app.morphe.patches.shared.ensureRegisterCount

val gboardZeroBottomInsetPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(sharedExtensionPatch)

    dependsOn(gboardCoreIntegrityPatch)

    execute {
        var patchedCount = 0

        // 1. KeyboardModeUtils.getKeyboardBottomOffset(Context, int, int, boolean) -> check runtime preference
        val fpKeyboardModeUtils = Fingerprint(
            strings = listOf("KeyboardModeUtils.java", "getKeyboardBottomOffset"),
            returnType = "I",
            parameters = listOf("Landroid/content/Context;", "I", "I", "Z"),
        )
        val methodKeyboardMode = fpKeyboardModeUtils.method
        methodKeyboardMode.ensureRegisterCount(5)
        methodKeyboardMode.addInstructions(
            0,
            """
                invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->getBottomPadding()I
                move-result v0
                if-ltz v0, :cond_skip_morphe_zero_inset_km
                return v0
                :cond_skip_morphe_zero_inset_km
            """.trimIndent(),
        )
        patchedCount++

        // 2. WindowMetricsNotification.getNavigationBarBottomInset() -> check runtime preference
        val fpWindowMetricsClass = Fingerprint(
            strings = listOf("WindowMetricsNotification.java", "No window/display metrics has been notified."),
        )
        val methodWindowMetrics = fpWindowMetricsClass.classDef.methods.first { method ->
            method.returnType == "I" && method.parameters.isEmpty() && (method.accessFlags and 0x8) != 0
        }
        methodWindowMetrics.ensureRegisterCount(1)
        methodWindowMetrics.addInstructions(
            0,
            """
                invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->getBottomPadding()I
                move-result v0
                if-ltz v0, :cond_skip_morphe_zero_inset_wm
                return v0
                :cond_skip_morphe_zero_inset_wm
            """.trimIndent(),
        )
        patchedCount++

        println("[Zero Bottom Inset] Hooked dynamic bottom offset preference across $patchedCount methods (KeyboardModeUtils, WindowMetricsNotification).")
    }
}
