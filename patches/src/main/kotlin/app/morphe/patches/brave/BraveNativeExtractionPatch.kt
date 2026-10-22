package app.morphe.patches.brave

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

/**
 * The stock APK stores native libraries uncompressed at 16 KB zip offsets and loads them from the APK
 * (extractNativeLibs=false). The patcher rewrites them at 4 KB offsets, which 16 KB page devices cannot map,
 * so the libraries are extracted to disk at install time instead.
 */
internal val braveNativeExtractionPatch = resourcePatch {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Brave Compatibility] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        document(manifestFile.absolutePath).use { doc ->
            val appElements = doc.getElementsByTagName("application")
            if (appElements.length > 0) {
                val app = appElements.item(0) as? Element
                app?.setAttribute("android:extractNativeLibs", "true")
            }
        }
        println("[Brave Compatibility] Enforced android:extractNativeLibs=true in AndroidManifest.xml")
    }
}
