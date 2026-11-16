package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.numberOfParameterRegisters
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

val feedNavigationDeclutterPatch = bytecodePatch(
    name = "Declutter Navigation Bar",
    description = "Removes extra tabs and buttons from navigation bars.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_LITE)

    val hideShopTab by booleanOption(
        key = "hideShopTab",
        default = true,
        title = "Hide Shop Navigation Tab",
        description = "Removes the TikTok Shop / Mall navigation tab and indicator from navigation bars. Disable if you want to keep access to your purchases and orders while still hiding video product tags.",
        required = false,
    )

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

    val hideFriendsTab by booleanOption(
        key = "hideFriendsTab",
        default = false,
        title = "Hide Friends Tab",
        description = "Removes the Friends navigation tab from navigation bars.",
        required = false,
    )

    val hideInboxTab by booleanOption(
        key = "hideInboxTab",
        default = false,
        title = "Hide Inbox Tab",
        description = "Removes the Inbox / Messages navigation tab from the bottom navigation bar.",
        required = false,
    )

    val hideStemTab by booleanOption(
        key = "hideStemTab",
        default = false,
        title = "Hide STEM Feed Tab",
        description = "Removes the STEM feed tab from the top navigation feed strip.",
        required = false,
    )

    execute {
        val isLite = packageMetadata.packageName == Constants.TIKTOK_LITE_PACKAGE_NAME

        if (hideShopTab != true &&
            hideNearbyTab != true &&
            hideCommunityTab != true &&
            hideTopLiveEntrance != true &&
            hideFeedSearchBar != true &&
            hidePublishTab != true &&
            hideFriendsAvatarPreview != true &&
            hideInboxBadge != true &&
            hideFriendsTab != true &&
            hideInboxTab != true &&
            hideStemTab != true
        ) {
            println("[Declutter Navigation Bar] Skipped: All declutter options are disabled.")
            return@execute
        }

        if (isLite) {
            executeLite(
                hideShopTab = hideShopTab,
                hideCommunityTab = hideCommunityTab,
                hidePublishTab = hidePublishTab,
                hideFriendsTab = hideFriendsTab,
                hideInboxTab = hideInboxTab,
                hideFeedSearchBar = hideFeedSearchBar,
                hideInboxBadge = hideInboxBadge,
            )
            return@execute
        }

        var patched = 0

        // 0. Feature: Hide Shop Navigation Tab
        if (hideShopTab == true) {
            // Disable ShopBottomTabProtocol
            val bottomTabFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/ecommerce/mall/ShopBottomTabProtocol;",
                name = "enable",
                returnType = "Z",
            )
            bottomTabFp.method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++

            // Disable ShopTopTabProtocol
            val topTabFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/ecommerce/mall/ShopTopTabProtocol;",
                name = "enable",
                returnType = "Z",
            )
            topTabFp.method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++

            // Disable ShopIconServiceImpl indicator
            val iconServiceFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/ecommerce/mall/vm/ShopIconServiceImpl;",
                parameters = emptyList(),
                returnType = "Z",
            )
            iconServiceFp.method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++

            println("[Declutter Navigation Bar] TikTok Shop / Mall navigation tabs eliminated.")
        }

        // 1. Feature: Hide Nearby Feed Tab
        if (hideNearbyTab == true) {
            val serviceClass = "Lcom/ss/android/ugc/nearby/service/NearbyServiceImpl;"
            val serviceFp = Fingerprint(
                definingClass = serviceClass,
                name = "LJIJ",
                parameters = emptyList(),
            )
            var tabProviderClass = "LX/03mE;"
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
                name = "LJIIIZ",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            Fingerprint(
                definingClass = serviceClass,
                name = "LJIILIIL",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            patched++

            println("[Declutter Navigation Bar] Nearby feed tab eliminated.")
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

            var tabProviderClass = "LX/04sd;"
            val serviceDef = classDefByOrNull("Lcom/ss/android/ugc/aweme/explore/service/ExploreFeedServiceImpl;")
            if (serviceDef != null) {
                for (method in serviceDef.methods) {
                    if (method.parameterTypes.isEmpty() && method.returnType.startsWith("LX/")) {
                        for (insn in method.implementation?.instructions ?: emptyList()) {
                            if (insn.opcode == Opcode.NEW_INSTANCE) {
                                val ref = (insn as? ReferenceInstruction)?.reference as? TypeReference
                                if (ref != null && ref.type.startsWith("LX/")) {
                                    tabProviderClass = ref.type
                                    break
                                }
                            }
                        }
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

            println("[Declutter Navigation Bar] Community (Explore) tab eliminated.")
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
                name = "LJIIJJI",
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

            println("[Declutter Navigation Bar] Top-Left LIVE button and tab eliminated.")
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

            println("[Declutter Navigation Bar] Feed search suggestion and trending bars neutralized.")
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

            println("[Declutter Navigation Bar] Bottom publish (+) button eliminated.")
        }

        val tabManagerClasses by lazy {
            val avatarAbilityClass = "Lcom/ss/android/ugc/aweme/friendstab/ability/BaseBottomTabAvatarAbility;"
            val abilityDef = classDefByOrNull(avatarAbilityClass)
            val candidateReturnTypes = abilityDef?.methods
                ?.filter { it.parameters.isEmpty() && it.returnType.startsWith("LX/") }
                ?.map { it.returnType }
                ?.toSet() ?: setOf("LX/06KN;")

            val managers = mutableListOf<String>()
            classDefForEach { cls ->
                if (cls.superclass in candidateReturnTypes) {
                    val hasAvatarMethod = cls.methods.any {
                        it.name == "LJI" && it.parameters == listOf("Lcom/ss/android/ugc/aweme/avatar/AvatarComponentView;", "Ljava/lang/String;")
                    }
                    if (hasAvatarMethod) {
                        managers.add(cls.type)
                    }
                }
            }
            if (managers.isEmpty()) {
                managers.addAll(listOf("LX/08Yg;", "LX/08Yx;"))
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

            // Avatar preview active check (dynamically discovered short boolean getter)
            val avatarAbilityDef = classDefBy("Lcom/ss/android/ugc/aweme/friendstab/ability/BaseBottomTabAvatarAbility;")
            val isAvatarActiveMethod = avatarAbilityDef.methods.firstOrNull { method ->
                method.returnType == "Z" && method.parameterTypes.isEmpty() &&
                    method.name != "isShowing" &&
                    (method.implementation?.instructions?.count() ?: 0) <= 4
            }?.name ?: "VW1"

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/ability/BaseBottomTabAvatarAbility;",
                name = isAvatarActiveMethod,
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

            println("[Declutter Navigation Bar] Friends tab avatar preview neutralized.")
        }

        // 7. Feature: Hide Inbox Notification Badge
        if (hideInboxBadge == true) {
            val managerMethodsToGuard = listOf("LJIIL", "LJJL", "LJIIJJI", "LJJIIJ")
            var guardedMethods = 0
            val guardedManagers = mutableSetOf<String>()

            for (managerClass in tabManagerClasses) {
                val cls = classDefByOrNull(managerClass) ?: continue
                for (method in cls.methods) {
                    if (method.name in managerMethodsToGuard) {
                        val paramTypes = method.parameterTypes
                        val fpParams: List<String> = paramTypes.map { it.toString() }
                        val stringIdx = fpParams.indexOf("Ljava/lang/String;")
                        if (stringIdx != -1) {
                            val isStatic = method.accessFlags and 0x0008 != 0
                            val pStart = if (isStatic) 0 else 1
                            var pString = pStart
                            for (i in 0 until stringIdx) {
                                pString += if (fpParams[i] == "J" || fpParams[i] == "D") 2 else 1
                            }
                            
                            val localsCount = (method.implementation?.registerCount ?: 0) - method.numberOfParameterRegisters
                            if (localsCount >= 1) {
                                val fpRet: String = method.returnType
                                Fingerprint(
                                    definingClass = managerClass,
                                    name = method.name,
                                    returnType = fpRet,
                                    parameters = fpParams,
                                ).method.apply {
                                    val returnInst = if (method.returnType == "V") "return-void" else if (method.returnType == "Z" || method.returnType == "I") "const/4 v0, 0\nreturn v0" else "const/4 v0, 0\nreturn-object v0"
                                    addInstructions(
                                        0,
                                        """
                                        const-string v0, "NOTIFICATION"
                                        invoke-virtual {p$pString, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                                        move-result v0
                                        if-eqz v0, :cond_skip_badge
                                        $returnInst
                                        :cond_skip_badge
                                        """.trimIndent()
                                    )
                                    patched++
                                    guardedMethods++
                                    guardedManagers.add(managerClass)
                                }
                            }
                        }
                    }
                }
            }

            if (guardedMethods > 0) {
                println("[Declutter Navigation Bar] Inbox bottom tab guarded on $guardedMethods method(s) across ${guardedManagers.size} manager(s).")
            } else {
                println("[Declutter Navigation Bar] Inbox bottom tab badge guard failed: Feature became no-op (no methods found with locals).")
                require(guardedMethods > 0) { "No inbox badge guard target resolved for NOTIFICATION tab" }
            }
        }

        // 8. Feature: Hide Friends Tab
        if (hideFriendsTab == true) {
            val bottomFriendsTabFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/tab/FriendsTabProtocol;",
                name = "enable",
                returnType = "Z",
            )
            bottomFriendsTabFp.method.replaceWithReturnBoolean(false)
            patched++

            val topFriendsTabFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/tab/FriendTopTabProtocol;",
                name = "enable",
                returnType = "Z",
            )
            topFriendsTabFp.method.replaceWithReturnBoolean(false)
            patched++

            println("[Declutter Navigation Bar] Friends navigation tabs (bottom & top) eliminated.")
        }

        // 9. Feature: Hide Inbox Tab
        if (hideInboxTab == true) {
            val inboxTabFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/notification/view/NotificationTabProtocol;",
                name = "enable",
                returnType = "Z",
            )
            inboxTabFp.method.replaceWithReturnBoolean(false)
            patched++

            println("[Declutter Navigation Bar] Bottom Messages (Inbox) tab eliminated.")
        }

        // 10. Feature: Hide STEM Feed Tab
        if (hideStemTab == true) {
            val stemTabFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/stemfeed/ui/tab/StemTabProtocol;",
                name = "enable",
                returnType = "Z",
            )
            stemTabFp.method.replaceWithReturnBoolean(false)
            patched++

            println("[Declutter Navigation Bar] Top STEM feed tab eliminated.")
        }

        if (patched == 0) {
            println("[Declutter Navigation Bar] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Declutter Navigation Bar] Applied $patched navigation & header declutter hook(s).")
        }
    }
}

