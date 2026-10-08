package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.getReference
import app.morphe.patches.shared.sharedExtensionPatch
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnBooleanObject
import app.morphe.patches.shared.replaceWithReturnIntegerObject
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val COMMENT_CLASS_DESCRIPTOR = "Lcom/ss/android/ugc/aweme/comment/model/Comment;"
private const val CLIP_DATA_CLASS_DESCRIPTOR = "Landroid/content/ClipData;"
private const val BASE_COMMENT_CELL_CLASS = "Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;"
private const val COMMENT_LYNX_CELL_CLASS = "Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/CommentLynxCell;"
private const val COMMENT_ITEM_LIST_CLASS = "Lcom/ss/android/ugc/aweme/comment/model/CommentItemList;"
private const val COMMENT_SURPRISE_STRUCT_CLASS = "Lcom/ss/android/ugc/aweme/comment/model/CommentSurpriseStruct;"
private const val COMMENT_SURPRISE_CLASS = "Lcom/ss/android/ugc/aweme/comment/model/CommentSurprise;"
private const val COMMENT_RESPONSE_CLASS = "Lcom/ss/android/ugc/aweme/comment/model/CommentResponse;"
private const val COMMENT_PUBLISH_VIEW_MODEL_CLASS =
    "Lcom/ss/android/ugc/aweme/commentv2/commentlist/viewmodel/CommentPublishViewModel;"

private data class MethodSignature(
    val definingClass: String,
    val name: String,
    val parameters: List<String>,
    val returnType: String,
) {
    fun matches(reference: MethodReference): Boolean =
        reference.definingClass == definingClass &&
            reference.name == name &&
            reference.parameterTypes.map { it.toString() } == parameters &&
            reference.returnType == returnType
}

private fun MethodReference.isClipDataNewPlainText(): Boolean =
    definingClass == CLIP_DATA_CLASS_DESCRIPTOR &&
        name == "newPlainText" &&
        parameterTypes == listOf("Ljava/lang/CharSequence;", "Ljava/lang/CharSequence;") &&
        returnType == "Landroid/content/ClipData;"

private fun MethodReference.isCommentGetText(): Boolean =
    definingClass == COMMENT_CLASS_DESCRIPTOR &&
        name == "getText" &&
        parameterTypes.isEmpty() &&
        returnType == "Ljava/lang/String;"

private fun MethodReference.isCommentGetUser(): Boolean =
    definingClass == COMMENT_CLASS_DESCRIPTOR &&
        name == "getUser" &&
        parameterTypes.isEmpty()

private fun Method.isCommentCopyBuilder(clipboardHelper: MethodSignature): Boolean {
    val implementation = implementation ?: return false
    var hasCommentText = false
    var hasCommentUser = false
    var hasClipboardHelperCall = false

    implementation.instructions.forEach { instruction ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEach
        // This runs over every instruction in the APK; read the dex-backed class string once.
        val definingClass = reference.definingClass
        if (definingClass == COMMENT_CLASS_DESCRIPTOR) {
            if (reference.isCommentGetText()) hasCommentText = true
            if (reference.isCommentGetUser()) hasCommentUser = true
        }
        if (definingClass == clipboardHelper.definingClass && clipboardHelper.matches(reference)) {
            hasClipboardHelperCall = true
        }
    }

    return hasCommentText && hasCommentUser && hasClipboardHelperCall
}

private fun Method.findClipboardHelperCallIndexes(clipboardHelper: MethodSignature): List<Int> =
    implementation!!.instructions.withIndex().mapNotNull { (index, instruction) ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@mapNotNull null
        if (clipboardHelper.matches(reference)) index else null
    }

private fun Instruction.argumentRegister(argumentIndex: Int): Int? =
    when (this) {
        is FiveRegisterInstruction -> when (argumentIndex) {
            0 -> registerC
            1 -> registerD
            2 -> registerE
            3 -> registerF
            4 -> registerG
            else -> null
        }
        is RegisterRangeInstruction -> startRegister + argumentIndex
        else -> null
    }

private fun Method.findCommentGetTextInvokeIndex(): Int? =
    implementation?.instructions?.withIndex()?.firstOrNull { (_, instruction) ->
        ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.isCommentGetText() == true
    }?.index

private fun MutableMethod.addCommentTextCaptureInstructions(getTextInvokeIndex: Int) {
    val moveResultIndex = getTextInvokeIndex + 1
    val moveResultInstruction = getInstruction<Instruction>(moveResultIndex)
    if (moveResultInstruction.opcode != Opcode.MOVE_RESULT_OBJECT) return
    val commentTextRegister = (moveResultInstruction as OneRegisterInstruction).registerA

    if (commentTextRegister <= 15) {
        addInstructions(
            moveResultIndex + 1,
            """
                invoke-static {v$commentTextRegister}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->captureCommentText(Ljava/lang/String;)V
            """.trimIndent(),
        )
    } else {
        addInstructions(
            moveResultIndex + 1,
            """
                invoke-static/range {v$commentTextRegister .. v$commentTextRegister}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->captureCommentText(Ljava/lang/String;)V
            """.trimIndent(),
        )
    }
}

private fun MutableMethod.addCommentCopySanitizerInstructions(
    insertIndex: Int,
    copiedTextRegister: Int,
) {
    if (copiedTextRegister <= 15) {
        addInstructions(
            insertIndex,
            """
                invoke-static {v$copiedTextRegister}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->sanitizeCopiedComment(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v$copiedTextRegister
            """.trimIndent(),
        )
    } else {
        addInstructions(
            insertIndex,
            """
                invoke-static/range {v$copiedTextRegister .. v$copiedTextRegister}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->sanitizeCopiedComment(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v$copiedTextRegister
            """.trimIndent(),
        )
    }
}

