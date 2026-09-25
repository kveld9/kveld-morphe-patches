package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnInt
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val hideSuggestedSearchesPatch = bytecodePatch(
    name = "Hide Suggested Searches",
    description = "Removes the suggested search keywords section ('You may like' / 'Search suggestions') from the search discovery page.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Filter guess_search card from search intermediate data payload
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
                    invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterGuessSearchRaw(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p1
                    invoke-static {p2}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterGuessSearchResponse(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] RecomDataWrapper.<init> note: ${e.message}")
        }

        // 2. Suppress cached preloaded guess search data (JSONObject)
        try {
            Fingerprint(
                definingClass = "LX/0HMZ;",
                name = "LIZ",
                returnType = "Lorg/json/JSONObject;",
            ).method.replaceWithReturnNull()
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] GuessSearchPreload.LIZ note: ${e.message}")
        }

        // 3. Suppress cached preloaded guess search data (String)
        try {
            Fingerprint(
                definingClass = "LX/0HMZ;",
                name = "LIZIZ",
                returnType = "Ljava/lang/String;",
            ).method.replaceWithReturnNull()
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] GuessSearchPreload.LIZIZ note: ${e.message}")
        }

        // 4. Force disable native guess search fallback flag in search middle page
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/search/middle/DynamicSingleIntermediateFragmentNew;",
                name = "yU",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] DynamicSingleIntermediateFragmentNew.yU note: ${e.message}")
        }

        // 5. Force disable show_suggest_search_words in AB evaluator method
        try {
            Fingerprint(
                definingClass = "LX/0HL2;",
                name = "LIZ",
                returnType = "I",
                parameters = listOf(
                    "Lkotlin/jvm/functions/Function0;",
                    "Lkotlin/jvm/functions/Function0;",
                ),
            ).method.replaceWithReturnInt(0)
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] ShowSuggestSearchWords.LIZ note: ${e.message}")
        }

        // 6. Reset static field in <clinit> to prevent observer initialization
        try {
            Fingerprint(
                definingClass = "LX/0HL2;",
                name = "<clinit>",
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] ShowSuggestSearchWords.<clinit> note: ${e.message}")
        }

        // 7. Filter show_suggest_search_words and related flags in Lynx abParams
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
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterSuggestedAbParams(Ljava/util/Map;)Ljava/util/Map;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] SparkHostApiImpl.LJLJI note: ${e.message}")
        }

        // 8. Sanitize Lynx schema URL to strip show_suggest_search_words
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
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterSuggestedSchema(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] SearchIntermediateSchema.LIZ note: ${e.message}")
        }

        println("[Hide Suggested Searches] Applied $patched suggested search suppression hook(s) -> 'Podría interesarte' neutralized.")
    }
}
