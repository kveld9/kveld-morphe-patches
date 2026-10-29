package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
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
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File
import java.nio.file.Files

private const val MAX_WALLPAPER_BYTES = 20 * 1024 * 1024L // 20MB limit
private const val PREF_KEY_WALLPAPER_PATH = "Chrome.NtpCustomization.BackgroundImageFilePath"
private const val PREF_KEY_WALLPAPER_TYPE = "Chrome.NtpCustomization.NtpBackgroundImageType"

private val COMPANION_KEYS = listOf(
    "BackgroundImagePortraitInfo",
    "BackgroundImageLandscapeInfo",
    "BackgroundInfo",
    "NtpCustomizationPrimaryColor",
    "IsMvtVisible",
)

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
            (it.opcode == Opcode.CONST_STRING || it.opcode == Opcode.CONST_STRING_JUMBO) &&
                (it as? ReferenceInstruction)?.reference?.let { ref ->
                    (ref as? StringReference)?.string == target.searchString
                } == true
        }
        if (constIdx == -1) return@mapNotNull null
        val iputRelOffset = instructions.drop(constIdx + 1).take(25).indexOfFirst {
            it.opcode == Opcode.IPUT_BOOLEAN
        }
        if (iputRelOffset == -1) return@mapNotNull null
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

private fun invokeConsumesRegister(inst: Any, reg: Int): Boolean {
    return when (inst) {
        is FiveRegisterInstruction -> {
            val count = inst.registerCount
            (count >= 1 && inst.registerC == reg) ||
                (count >= 2 && inst.registerD == reg) ||
                (count >= 3 && inst.registerE == reg) ||
                (count >= 4 && inst.registerF == reg) ||
                (count >= 5 && inst.registerG == reg)
        }
        is RegisterRangeInstruction -> {
            val start = inst.startRegister
            val end = start + inst.registerCount
            reg in start until end
        }
        else -> false
    }
}

