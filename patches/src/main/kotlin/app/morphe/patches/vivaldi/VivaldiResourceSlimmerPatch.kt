package app.morphe.patches.vivaldi

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_STUB_BYTES = byteArrayOf()

@Suppress("unused")
val vivaldiResourceSlimmerPatch = rawResourcePatch(
    name = "Resource Slimmer",
    description = "Strips bundled stock wallpapers, sponsored Speed Dial thumbnails, partner favicons, Privacy Sandbox attestations, and hardens declarative preference defaults.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        val (wallpapers, savedWp) = stripDirectoryFiles(get("assets/wallpapers"))
        val (thumbnails, savedTb) = stripDirectoryFiles(get("assets/sd_thumbnails"))
        val (favicons, savedFav) = stripDirectoryFiles(get("assets/favicons"))

        // Strip Google Privacy Sandbox attestations binary
        val attestationsFile = get("assets/privacy_sandbox_attestations/privacy-sandbox-attestations.dat")
        var savedAttestations = 0L
        if (attestationsFile.exists() && attestationsFile.isFile) {
            val origSize = attestationsFile.length()
            if (origSize > 0) {
                attestationsFile.writeBytes(EMPTY_STUB_BYTES)
                savedAttestations = origSize
            }
        }

        // Harden declarative preference defaults in assets/prefs_definitions.json
        val prefsDefFile = get("assets/prefs_definitions.json")
        var updatedPrefs = 0
        if (prefsDefFile.exists() && prefsDefFile.isFile) {
            val content = prefsDefFile.readText()
            val replacements = listOf(
                Regex("""("enable_document_blocking"\s*:\s*\{[^}]*"default"\s*:\s*)false""") to "$1true",
                Regex("""("direct_match_enabled"\s*:\s*\{[^}]*"default"\s*:\s*)true""") to "$1false",
                Regex("""("direct_match_boosted"\s*:\s*\{[^}]*"default"\s*:\s*)true""") to "$1false",
                Regex("""("donation_promo_dismissed"\s*:\s*\{[^}]*"default"\s*:\s*)0\.0""") to "$11.0",
                Regex("""("promote"\s*:\s*\{[^}]*"default"\s*:\s*)true""") to "$1false",
            )
            var newContent = content
            for ((pattern, replacement) in replacements) {
                if (pattern.containsMatchIn(newContent)) {
                    newContent = pattern.replaceFirst(newContent, replacement)
                    updatedPrefs++
                }
            }
            if (newContent != content) {
                prefsDefFile.writeText(newContent)
            }
        }

        val totalSavedBytes = savedWp + savedTb + savedFav + savedAttestations
        val totalSavedMb = String.format(java.util.Locale.US, "%.2f", totalSavedBytes.toDouble() / (1024 * 1024))
        println("[Vivaldi Resource Slimmer] Cleaned $wallpapers wallpapers, $thumbnails thumbnails, $favicons favicons, Privacy Sandbox attestations, and hardened $updatedPrefs preference defaults -> Saved $totalSavedMb MB")
    }
}

private fun stripDirectoryFiles(dir: java.io.File): Pair<Int, Long> {
    if (!dir.exists() || !dir.isDirectory) return 0 to 0L
    var count = 0
    var savedBytes = 0L
    dir.walkTopDown().filter { it.isFile }.forEach { file ->
        val originalSize = file.length()
        if (originalSize > 0) {
            file.writeBytes(EMPTY_STUB_BYTES)
            savedBytes += originalSize
            count++
        }
    }
    return count to savedBytes
}
