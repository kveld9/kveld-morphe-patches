package app.morphe.patches.tiktok.slimmer

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants
import java.io.File

private val EMPTY_BYTES = byteArrayOf()

val creatorBloatSlimmerPatch = rawResourcePatch(
    name = "Studio & Creation De-bloat",
    description = "Strips heavy video creation plugins, CapCut NLE editor SDKs, effect plugins, camera/music dynamic features, upload video encoders, and AR camera face models to significantly reduce APK size.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        val abis = listOf("lib/arm64-v8a", "lib/armeabi-v7a")
        val explicitLibs = listOf(
            "libeffect_plugin.so",
            "libttvesdk_plugin.so",
            "libEffectCreatorJni.so",
            "libILAMaterialSDK.so",
            // CutSame template engine (NEEDED-depends on NLE editor + ttvesdk, creation-only)
            "libCutSameJni.so",
            "libDavinciResourceJni.so",
            // Upload-path video encoders (playback uses ByteVC decoders linked by libttmplayer)
            "libbytevc1enc.so",
            "libbytevc1enc10b.so",
            // Camera/music dynamic features (df_ family, viewer-only builds skip them)
            "libdex_df_camera_biz.so",
            "libdex_df_music_dsp.so",
        )

        var savedBytes = 0L
        var count = 0

        fun stripFile(file: File) {
            if (file.exists() && file.isFile) {
                val orig = file.length()
                if (orig > 0) {
                    file.writeBytes(EMPTY_BYTES)
                    savedBytes += orig
                    count++
                }
            }
        }

        // 1. Explicit creator & editor libraries across ABIs
        abis.forEach { abi ->
            explicitLibs.forEach { lib ->
                stripFile(get("$abi/$lib"))
            }

            // 2. Dynamic CapCut NLE editor libraries across ABIs
            val abiDir = get(abi)
            if (abiDir.exists() && abiDir.isDirectory) {
                abiDir.listFiles()?.filter { it.isFile && it.name.startsWith("libNLE") && it.name.endsWith(".so") }?.forEach { file ->
                    stripFile(file)
                }
            }
        }

        // 3. Face models
        val faceModelDir = get("assets/model/ttfacemodel")
        if (faceModelDir.exists() && faceModelDir.isDirectory) {
            faceModelDir.walkTopDown().filter { it.isFile }.forEach { file ->
                stripFile(file)
            }
        }

        if (count > 0) {
            val savedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
            println("[Studio & Creation De-bloat] Stripped $count creator/editor binaries and models -> Saved $savedMb MB uncompressed")
        } else {
            println("[Studio & Creation De-bloat] Target creator binaries not present.")
        }
    }
}
