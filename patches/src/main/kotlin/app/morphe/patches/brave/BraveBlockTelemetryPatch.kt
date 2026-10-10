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
    name = "Brave Telemetry Resource Defaults",
    description = "Sets default values of P3A, Stats, and WDP switches to false in XML preferences.",
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
    }
}

// ── Hosts Blocker Patch: Redirects all 12 telemetry domain endpoints to 0.0.0.0 in libchrome.so ─────
private val braveHostsBlockerPatch = rawResourcePatch(
    name = "Brave Hosts Blocker Layer",
    description = "Redirects telemetry and diagnostic domain strings to 0.0.0.0 in libchrome.so as a second layer of defense.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        data class HostEntry(
            val arm64Offset: Long,
            val arm32Offset: Long,
            val hostName: String,
        )

        val hostEntries = listOf(
            HostEntry(0x0020d657L, 0x005a14b4L, "star-randsrv.bsg.brave.com"),
            HostEntry(0x0020d688L, 0x005a14e5L, "collector.bsg.brave.com"),
            HostEntry(0x0020d6bfL, 0x005a151cL, "usage-ping.brave.com"),
            HostEntry(0x0020d4e2L, 0x005a133fL, "patterns.wdp.brave.com"),
            HostEntry(0x0020d4f9L, 0x005a1356L, "collector.wdp.brave.com"),
            HostEntry(0x0020d511L, 0x005a136eL, "star.wdp.brave.com"),
            HostEntry(0x0020d524L, 0x005a1381L, "quorum.wdp.brave.com"),
            HostEntry(0x0020d4d5L, 0x005a1332L, "cr.brave.com"),
            HostEntry(0x0008ca50L, 0x00426dfbL, "crashpad.chromium.org"),
            HostEntry(0x004d92bcL, 0x00861123L, "crashpad.chromium.org"),
            HostEntry(0x0020d369L, 0x005a11c6L, "variations.brave.com"),
            HostEntry(0x003532a6L, 0x006ddcb2L, "variations.brave.com"),
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

        val redirectionIp = "0.0.0.0".toByteArray(Charsets.US_ASCII)

        for ((relPath, soFile, offsetSelector) in existingTargets) {
            var writtenHosts = 0

            RandomAccessFile(soFile, "rw").use { raf ->
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

                    // Construct replacement: "0.0.0.0" + null byte + zero padding to original length
                    val replacement = ByteArray(len)
                    System.arraycopy(redirectionIp, 0, replacement, 0, redirectionIp.size)
                    // remaining bytes are 0x00 (null padded)

                    raf.seek(offset)
                    raf.write(replacement)
                    writtenHosts++
                }
            }

            println("[Block Telemetry] $relPath: Redirected $writtenHosts / ${hostEntries.size} endpoints to 0.0.0.0 in libchrome.so")
        }
    }
}

// ── Bytecode Patch: Blocks Crash uploads, Variations seed, and forces telemetry gates to false
@Suppress("unused")
val braveBlockTelemetryPatch = bytecodePatch(
    name = "Block Brave Telemetry",
    description = "Blocks P3A product analytics, Brave Stats usage pings, crash dump uploads, WDP, Chromium UMA metrics, and Variations seed fetching.",
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
        title = "Block Offers host",
        description = "Rewrites DEX const-string literals containing offers.brave.com to 0.0.0.0. Enabled by default; disable to keep commercial offers endpoint.",
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

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Block Telemetry] Hooked ${hookedMethods.size} bytecode telemetry methods across ${targetClasses.size} classes (${hookedMethods.joinToString(", ")})")

        // 5. Offers host: Rewrite const-string literals containing offers.brave.com to 0.0.0.0
        if (blockOffersHost == true) {
            blockOffersHostInDex()
        } else {
            println("[Block Telemetry] Offers host blocking disabled by option.")
        }
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


