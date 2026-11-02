package app.morphe.patches.tiktok.slimmer

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_BYTES = byteArrayOf()

val coreAssetDebloatPatch = rawResourcePatch(
    name = "Remove Extra Files",
    description = "Removes card scanners, AI engines and extra fonts to save space.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var savedBytes = 0L
        var count = 0

        val abis = listOf("lib/arm64-v8a", "lib/armeabi-v7a")

        fun stripFile(path: String) {
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

        fun stripNative(abis: List<String>, libs: List<String>) {
            abis.forEach { abi ->
                libs.forEach { lib ->
                    stripFile("$abi/$lib")
                }
            }
        }

        fun stripDirectory(dirPath: String, predicate: (java.io.File) -> Boolean = { true }) {
            val dir = get(dirPath)
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().filter { it.isFile && predicate(it) }.forEach { file ->
                    val orig = file.length()
                    if (orig > 0) {
                        file.writeBytes(EMPTY_BYTES)
                        savedBytes += orig
                        count++
                    }
                }
            }
        }

        // C2PA libraries (libtt_c2pa_sdk.so, libtt_c2pa_sdk_d.so) are preserved
        // because stripping/zeroing them causes TikTok's media/photo picker
        // (MvChoosePhotoScene -> TTC2PAServiceImpl.init) to crash with Librarian EOFException on 0-byte ELF headers.

        // 1. Internal Profiling & Python Runtimes
        stripNative(
            abis,
            listOf(
                "libreschecker.so",
                "libpy-cv-numpycv.so",
                "libpythonA.so",
                "libpy-numpy.so",
                "libBDMicroPythonVM.so",
                "libBDPythonVM.so",
            ),
        )

        // 2. Pitaya AI native libraries
        stripNative(
            abis,
            listOf(
                "libAndroidPitayaCore.so",
                "libAndroidPitayaProxy.so",
                "libPitayaBdComponent.so",
                "libPitayaTTPPolicy.so",
                "libdex_df_pitaya.so",
            ),
        )

        // 3. TikTok Shop FinTech & Card Scanner libraries + checkout assets
        stripNative(
            abis,
            listOf(
                "libBlinkCard.so",
                "libdex_df_pipo_bnpl.so",
                "libdex_df_ccdc_impl_ocr.so",
                "libdex_df_pipo_external_payments.so",
                "libpipo-security-sdk.so",
            ),
        )
        listOf(
            "assets/pipo_ui_default_checkout.json",
            "assets/pipo_ui_default_components.json",
        ).forEach { stripFile(it) }

        // 4. ByteDance Local LLM & Gemini Nano libraries (libbytenn.so and libbytennwrapper.so preserved for libaudioeffect.so and libstarship_sdk.so)
        stripNative(
            abis,
            listOf(
                "libbytennllm.so",
                "libbytennllm-jni.so",
                "libdex_df_gemini_nano.so",
            ),
        )

        // 5. SSDP/DLNA local network scanner binary & ByteDance TTWebView engine
        stripNative(abis, listOf("libdex_df_live_cast.so", "libdex_df_ttwebview.so"))
        stripFile("assets/dynamic_feature_install/df_ttwebview.json")

        // 6. Microblink OCR models
        stripDirectory("assets/microblink")

        // 7. Non-Latin font files
        val nonLatinPatterns = listOf(
            "NotoSansGreek",
            "NotoSansHebrew",
            "NotoSansArmenian",
            "NotoSansGeorgian",
            "NotoSansKhmer",
            "NotoSansLao",
            "NotoSansMyanmar",
            "NotoSansThai",
        )
        stripDirectory("assets/fonts") { file ->
            nonLatinPatterns.any { pattern -> file.name.contains(pattern, ignoreCase = true) }
        }

        if (count > 0) {
            val savedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
            println("[Remove Extra Files] Stripped $count non-essential OCR models, AI/LLM engines, and regional fonts -> Saved $savedMb MB uncompressed")
        } else {
            println("[Remove Extra Files] Target assets already clean.")
        }
    }
}
