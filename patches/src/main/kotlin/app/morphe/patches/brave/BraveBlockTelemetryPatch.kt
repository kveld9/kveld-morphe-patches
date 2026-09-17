package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
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

        val targetFiles = get("res").walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .filter { file ->
                val content = file.readText()
                telemetrySwitches.any { key -> content.contains(key) }
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

// ── Hosts Blocker Patch: Redirects all 10 telemetry domain strings to 0.0.0.0 in libchrome.so ─────
private val braveHostsBlockerPatch = rawResourcePatch(
    name = "Brave Hosts Blocker Layer",
    description = "Redirects telemetry and diagnostic domain strings to 0.0.0.0 in libchrome.so as a second layer of defense.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val soFile = get("lib/arm64-v8a/libchrome.so")
        if (!soFile.exists()) {
            println("[BraveBlockTelemetry] Skipped: lib/arm64-v8a/libchrome.so not found.")
            return@execute
        }

        data class HostEntry(
            val offset: Long,
            val hostName: String,
        )

        val hostEntries = listOf(
            HostEntry(0x001f9329L, "star-randsrv.bsg.brave.com"),
            HostEntry(0x001f935aL, "collector.bsg.brave.com"),
            HostEntry(0x001f9391L, "usage-ping.brave.com"),
            HostEntry(0x001f91b4L, "patterns.wdp.brave.com"),
            HostEntry(0x001f91cbL, "collector.wdp.brave.com"),
            HostEntry(0x001f91e3L, "star.wdp.brave.com"),
            HostEntry(0x001f91f6L, "quorum.wdp.brave.com"),
            HostEntry(0x001f91a7L, "cr.brave.com"),
            HostEntry(0x0008970cL, "crashpad.chromium.org"),
            HostEntry(0x004a434bL, "crashpad.chromium.org"),
            HostEntry(0x001f903bL, "variations.brave.com"),
            HostEntry(0x00334a0cL, "variations.brave.com"),
        )

        val redirectionIp = "0.0.0.0".toByteArray(Charsets.US_ASCII)
        var writtenHosts = 0

        RandomAccessFile(soFile, "rw").use { raf ->
            for (entry in hostEntries) {
                val expectedBytes = entry.hostName.toByteArray(Charsets.US_ASCII)
                val len = expectedBytes.size
                if (entry.offset + len > raf.length()) {
                    throw PatchException("Host offset 0x${entry.offset.toString(16)} out of bounds in libchrome.so")
                }
                val buf = ByteArray(len)
                raf.seek(entry.offset)
                raf.readFully(buf)
                if (!buf.contentEquals(expectedBytes)) {
                    throw PatchException(
                        "Host fingerprint mismatch at 0x${entry.offset.toString(16)}. " +
                            "Expected: ${entry.hostName}, Found: ${String(buf, Charsets.US_ASCII)}",
                    )
                }

                // Construct replacement: "0.0.0.0" + null byte + zero padding to original length
                val replacement = ByteArray(len)
                System.arraycopy(redirectionIp, 0, replacement, 0, redirectionIp.size)
                // remaining bytes are 0x00 (null padded)

                raf.seek(entry.offset)
                raf.write(replacement)
                writtenHosts++
            }
        }

        println("[Block Telemetry] Redirected $writtenHosts / ${hostEntries.size} endpoints to 0.0.0.0 in libchrome.so")
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
    extendWith("extensions/extension.mpe")

    dependsOn(braveTelemetryResourcePatch, braveHostsBlockerPatch)

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
        } catch (e: Exception) {
            println("[Block Telemetry] Variations hook note: ${e.message}")
        }

        // 4. PrefService.e(String): Filter telemetry preferences (P3A, Stats, WDP) at return
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
                        invoke-static {p1, v$reg}, ${Constants.BRAVE_EXTENSION_CLASS}->filterTelemetryPref(Ljava/lang/String;Z)Z
                        move-result v$reg
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                hookedMethods.add("PrefService.e")
            }
        } catch (e: Exception) {
            println("[Block Telemetry] PrefService.e hook note: ${e.message}")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Block Telemetry] Hooked ${hookedMethods.size} bytecode telemetry methods across ${targetClasses.size} classes")
    }
}

