package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

// ── Raw Resource Patch: Sanitizes sponsored images JSON assets to empty campaigns schema ──
private val braveCleanNtpResourcePatch = rawResourcePatch(
    name = "Clean NTP Sponsored Assets Layer",
    description = "Sanitizes bundled sponsored images default.json to an empty campaigns schema to suppress sponsored wallpapers.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val cleanSponsoredJson = """{"schemaVersion":1,"campaigns":[]}"""
        var resetFiles = 0

        val sponsoredDir = get("assets/brave/sponsored-images")
        if (sponsoredDir.exists() && sponsoredDir.isDirectory) {
            sponsoredDir.walkTopDown()
                .filter { it.isFile && it.extension == "json" }
                .forEach { file ->
                    file.writeText(cleanSponsoredJson)
                    resetFiles++
                }
        }

        val defaultJson = get("assets/brave/sponsored-images/default.json")
        if (defaultJson.exists() && defaultJson.isFile && resetFiles == 0) {
            defaultJson.writeText(cleanSponsoredJson)
            resetFiles++
        }

        println("[Clean New Tab Page] Sanitized $resetFiles sponsored wallpaper asset configurations")
    }
}

// ── Resource Patch: Sets defaultValue="false" for NTP marketing switches in XML ──────────
private val braveCleanNtpXmlPatch = resourcePatch(
    name = "Clean NTP Resource Defaults",
    description = "Sets default values of NTP sponsored images, Brave News, and stats switches to false in XML preferences.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val ntpMarketingSwitches = listOf(
            "show_sponsored_images",
            "new_tab_page_show_sponsored_images",
            "brave_news_switch",
            "brave_today_switch",
            "show_brave_news",
            "show_brave_stats",
            "brave_stats_switch",
            "show_top_sites",
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

// ── Bytecode Patch: Suppresses sponsored wallpaper & news preferences at runtime ────────
@Suppress("unused")
val braveCleanNewTabPagePatch = bytecodePatch(
    name = "Clean New Tab Page",
    description = "Removes sponsored wallpaper images, Brave News/Today feeds, marketing widgets, and promo cards from the New Tab Page.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)
    extendWith("extensions/extension.mpe")

    dependsOn(braveCleanNtpResourcePatch, braveCleanNtpXmlPatch)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Force NTP marketing preference gates to false in PrefService.e at return
        try {
            val prefFp = Fingerprint(
                definingClass = "Lorg/chromium/components/prefs/PrefService;",
                name = "e",
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
                hookedMethods.add("PrefService.e (NTP gates)")
            }
        } catch (e: Exception) {
            println("[Clean New Tab Page] PrefService.e hook note: ${e.message}")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Clean New Tab Page] Neutralized ${hookedMethods.size} NTP marketing & sponsored wallpaper components across ${targetClasses.size} classes")
    }
}
