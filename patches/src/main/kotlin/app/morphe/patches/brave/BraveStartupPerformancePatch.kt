package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val bravePerformanceOptimizationPatch = bytecodePatch(
    name = "Brave Startup Performance Optimization",
    description = "Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

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
        fp.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                iput-object v0, p0, Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;->b:Ljava/lang/Boolean;
                return-void
            """,
        )

        val targetClass = fp.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        println("[Startup Performance] Neutralized async OEM initialization in $targetClass")
    }
}
