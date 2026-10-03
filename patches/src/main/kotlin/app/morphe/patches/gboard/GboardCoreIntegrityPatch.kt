package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount

val gboardCoreIntegrityPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    extendWith("extensions/extension.mpe")

    execute {
        patchSignatureBypass()
        patchLauncherTrampoline()
        patchPhenotypeResilience()
    }
}

private fun BytecodePatchContext.patchSignatureBypass() {
    val fp = Fingerprint(
        returnType = "V",
        strings = listOf("APK is signed by unrecognized certificates: "),
    )
    fp.method.addInstructions(
        0,
        """
            return-void
        """.trimIndent(),
    )

    val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
    println("[Core Integrity] Neutralized signature validation check in $targetClass.${fp.method.name}()")
}

private fun BytecodePatchContext.patchLauncherTrampoline() {
    var patched = 0

    val fpOnResume = Fingerprint(
        definingClass = "Lcom/google/android/libraries/inputmethod/launcher/LauncherActivity;",
        name = "onResume",
        parameters = emptyList(),
        returnType = "V",
    )

    fpOnResume.method.apply {
        clearTryBlocks()
        ensureRegisterCount(4)
        val count = implementation?.instructions?.count() ?: 0
        if (count > 0) {
            removeInstructions(0, count)
        }
        addInstructions(
            0,
            """
                invoke-super {p0}, Landroid/app/Activity;->onResume()V
                invoke-static {p0}, ${Constants.GBOARD_EXTENSION_CLASS}->checkFirstRun(Landroid/app/Activity;)Z
                move-result v0
                if-eqz v0, :cond_morphe_first_run
                return-void
                :cond_morphe_first_run
                new-instance v0, Landroid/content/Intent;
                invoke-direct {v0}, Landroid/content/Intent;-><init>()V
                const-string v1, "com.google.android.apps.inputmethod.latin.preference.SettingsActivity"
                invoke-virtual {v0, p0, v1}, Landroid/content/Intent;->setClassName(Landroid/content/Context;Ljava/lang/String;)Landroid/content/Intent;
                const v1, 0x10008000
                invoke-virtual {v0, v1}, Landroid/content/Intent;->setFlags(I)Landroid/content/Intent;
                const-string v1, "entry"
                const/4 v2, 2
                invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;I)Landroid/content/Intent;
                invoke-virtual {p0, v0}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
                invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                return-void
            """.trimIndent(),
        )
        patched++
    }

    println("[Core Integrity] Redirected LauncherActivity ($patched hook applied -> check onboarding wizard and direct SettingsActivity launch).")
}

private fun BytecodePatchContext.patchPhenotypeResilience() {
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
    // By removing this conditional jump, any attempt to overwrite an existing default value
    // falls through cleanly to the normal exit path without throwing IllegalStateException.
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
        println("[Core Integrity] Neutralized flag reset assertion jump at opcode index $branchIndex in $targetClass.${resetCheckFp.method.name}()")
    } else {
        error("[Core Integrity] Failed to locate Phenotype flag reset branch instruction.")
    }
}
