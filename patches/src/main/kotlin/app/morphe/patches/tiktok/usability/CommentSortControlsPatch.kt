package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.getReference
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnIntegerObject
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

val commentSortControlsPatch = bytecodePatch(
    name = "Comment Sort Controls",
    description = "Unlocks TikTok's native comment sorting menu (Hot, Newest, Creator only, With media) across all posts.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. Force option style to 2 (FULL_SORT_SHEET_STYLE)
        val optionStyleFp = Fingerprint(
            returnType = "L",
            strings = listOf("comment_sort_opt_style"),
            custom = { method, _ ->
                method.parameterTypes.size <= 1 && method.parameterTypes.all { it.startsWith("L") }
            },
        )
        val method = optionStyleFp.method
        method.replaceWithReturnIntegerObject(2)
        println("[Comment Sort Controls] Hooked comment_sort_opt_style getter (${method.definingClass}->${method.name}) -> Forced style=2 (Full Sheet).")
        patched++

        // 2. Force Aweme eligibility check to true
        val styleClassType = optionStyleFp.classDef.type
        val eligibilityFp = Fingerprint(
            returnType = "Z",
            parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
            custom = { _, classDef ->
                classDef.methods.any { m ->
                    m.name == "<clinit>" && m.implementation?.instructions?.any { ins ->
                        ins.opcode == Opcode.NEW_INSTANCE &&
                            ins.getReference<TypeReference>()?.type == styleClassType
                    } == true
                }
            },
        )
        val eligibilityMethod = eligibilityFp.method
        eligibilityMethod.replaceWithReturnBoolean(true)
        println("[Comment Sort Controls] Hooked Aweme comment sort eligibility (${eligibilityMethod.definingClass}->${eligibilityMethod.name}) -> Forced eligible=true.")
        patched++

        println("[Comment Sort Controls] Applied $patched comment sort control hook(s).")
    }
}
