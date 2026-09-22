package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val BASE_COMMENT_CELL_CLASS = "Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;"
private const val COMMENT_ITEM_LIST_CLASS = "Lcom/ss/android/ugc/aweme/comment/model/CommentItemList;"
private const val COMMENT_CLASS = "Lcom/ss/android/ugc/aweme/comment/model/Comment;"

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

val commentAutoTranslatePatch = bytecodePatch(
    name = "Auto-translate comments",
    description = "Automatically translates comments into your preferred language using TikTok's native translation engine.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Hook BaseCommentCell to bind comment View and native translation manager
        try {
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
                    "Auto-translate comments: could not locate initialized native comment translation manager.",
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
                println("[Auto-translate comments] Hooked BaseCommentCell.bind -> cell registration active.")
            }
        } catch (e: Exception) {
            println("[Auto-translate comments] BaseCommentCell hook note: ${e.message}")
        }

        // 2. Hook CommentList.onLoaded to capture and queue incoming comment batches
        try {
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
                    "Auto-translate comments: could not locate loaded comment list response.",
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
                println("[Auto-translate comments] Hooked CommentList.onLoaded -> automatic batch translation active.")
            }
        } catch (e: Exception) {
            println("[Auto-translate comments] CommentList hook note: ${e.message}")
        }

        println("[Auto-translate comments] Applied $patched comment translation hook(s).")
    }
}
