package app.morphe.patches.tiktok.slimmer

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_BYTES = byteArrayOf()

val coreAssetDebloatPatch = rawResourcePatch(
    name = "Core Asset De-bloat",
    description = "Strips embedded Microblink credit card OCR models, C2PA AI content origin verification libraries, and unused non-Latin fonts to save APK space.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var savedBytes = 0L
        var count = 0

        // 1. C2PA libraries
        val c2paTargets = listOf(
            "lib/arm64-v8a/libtt_c2pa_sdk.so",
            "lib/arm64-v8a/libtt_c2pa_sdk_d.so",
        )
        c2paTargets.forEach { path ->
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

        // 2. Microblink OCR models
        val microblinkDir = get("assets/microblink")
        if (microblinkDir.exists() && microblinkDir.isDirectory) {
            microblinkDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val orig = file.length()
                if (orig > 0) {
                    file.writeBytes(EMPTY_BYTES)
                    savedBytes += orig
                    count++
                }
            }
        }

        // 3. Non-Latin font files
        val fontsDir = get("assets/fonts")
        if (fontsDir.exists() && fontsDir.isDirectory) {
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
            fontsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                if (nonLatinPatterns.any { pattern -> file.name.contains(pattern, ignoreCase = true) }) {
                    val orig = file.length()
                    if (orig > 0) {
                        file.writeBytes(EMPTY_BYTES)
                        savedBytes += orig
                        count++
                    }
                }
            }
        }

        if (count > 0) {
            val savedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
            println("[Core Asset De-bloat] Stripped $count non-essential OCR models, C2PA SDKs, and regional fonts -> Saved $savedMb MB uncompressed (~9.5 MB in APK)")
        } else {
            println("[Core Asset De-bloat] Target assets already clean.")
        }
    }
}
