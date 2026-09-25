package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import org.w3c.dom.Element

// ── Resource Patch: Sets defaultValue="false" for NTP marketing switches in XML ──────────
private val braveCleanNtpXmlPatch = resourcePatch(
    name = "Clean NTP Resource Defaults",
    description = "Sets default values of NTP sponsored images, Brave News, and stats switches to false in XML preferences.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val ntpMarketingSwitches = listOf(
            "show_background_images",
            "show_sponsored_images",
            "new_tab_page_show_sponsored_images",
            "brave_news_switch",
            "brave_today_switch",
            "show_brave_news",
            "show_brave_stats",
            "brave_stats_switch",
        )

        val targetFiles = get("res").walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .filter { file ->
                val content = file.readText()
                ntpMarketingSwitches.any { key -> content.contains(key) }
            }
            .toList()

        var modifiedAttrs = 0
        var modifiedFiles = 0

        for (file in targetFiles) {
            var fileTouched = false
            document(file.absolutePath).use { doc ->
                val elements = doc.getElementsByTagName("*")
                for (i in 0 until elements.length) {
                    val node = elements.item(i) as? Element ?: continue
                    val key = node.getAttribute("android:key").takeIf { it.isNotEmpty() }
                        ?: node.getAttribute("key")
                    if (key in ntpMarketingSwitches) {
                        if (node.hasAttribute("defaultValue")) {
                            node.setAttribute("defaultValue", "false")
                        } else {
                            node.setAttribute("android:defaultValue", "false")
                        }
                        modifiedAttrs++
                        fileTouched = true
                    }
                }
            }
            if (fileTouched) modifiedFiles++
        }

        println("[Clean New Tab Page] Set $modifiedAttrs NTP marketing preference defaults to false across $modifiedFiles XML layout files")
    }
}

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
    name = "Clean New Tab Page",
    description = "Removes sponsored wallpaper images, Brave News/Today feeds, marketing widgets, and promo cards from the New Tab Page.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)
    extendWith("extensions/extension.mpe")

    val hideTopSites by booleanOption(
        key = "hideTopSites",
        default = false,
        title = "Hide Top Sites & Shortcuts",
        description = "Removes top sites shortcuts and most visited tiles from the New Tab Page for a completely blank home screen.",
        required = false,
    )

    dependsOn(braveCleanNtpXmlPatch)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Force NTP marketing preference gates to false in PrefService (b and e) at return
        listOf("b", "e").forEach { methodName ->
            try {
                val prefFp = Fingerprint(
                    definingClass = "Lorg/chromium/components/prefs/PrefService;",
                    name = methodName,
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
                    hookedMethods.add("PrefService.$methodName (NTP gates)")
                }
            } catch (e: Exception) {
                println("[Clean New Tab Page] PrefService.$methodName hook note: ${e.message}")
            }
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
            println("[Clean New Tab Page] Stats init hook note: ${e.message}")
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
            println("[Clean New Tab Page] Stats listener hook note: ${e.message}")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Clean New Tab Page] Neutralized ${hookedMethods.size} NTP marketing & layout components across ${targetClasses.size} classes")
    }
}
