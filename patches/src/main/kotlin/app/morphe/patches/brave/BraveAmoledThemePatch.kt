package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatch
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.colorOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File

internal const val PREF_KEY_DYNAMIC_PALETTE = "brave_android_dynamic_colors_enabled"
internal const val FEATURE_FLAG_DYNAMIC_PALETTE = "BraveAndroidDynamicColorsByDefault"
internal const val RES_PATH_DAY_COLORS = "values/colors.xml"
internal const val RES_PATH_NIGHT_COLORS = "values-night/colors.xml"
internal const val RES_PATH_NIGHT_V31_COLORS = "values-night-v31/colors.xml"

internal val ANDROID_S_SYS_COLOR_FILES = listOf("values-v31/colors.xml", "values-v34/colors.xml", "values-v35/colors.xml")

internal val DARK_CANONICAL_PALETTE = setOf(
    "#070608", "#0d0f14", "#0d1214", "#0f0f0f", "#121212",
    "#131221", "#17171f", "#1a1a1a", "#1c1c1d", "#1d1d36",
    "#1e2025", "#1e2029", "#1f1f1f", "#1f1f23", "#1f1f27",
    "#212121", "#212529", "#272733", "#2e3039", "#3b3e4f", )

internal val REGEX_SYS_DARK_BASE_ROLES = Regex("""@android:color/system_(?:neutral[123]_(?:700|800|900|1000)|(?:background|surface|surface_dim)_dark)$""", RegexOption.IGNORE_CASE)
internal val REGEX_SYS_DARK_CONTAINER_ROLES = Regex("""@android:color/system_(?:surface_bright|surface_variant|surface_container(?:_low|_lowest|_high|_highest)?)_dark$""", RegexOption.IGNORE_CASE)
internal val REGEX_SYS_DARK_ALL_ROLES = Regex("""@android:color/system_(?:neutral[123]_(?:700|800|900|1000)|(?:background|surface|surface_bright|surface_dim|surface_variant|surface_container(?:_low|_lowest|_high|_highest)?)_dark)$""", RegexOption.IGNORE_CASE)
internal val REGEX_LSTAR_SELECTOR_ITEM = Regex("""<item\s+(n\d+):color="(@android:color/system_[^"]+)"\s+\1:lStar="(\d+(?:\.\d+)?)"""", RegexOption.IGNORE_CASE)

private val REGEX_COLOR_TAG = Regex("""<color\s+name="([^"]+)"\s*>([^<]+)</color>""")
private val REGEX_HEX_CODE = Regex("""^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$""")

private fun File.subdirsNamed(prefix: String): List<File> = listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith(prefix) }.sortedBy { it.name }
private fun File.xmlChildFiles(): List<File> = listFiles().orEmpty().filter { it.isFile && it.extension.equals("xml", true) }.sortedBy { it.name }

private fun File.rewriteIfChanged(transform: (String) -> String): Boolean {
    val before = readText()
    val after = transform(before)
    if (after != before) { writeText(after); return true }
    return false }

internal data class ColorSample(val redVal: Int, val greenVal: Int, val blueVal: Int, val alphaVal: Int = 0xFF) {
    val peakComponent: Int = maxOf(redVal, greenVal, blueVal)
    val spreadDelta: Int = peakComponent - minOf(redVal, greenVal, blueVal) }

private fun unpackRgb3(d: String) = ColorSample(d.substring(0, 1).toInt(16) * 17, d.substring(1, 2).toInt(16) * 17, d.substring(2, 3).toInt(16) * 17)
private fun unpackRgb6(d: String) = ColorSample(d.substring(0, 2).toInt(16), d.substring(2, 4).toInt(16), d.substring(4, 6).toInt(16))
private fun unpackArgb8(d: String) = ColorSample(d.substring(2, 4).toInt(16), d.substring(4, 6).toInt(16), d.substring(6, 8).toInt(16), d.substring(0, 2).toInt(16))

internal fun decodeHexColor(inputString: String): ColorSample? {
    val trimmed = inputString.trim()
    if (!REGEX_HEX_CODE.matches(trimmed)) return null
    val payload = trimmed.substring(1)
    return when (payload.length) { 3 -> unpackRgb3(payload); 6 -> unpackRgb6(payload); 8 -> unpackArgb8(payload); else -> null } }

internal fun canonicalizeHex(inputString: String): String? = decodeHexColor(inputString)?.let { s ->
    if (s.alphaVal == 0xFF) String.format("#%02x%02x%02x", s.redVal, s.greenVal, s.blueVal) else null }

internal fun isSurfaceCandidate(hexString: String): Boolean {
    val canon = canonicalizeHex(hexString) ?: return false
    val sample = decodeHexColor(canon) ?: return false
    return sample.spreadDelta <= 40 && (sample.peakComponent <= 80 || canon in DARK_CANONICAL_PALETTE) }

internal fun isGroundTierColor(hexString: String): Boolean =
    isSurfaceCandidate(hexString) && (decodeHexColor(hexString)?.peakComponent ?: 255) <= 0x1c

internal fun isElevatedTierColor(hexString: String): Boolean =
    isSurfaceCandidate(hexString) && !isGroundTierColor(hexString)

internal fun isLegibleInkColor(hexString: String): Boolean {
    val canon = canonicalizeHex(hexString) ?: return false
    if (isSurfaceCandidate(canon)) return false
    val sample = decodeHexColor(canon) ?: return false
    return sample.spreadDelta <= 40 && sample.peakComponent >= 140 }

internal fun computeTextLuminanceRatio(sample: ColorSample): Float = when {
    sample.peakComponent >= 220 -> 1.0f
    sample.peakComponent >= 180 -> 0.81f
    else -> 0.62f }

internal fun modulateHexColor(baseHex: String, factor: Float): String {
    val sample = decodeHexColor(baseHex) ?: return baseHex
    return String.format("#%02x%02x%02x", (sample.redVal * factor).toInt().coerceIn(0, 255), (sample.greenVal * factor).toInt().coerceIn(0, 255), (sample.blueVal * factor).toInt().coerceIn(0, 255)) }

internal fun hasAccentSpread(sample: ColorSample): Boolean =
    sample.spreadDelta >= 50 && sample.peakComponent >= 100

internal fun isCoolDominantTone(sample: ColorSample): Boolean =
    sample.blueVal >= sample.greenVal && sample.greenVal >= sample.redVal - 30

internal fun isAccentCandidate(hexString: String): Boolean {
    val canon = canonicalizeHex(hexString) ?: return false
    val sample = decodeHexColor(canon) ?: return false
    return hasAccentSpread(sample) && isCoolDominantTone(sample) }

