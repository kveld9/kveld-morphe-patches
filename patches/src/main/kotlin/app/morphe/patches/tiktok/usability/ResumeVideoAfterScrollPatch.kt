package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.getReference
import app.morphe.patches.shared.replaceWithReturnBooleanObject
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val resumeVideoAfterScrollPatch = bytecodePatch(
    name = "Resume Video After Scroll",
    description = "Remembers playback timestamp when scrolling away and resumes from where playback stopped upon returning.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. Force FeedPlayProgressContinueConfig gate to return true
        val gateFp = Fingerprint(
            name = "invoke",
            returnType = "L",
            parameters = emptyList(),
            custom = { method, _ ->
                method.implementation?.instructions?.any { instruction ->
                    val field = instruction.getReference<FieldReference>()
                    field?.definingClass?.contains("FeedPlayProgressContinueConfig") == true &&
                        field.name == "enable"
                } == true
            },
        )
        val gateMethod = gateFp.method
        gateMethod.replaceWithReturnBooleanObject(true)
        println("[Resume Video After Scroll] Hooked continue gate (${gateMethod.definingClass}->${gateMethod.name}) -> Forced enable=true.")
        patched++

        // 2. Allow progress continuation across all feeds (not just landscape_change_keep_tag)
        val resumePositionFp = Fingerprint(
            returnType = "J",
            parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;", "Ljava/lang/String;"),
            strings = listOf("landscape_change_keep_tag"),
        )
        val positionMethod = resumePositionFp.method
        val instructions = positionMethod.implementation?.instructions?.toList() ?: emptyList()
        val containsIndex = instructions.indexOfFirst { ins ->
            val ref = ins.getReference<MethodReference>()
            ref?.definingClass == "Ljava/util/List;" && ref.name == "contains"
        }
        check(containsIndex >= 0) {
            "Could not find List.contains call in ${positionMethod.definingClass}->${positionMethod.name}"
        }

        val moveResultIdx = (containsIndex + 1 until instructions.size).firstOrNull {
            instructions[it].opcode == Opcode.MOVE_RESULT
        }
        check(moveResultIdx != null) {
            "Could not find move-result instruction following List.contains in ${positionMethod.definingClass}->${positionMethod.name}"
        }

        val moveResultIns = instructions[moveResultIdx] as OneRegisterInstruction
        val resultReg = moveResultIns.registerA
        positionMethod.addInstructions(
            moveResultIdx + 1,
            """
                const/4 v$resultReg, 0x1
            """.trimIndent(),
        )
        println("[Resume Video After Scroll] Hooked feed event type check in ${positionMethod.definingClass}->${positionMethod.name} -> Allowed across all feeds.")
        patched++

        println("[Resume Video After Scroll] Applied $patched resume playback hook(s).")
    }
}
