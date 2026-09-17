package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val AWEME_CLASS = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;"

private fun isTargetFeedClass(classDef: ClassDef): Boolean =
    classDef.methods.any { sibling ->
        sibling.implementation?.instructions?.any { instruction ->
            (instruction as? ReferenceInstruction)?.reference?.let { reference ->
                reference is StringReference &&
                    (reference.string == "homepage_hot" || reference.string == "FeedRecommendFragment")
            } ?: false
        } == true
    }

val showSeekbarPatch = bytecodePatch(
    name = "Show seekbar",
    description = "Restores TikTok's native video seekbar and scrubbing controls where normally hidden or disabled.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. ShouldShowProgressBar predicate -> return true when Aweme != null
        try {
            val fp = Fingerprint(
                accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
                returnType = "Z",
                parameters = listOf(AWEME_CLASS),
                custom = { method, classDef ->
                    isTargetFeedClass(classDef) && (method.implementation?.instructions?.count() ?: 0) <= 20
                },
            )
            val method = fp.method
            method.addInstructionsWithLabels(
                0,
                """
                    if-eqz p0, :show_seekbar_continue
                    const/4 p0, 0x1
                    return p0
                    :show_seekbar_continue
                    nop
                """.trimIndent(),
            )
            println("[Show Seekbar] Hooked feed ShouldShowProgressBar predicate -> Always enabled for active Aweme.")
            patched++
        } catch (e: Exception) {
            println("[Show Seekbar] ShouldShowProgressBar note: ${e.message}")
        }

        // 2. SetSeekBarShowType handler -> override hidden types (3 and 4) to visible type 0
        try {
            val fp = Fingerprint(
                strings = listOf("seekbar show type change, change to:"),
            )
            val method = fp.method
            if (method.parameterTypes.size == 1 && method.parameterTypes[0] == "I") {
                val isStatic = (method.accessFlags and AccessFlags.STATIC.value) != 0
                val paramReg = if (isStatic) "p0" else "p1"
                method.addInstructions(
                    0,
                    """
                        invoke-static {$paramReg}, ${Constants.TIKTOK_EXTENSION_SEEKBAR_HOOK}->sanitizeSeekbarShowType(I)I
                        move-result $paramReg
                    """.trimIndent(),
                )
                println("[Show Seekbar] Hooked setSeekBarShowType($paramReg) -> Safe runtime show type normalizer active.")
                patched++
            } else {
                println("[Show Seekbar] Note: setSeekBarShowType signature did not match (I)V.")
            }
        } catch (e: Exception) {
            println("[Show Seekbar] SetSeekBarShowType note: ${e.message}")
        }

        println("[Show Seekbar] Applied $patched seekbar scrubbing hook(s).")
    }
}
