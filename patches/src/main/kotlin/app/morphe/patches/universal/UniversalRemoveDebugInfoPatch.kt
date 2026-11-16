package app.morphe.patches.universal

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.iface.ClassDef

private const val MAX_MUTABLE_CLASSES = 50_000

@Suppress("unused")
val universalRemoveDebugInfoPatch = bytecodePatch(
    name = "Remove Debug Info",
    description = "Strips DEX debug info (source names and line tables) to reduce APK size.",
    default = false,
) {
    execute {
        val candidateClasses = mutableListOf<ClassDef>()
        classDefForEach { classDef ->
            val hasSourceFile = classDef.sourceFile != null
            val hasDebugItems = classDef.methods.any { it.implementation?.debugItems?.iterator()?.hasNext() == true }
            if (hasSourceFile || hasDebugItems) {
                candidateClasses.add(classDef)
            }
        }

        if (candidateClasses.isEmpty()) {
            println("[Remove Debug Info] No debug info found - nothing to strip.")
            return@execute
        }

        if (candidateClasses.size > MAX_MUTABLE_CLASSES) {
            println("[Remove Debug Info] Skipped: Target application has too many classes with debug metadata (${candidateClasses.size} > $MAX_MUTABLE_CLASSES); full multi-DEX rewrite exceeds patcher memory limits.")
            return@execute
        }

        var sourceCleared = 0
        var methodsCleared = 0

        for (classDef in candidateClasses) {
            try {
                val mutableClass: MutableClass = mutableClassDefBy(classDef)
                if (mutableClass.sourceFile != null) {
                    mutableClass.setSourceFile(null)
                    sourceCleared++
                }

                for (method: MutableMethod in mutableClass.methods) {
                    val impl = method.implementation ?: continue
                    var cleared = false
                    for (instruction in impl.instructions) {
                        val location = (instruction as? BuilderInstruction)?.location ?: continue
                        if (location.debugItems.isNotEmpty()) {
                            location.debugItems.clear()
                            cleared = true
                        }
                    }
                    if (cleared) methodsCleared++
                }
            } catch (_: Throwable) {
                // Safely skip malformed class structures without interrupting execution
            }
        }

        println("[Remove Debug Info] Cleared source names from $sourceCleared classes and line tables from $methodsCleared methods.")
    }
}
