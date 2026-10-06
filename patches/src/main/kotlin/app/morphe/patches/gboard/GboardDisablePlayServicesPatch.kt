package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.cleanClassName

// ConnectionResult.SERVICE_DISABLED. SERVICE_MISSING (1) is remapped to SERVICE_UPDATING (18) by
// GoogleApiAvailability when the GMS package is installed, which makes GoogleApiManager retry
// every few seconds; SERVICE_DISABLED is passed through and fails connections without retrying.
private const val SERVICE_DISABLED = 3

@Suppress("unused")
val gboardDisablePlayServicesPatch = bytecodePatch(
    name = "Disable Play Services Integration",
    description = "Makes Gboard's Google Play services availability check always report SERVICE_DISABLED, so GMS-backed code paths (Clearcut logging, Phenotype, account sync, Google Help feedback) are skipped at the source instead of being attempted. Note: Disables Gboard's inline Google Translate tool, which relies on Play Services Cronet.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        // GooglePlayServicesUtilLight.isGooglePlayServicesAvailable(Context, int); every
        // GoogleApiAvailability caller funnels through this single static check.
        val fp = Fingerprint(
            returnType = "I",
            parameters = listOf("Landroid/content/Context;", "I"),
            strings = listOf("Google Play services out of date for "),
        )
        fp.method.addInstructions(
            0,
            """
                const/4 v0, $SERVICE_DISABLED
                return v0
            """.trimIndent(),
        )

        val targetClass = cleanClassName(fp.originalClassDef.type)
        println("[Disable Play Services] Applied 1 hook -> $targetClass.${fp.method.name}() now reports SERVICE_DISABLED.")
    }
}
