package app.morphe.patches.universal

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnVoid

private const val LICENSE_ACTIVITY = "Lcom/pairip/licensecheck/LicenseActivity;"

// VM-provided startup initialization. Device testing shows that altering these classes
// (StartupLauncher.launch, VMRunner.invoke, VMRunner.<clinit>) crashes the app at boot,
// so the patch never touches them.
private const val STARTUP_LAUNCHER = "Lcom/pairip/StartupLauncher;"
private const val VM_RUNNER = "Lcom/pairip/VMRunner;"

// Local-verdict methods whose favorable result lets a re-signed build pass the on-device gate.
private val BOOLEAN_PASS_METHODS = setOf(
    "performLocalInstallerCheck",
    "isLocalCheckPassed",
    "verifySignatureMatches",
)

private fun isTargetMethodName(name: String, isLicenseActivity: Boolean): Boolean =
    name == "verifyIntegrity" ||
        name in BOOLEAN_PASS_METHODS ||
        (isLicenseActivity && name == "onStart")

/**
 * Forces a local verdict method to report success: a void check becomes a no-op and a
 * boolean check returns true, so the signature/installer verdict no longer aborts the app.
 */
private fun neutralizeVerdictMethod(method: MutableMethod): Boolean {
    if (method.implementation == null) return false
    val name = method.name
    val returnType = method.returnType
    when {
        name == "verifyIntegrity" && returnType == "V" -> method.replaceWithReturnVoid()
        name == "verifyIntegrity" && returnType == "Z" -> method.replaceWithReturnBoolean(true)
        name in BOOLEAN_PASS_METHODS && returnType == "Z" -> method.replaceWithReturnBoolean(true)
        else -> return false
    }
    return true
}

/**
 * Makes the license error activity dismiss itself immediately instead of blocking the user:
 * call through to the framework onStart, then finish the activity.
 */
private fun neutralizeLicenseActivityOnStart(method: MutableMethod): Boolean {
    val impl = method.implementation ?: return false
    if (method.name != "onStart" || method.returnType != "V" || method.parameterTypes.isNotEmpty()) return false
    method.clearTryBlocks()
    method.ensureRegisterCount(1)
    method.removeInstructions(0, impl.instructions.count())
    method.addInstructions(
        0,
        """
            invoke-super {p0}, Landroid/app/Activity;->onStart()V
            invoke-virtual {p0}, Landroid/app/Activity;->finish()V
            return-void
        """.trimIndent(),
    )
    return true
}

@Suppress("unused")
val universalRemovePairipPatch = bytecodePatch(
    name = "Remove Pairip",
    description = "Stops apps protected by Pairip from blocking a re-signed install. Cannot defeat checks that rely on a remote server.",
    default = false,
) {
    execute {
        var patched = 0
        var pairipClassesSeen = 0

        classDefForEach { classDef ->
            val type = classDef.type
            if (!type.startsWith("Lcom/pairip/")) return@classDefForEach
            if (type == STARTUP_LAUNCHER || type == VM_RUNNER) return@classDefForEach
            pairipClassesSeen++

            val isLicenseActivity = type == LICENSE_ACTIVITY
            if (classDef.methods.none { isTargetMethodName(it.name, isLicenseActivity) }) {
                return@classDefForEach
            }

            for (method in mutableClassDefBy(classDef).methods) {
                val applied = if (isLicenseActivity && method.name == "onStart") {
                    neutralizeLicenseActivityOnStart(method)
                } else {
                    neutralizeVerdictMethod(method)
                }
                if (applied) patched++
            }
        }

        if (pairipClassesSeen == 0) {
            println("[Remove Pairip] Skipped: no Pairip classes found; no changes applied.")
            return@execute
        }
        if (patched == 0) {
            println("[Remove Pairip] Skipped: Pairip present but no known local check methods matched; no changes applied.")
            return@execute
        }
        println("[Remove Pairip] Neutralized $patched on-device license/integrity check method(s).")
    }
}