private fun BytecodePatchContext.executeLite(
    hideShopTab: Boolean?,
    hideCommunityTab: Boolean?,
    hidePublishTab: Boolean?,
    hideFriendsTab: Boolean?,
    hideInboxTab: Boolean?,
    hideFeedSearchBar: Boolean?,
    hideInboxBadge: Boolean?,
) {
    var patched = 0

    val keysToRemove = mutableListOf<String>()
    if (hideShopTab == true) keysToRemove.add("SHOP_MALL")
    if (hideCommunityTab == true) keysToRemove.addAll(listOf("DISCOVER", "EXPLORE", "INCENTIVE", "MINI_DRAMA"))
    if (hidePublishTab == true) keysToRemove.addAll(listOf("CREATION", "SHOOT"))
    if (hideFriendsTab == true) keysToRemove.add("FRIENDS")
    if (hideInboxTab == true) keysToRemove.addAll(listOf("NOTIFICATION", "INBOX"))

    var g3fClass: String? = null

    if (keysToRemove.isNotEmpty()) {
        val g3fMethodFp = Fingerprint(
            strings = listOf("select_type", "open_app", "click_time"),
            parameters = listOf("Ljava/lang/String;", "Ljava/util/List;"),
            returnType = "V",
        )
        val g3fMethod = g3fMethodFp.method
        g3fClass = g3fMethod.definingClass

        var fvnClass: String? = null
        var lccMethodName: String? = null
        var g5rClass: String? = null
        for (insn in g3fMethod.implementation?.instructions ?: emptyList()) {
            val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
            if (ref.parameterTypes.size == 3 && ref.parameterTypes[1].toString() == "Z" && ref.parameterTypes[2].toString() == "I") {
                fvnClass = ref.definingClass
                lccMethodName = ref.name
                g5rClass = ref.parameterTypes[0].toString()
                break
            }
        }

        if (g5rClass != null) {
            val g5rDef = classDefBy(g5rClass)
            val keyField = g5rDef.instanceFields.first { it.type == "Ljava/lang/String;" }.name

            val comparisons = keysToRemove.joinToString("\n") { key ->
                """
                const-string v2, "$key"
                invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v2
                if-eqz v2, :remove_tab
                """.trimIndent()
            }

            g3fMethod.ensureRegisterCount(13)

            val filterSmali = """
                if-nez p1, :filter_done
                invoke-interface {p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                move-result-object v0
                :filter_loop
                invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z
                move-result v1
                if-nez v1, :filter_done
                invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v1
                if-nez v1, :filter_loop
                check-cast v1, $g5rClass
                iget-object v1, v1, $g5rClass->$keyField:Ljava/lang/String;
                if-nez v1, :filter_loop
                $comparisons
                goto :filter_loop
                :remove_tab
                invoke-interface {v0}, Ljava/util/Iterator;->remove()V
                goto :filter_loop
                :filter_done
            """.trimIndent()

            g3fMethod.addInstructions(0, filterSmali)
            patched++
            println("[Declutter Navigation Bar] Injected bottom tab filter in $g3fClass.${g3fMethod.name} (pruning ${keysToRemove.size} tab key(s)).")
        }

        if (fvnClass != null && lccMethodName != null) {
            val fvnDef = mutableClassDefBy(fvnClass)
            val lccMethod = fvnDef.methods.first { it.name == lccMethodName }
            val instructions = lccMethod.implementation?.instructions?.toList() ?: emptyList()
            val initIndices = instructions.mapIndexedNotNull { idx, insn ->
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference
                if (insn.opcode == Opcode.INVOKE_DIRECT &&
                    ref?.definingClass == "Landroid/widget/LinearLayout\$LayoutParams;" &&
                    ref.name == "<init>"
                ) idx else null
            }

            for (idx in initIndices.asReversed()) {
                val invokeInsn = instructions[idx] as? FiveRegisterInstruction
                val reg = invokeInsn?.registerC ?: 0
                val constReg = if (reg == 0) 1 else 0
                lccMethod.addInstructions(
                    idx + 1,
                    """
                    const/4 v$constReg, 0x0
                    iput v$constReg, v$reg, Landroid/view/ViewGroup${'$'}LayoutParams;->width:I
                    const/high16 v$constReg, 0x3f800000
                    iput v$constReg, v$reg, Landroid/widget/LinearLayout${'$'}LayoutParams;->weight:F
                    """.trimIndent()
                )
                patched++
            }
            println("[Declutter Navigation Bar] Injected dynamic LayoutParams weight in $fvnClass.$lccMethodName across ${initIndices.size} callsite(s).")
        }
    }

    if (hideInboxBadge == true) {
        if (g3fClass == null) {
            val g3fMethodFp = Fingerprint(
                strings = listOf("select_type", "open_app", "click_time"),
                parameters = listOf("Ljava/lang/String;", "Ljava/util/List;"),
                returnType = "V",
            )
            g3fClass = g3fMethodFp.method.definingClass
        }
        val g3fDef = mutableClassDefByOrNull(g3fClass ?: "")
        val lfllMethod = g3fDef?.methods?.firstOrNull {
            it.parameterTypes.size == 1 &&
                it.returnType == "V" &&
                !it.parameterTypes[0].toString().startsWith("Ljava/") &&
                it.parameterTypes[0].toString().startsWith("LX/")
        }
        if (lfllMethod != null) {
            val gc4Class = lfllMethod.parameterTypes[0].toString()
            val gc4Def = classDefByOrNull(gc4Class)
            val keyField = gc4Def?.instanceFields?.firstOrNull { it.type == "Ljava/lang/String;" }?.name ?: "L"

            lfllMethod.ensureRegisterCount(4)
            lfllMethod.addInstructions(
                0,
                """
                if-nez p0, :skip_badge_guard
                iget-object v0, p0, $gc4Class->$keyField:Ljava/lang/String;
                if-nez v0, :skip_badge_guard
                const-string v1, "NOTIFICATION"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :return_badge_void
                goto :skip_badge_guard
                :return_badge_void
                return-void
                :skip_badge_guard
                """.trimIndent()
            )
            patched++
            println("[Declutter Navigation Bar] Guarded inbox notification badge in Lite via $g3fClass.${lfllMethod.name}.")
        }
    }

    if (hideFeedSearchBar == true) {
        val miniliteMainPage = mutableClassDefByOrNull("Lcom/ss/android/ugc/aweme/minilite/homepage/MainPageFragment;")
        if (miniliteMainPage != null) {
            val p5Method = miniliteMainPage.methods.firstOrNull { it.name == "p5" && it.returnType == "V" && it.parameterTypes.isEmpty() }
            if (p5Method != null) {
                val fieldRef = p5Method.implementation?.instructions?.mapNotNull {
                    (it as? ReferenceInstruction)?.reference as? FieldReference
                }?.firstOrNull { it.definingClass == "Lcom/ss/android/ugc/aweme/minilite/homepage/MainPageFragment;" }

                val fieldName = fieldRef?.name ?: "LIIZ"
                val fieldType = fieldRef?.type ?: "LX/0Sg;"

                p5Method.clearTryBlocks()
                p5Method.ensureRegisterCount(4)
                p5Method.removeInstructions(0, p5Method.implementation!!.instructions.count())
                p5Method.addInstructions(
                    0,
                    """
                    iget-object v0, p0, Lcom/ss/android/ugc/aweme/minilite/homepage/MainPageFragment;->$fieldName:$fieldType
                    if-eqz v0, :skip_hide
                    const/16 v1, 0x8
                    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                    :skip_hide
                    return-void
                    """.trimIndent()
                )
                patched++
                println("[Declutter Navigation Bar] Neutralized minilite search icon via p5().")
            }
        }
    }

    if (patched == 0) {
        println("[Declutter Navigation Bar] WARNING: applied 0 hook(s) on TikTok Lite -> targets moved upstream.")
    } else {
        println("[Declutter Navigation Bar] Successfully applied $patched navigation declutter hook(s) on TikTok Lite.")
    }
}
