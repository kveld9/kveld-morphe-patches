package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstructions
import app.morphe.patcher.patch.BytecodePatchContext
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
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val feedInterfaceDeclutterPatch = bytecodePatch(
    name = "Declutter Video Interface",
    description = "Cleans up the buttons and overlays on top of videos.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_LITE)
    dependsOn(sharedExtensionPatch)

    val hideRepostBadge by booleanOption(
        key = "hideRepostBadge",
        default = true,
        title = "Hide Repost Badge",
        description = "Hides the repost and shared-by pill badge ('Shared by' / 'Reposted') above creator details on feed videos.",
        required = false,
    )

    val hideVideoDescriptions by booleanOption(
        key = "hideVideoDescriptions",
        default = true,
        title = "Hide Video Descriptions",
        description = "Hides video descriptions, captions, and hashtags across feed videos while keeping creator and author title intact.",
        required = false,
    )

    val hideAvatarFollowButton by booleanOption(
        key = "hideAvatarFollowButton",
        default = true,
        title = "Hide Profile Photo Follow Button",
        description = "Hides the plus (+) follow badge on creator profile avatars in the feed and disables its touch interaction.",
        required = false,
    )

    val disableStoryRings by booleanOption(
        key = "disableStoryRings",
        default = true,
        title = "Disable Story Feed Indicators",
        description = "Removes creator profile photo story rings from feed videos, ensuring avatar photos remain clean without blue story rings.",
        required = false,
    )

    val hidePlaylistBar by booleanOption(
        key = "hidePlaylistBar",
        default = true,
        title = "Hide Playlist Bottom Bar",
        description = "Hides the playlist indicator bar displayed above the bottom navigation when a video is part of a playlist.",
        required = false,
    )

    val hideCommentButton by booleanOption(
        key = "hideCommentButton",
        default = false,
        title = "Hide Comment Button",
        description = "Hides the comment button on the right-side action rail of feed videos.",
        required = false,
    )

    val hideSaveButton by booleanOption(
        key = "hideSaveButton",
        default = false,
        title = "Hide Save Button",
        description = "Hides the bookmark/favorite save button on the right-side action rail of feed videos.",
        required = false,
    )

    val hideMusicCover by booleanOption(
        key = "hideMusicCover",
        default = false,
        title = "Hide Music Cover Disc",
        description = "Hides the rotating vinyl music album cover disc at the bottom right corner of feed videos.",
        required = false,
    )

    val hideFullscreenButton by booleanOption(
        key = "hideFullscreenButton",
        default = false,
        title = "Hide Full Screen Button",
        description = "Hides the floating 'Full screen' landscape orientation button overlay on horizontal feed videos.",
        required = false,
    )

    val hideFeedbackButtons by booleanOption(
        key = "hideFeedbackButtons",
        default = true,
        title = "Hide Feedback Buttons",
        description = "Hides the 'Not interested' / 'Interested' feedback pills above the bottom navigation on feed videos.",
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
        description = "Action when long pressing feed video body: 'nothing' (default, suppresses menu/repost), 'comments' (opens comments), or 'copyLink' (copies link to clipboard).",
        default = "nothing",
        values = mapOf("Do nothing" to "nothing", "Open comments" to "comments", "Copy link" to "copyLink"),
        required = false,
    )

    execute {
        val isLite = packageMetadata.packageName == Constants.TIKTOK_LITE_PACKAGE_NAME

        if (hideRepostBadge != true &&
            hideVideoDescriptions != true &&
            hideAvatarFollowButton != true &&
            disableStoryRings != true &&
            hidePlaylistBar != true &&
            hideCommentButton != true &&
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

        if (isLite) {
            executeLite(
                hideCommentButton = hideCommentButton,
                hideSaveButton = hideSaveButton,
                hideMusicCover = hideMusicCover,
                hideAvatarFollowButton = hideAvatarFollowButton,
                hideVideoDescriptions = hideVideoDescriptions,
                disableStoryRings = disableStoryRings,
                disableDoubleTapLike = disableDoubleTapLike,
            )
            return@execute
        }

        var patched = 0

        // 1. Hide Repost Badge ('Shared by' / 'Reposted')
        if (hideRepostBadge == true) {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/feed/platform/cell/interact/info/UpvoteVideoTrigger;",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked UpvoteVideoTrigger -> return false.")
            patched++

            val assemClass = "Lcom/ss/android/ugc/aweme/upvote/detail/whitebar/UpvoteVideoAssemNew;"

            Fingerprint(
                definingClass = assemClass,
                name = "sq",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.sq() -> return false.")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "ka",
                returnType = "Z",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.ka() -> return false.")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "LJJLIIIJL",
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.LJJLIIIJL() -> return-void.")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "LLLJL",
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.LLLJL() -> return-void.")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "fr",
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.fr() -> return-void.")
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
                name = "n4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked UpvoteVideoAssemNew.n4() -> return-void.")
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
            val descReturns = descOnViewCreated.implementation!!.instructions.mapIndexedNotNull { index, instr ->
                if (instr.opcode.name == "return-void") index else null
            }.reversed()
            for (index in descReturns) {
                descOnViewCreated.addInstructions(
                    index,
                    """
                        invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->hideFeedDescForAssem(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            println("[Declutter Video Interface] Hooked VideoDescAssem.onViewCreated() -> appended setVisibility(GONE).")
            patched++

            val descJs = Fingerprint(
                definingClass = descTargetClass,
                returnType = "V",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method
            descJs.clearTryBlocks()
            val jsReturns = descJs.implementation!!.instructions.mapIndexedNotNull { index, instr ->
                if (instr.opcode.name == "return-void") index else null
            }.reversed()
            for (index in jsReturns) {
                descJs.addInstructions(
                    index,
                    """
                        invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->hideFeedDescForAssem(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            println("[Declutter Video Interface] Hooked VideoDescAssem feed desc binding -> appended GONE enforcement.")
            patched++

            // 2b. Redirect feed desc visibility calls to always-GONE wrappers
            // (reactive-proof hiding without touching method bodies).
            // LX/0As7 is instantiated 1:1 by VideoDescAssem.onViewCreated (in v47.7.3, was LX/0As4 in v47.7.2, LX/095y in v47.5.3), and
            // VideoDescAssem itself manages desc container visibility. Neither
            // VideoDescAssem nor LX/0As7 ever calls setVisibility directly: every desc
            // visibility change flows through the LX/00kD visibility helpers below (in v47.7.3, was LX/00kA in v47.7.2).
            // Redirecting those calls (same invoke shape and width, only the callee changes)
            // across both VideoDescAssem and LX/0As7 to extension wrappers that force GONE
            // covers every current and future show path, including see-more expansion flows,
            // with no control-flow disturbance.
            val descVisibilityRedirects = mapOf(
                "LLLLZI" to "${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->forceGoneViewLast(Landroid/view/View;I)V",
                "LLLLZIL" to "${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->forceGoneViewLast(Landroid/view/View;I)V",
                "LJLLLLLL" to "${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->forceGoneViewFirst(ILandroid/view/View;)V",
                "LJLLILLLL" to "${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->forceGoneViewFirst(ILandroid/view/View;)V",
            )
            var redirectedMethods = 0
            var redirectedCallsDescAssem = 0
            var redirectedCalls095y = 0
            var skippedShapes = 0
            val descRedirectClasses = listOf(descTargetClass, "LX/0As7;")
            for (targetClass in descRedirectClasses) {
                var classRedirectedCalls = 0
                for (target in mutableClassDefBy(targetClass).methods) {
                    val instructions = target.implementation?.instructions ?: continue
                    val redirectIndices = mutableListOf<Pair<Int, String>>()
                    for ((index, instruction) in instructions.withIndex()) {
                        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                        if (reference.definingClass != "LX/00kD;") continue
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
                    redirectedCalls095y = classRedirectedCalls
                }
            }
            val redirectedCalls = redirectedCallsDescAssem + redirectedCalls095y
            if (redirectedCallsDescAssem == 0) {
                println("[Declutter Video Interface] WARNING: VideoDescAssem contributed 0 redirects")
            }
            require(redirectedCalls > 0) { "VideoDescAssem/LX/0As7 desc redirect: no visibility helper call found" }
            println("[Declutter Video Interface] Hooked desc visibility calls -> $redirectedCalls call(s) (VideoDescAssem: $redirectedCallsDescAssem, LX/0As7: $redirectedCalls095y, skippedShapes: $skippedShapes) across $redirectedMethods method(s) forced GONE.")
            patched++

            // 2c. Direct prologue guards on LX/00kD visibility helpers (in v47.7.3, was LX/00kA in v47.7.2)
            val descHelpers = listOf(
                Triple("LLLLZI", listOf("LX/0Ar1;", "I"), "p0"),
                Triple("LLLLZIL", listOf("Lcom/bytedance/tux/input/TuxTextView;", "I"), "p0"),
                Triple("LJLLLLLL", listOf("I", "Landroid/widget/LinearLayout;"), "p1"),
                Triple("LJLLILLLL", listOf("I", "Landroid/view/View;"), "p1"),
            )
            var installedHelpers = 0
            for ((name, params, viewReg) in descHelpers) {
                try {
                    val method = Fingerprint(
                        definingClass = "LX/00kD;",
                        name = name,
                        returnType = "V",
                        parameters = params,
                    ).method
                    method.addInstructions(
                        0,
                        "invoke-static {$viewReg}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->hideDescViewIfDesc(Landroid/view/View;)V",
                    )
                    println("[Declutter Video Interface] Hooked LX/00kD.$name() prologue -> hideDescViewIfDesc($viewReg).")
                    installedHelpers++
                } catch (e: Exception) {
                    println("[Declutter Video Interface] WARNING: helper $name signature not found, skipped")
                }
            }
            println("[Declutter Video Interface] Installed desc prologue guards on $installedHelpers / ${descHelpers.size} helper(s).")
            require(installedHelpers == descHelpers.size) { "LX/00kD desc visibility helpers: expected ${descHelpers.size}, installed $installedHelpers" }
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
                    name = "n4",
                    returnType = "V",
                    parameters = listOf("Ljava/lang/Object;"),
                ).method.replaceWithReturnVoid()
                println("[Declutter Video Interface] Hooked $transClass i4() -> return-void.")
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
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked PlayListBottomBarAssemTrigger -> return false.")
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
                name = "n4", // in v47.7.2, was i4 in v47.6.3, j4 in v47.5.3, l4 in v47.4.3
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked InteractPlayListBottomBarAssem.n4() -> return-void.")
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
                name = "n4", // in v47.7.2, was i4 in v47.6.3, j4 in v47.5.3, l4 in v47.4.3
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked VideoFavoriteAssem.n4() -> return-void.")
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
                name = "n4", // in v47.7.2, was i4 in v47.6.3
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked VideoMusicCoverAssem.n4() -> return-void.")
            patched++

            Fingerprint(
                definingClass = musicCoverClass,
                name = "Kq", // in v47.7.3, was Iq in v47.7.2, tq in v47.6.3
                returnType = "V",
                parameters = emptyList(),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked VideoMusicCoverAssem.Kq() -> return-void.")
            patched++

            Fingerprint(
                definingClass = musicCoverClass,
                name = "Lq", // in v47.7.3, was Jq in v47.7.2, vq in v47.6.3
                returnType = "V",
                parameters = emptyList(),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked VideoMusicCoverAssem.Lq() -> return-void.")
            patched++
        }

        // 8. Hide Full Screen Button
        if (hideFullscreenButton == true) {
            val landscapeClass = "Lcom/ss/android/ugc/aweme/feed/landscape/LandscapeEntranceAssem;"

            Fingerprint(
                definingClass = landscapeClass,
                name = "Nd", // in v47.7.3, was Vd in v47.7.2, Ce in v47.6.3
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.Nd() -> return false.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "z9", // in v47.7.3, was D9 in v47.7.2, G9 in v47.6.3
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.z9() -> return false.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "Cq", // in v47.7.3, was yq in v47.7.2, kq in v47.6.3
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.Cq() -> return false.")
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
                name = "n4", // in v47.7.2, was i4 in v47.6.3
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.n4() -> return-void.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "LLLJL", // in v47.7.2, was LLLILZJ in v47.6.3
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.LLLJL() -> return-void.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "LJJLIIIJL", // in v47.7.3, was LJJL in v47.7.2, LJJLIIIJJI in v47.6.3
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked LandscapeEntranceAssem.LJJLIIIJL() -> return-void.")
            patched++
        }

        // 9. Hide Feedback Buttons ('Not interested' / 'Interested')
        if (hideFeedbackButtons == true) {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/earlyfeedback/EarlyFeedbackButtonTrigger;",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Declutter Video Interface] Hooked EarlyFeedbackButtonTrigger -> return false.")
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
                    name = "n4", // in v47.7.2, was i4 in v47.6.3
                    returnType = "V",
                    parameters = listOf("Ljava/lang/Object;"),
                ).method.replaceWithReturnVoid()
                println("[Declutter Video Interface] Hooked $assemClass.n4() -> return-void.")
                patched++
            }

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/earlyfeedback/EarlyFeedbackStandardButtonAssem;",
                returnType = "V",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked EarlyFeedbackStandardButtonAssem -> return-void.")
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
        }

        // 11. Disable Profile Photo LIVE Status
        if (disableAvatarLiveStatus == true) {
            val wrapClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarAssemWrap;"
            val liveAssemClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarLiveAssem;"

            // Discover and hook the author LIVE status validator (e.g. LX/097j.LIZIZ(Aweme, User)Z in v47.5.3, was LX/09A7; in v47.4.3)
            var liveCheckClass = "LX/097j;"
            var liveCheckMethod = "LIZIZ"

            val wrapFp = Fingerprint(definingClass = wrapClass)
            var foundLiveCheck = false
            for (m in wrapFp.classDef.methods) {
                val insns = m.implementation?.instructions ?: continue
                for (insn in insns) {
                    val ref = (insn as? ReferenceInstruction)?.reference?.toString() ?: continue
                    if (ref.contains("(Lcom/ss/android/ugc/aweme/feed/model/Aweme;Lcom/ss/android/ugc/aweme/profile/model/User;)Z")) {
                        val parts = ref.split("->")
                        if (parts.size == 2) {
                            liveCheckClass = parts[0]
                            liveCheckMethod = parts[1].substringBefore("(")
                            foundLiveCheck = true
                            break
                        }
                    }
                }
                if (foundLiveCheck) break
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

            // Hook FeedAvatarLiveAssem.Lp(User, Z, Z)V -> return-void (suppresses live entrance animations, was Ep in v47.4.3)
            Fingerprint(
                definingClass = liveAssemClass,
                returnType = "V",
                parameters = listOf(
                    "Lcom/ss/android/ugc/aweme/profile/model/User;",
                    "Z",
                    "Z",
                ),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked FeedAvatarLiveAssem.Lp(User, Z, Z) -> return-void.")
            patched++

            // Hook FeedAvatarLiveAssem.n4(Object)V -> return-void (suppresses live UI binding and click redirection, was j4 in v47.6.3)
            Fingerprint(
                definingClass = liveAssemClass,
                name = "n4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked FeedAvatarLiveAssem.n4(Object)V -> return-void.")
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

private fun BytecodePatchContext.executeLite(
    hideCommentButton: Boolean?,
    hideSaveButton: Boolean?,
    hideMusicCover: Boolean?,
    hideAvatarFollowButton: Boolean?,
    hideVideoDescriptions: Boolean?,
    disableStoryRings: Boolean?,
    disableDoubleTapLike: Boolean?,
) {
    var patched = 0

    // 1. Hide Comment Button
    if (hideCommentButton == true) {
        val commentPresenterFp = Fingerprint(
            returnType = "Ljava/lang/String;",
            parameters = emptyList(),
            strings = listOf("right_container_comment"),
        )
        val commentClass = commentPresenterFp.method.definingClass
        val commentDef = mutableClassDefBy(commentClass)

        // Find the container View field populated from findViewById(0x7f0900d7 / 2131296471)
        val initMethod = commentDef.methods.first { it.name == "<init>" }
        val initInstructions = initMethod.implementation!!.instructions
        var containerFieldName: String? = null
        val constIndex = initInstructions.indexOfFirst { ins ->
            ins is NarrowLiteralInstruction && ins.narrowLiteral == 2131296471
        }
        if (constIndex >= 0) {
            for (i in constIndex + 1 until (constIndex + 10).coerceAtMost(initInstructions.size)) {
                val ins = initInstructions[i]
                if (ins is ReferenceInstruction && ins.reference is FieldReference) {
                    val fieldRef = ins.reference as FieldReference
                    if (fieldRef.definingClass == commentDef.type && fieldRef.type == "Landroid/view/View;") {
                        containerFieldName = fieldRef.name
                        break
                    }
                }
            }
        }
        if (containerFieldName == null) {
            error("[Declutter Video Interface] Failed to resolve comment container View field in ${commentDef.type}")
        }

        // Inject setVisibility(GONE) at the end of <init>
        val returnIndex = initInstructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        if (returnIndex < 0) {
            error("[Declutter Video Interface] Failed to find RETURN_VOID in ${commentDef.type}.<init>")
        }
        initMethod.addInstructions(
            returnIndex,
            """
                iget-object v0, p0, ${commentDef.type}->$containerFieldName:Landroid/view/View;
                if-eqz v0, :cond_comment_init_skip
                const/16 v1, 0x8
                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                :cond_comment_init_skip
            """.trimIndent(),
        )
        println("[Declutter Video Interface] Hooked ${commentDef.type}.<init> -> Container View default set to GONE.")
        patched++

        // Override bind method to keep View GONE and clear click/touch listeners
        val bindMethod = commentDef.methods.firstOrNull {
            it.returnType == "V" && it.parameterTypes == listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;")
        } ?: error("[Declutter Video Interface] Failed to find bind method in ${commentDef.type}")

        bindMethod.clearTryBlocks()
        bindMethod.ensureRegisterCount(4)
        val bindCount = bindMethod.implementation!!.instructions.count()
        bindMethod.removeInstructions(0, bindCount)
        bindMethod.addInstructions(
            0,
            """
                iget-object v0, p0, ${commentDef.type}->$containerFieldName:Landroid/view/View;
                if-eqz v0, :cond_comment_bind_skip
                const/16 v1, 0x8
                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                const/4 v1, 0x0
                invoke-virtual {v0, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
                invoke-virtual {v0, v1}, Landroid/view/View;->setOnTouchListener(Landroid/view/View${'$'}OnTouchListener;)V
                :cond_comment_bind_skip
                return-void
            """.trimIndent(),
        )
        println("[Declutter Video Interface] Hooked ${commentDef.type}.${bindMethod.name}(Aweme) -> Bind replaced with GONE + null click/touch listeners.")
        patched++

        // Hook Lite MiniLite feed mode comment presenter (LX/Bwc;)
        val miniLiteCommentFp = Fingerprint(
            returnType = "Landroid/view/ViewGroup;",
            parameters = listOf("Landroid/view/View;"),
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    ins is NarrowLiteralInstruction && ins.narrowLiteral == 2131296471
                } == true
            },
        )
        val miniLiteDef = mutableClassDefBy(miniLiteCommentFp.method.definingClass)
        val miniLiteMethod = miniLiteCommentFp.method

        val miniLiteCount = miniLiteMethod.implementation!!.instructions.count()
        miniLiteMethod.clearTryBlocks()
        miniLiteMethod.ensureRegisterCount(4)
        miniLiteMethod.removeInstructions(0, miniLiteCount)
        miniLiteMethod.addInstructions(
            0,
            """
                const v0, 2131296471
                invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                move-result-object v0
                check-cast v0, Landroid/view/ViewGroup;
                if-eqz v0, :cond_skip
                const/16 v1, 0x8
                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                :cond_skip
                return-object v0
            """.trimIndent(),
        )
        println("[Declutter Video Interface] Hooked ${miniLiteDef.type}.${miniLiteMethod.name}(View)ViewGroup -> ViewGroup set to GONE before return.")
        patched++

        var miniLiteHelpersHooked = 0
        for (m in miniLiteDef.methods) {
            if (m.name != "<init>" && m.returnType == "V" && m.parameterTypes.isEmpty()) {
                m.replaceWithReturnVoid()
                miniLiteHelpersHooked++
            }
        }
        if (miniLiteHelpersHooked == 0) {
            error("[Declutter Video Interface] Failed to find ()V helper methods in ${miniLiteDef.type}")
        }
        println("[Declutter Video Interface] Hooked $miniLiteHelpersHooked helper method(s) in ${miniLiteDef.type} -> return-void.")
        patched += miniLiteHelpersHooked
    }

    // 2. Hide Save Button (Favorite/Bookmark)
    if (hideSaveButton == true) {
        val favPresenterFp = Fingerprint(
            returnType = "Ljava/lang/String;",
            parameters = emptyList(),
            strings = listOf("right_container_favorite"),
        )
        val favClass = favPresenterFp.method.definingClass
        val favDef = mutableClassDefBy(favClass)

        // Resolve container View field name from LILLLZ()Landroid/view/View;
        val lillzMethod = favDef.methods.firstOrNull {
            it.name == "LILLLZ" && it.returnType == "Landroid/view/View;" && it.parameterTypes.isEmpty()
        } ?: error("[Declutter Video Interface] Failed to find LILLLZ() in ${favDef.type}")
        val containerFieldName = lillzMethod.implementation!!.instructions.firstNotNullOfOrNull { ins ->
            ((ins as? ReferenceInstruction)?.reference as? FieldReference)?.name
        } ?: error("[Declutter Video Interface] Failed to resolve favorite container field in ${favDef.type}")

        // 2a. Inject setVisibility(GONE) at the end of <init>
        val initMethod = favDef.methods.first { it.name == "<init>" }
        val initInstructions = initMethod.implementation!!.instructions
        val returnIndex = initInstructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        if (returnIndex < 0) {
            error("[Declutter Video Interface] Failed to find RETURN_VOID in ${favDef.type}.<init>")
        }
        initMethod.addInstructions(
            returnIndex,
            """
                iget-object v0, p0, ${favDef.type}->$containerFieldName:Landroid/view/View;
                if-eqz v0, :cond_fav_init_skip
                const/16 v1, 0x8
                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                :cond_fav_init_skip
            """.trimIndent(),
        )
        println("[Declutter Video Interface] Hooked ${favDef.type}.<init> -> Favorite container View default set to GONE.")
        patched++

        // 2b. Neutralize onClick in standard feed favorite presenter
        val onClickMethod = favDef.methods.firstOrNull {
            it.name == "onClick" && it.returnType == "V" && it.parameterTypes == listOf("Landroid/view/View;")
        }
        if (onClickMethod != null) {
            onClickMethod.replaceWithReturnVoid()
            println("[Declutter Video Interface] Hooked ${favDef.type}.onClick(View) -> return-void.")
            patched++
        }

        // 2c. Force global favorite feed flag LX/DAx;->LC()Z to return false (forces LILZZL to set GONE)
        Fingerprint(
            definingClass = "LX/DAx;",
            name = "LC",
            returnType = "Z",
            parameters = emptyList(),
        ).method.replaceWithReturnBoolean(false)
        println("[Declutter Video Interface] Hooked LX/DAx.LC() -> return false (feed favorite disabled).")
        patched++

        // 2d. Hook MiniLite feed mode favorite presenter (LX/Bwk;)
        val miniLiteFavFp = Fingerprint(
            returnType = "Landroid/view/ViewGroup;",
            parameters = listOf("Landroid/view/View;"),
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    ins is NarrowLiteralInstruction && ins.narrowLiteral == 2131296979
                } == true
            },
        )
        val miniLiteFavDef = mutableClassDefBy(miniLiteFavFp.method.definingClass)
        val miniLiteFavMethod = miniLiteFavFp.method

        val miniLiteFavCount = miniLiteFavMethod.implementation!!.instructions.count()
        miniLiteFavMethod.clearTryBlocks()
        miniLiteFavMethod.ensureRegisterCount(4)
        miniLiteFavMethod.removeInstructions(0, miniLiteFavCount)
        miniLiteFavMethod.addInstructions(
            0,
            """
                const v0, 2131296979
                invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                move-result-object v0
                check-cast v0, Landroid/view/ViewGroup;
                if-eqz v0, :cond_fav_skip
                const/16 v1, 0x8
                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                :cond_fav_skip
                return-object v0
            """.trimIndent(),
        )
        println("[Declutter Video Interface] Hooked ${miniLiteFavDef.type}.${miniLiteFavMethod.name}(View)ViewGroup -> ViewGroup set to GONE before return.")
        patched++

        var miniLiteFavHelpersHooked = 0
        for (m in miniLiteFavDef.methods) {
            if (m.name != "<init>" && m.returnType == "V" && m.parameterTypes.isEmpty()) {
                m.replaceWithReturnVoid()
                miniLiteFavHelpersHooked++
            }
        }
        if (miniLiteFavHelpersHooked == 0) {
            error("[Declutter Video Interface] Failed to find ()V helper methods in ${miniLiteFavDef.type}")
        }
        println("[Declutter Video Interface] Hooked $miniLiteFavHelpersHooked helper method(s) in ${miniLiteFavDef.type} -> return-void.")
        patched += miniLiteFavHelpersHooked
    }

    println("[Declutter Video Interface] Successfully applied $patched hook(s) across selected feed interface options on TikTok Lite.")
}

