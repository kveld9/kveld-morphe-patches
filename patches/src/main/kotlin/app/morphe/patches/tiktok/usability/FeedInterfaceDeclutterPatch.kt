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
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

val feedInterfaceDeclutterPatch = bytecodePatch(
    name = "Feed Interface Declutter",
    description = "Customizes and cleans feed video overlay elements, including the repost pill, video descriptions, profile photo follow badges, and story rings.",
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

    execute {
        if (hideRepostBadge != true &&
            hideVideoDescriptions != true &&
            hideAvatarFollowButton != true &&
            disableStoryRings != true
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

        println("[Feed Interface Declutter] Successfully applied $patched hook(s) across selected feed interface options.")
    }
}