private val baseCommentCellBindFingerprint = Fingerprint(
    definingClass = BASE_COMMENT_CELL_CLASS,
    returnType = "V",
    parameters = listOf("L"),
    strings = listOf("comment_panel"),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: return@Fingerprint false
        instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference != null && reference.definingClass == BASE_COMMENT_CELL_CLASS && reference.returnType == COMMENT_CLASS_DESCRIPTOR
        }
    },
)

private val commentListLoadedFingerprint = Fingerprint(
    returnType = "V",
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: return@Fingerprint false
        var hasItemsField = false
        var hasLazySplitTask = false

        for (instruction in instructions) {
            val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: continue
            if (field.definingClass != COMMENT_ITEM_LIST_CLASS) continue
            if (field.name == "items" && field.type == "Ljava/util/List;") {
                hasItemsField = true
            } else if (field.name == "lazySplitItemsParseTask") {
                hasLazySplitTask = true
            }
        }

        hasItemsField && hasLazySplitTask
    },
)

private fun BytecodePatchContext.applyCommentSortControls(): Int {
    var patched = 0

    val optionStyleFp = Fingerprint(
        returnType = "L",
        strings = listOf("comment_sort_opt_style"),
        custom = { method, _ ->
            method.parameterTypes.size <= 1 && method.parameterTypes.all { it.startsWith("L") }
        },
    )
    val method = optionStyleFp.method
    method.replaceWithReturnIntegerObject(2)
    patched++

    val styleClassType = optionStyleFp.classDef.type
    val eligibilityFp = Fingerprint(
        returnType = "Z",
        parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
        custom = { _, classDef ->
            classDef.methods.any { m ->
                m.name == "<clinit>" && m.implementation?.instructions?.any { ins ->
                    ins.opcode == Opcode.NEW_INSTANCE &&
                        ins.getReference<TypeReference>()?.type == styleClassType
                } == true
            }
        },
    )
    val eligibilityMethod = eligibilityFp.method
    eligibilityMethod.replaceWithReturnBoolean(true)
    patched++

    println("[Comment Customizer] Comment sort controls unlocked.")
    return patched
}

private fun BytecodePatchContext.applyCopyWithoutUsername(): Int {
    var patched = 0

    try {
        val clipDataBuilderFingerprint = Fingerprint(
            returnType = "Landroid/content/ClipData;",
            parameters = listOf(
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Ljava/util/List;",
            ),
            strings = listOf("copy_label"),
            custom = { method, _ ->
                (method.accessFlags and AccessFlags.STATIC.value) != 0
            },
        )

        clipDataBuilderFingerprint.matchAll().forEach { match ->
            val method = match.method
            method.addInstructions(
                0,
                """
                    const-string p0, ""
                """.trimIndent(),
            )
            patched++
        }
    } catch (_: Exception) {
    }

    try {
        val clipboardHelperMatch = Fingerprint(
            returnType = "V",
            parameters = listOf(
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Landroid/content/Context;",
                "Lcom/bytedance/bpea/basics/Cert;",
            ),
            custom = { method, _ ->
                (method.accessFlags and AccessFlags.STATIC.value) != 0 &&
                    method.implementation?.instructions?.any { instruction ->
                        ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.isClipDataNewPlainText() == true
                    } == true
            },
        ).match()

        val clipboardHelper = MethodSignature(
            clipboardHelperMatch.originalClassDef.type,
            clipboardHelperMatch.originalMethod.name,
            clipboardHelperMatch.originalMethod.parameterTypes.map { it.toString() },
            clipboardHelperMatch.originalMethod.returnType,
        )

        val commentCopyFingerprint = Fingerprint(
            custom = { method, _ ->
                method.isCommentCopyBuilder(clipboardHelper)
            },
        )

        commentCopyFingerprint.matchAll().forEach { match ->
            val method = match.method
            val helperCallIndexes = method.findClipboardHelperCallIndexes(clipboardHelper)

            helperCallIndexes.asReversed().forEach { helperCallIndex ->
                val helperInstruction = method.getInstruction<Instruction>(helperCallIndex)
                val isStaticInvoke = helperInstruction.opcode == Opcode.INVOKE_STATIC ||
                    helperInstruction.opcode == Opcode.INVOKE_STATIC_RANGE
                val paramOffset = if (isStaticInvoke) 0 else 1
                val copiedTextRegister = helperInstruction.argumentRegister(paramOffset + 1)
                    ?: throw PatchException(
                        "Comment Customizer: clipboard helper call is not register-addressable in ${match.originalClassDef.type}->${method.name}.",
                    )

                method.addCommentCopySanitizerInstructions(helperCallIndex, copiedTextRegister)
                patched++
            }

            val getTextIndex = method.findCommentGetTextInvokeIndex()
            if (getTextIndex != null) {
                method.addCommentTextCaptureInstructions(getTextIndex)
            }
        }
    } catch (_: Exception) {
    }

    println("[Comment Customizer] Clean comment copying active.")
    return patched
}