private fun replaceXmlColorTags(xmlText: String, transform: (tagId: String, rawHex: String) -> String?): Pair<String, Int> {
    var replacedCount = 0
    val result = REGEX_COLOR_TAG.replace(xmlText) { match ->
        val (name, raw) = match.destructured
        val replacement = transform(name, raw.trim()) ?: return@replace match.value
        replacedCount++
        """<color name="$name">$replacement</color>""" }
    return result to replacedCount }

internal data class SurfaceTransformStats(val baseCount: Int, val containerCount: Int)

private fun resolveRaisedStatus(tagId: String, rawHex: String, baseTagNames: Set<String>, containerTagNames: Set<String>): Boolean =
    tagId !in baseTagNames && (tagId in containerTagNames || isElevatedTierColor(rawHex))

internal fun rewriteXmlSurfaces(
    xmlText: String, backgroundHex: String, containerHex: String,
    containerTagNames: Set<String> = emptySet(), baseTagNames: Set<String> = emptySet(),
    excludedTagNames: Set<String> = emptySet(),
): Pair<String, SurfaceTransformStats> {
    val canonicalBg = canonicalizeHex(backgroundHex) ?: throw PatchException("Provided background is not a valid opaque hex: $backgroundHex")
    val canonicalContainer = canonicalizeHex(containerHex) ?: throw PatchException("Provided container is not a valid opaque hex: $containerHex")
    val singleTone = canonicalContainer == canonicalBg
    var countBase = 0
    var countRaised = 0

    val (updatedXml, _) = replaceXmlColorTags(xmlText) { name, raw ->
        if (name in excludedTagNames || !isSurfaceCandidate(raw)) return@replaceXmlColorTags null
        val raised = resolveRaisedStatus(name, raw, baseTagNames, containerTagNames)
        val target = if (raised) canonicalContainer else canonicalBg
        if (canonicalizeHex(raw) == target) return@replaceXmlColorTags null
        if (raised && !singleTone) countRaised++ else countBase++
        target }
    return updatedXml to SurfaceTransformStats(baseCount = countBase, containerCount = countRaised) }

internal fun rewriteXmlTextInk(xmlText: String, textHex: String): Pair<String, Int> {
    val canonicalText = canonicalizeHex(textHex) ?: throw PatchException("Provided ink is not a valid opaque hex: $textHex")
    return replaceXmlColorTags(xmlText) { _, raw ->
        if (!isLegibleInkColor(raw)) return@replaceXmlColorTags null
        val sample = decodeHexColor(raw) ?: return@replaceXmlColorTags null
        val target = modulateHexColor(canonicalText, computeTextLuminanceRatio(sample))
        if (canonicalizeHex(raw) == target) null else target } }

internal fun rewriteXmlAccentInk(xmlText: String, accentHex: String): Pair<String, Int> {
    val canonicalAccent = canonicalizeHex(accentHex) ?: throw PatchException("Provided accent is not a valid opaque hex: $accentHex")
    return replaceXmlColorTags(xmlText) { _, raw ->
        if (isAccentCandidate(raw) && canonicalizeHex(raw) != canonicalAccent) canonicalAccent else null } }

internal fun rewriteMaterialYouSystemRoles(xmlText: String, bgHex: String, containerHex: String = bgHex): Pair<String, Int> {
    val canonicalBg = canonicalizeHex(bgHex) ?: throw PatchException("Provided background is not a valid opaque hex: $bgHex")
    val canonicalContainer = canonicalizeHex(containerHex) ?: throw PatchException("Provided container is not a valid opaque hex: $containerHex")
    return replaceXmlColorTags(xmlText) { _, raw ->
        when {
            REGEX_SYS_DARK_CONTAINER_ROLES.matches(raw) -> canonicalContainer
            REGEX_SYS_DARK_BASE_ROLES.matches(raw) -> canonicalBg
            else -> null } } }

internal fun discoverMaterialYouNeutralIds(xmlText: String): List<String> =
    REGEX_COLOR_TAG.findAll(xmlText).filter { REGEX_SYS_DARK_ALL_ROLES.matches(it.groupValues[2].trim()) }.map { m -> m.groupValues[1] }.toList()

internal fun discoverMaterialYouRoleMap(xmlText: String): Map<String, Boolean> =
    REGEX_COLOR_TAG.findAll(xmlText).mapNotNull { match ->
        val (name, raw) = match.destructured
        val trimmed = raw.trim()
        when {
            REGEX_SYS_DARK_CONTAINER_ROLES.matches(trimmed) -> name to true
            REGEX_SYS_DARK_BASE_ROLES.matches(trimmed) -> name to false
            else -> null }
    }.toMap()

private fun appendResourceColorTags(xmlText: String, entries: Map<String, String>): String {
    if (entries.isEmpty()) return xmlText
    val tagBlock = entries.toSortedMap().entries.joinToString("\n") { (k, v) -> """    <color name="$k">$v</color>""" }
    return if (xmlText.contains("</resources>")) xmlText.replace("</resources>", "$tagBlock\n</resources>") else "$xmlText\n<resources>\n$tagBlock\n</resources>\n" }

internal fun injectDayTextInkDefaults(dayXmlText: String, neededIds: Collection<String>, alreadyDeclaredIds: Set<String>, inkHex: String): Pair<String, Int> {
    val canonicalInk = canonicalizeHex(inkHex) ?: throw PatchException("Provided ink is not a valid opaque hex: $inkHex")
    val missing = neededIds.filter { it !in alreadyDeclaredIds }.toSortedSet()
    if (missing.isEmpty()) return dayXmlText to 0
    return appendResourceColorTags(dayXmlText, missing.associateWith { canonicalInk }) to missing.size }

internal fun extractDarkInkIdsFromDayXml(dayXmlText: String, textBearingIds: Collection<String>): List<String> =
    REGEX_COLOR_TAG.findAll(dayXmlText).filter { it.groupValues[1] in textBearingIds && isSurfaceCandidate(it.groupValues[2].trim()) }.map { m -> m.groupValues[1] }.distinct().toList()

internal fun injectNightTextInkDeclarations(
    nightXmlText: String, candidateIds: Collection<String>, nightDeclaredIds: Set<String>, inkHex: String, selectorIds: Set<String> = emptySet(),
): Pair<String, Int> {
    val canonicalInk = canonicalizeHex(inkHex) ?: throw PatchException("Provided ink is not a valid opaque hex: $inkHex")
    val inXmlAlready = REGEX_COLOR_TAG.findAll(nightXmlText).map { it.groupValues[1] }.toSet()
    val missing = candidateIds.filter { it !in nightDeclaredIds && it !in inXmlAlready && it !in selectorIds }.toSortedSet()
    if (missing.isEmpty()) return nightXmlText to 0
    return appendResourceColorTags(nightXmlText, missing.associateWith { canonicalInk }) to missing.size }

