package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnEmptyList
import app.morphe.patches.shared.replaceWithReturnVoid
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val suggestedAccountInjectors = listOf(
    "Lcom/ss/android/ugc/aweme/inbox/v2/container/UserCardWidgetContainerInjector;",
    "Lcom/ss/android/ugc/aweme/inbox/v2/container/InboxUserCardWidgetContainerInjector;",
    "Lcom/ss/android/ugc/aweme/inbox/v2/container/UserCardWidgetVisibleContainerInjector;",
    "Lcom/ss/android/ugc/aweme/inbox/followerv2/FollowerUserCardWidgetContainerInjector;",
    "Lcom/ss/android/ugc/aweme/inbox/widget/multi/FollowerUserCardLoadingWidgetV2Injector;",
    "Lcom/ss/android/ugc/aweme/relation/recuser/inbox/FollowerUserCardWidgetV2Injector;",
    "Lcom/ss/android/ugc/aweme/notification/v2/widget/container/UserCardWidgetContainerInjector;",
    "Lcom/ss/android/ugc/aweme/relation/recuser/inbox/NotificationRecommendUserWidgetV2Injector;",
)

val friendsSuggestionsFilterPatch = bytecodePatch(
    name = "Friends & Suggestions Filter",
    description = "Keeps the Friends feed strictly mutual and hides suggested accounts across the app.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val strictMutualsOnly by booleanOption(
        key = "strictMutualsOnly",
        title = "Strict Mutual Friends Feed",
        description = "The Friends feed only shows people you both follow.",
        default = true,
        required = false,
    )

    val filterFriendFeedBloat by booleanOption(
        key = "filterFriendFeedBloat",
        title = "Filter Friends Feed Bloat",
        description = "Removes suggested friend card insertions, recommended user horizontal lists, and swipe cards from the Friends feed.",
        default = true,
        required = false,
    )

    val hideSuggestedAccounts by booleanOption(
        key = "hideSuggestedAccounts",
        title = "Hide Suggested Accounts",
        description = "Removes suggested-account cards from profile headers and inbox surfaces.",
        default = true,
        required = false,
    )

    execute {
        if (strictMutualsOnly != true &&
            filterFriendFeedBloat != true &&
            hideSuggestedAccounts != true
        ) {
            println("[Friends & Suggestions Filter] Skipped: All friends & suggestions filters are disabled.")
            return@execute
        }

        var patched = 0

        // 1. Strict Mutuals Friends Feed Hooks (from FriendsFeedStrictMutuals)
        if (strictMutualsOnly == true) {
            // Hook FriendsFeedResponse constructor (p3: List<FriendsFeed>)
            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/friendstab/api/FriendsFeedResponse;",
                    name = "<init>",
                    parameters = listOf(
                        "I",
                        "Z",
                        "Ljava/util/List;",
                        "Ljava/lang/String;",
                        "Ljava/lang/String;",
                        "Lcom/ss/android/ugc/aweme/feed/model/LogPbBean;",
                        "I",
                        "Ljava/util/List;",
                        "Ljava/util/List;",
                        "Ljava/lang/String;",
                    ),
                    returnType = "V",
                ).method.addInstructions(
                    0,
                    """
                        invoke-static {p3}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterSuggestedVideosInFriendsFeedData(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
                println("[Friends & Suggestions Filter] Hooked FriendsFeedResponse.<init> -> Friends V2 feed filtered for mutuals only.")
                patched++
            } catch (e: Exception) {
                println("[Friends & Suggestions Filter] FriendsFeedResponse note: ${e.message}")
            }

            // Hook FriendsFeedApi network fetch return points (LX/06Bw;->LIZLLL in v47.4.3, was LX/06CX;)
            val friendsFeedApiFingerprint = Fingerprint(
                definingClass = "LX/064b;",
                name = "LIZLLL",
                parameters = listOf(
                    "I",
                    "I",
                    "Ljava/lang/String;",
                    "Ljava/util/List;",
                    "Ljava/util/List;",
                    "Ljava/util/List;",
                    "Ljava/util/List;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Z",
                    "Z",
                ),
                returnType = "Lcom/ss/android/ugc/aweme/friendstab/api/FriendsFeedResponse;",
            )
            val friendsFeedApiMethod = friendsFeedApiFingerprint.method
            val friendsFeedApiReturns = friendsFeedApiMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            friendsFeedApiReturns.asReversed().forEach { (returnIndex, reg) ->
                friendsFeedApiMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterSuggestedVideosInFriendsFeedResponse(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (friendsFeedApiReturns.isNotEmpty()) {
                println("[Friends & Suggestions Filter] Hooked FriendsFeedApi.LIZLLL (${friendsFeedApiReturns.size} return point(s)) -> Friends V2 network responses protected.")
                patched++
            }
        }

        // 2. Friends Feed Bloat, Suggested Friend Cards & Recommended Users (FeedBloat Section 7 blocks)
        if (filterFriendFeedBloat == true) {
            // Hook FriendsFeedResponse.<init>
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/api/FriendsFeedResponse;",
                name = "<init>",
                parameters = listOf(
                    "I",
                    "Z",
                    "Ljava/util/List;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Lcom/ss/android/ugc/aweme/feed/model/LogPbBean;",
                    "I",
                    "Ljava/util/List;",
                    "Ljava/util/List;",
                    "Ljava/lang/String;",
                ),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    invoke-static {p3}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterFeedBloatInFriendsFeedData(Ljava/lang/Object;)V
                    const/4 p8, 0x0
                    const/4 p9, 0x0
                """.trimIndent(),
            )
            println("[Friends & Suggestions Filter] Hooked FriendsFeedResponse.<init> -> Friends V2 feed filtered and inserted cards nulled.")
            patched++

            // Hook FriendsFeedApi network fetch return points (LX/06Bw;->LIZLLL in v47.4.3, was LX/06CX;)
            val friendsFeedApiFingerprint = Fingerprint(
                definingClass = "LX/064b;",
                name = "LIZLLL",
                parameters = listOf(
                    "I",
                    "I",
                    "Ljava/lang/String;",
                    "Ljava/util/List;",
                    "Ljava/util/List;",
                    "Ljava/util/List;",
                    "Ljava/util/List;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Z",
                    "Z",
                ),
                returnType = "Lcom/ss/android/ugc/aweme/friendstab/api/FriendsFeedResponse;",
            )
            val friendsFeedApiMethod = friendsFeedApiFingerprint.method
            val friendsFeedApiReturns = friendsFeedApiMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            friendsFeedApiReturns.asReversed().forEach { (returnIndex, reg) ->
                friendsFeedApiMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterFeedBloatInFriendsFeedResponse(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (friendsFeedApiReturns.isNotEmpty()) {
                println("[Friends & Suggestions Filter] Hooked FriendsFeedApi.LIZLLL (${friendsFeedApiReturns.size} return point(s)) -> Friends V2 network responses filtered and inserted cards nulled.")
                patched++
            }

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/v4/RecSwipeCardListAssem;",
                name = "Fp", // in v47.5.3, was Dp in v47.4.3
                parameters = listOf("Ljava/util/List;"),
                returnType = "Ljava/util/List;",
            ).method.replaceWithReturnEmptyList()
            println("[Friends & Suggestions Filter] Emptied RecSwipeCardListAssem.Fp() -> Friend suggestion swipe cards suppressed.")
            patched++

            try {
                val bigCardConfigFull = Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/RecUserBigCardConfig;",
                    name = "<init>",
                    parameters = listOf(
                        "I",
                        "I",
                        "J",
                        "J",
                        "J",
                        "I",
                        "I",
                        "I",
                        "Z",
                        "Z",
                        "Lcom/ss/android/ugc/aweme/relation/feed/FYPConfig;",
                        "Lcom/ss/android/ugc/aweme/relation/feed/FriendsTabConfig;",
                        "I",
                        "Z",
                    ),
                    returnType = "V",
                ).method
                val fullReturns = bigCardConfigFull.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN_VOID }
                    ?.map { it.index }
                    ?.toList() ?: emptyList()
                fullReturns.asReversed().forEach { returnIndex ->
                    bigCardConfigFull.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        """
                            invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->disableRecUserBigCardInFriendsTab(Ljava/lang/Object;)V
                        """.trimIndent(),
                    )
                }
                if (fullReturns.isNotEmpty()) {
                    println("[Friends & Suggestions Filter] Disabled RecUserBigCardConfig.<init>(full) -> Friends tab swipe BigCards disabled.")
                    patched++
                }
            } catch (e: Exception) {
                println("[Friends & Suggestions Filter] RecUserBigCardConfig(full) note: ${e.message}")
            }

            try {
                val methodLJIIIZ = Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/TTRecUserBigCardViewHolder;",
                    name = "LJIIIZ",
                    parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
                    returnType = "V",
                ).method
                val returnsLJIIIZ = methodLJIIIZ.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN_VOID }
                    ?.map { it.index }
                    ?.toList() ?: emptyList()
                returnsLJIIIZ.asReversed().forEach { returnIndex ->
                    methodLJIIIZ.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        """
                            invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->collapseRecUserCardCell(Ljava/lang/Object;)V
                        """.trimIndent(),
                    )
                }
                if (returnsLJIIIZ.isNotEmpty()) {
                    println("[Friends & Suggestions Filter] Collapsed TTRecUserBigCardViewHolder.LJIIIZ(Aweme).")
                    patched++
                } else {
                    println("[Friends & Suggestions Filter] TTRecUserBigCardViewHolder.LJIIIZ note: No RETURN_VOID found")
                }
            } catch (e: Exception) {
                println("[Friends & Suggestions Filter] TTRecUserBigCardViewHolder.LJIIIZ note: ${e.message}")
            }

            try {
                val methodNu = Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/TTRecUserBigCardViewHolder;",
                    name = "nu",
                    parameters = listOf("Landroid/view/View;"),
                    returnType = "V",
                ).method
                val returnsNu = methodNu.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN_VOID }
                    ?.map { it.index }
                    ?.toList() ?: emptyList()
                returnsNu.asReversed().forEach { returnIndex ->
                    methodNu.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        """
                            invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->collapseRecUserCardCell(Ljava/lang/Object;)V
                        """.trimIndent(),
                    )
                }
                if (returnsNu.isNotEmpty()) {
                    println("[Friends & Suggestions Filter] Collapsed TTRecUserBigCardViewHolder.nu(View).")
                    patched++
                } else {
                    println("[Friends & Suggestions Filter] TTRecUserBigCardViewHolder.nu note: No RETURN_VOID found")
                }
            } catch (e: Exception) {
                println("[Friends & Suggestions Filter] TTRecUserBigCardViewHolder.nu note: ${e.message}")
            }

            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/v4/RecSwipeViewModel;",
                    name = "a60",
                    parameters = listOf("Ljava/lang/String;"),
                    returnType = "Ljava/util/concurrent/CopyOnWriteArrayList;",
                ).method.addInstructions(
                    0,
                    """
                        new-instance v0, Ljava/util/concurrent/CopyOnWriteArrayList;
                        invoke-direct {v0}, Ljava/util/concurrent/CopyOnWriteArrayList;-><init>()V
                        return-object v0
                    """.trimIndent(),
                )
                println("[Friends & Suggestions Filter] Emptied RecSwipe backing list source (RecSwipeViewModel.a60) -> Friend suggestion swipe cards starved.")
                patched++
            } catch (e: Exception) {
                println("[Friends & Suggestions Filter] RecSwipeViewModel.a60 note: ${e.message}")
            }

            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/v4/RecSwipeViewModel;",
                    name = "U23",
                    parameters = listOf("Ljava/util/List;"),
                    returnType = "V",
                ).method.addInstructions(
                    0,
                    """
                        return-void
                    """.trimIndent(),
                )
                println("[Friends & Suggestions Filter] Neutralized RecSwipe backing list loader (RecSwipeViewModel.U23).")
                patched++
            } catch (e: Exception) {
                println("[Friends & Suggestions Filter] RecSwipeViewModel.U23 note: ${e.message}")
            }

            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/v4/RecSwipeViewModel;",
                    name = "Kc",
                    parameters = listOf("Ljava/lang/String;"),
                    returnType = "Z",
                ).method.addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        return v0
                    """.trimIndent(),
                )
                println("[Friends & Suggestions Filter] Neutralized RecSwipeViewModel.Kc() -> has-data gate forced false.")
                patched++
            } catch (e: Exception) {
                println("[Friends & Suggestions Filter] RecSwipeViewModel.Kc note: ${e.message}")
            }

            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/v4/RecSwipeCardListAssem;",
                    name = "onViewCreated",
                    parameters = listOf("Landroid/view/View;"),
                    returnType = "V",
                ).method.addInstructions(
                    0,
                    """
                        const/16 v0, 0x8
                        move-object/from16 v1, p1
                        invoke-virtual {v1, v0}, Landroid/view/View;->setVisibility(I)V
                        return-void
                    """.trimIndent(),
                )
                println("[Friends & Suggestions Filter] Collapsed RecSwipeCardListAssem.onViewCreated() -> swipe stack container hidden.")
                patched++
            } catch (e: Exception) {
                println("[Friends & Suggestions Filter] RecSwipeCardListAssem.onViewCreated note: ${e.message}")
            }
        }

        // 3. Branch: hideSuggestedAccounts (Profile header and inbox surfaces)
        if (hideSuggestedAccounts != false) {
            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/profile/platform/business/header/business/recommend/assemble/ProfileHeaderRecommendComponent;",
                    returnType = "V",
                    parameters = emptyList(),
                    strings = listOf("recommend_user_card"),
                ).method.replaceWithReturnVoid()
                println("[Friends & Suggestions Filter] Suppressed ProfileHeaderRecommendComponent -> Profile recommend cards blocked.")
                patched++
            } catch (e: Exception) {
                println("[Friends & Suggestions Filter] ProfileHeaderRecommendComponent note: ${e.message}")
            }

            suggestedAccountInjectors.forEach { injector ->
                try {
                    Fingerprint(
                        definingClass = injector,
                        name = "enable",
                        parameters = emptyList(),
                        returnType = "Z",
                    ).method.replaceWithReturnBoolean(false)
                    println("[Friends & Suggestions Filter] Disabled ${injector.substringAfterLast("/").removeSuffix(";")} -> Suggested cards blocked.")
                    patched++
                } catch (e: Exception) {
                    println("[Friends & Suggestions Filter] ${injector.substringAfterLast("/").removeSuffix(";")} note: ${e.message}")
                }
            }
        }

        if (patched == 0) {
            println("[Friends & Suggestions Filter] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Friends & Suggestions Filter] Applied $patched friends & suggestions filter hook(s).")
        }
    }
}
