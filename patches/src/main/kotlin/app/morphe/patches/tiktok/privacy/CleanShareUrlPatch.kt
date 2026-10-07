package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val cleanShareUrlPatch = bytecodePatch(
    name = "Clean Share URL",
    description = "Strips tracking parameters, user IDs, device fingerprints, and marketing tokens from shared TikTok links, with optional custom host redirection.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val shareHost by stringOption(
        key = "shareHost",
        title = "Custom Share Host",
        description = "Custom hostname to replace in shared links (e.g. 'vxtiktok.com', 'tiktxk.com', or empty to keep original host).",
        default = "",
        required = false,
    )

    execute {
        var patched = 0

        // 1. Initialize custom share host if specified
        val host = shareHost?.trim() ?: ""
        if (host.isNotEmpty()) {
            try {
                val clinit = Fingerprint(
                    definingClass = Constants.TIKTOK_EXTENSION_SHARE_HOST_HOOK,
                    name = "<clinit>",
                ).method
                val instructions = clinit.implementation!!.instructions
                val returnIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
                val insertIdx = if (returnIdx != -1) returnIdx else 0
                clinit.addInstructions(
                    insertIdx,
                    """
                        const-string v0, "$host"
                        sput-object v0, ${Constants.TIKTOK_EXTENSION_SHARE_HOST_HOOK}->customHost:Ljava/lang/String;
                    """.trimIndent(),
                )
                println("[Clean Share URL] Configured custom share host: $host")
            } catch (e: Exception) {
                println("[Clean Share URL] TikTokShareHostHook clinit note: ${e.message}")
            }
        }

        // 2. Hook Aweme.getShareUrl() return point
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
                    invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_SHARE_HOST_HOOK}->sanitizeShareUrlWithHost(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$reg
                """.trimIndent(),
            )
        }
        if (returnIndices.isNotEmpty()) {
            println("[Clean Share URL] Hooked Aweme.getShareUrl() (${returnIndices.size} return point(s)) -> Link sanitization with host replacement active.")
            patched++
        }

        println("[Clean Share URL] Applied $patched link tracking sanitizer hook(s).")
    }
}