private fun inspectEntryForNightOverride(
    colorName: String, rawHex: String, textBearingIds: Set<String>,
    baseRoleIds: Set<String>, containerRoleIds: Set<String>,
    allOverriddenNames: MutableSet<String>, raisedOverriddenNames: MutableSet<String>,
) {
    if (!isSurfaceCandidate(rawHex) || colorName in textBearingIds) return
    allOverriddenNames += colorName
    if (resolveRaisedStatus(colorName, rawHex, baseRoleIds, containerRoleIds)) raisedOverriddenNames += colorName }

internal fun buildNightV31OverrideMap(
    dayXmlText: String, nightXmlText: String, materialYouIds: Collection<String>,
    containerRoleIds: Set<String>, baseRoleIds: Set<String>, textBearingIds: Set<String>,
    bgHex: String, containerHex: String,
): Map<String, String> {
    val canonicalBg = canonicalizeHex(bgHex) ?: throw PatchException("Provided background is not a valid opaque hex: $bgHex")
    val canonicalContainer = canonicalizeHex(containerHex) ?: throw PatchException("Provided container is not a valid opaque hex: $containerHex")
    val allNames = linkedSetOf<String>()
    val raisedNames = linkedSetOf<String>()

    listOf(dayXmlText, nightXmlText).forEach { xml ->
        REGEX_COLOR_TAG.findAll(xml).forEach { match ->
            val (name, raw) = match.destructured
            inspectEntryForNightOverride(name, raw.trim(), textBearingIds, baseRoleIds, containerRoleIds, allNames, raisedNames) } }
    materialYouIds.forEach { id ->
        if (id !in textBearingIds) {
            allNames += id
            if (id !in baseRoleIds && id in containerRoleIds) raisedNames += id } }
    return allNames.toSortedSet().associateWith { id -> if (id in raisedNames) canonicalContainer else canonicalBg } }

internal fun renderNightV31XmlDocument(overrides: Map<String, String>): String = buildString {
    appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
    appendLine("<resources>")
    overrides.toSortedMap().forEach { (name, hex) ->
        val valid = canonicalizeHex(hex) ?: throw PatchException("Invalid override hex token: $hex")
        appendLine("""    <color name="$name">$valid</color>""") }
    appendLine("</resources>") }

private fun evaluateLStarSurfaceTone(roleString: String, toneValue: Double, bgHex: String, containerHex: String): String = when {
    REGEX_SYS_DARK_CONTAINER_ROLES.matches(roleString) -> containerHex
    REGEX_SYS_DARK_BASE_ROLES.matches(roleString) -> bgHex
    toneValue >= 12.0 -> containerHex
    else -> bgHex }

