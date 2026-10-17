package app.morphe.patches.chromium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val cleanShareUrlPatch = bytecodePatch(
    name = "Clean Shared Links",
    description = "Removes tracking codes from links you share or copy.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0
        val hookedTargets = mutableListOf<String>()

        // 0. Hook Share Intent builder so all Android share sheets receive cleaned URLs
        try {
            val fpShareIntent = Fingerprint(
                returnType = "Landroid/content/Intent;",
                strings = listOf("android.intent.extra.TEXT", "org.chromium.chrome.browser.share_origin"),
            )
            val method = fpShareIntent.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.CHROMIUM_EXTENSION_CLASS}->cleanShareIntent(Landroid/content/Intent;)Landroid/content/Intent;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                val className = fpShareIntent.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
                hookedTargets.add("$className.${method.name}")
                patched++
            }
        } catch (e: Exception) {
            println("[Clean Shared Links] Share Intent builder hook note: ${e.message}")
        }

        // 1. Hook ClipboardImpl.setPrimaryClip so all copied links, URLs, and text are sanitized
        try {
            val fpClipboard = Fingerprint(
                name = "setPrimaryClip",
                parameters = listOf("Landroid/content/ClipData;"),
                returnType = "V",
                strings = listOf("google"),
                custom = { _, classDef ->
                    classDef.type.contains("Clipboard")
                },
            )
            fpClipboard.method.apply {
                addInstructions(
                    0,
                    """
                        invoke-static {p1}, ${Constants.CHROMIUM_EXTENSION_CLASS}->cleanClipData(Landroid/content/ClipData;)Landroid/content/ClipData;
                        move-result-object p1
                    """.trimIndent(),
                )
                val className = fpClipboard.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
                hookedTargets.add("$className.$name")
                patched++
            }
        } catch (e: Exception) {
            println("[Clean Shared Links] ClipboardImpl.setPrimaryClip hook note: ${e.message}")
        }

        // 2. Fallback hook for Clipboard.setText
        try {
            Fingerprint(
                definingClass = "Lorg/chromium/ui/base/Clipboard;",
                name = "setText",
                parameters = listOf("Ljava/lang/String;"),
            ).method.apply {
                addInstructions(
                    0,
                    """
                        invoke-static {p1}, ${Constants.CHROMIUM_EXTENSION_CLASS}->cleanShareUrl(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p1
                    """.trimIndent(),
                )
                hookedTargets.add("Clipboard.setText")
                patched++
            }
        } catch (e: Exception) {
            println("[Clean Shared Links] Clipboard.setText hook note: ${e.message}")
        }

println("[Clean Shared Links] Applied $patched link tracking sanitizer hook(s) across target components (${hookedTargets.joinToString(", ")}).")
    }
}