private fun BytecodePatchContext.applyDisableSuggestedEmojis(): Int {
    var patched = 0

    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/ExposedEmojiPanelTrigger;",
        returnType = "Z",
        custom = { method, _ ->
            method.parameterTypes.firstOrNull()?.toString() == "Lcom/ss/android/ugc/aweme/comment/model/CommentContextSource;"
        },
    ).method.replaceWithReturnBoolean(false)
    patched++

    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/CommentPanelFakeInput;",
        returnType = "Z",
        parameters = emptyList(),
        custom = { method, _ ->
            method.implementation?.instructions?.any { instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference?.toString() ?: ""
                ref.contains("getForceDisableExposedEmoji") || ref.contains("PersonalizedEmojiExperiment")
            } == true
        },
    ).method.replaceWithReturnBoolean(false)
    patched++

    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/comment/model/CommentKeyboardModel;",
        name = "getForceDisableExposedEmoji",
        returnType = "Z",
        parameters = emptyList(),
    ).method.replaceWithReturnBoolean(true)
    patched++

    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/comment/experiment/PersonalizedEmojiExperiment;",
        returnType = "Z",
        parameters = emptyList(),
        custom = { method, _ ->
            method.implementation?.instructions?.any { instruction ->
                (instruction as? ReferenceInstruction)?.reference?.toString()?.contains("hideExposeEmoji") == true
            } == true
        },
    ).method.replaceWithReturnBoolean(true)
    patched++

    println("[Comment Customizer] Suggested emojis bar disabled.")
    return patched
}

private fun BytecodePatchContext.applyHideCommentQuickActions(): Int {
    var patched = 0

    val assemFp = Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/BaseInputAssem;",
        name = "onViewCreated",
        parameters = listOf("Landroid/view/View;"),
        returnType = "V",
    )
    val onViewCreatedMethod = assemFp.method
    val assemClass = assemFp.classDef
    val linearLayoutField = assemClass.fields.firstOrNull {
        it.type == "Landroid/widget/LinearLayout;"
    } ?: throw PatchException("Comment Customizer: LinearLayout action field not found in BaseInputAssem.")

    val putIndex = onViewCreatedMethod.implementation?.instructions?.withIndex()?.firstOrNull { (_, ins) ->
        val field = (ins as? ReferenceInstruction)?.reference as? FieldReference
        ins.opcode == Opcode.IPUT_OBJECT &&
            field?.definingClass == assemClass.type &&
            field?.name == linearLayoutField.name
    }?.index ?: throw PatchException("Comment Customizer: Could not find ${linearLayoutField.name} assignment in onViewCreated.")

    val putInstruction = onViewCreatedMethod.getInstruction<Instruction>(putIndex)
    val reg = (putInstruction as? TwoRegisterInstruction)?.registerA
        ?: throw PatchException("Comment Customizer: Could not determine register for ${linearLayoutField.name}.")

    if (reg <= 15) {
        onViewCreatedMethod.addInstructions(
            putIndex + 1,
            """
                invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->hideCommentQuickActions(Landroid/view/View;)V
            """.trimIndent(),
        )
    } else {
        onViewCreatedMethod.addInstructions(
            putIndex + 1,
            """
                invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->hideCommentQuickActions(Landroid/view/View;)V
            """.trimIndent(),
        )
    }
    patched++

    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/comment/model/CommentKeyboardModel;",
        name = "getHideIconGroupOnAgentOpenComment",
        returnType = "Z",
        parameters = emptyList(),
    ).method.replaceWithReturnBoolean(true)
    patched++

    println("[Comment Customizer] Comment quick actions hidden.")
    return patched
}

private fun BytecodePatchContext.applyHideCommentSurveys(): Int {
    var patched = 0

    // 1. Invalidate CommentSurveyDataItem provider so no survey model is built
    Fingerprint(
        returnType = "Lcom/ss/android/ugc/aweme/comment/experiment/CommentSurveyDataItem;",
        parameters = emptyList(),
    ).method.replaceWithReturnNull()
    patched++

    // 2. Disable comment survey display eligibility check (shouldShow)
    Fingerprint(
        returnType = "Z",
        parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
        strings = listOf("shouldShow return because"),
    ).method.replaceWithReturnBoolean(false)
    patched++

    // 3. Disable pre-layout and hot feed survey insertion gate
    Fingerprint(
        returnType = "Z",
        parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
        custom = { _, classDef ->
            classDef.methods.any { m ->
                m.implementation?.instructions?.any { ins ->
                    (ins as? ReferenceInstruction)?.reference?.toString()?.contains("LynxPreLayoutManager") == true
                } == true
            }
        },
    ).method.replaceWithReturnBoolean(false)
    patched++

    // 4. Neutralize CommentLynxCell binding to prevent Lynx view inflation and telemetry
    Fingerprint(
        definingClass = COMMENT_LYNX_CELL_CLASS,
        name = "onBindItemView",
    ).method.replaceWithReturnVoid()
    patched++

    // 5. Replace CommentLynxCell item view creation with an empty hidden GONE view
    val onCreateItemViewMethod = Fingerprint(
        definingClass = COMMENT_LYNX_CELL_CLASS,
        name = "onCreateItemView",
    ).method
    val impl = onCreateItemViewMethod.implementation
    if (impl != null) {
        onCreateItemViewMethod.clearTryBlocks()
        onCreateItemViewMethod.ensureRegisterCount(3)
        onCreateItemViewMethod.removeInstructions(0, impl.instructions.count())
        onCreateItemViewMethod.addInstructions(
            0,
            """
                new-instance v0, Landroid/view/View;
                invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;
                move-result-object v1
                invoke-direct {v0, v1}, Landroid/view/View;-><init>(Landroid/content/Context;)V
                const/16 v1, 0x8
                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                return-object v0
            """.trimIndent(),
        )
        patched++
    }

    println("[Comment Customizer] In-comment surveys and feedback cards blocked.")
    return patched
}

