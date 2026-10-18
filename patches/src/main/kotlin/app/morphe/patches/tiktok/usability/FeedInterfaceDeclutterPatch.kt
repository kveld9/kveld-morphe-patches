package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnInt
import app.morphe.patches.shared.replaceWithReturnIntegerObject
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val feedInterfaceDeclutterPatch = bytecodePatch(
    name = "Declutter Video Interface",
    description = "Hides buttons and overlays on videos.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val hideRepostBadge by booleanOption(
        key = "hideRepostBadge",
        default = true,
        title = "Repost badge",
        description = "Hides repost pill.",
        required = false,
    )

    val hideVideoDescriptions by booleanOption(
        key = "hideVideoDescriptions",
        default = true,
        title = "Descriptions",
        description = "Hides video text.",
        required = false,
    )

    val hideAvatarFollowButton by booleanOption(
        key = "hideAvatarFollowButton",
        default = true,
        title = "Follow badge",
        description = "Hides + button on avatars.",
        required = false,
    )

    val disableStoryRings by booleanOption(
        key = "disableStoryRings",
        default = true,
        title = "Story rings",
        description = "Removes story rings.",
        required = false,
    )

    val hidePlaylistBar by booleanOption(
        key = "hidePlaylistBar",
        default = true,
        title = "Playlist bar",
        description = "Hides playlist bar.",
        required = false,
    )

    val hideSaveButton by booleanOption(
        key = "hideSaveButton",
        default = false,
        title = "Save button",
        description = "Hides bookmark button.",
        required = false,
    )

    val hideMusicCover by booleanOption(
        key = "hideMusicCover",
        default = false,
        title = "Music disc",
        description = "Hides spinning music disc.",
        required = false,
    )

    val hideFullscreenButton by booleanOption(
        key = "hideFullscreenButton",
        default = false,
        title = "Fullscreen button",
        description = "Hides fullscreen button.",
        required = false,
    )

    val hideFeedbackButtons by booleanOption(
        key = "hideFeedbackButtons",
        default = true,
        title = "Feedback pills",
        description = "Hides interest pills.",
        required = false,
    )

    val disableDoubleTapLike by booleanOption(
        key = "disableDoubleTapLike",
        default = true,
        title = "Disable Double Tap to Like",
        description = "Disables the double tap gesture to like videos in the feed, preventing accidental likes while scrolling or pausing. Videos can still be liked using the like button.",
        required = false,
    )

    val disableAvatarLiveStatus by booleanOption(
        key = "disableAvatarLiveStatus",
        default = true,
        title = "Disable Profile Photo LIVE Status",
        description = "Removes the LIVE ring on avatars and keeps taps on the profile.",
        required = false,
    )

    val disableLikeRepost by booleanOption(
        key = "disableLikeRepost",
        title = "Disable Long-Press Like (Repost)",
        description = "Prevents holding the Like button on feed videos from opening the Repost action panel. Turns off long-press actions like repost, quick DM and quick emoji.",
        default = true,
        required = false,
    )

    val disableShareQuickDms by booleanOption(
        key = "disableShareQuickDms",
        title = "Disable Long-Press Share (Quick DMs)",
        description = "Prevents holding the Share button on feed videos from opening the quick share recent contacts tray. Turns off long-press actions like repost, quick DM and quick emoji.",
        default = true,
        required = false,
    )

    val disableCommentReactions by booleanOption(
        key = "disableCommentReactions",
        title = "Disable Long-Press Comment (Quick Emojis)",
        description = "Prevents holding the Comment button on feed videos from opening the quick reaction emojis picker. Turns off long-press actions like repost, quick DM and quick emoji.",
        default = true,
        required = false,
    )

    val longPressVideo by stringOption(
        key = "longPressVideo",
        title = "Long Press Video Action",
        description = "Action when long pressing feed video body: 'nothing' (default, suppresses menu/repost), 'comments' (opens comments), 'copyLink' (copies link to clipboard), or 'saveSound' (favorites/saves audio).",
        default = "nothing",
        values = mapOf("Do nothing" to "nothing", "Open comments" to "comments", "Copy link" to "copyLink", "Save sound" to "saveSound"),
        required = false,
    )

    execute {
        if (hideRepostBadge != true &&
            hideVideoDescriptions != true &&
            hideAvatarFollowButton != true &&
            disableStoryRings != true &&
            hidePlaylistBar != true &&
            hideSaveButton != true &&
            hideMusicCover != true &&
            hideFullscreenButton != true &&
            hideFeedbackButtons != true &&
            disableDoubleTapLike != true &&
            disableAvatarLiveStatus != true &&
            disableLikeRepost != true &&
            disableShareQuickDms != true &&
            disableCommentReactions != true &&
            (longPressVideo == null || longPressVideo == "nothing")
        ) {
            println("[Declutter Video Interface] Skipped: All declutter options are disabled.")
            return@execute
        }

        var patched = 0

        // 1. Hide Repost Badge ('Shared by' / 'Reposted')
        if (hideRepostBadge == true) {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/feed/platform/cell/interact/info/UpvoteVideoTrigger;",
                name = "yr",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked UpvoteVideoTrigger.yr() -> return false.")
            patched++

            val assemClass = "Lcom/ss/android/ugc/aweme/upvote/detail/whitebar/UpvoteVideoAssemNew;"

            Fingerprint(
                definingClass = assemClass,
                name = "tr",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.tr() -> return false.")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "hb",
                returnType = "Z",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.hb() -> return false.")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "LLLLIILL",
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.LLLLIILL() -> return-void.")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "LLILZ",
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.LLILZ() -> return-void.")
            patched++

            val onViewCreatedMethod = Fingerprint(
                definingClass = assemClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method
            onViewCreatedMethod.clearTryBlocks()
            onViewCreatedMethod.ensureRegisterCount(2)
            val instrCount = onViewCreatedMethod.implementation!!.instructions.count()
            onViewCreatedMethod.removeInstructions(0, instrCount)
            onViewCreatedMethod.addInstructions(
                0,
                """
                    const/16 v0, 0x8
                    invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                    return-void
                """.trimIndent(),
            )
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.onViewCreated() -> setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.z4() -> return-void.")
            patched++
        }

        // 2. Hide Video Descriptions
        if (hideVideoDescriptions == true) {
            val descTargetClass = "Lcom/ss/android/ugc/aweme/feed/assem/desc/VideoDescAssem;"

            val descOnViewCreated = Fingerprint(
                definingClass = descTargetClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method
            descOnViewCreated.clearTryBlocks()
            descOnViewCreated.ensureRegisterCount(4)
            val descReturns = descOnViewCreated.implementation!!.instructions.mapIndexedNotNull { index, instr ->
                if (instr.opcode.name == "return-void") index else null
            }.reversed()
            for ((i, index) in descReturns.withIndex()) {
                descOnViewCreated.addInstructions(
                    index,
                    """
                        move-object/from16 v0, p0
                        invoke-static {v0}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->hideFeedDescForAssem(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            println("[Declutter Video Interface] Hooked VideoDescAssem.onViewCreated() -> appended setVisibility(GONE).")
            patched++

            val descJs = Fingerprint(
                definingClass = descTargetClass,
                name = "js",
                returnType = "V",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method
            descJs.clearTryBlocks()
            descJs.ensureRegisterCount(2)
            val jsReturns = descJs.implementation!!.instructions.mapIndexedNotNull { index, instr ->
                if (instr.opcode.name == "return-void") index else null
            }.reversed()
            for ((i, index) in jsReturns.withIndex()) {
                descJs.addInstructions(
                    index,
                    """
                        move-object/from16 v0, p0
                        invoke-static {v0}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->hideFeedDescForAssem(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            println("[Declutter Video Interface] Hooked VideoDescAssem.js() -> appended GONE enforcement.")
            patched++

            // 2b. Redirect feed desc visibility calls to always-GONE wrappers
            // (reactive-proof hiding without touching method bodies).
            // LX/09Ce is instantiated 1:1 by VideoDescAssem.onViewCreated, and
            // VideoDescAssem itself manages desc container visibility. Neither
            // VideoDescAssem nor 09Ce ever calls setVisibility directly: every desc
            // visibility change flows through the LX/00kn visibility helpers below.
            // Redirecting those calls (same invoke shape and width, only the callee changes)
            // across both VideoDescAssem and LX/09Ce to extension wrappers that force GONE
            // covers every current and future show path, including see-more expansion flows,
            // with no control-flow disturbance.
            val descVisibilityRedirects = mapOf(
                "LLLLLZIL" to "${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->forceGoneViewLast(Landroid/view/View;I)V",
                "LLLLLZ" to "${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->forceGoneViewLast(Landroid/view/View;I)V",
                "LJLLLLLL" to "${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->forceGoneViewFirst(ILandroid/view/View;)V",
                "LJLLILLLL" to "${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->forceGoneViewFirst(ILandroid/view/View;)V",
            )
            var redirectedMethods = 0
            var redirectedCallsDescAssem = 0
            var redirectedCalls09Ce = 0
            var skippedShapes = 0
            val descRedirectClasses = listOf(descTargetClass, "LX/09Ce;")
            for (targetClass in descRedirectClasses) {
                var classRedirectedCalls = 0
                for (target in mutableClassDefBy(targetClass).methods) {
                    val instructions = target.implementation?.instructions ?: continue
                    val redirectIndices = mutableListOf<Pair<Int, String>>()
                    for ((index, instruction) in instructions.withIndex()) {
                        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                        if (reference.definingClass != "LX/00kn;") continue
                        if (reference.name !in descVisibilityRedirects) continue
                        val newCallee = descVisibilityRedirects[reference.name] ?: continue
                        val registers = extractInstructionRegisters(instruction)
                        if (registers == null || registers.size != 2) {
                            skippedShapes++
                            continue
                        }
                        val newInvoke = if (instruction is RegisterRangeInstruction) {
                            "invoke-static/range {v${registers[0]} .. v${registers[1]}}, $newCallee"
                        } else {
                            "invoke-static {v${registers[0]}, v${registers[1]}}, $newCallee"
                        }
                        redirectIndices.add(index to newInvoke)
                    }
                    for ((index, newInvoke) in redirectIndices.sortedByDescending { it.first }) {
                        target.replaceInstructions(index, newInvoke)
                        classRedirectedCalls++
                    }
                    if (redirectIndices.isNotEmpty()) {
                        redirectedMethods++
                    }
                }
                if (targetClass == descTargetClass) {
                    redirectedCallsDescAssem = classRedirectedCalls
                } else {
                    redirectedCalls09Ce = classRedirectedCalls
                }
            }
            val redirectedCalls = redirectedCallsDescAssem + redirectedCalls09Ce
            if (redirectedCallsDescAssem == 0) {
                println("[Declutter Video Interface] WARNING: VideoDescAssem contributed 0 redirects")
            }
            require(redirectedCalls > 0) { "VideoDescAssem/09Ce desc redirect: no visibility helper call found" }
            println("[Declutter Video Interface] Hooked desc visibility calls -> $redirectedCalls call(s) (VideoDescAssem: $redirectedCallsDescAssem, LX/09Ce: $redirectedCalls09Ce, skippedShapes: $skippedShapes) across $redirectedMethods method(s) forced GONE.")
            patched++

            // 2c. Direct prologue guards on LX/00kn visibility helpers
            val descHelpers = listOf(
                Triple("LLLLLZIL", listOf("Lcom/bytedance/tux/input/TuxTextView;", "I"), "p0"),
                Triple("LLLLLZ", listOf("LX/09BJ;", "I"), "p0"),
                Triple("LJLLLLLL", listOf("I", "Landroid/widget/LinearLayout;"), "p1"),
                Triple("LJLLILLLL", listOf("I", "Landroid/view/View;"), "p1"),
            )
            var installedHelpers = 0
            for ((name, params, viewReg) in descHelpers) {
                try {
                    val method = Fingerprint(
                        definingClass = "LX/00kn;",
                        name = name,
                        returnType = "V",
                        parameters = params,
                    ).method
                    method.addInstructions(
                        0,
                        "invoke-static {$viewReg}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->hideDescViewIfDesc(Landroid/view/View;)V",
                    )
                    println("[Declutter Video Interface] Hooked LX/00kn.$name() prologue -> hideDescViewIfDesc($viewReg).")
                    installedHelpers++
                } catch (e: Exception) {
                    println("[Declutter Video Interface] WARNING: helper $name signature not found, skipped")
                }
            }
            println("[Declutter Video Interface] Installed desc prologue guards on $installedHelpers / ${descHelpers.size} helper(s).")
            require(installedHelpers == descHelpers.size) { "LX/00kn desc visibility helpers: expected ${descHelpers.size}, installed $installedHelpers" }
            patched++

            val friendsDescClass = "Lcom/ss/android/ugc/aweme/friendstab/ui/feed/cell/component/desc/FriendsV3DescAssem;"

            Fingerprint(
                definingClass = friendsDescClass,
                name = "tr",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked FriendsV3DescAssem.tr() -> return false.")
            patched++

            Fingerprint(
                definingClass = friendsDescClass,
                name = "hb",
                returnType = "Z",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked FriendsV3DescAssem.hb() -> return false.")
            patched++

            val friendsOnViewCreated = Fingerprint(
                definingClass = friendsDescClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method
            friendsOnViewCreated.clearTryBlocks()
            friendsOnViewCreated.ensureRegisterCount(2)
            val friendsReturns = friendsOnViewCreated.implementation!!.instructions.mapIndexedNotNull { index, instr ->
                if (instr.opcode.name == "return-void") index else null
            }.reversed()
            for (index in friendsReturns) {
                friendsOnViewCreated.addInstructions(
                    index,
                    """
                        move-object/from16 v0, p1
                        const/16 v1, 0x8
                        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                    """.trimIndent(),
                )
            }
            println("[Declutter Video Interface] Hooked FriendsV3DescAssem.onViewCreated() -> appended setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = friendsDescClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked FriendsV3DescAssem.z4() -> return-void.")
            patched++

            Fingerprint(
                definingClass = friendsDescClass,
                name = "js",
                returnType = "V",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked FriendsV3DescAssem.js() -> return-void.")
            patched++

            val translationClasses = listOf(
                "Lcom/ss/android/ugc/aweme/translation/ui/TranslationControlsAssem;",
                "Lcom/ss/android/ugc/aweme/translation/ui/TranslationStatusAssem;",
            )

            for (transClass in translationClasses) {
                val transOnViewCreated = Fingerprint(
                    definingClass = transClass,
                    name = "onViewCreated",
                    returnType = "V",
                    parameters = listOf("Landroid/view/View;"),
                ).method
                transOnViewCreated.clearTryBlocks()
                transOnViewCreated.ensureRegisterCount(2)
                val transReturns = transOnViewCreated.implementation!!.instructions.mapIndexedNotNull { index, instr ->
                    if (instr.opcode.name == "return-void") index else null
                }.reversed()
                for (index in transReturns) {
                    transOnViewCreated.addInstructions(
                        index,
                        """
                            move-object/from16 v0, p1
                            const/16 v1, 0x8
                            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                        """.trimIndent(),
                    )
                }
                println("[Declutter Video Interface] Hooked $transClass onViewCreated() -> appended setVisibility(GONE).")
                patched++

                Fingerprint(
                    definingClass = transClass,
                    name = "z4",
                    returnType = "V",
                    parameters = listOf("Ljava/lang/Object;"),
                ).method.replaceWithReturnVoid()
                println("[Declutter Video Interface] Hooked $transClass z4() -> return-void.")
                patched++
            }
        }

        // 3. Hide Profile Photo Follow Button
        if (hideAvatarFollowButton == true) {
            val targetClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarDefaultAssem;"

            val qrMethod = Fingerprint(
                definingClass = targetClass,
                custom = { m, _ ->
                    m.parameterTypes.size == 3 &&
                        m.parameterTypes[0] == "Landroid/view/ViewGroup;" &&
                        m.parameterTypes[1] == "I" &&
                        m.returnType == "V"
                },
            ).method

            qrMethod.clearTryBlocks()
            val count = qrMethod.implementation!!.instructions.count()
            qrMethod.removeInstructions(0, count)
            qrMethod.addInstructions(
                0,
                """
                    invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->hideFollowButton(Landroid/view/View;)V
                    return-void
                """.trimIndent(),
            )
            println("[Declutter Video Interface] Hooked FeedAvatarDefaultAssem.${qrMethod.name}() -> Permanently GONE (0x8) and non-clickable.")
            patched++

            val onViewCreatedMethod = Fingerprint(
                definingClass = targetClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method

            val instructions = onViewCreatedMethod.implementation!!.instructions
            val iputIndex = instructions.indexOfFirst {
                val refStr = (it as? ReferenceInstruction)?.reference?.toString() ?: ""
                refStr.contains("FeedAvatarDefaultAssem;->") && refStr.contains(":Landroid/view/ViewGroup;")
            }

            if (iputIndex != -1) {
                val regA = (instructions[iputIndex] as TwoRegisterInstruction).registerA
                onViewCreatedMethod.addInstructions(
                    iputIndex + 1,
                    """
                        invoke-static/range {v$regA .. v$regA}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->hideFollowButton(Landroid/view/View;)V
                    """.trimIndent(),
                )
                println("[Declutter Video Interface] Hooked FeedAvatarDefaultAssem.onViewCreated() -> Immediate GONE initialization on v$regA.")
                patched++
            }
        }

        // 4. Disable Story Feed Indicators
        if (disableStoryRings == true) {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/profile/model/User;",
                name = "getStoryStatus",
                returnType = "I",
            ).method.replaceWithReturnInt(0)
            println("[Declutter Video Interface] Hooked User.getStoryStatus() -> 0.")
            patched++

            val socialPublishClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarSocialPublishAssem;"
            Fingerprint(
                definingClass = socialPublishClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked FeedAvatarSocialPublishAssem.onViewCreated() -> return-void.")
            patched++

            Fingerprint(
                definingClass = socialPublishClass,
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked FeedAvatarSocialPublishAssem.(Object)V -> return-void.")
            patched++

            Fingerprint(
                definingClass = socialPublishClass,
                returnType = "V",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked FeedAvatarSocialPublishAssem.(VideoItemParams)V -> return-void.")
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/service/SocPubDistributeServiceImpl;",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/profile/model/User;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked SocPubDistributeServiceImpl.(User)Z -> false.")
            patched++
        }

        // 5. Hide Playlist Bottom Bar
        if (hidePlaylistBar == true) {
            val triggerClass = "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/PlayListBottomBarAssemTrigger;"
            Fingerprint(
                definingClass = triggerClass,
                name = "yr",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked PlayListBottomBarAssemTrigger.yr() -> return false.")
            patched++

            val bottomBarClass = "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/InteractPlayListBottomBarAssem;"
            val bottomBarOnViewCreated = Fingerprint(
                definingClass = bottomBarClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method
            bottomBarOnViewCreated.clearTryBlocks()
            bottomBarOnViewCreated.ensureRegisterCount(2)
            val count = bottomBarOnViewCreated.implementation!!.instructions.count()
            bottomBarOnViewCreated.removeInstructions(0, count)
            bottomBarOnViewCreated.addInstructions(
                0,
                """
                    invoke-super {p0, p1}, Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/banner/InteractBottomBannerAssem;->onViewCreated(Landroid/view/View;)V
                    const/16 v0, 0x8
                    invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                    return-void
                """.trimIndent(),
            )
            println("[Declutter Video Interface] Hooked InteractPlayListBottomBarAssem.onViewCreated() -> setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = bottomBarClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked InteractPlayListBottomBarAssem.z4() -> return-void.")
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getPlaylist_info",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/PlayListInfo;",
            ).method.replaceWithReturnNull()
            println("[Declutter Video Interface] Hooked Aweme.getPlaylist_info() -> return null.")
            patched++
        }

        // 6. Hide Save Button (Favorite/Bookmark)
        if (hideSaveButton == true) {
            val favClass = "Lcom/ss/android/ugc/aweme/feed/favorite/VideoFavoriteAssem;"

            val favOnViewCreated = Fingerprint(
                definingClass = favClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method
            favOnViewCreated.clearTryBlocks()
            favOnViewCreated.ensureRegisterCount(2)
            val count = favOnViewCreated.implementation!!.instructions.count()
            favOnViewCreated.removeInstructions(0, count)
            favOnViewCreated.addInstructions(
                0,
                """
                    invoke-super {p0, p1}, Lcom/ss/android/ugc/feed/platform/cell/BaseCellSlotComponent;->onViewCreated(Landroid/view/View;)V
                    const/16 v0, 0x8
                    invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                    return-void
                """.trimIndent(),
            )
            println("[Declutter Video Interface] Hooked VideoFavoriteAssem.onViewCreated() -> setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = favClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked VideoFavoriteAssem.z4() -> return-void.")
            patched++
        }

        // 7. Hide Music Cover Disc
        if (hideMusicCover == true) {
            val musicCoverClass = "Lcom/ss/android/ugc/aweme/feed/assem/music/VideoMusicCoverAssem;"

            val coverOnViewCreated = Fingerprint(
                definingClass = musicCoverClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method
            coverOnViewCreated.clearTryBlocks()
            coverOnViewCreated.ensureRegisterCount(4)
            val count = coverOnViewCreated.implementation!!.instructions.count()
            coverOnViewCreated.removeInstructions(0, count)
            coverOnViewCreated.addInstructions(
                0,
                """
                    invoke-super/range {p0 .. p1}, Lcom/ss/android/ugc/feed/platform/cell/BaseCellSlotComponent;->onViewCreated(Landroid/view/View;)V
                    move-object/from16 v1, p1
                    const/16 v0, 0x8
                    invoke-virtual {v1, v0}, Landroid/view/View;->setVisibility(I)V
                    return-void
                """.trimIndent(),
            )
            println("[Declutter Video Interface] Hooked VideoMusicCoverAssem.onViewCreated() -> setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = musicCoverClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked VideoMusicCoverAssem.z4() -> return-void.")
            patched++

            Fingerprint(
                definingClass = musicCoverClass,
                name = "Tr",
                returnType = "V",
                parameters = emptyList(),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked VideoMusicCoverAssem.Tr() -> return-void.")
            patched++

            Fingerprint(
                definingClass = musicCoverClass,
                name = "Wr",
                returnType = "V",
                parameters = emptyList(),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked VideoMusicCoverAssem.Wr() -> return-void.")
            patched++
        }

        // 8. Hide Full Screen Button
        if (hideFullscreenButton == true) {
            val landscapeClass = "Lcom/ss/android/ugc/aweme/feed/landscape/LandscapeEntranceAssem;"

            Fingerprint(
                definingClass = landscapeClass,
                name = "Kr",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.Kr() -> return false.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "Aa",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.Aa() -> return false.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "bf",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.bf() -> return false.")
            patched++

            val entranceOnViewCreated = Fingerprint(
                definingClass = landscapeClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method
            entranceOnViewCreated.clearTryBlocks()
            entranceOnViewCreated.ensureRegisterCount(2)
            val count = entranceOnViewCreated.implementation!!.instructions.count()
            entranceOnViewCreated.removeInstructions(0, count)
            entranceOnViewCreated.addInstructions(
                0,
                """
                    invoke-super {p0, p1}, Lcom/ss/android/ugc/feed/platform/cell/BaseCellSlotComponent;->onViewCreated(Landroid/view/View;)V
                    const/16 v0, 0x8
                    invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                    return-void
                """.trimIndent(),
            )
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.onViewCreated() -> setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.z4() -> return-void.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "LLLLIILL",
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.LLLLIILL() -> return-void.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "LLILZ",
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.LLILZ() -> return-void.")
            patched++
        }

        // 9. Hide Feedback Buttons ('Not interested' / 'Interested')
        if (hideFeedbackButtons == true) {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/earlyfeedback/EarlyFeedbackButtonTrigger;",
                name = "yr",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked EarlyFeedbackButtonTrigger.yr() -> return false.")
            patched++

            val feedbackAssems = listOf(
                "Lcom/ss/android/ugc/aweme/feed/assem/earlyfeedback/EarlyFeedbackButtonAssem;",
                "Lcom/ss/android/ugc/aweme/feed/assem/earlyfeedback/EarlyFeedbackStandardButtonAssem;",
            )

            for (assemClass in feedbackAssems) {
                val feedbackOnViewCreated = Fingerprint(
                    definingClass = assemClass,
                    name = "onViewCreated",
                    returnType = "V",
                    parameters = listOf("Landroid/view/View;"),
                ).method
                feedbackOnViewCreated.clearTryBlocks()
                feedbackOnViewCreated.ensureRegisterCount(2)
                val feedbackCount = feedbackOnViewCreated.implementation!!.instructions.count()
                feedbackOnViewCreated.removeInstructions(0, feedbackCount)
                feedbackOnViewCreated.addInstructions(
                    0,
                    """
                        const/16 v0, 0x8
                        invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                        return-void
                    """.trimIndent(),
                )
                println("[Declutter Video Interface] Hooked $assemClass onViewCreated() -> setVisibility(GONE).")
                patched++

                Fingerprint(
                    definingClass = assemClass,
                    name = "z4",
                    returnType = "V",
                    parameters = listOf("Ljava/lang/Object;"),
                ).method.replaceWithReturnVoid()
                println("[Declutter Video Interface] Hooked $assemClass z4() -> return-void.")
                patched++
            }

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/earlyfeedback/EarlyFeedbackStandardButtonAssem;",
                name = "Mr",
                returnType = "V",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked EarlyFeedbackStandardButtonAssem.Mr() -> return-void.")
            patched++
        }

        // 10. Disable Double Tap to Like
        if (disableDoubleTapLike == true) {
            // Hook DiggPanelComponent.handleDoubleClick(MotionEvent) in main feed
            try {
                val method = Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/feed/platform/panel/digg/DiggPanelComponent;",
                    name = "handleDoubleClick",
                    parameters = listOf("Landroid/view/MotionEvent;"),
                    returnType = "V",
                ).method
                method.replaceWithReturnVoid()
                println("[Declutter Video Interface] Hooked DiggPanelComponent.handleDoubleClick -> Main feed double tap like neutralized.")
                patched++
            } catch (e: Exception) {
                println("[Declutter Video Interface] DiggPanelComponent note: ${e.message}")
            }

            // Hook LandscapeFragmentPanel.handleDoubleClick(MotionEvent) in landscape feed
            try {
                val method = Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/feed/landscape/LandscapeFragmentPanel;",
                    name = "handleDoubleClick",
                    parameters = listOf("Landroid/view/MotionEvent;"),
                    returnType = "V",
                ).method
                method.replaceWithReturnVoid()
                println("[Declutter Video Interface] Hooked LandscapeFragmentPanel.handleDoubleClick -> Landscape feed double tap like neutralized.")
                patched++
            } catch (e: Exception) {
                println("[Declutter Video Interface] LandscapeFragmentPanel note: ${e.message}")
            }

            // Hook FriendsV3GestureDetectorAssem.onDoubleTap(MotionEvent) in friends tab feed
            try {
                val method = Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/friendstab/ui/feed/cell/component/base/FriendsV3GestureDetectorAssem;",
                    name = "onDoubleTap",
                    parameters = listOf("Landroid/view/MotionEvent;"),
                    returnType = "V",
                ).method
                method.replaceWithReturnVoid()
                println("[Declutter Video Interface] Hooked FriendsV3GestureDetectorAssem.onDoubleTap -> Friends tab double tap like neutralized.")
                patched++
            } catch (e: Exception) {
                println("[Declutter Video Interface] FriendsV3GestureDetectorAssem note: ${e.message}")
            }
        }

        // 11. Disable Profile Photo LIVE Status
        if (disableAvatarLiveStatus == true) {
            val wrapClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarAssemWrap;"
            val liveAssemClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarLiveAssem;"

            // Discover and hook the author LIVE status validator (e.g. X.09A7.LIZIZ(Aweme, User)Z)
            var liveCheckClass = "LX/09A7;"
            var liveCheckMethod = "LIZIZ"

            val wrapFp = Fingerprint(definingClass = wrapClass)
            for (m in wrapFp.classDef.methods) {
                val insns = m.implementation?.instructions ?: continue
                for (insn in insns) {
                    val ref = (insn as? ReferenceInstruction)?.reference?.toString() ?: continue
                    if (ref.contains("(Lcom/ss/android/ugc/aweme/feed/model/Aweme;Lcom/ss/android/ugc/aweme/profile/model/User;)Z")) {
                        val parts = ref.split("->")
                        if (parts.size == 2) {
                            liveCheckClass = parts[0]
                            liveCheckMethod = parts[1].substringBefore("(")
                            break
                        }
                    }
                }
                if (liveCheckClass != "LX/09A7;") break
            }

            Fingerprint(
                definingClass = liveCheckClass,
                name = liveCheckMethod,
                returnType = "Z",
                parameters = listOf(
                    "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                    "Lcom/ss/android/ugc/aweme/profile/model/User;",
                ),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked $liveCheckClass->$liveCheckMethod() -> false (all authors treated as non-live).")
            patched++

            // Hook FeedAvatarAssemWrap.Yr()Z -> return false (prevents attaching FeedAvatarLiveAssem)
            Fingerprint(
                definingClass = wrapClass,
                returnType = "Z",
                parameters = emptyList(),
                custom = { m, _ ->
                    m.implementation?.instructions?.any {
                        (it as? ReferenceInstruction)?.reference?.toString()?.contains("getCurrentProfileUserRoomId") == true
                    } ?: false
                },
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked FeedAvatarAssemWrap room check -> false.")
            patched++

            // Hook FeedAvatarLiveAssem.ur()Z -> return false
            Fingerprint(
                definingClass = liveAssemClass,
                returnType = "Z",
                parameters = emptyList(),
                custom = { m, _ ->
                    m.implementation?.instructions?.any {
                        (it as? ReferenceInstruction)?.reference?.toString()?.contains("getCurrentProfileUserRoomId") == true
                    } ?: false
                },
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked FeedAvatarLiveAssem.ur() -> false.")
            patched++

            // Hook FeedAvatarLiveAssem.Ar(ZZ)V -> return-void (suppresses live entrance animations)
            Fingerprint(
                definingClass = liveAssemClass,
                returnType = "V",
                parameters = listOf("Z", "Z"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked FeedAvatarLiveAssem.Ar(ZZ) -> return-void.")
            patched++

            // Hook FeedAvatarLiveAssem.onBind/z4(Object)V -> return-void (suppresses live UI binding and click redirection)
            Fingerprint(
                definingClass = liveAssemClass,
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked FeedAvatarLiveAssem.(Object)V -> return-void.")
            patched++
        }

        // 12. Disable Feed Long-Press Actions
        val lpMode = longPressVideo ?: "nothing"

        // 12a. Disable or redirect long-press Like (Repost)
        if (disableLikeRepost != false || lpMode != "nothing") {
            val likeRepostFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/digg/VideoDiggAssem;",
                returnType = "Z",
                parameters = listOf("Landroid/view/View;"),
                strings = listOf(
                    "Long press detected on digg button for aweme: ",
                    "long_press_like_panel",
                ),
            )
            if (lpMode != "nothing") {
                likeRepostFingerprint.method.addInstructions(
                    0,
                    """
                        const-string v0, "$lpMode"
                        invoke-static/range {p1 .. p1}, ${Constants.TIKTOK_EXTENSION_GESTURE_HOOK}->handleLongPressVideoAction(Ljava/lang/Object;Ljava/lang/String;)Z
                        move-result v0
                        return v0
                    """.trimIndent(),
                )
                println("[Declutter Video Interface] Hooked VideoDiggAssem.Sr() -> redirected long-press to $lpMode.")
            } else {
                likeRepostFingerprint.method.replaceWithReturnBoolean(true)
                println("[Declutter Video Interface] Hooked VideoDiggAssem.Sr() -> consumed long-press, like repost disabled.")
            }
            patched++
        }

        // 12b. Disable or redirect long-press Share (Quick DMs)
        if (disableShareQuickDms != false || lpMode != "nothing") {
            val shareConfigMethod = Fingerprint(
                returnType = "Ljava/lang/Object;",
                parameters = emptyList(),
                strings = listOf("im_long_press_share_button_to_quick_share"),
            ).method
            shareConfigMethod.replaceWithReturnIntegerObject(0)
            println("[Declutter Video Interface] Hooked long-press quick share config lambda -> returns 0.")
            patched++

            val shareTriggerFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/unreadshare/ShareUnreadVideoQuickDMTrigger;",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            )
            if (lpMode != "nothing") {
                shareTriggerFingerprint.method.addInstructions(
                    0,
                    """
                        const-string v0, "$lpMode"
                        invoke-static/range {p1 .. p1}, ${Constants.TIKTOK_EXTENSION_GESTURE_HOOK}->handleLongPressVideoParams(Ljava/lang/Object;Ljava/lang/String;)V
                        const/4 v0, 0x0
                        return v0
                    """.trimIndent(),
                )
                println("[Declutter Video Interface] Hooked ShareUnreadVideoQuickDMTrigger trigger check -> redirected to $lpMode.")
            } else {
                shareTriggerFingerprint.method.replaceWithReturnBoolean(false)
                println("[Declutter Video Interface] Hooked ShareUnreadVideoQuickDMTrigger trigger check -> return false.")
            }
            patched++
        }

        // 12c. Disable long-press Comment (Quick Emojis)
        if (disableCommentReactions != false || lpMode != "nothing") {
            val commentConfigMethod = Fingerprint(
                returnType = "Ljava/lang/Object;",
                parameters = emptyList(),
                strings = listOf("long_press_quick_comment"),
            ).method
            commentConfigMethod.replaceWithReturnIntegerObject(0)
            println("[Declutter Video Interface] Hooked long-press quick comment config lambda -> returns 0.")
            patched++
        }

        println("[Declutter Video Interface] Successfully applied $patched hook(s) across selected feed interface options.")
    }
}

private fun extractInstructionRegisters(instruction: Instruction): List<Int>? {
    return when (instruction) {
        is FiveRegisterInstruction -> listOf(
            instruction.registerC,
            instruction.registerD,
            instruction.registerE,
            instruction.registerF,
            instruction.registerG,
        ).take(instruction.registerCount)
        is RegisterRangeInstruction -> (instruction.startRegister until instruction.startRegister + instruction.registerCount).toList()
        else -> null
    }
}

