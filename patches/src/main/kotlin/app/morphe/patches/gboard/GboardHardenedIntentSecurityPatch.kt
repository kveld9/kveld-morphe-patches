package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

// Exported without any permission, so every app on the device can query Gboard's debug bridge.
private const val WEB_DEBUG_BRIDGE_PROVIDER =
    "com.google.android.libraries.inputmethod.webdebugbridge.WebDebugBridgeContentProvider"

private val gboardRemoveWebDebugBridgePatch = resourcePatch {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Hardened Intent Security] Skipped provider removal: AndroidManifest.xml not found.")
            return@execute
        }

        var removed = 0
        document(manifestFile.absolutePath).use { doc ->
            val providers = doc.getElementsByTagName("provider")
            for (i in providers.length - 1 downTo 0) {
                val provider = providers.item(i) as? Element ?: continue
                if (provider.getAttribute("android:name") == WEB_DEBUG_BRIDGE_PROVIDER) {
                    provider.parentNode?.removeChild(provider)
                    removed++
                }
            }
        }

        if (removed == 0) {
            println("[Hardened Intent Security] Skipped provider removal: WebDebugBridgeContentProvider not declared.")
        } else {
            println("[Hardened Intent Security] Removed $removed exported WebDebugBridgeContentProvider declaration(s).")
        }
    }
}

val gboardHardenedIntentSecurityPatch = bytecodePatch(
    name = "Hardened Intent Security",
    description = "Enables Gboard internal external intent protection against unauthorized intent hijacking and removes the exported, permissionless web debug bridge content provider.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(gboardRemoveWebDebugBridgePatch)

    execute {
        val fingerprint = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("prevent_external_intents")),
        )

        val matchIndex = fingerprint.instructionMatches.first().index
        val reg = fingerprint.method.getInstruction<OneRegisterInstruction>(matchIndex + 1).registerA
        fingerprint.method.addInstructions(
            matchIndex + 2,
            "const/4 v$reg, 0x1",
        )

        val targetClass = app.morphe.patches.shared.LocaleUtils.cleanClassName(fingerprint.originalClassDef.type)
        println("[Hardened Intent Security] Injected flag override into $targetClass.<clinit>() at opcode index ${matchIndex + 2}")
    }
}
