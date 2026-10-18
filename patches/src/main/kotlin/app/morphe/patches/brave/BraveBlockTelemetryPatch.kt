package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.findXmlContaining
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import org.w3c.dom.Element
import java.io.RandomAccessFile

// ── Resource Patch: Sets defaultValue="false" on telemetry switches in XML ─────
private val braveTelemetryResourcePatch = resourcePatch(
    name = "Block Telemetry (Setup)",
    description = "Turns off analytics switches by default.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val telemetrySwitches = listOf(
            "privacy_preserving_analytics_switch",
            "statistics_reporting_switch",
            "web_discovery_project_switch",
        )

        val targetFiles = get("res").findXmlContaining(telemetrySwitches)

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
                    if (key in telemetrySwitches) {
                        when {
                            node.hasAttribute("android:defaultValue") -> {
                                node.setAttribute("android:defaultValue", "false")
                                modifiedAttrs++
                                fileTouched = true
                            }
                            node.hasAttribute("defaultValue") -> {
                                node.setAttribute("defaultValue", "false")
                                modifiedAttrs++
                                fileTouched = true
                            }
                        }
                    }
                }
            }
            if (fileTouched) modifiedFiles++
        }

        println("[Block Telemetry] Set $modifiedAttrs preference defaults to false across $modifiedFiles XML layout files")

        val manifestFile = get("AndroidManifest.xml")
        if (manifestFile.exists()) {
            var strippedNodes = 0
            document(manifestFile.absolutePath).use { doc ->
                val tagNames = listOf("uses-permission", "uses-permission-sdk-23", "property", "uses-library")
                val targetNames = setOf(
                    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
                    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
                    "android.adservices.AD_SERVICES_CONFIG",
                    "android.ext.adservices",
                )

                for (tagName in tagNames) {
                    val elements = doc.getElementsByTagName(tagName)
                    val toRemove = mutableListOf<Element>()
                    for (i in 0 until elements.length) {
                        val el = elements.item(i) as? Element ?: continue
                        val nameAttr = el.getAttribute("android:name").takeIf { it.isNotEmpty() }
                            ?: el.getAttribute("name")
                        if (nameAttr in targetNames) {
                            toRemove.add(el)
                        }
                    }
                    for (el in toRemove) {
                        el.parentNode?.removeChild(el)
                        strippedNodes++
                    }
                }
            }
            if (strippedNodes > 0) {
                println("[Block Telemetry] Stripped $strippedNodes Google Play referrer & AdServices entries from AndroidManifest.xml")
            }
        }
    }
}

// ── Hosts Blocker Patch: Redirects all 12 telemetry domain endpoints to 0.0.0.0 in libchrome.so ─────
private data class HostEntry(
    val arm64Offset: Long,
    val arm32Offset: Long,
    val hostName: String,
)

private fun assertNoDuplicateOffsets(
    relPath: String,
    hostEntries: List<HostEntry>,
    offsetSelector: (HostEntry) -> Long,
) {
    val seenOffsets = mutableSetOf<Long>()
    for (entry in hostEntries) {
        val offset = offsetSelector(entry)
        if (!seenOffsets.add(offset)) {
            throw PatchException("Duplicate host offset 0x${offset.toString(16)} in $relPath")
        }
    }
}

private fun validateHostEntries(
    raf: RandomAccessFile,
    relPath: String,
    hostEntries: List<HostEntry>,
    offsetSelector: (HostEntry) -> Long,
) {
    for (entry in hostEntries) {
        val offset = offsetSelector(entry)
        val expectedBytes = entry.hostName.toByteArray(Charsets.US_ASCII)
        val len = expectedBytes.size
        if (offset + len > raf.length()) {
            throw PatchException("Host offset 0x${offset.toString(16)} out of bounds in $relPath")
        }
        val buf = ByteArray(len)
        raf.seek(offset)
        raf.readFully(buf)
        if (!buf.contentEquals(expectedBytes)) {
            throw PatchException(
                "Host fingerprint mismatch at 0x${offset.toString(16)} in $relPath. " +
                    "Expected: ${entry.hostName}, Found: ${String(buf, Charsets.US_ASCII)}",
            )
        }
    }
}

