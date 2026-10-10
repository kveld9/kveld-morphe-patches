package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.cleanClassName
import app.morphe.patches.shared.getAttributeValue
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

// Exported without any permission, so every app on the device can query Gboard's debug bridge.
private const val WEB_DEBUG_BRIDGE_PROVIDER =
    "com.google.android.libraries.inputmethod.webdebugbridge.WebDebugBridgeContentProvider"

private val BRELLA_COMPONENTS_TO_REMOVE = setOf(
    "com.google.android.apps.inputmethod.libs.trainingcache.examplestoreservice.ExampleStoreServiceMultiplexer",
    "com.google.android.apps.inputmethod.libs.trainingcache.replaycache.precomputedfeature.speech.examplestoreservice.SpeechPrecomputedFeatureExampleStoreService",
    "com.google.android.apps.inputmethod.libs.trainingcache.replaycache.sanitycheckeval.nwpp13n.examplestoreservice.NWPSanityCheckEvalExampleStoreService",
    "com.google.android.libraries.inputmethod.trainingcache.localcomputation.LocalComputationResultHandlingService",
    "com.google.android.libraries.inputmethod.trainingcache.trainer.dynamictrainer.FederatedResultHandlingService",
    "com.google.android.gms.learning.internal.training.InAppTrainingService",
    "com.google.android.libraries.phenotype.registration.PhenotypeMetadataHolderService",
    "com.google.android.build.data.PropertiesServiceHolder",
    "android.net.http.MetaDataHolder",
    "com.google.android.libraries.inputmethod.pixelbundle.PixelBundleBroadcastReceiver",
    "com.google.android.libraries.inputmethod.accounts.checker.AccountsCapabilitiesChangedReceiver",
)

private val FORBIDDEN_COMPONENTS = setOf(
    "com.google.android.libraries.phenotype.client.stable.PhenotypeUpdateBackgroundBroadcastReceiver",
    "com.google.android.libraries.phenotype.client.stable.AccountRemovedBroadcastReceiver",
    "com.google.android.libraries.performance.primes.transmitter.LifeboatReceiver",
    "com.google.android.libraries.appdoctor.AppDoctorReceiver",
)

private val BRELLA_METADATA_PREFIXES = listOf(
    "com.google.android.gms.phenotype",
    "com.google.android.partnersetup",
    "com.android.stamp.",
)

private val gboardRemoveWebDebugBridgePatch = resourcePatch {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Hardened Intent Security] Skipped provider removal: AndroidManifest.xml not found.")
            return@execute
        }

        var removedWebDebugBridge = 0
        var removedComponents = 0
        var removedQueries = 0
        var removedMetaData = 0
        val foundComponentNames = mutableSetOf<String>()

        document(manifestFile.absolutePath).use { doc ->
            // Pass 1: WebDebugBridgeContentProvider removal
            val providers = doc.getElementsByTagName("provider")
            for (i in providers.length - 1 downTo 0) {
                val provider = providers.item(i) as? Element ?: continue
                if (provider.getAttribute("android:name") == WEB_DEBUG_BRIDGE_PROVIDER) {
                    provider.parentNode?.removeChild(provider)
                    removedWebDebugBridge++
                }
            }

            // Pass 2: Brella / federated-learning component, queries, and metadata purge
            for (tag in listOf("service", "receiver", "provider")) {
                val nodes = doc.getElementsByTagName(tag)
                for (i in nodes.length - 1 downTo 0) {
                    val elem = nodes.item(i) as? Element ?: continue
                    val name = getAttributeValue(elem, "name")
                    if (name in FORBIDDEN_COMPONENTS) continue
                    if (name in BRELLA_COMPONENTS_TO_REMOVE) {
                        elem.parentNode?.removeChild(elem)
                        foundComponentNames.add(name)
                        removedComponents++
                    }
                }
            }

            val queriesNodes = doc.getElementsByTagName("queries")
            for (q in 0 until queriesNodes.length) {
                val queriesElem = queriesNodes.item(q) as? Element ?: continue
                val packageNodes = queriesElem.getElementsByTagName("package")
                for (i in packageNodes.length - 1 downTo 0) {
                    val pkgElem = packageNodes.item(i) as? Element ?: continue
                    val name = getAttributeValue(pkgElem, "name")
                    if (name.startsWith("com.google.") || name == "com.android.vending") {
                        pkgElem.parentNode?.removeChild(pkgElem)
                        removedQueries++
                    }
                }
            }

            val appNodes = doc.getElementsByTagName("application")
            for (a in 0 until appNodes.length) {
                val application = appNodes.item(a) as? Element ?: continue
                val childNodes = application.childNodes
                for (i in childNodes.length - 1 downTo 0) {
                    val child = childNodes.item(i) as? Element ?: continue
                    if (child.nodeName == "meta-data") {
                        val name = getAttributeValue(child, "name")
                        if (name.contains("backup", ignoreCase = true)) continue
                        if (BRELLA_METADATA_PREFIXES.any { name.startsWith(it) }) {
                            child.parentNode?.removeChild(child)
                            removedMetaData++
                        }
                    }
                }
            }
        }

        if (removedWebDebugBridge == 0) {
            println("[Hardened Intent Security] Skipped provider removal: WebDebugBridgeContentProvider not declared.")
        } else {
            println("[Hardened Intent Security] Removed $removedWebDebugBridge exported WebDebugBridgeContentProvider declaration(s).")
        }

        val skippedComponents = BRELLA_COMPONENTS_TO_REMOVE.size - foundComponentNames.size
        println("[Hardened Intent Security] Removed $removedComponents Brella components ($skippedComponents skipped/absent), $removedQueries queries, $removedMetaData metadata entries.")
    }
}

val gboardHardenedIntentSecurityPatch = bytecodePatch(
    name = "Hardened Intent Security",
    description = "Enables Gboard internal external intent protection against unauthorized intent hijacking, removes the exported, permissionless web debug bridge content provider, and purges federated learning (Brella) services, receivers, queries, and metadata from AndroidManifest.xml.",
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

        val targetClass = cleanClassName(fingerprint.originalClassDef.type)
        println("[Hardened Intent Security] Injected flag override into $targetClass.<clinit>() at opcode index ${matchIndex + 2}")
    }
}
