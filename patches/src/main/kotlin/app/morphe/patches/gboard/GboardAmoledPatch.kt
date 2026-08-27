package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants
import java.util.Base64

private val COLOR_BLACK_BYTES by lazy {
    Base64.getDecoder().decode(
        "EjUKK2RlZmF1bHRfa2V5Ym9hcmRfYmFja2dyb3VuZF9zZWNvbmRhcnlfY29sb3ISBgiAgID4DxIzCilkZWZhdWx0X2tleWJvYXJkX2JhY2tncm91bmRfcHJpbWFyeV9jb2xvchIGCICAgPgPEi4KJGRlZmF1bHRfZ2VuZXJpY19hY2NlbnRfY29sb3JfcHJlc3NlZBIGCLTkyPkPEiYKHGRlZmF1bHRfZ2VuZXJpY19hY2NlbnRfY29sb3ISBgieuPD4DxIsCiJkZWZhdWx0X2JvcmRlcmVkX2tleV9jb2xvcl9wcmVzc2VkEgYIqNCg+Q8SJAoaZGVmYXVsdF9ib3JkZXJlZF9rZXlfY29sb3ISBgiUqND4DxIxCidkZWZhdWx0X2JvcmRlcmVkX2tleV9kYXJrX2NvbG9yX3ByZXNzZWQSBgiixIj5DxIpCh9kZWZhdWx0X2JvcmRlcmVkX2tleV9kYXJrX2NvbG9yEgYIjpy4+A8SNgosY29sb3JfZ2VuZXJpY19leHRlbnNpb25fYmFja2dyb3VuZF9hY3RpdmF0ZWQSBgimzJj5DxItCiNkZWZhdWx0X2FjdGlvbl9rZXlfYmFja2dyb3VuZF9jb2xvchIGCJ648PgPEjUKK2RlZmF1bHRfYWN0aW9uX2tleV9iYWNrZ3JvdW5kX2NvbG9yX3ByZXNzZWQSBgi05Mj5Dw=="
    )
}

private val COLOR_COMMON_BYTES by lazy {
    Base64.getDecoder().decode(
        "CjsKIi50b29sdGlwLm91dGxpbmVkX2J1dHRvbi5jbGlwYm9hcmQQAiITZGVmYXVsdF9sYWJlbF9jb2xvcgo7CiIudG9vbHRpcC5vdXRsaW5lZF9idXR0b24uY2xpcGJvYXJkEB4iE2RlZmF1bHRfbGFiZWxfY29sb3IKdwobLmtleWJvYXJkLWNsaXBib2FyZC10b29sdGlwCiEua2V5Ym9hcmQtY2xpcGJvYXJkLXRvb2x0aXAucGFuZWwQASIsY29sb3JfZ2VuZXJpY19leHRlbnNpb25fYmFja2dyb3VuZF9hY3RpdmF0ZWQyBQ0AAIA/CjkKFy5zb2Z0a2V5LWhpZ2hsaWdodC1maWxsEAEiHGNvbG9yX3NvZnRrZXlfaGlnaGxpZ2h0X2ZpbGwKOQoXLnNvZnRrZXktb3V0bGluZS1zdHJva2UQAiIcY29sb3Jfc29mdGtleV9vdXRsaW5lX3N0cm9rZRIlChhkZWZhdWx0X2tleXRvcF9lbGV2YXRpb24SCVEAAAAAAADwPxIvCiVjb2xvcl9pY29uX21vcmVfY2FuZGlkYXRlc19iYWNrZ3JvdW5kEgYI////5wwSNAoqZGVmYXVsdF9ib3JkZXJsZXNzX3NwYWNlX2Jhcl9jb2xvcl9wcmVzc2VkEgYI////9wwSKAoiZGVmYXVsdF9ib3JkZXJsZXNzX3NwYWNlX2Jhcl9jb2xvchICCAASTQoeZGVmYXVsdF9wb3B1cF9iYWNrZ3JvdW5kX2NvbG9yGitkZWZhdWx0X2tleWJvYXJkX2JhY2tncm91bmRfc2Vjb25kYXJ5X2NvbG9yEiIKHGNvbG9yX2tleWJvYXJkX3RvcF9zZXBhcmF0b3ISAggAEh8KGWNvbG9yX2NhbmRpZGF0ZV9zZXBhcmF0b3ISAggAEikKH2NvbG9yX2NhbmRpZGF0ZV9wYW5lbF9zZXBhcmF0b3ISBgj////XARI2Cixjb2xvcl9nZW5lcmljX2V4dGVuc2lvbl9iYWNrZ3JvdW5kX2FjdGl2YXRlZBIGCICAgOgEEiwKImNvbG9yX2dlbmVyaWNfZXh0ZW5zaW9uX2JhY2tncm91bmQSBgiZs+b8DxIuCiRjb2xvcl9rZXlib2FyZF9lZGl0aW5nX2VkZ2VfZGlzYWJsZWQSBgi3iO3+DxI4ChFjb2xvcl9ub3RpY2VfdGV4dBojZGVmYXVsdF9nZW5lcmljX2FjY2VudF9jb2xvcl9zdHJvbmcSNQoWY29sb3Jfbm90aWNlX2xpbmtfdGV4dBobZGVmYXVsdF9sYWJlbF9jb2xvcl92YXJpYW50EkcKKWRyYWdfaGFuZGxlcl9hY2Nlc3NfcG9pbnRfaXRlbV9iYWNrZ3JvdW5kGhpkZWZhdWx0X2JvcmRlcmVkX2tleV9jb2xvchJPCixhY2Nlc3NfcG9pbnRfY3VzdG9taXplZF9zdGF0ZV9pbmRpY2F0b3JfZmlsbBofZGVmYXVsdF9ib3JkZXJlZF9rZXlfZGFya19jb2xvchJVCihhY2Nlc3MtcG9pbnQtZWR1Y2F0aW9uLWZvb3Rlci1iYWNrZ3JvdW5kGilkZWZhdWx0X2tleWJvYXJkX2JhY2tncm91bmRfcHJpbWFyeV9jb2xvchJFChxjb2xvcl9zb2Z0a2V5X2hpZ2hsaWdodF9maWxsGiVkZWZhdWx0X3RyYW5zcGFyZW50X2tleV9jb2xvcl9wcmVzc2VkEkgKHGNvbG9yX3NvZnRrZXlfb3V0bGluZV9zdHJva2UaKGRlZmF1bHRfYm9yZGVyZWRfa2V5X2RhcmtfY29sb3JfY29udHJhc3QSHgoYY29sb3Jfa2V5Ym9hcmRfc2VwYXJhdG9yEgIIAA=="
    )
}

