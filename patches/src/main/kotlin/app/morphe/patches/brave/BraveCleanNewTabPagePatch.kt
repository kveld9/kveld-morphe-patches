package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
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
            it.opcode == Opcode.CONST_STRING &&
                (it as? ReferenceInstruction)?.reference?.let { ref ->
                    (ref as? StringReference)?.string == target.searchString
                } == true
        }
        if (constIdx == -1) return@mapNotNull null
        val iputRelOffset = instructions.drop(constIdx).indexOfFirst {
            it.opcode == Opcode.IPUT_BOOLEAN
        }
        if (iputRelOffset == -1) return@mapNotNull null
        val targetIdx = constIdx + iputRelOffset
        val reg = (instructions[targetIdx] as TwoRegisterInstruction).registerA
        Triple(targetIdx, reg, target.tag)
    }

    resolved.sortedByDescending { it.first }.forEach { (targetIdx, reg, tag) ->
        method.addInstructions(targetIdx, "const/4 v$reg, 0x0")
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

        // 1. Force NTP marketing preference gates to false in PrefService getBoolean at return
        try {
            val prefFp = Fingerprint(
                definingClass = "Lorg/chromium/components/prefs/PrefService;",
                name = "b",
                returnType = "Z",
                parameters = listOf("Ljava/lang/String;"),
            )
            val method = prefFp.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructions(
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
        } catch (e: Exception) {
            println("[Clean Start Page] PrefService.b hook note: ${e.message}")
        }

        val shouldHideTopSites = hideTopSites ?: false
        val prefTargets = mutableListOf(
            PrefTarget("show_brave_stats", "BraveNewTabPage.stats"),
        )
        if (shouldHideTopSites) {
            prefTargets.add(PrefTarget("show_top_sites", "BraveNewTabPage.topSites"))
        }

        // 2. Force NTP layout initialization guards
        try {
            val statsInitFp = Fingerprint(
                strings = listOf("recyclerview_visible_position_", "show_brave_stats"),
                returnType = "V",
                parameters = emptyList(),
            )
            injectPrefGuards(statsInitFp.method, prefTargets, hookedMethods)
        } catch (e: Exception) {
            println("[Clean Start Page] Stats init hook note: ${e.message}")
        }

        // 3. Force preference listener guards
        try {
            val statsListenerFp = Fingerprint(
                strings = listOf("brave_news_change_source", "show_brave_stats"),
                returnType = "V",
                parameters = listOf("Landroid/content/SharedPreferences;", "Ljava/lang/String;"),
            )
            injectPrefGuards(statsListenerFp.method, prefTargets, hookedMethods)
        } catch (e: Exception) {
            println("[Clean Start Page] Stats listener hook note: ${e.message}")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Clean Start Page] Neutralized ${hookedMethods.size} NTP marketing & layout components across ${targetClasses.size} classes")
    }
}