private fun BytecodePatchContext.applyHideStoryRings(): Int {
    var patched = 0

    val avatarRingClassDef = Fingerprint(
        name = "setRingStrokeWidthByAvatarSize",
        parameters = listOf("I"),
        returnType = "V",
        custom = { _, classDef ->
            classDef.superclass == "Landroid/widget/FrameLayout;"
        },
    ).classDef

    Fingerprint(
        definingClass = avatarRingClassDef.type,
        name = "setMode",
        returnType = "V",
    ).method.replaceWithReturnVoid()
    patched++

    val drawMethod = Fingerprint(
        definingClass = avatarRingClassDef.type,
        name = "draw",
        returnType = "V",
        parameters = listOf("Landroid/graphics/Canvas;"),
    ).method
    val drawImpl = drawMethod.implementation
    if (drawImpl != null) {
        drawMethod.clearTryBlocks()
        drawMethod.ensureRegisterCount(2)
        drawMethod.removeInstructions(0, drawImpl.instructions.count())
        drawMethod.addInstructions(
            0,
            """
                invoke-super {p0, p1}, Landroid/widget/FrameLayout;->draw(Landroid/graphics/Canvas;)V
                return-void
            """.trimIndent(),
        )
        patched++
    }

    Fingerprint(
        definingClass = avatarRingClassDef.type,
        name = "onInterceptTouchEvent",
        returnType = "Z",
        parameters = listOf("Landroid/view/MotionEvent;"),
    ).method.replaceWithReturnBoolean(false)
    patched++

    println("[Comment Customizer] Profile photo story rings and click interceptors disabled on comment avatars.")
    return patched
}

private fun BytecodePatchContext.applyEnableVoiceComments(): Int {
    var patched = 0

    Fingerprint(
        returnType = "Ljava/lang/Object;",
        parameters = emptyList(),
        strings = listOf("audio_comment_publish"),
    ).method.replaceWithReturnIntegerObject(1)
    patched++

    Fingerprint(
        returnType = "Z",
        parameters = listOf("Lcom/ss/android/ugc/aweme/comment/model/CommentContextSource;"),
        strings = listOf("comment_audio_publish_entry_forbidden"),
    ).method.replaceWithReturnBoolean(true)
    patched++

    Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/comment/model/CommentKeyboardModel;",
        name = "getForceDisableCommentAudio",
        returnType = "Z",
        parameters = emptyList(),
    ).method.replaceWithReturnBoolean(false)
    patched++

    Fingerprint(
        returnType = "Z",
        parameters = listOf("Landroid/content/Context;"),
        custom = { method, classDef ->
            method.definingClass.startsWith("LX/") &&
            classDef.methods.any {
                it.returnType == "Lcom/ss/android/vesdk/VEAudioRecorder;"
            }
        },
    ).method.replaceWithReturnBoolean(true)
    patched++

    Fingerprint(
        returnType = "Ljava/lang/Object;",
        parameters = emptyList(),
        strings = listOf("comment_audio_asr_translate_enable"),
    ).method.replaceWithReturnBooleanObject(true)
    patched++

    println("[Comment Customizer] Voice comments enabled.")
    return patched
}

private fun BytecodePatchContext.applyAutoTranslate(excludedLanguages: String): Int {
    var patched = 0

    baseCommentCellBindFingerprint.match().method.apply {
        val instructions = implementation!!.instructions
        val managerMatch = instructions.withIndex().mapNotNull { (index, instruction) ->
            val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (instruction.opcode != Opcode.IPUT_OBJECT ||
                field.type != COMMENT_CLASS_DESCRIPTOR ||
                instruction !is TwoRegisterInstruction
            ) {
                return@mapNotNull null
            }

            val managerRegister = instruction.registerB
            var matchingWrites = 0
            var lastWriteIndex = index
            val searchEnd = (index + 6).coerceAtMost(instructions.lastIndex)
            for (candidateIndex in (index + 1)..searchEnd) {
                val candidate = instructions[candidateIndex]
                val candidateField = (candidate as? ReferenceInstruction)?.reference as? FieldReference
                if (candidate.opcode == Opcode.IPUT_OBJECT &&
                    candidate is TwoRegisterInstruction &&
                    candidate.registerB == managerRegister &&
                    candidateField?.definingClass == field.definingClass
                ) {
                    matchingWrites++
                    lastWriteIndex = candidateIndex
                }
            }

            if (matchingWrites >= 2) lastWriteIndex to managerRegister else null
        }.lastOrNull() ?: throw PatchException(
            "Comment Customizer: could not locate initialized native comment translation manager.",
        )

        val (managerReadyIndex, managerRegister) = managerMatch
        val hookInstructions = if (managerRegister <= 15) {
            """
                move-object/from16 v0, p0
                iget-object v0, v0, Landroidx/recyclerview/widget/RecyclerView${'$'}ViewHolder;->itemView:Landroid/view/View;
                invoke-static {v0, v$managerRegister}, ${Constants.TIKTOK_EXTENSION_COMMENT_TRANSLATE_HOOK}->registerCommentCell(Landroid/view/View;Ljava/lang/Object;)V
            """.trimIndent()
        } else {
            """
                move-object/from16 v0, p0
                iget-object v0, v0, Landroidx/recyclerview/widget/RecyclerView${'$'}ViewHolder;->itemView:Landroid/view/View;
                move-object/from16 v1, v$managerRegister
                invoke-static {v0, v1}, ${Constants.TIKTOK_EXTENSION_COMMENT_TRANSLATE_HOOK}->registerCommentCell(Landroid/view/View;Ljava/lang/Object;)V
            """.trimIndent()
        }

        addInstructions(managerReadyIndex + 1, hookInstructions)
        patched++
    }

    commentListLoadedFingerprint.match().method.apply {
        if (excludedLanguages.isNotEmpty()) {
            ensureRegisterCount(1)
            addInstructions(
                0,
                """
                    const-string v0, "$excludedLanguages"
                    invoke-static {v0}, ${Constants.TIKTOK_EXTENSION_COMMENT_TRANSLATE_HOOK}->setExcludedLanguages(Ljava/lang/String;)V
                """.trimIndent(),
            )
            println("[Comment Customizer] Pushed do-not-translate languages ($excludedLanguages).")
        }

        val match = implementation!!.instructions.withIndex()
            .firstNotNullOfOrNull { (index, instruction) ->
                val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: return@firstNotNullOfOrNull null
                if (field.definingClass == COMMENT_ITEM_LIST_CLASS &&
                    field.name == "lazySplitItemsParseTask" &&
                    instruction is TwoRegisterInstruction
                ) {
                    index to instruction.registerB
                } else null
            } ?: throw PatchException(
            "Comment Customizer: could not locate loaded comment list response.",
        )

        val (responseReadyIndex, listRegister) = match
        if (listRegister <= 15) {
            addInstruction(
                responseReadyIndex,
                "invoke-static {v$listRegister}, ${Constants.TIKTOK_EXTENSION_COMMENT_TRANSLATE_HOOK}->onCommentListLoaded(Ljava/lang/Object;)V",
            )
        } else {
            addInstruction(
                responseReadyIndex,
                "invoke-static/range {v$listRegister .. v$listRegister}, ${Constants.TIKTOK_EXTENSION_COMMENT_TRANSLATE_HOOK}->onCommentListLoaded(Ljava/lang/Object;)V",
            )
        }
        patched++
    }

    println("[Comment Customizer] Automatic batch translation active.")
    return patched
}

