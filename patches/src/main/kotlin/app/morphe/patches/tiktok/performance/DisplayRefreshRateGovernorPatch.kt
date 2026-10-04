package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode

val displayRefreshRateGovernorPatch = bytecodePatch(
    name = "Display Refresh Rate Governor",
    description = "Forces TikTok to run at peak display refresh rate (120Hz/90Hz/60Hz) and neutralizes video playback framerate downclocking routines.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val targetRate by stringOption(
        key = "targetRate",
        title = "Target Refresh Rate",
        description = "Select target display refresh rate: max (display peak rate), 120 (120 Hz), 90 (90 Hz), or 60 (60 Hz). Clamped automatically to display maximum.",
        default = "max",
        required = false,
    )

    execute {
        val rateFloat = when (val trimmed = targetRate?.trim()?.lowercase()) {
            null, "", "max" -> 0.0f
            else -> trimmed.toFloatOrNull() ?: 0.0f
        }
        val rateBits = java.lang.Float.floatToIntBits(rateFloat)

        var patched = 0

        // 1. Initialize default targetRefreshRate in TikTokRefreshRateHook.<clinit>
        val hookClinitFp = Fingerprint(
            definingClass = Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK,
            name = "<clinit>",
        )
        val method = hookClinitFp.method
        val instructions = method.implementation?.instructions?.toList() ?: emptyList()
        val returnIndex = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        val targetIndex = if (returnIndex >= 0) returnIndex else 0
        method.addInstructions(
            targetIndex,
            """
                const v0, $rateBits
                sput v0, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->targetRefreshRate:F
            """.trimIndent(),
        )
        println("[Refresh Rate Governor] Initialized targetRefreshRate=${if (rateFloat == 0f) "max" else "${rateFloat.toInt()}Hz"} (hardware bounded).")
        patched++

        // 2. Neutralize PlayerController video framerate downclocking by forcing ui_video_frame_rate_opt
        Fingerprint(
            name = "invoke",
            returnType = "Ljava/lang/Object;",
            strings = listOf("ui_video_frame_rate_opt"),
        ).method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                return-object v0
            """.trimIndent(),
        )
        println("[Refresh Rate Governor] Forced ui_video_frame_rate_opt -> Boolean.TRUE (bypasses PlayerController downclocking safely without aborting onRenderFirstFrame).")
        patched++

        // 2.2 Neutralize setRefreshRateIfNeeded
        Fingerprint(
            name = "invoke",
            returnType = "Ljava/lang/Object;",
            strings = listOf("setRefreshRateIfNeeded preferredRefreshRate:"),
        ).method.addInstructions(
            0,
            """
                sget-object v0, Lkotlin/Unit;->LIZ:Lkotlin/Unit;
                return-object v0
            """.trimIndent(),
        )
        println("[Refresh Rate Governor] Neutralized setRefreshRateIfNeeded -> forced Unit.LIZ.")
        patched++

        // 3. Override window refresh rate reset when drag stops
        Fingerprint(
            name = "LIZ",
            returnType = "V",
            strings = listOf("set dragging rate "),
        ).method.addInstructions(
            0,
            """
                invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->applyToWindow(Landroid/app/Activity;)V
                return-void
            """.trimIndent(),
        )
        println("[Refresh Rate Governor] Overrode drag rate reset -> enforced target rate on drag release.")
        patched++

        // 4. Override RefreshFrequencyTutor
        val tutorLIZfp = Fingerprint(
            name = "LIZ",
            parameters = listOf("Landroid/app/Activity;"),
            returnType = "V",
            strings = listOf("restore to default"),
        )
        tutorLIZfp.method.addInstructions(
            0,
            """
                invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->applyToWindow(Landroid/app/Activity;)V
                return-void
            """.trimIndent(),
        )
        println("[Refresh Rate Governor] Overrode RefreshFrequencyTutor.restore -> enforced target rate.")
        patched++

        Fingerprint(
            definingClass = tutorLIZfp.method.definingClass,
            name = "LIZIZ",
            parameters = listOf("Landroid/app/Activity;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->applyToWindow(Landroid/app/Activity;)V
                return-void
            """.trimIndent(),
        )
        println("[Refresh Rate Governor] Overrode RefreshFrequencyTutor.set -> enforced target rate.")
        patched++

        // 5. Lock refresh rate in MainActivity.onResume()
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/main/MainActivity;",
            name = "onResume",
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->applyToWindow(Landroid/app/Activity;)V
            """.trimIndent(),
        )
        println("[Refresh Rate Governor] Hooked MainActivity.onResume() -> enforced target rate.")
        patched++

        // 6. Lock refresh rate in MainActivity.onWindowFocusChanged(Z)
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/main/MainActivity;",
            name = "onWindowFocusChanged",
            parameters = listOf("Z"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                if-eqz p1, :skip_refresh
                invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->applyToWindow(Landroid/app/Activity;)V
                :skip_refresh
            """.trimIndent(),
        )
        println("[Refresh Rate Governor] Hooked MainActivity.onWindowFocusChanged() -> enforced target rate on focus gain.")
        patched++

        println("[Refresh Rate Governor] Applied $patched refresh rate governor hook(s).")
    }
}
