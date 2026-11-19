package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val cleanShareUrlPatch = bytecodePatch(
    name = "Clean Share Links",
    description = "Removes tracking parameters from shared links.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_LITE)
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
        val isLite = packageMetadata.packageName == Constants.TIKTOK_LITE_PACKAGE_NAME

        // 1. Initialize custom share host if specified
        val host = shareHost?.trim() ?: ""
        if (host.isNotEmpty()) {
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
            println("[Clean Share Links] Configured custom share host: $host")
        }

        if (isLite) {
            // The Lite share panel and direct-share sheets build their links in two generators.
            // Aweme.getShareUrl() is never called (the panel reads the shareUrl field directly),
            // so anchoring on the generators is what actually sanitizes copied and shared links.
            val generatorMethods = Fingerprint(
                strings = listOf("share_convert_loading_duration", "checksum_invitation_scene"),
                returnType = "Ljava/lang/String;",
            ).matchAll().map { it.method }

            generatorMethods.forEach { method ->
                val urlRegister = if (AccessFlags.STATIC.isSet(method.accessFlags)) "p0" else "p1"
                method.addInstructions(
                    0,
                    """
                        invoke-static/range {$urlRegister .. $urlRegister}, ${Constants.TIKTOK_EXTENSION_SHARE_HOST_HOOK}->sanitizeShareUrlWithHost(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object $urlRegister
                        return-object $urlRegister
                    """.trimIndent(),
                )
                println("[Clean Share Links] Hooked ${method.definingClass}.${method.name}() -> Link sanitization with host replacement active.")
                patched++
            }

            val promoTail = Fingerprint(
                strings = listOf("https://www.tiktok.com/tiktoklite"),
                parameters = emptyList(),
                returnType = "Ljava/lang/String;",
            ).method
            promoTail.addInstructions(
                0,
                """
                    const-string v0, ""
                    return-object v0
                """.trimIndent(),
            )
            println("[Clean Share Links] Neutralized the Lite share promo tail -> Only the clean video link is shared.")
            patched++
        } else {
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
                println("[Clean Share Links] Hooked Aweme.getShareUrl() (${returnIndices.size} return point(s)) -> Link sanitization with host replacement active.")
                patched++
            }
        }

        if (patched == 0) {
            println("[Clean Share Links] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Clean Share Links] Applied $patched link tracking sanitizer hook(s).")
        }
    }
}