private fun isSendCheckMethod(ref: MethodReference): Boolean {
    if (ref.returnType != "Z") return false
    val params = ref.parameterTypes.map { it.toString() }
    if (params.size != 5) return false
    return params[1] == "Lcom/ss/android/ugc/aweme/feed/model/Aweme;" &&
        params[2] == "Ljava/lang/String;" &&
        params[3] == "Ljava/lang/String;" &&
        params[4].endsWith("/CommentContextSource;")
}

private fun hasClickCommentSendString(method: Method): Boolean {
    val instructions = method.implementation?.instructions ?: return false
    for (instruction in instructions) {
        val ref = (instruction as? ReferenceInstruction)?.reference ?: continue
        val str = when (ref) {
            is StringReference -> ref.string
            else -> ref.toString()
        }
        if (str == "click_comment_send") {
            return true
        }
    }
    return false
}

private fun BytecodePatchContext.applyCommentSendFix(): Int {
    var patched = 0

    val publishSendFp = Fingerprint(
        definingClass = COMMENT_PUBLISH_VIEW_MODEL_CLASS,
        custom = { method, _ ->
            val instructions = method.implementation?.instructions ?: return@Fingerprint false
            hasClickCommentSendString(method) && instructions.any { ins ->
                val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                (ins.opcode == Opcode.INVOKE_STATIC || ins.opcode == Opcode.INVOKE_STATIC_RANGE) &&
                    isSendCheckMethod(ref)
            }
        },
    )
    // matchAll() ignores definingClass as an index and scans every class; scoping it to the class gives the same matches.
    val matches = publishSendFp.matchAll(classDefBy(COMMENT_PUBLISH_VIEW_MODEL_CLASS))
    if (matches.size != 1) {
        throw PatchException(
            "Comment Customizer: expected exactly 1 publish entry method in CommentPublishViewModel, found ${matches.size}.",
        )
    }

    val match = matches.first()
    val method = match.method
    val instructions = method.implementation?.instructions?.toList()
        ?: throw PatchException("Comment Customizer: publish entry method has no implementation.")

    val checkCallMatches = instructions.withIndex().filter { (_, ins) ->
        val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@filter false
        (ins.opcode == Opcode.INVOKE_STATIC || ins.opcode == Opcode.INVOKE_STATIC_RANGE) &&
            isSendCheckMethod(ref)
    }
    if (checkCallMatches.size != 1) {
        throw PatchException(
            "Comment Customizer: expected exactly 1 send-check call in ${method.name}, found ${checkCallMatches.size}.",
        )
    }
    val (checkIndex, _) = checkCallMatches.first()

    // Locate the top-page screen read before the check call
    val screenReadMatches = instructions.withIndex().filter { (idx, ins) ->
        if (idx >= checkIndex) return@filter false
        val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@filter false
        val isScreenMethod = (ins.opcode == Opcode.INVOKE_VIRTUAL || ins.opcode == Opcode.INVOKE_VIRTUAL_RANGE) &&
            (ref.name == "getActivity" || (ref.name == "LIZIZ" && ref.returnType == "Landroid/app/Activity;" && ref.parameterTypes.isEmpty()))
        if (!isScreenMethod) return@filter false
        val nextIdx = idx + 1
        if (nextIdx >= instructions.size) return@filter false
        val nextIns = instructions[nextIdx]
        nextIns.opcode == Opcode.MOVE_RESULT_OBJECT
    }

    if (screenReadMatches.size != 1) {
        throw PatchException(
            "Comment Customizer: expected exactly 1 top-page screen read before check call in ${method.name}, found ${screenReadMatches.size}.",
        )
    }
    val (screenInvokeIndex, _) = screenReadMatches.first()
    val screenMoveResultIndex = screenInvokeIndex + 1
    val screenMoveResultIns = instructions[screenMoveResultIndex] as OneRegisterInstruction
    val screenReg = screenMoveResultIns.registerA

    // Locate Context-derived Activity call (e.g. LX/03o6;.LIZ:(Context)Activity) occurring between screen read and check call
    val noteActivityMatches = instructions.withIndex().filter { (idx, ins) ->
        if (idx <= screenMoveResultIndex || idx >= checkIndex) return@filter false
        val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@filter false
        val isContextToActivity = (ins.opcode == Opcode.INVOKE_STATIC || ins.opcode == Opcode.INVOKE_STATIC_RANGE) &&
            ref.parameterTypes.size == 1 &&
            ref.parameterTypes[0].endsWith("Context;") &&
            ref.returnType == "Landroid/app/Activity;"
        if (!isContextToActivity) return@filter false
        val nextIdx = idx + 1
        if (nextIdx >= instructions.size) return@filter false
        val nextIns = instructions[nextIdx]
        nextIns.opcode == Opcode.MOVE_RESULT_OBJECT
    }

    if (noteActivityMatches.size != 1) {
        throw PatchException(
            "Comment Customizer: expected exactly 1 Context-to-Activity call before check call in ${method.name}, found ${noteActivityMatches.size}.",
        )
    }
    val (noteInvokeIndex, _) = noteActivityMatches.first()
    val noteMoveResultIndex = noteInvokeIndex + 1
    val noteMoveResultIns = instructions[noteMoveResultIndex] as OneRegisterInstruction
    val activityReg = noteMoveResultIns.registerA

    // Order: insert at higher index first so preceding indices remain valid
    // 1. After Context->Activity move-result-object (vAct): noteActivity(vAct)
    val noteInstructions = if (activityReg <= 15) {
        "invoke-static {v$activityReg}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->noteActivity(Landroid/app/Activity;)V"
    } else {
        "invoke-static/range {v$activityReg .. v$activityReg}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->noteActivity(Landroid/app/Activity;)V"
    }
    method.addInstructions(noteMoveResultIndex + 1, noteInstructions)
    patched++

    // 2. After screen move-result-object (vS): null-fill substitution
    val nullFillInstructions = """
        if-nez v$screenReg, :has_screen
        invoke-static {}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->panelActivity()Landroid/app/Activity;
        move-result-object v$screenReg
    """.trimIndent()
    method.addInstructionsWithLabels(
        screenMoveResultIndex + 1,
        nullFillInstructions,
        ExternalLabel("has_screen", method.getInstruction(screenMoveResultIndex + 1)),
    )
    patched++

    println("[Comment Customizer] Comment send fix active.")
    return patched
}

