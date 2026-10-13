package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnInt
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/**
 * Forces every boolean return point of [method] to true.
 */
private fun forceBooleanGateTrue(method: MutableMethod, tag: String): Int {
    val instructions = method.implementation?.instructions ?: return 0
    val returnIndices = instructions.withIndex()
        .filter { it.value.opcode == Opcode.RETURN }
        .map { it.index to (it.value as OneRegisterInstruction).registerA }
        .toList()
    returnIndices.asReversed().forEach { (returnIndex, reg) ->
        method.addInstructionsAtControlFlowLabel(
            returnIndex,
            "const/4 v$reg, 0x1",
        )
    }
    if (returnIndices.isNotEmpty()) {
        println("[$tag] Forced ${returnIndices.size} gate return(s) -> true.")
    }
    return if (returnIndices.isNotEmpty()) 1 else 0
}

val searchExperienceCustomizerPatch = bytecodePatch(
    name = "Search Experience",
    description = "Customizes TikTok search and discovery surfaces via per-feature toggles.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val hideSuggestedSearches by booleanOption(
        key = "hideSuggestedSearches",
        default = true,
        title = "Hide Suggested Searches",
        description = "Removes the suggested search keywords section ('You may like' / 'Search suggestions') from the search discovery page.",
        required = false,
    )

    val hidePopularLives by booleanOption(
        key = "hidePopularLives",
        default = true,
        title = "Hide Popular Lives In Search",
        description = "Removes the Popular LIVEs recommendation card and live stream broadcasts from the search discovery page.",
        required = false,
    )

    val disableSearchAutoplay by booleanOption(
        key = "disableSearchAutoplay",
        default = true,
        title = "Disable Search Video Autoplay",
        description = "Disables automatic video playback in search results. Videos only play when tapped to view in detail.",
        required = false,
    )

    val forceNonPersonalized by booleanOption(
        key = "forceNonPersonalized",
        default = true,
        title = "Non-Personalized Search",
        description = "Forces TikTok's non-personalized search mode instead of the saved account choice.",
        required = false,
    )

    val enableLiveSearch by booleanOption(
        key = "enableLiveSearch",
        default = false,
        title = "Enable Live Search",
        description = "Shows TikTok's search entry in the Live drawer where supported.",
        required = false,
    )

    execute {
        if (hideSuggestedSearches != true &&
            hidePopularLives != true &&
            disableSearchAutoplay != true &&
            forceNonPersonalized != true &&
            enableLiveSearch != true
        ) {
            println("[Search Experience] All toggles disabled -> nothing to patch.")
            return@execute
        }

        var patched = 0

        // 1. Hide Suggested Searches
        if (hideSuggestedSearches == true) {
            // Filter guess_search card from search intermediate data payload
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

            // Suppress cached preloaded guess search data (JSONObject) (LX/0HC2; in v47.1.4, was LX/0HBy;)
            Fingerprint(
                definingClass = "LX/0HC2;",
                name = "LIZ",
                returnType = "Lorg/json/JSONObject;",
            ).method.replaceWithReturnNull()
            patched++

            // Suppress cached preloaded guess search data (String) (LX/0HC2; in v47.1.4, was LX/0HBy;)
            Fingerprint(
                definingClass = "LX/0HC2;",
                name = "LIZIZ",
                returnType = "Ljava/lang/String;",
            ).method.replaceWithReturnNull()
            patched++

            // Force disable native guess search fallback flag in search middle page
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/search/middle/DynamicSingleIntermediateFragmentNew;",
                name = "pZ",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            patched++

            // Force disable show_suggest_search_words in AB evaluator method (LX/0HAX; in v47.1.4, was LX/0HAT;)
            Fingerprint(
                definingClass = "LX/0HAX;",
                name = "LIZ",
                returnType = "I",
                parameters = listOf(
                    "Lkotlin/jvm/functions/Function0;",
                    "Lkotlin/jvm/functions/Function0;",
                ),
            ).method.replaceWithReturnInt(0)
            patched++

            // Reset static field in <clinit> to prevent observer initialization (LX/0HAX; in v47.1.4, was LX/0HAT;)
            val hatClinitFp = Fingerprint(
                definingClass = "LX/0HAX;",
                name = "<clinit>",
                returnType = "V",
            )
            val hatMethod = hatClinitFp.method
            val hatReturnIndices = hatMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_VOID }
                ?.map { it.index }
                ?.toList() ?: emptyList()

            hatReturnIndices.asReversed().forEach { returnIndex ->
                hatMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        const/4 v0, 0x0
                        sput-boolean v0, ${hatMethod.definingClass}->LIZ:Z
                    """.trimIndent(),
                )
            }
            patched++

            // Filter show_suggest_search_words and related flags in Lynx abParams
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
                method.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterSuggestedAbParams(Ljava/util/Map;)Ljava/util/Map;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            patched++

            // Sanitize Lynx schema URL to strip show_suggest_search_words (LX/0HAc; in v47.1.4, was LX/0HAY;)
            val schemaFp = Fingerprint(
                definingClass = "LX/0HAc;",
                name = "LIZ",
                returnType = "Ljava/lang/String;",
            )
            val schemaMethod = schemaFp.method
            val schemaReturnIndices = schemaMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            schemaReturnIndices.asReversed().forEach { (returnIndex, reg) ->
                schemaMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterSuggestedSchema(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            patched++

            if (patched == 0) {
                println("[Search Experience] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
            } else {
                println("[Search Experience] Applied $patched suggested search suppression hook(s) -> 'You may like' neutralized.")
            }
        }

        // 2. Hide Popular Lives In Search
        if (hidePopularLives == true) {
            // Filter live_popular card from search intermediate data payload
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

            // Intercept Lynx AB parameters to disable has_transfer_tab_live and clear transfer_tab_live_url
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
                method.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterPopularLivesAbParams(Ljava/util/Map;)Ljava/util/Map;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            patched++

            // Sanitize Lynx schema URL to strip intermediate_show_trending_billboard (LX/0HAc; in v47.1.4, was LX/0HAY;)
            val schemaFp = Fingerprint(
                definingClass = "LX/0HAc;",
                name = "LIZ",
                returnType = "Ljava/lang/String;",
            )
            val schemaMethod = schemaFp.method
            val schemaReturnIndices = schemaMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            schemaReturnIndices.asReversed().forEach { (returnIndex, reg) ->
                schemaMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterPopularLivesSchema(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            patched++

            println("[Search Experience] Applied $patched popular lives suppression hook(s) -> 'LIVE populares' neutralized.")
        }

        // 3. Disable Search Video Autoplay
        if (disableSearchAutoplay == true) {
            // Hook SearchListAutoplayHelper check logic loop to suppress background evaluation of autoplay candidates
            val searchListAutoplayFp = Fingerprint(
                returnType = "V",
                strings = listOf("checkLogic() is not called on main thread"),
            )
            searchListAutoplayFp.method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            patched++

            // Hook SearchCardVideoPlayerAssem$autoPlayAbility$2$1 to disable card autoplay ability and playback triggers
            val videoCardAbilityClass = "Lcom/ss/android/ugc/aweme/search/arch/v2/protocol/card/components/SearchCardVideoPlayerAssem${'$'}autoPlayAbility${'$'}2${'$'}1;"

            val videoCardL2Fp = Fingerprint(
                definingClass = videoCardAbilityClass,
                name = "l2",
                returnType = "Z",
            )
            videoCardL2Fp.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            patched++

            val videoCardRFp = Fingerprint(
                definingClass = videoCardAbilityClass,
                name = "r",
                returnType = "V",
            )
            videoCardRFp.method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            patched++

            // Hook SearchVideoForLynx$ability$1 to disable Lynx search video autoplay ability and playback triggers
            val lynxVideoAbilityClass = "Lcom/ss/android/ugc/aweme/search/lynx/xsearch/searchvideo/core/ui/SearchVideoForLynx${'$'}ability${'$'}1;"

            val lynxVideoL2Fp = Fingerprint(
                definingClass = lynxVideoAbilityClass,
                name = "l2",
                returnType = "Z",
            )
            lynxVideoL2Fp.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            patched++

            val lynxVideoRFp = Fingerprint(
                definingClass = lynxVideoAbilityClass,
                name = "r",
                returnType = "V",
            )
            lynxVideoRFp.method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            patched++

            // Hook SearchCardPhotoPlayerAssem$autoPlayAbility$2$1 to disable photo/slideshow search card autoplay
            val photoCardAbilityClass = "Lcom/ss/android/ugc/aweme/search/arch/v2/protocol/card/components/SearchCardPhotoPlayerAssem${'$'}autoPlayAbility${'$'}2${'$'}1;"

            val photoCardL2Fp = Fingerprint(
                definingClass = photoCardAbilityClass,
                name = "l2",
                returnType = "Z",
            )
            photoCardL2Fp.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            patched++

            val photoCardRFp = Fingerprint(
                definingClass = photoCardAbilityClass,
                name = "r",
                returnType = "V",
            )
            photoCardRFp.method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            patched++

            println("[Search Experience] Applied $patched search autoplay hook(s) -> Search video autoplay disabled.")
        }

        // 4. Force Non-Personalized Search
        if (forceNonPersonalized == true) {
            try {
                val gate = Fingerprint(
                    returnType = "Z",
                    parameters = emptyList(),
                    strings = listOf("enable_non_personalized_search"),
                ).method
                patched += forceBooleanGateTrue(gate, "Search Experience")
            } catch (e: Exception) {
                println("[Search Experience] Search gate note: ${e.message}")
            }

            try {
                val state = Fingerprint(
                    returnType = "Z",
                    parameters = emptyList(),
                    strings = listOf("non_personalized_search_state_"),
                ).method
                patched += forceBooleanGateTrue(state, "Search Experience")
            } catch (e: Exception) {
                println("[Search Experience] Search state note: ${e.message}")
            }

            println("[Search Experience] Applied non-personalized search hook(s).")
        }

        // 5. Enable Live Search
        if (enableLiveSearch == true) {
            try {
                val gate = Fingerprint(
                    definingClass = "Lcom/bytedance/android/livesdk/livesetting/feed/LiveDrawerSearchEnableSetting;",
                    name = "getValue",
                ).method
                val returnType = gate.returnType.toString()
                if (returnType != "Z" && returnType != "I") {
                    println("[Search Experience] Note: getValue returns $returnType, skipped.")
                } else {
                    val returnIndices = gate.implementation?.instructions?.withIndex()
                        ?.filter { it.value.opcode == Opcode.RETURN }
                        ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                        ?.toList() ?: emptyList()
                    returnIndices.asReversed().forEach { (returnIndex, reg) ->
                        gate.addInstructionsAtControlFlowLabel(
                            returnIndex,
                            "const/4 v$reg, 0x1",
                        )
                    }
                    if (returnIndices.isNotEmpty()) {
                        println("[Search Experience] Forced LiveDrawerSearchEnableSetting.getValue (${returnIndices.size} return(s)) -> enabled.")
                        patched++
                    }
                }
            } catch (e: Exception) {
                println("[Search Experience] Live drawer search gate note: ${e.message}")
            }

            println("[Search Experience] Applied live search hook(s).")
        }

        println("[Search Experience] Applied $patched hooks -> search experience customized.")
    }
}
