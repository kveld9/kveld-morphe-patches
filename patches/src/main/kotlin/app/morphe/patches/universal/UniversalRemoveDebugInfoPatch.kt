package app.morphe.patches.universal

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.builder.BuilderInstruction

@Suppress("unused")
val universalRemoveDebugInfoPatch = bytecodePatch(
    name = "Remove Debug Info",
    description = "Strips DEX debug info (source names and line tables) to reduce APK size.",
    default = false,
) {
    execute {
        var sourceCleared = 0
        var methodsCleared = 0

        classDefForEach { classDef ->
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

        if (sourceCleared == 0 && methodsCleared == 0) {
            println("[Remove Debug Info] No debug info found - nothing to strip.")
            return@execute
        }

        println("[Remove Debug Info] Cleared source names from $sourceCleared classes and line tables from $methodsCleared methods.")
    }
}
