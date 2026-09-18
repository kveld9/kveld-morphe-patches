package app.morphe.patches.vivaldi

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element
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
            println("[Block Telemetry] Skipped: libchrome.so not found.")
            return@execute
        }

        data class HostEntry(
            val offsets: List<Long>,
            val hostName: String,
        )

        val hostEntries = listOf(
            HostEntry(listOf(0x00a1b8a3L, 0x00a1a113L, 0x00a1b01bL, 0x00a1b41bL), "https://update.vivaldi.com/rep/rep"),
            HostEntry(listOf(0x000c6834L, 0x000c6824L), "https://crash.vivaldi.com/submit"),
            HostEntry(listOf(0x0007f80eL, 0x0007f7feL), "crashpad.chromium.org"),
            HostEntry(listOf(0x00435145L, 0x00435148L, 0x00435298L, 0x0043515cL, 0x004352b1L), "crashpad.chromium.org"),
            HostEntry(listOf(0x001a25b4L, 0x001a25c6L, 0x001a25a6L, 0x001a25a4L, 0x001a2634L), "https://downloads.vivaldi.com/directmatch/"),
            HostEntry(listOf(0x00929846L, 0x00929c56L), "https://location.vivaldi.com/lookup"),
            HostEntry(listOf(0x00a1a976L, 0x00a1ad76L), "https://downloads.vivaldi.com/lists/vivaldi/partners-current.txt"),
            HostEntry(listOf(0x001a24e2L, 0x001a2572L), "https://downloads.vivaldi.com/search/{}/search_engines.json"),
            HostEntry(listOf(0x001a2421L, 0x001a24b1L), "https://downloads.vivaldi.com/search/{}/search_engines_prompt.json"),
            HostEntry(listOf(0x004350b7L, 0x0043520cL), "https://vivaldi.com/bk/"),
        )

        val redirectionIp = "0.0.0.0".toByteArray(Charsets.US_ASCII)
        val redirectionUrl = "https://0.0.0.0".toByteArray(Charsets.US_ASCII)
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

                val replacementTarget = if (entry.hostName.startsWith("https://")) redirectionUrl else redirectionIp
                val replacement = ByteArray(len)
                System.arraycopy(replacementTarget, 0, replacement, 0, replacementTarget.size)
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
            println("[Block Vivaldi Sync] Skipped: libchrome.so not found.")
            return@execute
        }

        val syncOffsets = listOf(0x0031b2b2L, 0x0031b2b5L, 0x0031b34fL, 0x0031b2c1L, 0x0031b403L)
        val syncUrl = "https://bifrost.vivaldi.com/vivid-sync"
        val expectedBytes = syncUrl.toByteArray(Charsets.US_ASCII)
        val len = expectedBytes.size
        val redirectionUrl = "https://0.0.0.0".toByteArray(Charsets.US_ASCII)

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
            System.arraycopy(redirectionUrl, 0, replacement, 0, redirectionUrl.size)

            raf.seek(validOffset)
            raf.write(replacement)
        }

        println("[Block Vivaldi Sync] Redirected bifrost.vivaldi.com in libchrome.so")
    }
}

// ── Resource Patch: Disables Google DataTransport schedulers in AndroidManifest.xml ─────
private val vivaldiDataTransportResourcePatch = resourcePatch(
    name = "Vivaldi DataTransport Neutralizer",
    description = "Disables Google DataTransport background job and alarm schedulers in AndroidManifest.xml.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) return@execute

        val disabledComponents = setOf(
            "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
            "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
        )

        var disabledCount = 0
        document(manifestFile.absolutePath).use { doc ->
            val tags = listOf("service", "receiver")
            for (tag in tags) {
                val elements = doc.getElementsByTagName(tag)
                for (i in 0 until elements.length) {
                    val elem = elements.item(i) as? Element ?: continue
                    val name = elem.getAttribute("android:name")
                    if (name in disabledComponents) {
                        elem.setAttribute("android:enabled", "false")
                        disabledCount++
                    }
                }
            }
        }
        if (disabledCount > 0) {
            println("[Block Telemetry] Disabled $disabledCount DataTransport schedulers in AndroidManifest.xml")
        }
    }
}

