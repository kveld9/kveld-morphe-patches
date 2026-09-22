package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

val braveNativeExtractionPatch = resourcePatch(
    name = "Brave Native Library Extraction Compatibility",
    description = "Enforces native library extraction in AndroidManifest.xml to ensure 16 KB page and BTI compatibility across modern ARM64 devices.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Startup Performance] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        document(manifestFile.absolutePath).use { doc ->
            val appElements = doc.getElementsByTagName("application")
            if (appElements.length > 0) {
                val app = appElements.item(0) as? Element
                app?.setAttribute("android:extractNativeLibs", "true")
            }
        }
        println("[Startup Performance] Enforced android:extractNativeLibs=true in AndroidManifest.xml")
    }
}

@Suppress("unused")
val bravePerformanceOptimizationPatch = bytecodePatch(
    name = "Brave Startup Performance Optimization",
    description = "Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)
    dependsOn(braveNativeExtractionPatch)

    execute {
        // Neutralize PartnerBrowserCustomizations.initializeAsync(Context).
        // Immediately marks the component as initialized (Boolean.TRUE) and returns,
        // avoiding main-thread SharedPreferences reads, background ThreadPool tasks,
        // ContentResolver queries, and 10-second timeout task scheduling.
        val fp = Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;",
            returnType = "V",
            parameters = listOf("Landroid/content/Context;"),
            strings = listOf(
                "Chrome.Homepage.PartnerCustomizedDefaultGurl",
                "Chrome.Homepage.PartnerCustomizedDefaultUri",
            ),
        )
        val boolField = fp.originalClassDef.fields.firstOrNull { it.type == "Ljava/lang/Boolean;" }?.name ?: "b"
        fp.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                iput-object v0, p0, Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;->$boolField:Ljava/lang/Boolean;
                return-void
            """,
        )

        val targetClass = fp.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        println("[Startup Performance] Neutralized async OEM initialization in $targetClass (field $boolField)")
    }
}
