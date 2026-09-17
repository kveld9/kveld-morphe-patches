package app.morphe.patches.chromium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val cleanShareUrlPatch = bytecodePatch(
    name = "Clean Share URL",
    description = "Strips tracking parameters (utm_*, fbclid, gclid, igshid, si, msclkid) when sharing or copying links.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE, Constants.COMPATIBILITY_VIVALDI)
    extendWith("extensions/extension.mpe")

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
                method.addInstructions(
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
            println("[Clean Share URL] Share Intent builder hook note: ${e.message}")
        }

        // 1. Hook Clipboard.setText so copied links (address bar, context menu, share dialog) are cleaned
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
            println("[Clean Share URL] Clipboard.setText hook note: ${e.message}")
        }

        println("[Clean Share URL] Applied $patched link tracking sanitizer hook(s) across target components (${hookedTargets.joinToString(", ")}).")
    }
}