// ── Bytecode Patch: Neutralizes Crash Dump Upload services ─────
@Suppress("unused")
val vivaldiBlockTelemetryPatch = bytecodePatch(
    name = "Block Vivaldi Telemetry",
    description = "Blocks Vivaldi usage statistics, crash reporting, Crashpad endpoints, DirectMatch suggestions, install referrer transmission, and background wallpaper polling.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    dependsOn(vivaldiHostsBlockerPatch, vivaldiDataTransportResourcePatch)

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

        // 3. Google Play Install Referrer: Neutralize feedback reporting
        try {
            Fingerprint(
                returnType = "V",
                parameters = listOf("I"),
                strings = listOf("https://feedback.vivaldi.com/store-referrer-data"),
            ).method.apply {
                addInstructions(0, "return-void")
                hookedMethods.add("InstallReferrerHelper.storeReferrerData")
            }
        } catch (e: Exception) {
            println("[Block Telemetry] Install Referrer hook note: ${e.message}")
        }

        // 4. Daily Wallpaper Background Downloader: Neutralize image-mobile.json fetch
        try {
            Fingerprint(
                returnType = "V",
                parameters = emptyList(),
                strings = listOf("https://downloads.vivaldi.com/background/image-mobile.json"),
            ).method.apply {
                addInstructions(0, "return-void")
                hookedMethods.add("DailyWallpaperDownloader.run")
            }
        } catch (e: Exception) {
            println("[Block Telemetry] Daily Wallpaper hook note: ${e.message}")
        }

        // 5. DirectMatch Java Layer: Return empty list and neutralize download callbacks
        try {
            Fingerprint(
                definingClass = "Lorg/vivaldi/browser/common/DirectMatchBridge;",
                name = "a",
                returnType = "Ljava/util/ArrayList;",
                parameters = emptyList(),
            ).method.apply {
                addInstructions(
                    0,
                    """
                        new-instance v0, Ljava/util/ArrayList;
                        invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                        return-object v0
                    """,
                )
                hookedMethods.add("DirectMatchBridge.a")
            }

            Fingerprint(
                definingClass = "Lorg/vivaldi/browser/common/DirectMatchBridge;",
                name = "directMatchIconsDownloadFinished",
                returnType = "V",
                parameters = emptyList(),
            ).method.apply {
                addInstructions(0, "return-void")
                hookedMethods.add("DirectMatchBridge.directMatchIconsDownloadFinished")
            }

            Fingerprint(
                definingClass = "Lorg/vivaldi/browser/common/DirectMatchBridge;",
                name = "directMatchUnitsDownloadFinished",
                returnType = "V",
                parameters = emptyList(),
            ).method.apply {
                addInstructions(0, "return-void")
                hookedMethods.add("DirectMatchBridge.directMatchUnitsDownloadFinished")
            }
        } catch (e: Exception) {
            println("[Block Telemetry] DirectMatch bridge hook note: ${e.message}")
        }

        // 6. Google Privacy Sandbox Attestations & Metrics Permitted By User: Return false
        try {
            Fingerprint(
                returnType = "Z",
                parameters = emptyList(),
                strings = listOf("Chrome.Privacy.UsageAndCrashReportingPermittedByUser"),
            ).method.apply {
                addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                hookedMethods.add("PrivacyPreferencesManagerImpl.isUsageAndCrashReportingPermitted")
            }
        } catch (e: Exception) {
            println("[Block Telemetry] Privacy preferences hook note: ${e.message}")
        }

        // 7. UKM (URL-Keyed Metrics): Neutralize event recording dispatch to native
        try {
            Fingerprint(
                definingClass = "Lorg/chromium/components/ukm/UkmRecorder;",
                name = "c",
                returnType = "V",
                parameters = emptyList(),
            ).method.apply {
                addInstructions(0, "return-void")
                hookedMethods.add("UkmRecorder.c")
            }
        } catch (e: Exception) {
            println("[Block Telemetry] UKM recorder hook note: ${e.message}")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Block Telemetry] Neutralized ${hookedMethods.size} telemetry & background services across ${targetClasses.size} classes")
    }
}