private fun isCommentSurpriseStructInit(ref: MethodReference): Boolean =
    ref.definingClass == COMMENT_SURPRISE_STRUCT_CLASS &&
        ref.name == "<init>" &&
        ref.parameterTypes.map { it.toString() } == listOf(
            COMMENT_CLASS_DESCRIPTOR,
            COMMENT_SURPRISE_CLASS,
            "Z",
        ) &&
        ref.returnType == "V"

private fun isSurpriseStructInitInvoke(ins: Instruction): Boolean {
    if (ins.opcode.referenceType != ReferenceType.METHOD) return false
    val ref = (ins as ReferenceInstruction).reference as MethodReference
    if (ref.name != "<init>") return false
    return isCommentSurpriseStructInit(ref)
}

private fun findCommentSurpriseStructInitIndexes(method: Method): List<Int> =
    method.implementation?.instructions?.withIndex()?.mapNotNull { (index, ins) ->
        val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@mapNotNull null
        if ((ins.opcode == Opcode.INVOKE_DIRECT || ins.opcode == Opcode.INVOKE_DIRECT_RANGE) &&
            isCommentSurpriseStructInit(ref)
        ) {
            index
        } else {
            null
        }
    } ?: emptyList()

private fun isPlayMethod(ref: MethodReference): Boolean {
    if (ref.returnType != "V") return false
    val params = ref.parameterTypes.map { it.toString() }
    if (params.size != 3) return false
    return params[0] == COMMENT_SURPRISE_STRUCT_CLASS &&
        params[1] == "I" &&
        params[2] == "Ljava/lang/String;"
}

private fun readsCommentItemListSurprise(instructions: Iterable<Instruction>): Boolean =
    instructions.any { ins ->
        val field = (ins as? ReferenceInstruction)?.reference as? FieldReference ?: return@any false
        field.definingClass == COMMENT_ITEM_LIST_CLASS && field.name == "commentSurprise"
    }

private fun readsCommentResponseSurprise(instructions: Iterable<Instruction>): Boolean =
    instructions.any { ins ->
        val field = (ins as? ReferenceInstruction)?.reference as? FieldReference ?: return@any false
        (field.definingClass == COMMENT_RESPONSE_CLASS || field.definingClass.endsWith("/CommentResponse;")) &&
            field.name == "commentSurprise"
    }

