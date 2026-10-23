package app.morphe.patches.universal

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.replaceWithReturnBoolean

private const val ROOTBEER_CLASS = "Lcom/scottyab/rootbeer/RootBeer;"
private const val ROOTBEER_NATIVE_CLASS = "Lcom/scottyab/rootbeer/RootBeerNative;"

private val TARGET_CLASSES = listOf(
    ROOTBEER_CLASS,
    ROOTBEER_NATIVE_CLASS,
)

private fun isDetectionMethod(method: MutableMethod): Boolean {
    if (method.returnType != "Z") return false
    if (method.implementation == null) return false
    return method.name != "equals"
}

private fun neutralizeClassMethods(mutableClass: MutableClass): Int {
    var count = 0
    for (method in mutableClass.methods) {
        if (isDetectionMethod(method)) {
            method.replaceWithReturnBoolean(false)
            count++
        }
    }
    return count
}

private fun reportOutcome(classesFound: Int, patched: Int) {
    if (classesFound == 0) {
        println("[Disable Root Checks] Skipped: RootBeer library not found; no changes applied.")
        return
    }
    if (patched == 0) {
        println("[Disable Root Checks] Skipped: RootBeer present but no detection methods matched; no changes applied.")
        return
    }
    println("[Disable Root Checks] Neutralized $patched root-detection method(s).")
}

@Suppress("unused")
val universalDisableRootChecksPatch = bytecodePatch(
    name = "Disable Root Checks",
    description = "Stops apps that use the common root-detection library from detecting a rooted device.",
    default = false,
) {
    execute {
        var classesFound = 0
        var patched = 0

        for (descriptor in TARGET_CLASSES) {
            val classDef = mutableClassDefByOrNull(descriptor) ?: continue
            classesFound++
            patched += neutralizeClassMethods(classDef)
        }

        reportOutcome(classesFound, patched)
    }
}
