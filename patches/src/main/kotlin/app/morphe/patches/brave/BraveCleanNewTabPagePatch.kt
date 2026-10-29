package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.ResourcePatch
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.filePathOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File

private const val MAX_WALLPAPER_BYTES = 8 * 1024 * 1024L // 8MB limit
private const val PREF_KEY_WALLPAPER_PATH = "Chrome.NtpCustomization.BackgroundImageFilePath"
private const val PREF_KEY_WALLPAPER_TYPE = "Chrome.NtpCustomization.NtpBackgroundImageType"

private val COMPANION_KEYS = listOf(
    "BackgroundImagePortraitInfo",
    "BackgroundImageLandscapeInfo",
    "BackgroundInfo",
    "NtpCustomizationPrimaryColor",
    "IsMvtVisible",
)

private var deployedWallpaperExt: String? = null
private var deployedWallpaperBytes: Long = 0L
private var wallpaperSkipReason: String? = null

private fun detectImageExtension(headerBytes: ByteArray): String? {
    if (headerBytes.size >= 2 &&
        headerBytes[0] == 0xFF.toByte() &&
        headerBytes[1] == 0xD8.toByte()
    ) {
        return "jpg"
    }
    if (headerBytes.size >= 8 &&
        headerBytes[0] == 0x89.toByte() &&
        headerBytes[1] == 0x50.toByte() &&
        headerBytes[2] == 0x4E.toByte() &&
        headerBytes[3] == 0x47.toByte()
    ) {
        return "png"
    }
    if (headerBytes.size >= 12 &&
        headerBytes[0] == 0x52.toByte() &&
        headerBytes[1] == 0x49.toByte() &&
        headerBytes[2] == 0x46.toByte() &&
        headerBytes[3] == 0x46.toByte() &&
        headerBytes[8] == 0x57.toByte() &&
        headerBytes[9] == 0x45.toByte() &&
        headerBytes[10] == 0x42.toByte() &&
        headerBytes[11] == 0x50.toByte()
    ) {
        return "webp"
    }
    return null
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

private fun classMentionsCompanionKeys(classDef: ClassDef): Boolean {
    for (m in classDef.methods) {
        val instructions = m.implementation?.instructions ?: continue
        for (inst in instructions) {
            val str = ((inst as? ReferenceInstruction)?.reference as? StringReference)?.string ?: continue
            if (COMPANION_KEYS.any { key -> str.contains(key) }) {
                return true
            }
        }
    }
    return false
}

private fun hookWallpaperReadSites(
    method: MutableMethod,
    keyString: String,
    isPathHook: Boolean,
    hookedMethods: MutableList<String>,
): Int {
    val instructions = method.implementation?.instructions?.toList() ?: return 0
    val matches = mutableListOf<Triple<Int, Int, Int>>() // injectionIdx, keyReg, resultReg

    instructions.forEachIndexed { constIdx, inst ->
        if (inst.opcode == Opcode.CONST_STRING || inst.opcode == Opcode.CONST_STRING_JUMBO) {
            val refStr = ((inst as? ReferenceInstruction)?.reference as? StringReference)?.string
            if (refStr == keyString) {
                val keyReg = (inst as? OneRegisterInstruction)?.registerA ?: return@forEachIndexed
                val subsequent = instructions.drop(constIdx + 1).take(15)
                val invokeOffset = subsequent.indexOfFirst { it.opcode.name.startsWith("invoke-") }
                if (invokeOffset != -1) {
                    val moveResultIdx = constIdx + 1 + invokeOffset + 1
                    if (moveResultIdx < instructions.size) {
                        val moveResultInst = instructions[moveResultIdx]
                        val isExpectedMoveResult = if (isPathHook) {
                            moveResultInst.opcode == Opcode.MOVE_RESULT_OBJECT
                        } else {
                            moveResultInst.opcode == Opcode.MOVE_RESULT
                        }
                        if (isExpectedMoveResult && moveResultInst is OneRegisterInstruction) {
                            val resultReg = moveResultInst.registerA
                            val injectionIdx = moveResultIdx + 1
                            matches.add(Triple(injectionIdx, keyReg, resultReg))
                        }
                    }
                }
            }
        }
    }

    if (matches.isEmpty()) return 0

    var counter = 0
    matches.sortedByDescending { it.first }.forEach { (injectionIdx, keyReg, resultReg) ->
        val labelTag = if (isPathHook) "skip_wp" else "skip_type"
        val labelName = ":${labelTag}_${method.name}_$injectionIdx"
        val smali = if (keyReg != resultReg) {
            if (isPathHook) {
                """
                    invoke-static {}, ${Constants.BRAVE_EXTENSION_CLASS}->ntpWallpaperPath()Ljava/lang/String;
                    move-result-object v$keyReg
                    if-nez v$keyReg, $labelName
                    move-object v$resultReg, v$keyReg
                    $labelName
                """.trimIndent()
            } else {
                """
                    invoke-static {}, ${Constants.BRAVE_EXTENSION_CLASS}->ntpWallpaperPath()Ljava/lang/String;
                    move-result-object v$keyReg
                    if-nez v$keyReg, $labelName
                    const/4 v$resultReg, 0x1
                    $labelName
                """.trimIndent()
            }
        } else {
            if (isPathHook) {
                """
                    invoke-static {v$resultReg}, ${Constants.BRAVE_EXTENSION_CLASS}->overrideWallpaperPath(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$resultReg
                """.trimIndent()
            } else {
                """
                    invoke-static {v$resultReg}, ${Constants.BRAVE_EXTENSION_CLASS}->overrideWallpaperType(I)I
                    move-result v$resultReg
                """.trimIndent()
            }
        }
        method.addInstructionsWithLabels(injectionIdx, smali)
        val tag = if (isPathHook) "NtpBackground.filePath" else "NtpBackground.imageType"
        hookedMethods.add("${method.definingClass.substringAfterLast('/').removeSuffix(";")}.${method.name} ($tag)")
        counter++
    }

    return counter
}

// ── Resource Patch: Packages custom wallpaper asset into the APK when configured ────────
private val braveCleanNewTabPageResourcePatch: ResourcePatch = resourcePatch(
    name = "Clean Start Page resources",
    description = "Packages opt-in custom New Tab Page wallpaper asset into the application APK.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        deployedWallpaperExt = null
        deployedWallpaperBytes = 0L
        wallpaperSkipReason = null

        val selectedPath = (braveCleanNewTabPagePatch.options["wallpaperImage"]?.value as? String)?.trim().orEmpty()
        if (selectedPath.isEmpty()) {
            wallpaperSkipReason = "no wallpaper image selected (wallpaperImage is empty)"
            return@execute
        }

        val file = File(selectedPath)
        if (!file.isFile) {
            wallpaperSkipReason = "custom wallpaper file not found: $selectedPath"
            println("[Clean Start Page] Skipped: $wallpaperSkipReason")
            return@execute
        }

        val fileLength = file.length()
        if (fileLength == 0L) {
            wallpaperSkipReason = "custom wallpaper file is empty: $selectedPath"
            println("[Clean Start Page] Skipped: $wallpaperSkipReason")
            return@execute
        }

        if (fileLength > MAX_WALLPAPER_BYTES) {
            wallpaperSkipReason = "custom wallpaper file exceeds 8MB limit: $fileLength bytes"
            println("[Clean Start Page] Skipped: $wallpaperSkipReason")
            return@execute
        }

        val headerBytes = ByteArray(12)
        val bytesRead = file.inputStream().use { it.read(headerBytes) }
        val validHeader = if (bytesRead > 0) headerBytes.copyOf(bytesRead) else headerBytes
        val ext = detectImageExtension(validHeader)
        if (ext == null) {
            wallpaperSkipReason = "unrecognized image format (expected JPEG, PNG, or WebP magic bytes): $selectedPath"
            println("[Clean Start Page] Skipped: $wallpaperSkipReason")
            return@execute
        }

        val targetAsset = get("assets/morphe_ntp_wallpaper.$ext")
        targetAsset.parentFile?.mkdirs()
        val content = file.readBytes()
        targetAsset.writeBytes(content)

        deployedWallpaperExt = ext
        deployedWallpaperBytes = content.size.toLong()
        wallpaperSkipReason = null
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
    dependsOn(sharedExtensionPatch, braveCleanNewTabPageResourcePatch)

    val wallpaperImage by filePathOption(
        key = "wallpaperImage",
        default = null,
        title = "Custom NTP wallpaper",
        description = "Image file on this device used as New Tab background; empty keeps the clean blank page.",
        required = false,
    )

    val hideTopSites by booleanOption(
        key = "hideTopSites",
        default = false,
        title = "Hide shortcut tiles",
        description = "Removes most-visited tiles for a blank start page.",
        required = false,
    )

    execute {
        val hookedMethods = mutableListOf<String>()
        val isWallpaperConfigured = deployedWallpaperBytes > 0

        // 1. Force NTP marketing preference gates in PrefService getBoolean at return
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

            val filterMethod = if (isWallpaperConfigured) "filterNtpPrefWithWallpaper" else "filterNtpPref"
            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {p1, v$reg}, ${Constants.BRAVE_EXTENSION_CLASS}->$filterMethod(Ljava/lang/String;Z)Z
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

        var wallpaperReadSitesHooked = 0
        if (isWallpaperConfigured) {
            // Verify ContextUtils by fingerprint before relying on it
            try {
                val contextUtilsFp = Fingerprint(
                    definingClass = "Lorg/chromium/base/ContextUtils;",
                    name = "getApplicationContext",
                    returnType = "Landroid/content/Context;",
                    parameters = emptyList(),
                )
                val matches = contextUtilsFp.matchAll()
                if (matches.isNotEmpty()) {
                    hookedMethods.add("ContextUtils.getApplicationContext")
                } else {
                    println("[Clean Start Page] ContextUtils verification note: ContextUtils.getApplicationContext not found by fingerprint in this target build.")
                }
            } catch (e: Exception) {
                println("[Clean Start Page] ContextUtils verification note: ${e.message}")
            }

            // 4. Hook SharedPreferences reads in NTP background manager class(es)
            try {
                val pathFp = Fingerprint(strings = listOf(PREF_KEY_WALLPAPER_PATH))
                val typeFp = Fingerprint(strings = listOf(PREF_KEY_WALLPAPER_TYPE))
                val pathMatches = pathFp.matchAll()
                val typeMatches = typeFp.matchAll()

                val candidateMethods = (pathMatches + typeMatches)
                    .filter { classMentionsCompanionKeys(it.classDef) }
                    .map { it.method }
                    .distinctBy { "${it.definingClass}->${it.name}" }

                for (candidate in candidateMethods) {
                    wallpaperReadSitesHooked += hookWallpaperReadSites(candidate, PREF_KEY_WALLPAPER_PATH, true, hookedMethods)
                    wallpaperReadSitesHooked += hookWallpaperReadSites(candidate, PREF_KEY_WALLPAPER_TYPE, false, hookedMethods)
                }
            } catch (e: Exception) {
                println("[Clean Start Page] NTP background manager hook note: ${e.message}")
            }
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        val wallpaperSummary = when {
            isWallpaperConfigured -> " (custom wallpaper: $deployedWallpaperBytes bytes, $wallpaperReadSitesHooked read sites hooked)"
            wallpaperSkipReason != null -> " (wallpaper skipped: $wallpaperSkipReason)"
            else -> ""
        }
        println("[Clean Start Page] Neutralized ${hookedMethods.size} NTP marketing & layout components across ${targetClasses.size} classes$wallpaperSummary")
    }
}
