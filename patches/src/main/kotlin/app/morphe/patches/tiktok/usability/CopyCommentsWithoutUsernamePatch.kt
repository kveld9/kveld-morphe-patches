package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val COMMENT_CLASS_DESCRIPTOR = "Lcom/ss/android/ugc/aweme/comment/model/Comment;"
private const val CLIP_DATA_CLASS_DESCRIPTOR = "Landroid/content/ClipData;"

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

val copyCommentsWithoutUsernamePatch = bytecodePatch(
    name = "Copy Comments Without Username",
    description = "Copies only the comment text without prepending the author username.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Primary hook: Universal ClipData builder LX/15Tr;->LIZ(String, String, List)ClipData
        // In TikTok v46.9.3, the main comment sheet copy action calls this method with
        // (authorPrefix, commentText, textExtraList). By zeroing the authorPrefix (p0 = ""),
        // the returned ClipData contains clean comment text only without the "@username: " prefix.
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
                // Method is static with 3 parameters: p0 = prefix, p1 = commentText, p2 = textExtra
                // Inject `const-string p0, ""` at method entry (index 0).
                method.addInstructions(
                    0,
                    """
                        const-string p0, ""
                    """.trimIndent(),
                )
                patched++
            }
            println("[Copy Comments Without Username] Hooked $patched ClipData comment builder method(s).")
        } catch (e: Exception) {
            println("[Copy Comments Without Username] ClipData builder hook note: ${e.message}")
        }

        // 2. Secondary fallback: BPEA clipboard helper callers (e.g. bulletin/now/favorites)
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

                // Hook clipboard helper invocations directly at call site to sanitize copied text
                helperCallIndexes.asReversed().forEach { helperCallIndex ->
                    val helperInstruction = method.getInstruction<Instruction>(helperCallIndex)
                    val isStaticInvoke = helperInstruction.opcode == Opcode.INVOKE_STATIC ||
                        helperInstruction.opcode == Opcode.INVOKE_STATIC_RANGE
                    val paramOffset = if (isStaticInvoke) 0 else 1
                    val copiedTextRegister = helperInstruction.argumentRegister(paramOffset + 1)
                        ?: throw PatchException(
                            "Copy Comments Without Username: clipboard helper call is not register-addressable in ${match.originalClassDef.type}->${method.name}.",
                        )

                    method.addCommentCopySanitizerInstructions(helperCallIndex, copiedTextRegister)
                    patched++
                }

                // Hook Comment.getText() to capture clean comment text into runtime holder
                val getTextIndex = method.findCommentGetTextInvokeIndex()
                if (getTextIndex != null) {
                    method.addCommentTextCaptureInstructions(getTextIndex)
                }
            }
        } catch (e: Exception) {
            println("[Copy Comments Without Username] Comment copy clipboard note: ${e.message}")
        }

        println("[Copy Comments Without Username] Applied $patched comment sanitization hook(s).")
    }
}
