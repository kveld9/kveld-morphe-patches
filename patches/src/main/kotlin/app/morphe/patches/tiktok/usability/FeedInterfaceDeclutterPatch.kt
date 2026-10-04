package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnInt
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

val feedInterfaceDeclutterPatch = bytecodePatch(
    name = "Feed Interface Declutter",
    description = "Customizes and cleans feed video overlay elements, including the full screen button, repost pill, video descriptions, profile photo follow badges, story rings, playlist bottom bars, save buttons, and music discs.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    extendWith("extensions/extension.mpe")

    val hideRepostBadge by booleanOption(
        key = "hideRepostBadge",
        default = true,
        title = "Hide Repost Badge",
        description = "Hides the repost and shared-by pill badge ('Compartido por') above creator details on feed videos.",
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
        description = "Hides the floating 'Full screen' ('Pantalla completa') landscape orientation button overlay on horizontal feed videos.",
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
            hideFullscreenButton != true
        ) {
            println("[Feed Interface Declutter] Skipped: All declutter options are disabled.")
            return@execute
        }

        var patched = 0

        // 1. Hide Repost Badge ('Compartido por')
        if (hideRepostBadge == true) {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/feed/platform/cell/interact/info/UpvoteVideoTrigger;",
                name = "yr",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Feed Interface Declutter] Hooked UpvoteVideoTrigger.yr() -> return false.")
            patched++

            val assemClass = "Lcom/ss/android/ugc/aweme/upvote/detail/whitebar/UpvoteVideoAssemNew;"

            Fingerprint(
                definingClass = assemClass,
                name = "tr",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Feed Interface Declutter] Hooked UpvoteVideoAssemNew.tr() -> return false.")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "hb",
                returnType = "Z",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Feed Interface Declutter] Hooked UpvoteVideoAssemNew.hb() -> return false.")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "LLLLIILL",
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked UpvoteVideoAssemNew.LLLLIILL() -> return-void.")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "LLILZ",
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked UpvoteVideoAssemNew.LLILZ() -> return-void.")
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
            println("[Feed Interface Declutter] Hooked UpvoteVideoAssemNew.onViewCreated() -> setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = assemClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked UpvoteVideoAssemNew.z4() -> return-void.")
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
            descOnViewCreated.ensureRegisterCount(2)
            val descCount = descOnViewCreated.implementation!!.instructions.count()
            descOnViewCreated.removeInstructions(0, descCount)
            descOnViewCreated.addInstructions(
                0,
                """
                    invoke-super {p0, p1}, Lcom/ss/android/ugc/feed/platform/cell/BaseCellSlotComponent;->onViewCreated(Landroid/view/View;)V
                    const/16 v0, 0x8
                    invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                    return-void
                """.trimIndent(),
            )
            println("[Feed Interface Declutter] Hooked VideoDescAssem.onViewCreated() -> setVisibility(GONE).")
            patched++

            val descZ4 = Fingerprint(
                definingClass = descTargetClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method
            descZ4.clearTryBlocks()
            descZ4.ensureRegisterCount(2)
            val z4Count = descZ4.implementation!!.instructions.count()
            descZ4.removeInstructions(0, z4Count)
            descZ4.addInstructions(
                0,
                """
                    invoke-virtual {p0}, Lcom/ss/android/ugc/aweme/feed/assem/desc/VideoDescAssem;->qc()Landroid/view/View;
                    move-result-object v0
                    if-eqz v0, :cond_skip
                    const/16 v1, 0x8
                    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                    :cond_skip
                    return-void
                """.trimIndent(),
            )
            println("[Feed Interface Declutter] Hooked VideoDescAssem.z4() -> enforce GONE and suppress binding.")
            patched++

            Fingerprint(
                definingClass = descTargetClass,
                name = "js",
                returnType = "V",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked VideoDescAssem.js() -> return-void.")
            patched++

            val friendsDescClass = "Lcom/ss/android/ugc/aweme/friendstab/ui/feed/cell/component/desc/FriendsV3DescAssem;"

            Fingerprint(
                definingClass = friendsDescClass,
                name = "tr",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Feed Interface Declutter] Hooked FriendsV3DescAssem.tr() -> return false.")
            patched++

            Fingerprint(
                definingClass = friendsDescClass,
                name = "hb",
                returnType = "Z",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Feed Interface Declutter] Hooked FriendsV3DescAssem.hb() -> return false.")
            patched++

            val friendsOnViewCreated = Fingerprint(
                definingClass = friendsDescClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method
            friendsOnViewCreated.clearTryBlocks()
            friendsOnViewCreated.ensureRegisterCount(2)
            val friendsCount = friendsOnViewCreated.implementation!!.instructions.count()
            friendsOnViewCreated.removeInstructions(0, friendsCount)
            friendsOnViewCreated.addInstructions(
                0,
                """
                    const/16 v0, 0x8
                    invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                    return-void
                """.trimIndent(),
            )
            println("[Feed Interface Declutter] Hooked FriendsV3DescAssem.onViewCreated() -> setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = friendsDescClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked FriendsV3DescAssem.z4() -> return-void.")
            patched++

            Fingerprint(
                definingClass = friendsDescClass,
                name = "js",
                returnType = "V",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked FriendsV3DescAssem.js() -> return-void.")
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
                val count = transOnViewCreated.implementation!!.instructions.count()
                transOnViewCreated.removeInstructions(0, count)
                transOnViewCreated.addInstructions(
                    0,
                    """
                        invoke-super {p0, p1}, Lcom/ss/android/ugc/feed/platform/cell/BaseCellSlotComponent;->onViewCreated(Landroid/view/View;)V
                        const/16 v0, 0x8
                        invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                        return-void
                    """.trimIndent(),
                )
                println("[Feed Interface Declutter] Hooked $transClass onViewCreated() -> setVisibility(GONE).")
                patched++

                Fingerprint(
                    definingClass = transClass,
                    name = "z4",
                    returnType = "V",
                    parameters = listOf("Ljava/lang/Object;"),
                ).method.replaceWithReturnVoid()
                println("[Feed Interface Declutter] Hooked $transClass z4() -> return-void.")
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
            println("[Feed Interface Declutter] Hooked FeedAvatarDefaultAssem.${qrMethod.name}() -> Permanently GONE (0x8) and non-clickable.")
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
                        invoke-static {v$regA}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->hideFollowButton(Landroid/view/View;)V
                    """.trimIndent(),
                )
                println("[Feed Interface Declutter] Hooked FeedAvatarDefaultAssem.onViewCreated() -> Immediate GONE initialization on v$regA.")
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
            println("[Feed Interface Declutter] Hooked User.getStoryStatus() -> 0.")
            patched++

            val socialPublishClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarSocialPublishAssem;"
            Fingerprint(
                definingClass = socialPublishClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked FeedAvatarSocialPublishAssem.onViewCreated() -> return-void.")
            patched++

            Fingerprint(
                definingClass = socialPublishClass,
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked FeedAvatarSocialPublishAssem.(Object)V -> return-void.")
            patched++

            Fingerprint(
                definingClass = socialPublishClass,
                returnType = "V",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked FeedAvatarSocialPublishAssem.(VideoItemParams)V -> return-void.")
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/service/SocPubDistributeServiceImpl;",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/profile/model/User;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Feed Interface Declutter] Hooked SocPubDistributeServiceImpl.(User)Z -> false.")
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
            println("[Feed Interface Declutter] Hooked PlayListBottomBarAssemTrigger.yr() -> return false.")
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
            println("[Feed Interface Declutter] Hooked InteractPlayListBottomBarAssem.onViewCreated() -> setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = bottomBarClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked InteractPlayListBottomBarAssem.z4() -> return-void.")
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getPlaylist_info",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/PlayListInfo;",
            ).method.replaceWithReturnNull()
            println("[Feed Interface Declutter] Hooked Aweme.getPlaylist_info() -> return null.")
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
            println("[Feed Interface Declutter] Hooked VideoFavoriteAssem.onViewCreated() -> setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = favClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked VideoFavoriteAssem.z4() -> return-void.")
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
            coverOnViewCreated.ensureRegisterCount(2)
            val count = coverOnViewCreated.implementation!!.instructions.count()
            coverOnViewCreated.removeInstructions(0, count)
            coverOnViewCreated.addInstructions(
                0,
                """
                    invoke-super {p0, p1}, Lcom/ss/android/ugc/feed/platform/cell/BaseCellSlotComponent;->onViewCreated(Landroid/view/View;)V
                    const/16 v0, 0x8
                    invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                    return-void
                """.trimIndent(),
            )
            println("[Feed Interface Declutter] Hooked VideoMusicCoverAssem.onViewCreated() -> setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = musicCoverClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked VideoMusicCoverAssem.z4() -> return-void.")
            patched++

            Fingerprint(
                definingClass = musicCoverClass,
                name = "Tr",
                returnType = "V",
                parameters = emptyList(),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked VideoMusicCoverAssem.Tr() -> return-void.")
            patched++

            Fingerprint(
                definingClass = musicCoverClass,
                name = "Wr",
                returnType = "V",
                parameters = emptyList(),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked VideoMusicCoverAssem.Wr() -> return-void.")
            patched++
        }

        // 8. Hide Full Screen Button ('Pantalla completa')
        if (hideFullscreenButton == true) {
            val landscapeClass = "Lcom/ss/android/ugc/aweme/feed/landscape/LandscapeEntranceAssem;"

            Fingerprint(
                definingClass = landscapeClass,
                name = "Kr",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Feed Interface Declutter] Hooked LandscapeEntranceAssem.Kr() -> return false.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "Aa",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Feed Interface Declutter] Hooked LandscapeEntranceAssem.Aa() -> return false.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "bf",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Feed Interface Declutter] Hooked LandscapeEntranceAssem.bf() -> return false.")
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
            println("[Feed Interface Declutter] Hooked LandscapeEntranceAssem.onViewCreated() -> setVisibility(GONE).")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "z4",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked LandscapeEntranceAssem.z4() -> return-void.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "LLLLIILL",
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked LandscapeEntranceAssem.LLLLIILL() -> return-void.")
            patched++

            Fingerprint(
                definingClass = landscapeClass,
                name = "LLILZ",
                returnType = "V",
                parameters = listOf("I"),
            ).method.replaceWithReturnVoid()
            println("[Feed Interface Declutter] Hooked LandscapeEntranceAssem.LLILZ() -> return-void.")
            patched++
        }

        println("[Feed Interface Declutter] Successfully applied $patched hook(s) across selected feed interface options.")
    }
}
