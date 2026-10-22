package app.morphe.patches.universal

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.childrenNamed
import app.morphe.patches.shared.getAttributeValue
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val ACTION_VIEW = "android.intent.action.VIEW"
private const val CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE"

private val WEB_SCHEMES = setOf("http", "https")

private val COMPONENT_TAGS = listOf("activity", "activity-alias", "service", "receiver")

private fun hasAction(filter: Element, action: String): Boolean =
    filter.childrenNamed("action").any { getAttributeValue(it, "name") == action }

private fun hasCategory(filter: Element, category: String): Boolean =
    filter.childrenNamed("category").any { getAttributeValue(it, "name") == category }

private data class PruneResult(val dataNodesRemoved: Int, val filtersRemoved: Int)

/**
 * Removes http/https `<data>` entries from VIEW + BROWSABLE intent filters.
 *
 * A filter left with no `<data>` nodes whose only actions are VIEW existed
 * purely for web links; when [removeEmptied] is true the whole filter is
 * dropped (leaving a dataless VIEW + BROWSABLE filter would match every
 * VIEW intent, which is worse). Filters also carrying local schemes
 * (`file`, `content`) keep those entries and the filter itself.
 */
private fun pruneBrowserAssociations(doc: Document, removeEmptied: Boolean): PruneResult {
    var dataNodesRemoved = 0
    var filtersRemoved = 0

    for (tag in COMPONENT_TAGS) {
        val nodes = doc.getElementsByTagName(tag)
        for (i in 0 until nodes.length) {
            val component = nodes.item(i) as? Element ?: continue
            val filtersToRemove = mutableListOf<Element>()
            for (filter in component.childrenNamed("intent-filter")) {
                if (!hasAction(filter, ACTION_VIEW)) continue
                if (!hasCategory(filter, CATEGORY_BROWSABLE)) continue
                val webData = filter.childrenNamed("data")
                    .filter { getAttributeValue(it, "scheme").lowercase() in WEB_SCHEMES }
                if (webData.isEmpty()) continue
                webData.forEach { filter.removeChild(it) }
                dataNodesRemoved += webData.size
                val remainingData = filter.childrenNamed("data")
                val actions = filter.childrenNamed("action").map { getAttributeValue(it, "name") }
                if (removeEmptied && remainingData.isEmpty() && actions.all { it == ACTION_VIEW }) {
                    filtersToRemove.add(filter)
                }
            }
            filtersToRemove.forEach { component.removeChild(it) }
            filtersRemoved += filtersToRemove.size
        }
    }

    return PruneResult(dataNodesRemoved, filtersRemoved)
}

@Suppress("unused")
val universalBrowserAssociationPatch = resourcePatch(
    name = "Disable Browser File Associations",
    description = "Removes HTTP/HTTPS web-link intent filters (VIEW + BROWSABLE) so the app no longer appears in the browser app chooser for online files, while preserving local file handling.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val stripWebSchemes by booleanOption(
        key = "stripWebSchemes",
        default = true,
        title = "Strip HTTP/HTTPS Web Schemes",
        description = "Remove http/https data entries from VIEW + BROWSABLE intent filters.",
        required = false,
    )

    val removeEmptiedFilters by booleanOption(
        key = "removeEmptiedFilters",
        default = true,
        title = "Remove Emptied Filters",
        description = "Drop intent filters left with no data entries whose only action is VIEW. Disable to keep the (now dataless) filter skeleton.",
        required = false,
    )

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Disable Browser File Associations] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        if (stripWebSchemes == false) {
            println("[Disable Browser File Associations] Skipped: stripWebSchemes is disabled.")
            return@execute
        }
        val dropEmptied = removeEmptiedFilters ?: true

        var result = PruneResult(0, 0)
        document(manifestFile.absolutePath).use { doc ->
            result = pruneBrowserAssociations(doc, dropEmptied)
        }

        if (result.dataNodesRemoved == 0) {
            println("[Disable Browser File Associations] No HTTP/HTTPS browser intent filters found - nothing to strip.")
            return@execute
        }

        println("[Disable Browser File Associations] Stripped ${result.dataNodesRemoved} web scheme entr(ies) and removed ${result.filtersRemoved} emptied browser filter(s). Local file handlers untouched.")
    }
}