private fun hasMilestoneMarker(instructions: Iterable<Instruction>): Boolean =
    instructions.any { ins ->
        val ref = (ins as? ReferenceInstruction)?.reference
        val fieldName = (ref as? FieldReference)?.name ?: ""
        val str = when (ref) {
            is StringReference -> ref.string
            else -> ref?.toString() ?: ""
        }
        fieldName == "FIRST_COMMENT_MILESTONE" || fieldName.contains("FIRST_COMMENT_MILESTONE") ||
            str.contains("FIRST_COMMENT_MILESTONE")
    }

private fun callsLruCacheGet(instructions: Iterable<Instruction>): Boolean =
    instructions.any { ins ->
        val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
        ref.definingClass == "Landroid/util/LruCache;" && ref.name == "get"
    }

private fun BytecodePatchContext.applyHideCommentPopupAds(): Int {
    var patched = 0

    // Root constructor hook: prepend filterSurprise(p2)
    val structInitFp = Fingerprint(
        definingClass = COMMENT_SURPRISE_STRUCT_CLASS,
        name = "<init>",
        parameters = listOf(
            COMMENT_CLASS_DESCRIPTOR,
            COMMENT_SURPRISE_CLASS,
            "Z",
        ),
        returnType = "V",
    )
    val structInitMethod = structInitFp.method
    structInitMethod.addInstructions(
        0,
        """
            invoke-static/range {p2 .. p2}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->filterSurprise(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object p2
            check-cast p2, $COMMENT_SURPRISE_CLASS
        """.trimIndent(),
    )
    patched++

    // One walk over all methods instead of three; each path mark still applies its own predicate at its original point.
    val surpriseStructCandidates = Fingerprint(custom = { method, _ -> method.implementation?.instructions?.any(::isSurpriseStructInitInvoke) == true }).matchAll().map { it.method }

    // Path mark (i): page-loader method
    val pageLoaderMatchesPredicate: (Method) -> Boolean = pageLoaderMatchesPredicate@{ method ->
        val instructions = method.implementation?.instructions ?: return@pageLoaderMatchesPredicate false
        val hasStructInit = instructions.any { ins ->
            val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
            isCommentSurpriseStructInit(ref)
        }
        hasStructInit &&
            readsCommentItemListSurprise(instructions) &&
            instructions.any { ins ->
                val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                isPlayMethod(ref)
            }
    }
    val pageLoaderMatches = surpriseStructCandidates.filter(pageLoaderMatchesPredicate)
    if (pageLoaderMatches.size != 1) {
        throw PatchException(
            "Comment Customizer: expected exactly 1 page-loader surprise method, found ${pageLoaderMatches.size}.",
        )
    }
    val pageLoaderMethod = pageLoaderMatches.first()
    val pageLoaderInits = findCommentSurpriseStructInitIndexes(pageLoaderMethod)
    if (pageLoaderInits.size != 1) {
        throw PatchException(
            "Comment Customizer: expected exactly 1 CommentSurpriseStruct.<init> invoke in page-loader, found ${pageLoaderInits.size}.",
        )
    }
    pageLoaderMethod.addInstructions(
        pageLoaderInits.first(),
        """
            invoke-static {}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->markPageLoaderSurprise()V
        """.trimIndent(),
    )
    patched++

    // Path mark (ii): publish-response method
    val publishResponseMatchesPredicate: (Method) -> Boolean = publishResponseMatchesPredicate@{ method ->
        val instructions = method.implementation?.instructions ?: return@publishResponseMatchesPredicate false
        val hasStructInit = instructions.any { ins ->
            val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
            isCommentSurpriseStructInit(ref)
        }
        hasStructInit && readsCommentResponseSurprise(instructions)
    }
    val publishResponseMatches = surpriseStructCandidates.filter(publishResponseMatchesPredicate)
    if (publishResponseMatches.size != 1) {
        throw PatchException(
            "Comment Customizer: expected exactly 1 publish-response surprise method, found ${publishResponseMatches.size}.",
        )
    }
    val publishResponseMethod = publishResponseMatches.first()
    val publishResponseInits = findCommentSurpriseStructInitIndexes(publishResponseMethod)
    if (publishResponseInits.size != 1) {
        throw PatchException(
            "Comment Customizer: expected exactly 1 CommentSurpriseStruct.<init> invoke in publish-response, found ${publishResponseInits.size}.",
        )
    }
    publishResponseMethod.addInstructions(
        publishResponseInits.first(),
        """
            invoke-static {}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->markPublishResponseSurprise()V
        """.trimIndent(),
    )
    patched++

    // Path mark (iii): milestone method
    val milestoneMatchesPredicate: (Method) -> Boolean = milestoneMatchesPredicate@{ method ->
        val instructions = method.implementation?.instructions ?: return@milestoneMatchesPredicate false
        val hasStructInit = instructions.any { ins ->
            val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
            isCommentSurpriseStructInit(ref)
        }
        hasStructInit && hasMilestoneMarker(instructions) && callsLruCacheGet(instructions)
    }
    val milestoneMatches = surpriseStructCandidates.filter(milestoneMatchesPredicate)
    if (milestoneMatches.size != 1) {
        throw PatchException(
            "Comment Customizer: expected exactly 1 milestone surprise method, found ${milestoneMatches.size}.",
        )
    }
    val milestoneMethod = milestoneMatches.first()
    val milestoneInits = findCommentSurpriseStructInitIndexes(milestoneMethod)
    if (milestoneInits.size != 1) {
        throw PatchException(
            "Comment Customizer: expected exactly 1 CommentSurpriseStruct.<init> invoke in milestone, found ${milestoneInits.size}.",
        )
    }
    milestoneMethod.addInstructions(
        milestoneInits.first(),
        """
            invoke-static {}, ${Constants.TIKTOK_EXTENSION_COMMENT_HOOK}->markMilestoneSurprise()V
        """.trimIndent(),
    )
    patched++

    println("[Comment Customizer] Comment popup ads blocked.")
    return patched
}

