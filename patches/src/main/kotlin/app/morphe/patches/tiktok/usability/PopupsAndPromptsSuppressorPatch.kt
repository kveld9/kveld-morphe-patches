package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags

private const val NEW_USER_JOURNEY_SERVICE = "Lcom/ss/android/ugc/aweme/NewUserJourneyService;"

val popupsAndPromptsSuppressorPatch = bytecodePatch(
    name = "Popups & Prompts Suppressor",
    description = "Blocks popups, nags and promo dialogs across the app.",
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

    val suppressPostDownloadDialog by booleanOption(
        key = "suppressPostDownloadDialog",
        default = false,
        title = "Disable Post-Download Share Dialog",
        description = "Suppresses the automatic 'Share to' and friend suggestions bottom sheet that pops up after finishing a download.",
        required = false,
    )

    val suppressUpdateNags by booleanOption(
        key = "suppressUpdateNags",
        default = true,
        title = "Update Prompt Suppressor",
        description = "Neutralizes background update polling tasks and device ID check routines to prevent forced update popups.",
        required = false,
    )

    val skipOnboarding by booleanOption(
        key = "skipOnboarding",
        default = true,
        title = "Skip First-Launch Onboarding",
        description = "Skips the first-launch intro, tutorials and consent screens.",
        required = false,
    )

    val skipContentWarnings by booleanOption(
        key = "skipContentWarnings",
        default = false,
        title = "Skip Content Warnings",
        description = "Bypasses and clears sensitive content warnings, graphic media blur overlays, and age gates on feed videos.",
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

        if (suppressPostDownloadDialog == true) {
            patched += applyPostDownloadDialogHook()
        }

        if (suppressUpdateNags == true) {
            patched += applyUpdateNagHooks()
        }

        if (skipOnboarding == true) {
            patched += applySkipOnboardingHooks()
        }

        if (skipContentWarnings == true) {
            patched += applySkipContentWarningsHook()
        }

        if (patched == 0) {
            println("[Popups & Prompts Suppressor] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Popups & Prompts Suppressor] Applied $patched popup and prompt suppression hook(s).")
        }
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
    var count = 0

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
    count++

    // Dynamically neutralizes the typing strip UI (catches shifted methods)
    val typingAssemClass = Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/im/sdk/chat/ui/base/assems/input/typingrecommendation/TypingRecommendationPanelAssem;"
    ).classDef

    var typingHooks = 0

    typingAssemClass.methods.filter {
        !AccessFlags.STATIC.isSet(it.accessFlags) &&
            it.parameterTypes.isEmpty() &&
            it.returnType == "Z" &&
            it.name != "<init>"
    }.forEach { method ->
        method.replaceWithReturnBoolean(false)
        typingHooks++
    }

    typingAssemClass.methods.filter {
        !AccessFlags.STATIC.isSet(it.accessFlags) &&
            it.parameterTypes.size == 1 &&
            it.returnType == "V" &&
            !it.name.startsWith("on") &&
            it.name != "<init>"
    }.forEach { method ->
        method.replaceWithReturnVoid()
        typingHooks++
    }

    if (typingHooks > 0) {
        println("[Popups & Prompts Suppressor] Hooked TypingRecommendationPanelAssem ($typingHooks dynamic methods) -> typing sticker UI blocked.")
        count++
    }

    return count
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

private fun BytecodePatchContext.applyPostDownloadDialogHook(): Int {
    val showFp = Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/internalshare/impl/fragment/DownloadAndShareFragment;",
        strings = listOf("after_video_saved_share_to_nscreen"),
        returnType = "V",
    )
    val showMethod = showFp.method
    showMethod.addInstructions(
        0,
        """
            return-void
        """.trimIndent(),
    )
    println("[Popups & Prompts Suppressor] Hooked DownloadAndShareFragment.${showMethod.name} -> Suppressed post-download popup.")
    return 1
}

private fun BytecodePatchContext.applyUpdateNagHooks(): Int {
    var count = 0

    // 1. CheckUpdateChangeDeviceIDTaskHolder$Background
    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/CheckUpdateChangeDeviceIDTaskHolder\$Background;",
        name = "run",
        returnType = "V",
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized CheckUpdateChangeDeviceIDTaskHolder\$Background.run().")
    count++

    // 2. CheckUpdateChangeDeviceIDTaskHolder$BootFinish
    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/CheckUpdateChangeDeviceIDTaskHolder\$BootFinish;",
        name = "run",
        returnType = "V",
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized CheckUpdateChangeDeviceIDTaskHolder\$BootFinish.run().")
    count++

    // 3. CheckUpdateChangeDeviceIDTask (cold startup update check)
    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/app/application/task/CheckUpdateChangeDeviceIDTask;",
        name = "run",
        returnType = "V",
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized CheckUpdateChangeDeviceIDTask.run().")
    count++

    return count
}

