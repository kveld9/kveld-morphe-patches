package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val hideSearchPopularLivesPatch = bytecodePatch(
    name = "Hide Popular Lives In Search",
    description = "Removes the Popular LIVEs recommendation card and live stream broadcasts from the search discovery page.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Filter live_popular card from search intermediate data payload
        try {
            val recomWrapperFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/discover/model/suggest/RecomDataWrapper;",
                name = "<init>",
                returnType = "V",
                parameters = listOf(
                    "Ljava/lang/String;",
                    "Lcom/ss/android/ugc/aweme/discover/model/suggest/SuggestWordResponse;",
                ),
            )
            recomWrapperFp.method.addInstructions(
                1,
                """
                    invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterPopularLivesRaw(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p1
                    invoke-static {p2}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterPopularLivesResponse(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            patched++
        } catch (e: Exception) {
            println("[Hide Popular Lives In Search] RecomDataWrapper.<init> note: ${e.message}")
        }

        // 2. Intercept Lynx AB parameters to disable has_transfer_tab_live and clear transfer_tab_live_url
        try {
            val sparkHostFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/spark/SparkHostApiImpl;",
                name = "LJLJI",
                returnType = "Ljava/util/Map;",
                parameters = listOf("Ljava/lang/String;"),
            )
            val method = sparkHostFp.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterPopularLivesAbParams(Ljava/util/Map;)Ljava/util/Map;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            patched++
        } catch (e: Exception) {
            println("[Hide Popular Lives In Search] SparkHostApiImpl.LJLJI note: ${e.message}")
        }

        // 3. Sanitize Lynx schema URL to strip intermediate_show_trending_billboard
        try {
            val schemaFp = Fingerprint(
                definingClass = "LX/0HLB;",
                name = "LIZ",
                returnType = "Ljava/lang/String;",
            )
            val schemaMethod = schemaFp.method
            val schemaReturnIndices = schemaMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            schemaReturnIndices.asReversed().forEach { (returnIndex, reg) ->
                schemaMethod.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterPopularLivesSchema(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            patched++
        } catch (e: Exception) {
            println("[Hide Popular Lives In Search] SearchIntermediateSchema.LIZ note: ${e.message}")
        }

        println("[Hide Popular Lives In Search] Applied $patched popular lives suppression hook(s) -> 'LIVE populares' neutralized.")
    }
}
