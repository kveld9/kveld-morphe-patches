package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid
import app.morphe.patches.shared.sharedExtensionPatch

val popupsAndPromptsSuppressorPatch = bytecodePatch(
    name = "Popups & Prompts Suppressor",
    description = "Suppresses intrusive popups, dialogs, and modal prompts, including 'Follow your friends' dialogs, contacts sync overlays, multi-account notification guides, 2SV security checkup modals, PopLayer promotional sheets, live stream teaser bubbles, sticker recommendations, and DM streak expiration warnings.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val suppressAccountPrompts by booleanOption(
        key = "suppressAccountPrompts",
        default = true,
        title = "Suppress Account & Permission Nags",
        description = "Suppresses 'Follow your friends' modals, 'Find contacts' Friends tab sync overlays, multi-account notification guides, and 'Security checkup 2SV' upsells.",
        required = false,
    )

    val suppressStickerRecommendations by booleanOption(
        key = "suppressStickerRecommendations",
        default = true,
        title = "Suppress Sticker Recommendations",
        description = "Disables personalized sticker suggestion popups and typing recommendations in direct messages.",
        required = false,
    )

    val filterPopLayerPrompts by booleanOption(
        key = "filterPopLayerPrompts",
        default = true,
        title = "Filter PopLayer Prompts & Nags",
        description = "Suppresses repetitive PopLayer prompts including favorites collection guides, launcher shortcut dialogs, repost newbie sheets, STEM feed prompts, campus education sheets, creator inbox guides, app review dialogs, marketing opt-ins, FYP surveys, CapCut/Lemon8 upsells, profile visitor prompts, and story intro sheets.",
        required = false,
    )

    val suppressLiveTeaserBubble by booleanOption(
        key = "suppressLiveTeaserBubble",
        default = true,
        title = "Suppress Live Teaser Bubbles",
        description = "Disables floating live stream preview teasers and popup windows from appearing over the video feed.",
        required = false,
    )

    val suppressStreakReminders by booleanOption(
        key = "suppressStreakReminders",
        default = true,
        title = "Suppress DM Streak Reminders",
        description = "Suppresses direct message streak expiration warning banners and inline urgency reminders.",
        required = false,
    )

    execute {
        var patched = 0

        if (suppressAccountPrompts == true) {
            patched += applyAccountPromptHooks()
        }

        if (suppressStickerRecommendations == true) {
            patched += applyStickerRecommendationHooks()
        }

        if (filterPopLayerPrompts == true) {
            patched += applyPopLayerFilterHook()
        }

        if (suppressLiveTeaserBubble == true) {
            patched += applyLiveTeaserBubbleHooks()
        }

        if (suppressStreakReminders == true) {
            patched += applyStreakReminderHook()
        }

        println("[Popups & Prompts Suppressor] Applied $patched popup and prompt suppression hook(s).")
    }
}

private fun BytecodePatchContext.applyAccountPromptHooks(): Int {
    var count = 0

    // 1. "Follow your friends" dialog
    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/recommend/RecUserPopupInMainActivityController;",
        name = "LIZLLL",
        returnType = "V",
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized RecUserPopupInMainActivityController.LIZLLL() -> Follow friends popup blocked.")
    count++

    // 2. "Get notifications from other accounts" multi-account push guide
    Fingerprint(
        definingClass = "LX/0YL4;",
        name = "LJII",
        returnType = "V",
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized LX/0YL4.LJII() -> Multi-account push guide dialog blocked.")
    count++

    // 3. "Find contacts" Friends tab overlay (PopLayer element)
    Fingerprint(
        definingClass = "LX/0v6A;",
        name = "canShow",
        returnType = "Z",
    ).method.replaceWithReturnBoolean(false)
    println("[Popups & Prompts Suppressor] Neutralized LX/0v6A.canShow() -> Find contacts PopLayer overlay blocked.")
    count++

    Fingerprint(
        definingClass = "LX/0v6A;",
        name = "LJII",
        returnType = "V",
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized LX/0v6A.LJII() -> Find contacts dialog inflation blocked.")
    count++

    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/relation/auth/pipeline/common/RelationAuthDialogControl;",
        name = "LJFF",
        returnType = "V",
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized RelationAuthDialogControl.LJFF() -> Relation auth trigger blocked.")
    count++

    Fingerprint(
        definingClass = "LX/0v5r;",
        name = "LIZIZ",
        parameters = listOf(
            "LX/1Gbn;",
            "[LX/0Se8;",
        ),
        returnType = "V",
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized LX/0v5r.LIZIZ() -> Find contacts access dialog pipeline blocked.")
    count++

    // 4. "Security checkup 2SV" upsell modal
    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/services/popsuite/local/LocalCampaignManager;",
        name = "showLocalCampaign",
        returnType = "Z",
    ).method.replaceWithReturnBoolean(false)
    println("[Popups & Prompts Suppressor] Neutralized LocalCampaignManager.showLocalCampaign() -> Local campaigns blocked.")
    count++

    val popSuiteMethod = Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/services/popsuite/PopSuiteManagerService;",
        name = "shouldShowPopSuitePopup",
        parameters = listOf("Ljava/lang/String;"),
        returnType = "Z",
    ).method
    popSuiteMethod.addInstructions(
        0,
        """
            invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_POPUP_HOOK}->shouldSuppressPopSuite(Ljava/lang/String;)Z
            move-result v0
            if-eqz v0, :cond_orig
            const/4 v0, 0
            return v0
            :cond_orig
        """.trimIndent(),
    )
    println("[Popups & Prompts Suppressor] Hooked PopSuiteManagerService.shouldShowPopSuitePopup() -> Generalized PopSuite suppression activated.")
    count++

    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/services/popsuite/local/Gpppa2svUpsellCampaign;",
        name = "startCampaign",
        parameters = listOf(
            "Lcom/ss/android/ugc/aweme/IPopSuiteManagerService\$PopupConfigObject;",
            "LX/0Ck6;",
        ),
        returnType = "Z",
    ).method.replaceWithReturnBoolean(false)
    println("[Popups & Prompts Suppressor] Neutralized Gpppa2svUpsellCampaign.startCampaign() -> GPPPA 2SV security checkup sheet blocked.")
    count++

    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/awemepushlib/manager/PushPermissionPopupManager;",
        name = "LJI",
        parameters = listOf(
            "Landroid/app/Activity;",
            "Ljava/lang/String;",
            "LX/1QQN;",
        ),
        returnType = "V",
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized PushPermissionPopupManager.LJI() -> Combined push permission popup blocked.")
    count++

    return count
}

