package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

val alwaysShowPublishDatePatch = bytecodePatch(
    name = "Always show publish date",
    description = "Forces video publish/upload date to remain visible in video author information across all feed types.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        try {
            val fp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoVM;",
                custom = { method, _ ->
                    method.name == "paramSync2StateAccept" &&
                        "Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;" in method.parameterTypes
                },
            )
            val method = fp.method
            val instructions = method.implementation!!.instructions

            val regionStart = instructions.indexOfFirst { instruction ->
                val reference = (instruction as? ReferenceInstruction)?.reference
                reference is StringReference && reference.string == "v3"
            }
            val regionEnd = instructions.withIndex().indexOfFirst { (index, instruction) ->
                if (index <= regionStart) return@indexOfFirst false
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                reference?.name == "getCreateTime" &&
                    reference.parameterTypes.isEmpty() &&
                    reference.returnType == "J"
            }

            if (regionStart >= 0 && regionEnd > regionStart) {
                val gateCallIndices = instructions.withIndex()
                    .filter { (index, instruction) ->
                        index in regionStart..regionEnd &&
                            (instruction as? Instruction)?.opcode == Opcode.INVOKE_STATIC &&
                            ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.let { ref ->
                                ref.returnType == "Z" && ref.parameterTypes == listOf("Ljava/lang/String;")
                            } == true
                    }
                    .filter { (index, _) -> method.getInstruction(index + 1).opcode == Opcode.MOVE_RESULT }
                    .map { it.index }

                gateCallIndices.asReversed().forEach { index ->
                    val resultRegister = method.getInstruction<OneRegisterInstruction>(index + 1).registerA
                    method.addInstructions(
                        index + 2,
                        """
                            const/16 v$resultRegister, 0x1
                        """.trimIndent(),
                    )
                    patched++
                }
                println("[Always Show Publish Date] Unblocked $patched video author post-time visibility gates.")
            } else {
                println("[Always Show Publish Date] Note: post-time visibility region boundary not located.")
            }
        } catch (e: Exception) {
            println("[Always Show Publish Date] VideoAuthorInfoVM note: ${e.message}")
        }

        println("[Always Show Publish Date] Applied $patched publish date visibility hook(s).")
    }
}
