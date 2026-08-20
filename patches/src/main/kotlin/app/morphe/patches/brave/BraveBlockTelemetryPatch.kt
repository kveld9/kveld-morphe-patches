package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
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

        for (file in targetFiles) {
            document(file.absolutePath).use { doc ->
                val elements = doc.getElementsByTagName("*")
                for (i in 0 until elements.length) {
                    val node = elements.item(i) as? Element ?: continue
                    val key = node.getAttribute("android:key").takeIf { it.isNotEmpty() }
                        ?: node.getAttribute("key")
                    if (key in telemetrySwitches) {
                        when {
                            node.hasAttribute("android:defaultValue") ->
                                node.setAttribute("android:defaultValue", "false")
                            node.hasAttribute("defaultValue") ->
                                node.setAttribute("defaultValue", "false")
                        }
                    }
                }
            }
        }
    }
}

// ── Native Patch: Neutralizes P3A, Stats, and WDP branches in libchrome.so (ARM64) ─────
private val braveNativeTelemetryPatch = rawResourcePatch(
    name = "Brave Native Telemetry Patch",
    description = "Patches ARM64 branches in libchrome.so to abort P3A, Stats, and WDP network dispatch.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val soFile = get("lib/arm64-v8a/libchrome.so")
        if (!soFile.exists()) {
            return@execute
        }

        data class NativeGate(
            val name: String,
            val offset: Long,
            val expectedOriginal: ByteArray,
            val replacement: ByteArray,
        )

        val gates = listOf(
            // Gate 1: P3A (P3AService::InitScheduler) -> force branch to abort path 0x0ab8aaac
            NativeGate(
                name = "P3A Native Gate",
                offset = 0x0ab8abe0L,
                expectedOriginal = byteArrayOf(0x60.toByte(), 0xf6.toByte(), 0x07.toByte(), 0x36.toByte()),
                replacement = byteArrayOf(0xb3.toByte(), 0xff.toByte(), 0xff.toByte(), 0x17.toByte()),
            ),
            // Gate 2: Brave Stats (BraveStatsUpdater::Start) -> nop the start upload branch
            NativeGate(
                name = "Brave Stats Native Gate",
                offset = 0x0c3aa0e8L,
                expectedOriginal = byteArrayOf(0xc0.toByte(), 0x0b.toByte(), 0x00.toByte(), 0x37.toByte()),
                replacement = byteArrayOf(0x1f.toByte(), 0x20.toByte(), 0x03.toByte(), 0xd5.toByte()),
            ),
            // Gate 3: WDP (BraveSearchDefaultHostExtractor) -> force branch to skip path 0x0c3e9394
            NativeGate(
                name = "WDP Native Gate",
                offset = 0x0c3e9388L,
                expectedOriginal = byteArrayOf(0x60.toByte(), 0x00.toByte(), 0x00.toByte(), 0x36.toByte()),
                replacement = byteArrayOf(0x03.toByte(), 0x00.toByte(), 0x00.toByte(), 0x14.toByte()),
            ),
        )

        RandomAccessFile(soFile, "rw").use { raf ->
            val buf = ByteArray(4)
            for (gate in gates) {
                if (gate.offset + 4 > raf.length()) {
                    throw PatchException("Offset 0x${gate.offset.toString(16)} for ${gate.name} out of bounds in libchrome.so")
                }
                raf.seek(gate.offset)
                raf.readFully(buf)
                if (!buf.contentEquals(gate.expectedOriginal)) {
                    throw PatchException(
                        "Fingerprint mismatch for ${gate.name} at 0x${gate.offset.toString(16)}. " +
                            "Expected: ${gate.expectedOriginal.joinToString(" ") { "%02x".format(it) }}, " +
                            "Found: ${buf.joinToString(" ") { "%02x".format(it) }}",
                    )
                }
                raf.seek(gate.offset)
                raf.write(gate.replacement)
            }
        }
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
            return@execute
        }

        data class HostEntry(
            val offset: Long,
            val hostName: String,
        )

        val hostEntries = listOf(
            HostEntry(0x001e209bL, "star-randsrv.bsg.brave.com"),
            HostEntry(0x001e20ccL, "collector.bsg.brave.com"),
            HostEntry(0x001e2103L, "usage-ping.brave.com"),
            HostEntry(0x001e1f26L, "patterns.wdp.brave.com"),
            HostEntry(0x001e1f3dL, "collector.wdp.brave.com"),
            HostEntry(0x001e1f55L, "star.wdp.brave.com"),
            HostEntry(0x001e1f68L, "quorum.wdp.brave.com"),
            HostEntry(0x001e1f19L, "cr.brave.com"),
            HostEntry(0x00086432L, "crashpad.chromium.org"),
            HostEntry(0x004816d1L, "crashpad.chromium.org"),
            HostEntry(0x001e1dadL, "variations.brave.com"),
            HostEntry(0x0030e4dfL, "variations.brave.com"),
        )

        val redirectionIp = "0.0.0.0".toByteArray(Charsets.US_ASCII)

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
            }
        }
    }
}

// ── Bytecode Patch: Blocks Crash uploads, Variations seed, and forces telemetry gates to false
@Suppress("unused")
val braveBlockTelemetryPatch = bytecodePatch(
    name = "Block Brave Telemetry",
    description = "Blocks P3A product analytics, Brave Stats usage pings, crash dump uploads, WDP, and Variations seed fetching.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    dependsOn(braveTelemetryResourcePatch, braveNativeTelemetryPatch, braveHostsBlockerPatch)

    execute {
        // 1. Crash Upload: Primary point
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/crash/MinidumpUploadServiceImpl;",
            name = "tryUploadCrashDumpWithLocalId",
            returnType = "V",
            parameters = listOf("Ljava/lang/String;"),
        ).method.addInstructions(
            0,
            """
                return-void
            """,
        )

        // 2. Crash Upload: Defense in depth
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/crash/ChromeMinidumpUploadJobService;",
            name = "onStartJob",
            returnType = "Z",
            parameters = listOf("Landroid/app/job/JobParameters;"),
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        // 3. Variations: Abort HTTP connection before socket opens
        Fingerprint(
            returnType = "Ljava/net/HttpURLConnection;",
            strings = listOf("https://variations.brave.com/seed"),
        ).method.addInstructions(
            0,
            """
                new-instance v0, Ljava/io/IOException;
                const-string v1, "Blocked by Morphe"
                invoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V
                throw v0
            """,
        )

        // 4. PrefService.e(String): Strict conditional check for P3A, Stats, and WDP
        Fingerprint(
            definingClass = "Lorg/chromium/components/prefs/PrefService;",
            name = "e",
            returnType = "Z",
            parameters = listOf("Ljava/lang/String;"),
        ).method.addInstructions(
            0,
            """
                const-string v0, "brave.p3a.enabled"
                invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :not_p3a
                const/4 v0, 0x0
                return v0
                :not_p3a

                const-string v0, "brave.stats.reporting_enabled"
                invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :not_stats
                const/4 v0, 0x0
                return v0
                :not_stats

                const-string v0, "brave.web_discovery_enabled"
                invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :not_wdp
                const/4 v0, 0x0
                return v0
                :not_wdp
            """,
        )
    }
}
