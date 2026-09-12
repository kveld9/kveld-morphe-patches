package app.morphe.patches.tiktok.slimmer

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants
import java.io.File

private val EMPTY_BYTES = byteArrayOf()

val creatorBloatSlimmerPatch = rawResourcePatch(
    name = "Studio & Creation De-bloat",
    description = "Strips heavy video creation, CapCut-like video editor SDK (libttvesdk_plugin.so), and AR camera face models to save over 22MB.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        val targets = listOf(
            "lib/arm64-v8a/libeffect_plugin.so",
            "lib/arm64-v8a/libttvesdk_plugin.so",
            "lib/arm64-v8a/libdex_df_camera_biz.so",
            "lib/arm64-v8a/libEffectCreatorJni.so",
        )

        var savedBytes = 0L
        var count = 0

        targets.forEach { path ->
            val file = get(path)
            if (file.exists() && file.isFile) {
                val orig = file.length()
                if (orig > 0) {
                    file.writeBytes(EMPTY_BYTES)
                    savedBytes += orig
                    count++
                }
            }
        }

        val faceModelDir = get("assets/model/ttfacemodel")
        if (faceModelDir.exists() && faceModelDir.isDirectory) {
            faceModelDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val orig = file.length()
                if (orig > 0) {
                    file.writeBytes(EMPTY_BYTES)
                    savedBytes += orig
                    count++
                }
            }
        }

        if (count > 0) {
            val savedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
            println("[Studio & Creation De-bloat] Stripped $count creator/editor binaries and models -> Saved $savedMb MB uncompressed (~22 MB in APK)")
        } else {
            println("[Studio & Creation De-bloat] Target creator binaries not present.")
        }
    }
}
