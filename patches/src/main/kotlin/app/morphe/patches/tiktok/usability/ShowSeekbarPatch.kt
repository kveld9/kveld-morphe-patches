package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.replaceWithReturnIntegerObject
import com.android.tools.smali.dexlib2.AccessFlags

private const val AWEME_CLASS = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;"

val showSeekbarPatch = bytecodePatch(
    name = "Show Seekbar",
    description = "Restores the video progress seekbar and scrubbing controls.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_LITE)
    dependsOn(sharedExtensionPatch)

    val showThumbnailOption = booleanOption(
        key = "showThumbnail",
        default = true,
        title = "Show Thumbnail Preview",
        description = "Shows frame previews while dragging the seekbar.",
    )

    execute {
        var patched = 0
        val isLite = packageMetadata.packageName == Constants.TIKTOK_LITE_PACKAGE_NAME

        // 1. Aweme.getVideoControl() -> force draftProgressBar = 1 and showProgressBar = 1 (musically) or 2 (Lite)
        val awemeClass = if (isLite) "Lcom/ss/android/ugc/aweme/minilite/feed/model/Aweme;" else AWEME_CLASS
        val videoControlClass = if (isLite) "Lcom/ss/android/ugc/aweme/minilite/feed/model/VideoControl;" else "Lcom/ss/android/ugc/aweme/feed/model/VideoControl;"
        val showProgressBarVal = if (isLite) 2 else 1
        val fp = Fingerprint(
            definingClass = awemeClass,
            name = "getVideoControl",
            returnType = videoControlClass,
        )
        val method = fp.method
        method.ensureRegisterCount(3)
        method.addInstructionsWithLabels(
            0,
            """
                iget-object v0, p0, $awemeClass->videoControl:$videoControlClass
                if-nez v0, :show_seekbar_init
                new-instance v0, $videoControlClass
                invoke-direct {v0}, $videoControlClass-><init>()V
                iput-object v0, p0, $awemeClass->videoControl:$videoControlClass
                :show_seekbar_init
                const/4 v1, 0x1
                iput v1, v0, $videoControlClass->draftProgressBar:I
                const/4 v1, $showProgressBarVal
                iput v1, v0, $videoControlClass->showProgressBar:I
                return-object v0
            """.trimIndent(),
        )
        println("[Show Seekbar] Hooked Aweme.getVideoControl() -> Forced draftProgressBar=1 and showProgressBar=$showProgressBarVal.")
        patched++

        if (!isLite) {
            // 2. SetSeekBarShowType handler -> override hidden types (3 and 4) to visible type 0
            val fpShowType = Fingerprint(
                strings = listOf("seekbar show type change, change to:"),
            )
            val showTypeMethod = fpShowType.method
            if (showTypeMethod.parameterTypes.size == 1 && showTypeMethod.parameterTypes[0] == "I") {
                val isStatic = (showTypeMethod.accessFlags and AccessFlags.STATIC.value) != 0
                val paramReg = if (isStatic) "p0" else "p1"
                showTypeMethod.addInstructions(
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

            // 3. Seekbar Thumbnail Preview AB experiment -> force seekbar_show_thumbnail_when_drag = 1
            if (showThumbnailOption?.value != false) {
                val fpThumb = Fingerprint(
                    returnType = "Ljava/lang/Object;",
                    parameters = emptyList(),
                    strings = listOf("seekbar_show_thumbnail_when_drag"),
                )
                val thumbMethod = fpThumb.method
                thumbMethod.replaceWithReturnIntegerObject(1)
                println("[Show Seekbar] Hooked seekbar_show_thumbnail_when_drag -> Forced dragging thumbnail preview enabled.")
                patched++
            }
        }

        if (patched == 0) {
            println("[Show Seekbar] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Show Seekbar] Applied $patched seekbar hook(s).")
        }
    }
}
