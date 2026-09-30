package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

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

        val fpWindowMetrics = Fingerprint(
            strings = listOf("WindowMetricsNotification.java", "notifyWithWindow"),
            returnType = "V",
            parameters = listOf("Landroid/view/View;", "I", "I", "I", "I", "I", "I", "I", "I"),
        )

        val method = fpWindowMetrics.method
        val instructions = method.implementation?.instructions?.toList() ?: emptyList()

        // Find iput stores into Rect.bottom within insets computation (first two occurrences: API 30+ and legacy)
        val iputIndices = instructions.withIndex().filter {
            it.value.opcode == Opcode.IPUT &&
                ((it.value as? ReferenceInstruction)?.reference as? FieldReference)?.let { field ->
                    field.definingClass == "Landroid/graphics/Rect;" && field.name == "bottom" && field.type == "I"
                } == true
        }.map { it.index }

        val targetIndices = iputIndices.take(2)
        require(targetIndices.size == 2) {
            "Expected 2 insets Rect.bottom iput sites in WindowMetricsHelper.onLayoutChange, found ${targetIndices.size}"
        }

        for (idx in targetIndices.asReversed()) {
            val insn = instructions[idx] as TwoRegisterInstruction
            val regA = insn.registerA
            val constInsn = if (parsedPadding == 0) "const/4 v$regA, 0" else "const/16 v$regA, $parsedPadding"
            method.addInstructions(idx, constInsn)
        }

        println("[Zero Bottom Inset] Overrode navigation bar bottom inset ($parsedPadding px) across ${targetIndices.size} opcode site(s) in WindowMetricsHelper.onLayoutChange.")
    }
}