private val gboardAmoledResourcePatch = rawResourcePatch(
    name = "Gboard AMOLED Theme Palette",
    description = "Rewrites style_sheet_color_black.binarypb and style_sheet_color_common.binarypb with Pure Black AMOLED theme palette.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        var replacedFiles = 0
        val blackSheet = get("assets/theme/style_sheet_color_black.binarypb")
        if (blackSheet.exists()) {
            blackSheet.writeBytes(COLOR_BLACK_BYTES)
            replacedFiles++
        }
        val commonSheet = get("assets/theme/style_sheet_color_common.binarypb")
        if (commonSheet.exists()) {
            commonSheet.writeBytes(COLOR_COMMON_BYTES)
            replacedFiles++
        }

        val totalBytes = COLOR_BLACK_BYTES.size + COLOR_COMMON_BYTES.size
        println("[AMOLED Theme] Replaced $replacedFiles AMOLED stylesheet assets ($totalBytes bytes binarypb)")
    }
}

val gboardAmoledPatch = bytecodePatch(
    name = "Add AMOLED Theme",
    description = "Adds a selectable Pure Black AMOLED theme using Gboard's native color_black theme package, without replacing Dark, Light, System Auto, or Dynamic Color.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(gboardAmoledResourcePatch)

    execute {
        val themeListingFragmentFingerprint = Fingerprint(
            definingClass = "Lcom/google/android/apps/inputmethod/libs/theme/listing/ThemeListingFragment;",
            name = "f",
            parameters = listOf("Landroid/os/Bundle;"),
            returnType = "V",
            filters = listOf(
                methodCall(
                    definingClass = "Lxgy;",
                    name = "e",
                    returnType = "Z",
                ),
            ),
        )

        val matchIndex = themeListingFragmentFingerprint.instructionMatches.first().index
        themeListingFragmentFingerprint.method.addInstructions(
            matchIndex,
            """
                const-string v6, "assets:theme_package_metadata_color_black.binarypb"
                const/4 v7, 0x1
                new-instance v8, Lxhh;
                invoke-direct {v8, v6, v7}, Lxhh;-><init>(Ljava/lang/String;Z)V
                invoke-static {p1, v8}, Lmxd;->e(Landroid/content/Context;Lxhh;)Lmxd;
                move-result-object v6
                new-instance v7, Lmwc;
                const-string v8, "AMOLED"
                invoke-direct {v7, v8, v6, v8}, Lmwc;-><init>(Ljava/lang/String;Lmxd;Ljava/lang/String;)V
                invoke-interface {v5, v7}, Ljava/util/List;->add(Ljava/lang/Object;)Z
            """.trimIndent(),
        )

        val targetClass = themeListingFragmentFingerprint.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        println("[AMOLED Theme] Injected AMOLED theme entry into $targetClass.f() at opcode index $matchIndex")
    }
}