private fun BytecodePatchContext.applyStickerRecommendationHooks(): Int {
    // ChatFeatureListConf.featureEnable(TYPING_RECOMMEND) -> false
    val featureEnableMethod = Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/im/strategy/businessconfig/ChatFeatureListConf;",
        name = "featureEnable",
        parameters = listOf("LX/0pdZ;"),
        returnType = "Z",
    ).method
    featureEnableMethod.addInstructions(
        0,
        """
            sget-object v0, LX/0pdZ;->TYPING_RECOMMEND:LX/0pdZ;
            if-ne p1, v0, :cond_orig
            const/4 v0, 0
            return v0
            :cond_orig
        """.trimIndent(),
    )
    println("[Popups & Prompts Suppressor] Hooked ChatFeatureListConf.featureEnable() -> Typing recommendations disabled.")
    return 1
}

private fun BytecodePatchContext.applyPopLayerFilterHook(): Int {
    var count = 0

    val method = Fingerprint(
        definingClass = "LX/07Q5;",
        name = "canShow",
        returnType = "Z",
    ).method
    method.addInstructions(
        0,
        """
            invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_POPUP_HOOK}->shouldSuppressPopLayer(Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0, :cond_allow
            const/4 v0, 0
            return v0
            :cond_allow
        """.trimIndent(),
    )
    println("[Popups & Prompts Suppressor] Hooked LX/07Q5.canShow() -> PopLayer semantic label filter activated.")
    count++

    Fingerprint(
        definingClass = "LX/0P2r;",
        name = "LIZ",
        returnType = "V",
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized LX/0P2r.LIZ() -> Profile view history turn-on sheet blocked.")
    count++

    Fingerprint(
        definingClass = "LX/0OOs;",
        name = "invoke",
        parameters = listOf("Ljava/lang/Object;"),
        returnType = "Ljava/lang/Object;",
    ).method.replaceWithReturnNull()
    println("[Popups & Prompts Suppressor] Neutralized LX/0OOs.invoke() -> Profile view history alternate sheet trigger blocked.")
    count++

    return count
}

private fun BytecodePatchContext.applyLiveTeaserBubbleHooks(): Int {
    var count = 0

    // 1. LiveBubbleUtil.LIZ(LiveBubbleData) -> return-void
    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/feed/util/LiveBubbleUtil;",
        name = "LIZ",
        parameters = listOf("Lcom/bytedance/android/livesdkapi/depend/model/live/bubble/LiveBubbleData;"),
        returnType = "V",
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized LiveBubbleUtil.LIZ() -> Floating live stream teaser bubble blocked.")
    count++

    // 2. LiveBubbleUtil.LJIIIIZZ() -> false
    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/feed/util/LiveBubbleUtil;",
        name = "LJIIIIZZ",
        returnType = "Z",
    ).method.replaceWithReturnBoolean(false)
    println("[Popups & Prompts Suppressor] Neutralized LiveBubbleUtil.LJIIIIZZ() -> Live bubble display check forced false.")
    count++

    return count
}

private fun BytecodePatchContext.applyStreakReminderHook(): Int {
    val method = Fingerprint(
        definingClass = "LX/0O2v;",
        name = "LIZ",
        parameters = listOf("Ljava/lang/String;", "LX/0BjM;"),
        returnType = "Ljava/lang/Object;",
    ).method
    method.addInstructions(
        0,
        """
            const-string v0, "has_streak_reminder_inline_msg"
            invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0, :cond_orig
            sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
            return-object v0
            :cond_orig
        """.trimIndent(),
    )
    println("[Popups & Prompts Suppressor] Hooked LX/0O2v.LIZ() -> DM streak expiration reminders blocked.")
    return 1
}
