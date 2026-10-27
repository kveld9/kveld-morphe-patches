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
            // TikTok Lite share path: HomePageShareServiceImpl (share sheet/copy link) and Cem (direct share)
            val homePageFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/share/arch/HomePageShareServiceImpl;",
                strings = listOf("share_convert_loading_duration", "checksum_invitation_scene"),
                returnType = "Ljava/lang/String;",
            )
            val homePageMethod = homePageFp.method
            val homePageUrlReg = if (AccessFlags.STATIC.isSet(homePageMethod.accessFlags)) "p0" else "p1"
            homePageMethod.addInstructions(
                0,
                """
                    invoke-static/range {$homePageUrlReg .. $homePageUrlReg}, ${Constants.TIKTOK_EXTENSION_SHARE_HOST_HOOK}->sanitizeShareUrlWithHost(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object $homePageUrlReg
                    return-object $homePageUrlReg
                """.trimIndent(),
            )
            println("[Clean Share Links] Hooked Lite HomePageShareServiceImpl.LFFLLL() -> Link sanitization with host replacement active.")
            patched++

            val cemFp = Fingerprint(
                definingClass = "LX/Cem;",
                strings = listOf("share_convert_loading_duration", "checksum_invitation_scene"),
                returnType = "Ljava/lang/String;",
            )
            val cemMethod = cemFp.method
            val cemUrlReg = if (AccessFlags.STATIC.isSet(cemMethod.accessFlags)) "p0" else "p1"
            cemMethod.addInstructions(
                0,
                """
                    invoke-static/range {$cemUrlReg .. $cemUrlReg}, ${Constants.TIKTOK_EXTENSION_SHARE_HOST_HOOK}->sanitizeShareUrlWithHost(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object $cemUrlReg
                    return-object $cemUrlReg
                """.trimIndent(),
            )
            println("[Clean Share Links] Hooked Lite Cem.LBL() -> Link sanitization with host replacement active.")
            patched++
        } else {
            // TikTok main (musically): Hook Aweme.getShareUrl() return point
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
