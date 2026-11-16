package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

val hideCommentButtonPatch = bytecodePatch(
    name = "Hide Comment Button",
    description = "Hides the comment button and count from videos in the feed.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK_LITE)

    execute {
        var patched = 0

        // 1. Hook Lite Standard feed mode comment presenter (LX/FrH;)
        // Resolves the presenter method returning "right_container_comment".
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
            error("[Hide Comment Button] Failed to resolve container View field in ${commentDef.type}")
        }

        // 1a. Inject setVisibility(GONE) at the end of <init>
        val returnIndex = initInstructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        if (returnIndex < 0) {
            error("[Hide Comment Button] Failed to find RETURN_VOID in ${commentDef.type}.<init>")
        }
        initMethod.addInstructions(
            returnIndex,
            """
                iget-object v0, p0, ${commentDef.type}->$containerFieldName:Landroid/view/View;
                if-eqz v0, :cond_init_skip
                const/16 v1, 0x8
                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                :cond_init_skip
            """.trimIndent(),
        )
        println("[Hide Comment Button] Hooked ${commentDef.type}.<init> -> Container View default set to GONE.")
        patched++

        // 1b. Override bind method to keep View GONE and clear click/touch listeners
        val bindMethod = commentDef.methods.firstOrNull {
            it.returnType == "V" && it.parameterTypes == listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;")
        } ?: error("[Hide Comment Button] Failed to find bind method in ${commentDef.type}")

        bindMethod.clearTryBlocks()
        bindMethod.ensureRegisterCount(4)
        val bindCount = bindMethod.implementation!!.instructions.count()
        bindMethod.removeInstructions(0, bindCount)
        bindMethod.addInstructions(
            0,
            """
                iget-object v0, p0, ${commentDef.type}->$containerFieldName:Landroid/view/View;
                if-eqz v0, :cond_bind_skip
                const/16 v1, 0x8
                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                const/4 v1, 0x0
                invoke-virtual {v0, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
                invoke-virtual {v0, v1}, Landroid/view/View;->setOnTouchListener(Landroid/view/View${'$'}OnTouchListener;)V
                :cond_bind_skip
                return-void
            """.trimIndent(),
        )
        println("[Hide Comment Button] Hooked ${commentDef.type}.${bindMethod.name}(Aweme) -> Bind replaced with GONE + null click/touch listeners.")
        patched++

        // 2. Hook Lite MiniLite feed mode comment presenter (LX/Bwc;)
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

        // 2a. Re-write LBL to set ViewGroup to GONE before returning
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
        println("[Hide Comment Button] Hooked ${miniLiteDef.type}.${miniLiteMethod.name}(View)ViewGroup -> ViewGroup set to GONE before return.")
        patched++

        // 2b. Neutralize text and icon updating methods in MiniLite presenter
        var miniLiteHelpersHooked = 0
        for (m in miniLiteDef.methods) {
            if (m.name != "<init>" && m.returnType == "V" && m.parameterTypes.isEmpty()) {
                m.replaceWithReturnVoid()
                miniLiteHelpersHooked++
            }
        }
        if (miniLiteHelpersHooked == 0) {
            error("[Hide Comment Button] Failed to find ()V helper methods in ${miniLiteDef.type}")
        }
        println("[Hide Comment Button] Hooked $miniLiteHelpersHooked helper method(s) in ${miniLiteDef.type} -> return-void.")
        patched += miniLiteHelpersHooked

        println("[Hide Comment Button] Applied $patched hooks -> Comment button disabled.")
    }
}
