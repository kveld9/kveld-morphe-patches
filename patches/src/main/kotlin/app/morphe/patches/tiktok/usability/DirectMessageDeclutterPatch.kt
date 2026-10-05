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
import app.morphe.patches.shared.replaceWithReturnNull
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

val directMessageDeclutterPatch = bytecodePatch(
    name = "Direct Message Declutter",
    description = "Removes visual clutter in direct messages and chat list, including the call button, reaction tray, message forward button, camera icons, and input action buttons.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    val hideChatListCamera by booleanOption(
        key = "hideChatListCamera",
        title = "Hide Chat List Camera Button",
        description = "Removes the camera icon next to chats in the direct messages conversation list.",
        default = true,
        required = false,
    )

    val hideCallButton by booleanOption(
        key = "hideCallButton",
        title = "Hide Header Call Button",
        description = "Removes the phone receiver (voice/video call) button from the direct message chat header.",
        default = true,
        required = false,
    )

    val hideMessageForwardButton by booleanOption(
        key = "hideMessageForwardButton",
        title = "Hide Message Forward Button",
        description = "Removes the quick forward/share arrow button displayed next to messages and shared videos.",
        default = true,
        required = false,
    )

    val hideReactionTray by booleanOption(
        key = "hideReactionTray",
        title = "Hide Reaction and Streak Bar",
        description = "Removes the reaction bar and streak mascot banner displayed above the text input bar.",
        default = true,
        required = false,
    )

    val hideInputCamera by booleanOption(
        key = "hideInputCamera",
        title = "Hide Input Bar Camera Button",
        description = "Removes the camera button located to the left of the direct message text input field.",
        default = true,
        required = false,
    )

    val hideGalleryButton by booleanOption(
        key = "hideGalleryButton",
        title = "Hide Gallery Button",
        description = "Removes the photo gallery button located inside the right side of the text input field.",
        default = true,
        required = false,
    )

    val hideEmojiButton by booleanOption(
        key = "hideEmojiButton",
        title = "Hide Emoji/Stickers Button",
        description = "Removes the stickers and emoji button located inside the right side of the text input field.",
        default = true,
        required = false,
    )

    val hideVoiceRecordButton by booleanOption(
        key = "hideVoiceRecordButton",
        title = "Hide Voice Recording Button",
        description = "Removes the audio recording/microphone button located inside the right side of the text input field.",
        default = true,
        required = false,
    )

    execute {
        if (hideChatListCamera != true &&
            hideCallButton != true &&
            hideMessageForwardButton != true &&
            hideReactionTray != true &&
            hideInputCamera != true &&
            hideGalleryButton != true &&
            hideEmojiButton != true &&
            hideVoiceRecordButton != true
        ) {
            println("[Direct Message Declutter] All toggles disabled -> nothing to patch.")
            return@execute
        }

        var patched = 0

        // 1. Feature: Hide Camera Icon in Chat List (Inbox conversation rows)
        if (hideChatListCamera == true) {
            val chatListItemModelFp = Fingerprint(
                custom = { _, classDef ->
                    classDef.fields.any { it.name == "showCameraIcon" } &&
                        classDef.fields.any { it.name == "showPhotoSwapThumbnail" }
                },
            )
            val modelClass = chatListItemModelFp.classDef
            val initMethod = modelClass.methods.first {
                it.name == "<init>" && it.parameterTypes.size == 4
            }
            val impl = initMethod.implementation
            if (impl != null) {
                initMethod.clearTryBlocks()
                initMethod.removeInstructions(0, impl.instructions.count())
                initMethod.addInstructions(
                    0,
                    """
                        invoke-direct {p0}, Ljava/lang/Object;-><init>()V
                        const/4 v0, 0x0
                        iput-boolean v0, p0, ${modelClass.type}->showCameraIcon:Z
                        iput-boolean p2, p0, ${modelClass.type}->showPhotoSwapThumbnail:Z
                        iput-object p3, p0, ${modelClass.type}->photoSwapThumbnailUrls:Ljava/util/List;
                        iput-object p4, p0, ${modelClass.type}->socialBlinkThumbnailUrlsToShow:Ljava/util/List;
                        return-void
                    """.trimIndent(),
                )
                println("[Direct Message Declutter] Hooked ${modelClass.type}.<init> -> showCameraIcon=false.")
                patched++
            }

            val setterMethod = modelClass.methods.firstOrNull { it.name == "setShowCameraIcon" }
            if (setterMethod != null) {
                val setterImpl = setterMethod.implementation
                if (setterImpl != null) {
                    setterMethod.clearTryBlocks()
                    setterMethod.removeInstructions(0, setterImpl.instructions.count())
                    setterMethod.addInstructions(
                        0,
                        """
                            const/4 v0, 0x0
                            iput-boolean v0, p0, ${modelClass.type}->showCameraIcon:Z
                            return-void
                        """.trimIndent(),
                    )
                    println("[Direct Message Declutter] Hooked ${modelClass.type}.setShowCameraIcon -> force false.")
                    patched++
                }
            }
        }

        // 2. Feature: Hide Call Button in Single Chat Header (Phone receiver icon)
        if (hideCallButton == true) {
            val titleBarFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/chatroom/common/titlebar/BaseSingleChatTitleBarRightAssem;",
                name = "onViewCreated",
                parameters = listOf("Landroid/view/View;"),
                returnType = "V",
            )
            val titleBarMethod = titleBarFp.method
            val titleBarImpl = titleBarMethod.implementation
            if (titleBarImpl != null) {
                titleBarMethod.ensureRegisterCount(4)
                titleBarMethod.addInstructions(
                    0,
                    """
                        move-object/from16 v0, p1
                        const v1, 0x7f0a39ae
                        invoke-virtual {v0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                        move-result-object v0
                        if-eqz v0, :cond_skip_call
                        const/16 v1, 0x8
                        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                        :cond_skip_call
                    """.trimIndent(),
                )
                for ((index, instruction) in titleBarImpl.instructions.withIndex()) {
                    if (instruction is ReferenceInstruction) {
                        val fieldRef = instruction.reference as? FieldReference
                        if (fieldRef?.name == "LLLFF") {
                            titleBarMethod.addInstructions(
                                index + 1,
                                """
                                    const/4 v0, 0x0
                                    iput-object v0, p0, Lcom/ss/android/ugc/aweme/im/chatroom/common/titlebar/BaseSingleChatTitleBarRightAssem;->LLLFF:Lcom/bytedance/tux/icon/TuxIconView;
                                """.trimIndent(),
                            )
                            break
                        }
                    }
                }
                println("[Direct Message Declutter] Hooked BaseSingleChatTitleBarRightAssem.onViewCreated -> Call button GONE.")
                patched++
            }
        }

        // 3. Feature: Hide Quick Forward Button beside Messages
        if (hideMessageForwardButton == true) {
            val sideSlotClassFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/sdk/chat/ui/slots/SideMessageStatusReusedSkeletonUISlot;",
            )
            val vsMethod = sideSlotClassFp.classDef.methods.first {
                AccessFlags.STATIC.isSet(it.accessFlags) &&
                    it.parameters.size == 1 &&
                    it.returnType != "V"
            }
            val vsImpl = vsMethod.implementation
            if (vsImpl != null) {
                val enumType = vsMethod.returnType
                var hooked = false
                for ((index, instruction) in vsImpl.instructions.withIndex()) {
                    if (instruction is ReferenceInstruction) {
                        val fieldRef = instruction.reference as? FieldReference
                        if (fieldRef?.name == "FORWARD") {
                            vsMethod.removeInstructions(index, 1)
                            vsMethod.addInstructions(
                                index,
                                "sget-object v0, $enumType->NOTHING:$enumType",
                            )
                            hooked = true
                            break
                        }
                    }
                }
                if (hooked) {
                    println("[Direct Message Declutter] Hooked SideMessageStatusReusedSkeletonUISlot.vs -> NOTHING.")
                    patched++
                }
            }
        }

        // 4. Feature: Hide Reaction Tray & Streak Mascot Bar above input field
        if (hideReactionTray == true) {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/actionbar/serviceimpl/ActionBarServiceImpl;",
                name = "LJI",
                parameters = emptyList(),
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            println("[Direct Message Declutter] Hooked ActionBarServiceImpl.LJI -> false.")
            patched++

            val actionBarUiAssemFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/actionbar/serviceimpl/ActionBarUIAssem;",
                name = "onViewCreated",
                parameters = listOf("Landroid/view/View;"),
                returnType = "V",
            )
            val actionBarUiMethod = actionBarUiAssemFp.method
            val actionBarUiImpl = actionBarUiMethod.implementation
            if (actionBarUiImpl != null) {
                actionBarUiMethod.clearTryBlocks()
                actionBarUiMethod.ensureRegisterCount(2)
                actionBarUiMethod.removeInstructions(0, actionBarUiImpl.instructions.count())
                actionBarUiMethod.addInstructions(
                    0,
                    """
                        move-object/from16 v0, p1
                        const/16 v1, 0x8
                        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                        return-void
                    """.trimIndent(),
                )
                println("[Direct Message Declutter] Hooked ActionBarUIAssem.onViewCreated -> View.GONE.")
                patched++
            }
        }

        // 5. Feature: Hide Quick Camera Button left of input field
        if (hideInputCamera == true) {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/saas/host/impl/feature/camera/DMCameraFeatureImpl;",
                name = "LJII",
                returnType = "Lkotlin/jvm/functions/Function1;",
            ).method.replaceWithReturnNull()
            println("[Direct Message Declutter] Hooked DMCameraFeatureImpl.LJII -> return-null.")
            patched++

            val inputAssemFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/assem/IMInputAssem;",
                name = "onViewCreated",
                parameters = listOf("Landroid/view/View;"),
                returnType = "V",
            )
            val inputAssemMethod = inputAssemFp.method
            val inputAssemImpl = inputAssemMethod.implementation
            if (inputAssemImpl != null) {
                inputAssemMethod.ensureRegisterCount(4)
                inputAssemMethod.addInstructions(
                    0,
                    """
                        move-object/from16 v0, p1
                        const v1, 0x7f0a479c
                        invoke-virtual {v0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                        move-result-object v0
                        if-eqz v0, :cond_skip_cam
                        const/16 v1, 0x8
                        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                        :cond_skip_cam
                    """.trimIndent(),
                )
                println("[Direct Message Declutter] Hooked IMInputAssem.onViewCreated -> Camera slot GONE.")
                patched++
            }
        }

        // 6. Feature: Hide Gallery/Album Button inside input field
        if (hideGalleryButton == true) {
            val galleryAssemFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/chatroom/input/components/misc/IMImageBtnViewAssem;",
                name = "Wr",
                parameters = listOf("Landroid/view/View;"),
                returnType = "V",
            )
            val galleryMethod = galleryAssemFp.method
            val galleryImpl = galleryMethod.implementation
            if (galleryImpl != null) {
                galleryMethod.clearTryBlocks()
                galleryMethod.ensureRegisterCount(2)
                galleryMethod.removeInstructions(0, galleryImpl.instructions.count())
                galleryMethod.addInstructions(
                    0,
                    """
                        move-object/from16 v0, p1
                        const/16 v1, 0x8
                        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                        return-void
                    """.trimIndent(),
                )
                println("[Direct Message Declutter] Hooked IMImageBtnViewAssem.Wr -> View.GONE.")
                patched++
            }
        }

        // 7. Feature: Hide Emoji/Stickers Button inside input field
        if (hideEmojiButton == true) {
            val emojiAssemFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/chatroom/input/components/stickerbtn/InputEmojiButtonUIAssem;",
                name = "onViewCreated",
                parameters = listOf("Landroid/view/View;"),
                returnType = "V",
            )
            val emojiMethod = emojiAssemFp.method
            val emojiImpl = emojiMethod.implementation
            if (emojiImpl != null) {
                emojiMethod.clearTryBlocks()
                emojiMethod.ensureRegisterCount(2)
                emojiMethod.removeInstructions(0, emojiImpl.instructions.count())
                emojiMethod.addInstructions(
                    0,
                    """
                        move-object/from16 v0, p1
                        const/16 v1, 0x8
                        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                        return-void
                    """.trimIndent(),
                )
                println("[Direct Message Declutter] Hooked InputEmojiButtonUIAssem.onViewCreated -> View.GONE.")
                patched++
            }
        }

        // 8. Feature: Hide Voice Recording Button inside input field
        if (hideVoiceRecordButton == true) {
            val recordAssemFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/chatroom/input/components/record/RecordBtnAssem;",
                name = "onViewCreated",
                parameters = listOf("Landroid/view/View;"),
                returnType = "V",
            )
            val recordMethod = recordAssemFp.method
            val recordImpl = recordMethod.implementation
            if (recordImpl != null) {
                recordMethod.clearTryBlocks()
                recordMethod.ensureRegisterCount(2)
                recordMethod.removeInstructions(0, recordImpl.instructions.count())
                recordMethod.addInstructions(
                    0,
                    """
                        move-object/from16 v0, p1
                        const/16 v1, 0x8
                        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                        return-void
                    """.trimIndent(),
                )
                println("[Direct Message Declutter] Hooked RecordBtnAssem.onViewCreated -> View.GONE.")
                patched++
            }
        }

        println("[Direct Message Declutter] Applied $patched hooks -> direct messages decluttered.")
    }
}
