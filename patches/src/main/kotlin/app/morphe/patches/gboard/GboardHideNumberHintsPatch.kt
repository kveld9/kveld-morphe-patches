package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.cleanClassName
import app.morphe.patches.shared.sharedExtensionPatch

val gboardHideNumberHintsPatch = bytecodePatch(
    name = "Hide Number Hints",
    description = "Hides the small digit labels above the top letter row and re-centers letters vertically by collapsing the empty hint slot.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(sharedExtensionPatch)

    dependsOn(gboardCoreIntegrityPatch)

    execute {
        // SoftKeyView.q(SoftKeyDef, J) is the canonical key bind entry point
        // (it opens with the "SoftKeyView.setSoftKeyDef" trace section). The
        // small secondary labels (1-0 above the letter row) live in
        // SoftKeyDef.h, while long-press behavior lives in SoftKeyDef.g, so
        // sanitizing digit labels before the bind hides the hints without
        // touching long-press input handling.
        var patched = 0
        val fp = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/widgets/SoftKeyView;",
            name = "q",
            parameters = listOf("Lcom/google/android/libraries/inputmethod/metadata/SoftKeyDef;", "J"),
            returnType = "Z",
        )
        fp.method.addInstructions(
            0,
            """
                invoke-static {p0, p1}, ${Constants.GBOARD_EXTENSION_CLASS}->sanitizeNumberHints(Ljava/lang/Object;Ljava/lang/Object;)V
            """.trimIndent(),
        )
        patched++

        val targetClass = cleanClassName(fp.originalClassDef.type)
        println("[Hide Number Hints] Hooked key bind in $targetClass.q() ($patched hook applied -> digit labels collapsed and letter recentered).")
    }
}