private fun BytecodePatchContext.applySkipOnboardingHooks(): Int {
    var count = 0

    // 1. NewUserJourneyService.LIZJ()Z -> return true (reports did_finish_nuj = true)
    Fingerprint(
        definingClass = NEW_USER_JOURNEY_SERVICE,
        returnType = "Z",
        strings = listOf("did_finish_nuj"),
    ).method.replaceWithReturnBoolean(true)
    println("[Popups & Prompts Suppressor] Forced NewUserJourneyService.LIZJ() -> Reported did_finish_nuj = true.")
    count++

    // 2. NewUserJourneyService.LJJJI(Landroid/app/Activity;)Z -> return false (never show NUJ)
    Fingerprint(
        definingClass = NEW_USER_JOURNEY_SERVICE,
        returnType = "Z",
        parameters = listOf("Landroid/app/Activity;"),
        strings = listOf("new_user_journey"),
    ).method.replaceWithReturnBoolean(false)
    println("[Popups & Prompts Suppressor] Neutralized NewUserJourneyService.LJJJI() -> Should show NUJ suppressed.")
    count++

    // 3. NewUserJourneyService.LJIJI(...)V -> return-void (suppress launching NUJ Activity)
    Fingerprint(
        definingClass = NEW_USER_JOURNEY_SERVICE,
        returnType = "V",
        strings = listOf("deeplink_intent_about_welcome_screen"),
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized NewUserJourneyService.LJIJI() -> Launch activity intent suppressed.")
    count++

    // 4. NewUserJourneyService.LJJJ(Landroid/app/Activity;, Landroid/content/Intent;)V -> return-void
    Fingerprint(
        definingClass = NEW_USER_JOURNEY_SERVICE,
        returnType = "V",
        parameters = listOf("Landroid/app/Activity;", "Landroid/content/Intent;"),
        strings = listOf("reorder_new_journey_front"),
    ).method.replaceWithReturnVoid()
    println("[Popups & Prompts Suppressor] Neutralized NewUserJourneyService.LJJJ() -> Reorder NUJ suppressed.")
    count++

    // 5. NewUserJourneyService.LJIJJLI(Lcom/bytedance/ies/foundation/activity/BaseActivity;)Z -> return false
    val candidates = Fingerprint(
        definingClass = NEW_USER_JOURNEY_SERVICE,
        returnType = "Z",
        parameters = listOf("Lcom/bytedance/ies/foundation/activity/BaseActivity;"),
    ).matchAll(classDefBy(NEW_USER_JOURNEY_SERVICE)).map { it.method }
    require(candidates.size == 1) {
        "Ambiguous LJIJJLI overloads: ${candidates.size}"
    }
    val method = candidates.first()
    method.replaceWithReturnBoolean(false)
    println("[Popups & Prompts Suppressor] Neutralized NewUserJourneyService.LJIJJLI() -> Container activity launch suppressed.")
    count++

    return count
}

private fun BytecodePatchContext.applySkipContentWarningsHook(): Int {
    val fp = Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoVM;",
        custom = { method, _ ->
            method.name == "paramSync2StateAccept" &&
                "Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;" in method.parameterTypes
        },
    )
    val method = try {
        fp.method
    } catch (e: Exception) {
        throw PatchException("Anchor VideoAuthorInfoVM.paramSync2StateAccept(VideoItemParams) not found for Skip Content Warnings", e)
    }

    val vipType = "Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"
    val paramTypeStrings = method.parameterTypes.map { it.toString() }
    val vipIndex = paramTypeStrings.indexOf(vipType)
    if (vipIndex < 0) throw PatchException("VideoItemParams parameter not found in VideoAuthorInfoVM.paramSync2StateAccept")
    var slot = if (AccessFlags.STATIC.isSet(method.accessFlags)) 0 else 1
    for (i in 0 until vipIndex) { slot += 1; val t = paramTypeStrings[i]; if (t == "J" || t == "D") slot += 1 }
    val paramReg = "p$slot"
    method.addInstructions(
        0,
        "invoke-static/range {$paramReg .. $paramReg}, ${Constants.TIKTOK_EXTENSION_SENSITIVE_WARNINGS_HOOK}->clear(Ljava/lang/Object;)V",
    )
    println("[Popups & Prompts Suppressor] Neutralized content warnings via VideoAuthorInfoVM.paramSync2StateAccept.")
    return 1
}