private fun buildHostReplacement(redirectionIp: ByteArray, len: Int): ByteArray {
    val replacement = ByteArray(len)
    System.arraycopy(redirectionIp, 0, replacement, 0, redirectionIp.size)
    return replacement
}

private fun writeHostEntries(
    raf: RandomAccessFile,
    relPath: String,
    hostEntries: List<HostEntry>,
    offsetSelector: (HostEntry) -> Long,
    redirectionIp: ByteArray,
): Int {
    var writtenHosts = 0
    for (entry in hostEntries) {
        val offset = offsetSelector(entry)
        val len = entry.hostName.toByteArray(Charsets.US_ASCII).size
        val replacement = buildHostReplacement(redirectionIp, len)

        raf.seek(offset)
        raf.write(replacement)

        raf.seek(offset)
        val verifyBuf = ByteArray(len)
        raf.readFully(verifyBuf)
        if (!verifyBuf.contentEquals(replacement)) {
            throw PatchException("Write verification failed at 0x${offset.toString(16)} in $relPath")
        }

        writtenHosts++
    }
    return writtenHosts
}

private val braveHostsBlockerPatch = rawResourcePatch(
    name = "Block Tracking Servers",
    description = "Redirects tracking servers to 0.0.0.0.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val hostEntries = listOf(
            HostEntry(0x0020d80bL, 0x005a1668L, "star-randsrv.bsg.brave.com"),
            HostEntry(0x0020d83cL, 0x005a1699L, "collector.bsg.brave.com"),
            HostEntry(0x0020d873L, 0x005a16d0L, "usage-ping.brave.com"),
            HostEntry(0x0020d696L, 0x005a14f3L, "patterns.wdp.brave.com"),
            HostEntry(0x0020d6adL, 0x005a150aL, "collector.wdp.brave.com"),
            HostEntry(0x0020d6c5L, 0x005a1522L, "star.wdp.brave.com"),
            HostEntry(0x0020d6d8L, 0x005a1535L, "quorum.wdp.brave.com"),
            HostEntry(0x0020d689L, 0x005a14e6L, "cr.brave.com"),
            HostEntry(0x0008caa0L, 0x00426e4bL, "crashpad.chromium.org"),
            HostEntry(0x004d94daL, 0x00861341L, "crashpad.chromium.org"),
            HostEntry(0x0020d51dL, 0x005a137aL, "variations.brave.com"),
            HostEntry(0x003534cbL, 0x006dded7L, "variations.brave.com"),
        )

        val targets = listOf(
            "lib/arm64-v8a/libchrome.so" to { entry: HostEntry -> entry.arm64Offset },
            "lib/armeabi-v7a/libchrome.so" to { entry: HostEntry -> entry.arm32Offset },
        )

        val existingTargets = targets
            .map { (path, offsetSelector) -> Triple(path, get(path), offsetSelector) }
            .filter { (_, file, _) -> file.exists() && file.isFile }

        if (existingTargets.isEmpty()) {
            println("[Block Telemetry] Skipped: no arm64-v8a or armeabi-v7a libchrome.so found.")
            return@execute
        }

        // Phase 1: Validate all targets and entries before opening any file for writing
        for ((relPath, soFile, offsetSelector) in existingTargets) {
            assertNoDuplicateOffsets(relPath, hostEntries, offsetSelector)
            RandomAccessFile(soFile, "r").use { raf ->
                validateHostEntries(raf, relPath, hostEntries, offsetSelector)
            }
        }

        // Phase 2: Write replacement bytes and verify each entry in-situ
        val redirectionIp = "0.0.0.0".toByteArray(Charsets.US_ASCII)
        for ((relPath, soFile, offsetSelector) in existingTargets) {
            val writtenHosts = RandomAccessFile(soFile, "rw").use { raf ->
                writeHostEntries(raf, relPath, hostEntries, offsetSelector, redirectionIp)
            }

            println("[Block Telemetry] $relPath: Redirected $writtenHosts / ${hostEntries.size} endpoints to 0.0.0.0 in libchrome.so")
        }
    }
}

