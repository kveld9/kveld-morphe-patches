package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils

val gboardPhenotypeResiliencePatch = bytecodePatch(
    name = "Phenotype Flag Resilience",
    description = "Neutralizes Phenotype flag registration conflicts to allow runtime flag overrides without crashes.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val resetCheckFp = Fingerprint(
            returnType = "Z",
            parameters = listOf("Ljava/lang/Object;", "Z"),
            filters = listOf(
                string("Resetting default value is disallowed ["),
                string("]."),
            ),
        )

        // Find the conditional branch immediately following Objects.deepEquals
        // that jumps to the "Resetting default value is disallowed" IllegalStateException block.
        // In Gboard 18.2.4:
        //   [08] invoke-static v4, v6, Lj$/util/Objects;->deepEquals(Ljava/lang/Object; Ljava/lang/Object;)Z
        //   [09] move-result v6
        //   [10] if-eqz v6, +027h (jumps to [30] -> throw IllegalStateException)
        // By removing this conditional jump, any attempt to overwrite an existing default value
        // falls through cleanly to the compiler-provided normal exit path ([28] monitor-exit; [29] return false)
        // without throwing IllegalStateException, without monitor imbalance, and without try-block corruption.
        val instructions = resetCheckFp.method.instructions
        val deepEqualsIndex = instructions.indices.firstOrNull { idx ->
            val op0 = instructions[idx].opcode.name.lowercase()
            idx + 2 < instructions.size &&
                op0.contains("invoke-static") &&
                instructions[idx + 1].opcode.name.lowercase().contains("move-result") &&
                instructions[idx + 2].opcode.name.lowercase().startsWith("if-")
        }

        if (deepEqualsIndex != null) {
            val branchIndex = deepEqualsIndex + 2
            resetCheckFp.method.removeInstruction(branchIndex)
            val targetClass = LocaleUtils.cleanClassName(resetCheckFp.originalClassDef.type)
            println("[Phenotype Resilience] Neutralized flag reset assertion jump at opcode index $branchIndex in $targetClass.${resetCheckFp.method.name}()")
        } else {
            println("[Phenotype Resilience] Warning: Could not locate flag reset branch instruction.")
        }
    }
}
