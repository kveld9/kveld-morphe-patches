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
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val directMessageDeclutterPatch = bytecodePatch(
    name = "Direct Message Declutter",
    description = "Removes visual clutter in direct messages and chat list, including the call button, reaction tray, message forward button, camera icons, input action buttons, try effect button, sticker reply suggestions, and the typing-triggered sticker strip.",
    default = false,
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

    val hideTryEffectButton by booleanOption(
        key = "hideTryEffectButton",
        title = "Hide Try Effect Button",
        description = "Removes the 'Try effect' camera button shown on shared videos that use an effect in direct messages.",
        default = true,
        required = false,
    )

    val hideStickerReplySuggestions by booleanOption(
        key = "hideStickerReplySuggestions",
        title = "Hide Sticker Reply Suggestions",
        description = "Removes the automatic 'Tap a sticker to reply' suggestion panel and the typing-triggered sticker/GIF strip above the input bar. The manual sticker reply button keeps working.",
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
            hideVoiceRecordButton != true &&
            hideTryEffectButton != true &&
            hideStickerReplySuggestions != true
        ) {
            println("[Direct Message Declutter] All toggles disabled -> nothing to patch.")
            return@execute
        }

        var patched = 0

        // 1. Feature: Hide Camera Icon in Chat List (Inbox conversation rows)
        if (hideChatListCamera == true) {
            // The patch writes these fields with iput-boolean, so they are instance fields; iterating only instance fields skips decoding static fields.
            val chatListItemModelFp = Fingerprint(
                custom = { _, classDef ->
                    classDef.instanceFields.any { it.name == "showCameraIcon" } &&
                        classDef.instanceFields.any { it.name == "showPhotoSwapThumbnail" }
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
            val sideSlotClass = sideSlotClassFp.classDef
            val getterMethod = sideSlotClass.methods.first {
                !AccessFlags.STATIC.isSet(it.accessFlags) &&
                    it.parameterTypes.isEmpty() &&
                    it.returnType == "Lcom/bytedance/tux/icon/TuxIconView;"
            }
            val bindForwardMethod = sideSlotClass.methods.first {
                !AccessFlags.STATIC.isSet(it.accessFlags) &&
                    it.parameterTypes.size == 1 &&
                    it.returnType == "V" &&
                    it.implementation?.instructions?.any { ins ->
                        ins is ReferenceInstruction &&
                            (ins.reference as? MethodReference)?.let { mRef ->
                                mRef.definingClass == sideSlotClass.type &&
                                    mRef.name == getterMethod.name
                            } == true
                    } == true
            }
            val bindImpl = bindForwardMethod.implementation
            if (bindImpl != null) {
                bindForwardMethod.clearTryBlocks()
                bindForwardMethod.ensureRegisterCount(4)
                bindForwardMethod.removeInstructions(0, bindImpl.instructions.count())
                bindForwardMethod.addInstructions(
                    0,
                    """
                        invoke-virtual {p0}, ${sideSlotClass.type}->${getterMethod.name}()Lcom/bytedance/tux/icon/TuxIconView;
                        move-result-object v0
                        if-eqz v0, :cond_skip_forward
                        const/16 v1, 0x8
                        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                        :cond_skip_forward
                        return-void
                    """.trimIndent(),
                )
                println("[Direct Message Declutter] Hooked ${sideSlotClass.type}.${bindForwardMethod.name} -> Forward TuxIconView GONE.")
                patched++
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

            val isAlbumViewConfigMethod: (Method) -> Boolean = { method ->
                AccessFlags.STATIC.isSet(method.accessFlags) &&
                    method.parameterTypes.size == 3 &&
                    method.parameterTypes[1] == "Ljava/lang/Object;" &&
                    method.parameterTypes[2] == "Ljava/lang/Object;" &&
                    method.implementation?.instructions?.any { ins ->
                        val methodRef = (ins as? ReferenceInstruction)?.reference as? MethodReference
                        methodRef?.parameterTypes?.size == 1 &&
                            methodRef.parameterTypes[0] == "Lcom/bytedance/assem/arch/core/UIAssem;" &&
                            methodRef.returnType == "V"
                    } == true &&
                    method.implementation?.instructions?.any { ins ->
                        val methodRef = (ins as? ReferenceInstruction)?.reference as? MethodReference
                        methodRef?.name == "setMarginEnd"
                    } == true
            }

            // isAlbumViewConfigMethod requires STATIC and static methods are always direct methods in DEX, so iterating directMethods gives the same result without decoding virtual methods.
            val redesignedAlbumClassFp = Fingerprint(
                custom = { _, classDef ->
                    classDef.directMethods.any(isAlbumViewConfigMethod)
                },
            )
            val albumMethods = redesignedAlbumClassFp.classDef.directMethods.filter(isAlbumViewConfigMethod)
            var hookedAlbumCount = 0
            for (redesignedMethod in albumMethods) {
                val instructions = redesignedMethod.implementation?.instructions?.toList()
                if (instructions != null) {
                    val unitIndex = instructions.indexOfLast { ins ->
                        ((ins as? ReferenceInstruction)?.reference as? FieldReference)?.definingClass == "Lkotlin/Unit;"
                    }
                    if (unitIndex >= 0) {
                        redesignedMethod.ensureRegisterCount(6)
                        val layoutParamsType = "Landroid/view/ViewGroup\$LayoutParams;"
                        val marginLayoutParamsType = "Landroid/view/ViewGroup\$MarginLayoutParams;"
                        redesignedMethod.addInstructions(
                            unitIndex,
                            """
                                move-object/from16 v0, p2
                                if-eqz v0, :cond_skip_album
                                instance-of v1, v0, Landroid/view/View;
                                if-eqz v1, :cond_skip_album
                                check-cast v0, Landroid/view/View;
                                const/4 v1, 0x0
                                invoke-virtual {v0, v1}, Landroid/view/View;->setClickable(Z)V
                                invoke-virtual {v0, v1}, Landroid/view/View;->setEnabled(Z)V
                                invoke-virtual {v0, v1}, Landroid/view/View;->setScaleX(F)V
                                invoke-virtual {v0, v1}, Landroid/view/View;->setScaleY(F)V
                                const/16 v1, 0x8
                                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                                invoke-virtual {v0}, Landroid/view/View;->getLayoutParams()$layoutParamsType
                                move-result-object v1
                                if-eqz v1, :cond_skip_album
                                const/4 v2, 0x0
                                iput v2, v1, $layoutParamsType->width:I
                                iput v2, v1, $layoutParamsType->height:I
                                instance-of v2, v1, $marginLayoutParamsType
                                if-eqz v2, :cond_skip_margin
                                check-cast v1, $marginLayoutParamsType
                                const/4 v2, 0x0
                                invoke-virtual {v1, v2}, $marginLayoutParamsType->setMarginEnd(I)V
                                :cond_skip_margin
                                invoke-virtual {v0, v1}, Landroid/view/View;->setLayoutParams($layoutParamsType)V
                                :cond_skip_album
                            """.trimIndent(),
                        )
                        hookedAlbumCount++
                    }
                }
            }
            if (hookedAlbumCount > 0) {
                println("[Direct Message Declutter] Hooked redesigned input SCALING_ALBUM_BTN slot setup ($hookedAlbumCount targets) -> 0x0, scale 0, disabled, GONE.")
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

            val redesignedEmojiBtnId = 0x7f0a3aba

            val isRedesignedEmojiSetId: (Instruction, Instruction) -> Boolean = { a, b ->
                val isLiteral = (a as? NarrowLiteralInstruction)?.narrowLiteral == redesignedEmojiBtnId ||
                    (a as? WideLiteralInstruction)?.wideLiteral == redesignedEmojiBtnId.toLong()
                val methodRef = (b as? ReferenceInstruction)?.reference as? MethodReference
                isLiteral &&
                    methodRef?.definingClass == "Landroid/view/View;" &&
                    methodRef.name == "setId" &&
                    methodRef.returnType == "V" &&
                    methodRef.parameterTypes.size == 1 &&
                    methodRef.parameterTypes[0] == "I"
            }

            val isKotlinUnitField: (Instruction) -> Boolean = { ins ->
                ((ins as? ReferenceInstruction)?.reference as? FieldReference)?.definingClass == "Lkotlin/Unit;"
            }

            val redesignedEmojiFp = Fingerprint(
                returnType = "Ljava/lang/Object;",
                custom = { method, _ ->
                    AccessFlags.STATIC.isSet(method.accessFlags) &&
                        method.parameterTypes.size == 3 &&
                        method.parameterTypes[1] == "Ljava/lang/Object;" &&
                        method.parameterTypes[2] == "Ljava/lang/Object;" &&
                        method.implementation?.instructions?.zipWithNext()?.any { (a, b) -> isRedesignedEmojiSetId(a, b) } == true
                },
            )
            val redesignedMethod = redesignedEmojiFp.method
            val instructions = redesignedMethod.implementation?.instructions?.toList()
            if (instructions != null) {
                val setIdIndex = instructions.zipWithNext().indexOfFirst { (a, b) -> isRedesignedEmojiSetId(a, b) } + 1
                if (setIdIndex > 0) {
                    val unitIndex = instructions.withIndex().firstOrNull { (index, ins) ->
                        index > setIdIndex && isKotlinUnitField(ins)
                    }?.index
                    if (unitIndex != null) {
                        val layoutParamsType = "Landroid/view/ViewGroup\$LayoutParams;"
                        val marginLayoutParamsType = "Landroid/view/ViewGroup\$MarginLayoutParams;"
                        redesignedMethod.addInstructions(
                            unitIndex,
                            """
                                const/4 v0, 0x0
                                iput v0, v1, $layoutParamsType->width:I
                                iput v0, v1, $layoutParamsType->height:I
                                invoke-virtual {v1, v0}, $marginLayoutParamsType->setMarginEnd(I)V
                                invoke-virtual {p2, v1}, Landroid/view/View;->setLayoutParams($layoutParamsType)V
                                invoke-virtual {p2, v0}, Landroid/view/View;->setClickable(Z)V
                                invoke-virtual {p2, v0}, Landroid/view/View;->setEnabled(Z)V
                                invoke-virtual {p2, v0}, Landroid/view/View;->setScaleX(F)V
                                invoke-virtual {p2, v0}, Landroid/view/View;->setScaleY(F)V
                                const/16 v0, 0x8
                                invoke-virtual {p2, v0}, Landroid/view/View;->setVisibility(I)V
                            """.trimIndent(),
                        )
                        println("[Direct Message Declutter] Hooked redesigned input EMOJI_BTN slot setup -> 0x0, scale 0, disabled, GONE.")
                        patched++
                    } else {
                        println("[Direct Message Declutter] Skipped redesigned EMOJI_BTN hook: Unit return anchor not found.")
                    }
                } else {
                    println("[Direct Message Declutter] Skipped redesigned EMOJI_BTN hook: setId anchor not found.")
                }
            } else {
                println("[Direct Message Declutter] Skipped redesigned EMOJI_BTN hook: no implementation.")
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

        // 9. Feature: Hide Try Effect Button on shared video cards
        if (hideTryEffectButton == true) {
            val isTryEffectSiblingGate: (Method) -> Boolean = { m ->
                AccessFlags.STATIC.isSet(m.accessFlags) &&
                    m.returnType == "Z" &&
                    m.parameterTypes.size == 4 &&
                    m.parameterTypes[0].startsWith("L") &&
                    m.parameterTypes[1] == "Lcom/ss/android/ugc/aweme/im/chatroom/api/model/SessionInfo;" &&
                    m.parameterTypes[2] == "Ljava/lang/String;" &&
                    m.parameterTypes[3] == "Z"
            }

            val tryEffectFp = Fingerprint(
                returnType = "Z",
                custom = { method, classDef ->
                    AccessFlags.STATIC.isSet(method.accessFlags) &&
                        method.parameterTypes.size == 5 &&
                        method.parameterTypes[0] == "Landroidx/fragment/app/Fragment;" &&
                        method.parameterTypes[1].startsWith("L") &&
                        method.parameterTypes[2] == "Lcom/ss/android/ugc/aweme/im/chatroom/api/model/SessionInfo;" &&
                        method.parameterTypes[3] == "Ljava/lang/String;" &&
                        method.parameterTypes[4] == "Z" &&
                        classDef.methods.any(isTryEffectSiblingGate)
                },
            )
            val gateClass = tryEffectFp.classDef
            val method5Arg = tryEffectFp.method
            val method4Arg = gateClass.methods.first(isTryEffectSiblingGate)

            method5Arg.replaceWithReturnBoolean(false)
            println("[Direct Message Declutter] Hooked ${gateClass.type}.${method5Arg.name} -> Try effect CTA disabled.")
            patched++

            method4Arg.replaceWithReturnBoolean(false)
            println("[Direct Message Declutter] Hooked ${gateClass.type}.${method4Arg.name} -> Try effect CTA disabled.")
            patched++
        }

        // 10. Feature: Hide Sticker Reply Suggestions above input bar
        if (hideStickerReplySuggestions == true) {
            val stickerRecommendationFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/sdk/chat/feature/replytosticker/ReplyToStickerRecommendationViewModel;",
                returnType = "V",
                custom = { method, _ ->
                    AccessFlags.STATIC.isSet(method.accessFlags) &&
                        method.parameterTypes.size == 4 &&
                        method.parameterTypes[0] == "Lcom/ss/android/ugc/aweme/im/sdk/chat/feature/replytosticker/ReplyToStickerRecommendationViewModel;" &&
                        method.parameterTypes[1] == "Z" &&
                        method.parameterTypes[3] == "I"
                },
            )
            val stickerMethod = stickerRecommendationFp.method
            stickerMethod.addInstructions(
                0,
                """
                    and-int/lit8 v0, p3, 0x2
                    if-eqz v0, :keep_sticker_reply
                    return-void
                    :keep_sticker_reply
                """.trimIndent(),
            )
            println("[Direct Message Declutter] Hooked ReplyToStickerRecommendationViewModel.${stickerMethod.name} -> automatic sticker reply suggestions disabled.")
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/sdk/chat/ui/base/assems/preshown/PreshownStickerBannerProtocol;",
                name = "isEnabled",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Direct Message Declutter] Hooked PreshownStickerBannerProtocol.isEnabled -> preshown sticker reply banner disabled.")
            patched++

            val interceptFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/sdk/chat/ui/base/assems/preshown/PreshownStickerBannerProtocol;",
                name = "intercept",
                returnType = "Ljava/util/List;",
                parameters = listOf("Ljava/util/List;"),
            )
            val interceptMethod = interceptFp.method
            val interceptImpl = interceptMethod.implementation
            if (interceptImpl != null) {
                interceptMethod.clearTryBlocks()
                interceptMethod.removeInstructions(0, interceptImpl.instructions.count())
                interceptMethod.addInstructions(
                    0,
                    """
                        return-object p1
                    """.trimIndent(),
                )
                println("[Direct Message Declutter] Hooked PreshownStickerBannerProtocol.intercept -> passthrough message list.")
                patched++
            }

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
                println("[Direct Message Declutter] Hooked TypingRecommendationPanelAssem ($typingHooks dynamic methods) -> typing sticker recommendations disabled.")
                patched++
            }
        }

        println("[Direct Message Declutter] Applied $patched hooks -> direct messages decluttered.")
    }
}
