package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags

val skipContentWarningsPatch = bytecodePatch(
    name = "Skip Content Warnings",
    description = "Bypasses and clears sensitive content warnings, graphic media blur overlays, and age gates on feed videos.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        val fp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoVM;",
            custom = { method, _ ->
                method.name == "paramSync2StateAccept" &&
                    "Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;" in method.parameterTypes
            },
        )
        val method = try {
            fp.method
        } catch (e: Exception) {
            throw PatchException("Anchor VideoAuthorInfoVM.paramSync2StateAccept(VideoItemParams) not found for Skip Content Warnings", e)
        }

        val vipType = "Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"
        val paramTypeStrings = method.parameterTypes.map { it.toString() }
        val vipIndex = paramTypeStrings.indexOf(vipType)
        if (vipIndex < 0) throw PatchException("VideoItemParams parameter not found in VideoAuthorInfoVM.paramSync2StateAccept")
        var slot = if (AccessFlags.STATIC.isSet(method.accessFlags)) 0 else 1
        for (i in 0 until vipIndex) { slot += 1; val t = paramTypeStrings[i]; if (t == "J" || t == "D") slot += 1 }
        val paramReg = "p$slot"
        method.addInstructions(
            0,
            "invoke-static/range {$paramReg .. $paramReg}, ${Constants.TIKTOK_EXTENSION_SENSITIVE_WARNINGS_HOOK}->clear(Ljava/lang/Object;)V",
        )
        patched++

        println("[Skip Content Warnings] Applied $patched content warning hook(s).")
    }
}
