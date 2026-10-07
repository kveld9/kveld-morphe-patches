package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.cleanClassName
import app.morphe.patches.shared.sharedExtensionPatch

val gboardHideNumberHintsPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(sharedExtensionPatch)

    dependsOn(gboardCoreIntegrityPatch)

    execute {
        // Lftl.d() paints the small secondary labels (1-0 hints above Q-P) via
        // SoftKeyView.p(hintViewId, label). It only writes presentation state;
        // long-press input handling (SoftKeyView.A/k) is untouched, so the
        // long-press action keeps working while the hint stays hidden.
        val fp = Fingerprint(
            definingClass = "Lftl;",
            name = "d",
            parameters = emptyList(),
            returnType = "V",
        )
        fp.method.addInstructions(
            0,
            """
                invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isHideNumberHintsEnabled()Z
                move-result v0
                if-eqz v0, :cond_skip_morphe_hide_hints
                return-void
                :cond_skip_morphe_hide_hints
            """.trimIndent(),
        )

        val targetClass = cleanClassName(fp.originalClassDef.type)
        println("[Hide Number Hints] Hooked hint painter in $targetClass.d() -> secondary labels controlled by preference.")
    }
}