// ── Bytecode Patch: Blocks Crash uploads, Variations seed, and forces telemetry gates to false
internal const val BRAVE_UNSUPPORTED_VARIANT_MESSAGE =
    "Unsupported Brave APK variant or drifted target: required hook not found in this build. " +
        "Only Bravemonoarm64.apk / BraveMonoarm.apk v${Constants.BRAVE_TARGET_VERSION} from github.com/brave/brave-browser/releases " +
        "are supported; Bravearm64Universal.apk is rejected. See the repository README for details."

@Suppress("unused")
val braveBlockTelemetryPatch = bytecodePatch(
    name = "Block Telemetry",
    description = "Blocks usage analytics, crash uploads and experiment downloads.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)
    dependsOn(sharedExtensionPatch)

    dependsOn(
        braveTelemetryResourcePatch,
        braveHostsBlockerPatch,
        braveNativeExtractionPatch,
        braveBtiCompatibilityPatch,
    )

    val blockOffersHost by booleanOption(
        key = "blockOffersHost",
        default = true,
        title = "Block offers server",
        description = "Also blocks the shopping offers server. Turn off to keep it.",
        required = false,
    )

    // Note: Google Privacy Sandbox APIs (Topics, Protected Audience) and upstream UKM metric
    // reporting to Google servers are already stripped/disabled by Brave at the C++ engine level
    // (brave-core). Upstream UkmRecorder hooks and dat zeroing are omitted here as Brave routes
    // its telemetry through P3A, Stats, and WDP, which are fully neutralized below and in libchrome.so.
    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Crash Upload: Primary point
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/crash/MinidumpUploadServiceImpl;",
            name = "tryUploadCrashDumpWithLocalId",
            returnType = "V",
            parameters = listOf("Ljava/lang/String;"),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("MinidumpUploadServiceImpl.tryUploadCrashDumpWithLocalId")
        }

        // 2. Crash Upload: Defense in depth
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/crash/ChromeMinidumpUploadJobService;",
            name = "onStartJob",
            returnType = "Z",
            parameters = listOf("Landroid/app/job/JobParameters;"),
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            hookedMethods.add("ChromeMinidumpUploadJobService.onStartJob")
        }

        // 3. Variations: Abort HTTP connection before socket opens
        try {
            val variationsFp = Fingerprint(
                returnType = "Ljava/net/HttpURLConnection;",
                strings = listOf("https://variations.brave.com/seed"),
            )
            variationsFp.method.apply {
                addInstructions(
                    0,
                    """
                        new-instance v0, Ljava/io/IOException;
                        const-string v1, "Blocked by Morphe"
                        invoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V
                        throw v0
                    """,
                )
                val className = variationsFp.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
                hookedMethods.add("$className.$name")
            }
        } catch (_: PatchException) {
            throw PatchException(BRAVE_UNSUPPORTED_VARIANT_MESSAGE)
        }

        // 4. PrefService.e(String): Filter telemetry preferences (P3A, Stats, WDP) at return
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
                    invoke-static {p1, v$reg}, ${Constants.BRAVE_EXTENSION_CLASS}->filterTelemetryPref(Ljava/lang/String;Z)Z
                    move-result v$reg
                """.trimIndent(),
            )
        }
        if (returnIndices.isNotEmpty()) {
            hookedMethods.add("PrefService.e")
        }

        // 5. Offers host: Rewrite const-string literals containing offers.brave.com to 0.0.0.0
        if (blockOffersHost == true) {
            blockOffersHostInDex()
        } else {
            println("[Block Telemetry] Offers host blocking disabled by option.")
        }

        // 6. Play Install Referrer: Neutralize initReferrer() to prevent Play Store attribution binding
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/referrer/BraveReferrer;",
            name = "initReferrer",
            returnType = "V",
            parameters = emptyList(),
        ).method.apply {
            addInstructions(
                0,
                """
                    invoke-virtual {p0}, Lorg/chromium/chrome/browser/referrer/BraveReferrer;->d()V
                    return-void
                """.trimIndent(),
            )
            hookedMethods.add("BraveReferrer.initReferrer")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Block Telemetry] Hooked ${hookedMethods.size} bytecode telemetry methods across ${targetClasses.size} classes (${hookedMethods.joinToString(", ")})")
    }
}

private const val OFFERS_HOST = "offers.brave.com"
private const val SINK_HOST = "0.0.0.0"

private data class PendingOffersRewrite(
    val index: Int,
    val register: Int,
    val replacement: String,
)

private fun BytecodePatchContext.blockOffersHostInDex() {
    var rewrittenStrings = 0
    var touchedClasses = 0

    classDefForEach { classDef ->
        if (!hasOffersLiteral(classDef)) return@classDefForEach

        val mutableClass = mutableClassDefBy(classDef)
        var classModified = false

        for (method in mutableClass.methods) {
            val count = rewriteOffersInMethod(method)
            if (count > 0) {
                rewrittenStrings += count
                classModified = true
            }
        }

        if (classModified) touchedClasses++
    }

    logOffersResult(rewrittenStrings, touchedClasses)
}

private fun logOffersResult(rewrittenStrings: Int, touchedClasses: Int) {
    if (rewrittenStrings == 0) {
        println("[Block Telemetry] No offers.brave.com literals found.")
    } else {
        println("[Block Telemetry] Rewrote $rewrittenStrings offers.brave.com literal(s) across $touchedClasses class(es) -> 0.0.0.0.")
    }
}

private fun rewriteOffersInMethod(method: MutableMethod): Int {
    val rewrites = collectOffersRewrites(method)
    if (rewrites.isEmpty()) return 0
    applyOffersRewrites(method, rewrites)
    return rewrites.size
}

private fun hasOffersLiteral(classDef: ClassDef): Boolean =
    classDef.methods.any { methodHasOffersLiteral(it) }

private fun methodHasOffersLiteral(method: Method): Boolean {
    val instructions = method.instructionsOrNull ?: return false
    return instructions.any { isOffersConstString(it) }
}

private fun isConstStringOpcode(opcode: Opcode): Boolean =
    opcode == Opcode.CONST_STRING || opcode == Opcode.CONST_STRING_JUMBO

private fun extractOffersLiteral(instruction: Instruction): String? {
    if (!isConstStringOpcode(instruction.opcode)) return null
    val ref = (instruction as? ReferenceInstruction)?.reference as? StringReference ?: return null
    val original = ref.string
    return if (original.contains(OFFERS_HOST, ignoreCase = true)) original else null
}

private fun isOffersConstString(instruction: Instruction): Boolean =
    extractOffersLiteral(instruction) != null

private fun buildOffersRewrite(instruction: Instruction, index: Int): PendingOffersRewrite? {
    val original = extractOffersLiteral(instruction) ?: return null
    val replacement = original.replace(OFFERS_HOST, SINK_HOST, ignoreCase = true)
    val register = (instruction as? OneRegisterInstruction)?.registerA ?: return null
    return PendingOffersRewrite(index, register, replacement)
}

private fun collectOffersRewrites(method: MutableMethod): List<PendingOffersRewrite> {
    val instructions = method.instructionsOrNull?.toList() ?: return emptyList()
    val rewrites = mutableListOf<PendingOffersRewrite>()
    for ((index, instruction) in instructions.withIndex()) {
        val rewrite = buildOffersRewrite(instruction, index) ?: continue
        rewrites.add(rewrite)
    }
    return rewrites
}

private fun applyOffersRewrites(method: MutableMethod, rewrites: List<PendingOffersRewrite>) {
    for (rewrite in rewrites.sortedByDescending { it.index }) {
        val opcode = if (rewrite.register > 255) "const-string/jumbo" else "const-string"
        method.replaceInstruction(
            rewrite.index,
            "$opcode v${rewrite.register}, \"${escapeSmaliLiteral(rewrite.replacement)}\"",
        )
    }
}

private fun escapeSmaliLiteral(value: String): String =
    value.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")


