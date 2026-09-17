package app.morphe.patches.vivaldi

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val vivaldiBackgroundMediaPatch = bytecodePatch(
    name = "Background Media Playback",
    description = "Enables uninterrupted background audio and video playback when minimizing the browser or switching tabs by disabling background media suspension.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        try {
            val fpCommandLine = Fingerprint(
                definingClass = "Lorg/chromium/base/CommandLine;",
                parameters = listOf("[Ljava/lang/String;"),
                returnType = "V",
            )

            val singletonField = fpCommandLine.originalClassDef.fields.firstOrNull {
                it.type == "Lorg/chromium/base/CommandLine;"
            }?.name ?: "d"

            val appendMethod = fpCommandLine.originalClassDef.methods.firstOrNull {
                it.parameterTypes == listOf("Ljava/lang/String;", "Ljava/lang/String;") && it.returnType == "V"
            }?.name ?: "a"

            val returnIdx = fpCommandLine.method.implementation?.instructions?.indexOfLast {
                it.opcode == Opcode.RETURN_VOID
            } ?: -1

            if (returnIdx < 0) {
                println("[Background Media Playback] Skipped: return-void instruction not found in CommandLine.init")
                return@execute
            }

            fpCommandLine.method.addInstructionsWithLabels(
                returnIdx,
                """
                    sget-object v1, Lorg/chromium/base/CommandLine;->$singletonField:Lorg/chromium/base/CommandLine;
                    if-nez v1, :skip_bg_media
                    const-string v2, "disable-background-media-suspend"
                    const/4 v3, 0x0
                    invoke-virtual {v1, v2, v3}, Lorg/chromium/base/CommandLine;->$appendMethod(Ljava/lang/String;Ljava/lang/String;)V
                    :skip_bg_media
                    nop
                """,
            )

            println("[Background Media Playback] Injected --disable-background-media-suspend via CommandLine.$appendMethod")
        } catch (e: Exception) {
            println("[Background Media Playback] CommandLine hook note: ${e.message}")
        }
    }
}
