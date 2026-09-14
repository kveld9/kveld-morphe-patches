package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val cleanShareUrlPatch = bytecodePatch(
    name = "Clean Share URL",
    description = "Strips tracking parameters, user IDs, device fingerprints, and marketing tokens from shared TikTok links.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Hook Aweme.getShareUrl() return point
        try {
            val fp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getShareUrl",
                returnType = "Ljava/lang/String;",
            )
            val method = fp.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->sanitizeShareUrl(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Clean Share URL] Hooked Aweme.getShareUrl() (${returnIndices.size} return point(s)) -> Link sanitization active.")
                patched++
            }
        } catch (e: Exception) {
            println("[Clean Share URL] Aweme.getShareUrl note: ${e.message}")
        }

        println("[Clean Share URL] Applied $patched link tracking sanitizer hook(s).")
    }
}
