package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.getReference
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnBooleanObject
import app.morphe.patches.shared.replaceWithReturnIntegerObject
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val COMMENT_CLASS_DESCRIPTOR = "Lcom/ss/android/ugc/aweme/comment/model/Comment;"
private const val CLIP_DATA_CLASS_DESCRIPTOR = "Landroid/content/ClipData;"
private const val BASE_COMMENT_CELL_CLASS = "Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;"
private const val COMMENT_ITEM_LIST_CLASS = "Lcom/ss/android/ugc/aweme/comment/model/CommentItemList;"
private const val COMMENT_CLASS = "Lcom/ss/android/ugc/aweme/comment/model/Comment;"

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
        if (reference.isCommentGetText()) hasCommentText = true
        if (reference.isCommentGetUser()) hasCommentUser = true
        if (clipboardHelper.matches(reference)) hasClipboardHelperCall = true
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
            reference != null && reference.definingClass == BASE_COMMENT_CELL_CLASS && reference.returnType == COMMENT_CLASS
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

private fun BytecodePatchContext.applyAutoTranslate(): Int {
    var patched = 0

    baseCommentCellBindFingerprint.match().method.apply {
        val instructions = implementation!!.instructions
        val managerMatch = instructions.withIndex().mapNotNull { (index, instruction) ->
            val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: return@mapNotNull null
            if (instruction.opcode != Opcode.IPUT_OBJECT ||
                field.type != COMMENT_CLASS ||
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

val commentCustomizerPatch = bytecodePatch(
    name = "Comment Customizer",
    description = "Customizes TikTok's comment section, including native sort controls, clean text copying, disabling suggested emojis bar, enabling voice comments, and automatic comment translation.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    extendWith("extensions/extension.mpe")

    val commentSortControls by booleanOption(
        key = "commentSortControls",
        default = true,
        title = "Comment Sort Controls",
        description = "Unlocks TikTok's native comment sorting menu (Hot, Newest, Creator only, With media) across all posts.",
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

    val enableVoiceComments by booleanOption(
        key = "enableVoiceComments",
        default = true,
        title = "Enable Voice Comments",
        description = "Forces the native voice comment recording button in comment input bars, bypassing regional rollout restrictions and remote server blocks.",
        required = false,
    )

    val autoTranslate by booleanOption(
        key = "autoTranslate",
        default = false,
        title = "Auto-Translate Comments",
        description = "Automatically translates comments into your preferred language using TikTok's native translation engine.",
        required = false,
    )

    execute {
        if (commentSortControls != true &&
            copyWithoutUsername != true &&
            disableSuggestedEmojis != true &&
            enableVoiceComments != true &&
            autoTranslate != true
        ) {
            println("[Comment Customizer] Skipped: All comment customization options are disabled.")
            return@execute
        }

        var patched = 0

        if (commentSortControls == true) {
            patched += applyCommentSortControls()
        }

        if (copyWithoutUsername == true) {
            patched += applyCopyWithoutUsername()
        }

        if (disableSuggestedEmojis == true) {
            patched += applyDisableSuggestedEmojis()
        }

        if (enableVoiceComments == true) {
            patched += applyEnableVoiceComments()
        }

        if (autoTranslate == true) {
            patched += applyAutoTranslate()
        }

        println("[Comment Customizer] Applied $patched comment customization hook(s).")
    }
}
