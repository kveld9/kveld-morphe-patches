package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants

val gboardZeroBottomInsetPatch = bytecodePatch(
    name = "Zero Bottom Inset",
    description = "Eliminates or customizes the navigation bar bottom inset padding (bottom chin/blank space) under the keyboard in gesture navigation mode.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    dependsOn(gboardCoreIntegrityPatch)

    val bottomPadding by stringOption(
        key = "bottomPadding",
        title = "Bottom padding (px)",
        description = "Forced bottom margin padding in pixels (0 for completely flush with screen bottom, range: 0..150. Default: 0).",
        default = "0",
        required = false,
    )

    execute {
        val parsedPadding = bottomPadding?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() }?.coerceIn(0, 150) ?: 0
        val constInsn = if (parsedPadding == 0) "const/4 v0, 0" else "const/16 v0, $parsedPadding"
        val returnInsn = "return v0"
        var patchedCount = 0

        // 1. KeyboardModeUtils.getKeyboardBottomOffset(Context, int, int, boolean) -> force parsedPadding
        val fpKeyboardModeUtils = Fingerprint(
            strings = listOf("KeyboardModeUtils.java", "getKeyboardBottomOffset"),
            returnType = "I",
            parameters = listOf("Landroid/content/Context;", "I", "I", "Z"),
        )
        val methodKeyboardMode = fpKeyboardModeUtils.method
        methodKeyboardMode.addInstructions(0, "$constInsn\n$returnInsn")
        patchedCount++

        // 2. WindowMetricsNotification.getNavigationBarBottomInset() -> force parsedPadding
        val fpWindowMetricsClass = Fingerprint(
            strings = listOf("WindowMetricsNotification.java", "No window/display metrics has been notified."),
        )
        val methodWindowMetrics = fpWindowMetricsClass.classDef.methods.first { method ->
            method.returnType == "I" && method.parameters.isEmpty() && (method.accessFlags and 0x8) != 0
        }
        methodWindowMetrics.addInstructions(0, "$constInsn\n$returnInsn")
        patchedCount++

        println("[Zero Bottom Inset] Overrode bottom offsets ($parsedPadding px) across $patchedCount methods (KeyboardModeUtils, WindowMetricsNotification).")
    }
}
