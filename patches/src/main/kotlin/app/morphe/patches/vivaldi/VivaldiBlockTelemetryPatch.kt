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
            val offsets: List<Long>,
            val hostName: String,
        )

        val hostEntries = listOf(
            HostEntry(listOf(0x00a1b8a3L, 0x00a1a113L), "https://update.vivaldi.com/rep/rep"),
            HostEntry(listOf(0x000c6834L, 0x000c6824L), "https://crash.vivaldi.com/submit"),
            HostEntry(listOf(0x0007f80eL, 0x0007f7feL), "crashpad.chromium.org"),
            HostEntry(listOf(0x00435145L, 0x00435148L, 0x00435298L), "crashpad.chromium.org"),
            HostEntry(listOf(0x001a25b4L, 0x001a25c6L, 0x001a25a6L), "https://downloads.vivaldi.com/directmatch/"),
        )

        val redirectionIp = "0.0.0.0".toByteArray(Charsets.US_ASCII)
        var writtenHosts = 0

        RandomAccessFile(soFile, "rw").use { raf ->
            for (entry in hostEntries) {
                val expectedBytes = entry.hostName.toByteArray(Charsets.US_ASCII)
                val len = expectedBytes.size
                val validOffset = entry.offsets.firstOrNull { offset ->
                    if (offset + len > raf.length()) return@firstOrNull false
                    val buf = ByteArray(len)
                    raf.seek(offset)
                    raf.readFully(buf)
                    buf.contentEquals(expectedBytes)
                } ?: throw PatchException(
                    "Host fingerprint mismatch for ${entry.hostName}. None of candidate offsets matched in libchrome.so",
                )

                val replacement = ByteArray(len)
                System.arraycopy(redirectionIp, 0, replacement, 0, redirectionIp.size)
                // remaining bytes are 0x00 (null padded)

                raf.seek(validOffset)
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

        val syncOffsets = listOf(0x0031b2b2L, 0x0031b2b5L, 0x0031b34fL)
        val syncUrl = "https://bifrost.vivaldi.com/vivid-sync"
        val expectedBytes = syncUrl.toByteArray(Charsets.US_ASCII)
        val len = expectedBytes.size
        val redirectionIp = "0.0.0.0".toByteArray(Charsets.US_ASCII)

        RandomAccessFile(soFile, "rw").use { raf ->
            val validOffset = syncOffsets.firstOrNull { offset ->
                if (offset + len > raf.length()) return@firstOrNull false
                val buf = ByteArray(len)
                raf.seek(offset)
                raf.readFully(buf)
                buf.contentEquals(expectedBytes)
            } ?: throw PatchException(
                "Sync fingerprint mismatch for $syncUrl in libchrome.so",
            )

            val replacement = ByteArray(len)
            System.arraycopy(redirectionIp, 0, replacement, 0, redirectionIp.size)

            raf.seek(validOffset)
            raf.write(replacement)
        }

        println("[Block Vivaldi Sync] Redirected bifrost.vivaldi.com in libchrome.so")
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