private fun hookWallpaperReadSites(
    method: MutableMethod,
    keyString: String,
    isPathHook: Boolean,
    hookedMethods: MutableList<String>,
): Int {
    if (method.name == "<init>" || method.name == "<clinit>") return 0

    val instructions = method.implementation?.instructions?.toList() ?: return 0
    val matches = mutableListOf<Pair<Int, Int>>() // injectionIdx, resultReg

    instructions.forEachIndexed { constIdx, inst ->
        if (inst.opcode == Opcode.CONST_STRING || inst.opcode == Opcode.CONST_STRING_JUMBO) {
            val refStr = ((inst as? ReferenceInstruction)?.reference as? StringReference)?.string
            if (refStr == keyString) {
                val keyReg = (inst as? OneRegisterInstruction)?.registerA ?: return@forEachIndexed
                val subsequent = instructions.drop(constIdx + 1).take(15)
                val invokeOffset = subsequent.indexOfFirst { it.opcode.name.startsWith("invoke-") }
                if (invokeOffset != -1) {
                    val invokeInst = subsequent[invokeOffset]
                    if (!invokeConsumesRegister(invokeInst, keyReg)) {
                        return@forEachIndexed
                    }

                    val methodRef = (invokeInst as? ReferenceInstruction)?.reference as? MethodReference
                    val expectedReturnType = if (isPathHook) "Ljava/lang/String;" else "I"
                    if (methodRef != null && methodRef.returnType != expectedReturnType) {
                        return@forEachIndexed
                    }

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
                            matches.add(injectionIdx to resultReg)
                        }
                    }
                }
            }
        }
    }

    if (matches.isEmpty()) return 0

    var counter = 0
    matches.sortedByDescending { it.first }.forEach { (injectionIdx, resultReg) ->
        val invokeOp = if (resultReg <= 15) {
            "invoke-static {v$resultReg}"
        } else {
            "invoke-static/range {v$resultReg .. v$resultReg}"
        }
        val helperMethod = if (isPathHook) {
            "overrideWallpaperPath(Ljava/lang/String;)Ljava/lang/String;"
        } else {
            "overrideWallpaperType(I)I"
        }
        val moveResultOp = if (isPathHook) "move-result-object" else "move-result"

        method.addInstructions(
            injectionIdx,
            """
                $invokeOp, ${Constants.BRAVE_EXTENSION_CLASS}->$helperMethod
                $moveResultOp v$resultReg
            """.trimIndent(),
        )
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
        deployedWallpaperBytes = 0L
        wallpaperSkipReason = null

        val selectedPath = (braveCleanNewTabPagePatch.options["wallpaperImage"]?.value as? String)?.trim().orEmpty()
        if (selectedPath.isEmpty()) {
            wallpaperSkipReason = "no wallpaper image selected (wallpaperImage is empty)"
            return@execute
        }

        val file = File(selectedPath)
        val fileName = file.name
        if (Files.isSymbolicLink(file.toPath()) || !file.isFile) {
            wallpaperSkipReason = "custom wallpaper file not found: $fileName"
            println("[Clean Start Page] Skipped: $wallpaperSkipReason")
            return@execute
        }

        val maxCap = (MAX_WALLPAPER_BYTES + 1).toInt()
        val content = file.inputStream().use { it.readNBytes(maxCap) }
        if (content.isEmpty()) {
            wallpaperSkipReason = "custom wallpaper file is empty: $fileName"
            println("[Clean Start Page] Skipped: $wallpaperSkipReason")
            return@execute
        }

        if (content.size > MAX_WALLPAPER_BYTES) {
            wallpaperSkipReason = "custom wallpaper file exceeds ${MAX_WALLPAPER_BYTES / 1024 / 1024}MB limit: ${content.size} bytes"
            println("[Clean Start Page] Skipped: $wallpaperSkipReason")
            return@execute
        }

        val ext = detectImageExtension(content)
        if (ext == null) {
            wallpaperSkipReason = "unrecognized image format (expected JPEG, PNG, or WebP magic bytes): $fileName"
            println("[Clean Start Page] Skipped: $wallpaperSkipReason")
            return@execute
        }

        // Asset contract: Keep in sync with BraveExtension.java CANDIDATE_EXTENSIONS probe and dest construction
        val targetAsset = get("assets/morphe_ntp_wallpaper.$ext")
        targetAsset.parentFile?.mkdirs()
        targetAsset.writeBytes(content)

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

        val filterMethod = if (isWallpaperConfigured) "filterNtpPrefWithWallpaper" else "filterNtpPref"
        returnIndices.asReversed().forEach { (returnIndex, reg) ->
            prefMethod.addInstructions(
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

        var wallpaperReadSitesHooked = 0
        if (isWallpaperConfigured) {
            // 4. Hook SharedPreferences reads in NTP background manager class(es)
            val candidateMethods = mutableListOf<MutableMethod>()
            try {
                val pathFp = Fingerprint(strings = listOf(PREF_KEY_WALLPAPER_PATH))
                candidateMethods.addAll(
                    pathFp.matchAll()
                        .filter { classMentionsCompanionKeys(it.classDef) }
                        .map { it.method },
                )
            } catch (_: NoSuchElementException) {
            }

            try {
                val typeFp = Fingerprint(strings = listOf(PREF_KEY_WALLPAPER_TYPE))
                candidateMethods.addAll(
                    typeFp.matchAll()
                        .filter { classMentionsCompanionKeys(it.classDef) }
                        .map { it.method },
                )
            } catch (_: NoSuchElementException) {
            }

            for (candidate in candidateMethods.distinctBy { "${it.definingClass}->${it.name}" }) {
                wallpaperReadSitesHooked += hookWallpaperReadSites(candidate, PREF_KEY_WALLPAPER_PATH, true, hookedMethods)
                wallpaperReadSitesHooked += hookWallpaperReadSites(candidate, PREF_KEY_WALLPAPER_TYPE, false, hookedMethods)
            }

            if (candidateMethods.isEmpty() || wallpaperReadSitesHooked == 0) {
                error("Failed to resolve NTP background manager wallpaper anchors: no read sites hooked for custom wallpaper display path")
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
