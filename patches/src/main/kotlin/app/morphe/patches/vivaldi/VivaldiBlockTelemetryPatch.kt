package app.morphe.patches.vivaldi

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants
import java.io.RandomAccessFile

// ── Native Hosts Blocker: Redirects telemetry & crash endpoints in libchrome.so ─────
private val vivaldiHostsBlockerPatch = rawResourcePatch(
    name = "Vivaldi Hosts Blocker Layer",
    description = "Redirects Vivaldi telemetry, Crashpad, and DirectMatch endpoints to 0.0.0.0 in libchrome.so.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

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
            HostEntry(0x00a19753L, "https://update.vivaldi.com/rep/rep"),
            HostEntry(0x000c677eL, "https://crash.vivaldi.com/submit"),
            HostEntry(0x0007f78eL, "crashpad.chromium.org"),
            HostEntry(0x00434ef4L, "crashpad.chromium.org"),
            HostEntry(0x001a242aL, "https://downloads.vivaldi.com/directmatch/"),
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

                val replacement = ByteArray(len)
                System.arraycopy(redirectionIp, 0, replacement, 0, redirectionIp.size)
                // remaining bytes are 0x00 (null padded)

                raf.seek(entry.offset)
                raf.write(replacement)
                writtenHosts++
            }
        }

        println("[Block Telemetry] Redirected $writtenHosts / ${hostEntries.size} telemetry & crash endpoints to 0.0.0.0 in libchrome.so")
    }
}

// ── Opt-in Sync Blocker: Redirects bifrost.vivaldi.com for air-gapped privacy ─────
@Suppress("unused")
val vivaldiBlockSyncPatch = rawResourcePatch(
    name = "Block Vivaldi Sync",
    description = "Redirects bifrost.vivaldi.com to 0.0.0.0 in libchrome.so to completely disable Vivaldi account synchronization for an air-gapped experience.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        val soFile = get("lib/arm64-v8a/libchrome.so")
        if (!soFile.exists()) {
            return@execute
        }

        val syncOffset = 0x0031aff2L
        val syncUrl = "https://bifrost.vivaldi.com/vivid-sync"
        val expectedBytes = syncUrl.toByteArray(Charsets.US_ASCII)
        val len = expectedBytes.size
        val redirectionIp = "0.0.0.0".toByteArray(Charsets.US_ASCII)

        RandomAccessFile(soFile, "rw").use { raf ->
            if (syncOffset + len > raf.length()) {
                throw PatchException("Sync offset 0x${syncOffset.toString(16)} out of bounds in libchrome.so")
            }
            val buf = ByteArray(len)
            raf.seek(syncOffset)
            raf.readFully(buf)
            if (!buf.contentEquals(expectedBytes)) {
                throw PatchException(
                    "Sync fingerprint mismatch at 0x${syncOffset.toString(16)}. " +
                        "Expected: $syncUrl, Found: ${String(buf, Charsets.US_ASCII)}",
                )
            }

            val replacement = ByteArray(len)
            System.arraycopy(redirectionIp, 0, replacement, 0, redirectionIp.size)

            raf.seek(syncOffset)
            raf.write(replacement)
        }

        println("[Block Vivaldi Sync] Redirected bifrost.vivaldi.com at offset 0x${syncOffset.toString(16)} in libchrome.so")
    }
}

// ── Bytecode Patch: Neutralizes Crash Dump Upload services ─────
@Suppress("unused")
val vivaldiBlockTelemetryPatch = bytecodePatch(
    name = "Block Vivaldi Telemetry",
    description = "Blocks Vivaldi usage statistics, crash reporting, Crashpad endpoints, and DirectMatch suggestions.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    dependsOn(vivaldiHostsBlockerPatch)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Minidump Upload Service: Primary point
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/crash/MinidumpUploadServiceImpl;",
            name = "tryUploadCrashDumpWithLocalId",
            returnType = "V",
            parameters = listOf("Ljava/lang/String;"),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("MinidumpUploadServiceImpl.tryUploadCrashDumpWithLocalId")
        }

        // 2. Crash Upload Job Service: Defense in depth
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/crash/ChromeMinidumpUploadJobService;",
            name = "onStartJob",
            returnType = "Z",
            parameters = listOf("Landroid/app/job/JobParameters;"),
        ).method.apply {
            addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            hookedMethods.add("ChromeMinidumpUploadJobService.onStartJob")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Block Telemetry] Neutralized ${hookedMethods.size} minidump crash upload services across ${targetClasses.size} classes")
    }
}
