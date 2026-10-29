package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private class PrefTarget(val searchString: String, val tag: String)

private fun injectPrefGuards(
    method: MutableMethod,
    targets: List<PrefTarget>,
    hookedMethods: MutableList<String>,
) {
    val instructions = method.implementation?.instructions?.toList() ?: return
    val resolved = targets.mapNotNull { target ->
        val constIdx = instructions.indexOfFirst {
            (it.opcode == Opcode.CONST_STRING || it.opcode == Opcode.CONST_STRING_JUMBO) &&
                (it as? ReferenceInstruction)?.reference?.let { ref ->
                    (ref as? StringReference)?.string == target.searchString
                } == true
        }
        if (constIdx == -1) return@mapNotNull null
        val iputRelOffset = instructions.drop(constIdx + 1).take(25).indexOfFirst {
            it.opcode == Opcode.IPUT_BOOLEAN
        }
        if (iputRelOffset == -1) error("Matching IPUT_BOOLEAN not found for pref: ${target.searchString}")
        val targetIdx = constIdx + 1 + iputRelOffset
        val reg = (instructions[targetIdx] as TwoRegisterInstruction).registerA
        Triple(targetIdx, reg, target.tag)
    }

    resolved.sortedByDescending { it.first }.forEach { (targetIdx, reg, tag) ->
        val constInst = if (reg <= 15) "const/4 v$reg, 0x0" else "const/16 v$reg, 0x0"
        method.addInstructions(targetIdx, constInst)
        hookedMethods.add(tag)
    }
}

// ── Bytecode Patch: Suppresses sponsored wallpaper & news preferences at runtime ────────
@Suppress("unused")
val braveCleanNewTabPagePatch = bytecodePatch(
    name = "Clean Start Page",
    description = "Removes sponsored wallpapers, news feed and promo cards.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)
    dependsOn(sharedExtensionPatch)

    val hideTopSites by booleanOption(
        key = "hideTopSites",
        default = false,
        title = "Hide shortcut tiles",
        description = "Removes most-visited tiles for a blank start page.",
        required = false,
    )

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Force NTP marketing preference gates in PrefService getBoolean at return
        val prefFp = Fingerprint(
            definingClass = "Lorg/chromium/components/prefs/PrefService;",
            name = "b",
            returnType = "Z",
            parameters = listOf("Ljava/lang/String;"),
        )
        val prefMethod = prefFp.method
        val prefInstructions = prefMethod.implementation?.instructions?.toList() ?: emptyList()
        val returnIndices = prefInstructions.withIndex()
            .filter { it.value.opcode == Opcode.RETURN }
            .mapNotNull { iv ->
                val returnIndex = iv.index
                val reg = (iv.value as? OneRegisterInstruction)?.registerA ?: return@mapNotNull null
                val insertIdx = if (returnIndex > 0 && prefInstructions[returnIndex - 1].opcode == Opcode.MONITOR_EXIT) {
                    returnIndex - 1
                } else {
                    returnIndex
                }
                insertIdx to reg
            }

        returnIndices.asReversed().forEach { (returnIndex, reg) ->
            if (reg > 15) {
                error("PrefService.b return register v$reg exceeds 4-bit range for non-range invoke-static")
            }
            prefMethod.addInstructions(
                returnIndex,
                """
                    invoke-static {p1, v$reg}, ${Constants.BRAVE_EXTENSION_CLASS}->filterNtpPref(Ljava/lang/String;Z)Z
                    move-result v$reg
                """.trimIndent(),
            )
        }
        if (returnIndices.isNotEmpty()) {
            hookedMethods.add("PrefService.b (NTP gates)")
        }

        val shouldHideTopSites = hideTopSites ?: false
        val prefTargets = mutableListOf(
            PrefTarget("show_brave_stats", "BraveNewTabPage.stats"),
        )
        if (shouldHideTopSites) {
            prefTargets.add(PrefTarget("show_top_sites", "BraveNewTabPage.topSites"))
        }

        // 2. Force NTP layout initialization guards
        val statsInitFp = Fingerprint(
            strings = listOf("recyclerview_visible_position_", "show_brave_stats"),
            returnType = "V",
            parameters = emptyList(),
        )
        injectPrefGuards(statsInitFp.method, prefTargets, hookedMethods)

        // 3. Force preference listener guards
        val statsListenerFp = Fingerprint(
            strings = listOf("brave_news_change_source", "show_brave_stats"),
            returnType = "V",
            parameters = listOf("Landroid/content/SharedPreferences;", "Ljava/lang/String;"),
        )
        injectPrefGuards(statsListenerFp.method, prefTargets, hookedMethods)

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Clean Start Page] Neutralized ${hookedMethods.size} NTP marketing & layout components across ${targetClasses.size} classes")
    }
}