internal fun rewriteLStarColorSelectors(xmlText: String, bgHex: String, containerHex: String = bgHex): Pair<String, Int> {
    val canonicalBg = canonicalizeHex(bgHex) ?: throw PatchException("Provided background is not a valid opaque hex: $bgHex")
    val canonicalContainer = canonicalizeHex(containerHex) ?: throw PatchException("Provided container is not a valid opaque hex: $containerHex")
    var totalRewrites = 0

    val transformed = REGEX_LSTAR_SELECTOR_ITEM.replace(xmlText) { match ->
        val (prefix, systemRole, toneString) = match.destructured
        val tone = toneString.toDoubleOrNull() ?: return@replace match.value
        if (tone > 24.0) return@replace match.value

        val targetHex = evaluateLStarSurfaceTone(systemRole, tone, canonicalBg, canonicalContainer)
        totalRewrites++
        val colorPattern = Regex("""${Regex.escape(prefix)}:color="[^"]*"""")
        val lStarPattern = Regex("""\s+${Regex.escape(prefix)}:lStar="[^"]*"""")
        match.value.replace(colorPattern, """$prefix:color="$targetHex"""").replace(lStarPattern, "") }
    return transformed to totalRewrites }

private val REGEX_VECTOR_PATH = Regex("""<path\b""")
private val REGEX_VECTOR_FILL_ATTR = Regex("""(n\d+:fillColor)="([^"]+)"""")

internal fun rewriteVectorDrawableSurfaceFills(xmlText: String, bgHex: String, containerHex: String = bgHex): Pair<String, Int> {
    val canonicalBg = canonicalizeHex(bgHex) ?: throw PatchException("Provided background is not a valid opaque hex: $bgHex")
    val canonicalContainer = canonicalizeHex(containerHex) ?: throw PatchException("Provided container is not a valid opaque hex: $containerHex")
    val pathCount = REGEX_VECTOR_PATH.findAll(xmlText).count()
    if (pathCount !in 1..3) return xmlText to 0

    var modified = 0
    val transformed = REGEX_VECTOR_FILL_ATTR.replace(xmlText) { match ->
        val (attrPrefix, fillValue) = match.destructured
        if (!isSurfaceCandidate(fillValue)) return@replace match.value
        val targetHex = if (isElevatedTierColor(fillValue)) canonicalContainer else canonicalBg
        modified++
        """$attrPrefix="$targetHex"""" }
    return transformed to modified }

private val REGEX_PREF_DEFAULT_ATTR = Regex("""([\s](?:[\w.]+:)?defaultValue=")[^"]*(")""")
private val REGEX_DYNAMIC_PREF_ELEMENT = Regex("""([\s](?:[\w.]+:)?key="$PREF_KEY_DYNAMIC_PALETTE"[^>]*?)(/>|>)""")

internal fun patchPreferenceElementDefaultFalse(xmlText: String): String {
    if (!xmlText.contains(PREF_KEY_DYNAMIC_PALETTE)) return xmlText
    return REGEX_DYNAMIC_PREF_ELEMENT.replace(xmlText) { match ->
        val (prefix, closing) = match.destructured
        val updatedTag = if (REGEX_PREF_DEFAULT_ATTR.containsMatchIn(prefix)) {
            REGEX_PREF_DEFAULT_ATTR.replace(prefix, "$1false$2")
        } else {
            prefix + """ android:defaultValue="false"""" }
        updatedTag + closing } }

private val REGEX_STYLE_TAG_OPEN = Regex("""<style\s+name="([^"]+)"([^>]*)>""")
private val REGEX_STYLE_PARENT_ATTRIBUTE = Regex("""parent="([^"]+)"""")
private val REGEX_STYLE_ITEM_TAG = Regex("""<item\s+name="([^"]+)"[^>]*>\s*([^<]*?)\s*<""")

internal data class ParsedThemeStyle(val styleIdentifier: String, val parentIdentifier: String?, val attributesMap: Map<String, String>)

private val WINDOW_BACKGROUND_COLOR_NAMES = setOf("windowBackground", "android:windowBackground", "colorBackground", "android:colorBackground")

private fun resolveStyleParentName(name: String, tagTail: String): String? {
    val explicitParent = REGEX_STYLE_PARENT_ATTRIBUTE.find(tagTail)?.groupValues?.get(1)?.removePrefix("@style/")
    if (explicitParent != null) return explicitParent
    val derivedPrefix = name.substringBeforeLast('.', missingDelimiterValue = "")
    return if (derivedPrefix.isNotEmpty()) derivedPrefix else null }

private fun extractStyleInnerContent(fullXml: String, matchResult: MatchResult): String {
    val tail = matchResult.groupValues[2]
    if (tail.trimEnd().endsWith("/")) return ""
    val closingIndex = fullXml.indexOf("</style>", matchResult.range.last + 1)
    return if (closingIndex < 0) "" else fullXml.substring(matchResult.range.last + 1, closingIndex) }

internal fun parseStyleElementsFromXml(xmlText: String): List<ParsedThemeStyle> =
    REGEX_STYLE_TAG_OPEN.findAll(xmlText).map { match ->
        val name = match.groupValues[1]
        val parent = resolveStyleParentName(name, match.groupValues[2])
        val body = extractStyleInnerContent(xmlText, match)
        val items = REGEX_STYLE_ITEM_TAG.findAll(body).associate { it.groupValues[1] to it.groupValues[2].trim() }
        ParsedThemeStyle(name, parent, items)
    }.toList()

private fun traceAttributeReference(styleHierarchy: List<ParsedThemeStyle>, attrName: String, visitedAttrs: MutableSet<String>): String? {
    if (!visitedAttrs.add(attrName)) return null
    for (node in styleHierarchy) {
        val rawValue = node.attributesMap[attrName] ?: continue
        return when {
            rawValue.startsWith("@color/") -> rawValue.removePrefix("@color/")
            rawValue.startsWith("?") -> traceAttributeReference(styleHierarchy, rawValue.removePrefix("?").removePrefix("attr/"), visitedAttrs)
            else -> null } }
    return null }

private fun buildThemeInheritanceChain(
    qualifierName: String, initialStyle: ParsedThemeStyle,
    stylesByQualifier: Map<String, Map<String, ParsedThemeStyle>>, stylesByGlobalName: Map<String, ParsedThemeStyle>,
): List<ParsedThemeStyle> {
    val hierarchy = mutableListOf<ParsedThemeStyle>()
    val visitedNames = mutableSetOf<String>()
    var currentQualifier: String? = qualifierName
    var currentStyle: ParsedThemeStyle? = initialStyle

    while (currentStyle != null && visitedNames.add(currentStyle.styleIdentifier)) {
        hierarchy += currentStyle
        val parentKey = currentStyle.parentIdentifier ?: break
        val nextStyle = currentQualifier?.let { stylesByQualifier[it]?.get(parentKey) } ?: stylesByGlobalName[parentKey]
        if (nextStyle != null) {
            currentQualifier = stylesByQualifier.entries.firstOrNull { it.value[parentKey] === nextStyle }?.key ?: currentQualifier }
        currentStyle = nextStyle }
    return hierarchy }

private fun extractWindowBackgroundIdsFromChain(hierarchy: List<ParsedThemeStyle>, collectedIds: MutableSet<String>) {
    val matchingStyle = hierarchy.firstOrNull { node -> WINDOW_BACKGROUND_COLOR_NAMES.any { it in node.attributesMap } } ?: return
    for (targetAttr in WINDOW_BACKGROUND_COLOR_NAMES) {
        val raw = matchingStyle.attributesMap[targetAttr] ?: continue
        when {
            raw.startsWith("@color/") -> collectedIds += raw.removePrefix("@color/")
            raw.startsWith("?") -> traceAttributeReference(hierarchy, raw.removePrefix("?").removePrefix("attr/"), mutableSetOf())?.let { collectedIds += it } } } }

internal fun discoverWindowBackgroundResourceIds(resRoot: File): Set<String> {
    val valuesDirs = resRoot.subdirsNamed("values")
    val stylesByDir = mutableMapOf<String, Map<String, ParsedThemeStyle>>()
    for (dir in valuesDirs) {
        val parsedInDir = mutableMapOf<String, ParsedThemeStyle>()
        dir.xmlChildFiles()
            .filter { it.name.equals("styles.xml", true) || it.name.equals("themes.xml", true) }
            .forEach { xmlFile -> parseStyleElementsFromXml(xmlFile.readText()).forEach { parsedInDir[it.styleIdentifier] = it } }
        stylesByDir[dir.name] = parsedInDir }

    val globalStyleMap = linkedMapOf<String, ParsedThemeStyle>()
    for (dir in valuesDirs) {
        stylesByDir.getValue(dir.name).toSortedMap().forEach { (k, v) -> globalStyleMap.putIfAbsent(k, v) } }

    val outputIds = linkedSetOf<String>()
    for (dir in valuesDirs) {
        val q = dir.name
        val localStyles = stylesByDir.getValue(q)
        for (styleKey in localStyles.keys.sorted()) {
            val chain = buildThemeInheritanceChain(q, localStyles.getValue(styleKey), stylesByDir, globalStyleMap)
            extractWindowBackgroundIdsFromChain(chain, outputIds) } }
    return outputIds }

private val REGEX_THEME_BACKGROUND_ITEM = Regex("""(<item\s+name="(?:android:)?(?:colorBackground|windowBackground)"[^>]*>\s*)(?:\?[^<]+|@android:color/transparent)(\s*</item>)""")
private val REGEX_HEX_ATTR_REFERENCE = Regex("""\?(?:[^"]*?)(0x[0-9a-fA-F]+)""")

private fun replaceThemeBackgroundValues(resRoot: File, collectedAttrs: MutableSet<String>): Int {
    var modifiedCount = 0
    resRoot.subdirsNamed("values").flatMap { it.xmlChildFiles() }.forEach { file ->
        file.rewriteIfChanged { original ->
            REGEX_THEME_BACKGROUND_ITEM.findAll(original).forEach { m -> REGEX_HEX_ATTR_REFERENCE.find(m.value)?.let { collectedAttrs += it.groupValues[1] } }
            REGEX_THEME_BACKGROUND_ITEM.replace(original) { m -> modifiedCount++; "${m.groupValues[1]}@color/amoled_window_background${m.groupValues[2]}" } } }
    return modifiedCount }

private fun inspectLayoutCoordinatorWindowAttrs(resRoot: File, collectedAttrs: MutableSet<String>) {
    val pattern = Regex("""<androidx\.coordinatorlayout\.widget\.CoordinatorLayout[^>]*?\w+:background="\?[^"]*?(0x[0-9a-fA-F]+)[^"]*"""")
    resRoot.subdirsNamed("layout").flatMap { it.xmlChildFiles() }.forEach { file ->
        pattern.findAll(file.readText()).forEach { collectedAttrs += it.groupValues[1] } } }

private fun replaceLayoutBackgroundReferences(resRoot: File, attrKeys: Set<String>): Int {
    if (attrKeys.isEmpty()) return 0
    var count = 0
    val keysPattern = attrKeys.joinToString("|")
    val standardBgRegex = Regex("""(?:android:)?background="\?[^"]*?(?:$keysPattern)[^"]*"""")
    val prefixedBgRegex = Regex("""\w+:background="\?[^"]*?(?:$keysPattern)[^"]*"""")

    resRoot.subdirsNamed("layout").flatMap { it.xmlChildFiles() }.forEach { file ->
        file.rewriteIfChanged { original ->
            var next = standardBgRegex.replace(original) { count++; """android:background="@color/amoled_window_background"""" }
            next = prefixedBgRegex.replace(next) { m ->
                val prefix = m.value.substringBefore(":background")
                count++
                """$prefix:background="@color/amoled_window_background"""" }
            next } }
    return count }

internal fun redirectWindowBackgroundReferences(resRoot: File): Int {
    val windowAttrs = mutableSetOf<String>()
    var count = replaceThemeBackgroundValues(resRoot, windowAttrs)
    if (windowAttrs.isEmpty()) inspectLayoutCoordinatorWindowAttrs(resRoot, windowAttrs)
    if (windowAttrs.isEmpty()) windowAttrs += "0x7f05018f"
    count += replaceLayoutBackgroundReferences(resRoot, windowAttrs)
    return count }

internal fun resolveCardFillHex(surfaceHex: String): String? {
    val canonical = canonicalizeHex(surfaceHex) ?: return null
    return if (canonical.length == 9 && canonical.startsWith("#ff", true)) "#" + canonical.substring(3) else canonical }

internal fun createAmoledCardDrawableXml(resRoot: File, surfaceHex: String): Boolean {
    val drawableDirectory = File(resRoot, "drawable")
    if (!drawableDirectory.isDirectory) drawableDirectory.mkdirs()
    val cardFile = File(drawableDirectory, "amoled_preference_card.xml")
    if (cardFile.exists()) return false

    val fillColor = resolveCardFillHex(surfaceHex) ?: return false
    cardFile.writeText("""<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"><solid android:color="$fillColor"/><corners android:radius="16dp"/></shape>""")
    return true }

private fun replacePreferenceItemCardBackgrounds(originalXml: String): Pair<String, Int> {
    val bgPattern1 = Regex("""[\w:]*background="[^"]*selectableItemBackground[^"]*"""")
    val bgPattern2 = Regex("""[\w:]*background="[^"]*0101030E[^"]*"""")
    val marginAnchor = Regex("""(background="@drawable/amoled_preference_card")""")
    val extraMargins = """ android:layout_marginStart="8dp" android:layout_marginEnd="8dp" android:layout_marginTop="4dp" android:layout_marginBottom="4dp""""

    var replacementCount = 0
    var nextText = bgPattern1.replace(originalXml) { m -> replacementCount++; val tag = m.value.substringBefore("="); """$tag="@drawable/amoled_preference_card"""" }
    nextText = bgPattern2.replace(nextText) { m -> replacementCount++; val tag = m.value.substringBefore("="); """$tag="@drawable/amoled_preference_card"""" }
    if (nextText != originalXml) nextText = marginAnchor.replace(nextText) { m -> "${m.groupValues[1]}$extraMargins" }
    return nextText to replacementCount }

internal fun applyCardBackgroundsToPreferenceLayouts(resRoot: File): Int {
    var totalCardsModified = 0
    resRoot.subdirsNamed("layout").flatMap { it.xmlChildFiles() }.forEach { file ->
        file.rewriteIfChanged { content ->
            val (updated, count) = replacePreferenceItemCardBackgrounds(content)
            if (count > 0) totalCardsModified += count
            updated } }
    return totalCardsModified }

private val REGEX_STYLE_TEXT_COLOR_ITEM = Regex("""textColor[A-Za-z]*">@color/([^<]+)<""")

internal fun extractStyleTextColorIdentifiers(resRoot: File): List<String> =
    resRoot.subdirsNamed("values").flatMap { it.xmlChildFiles() }
        .filter { it.name.equals("styles.xml", true) }
        .flatMap { REGEX_STYLE_TEXT_COLOR_ITEM.findAll(it.readText()).map { m -> m.groupValues[1] } }
        .distinct()

private val REGEX_STYLE_TEXT_ATTR = Regex("""<item\s+name="(?:android:)?text[A-Za-z]*"\s*>\s*@color/([^<]+)\s*<""")
private val REGEX_LAYOUT_TEXT_ATTR = Regex("""(?:android:|\bn\d+:)text[A-Za-z]*\s*=\s*"@color/([^"]+)"""")

internal fun discoverTextBearingColorIdentifiers(resRoot: File): Set<String> {
    val detectedIds = linkedSetOf<String>()
    resRoot.subdirsNamed("values").flatMap { it.xmlChildFiles() }
        .filter { it.name.equals("styles.xml", true) }
        .forEach { file -> REGEX_STYLE_TEXT_ATTR.findAll(file.readText()).forEach { detectedIds += it.groupValues[1] } }

    resRoot.subdirsNamed("layout").forEach { dir ->
        dir.walkTopDown().filter { it.isFile && it.extension.equals("xml", true) }.sortedBy { it.absolutePath }
            .forEach { file -> REGEX_LAYOUT_TEXT_ATTR.findAll(file.readText()).forEach { detectedIds += it.groupValues[1] } } }
    return detectedIds }

internal fun discoverColorSelectorNames(resRoot: File): Set<String> =
    resRoot.subdirsNamed("color").flatMap { it.xmlChildFiles() }.mapTo(linkedSetOf()) { it.nameWithoutExtension }

internal fun discoverDeclaredColorNames(resRoot: File): Set<String> {
    val declared = linkedSetOf<String>()
    resRoot.subdirsNamed("values").flatMap { it.xmlChildFiles() }
        .filter { it.name.equals("colors.xml", true) }
        .forEach { file -> REGEX_COLOR_TAG.findAll(file.readText()).forEach { declared += it.groupValues[1] } }
    declared += discoverColorSelectorNames(resRoot)
    return declared }

internal fun discoverColorNamesInFile(xmlFile: File): Set<String> =
    if (!xmlFile.isFile) emptySet() else REGEX_COLOR_TAG.findAll(xmlFile.readText()).mapTo(linkedSetOf()) { it.groupValues[1] }

internal fun locateNightColorsXmlFile(resRoot: File): File? {
    val standardPath = File(resRoot, RES_PATH_NIGHT_COLORS)
    if (standardPath.isFile) return standardPath
    return resRoot.subdirsNamed("values-night").map { File(it, "colors.xml") }.firstOrNull { it.isFile } }

private fun setupTwoTierSurfaceEnvironment(resRoot: File, useBgForSurfaces: Boolean, surfaceHex: String) {
    if (useBgForSurfaces) return
    redirectWindowBackgroundReferences(resRoot)
    val nightFile = File(resRoot, RES_PATH_NIGHT_COLORS)
    if (nightFile.isFile) {
        val originalXml = nightFile.readText()
        if (!originalXml.contains("amoled_window_background")) {
            val injected = originalXml.replace("</resources>", "    <color name=\"amoled_window_background\">#ff000000</color>\n</resources>")
            nightFile.writeText(injected) } }
    val cardGenerated = createAmoledCardDrawableXml(resRoot, surfaceHex)
    val layoutCount = applyCardBackgroundsToPreferenceLayouts(resRoot)
    println("[AMOLED] preferenceCards created=$cardGenerated layouts=$layoutCount") }

private data class DayProcessingSummary(
    val daySurfaceCount: Int, val containerCount: Int, val textCount: Int, val accentCount: Int, val inkInjectedCount: Int, )

private fun executeDayColorsTransform(
    dayFile: File, bgHex: String, surfaceHex: String, containerIds: Set<String>, baseIds: Set<String>,
    textBearingIds: Set<String>, neededTextIds: List<String>, declaredColors: Set<String>,
    configuredTextHex: String?, configuredAccentHex: String?,
): DayProcessingSummary {
    if (!dayFile.isFile) return DayProcessingSummary(0, 0, 0, 0, 0)
    var currentXml = dayFile.readText()
    val (afterSurfaces, stats) = rewriteXmlSurfaces(
        xmlText = currentXml, backgroundHex = bgHex, containerHex = surfaceHex,
        containerTagNames = containerIds, baseTagNames = baseIds, excludedTagNames = textBearingIds, )
    currentXml = afterSurfaces
    var replacedText = 0
    var injectedInk = 0
    var replacedAccent = 0

    if (configuredTextHex != null) {
        val (textTransformed, tCount) = rewriteXmlTextInk(currentXml, configuredTextHex)
        val (inkInjected, iCount) = injectDayTextInkDefaults(textTransformed, neededTextIds, declaredColors, "#202124")
        currentXml = inkInjected
        replacedText = tCount
        injectedInk = iCount }
    if (configuredAccentHex != null) {
        val (accentTransformed, aCount) = rewriteXmlAccentInk(currentXml, configuredAccentHex)
        currentXml = accentTransformed
        replacedAccent = aCount }
    dayFile.writeText(currentXml)
    return DayProcessingSummary(stats.baseCount, stats.containerCount, replacedText, replacedAccent, injectedInk) }

private data class NightProcessingSummary(
    val nightSurfaceCount: Int, val containerCount: Int, val textCount: Int, val accentCount: Int, val nightInkDeclaredCount: Int, )

private fun executeNightColorsTransform(
    nightFile: File?, originalDayXml: String, resRoot: File,
    bgHex: String, surfaceHex: String, containerIds: Set<String>, baseIds: Set<String>,
    textBearingIds: Set<String>, configuredTextHex: String?, configuredAccentHex: String?,
): NightProcessingSummary {
    if (nightFile == null || !nightFile.isFile) return NightProcessingSummary(0, 0, 0, 0, 0)
    var currentXml = nightFile.readText()
    val (afterSurfaces, stats) = rewriteXmlSurfaces(
        xmlText = currentXml, backgroundHex = bgHex, containerHex = surfaceHex,
        containerTagNames = containerIds, baseTagNames = baseIds, excludedTagNames = textBearingIds, )
    currentXml = afterSurfaces
    var replacedText = 0
    var replacedAccent = 0
    var declaredInk = 0

    if (configuredTextHex != null) {
        val (textTransformed, tCount) = rewriteXmlTextInk(currentXml, configuredTextHex)
        val darkInkIds = extractDarkInkIdsFromDayXml(originalDayXml, textBearingIds)
        val nightDeclared = discoverColorNamesInFile(nightFile)
        val selectorNames = discoverColorSelectorNames(resRoot)
        val (inkDeclared, dCount) = injectNightTextInkDeclarations(textTransformed, darkInkIds, nightDeclared, configuredTextHex, selectorNames)
        currentXml = inkDeclared
        replacedText = tCount
        declaredInk = dCount }
    if (configuredAccentHex != null) {
        val (accentTransformed, aCount) = rewriteXmlAccentInk(currentXml, configuredAccentHex)
        currentXml = accentTransformed
        replacedAccent = aCount }
    nightFile.writeText(currentXml)
    return NightProcessingSummary(stats.baseCount, stats.containerCount, replacedText, replacedAccent, declaredInk) }

private fun rewriteMaterialYouSystemRolesInDirs(
    resRoot: File, disableDynamicPalette: Boolean, bgHex: String, surfaceHex: String, collectedNeutralIds: MutableList<String>,
): Int {
    if (!disableDynamicPalette) return 0
    var totalRewrites = 0
    ANDROID_S_SYS_COLOR_FILES.forEach { relativePath ->
        val targetFile = File(resRoot, relativePath)
        if (targetFile.isFile) {
            val content = targetFile.readText()
            collectedNeutralIds += discoverMaterialYouNeutralIds(content)
            val (updated, count) = rewriteMaterialYouSystemRoles(content, bgHex, surfaceHex)
            targetFile.writeText(updated)
            totalRewrites += count } }
    return totalRewrites }

private fun rewriteSelectorLStarEntriesInDirs(resRoot: File, disableDynamicPalette: Boolean, bgHex: String, surfaceHex: String): Int {
    if (!disableDynamicPalette) return 0
    var totalRewrites = 0
    resRoot.listFiles().orEmpty().filter { it.isDirectory && (it.name == "color" || it.name.startsWith("color-")) }.sortedBy { it.name }
        .forEach { dir ->
            dir.xmlChildFiles().forEach { file ->
                val (updated, count) = rewriteLStarColorSelectors(file.readText(), bgHex, surfaceHex)
                if (count > 0) { file.writeText(updated); totalRewrites += count } } }
    return totalRewrites }

private fun rewriteVectorDrawablesInDirs(resRoot: File, bgHex: String, surfaceHex: String): Int {
    var totalFills = 0
    resRoot.subdirsNamed("drawable").forEach { dir ->
        dir.xmlChildFiles().forEach { file ->
            val text = file.readText()
            if (text.contains("<path")) {
                val (updated, count) = rewriteVectorDrawableSurfaceFills(text, bgHex, surfaceHex)
                if (count > 0) { file.writeText(updated); totalFills += count } } } }
    return totalFills }

private fun patchDynamicPreferenceXmlInDirs(resRoot: File): Int {
    val xmlDir = File(resRoot, "xml")
    if (!xmlDir.isDirectory) return 0
    var filesChanged = 0
    xmlDir.xmlChildFiles().forEach { file -> if (file.rewriteIfChanged(::patchPreferenceElementDefaultFalse)) filesChanged++ }
    return filesChanged }

internal data class AmoledExecutionSummary(
    val dayColorsReplaced: Int, val nightColorsReplaced: Int, val v31MaterialYouReplaced: Int, val nightV31Overrides: Int,
    val containerColorsReplaced: Int = 0, val textColorsReplaced: Int = 0, val accentColorsReplaced: Int = 0,
    val textIdsInjected: Int = 0, val nightTextDeclared: Int = 0, val textBearingIds: Int = 0,
    val lStarSelectorsReplaced: Int = 0, val drawableSurfaceFills: Int = 0, val preferenceFilesChanged: Int, val windowBackgroundIds: Int = 0, )

internal fun executeAmoledResourceTransformation(
    resRoot: File, backgroundHex: String, textColorHex: String? = null,
    accentColorHex: String? = null, disableDynamicColors: Boolean = true,
    surfaceUsesBackground: Boolean = true, surfaceColorHex: String? = null,
): AmoledExecutionSummary {
    val canonicalBg = canonicalizeHex(backgroundHex) ?: throw PatchException("Provided background is not a valid opaque hex: $backgroundHex")
    val canonicalSurface = if (surfaceUsesBackground) canonicalBg else {
        canonicalizeHex(surfaceColorHex.orEmpty()) ?: throw PatchException("Provided surface color is not an opaque hex: ${surfaceColorHex ?: "null"}") }

    val styleTextRefs = extractStyleTextColorIdentifiers(resRoot)
    val declaredColorNames = discoverDeclaredColorNames(resRoot)
    val textBearingIds = discoverTextBearingColorIdentifiers(resRoot)

    val roleTiers = mutableMapOf<String, Boolean>()
    ANDROID_S_SYS_COLOR_FILES.forEach { relPath ->
        val f = File(resRoot, relPath)
        if (f.isFile) roleTiers += discoverMaterialYouRoleMap(f.readText()) }

    val containerNames = if (surfaceUsesBackground) emptySet() else roleTiers.filterValues { it }.keys
    setupTwoTierSurfaceEnvironment(resRoot, surfaceUsesBackground, canonicalSurface)
    val baseNames = if (surfaceUsesBackground) emptySet() else discoverWindowBackgroundResourceIds(resRoot)

    val dayFile = File(resRoot, RES_PATH_DAY_COLORS)
    val nightFile = locateNightColorsXmlFile(resRoot)
    val originalDayXml = if (dayFile.isFile) dayFile.readText() else ""
    val originalNightXml = if (nightFile?.isFile == true) nightFile.readText() else ""

    val daySummary = executeDayColorsTransform(
        dayFile, canonicalBg, canonicalSurface, containerNames, baseNames,
        textBearingIds, styleTextRefs, declaredColorNames, textColorHex, accentColorHex, )
    val nightSummary = executeNightColorsTransform(
        nightFile, originalDayXml, resRoot, canonicalBg, canonicalSurface,
        containerNames, baseNames, textBearingIds, textColorHex, accentColorHex, )

    val collectedNeutralIds = mutableListOf<String>()
    val matYouCount = rewriteMaterialYouSystemRolesInDirs(resRoot, disableDynamicColors, canonicalBg, canonicalSurface, collectedNeutralIds)
    val overrides = buildNightV31OverrideMap(
        originalDayXml, originalNightXml, collectedNeutralIds, containerNames, baseNames, textBearingIds, canonicalBg, canonicalSurface, )
    val lStarCount = rewriteSelectorLStarEntriesInDirs(resRoot, disableDynamicColors, canonicalBg, canonicalSurface)
    val drawableFillsCount = rewriteVectorDrawablesInDirs(resRoot, canonicalBg, canonicalSurface)

    if (overrides.isNotEmpty()) {
        val nightV31File = File(resRoot, RES_PATH_NIGHT_V31_COLORS)
        nightV31File.parentFile?.mkdirs()
        nightV31File.writeText(renderNightV31XmlDocument(overrides)) }

    val prefChanges = if (disableDynamicColors) patchDynamicPreferenceXmlInDirs(resRoot) else 0

    return AmoledExecutionSummary(
        dayColorsReplaced = daySummary.daySurfaceCount, nightColorsReplaced = nightSummary.nightSurfaceCount,
        v31MaterialYouReplaced = matYouCount, nightV31Overrides = overrides.size,
        containerColorsReplaced = daySummary.containerCount + nightSummary.containerCount,
        textColorsReplaced = daySummary.textCount + nightSummary.textCount,
        accentColorsReplaced = daySummary.accentCount + nightSummary.accentCount,
        textIdsInjected = daySummary.inkInjectedCount, nightTextDeclared = nightSummary.nightInkDeclaredCount,
        textBearingIds = textBearingIds.size, lStarSelectorsReplaced = lStarCount,
        drawableSurfaceFills = drawableFillsCount, preferenceFilesChanged = prefChanges,
        windowBackgroundIds = baseNames.size, ) }

private val PREFERENCE_SCREEN_FILTER_HINTS = setOf(
    "brave_night_mode", "brave_bottom_toolbar", "brave_disable_sharing", "brave_rewards",
    "brave_enable_tab_groups", "show_undo_when_tabs", "ads_switch", "bookmark_bar",
    "address_bar", "ui_theme", "brave_customize_menu", "enable_multi_windows",
    "toolbar_shortcut", "navigation_section", "general_section", )

internal fun collectReferencedMethodStrings(targetMethod: com.android.tools.smali.dexlib2.iface.Method): List<String> =
    targetMethod.implementation?.instructions?.mapNotNull { (it as? ReferenceInstruction)?.reference as? StringReference }?.map { it.string }.orEmpty()

internal fun isDedicatedDynamicPaletteGetter(targetMethod: com.android.tools.smali.dexlib2.iface.Method): Boolean {
    if (targetMethod.returnType != "Z") return false
    val referencedStrings = collectReferencedMethodStrings(targetMethod)
    val mentionsDynamicPalette = referencedStrings.any { it == PREF_KEY_DYNAMIC_PALETTE || it == FEATURE_FLAG_DYNAMIC_PALETTE }
    if (!mentionsDynamicPalette) return false
    return PREFERENCE_SCREEN_FILTER_HINTS.none { hintKey -> referencedStrings.any { it.contains(hintKey) } } }

private fun BytecodePatchContext.disableMaterialYouDynamicColors() {
    val prefAccessorFingerprint = Fingerprint(returnType = "Z", strings = listOf(PREF_KEY_DYNAMIC_PALETTE))
    val flagAccessorFingerprint = Fingerprint(strings = listOf(FEATURE_FLAG_DYNAMIC_PALETTE))

    val prefCandidates = prefAccessorFingerprint.matchAll().map { m -> m.method }
    val flagCandidates = flagAccessorFingerprint.matchAll().flatMap { m -> m.classDef.methods }
    val combinedCandidates = (prefCandidates + flagCandidates).distinctBy { m -> "${m.definingClass}->${m.name}" }

    val gettersToNeutralize = combinedCandidates.filter(::isDedicatedDynamicPaletteGetter)
    if (gettersToNeutralize.isEmpty()) {
        throw PatchException("No Material You dynamic-colors boolean readers found; refusing to ship AMOLED that cannot disable Material You") }

    gettersToNeutralize.forEach { method -> method.addInstructions(0, "const/4 v0, 0x0\nreturn v0") }
    println("[AMOLED] Forced ${gettersToNeutralize.size} dynamic-colors reader(s) to return false") }

private data class ResolvedAmoledThemeSettings(
    val bgHex: String, val textHex: String?, val accentHex: String?,
    val surfaceHex: String?, val disableDynamic: Boolean, val useBgForSurfaces: Boolean, )

private fun fetchConfiguredOpaqueHex(optionKey: String): String? =
    (braveAmoledThemePatch.options[optionKey].value as? String)?.let { canonicalizeHex(it) }

private fun fetchConfiguredBoolean(optionKey: String, fallbackValue: Boolean): Boolean =
    (braveAmoledThemePatch.options[optionKey].value as? Boolean) ?: fallbackValue

private fun buildAmoledThemeSettings(): ResolvedAmoledThemeSettings {
    val bg = fetchConfiguredOpaqueHex("backgroundColor") ?: canonicalizeHex("#000000") ?: throw PatchException("Invalid AMOLED background color")
    return ResolvedAmoledThemeSettings(
        bgHex = bg, textHex = fetchConfiguredOpaqueHex("textColor"),
        accentHex = fetchConfiguredOpaqueHex("accentColor"), surfaceHex = fetchConfiguredOpaqueHex("surfaceColor"),
        disableDynamic = fetchConfiguredBoolean("disableDynamicColors", true),
        useBgForSurfaces = fetchConfiguredBoolean("surfaceUsesBackground", true), ) }

private fun outputAmoledSummaryLog(s: ResolvedAmoledThemeSettings, r: AmoledExecutionSummary) {
    val tierActive = !s.useBgForSurfaces
    val txt = s.textHex ?: "-"
    val acc = s.accentHex ?: "-"
    val surf = s.surfaceHex ?: "-"
    println("[AMOLED] background=${s.bgHex} text=$txt accent=$acc surface=$surf surfaceTier=$tierActive dayReplaced=${r.dayColorsReplaced} nightReplaced=${r.nightColorsReplaced} containerReplaced=${r.containerColorsReplaced} materialYouReplaced=${r.v31MaterialYouReplaced} nightV31Overrides=${r.nightV31Overrides} textReplaced=${r.textColorsReplaced} accentReplaced=${r.accentColorsReplaced} textIdsInjected=${r.textIdsInjected} nightTextDeclared=${r.nightTextDeclared} textBearingIds=${r.textBearingIds} lStarSelectors=${r.lStarSelectorsReplaced} drawableFills=${r.drawableSurfaceFills} prefFiles=${r.preferenceFilesChanged} windowBackgroundIds=${r.windowBackgroundIds}") }

private fun runAmoledThemeResourcePipeline(resDir: File) {
    if (!resDir.isDirectory) throw PatchException("Decoded res/ directory not found")
    val themeSettings = buildAmoledThemeSettings()
    val summary = executeAmoledResourceTransformation(
        resRoot = resDir, backgroundHex = themeSettings.bgHex, textColorHex = themeSettings.textHex,
        accentColorHex = themeSettings.accentHex, disableDynamicColors = themeSettings.disableDynamic,
        surfaceUsesBackground = themeSettings.useBgForSurfaces, surfaceColorHex = themeSettings.surfaceHex, )
    if (summary.dayColorsReplaced + summary.nightColorsReplaced + summary.nightV31Overrides == 0) {
        throw PatchException("No dark surface colors matched; refusing to ship an empty AMOLED rewrite") }
    outputAmoledSummaryLog(themeSettings, summary) }

private val braveAmoledResourcePatch: ResourcePatch = resourcePatch(
    name = "Brave AMOLED theme resources",
    description = "Rewrites Brave color palettes, system roles, and UI drawables to true AMOLED black.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)
    execute {
        decode("res/values*", "res/color*", "res/drawable*", "res/layout*", "res/xml*")
        runAmoledThemeResourcePipeline(get("res")) } }

@Suppress("unused")
val braveAmoledThemePatch: BytecodePatch = bytecodePatch(
    name = "Brave AMOLED theme",
    description = "Applies high-contrast true black (#000000) styling to Brave dark theme chrome. " +
        "Neutral dark surfaces are rewritten to OLED black, with optional elevated tones for cards " +
        "and sheets. Supports custom text and accent colors, and disables Material You dynamic " +
        "wallpaper palettes so custom dark styling remains intact.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)
    dependsOn(braveAmoledResourcePatch)

    val backgroundColor by colorOption(
        key = "backgroundColor", default = "#000000", title = "AMOLED background color",
        description = "Opaque hexadecimal color for primary backgrounds and base window chrome. Use #000000 for pure OLED black.",
        required = true, validator = { hex -> hex == null || canonicalizeHex(hex) != null }, )
    val textColor by colorOption(
        key = "textColor", default = "#f0f2ff", title = "Text ink color",
        description = "Opaque hexadecimal color applied to primary and secondary text labels. Defaults to Brave light slate ink (#f0f2ff).",
        required = true, validator = { hex -> hex == null || canonicalizeHex(hex) != null }, )
    val accentColor by colorOption(
        key = "accentColor", default = "#737ade", title = "Accent highlight color",
        description = "Opaque hexadecimal color for interactive highlights, active switches, and links. Defaults to Brave purple-blue accent (#737ade).",
        required = true, validator = { hex -> hex == null || canonicalizeHex(hex) != null }, )
    val disableDynamicColors by booleanOption(
        key = "disableDynamicColors", default = true, title = "Disable Material You dynamic colors",
        description = "Prevents Android 12+ wallpaper palette extraction from replacing AMOLED dark surfaces. When disabled, dynamic system colors take precedence.",
        required = false, )
    val surfaceUsesBackground by booleanOption(
        key = "surfaceUsesBackground", default = true, title = "Surfaces use the background color",
        description = "When enabled, cards, sheets, and dialogs share the primary background color. When disabled, elevated surfaces use the dedicated surface color below.",
        required = false, )
    val surfaceColor by colorOption(
        key = "surfaceColor", default = "#1e2029", title = "Surface / button color",
        description = "Opaque hexadecimal color for elevated surfaces such as cards and dialogs. Only active when 'Surfaces use the background color' is disabled. Defaults to #1e2029.",
        required = false, validator = { hex -> hex == null || canonicalizeHex(hex) != null }, )

    execute {
        if (disableDynamicColors == false) {
            println("[AMOLED] Leaving Material You getters untouched (user opt-out)")
        } else {
            disableMaterialYouDynamicColors() } } }
