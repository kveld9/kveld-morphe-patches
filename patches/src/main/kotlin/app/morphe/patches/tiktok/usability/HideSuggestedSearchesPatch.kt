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

        // 2. Suppress cached preloaded guess search data (JSONObject)
        Fingerprint(
            definingClass = "LX/0HBy;",
            name = "LIZ",
            returnType = "Lorg/json/JSONObject;",
        ).method.replaceWithReturnNull()
        patched++

        // 3. Suppress cached preloaded guess search data (String)
        Fingerprint(
            definingClass = "LX/0HBy;",
            name = "LIZIZ",
            returnType = "Ljava/lang/String;",
        ).method.replaceWithReturnNull()
        patched++

        // 4. Force disable native guess search fallback flag in search middle page
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/search/middle/DynamicSingleIntermediateFragmentNew;",
            name = "pZ",
            returnType = "Z",
        ).method.replaceWithReturnBoolean(false)
        patched++

        // 5. Force disable show_suggest_search_words in AB evaluator method
        Fingerprint(
            definingClass = "LX/0HAT;",
            name = "LIZ",
            returnType = "I",
            parameters = listOf(
                "Lkotlin/jvm/functions/Function0;",
                "Lkotlin/jvm/functions/Function0;",
            ),
        ).method.replaceWithReturnInt(0)
        patched++

        // 6. Reset static field in <clinit> to prevent observer initialization
        val hatClinitFp = Fingerprint(
            definingClass = "LX/0HAT;",
            name = "<clinit>",
            returnType = "V",
        )
        val hatMethod = hatClinitFp.method
        val hatReturnIndices = hatMethod.implementation?.instructions?.withIndex()
            ?.filter { it.value.opcode == Opcode.RETURN_VOID }
            ?.map { it.index }
            ?.toList() ?: emptyList()

        hatReturnIndices.asReversed().forEach { returnIndex ->
            hatMethod.addInstructions(
                returnIndex,
                """
                    const/4 v0, 0x0
                    sput-boolean v0, LX/0HAT;->LIZ:Z
                """.trimIndent(),
            )
        }
        patched++

        // 7. Filter show_suggest_search_words and related flags in Lynx abParams
        val sparkHostFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/spark/SparkHostApiImpl;",
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

        // 8. Sanitize Lynx schema URL to strip show_suggest_search_words
        val schemaFp = Fingerprint(
            definingClass = "LX/0HAY;",
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

        println("[Hide Suggested Searches] Applied $patched suggested search suppression hook(s) -> 'Podría interesarte' neutralized.")
    }
}
