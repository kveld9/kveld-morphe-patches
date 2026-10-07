package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

val feedNavigationDeclutterPatch = bytecodePatch(
    name = "Navigation & Header Declutter",
    description = "Removes clutter from the feed navigation and top header bar, including the Nearby feed tab, Community (Explore) tab, top-left LIVE broadcast button, central '+' create content button, in-video bottom search suggestion bar, friend profile photo previews on the bottom Friends tab, and unread notification badges on the bottom Messages (Inbox) tab.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    val hideNearbyTab by booleanOption(
        key = "hideNearbyTab",
        default = true,
        title = "Hide Nearby Feed Tab",
        description = "Removes the Nearby (local city or region) feed tab from the top navigation feed strip.",
        required = false,
    )

    val hideCommunityTab by booleanOption(
        key = "hideCommunityTab",
        default = true,
        title = "Hide Community Tab",
        description = "Removes the Community (Explore) tab from the top navigation feed strip and bottom bar.",
        required = false,
    )

    val hideTopLiveEntrance by booleanOption(
        key = "hideTopLiveEntrance",
        default = false,
        title = "Hide Top-Left LIVE Button",
        description = "Removes the top-left LIVE broadcast button and tab entry point from the top navigation bar.",
        required = false,
    )

    val hideFeedSearchBar by booleanOption(
        key = "hideFeedSearchBar",
        default = true,
        title = "Hide Feed Search Bar",
        description = "Removes the search suggestion pill and trending bar ('Search · <keyword>') from the bottom of feed videos.",
        required = false,
    )

    val hidePublishTab by booleanOption(
        key = "hidePublishTab",
        default = false,
        title = "Hide Create / Publish Button",
        description = "Removes the central '+' create / publish content button from the bottom navigation bar.",
        required = false,
    )

    val hideFriendsAvatarPreview by booleanOption(
        key = "hideFriendsAvatarPreview",
        default = true,
        title = "Hide Friends Tab Avatar Preview",
        description = "Prevents friend profile pictures from replacing the Friends icon on the bottom navigation bar and keeps the standard icon visible.",
        required = false,
    )

    val hideInboxBadge by booleanOption(
        key = "hideInboxBadge",
        default = true,
        title = "Hide Inbox Notification Badge",
        description = "Removes the unread message counter badge and notification red dot from the bottom Messages (Inbox) tab.",
        required = false,
    )

    execute {
        if (hideNearbyTab != true &&
            hideCommunityTab != true &&
            hideTopLiveEntrance != true &&
            hideFeedSearchBar != true &&
            hidePublishTab != true &&
            hideFriendsAvatarPreview != true &&
            hideInboxBadge != true
        ) {
            println("[Navigation & Header Declutter] Skipped: All declutter options are disabled.")
            return@execute
        }

        var patched = 0

        // 1. Feature: Hide Nearby Feed Tab
        if (hideNearbyTab == true) {
            val serviceClass = "Lcom/ss/android/ugc/nearby/service/NearbyServiceImpl;"
            val serviceFp = Fingerprint(
                definingClass = serviceClass,
                name = "LJIIZILJ",
                parameters = emptyList(),
            )
            var tabProviderClass = "LX/03ry;"
            val serviceInsns = serviceFp.method.implementation?.instructions
            if (serviceInsns != null) {
                for (insn in serviceInsns) {
                    val ref = (insn as? ReferenceInstruction)?.reference
                    if (ref is TypeReference) {
                        tabProviderClass = ref.type
                        break
                    }
                }
            }

            Fingerprint(
                definingClass = tabProviderClass,
                name = "LJ",
                parameters = emptyList(),
            ).method.replaceWithReturnNull()
            patched++

            val nearbyTabClass = "Lcom/ss/android/ugc/nearby/tab/NearbyTabProtocol;"
            val nearbyTabFp = Fingerprint(
                definingClass = nearbyTabClass,
                name = "enable",
                returnType = "Z",
                parameters = emptyList(),
            )

            val nearbyTabInsns = nearbyTabFp.method.implementation?.instructions
            if (nearbyTabInsns != null) {
                for (insn in nearbyTabInsns) {
                    val ref = (insn as? ReferenceInstruction)?.reference
                    if (ref is MethodReference && !ref.definingClass.startsWith("Ljava/")) {
                        val evalClass = ref.definingClass
                        val methodName = ref.name
                        Fingerprint(
                            definingClass = evalClass,
                            name = methodName,
                            returnType = "Z",
                            parameters = emptyList(),
                        ).method.replaceWithReturnBoolean(false)
                        patched++
                        break
                    }
                }
            }

            nearbyTabFp.method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = serviceClass,
                name = "LJIIIIZZ",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = serviceClass,
                name = "LJIIL",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            println("[Navigation & Header Declutter] Nearby feed tab eliminated.")
        }

        // 2. Feature: Hide Community Tab
        if (hideCommunityTab == true) {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/explore/service/ExploreFeedServiceImpl;",
                name = "LIZ",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            val serviceFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/explore/service/ExploreFeedServiceImpl;",
                name = "LJJIII",
                parameters = emptyList(),
            )
            var tabProviderClass = "LX/03rx;"
            val insns = serviceFp.method.implementation?.instructions
            if (insns != null) {
                for (insn in insns) {
                    val ref = (insn as? ReferenceInstruction)?.reference
                    if (ref is TypeReference) {
                        tabProviderClass = ref.type
                        break
                    }
                }
            }

            Fingerprint(
                definingClass = tabProviderClass,
                name = "LJ",
                parameters = emptyList(),
            ).method.replaceWithReturnNull()
            patched++

            Fingerprint(
                definingClass = tabProviderClass,
                name = "LIZ",
                parameters = emptyList(),
            ).method.replaceWithReturnNull()
            patched++

            val xTabClass = "Lcom/ss/android/ugc/aweme/explore/entrance/ExploreXTabProtocol;"
            val xTabFp = Fingerprint(
                definingClass = xTabClass,
                name = "enable",
                returnType = "Z",
                parameters = emptyList(),
            )

            val xTabInsns = xTabFp.method.implementation?.instructions
            if (xTabInsns != null) {
                for (insn in xTabInsns) {
                    val ref = (insn as? ReferenceInstruction)?.reference
                    if (ref is MethodReference) {
                        val evalClass = ref.definingClass
                        Fingerprint(
                            definingClass = evalClass,
                            name = "LIZIZ",
                            returnType = "Z",
                            parameters = emptyList(),
                        ).method.replaceWithReturnBoolean(false)
                        patched++

                        Fingerprint(
                            definingClass = evalClass,
                            name = "LIZ",
                            returnType = "Z",
                            parameters = emptyList(),
                        ).method.replaceWithReturnBoolean(false)
                        patched++
                        break
                    }
                }
            }

            xTabFp.method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/explore/entrance/ExploreBottomTabProtocol;",
                name = "enable",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            println("[Navigation & Header Declutter] Community (Explore) tab eliminated.")
        }

        // 3. Feature: Hide Top-Left LIVE Button
        if (hideTopLiveEntrance == true) {
            val liveGenClass = "Lcom/bytedance/tiktok/homepage/mainfragment/toolbar/LiveIconGenerator;"

            Fingerprint(
                definingClass = liveGenClass,
                name = "enabled",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = liveGenClass,
                name = "LIZLLL",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            val viewMethod = Fingerprint(
                definingClass = liveGenClass,
                returnType = "Landroid/view/View;",
                parameters = listOf("Landroid/content/Context;"),
            ).method
            viewMethod.replaceWithReturnNull()
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/toptab/LiveTabProtocol;",
                name = "enable",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            println("[Navigation & Header Declutter] Top-Left LIVE button and tab eliminated.")
        }

        // 4. Feature: Hide Feed Search Bar
        if (hideFeedSearchBar == true) {
            val triggerClasses = listOf(
                "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssemTrigger;",
                "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssemTriggerV2;",
                "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/TrendingBottomBarAssemTrigger;",
                "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/AdFeedSearchBottomBarAssemTrigger;",
                "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedEcSearchBottomBarAssemTrigger;",
            )

            for (triggerClass in triggerClasses) {
                Fingerprint(
                    definingClass = triggerClass,
                    returnType = "Z",
                    parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
                ).method.replaceWithReturnBoolean(false)
                patched++
            }

            val assemClasses = listOf(
                "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssem;",
                "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssemV2;",
                "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/TrendingBottomBarAssem;",
                "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/AdFeedSearchBottomBarAssem;",
                "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedEcSearchBottomBarAssem;",
            )

            for (assemClass in assemClasses) {
                Fingerprint(
                    definingClass = assemClass,
                    name = "onViewCreated",
                    returnType = "V",
                    parameters = listOf("Landroid/view/View;"),
                ).method.replaceWithReturnVoid()
                patched++

                Fingerprint(
                    definingClass = assemClass,
                    returnType = "V",
                    parameters = listOf("Ljava/lang/Object;"),
                ).method.replaceWithReturnVoid()
                patched++
            }

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "isDisableSearchTrendingBar",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(true)
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "hasTrendingBar",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "hasTrendingBarFYP",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getTrendingBar",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/AwemeTrendingBar;",
            ).method.replaceWithReturnNull()
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getTrendingBarFYP",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/AwemeTrendingBar;",
            ).method.replaceWithReturnNull()
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getHotSearchInfo",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/HotSearchInfo;",
            ).method.replaceWithReturnNull()
            patched++

            println("[Navigation & Header Declutter] Feed search suggestion and trending bars neutralized.")
        }

        // 5. Feature: Hide Create / Publish Button
        if (hidePublishTab == true) {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/homepage/ui/view/tab/bottom/publishtab/PublishTabProtocol;",
                name = "enable",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/homepage/ui/view/tab/bottom/publishtab/PublishBottomTabViewFactory;",
                name = "LIZ",
                returnType = "Landroid/view/View;",
            ).method.apply {
                val moveResultIdx = implementation?.instructions?.indexOfFirst {
                    it.opcode == Opcode.MOVE_RESULT_OBJECT
                } ?: -1
                if (moveResultIdx != -1) {
                    val viewReg = (getInstruction<OneRegisterInstruction>(moveResultIdx)).registerA
                    val constReg = if (viewReg == 0) 1 else 0
                    addInstructions(
                        moveResultIdx + 1,
                        """
                        const/16 v$constReg, 0x8
                        invoke-virtual {v$viewReg, v$constReg}, Landroid/view/View;->setVisibility(I)V
                        """,
                    )
                    patched++
                }
            }

            println("[Navigation & Header Declutter] Bottom publish (+) button eliminated.")
        }

        val tabManagerClasses by lazy {
            val avatarAbilityClass = "Lcom/ss/android/ugc/aweme/friendstab/ability/BaseBottomTabAvatarAbility;"
            val ksMethod = classDefByOrNull(avatarAbilityClass)?.methods?.firstOrNull { it.name == "kS" }
            val tabManagerBaseType = ksMethod?.returnType ?: "LX/05Ha;"

            val managers = mutableListOf<String>()
            classDefForEach { cls ->
                if (cls.superclass == tabManagerBaseType) {
                    managers.add(cls.type)
                }
            }
            if (managers.isEmpty()) {
                managers.addAll(listOf("LX/06vO;", "LX/06vG;"))
            }
            managers
        }

        // 6. Feature: Hide Friends Tab Avatar Preview
        if (hideFriendsAvatarPreview == true) {
            val redDotServiceClass = classDefByOrNull { cls ->
                cls.interfaces.contains("Lcom/ss/android/ugc/aweme/friendstab/service/ISocial2TabRedDotService;")
            }?.type ?: "LX/08bf;"

            Fingerprint(
                definingClass = redDotServiceClass,
                name = "enableTabAvatar",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = redDotServiceClass,
                name = "loadAvatarAbility",
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++

            Fingerprint(
                definingClass = redDotServiceClass,
                name = "dealWithFriendsAvatar",
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/ability/BaseBottomTabAvatarAbility;",
                name = "isShowing",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/ability/BaseBottomTabAvatarAbility;",
                name = "cr2",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/ability/FriendBottomTabAvatarAbility;",
                name = "cr2",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            patched++

            for (managerClass in tabManagerClasses) {
                Fingerprint(
                    definingClass = managerClass,
                    name = "LJI",
                    returnType = "V",
                    parameters = listOf("Lcom/ss/android/ugc/aweme/avatar/AvatarComponentView;", "Ljava/lang/String;"),
                ).method.replaceWithReturnVoid()
                patched++

                Fingerprint(
                    definingClass = managerClass,
                    name = "LJII",
                    returnType = "V",
                    parameters = listOf("Lcom/ss/android/ugc/aweme/base/model/UrlModel;", "Ljava/lang/String;"),
                ).method.replaceWithReturnVoid()
                patched++

                Fingerprint(
                    definingClass = managerClass,
                    name = "LJJJI",
                    returnType = "V",
                    parameters = listOf("Lcom/ss/android/ugc/aweme/friendstab/model/UserNewContent;", "Ljava/lang/String;"),
                ).method.replaceWithReturnVoid()
                patched++

                Fingerprint(
                    definingClass = managerClass,
                    name = "LJJIIZ",
                    returnType = "Z",
                    parameters = listOf("Ljava/lang/String;"),
                ).method.replaceWithReturnBoolean(false)
                patched++
            }

            val reminderExpFp = Fingerprint(
                strings = listOf("tt_friends_tab_avatar_exemption_new_user_days"),
                returnType = "Z",
            )
            val expClass = reminderExpFp.classDef.type
            Fingerprint(
                definingClass = expClass,
                name = "LIZIZ",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = expClass,
                name = "LIZ",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            println("[Navigation & Header Declutter] Friends tab avatar preview neutralized.")
        }

        // 7. Feature: Hide Inbox Notification Badge
        if (hideInboxBadge == true) {
            val noticeServiceImplClass = "Lcom/ss/android/ugc/aweme/notification/service/NoticeCountTabBadgePresentServiceImpl;"

            listOf("onResume", "onReset", "LIZ", "LJ", "LJFF").forEach { name ->
                Fingerprint(
                    definingClass = noticeServiceImplClass,
                    name = name,
                    parameters = emptyList(),
                ).method.replaceWithReturnVoid()
                patched++
            }

            listOf("LIZIZ", "LIZLLL").forEach { name ->
                Fingerprint(
                    definingClass = noticeServiceImplClass,
                    name = name,
                    parameters = listOf("Z"),
                ).method.replaceWithReturnVoid()
                patched++
            }

            Fingerprint(
                definingClass = noticeServiceImplClass,
                name = "isShowing",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            val serviceDef = classDefByOrNull(noticeServiceImplClass)
            val presenterClass = serviceDef?.fields?.firstOrNull { it.name == "LIZ" }?.type ?: "LX/0CxD;"

            Fingerprint(
                definingClass = presenterClass,
                name = "onNoticeCountChangedEvent",
                parameters = listOf("LX/0716;"),
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++

            Fingerprint(
                definingClass = presenterClass,
                name = "LJJIJIIJI",
                parameters = listOf("LX/0716;"),
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++

            Fingerprint(
                definingClass = presenterClass,
                name = "LJIILIIL",
                parameters = emptyList(),
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++

            Fingerprint(
                definingClass = presenterClass,
                name = "LJIILL",
                parameters = emptyList(),
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++

            Fingerprint(
                definingClass = presenterClass,
                name = "LJJIIJZLJL",
                parameters = listOf("Z"),
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++

            Fingerprint(
                definingClass = "LX/0ALx;",
                name = "onInboxBadgeChanged",
                parameters = listOf("LX/0716;"),
                returnType = "V",
            ).method.replaceWithReturnVoid()
            println("[Navigation & Header Declutter] Neutralized LX/0ALx.onInboxBadgeChanged() -> Inbox tab badge change events suppressed.")
            patched++

            Fingerprint(
                definingClass = presenterClass,
                name = "LJIJJLI",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            println("[Navigation & Header Declutter] Inbox notification badge and unread counters suppressed.")
        }

        println("[Navigation & Header Declutter] Applied $patched navigation & header declutter hook(s).")
    }
}
