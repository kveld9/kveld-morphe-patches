package app.morphe.patches.universal

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount

private const val LICENSE_CHECKER = "Lcom/android/vending/licensing/LicenseChecker;"
private const val LICENSE_CALLBACK = "Lcom/android/vending/licensing/LicenseCheckerCallback;"

// Reason code 0x0100 = Policy.LICENSED, the standard value the library passes to allow().
private const val ALLOW_REASON = "0x0100"

/**
 * Returns true when the method accepts exactly one parameter of type LicenseCheckerCallback,
 * returns void, has a bytecode body, and is not a constructor or static initializer. Identified
 * by signature shape, not by name, so it survives R8 method-name minification within the library
 * classes.
 */
private fun isCheckAccessMethod(method: MutableMethod): Boolean {
    if (method.name.startsWith("<")) return false
    if (method.returnType != "V") return false
    if (method.implementation == null) return false
    val params = method.parameterTypes
    return params.size == 1 && params[0] == LICENSE_CALLBACK
}

/**
 * Replaces checkAccess(LicenseCheckerCallback) so it immediately delivers a licensed verdict
 * to the callback, without contacting Play or consulting the local cache.
 *
 * The injected body is:
 *   const/16 v0, 0x0100
 *   invoke-interface {p1, v0}, LicenseCheckerCallback->allow(I)V
 *   return-void
 *
 * p1 is the callback parameter (index 1 in the non-static method frame).
 */
private fun forceAllowVerdict(method: MutableMethod) {
    val impl = method.implementation ?: return
    method.clearTryBlocks()
    method.ensureRegisterCount(1)
    method.removeInstructions(0, impl.instructions.count())
    method.addInstructions(
        0,
        """
            const/16 v0, $ALLOW_REASON
            invoke-interface {p1, v0}, $LICENSE_CALLBACK->allow(I)V
            return-void
        """.trimIndent(),
    )
}

@Suppress("unused")
val universalBypassPlayLicensePatch = bytecodePatch(
    name = "Bypass Play License",
    description = "Stops apps that use the legacy Play licensing client library from blocking a re-signed install. Cannot affect server-side Play Integrity or subscription entitlements.",
    default = false,
) {
    execute {
        val checkerClass = mutableClassDefByOrNull(LICENSE_CHECKER)
        if (checkerClass == null) {
            println("[Bypass Play License] Skipped: Play licensing library not found; no changes applied.")
            return@execute
        }

        var patched = 0
        for (method in checkerClass.methods) {
            if (isCheckAccessMethod(method)) {
                forceAllowVerdict(method)
                patched++
            }
        }

        if (patched == 0) {
            println("[Bypass Play License] Skipped: LicenseChecker present but no checkAccess method matched; no changes applied.")
            return@execute
        }
        println("[Bypass Play License] Normalized $patched local license verdict(s) to licensed.")
    }
}