val commentCustomizerPatch = bytecodePatch(
    name = "Comment Customizer",
    description = "Customizes TikTok's comment section, including native sort controls, clean text copying, disabling suggested emojis bar, hiding comment quick actions, hiding in-comment surveys and feedback cards, hiding profile photo story rings, enabling voice comments, automatic comment translation, fixing silent comment drops, and hiding comment popup ads.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val commentSortControls by booleanOption(
        key = "commentSortControls",
        default = true,
        title = "Comment Sort Controls",
        description = "Unlocks TikTok's native comment sorting menu (Hot, Newest, Creator only, With media) across all posts.",
        required = false,
    )

    val commentSendFix by booleanOption(
        key = "commentSendFix",
        default = true,
        title = "Fix Silent Comment Drops",
        description = "Prevents silent comment publishing drops when the top-page screen context is detached or null.",
        required = false,
    )

    val copyWithoutUsername by booleanOption(
        key = "copyWithoutUsername",
        default = true,
        title = "Copy Comments Without Username",
        description = "Copies only the comment text without prepending the author username.",
        required = false,
    )

    val disableSuggestedEmojis by booleanOption(
        key = "disableSuggestedEmojis",
        default = true,
        title = "Disable Suggested Emojis",
        description = "Removes the horizontal bar of suggested quick emojis displayed above the comment input box.",
        required = false,
    )

    val hideCommentQuickActions by booleanOption(
        key = "hideCommentQuickActions",
        default = true,
        title = "Hide Comment Quick Actions",
        description = "Hides the quick action buttons (photo, emoji, and mention) inside the comment input bar.",
        required = false,
    )

    val hideCommentSurveys by booleanOption(
        key = "hideCommentSurveys",
        default = true,
        title = "Hide Comment Surveys & Feedback Cards",
        description = "Hides surveys, opinion questionnaires, and feedback cards embedded within comment lists.",
        required = false,
    )

    val hideStoryRings by booleanOption(
        key = "hideStoryRings",
        default = true,
        title = "Hide Comment Story Rings",
        description = "Removes profile photo story rings from avatars in the comment section.",
        required = false,
    )

    val enableVoiceComments by booleanOption(
        key = "enableVoiceComments",
        default = true,
        title = "Enable Voice Comments",
        description = "Forces the native voice comment recording button in comment input bars, bypassing regional rollout restrictions and remote server blocks.",
        required = false,
    )

    val hideCommentPopupAds by booleanOption(
        key = "hideCommentPopupAds",
        default = true,
        title = "Hide Comment Popup Ads",
        description = "Suppresses promotional brand surprise animation popups and commercial campaign effects when loading comments or publishing.",
        required = false,
    )

    val autoTranslate by booleanOption(
        key = "autoTranslate",
        default = false,
        title = "Auto-Translate Comments",
        description = "Automatically translates comments into your preferred language using TikTok's native translation engine.",
        required = false,
    )

    val translationExcludedLanguages by stringOption(
        key = "translationExcludedLanguages",
        title = "Do-Not-Translate Languages",
        description = "Comma-separated ISO 639 codes (e.g. 'en,es,zh') whose comments keep their original text. Applies on top of TikTok's native do-not-translate list and only matters when Auto-Translate Comments is enabled.",
        default = "",
        required = false,
    )

    execute {
        if (commentSortControls != true &&
            copyWithoutUsername != true &&
            disableSuggestedEmojis != true &&
            hideCommentQuickActions != true &&
            hideCommentSurveys != true &&
            hideStoryRings != true &&
            enableVoiceComments != true &&
            autoTranslate != true &&
            commentSendFix != true &&
            hideCommentPopupAds != true
        ) {
            println("[Comment Customizer] Skipped: All comment customization options are disabled.")
            return@execute
        }

        var patched = 0

        if (commentSortControls == true) {
            patched += applyCommentSortControls()
        }

        if (commentSendFix == true) {
            patched += applyCommentSendFix()
        }

        if (copyWithoutUsername == true) {
            patched += applyCopyWithoutUsername()
        }

        if (disableSuggestedEmojis == true) {
            patched += applyDisableSuggestedEmojis()
        }

        if (hideCommentQuickActions == true) {
            patched += applyHideCommentQuickActions()
        }

        if (hideCommentSurveys == true) {
            patched += applyHideCommentSurveys()
        }

        if (hideStoryRings == true) {
            patched += applyHideStoryRings()
        }

        if (enableVoiceComments == true) {
            patched += applyEnableVoiceComments()
        }

        if (hideCommentPopupAds == true) {
            patched += applyHideCommentPopupAds()
        }

        if (autoTranslate == true) {
            val normalizedExclusions = (translationExcludedLanguages ?: "")
                .split(Regex("[,;\\s]+"))
                .map { it.lowercase().substringBefore('-').substringBefore('_') }
                .filter { it.matches(Regex("^[a-z]{2,3}$")) && it != "und" }
                .distinct()
                .sorted()
                .joinToString(",")
            patched += applyAutoTranslate(normalizedExclusions)
        }

        println("[Comment Customizer] Applied $patched comment customization hook(s).")
    }
}
