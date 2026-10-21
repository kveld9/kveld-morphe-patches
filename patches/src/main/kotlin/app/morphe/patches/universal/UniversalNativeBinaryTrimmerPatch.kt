package app.morphe.patches.universal

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.LocaleUtils
import java.io.File

private val CRASH_AND_TRACKING_LIBS = setOf(
    "libcrashlytics.so",
    "libcrashlytics-trampoline.so",
    "libcrashlytics_trampoline.so",
    "libsentry.so",
    "libsentry-android.so",
    "libsentry_android.so",
    "libbugly.so",
    "libfirebase-crashlytics.so",
    "libfirebase_crashlytics.so",
    "libapp-measurement.so",
    "libapp_measurement.so",
    "libplcrashreporter.so",
)

private val DEBUG_AND_PROFILER_LIBS = setOf(
    "libgwp-asan.so",
    "libgwp_asan.so",
    "libprofiler-service.so",
    "libprofiler_service.so",
    "libperfa.so",
    "libperfa_arm.so",
    "libperfa_arm64.so",
    "libsimpleperf.so",
    "libleaktracer.so",
)

private val BANDWIDTH_SHARING_LIBS = setOf("libhopmn.so")

private val EMPTY_STUB_BYTES = byteArrayOf()

private fun zeroBinaryFile(file: File): Long {
    val originalSize = file.length()
    if (originalSize <= 0) return 0L
    file.writeBytes(EMPTY_STUB_BYTES)
    return originalSize
}

@Suppress("unused")
val universalNativeBinaryTrimmerPatch = rawResourcePatch(
    name = "Remove Tracking Libraries",
    description = "Deletes tracking and crash-reporting native libraries.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val trimCrashReporters by booleanOption(
        key = "trimCrashReporters",
        default = true,
        title = "Crash reporters",
        description = "Removes native crash reporting libraries.",
        required = false,
    )

    val trimDebugProfilers by booleanOption(
        key = "trimDebugProfilers",
        default = true,
        title = "Debuggers",
        description = "Removes debug memory and profiling libraries.",
        required = false,
    )

    val trimBandwidthSharing by booleanOption(
        key = "trimBandwidthSharing",
        default = true,
        title = "Bandwidth-sharing SDKs",
        description = "Removes Honeygain bandwidth monetization native library.",
        required = false,
    )

    execute {
        val libEntries = listApkEntries("lib/")
            .filter { !it.endsWith('/') }

        if (libEntries.isEmpty()) {
            println("[Remove Tracking Libraries] lib/ directory not found - skipping safely.")
            return@execute
        }

        val targetLibs = mutableSetOf<String>()
        if (trimCrashReporters ?: true) targetLibs.addAll(CRASH_AND_TRACKING_LIBS)
        if (trimDebugProfilers ?: true) targetLibs.addAll(DEBUG_AND_PROFILER_LIBS)
        if (trimBandwidthSharing ?: true) targetLibs.addAll(BANDWIDTH_SHARING_LIBS)

        if (targetLibs.isEmpty()) {
            println("[Remove Tracking Libraries] All trimming options disabled - skipping.")
            return@execute
        }

        val candidateEntries = libEntries.filter { entry ->
            val name = entry.substringAfterLast('/')
            name.endsWith(".so", ignoreCase = true) && name.lowercase() in targetLibs
        }

        if (candidateEntries.isEmpty()) {
            println("[Remove Tracking Libraries] No candidate tracking or debug native libraries found in lib/ - skipping.")
            return@execute
        }

        var savedBytes = 0L
        val strippedLibs = mutableListOf<String>()

        for (entry in candidateEntries) {
            val file = get(entry)
            val freed = zeroBinaryFile(file)
            if (freed > 0) {
                savedBytes += freed
                strippedLibs.add(entry)
            }
        }

        if (strippedLibs.isEmpty()) {
            println("[Remove Tracking Libraries] Target native libraries were already zeroed.")
            return@execute
        }

        val savedFormatted = LocaleUtils.formatBytes(savedBytes)
        println("[Remove Tracking Libraries] Trimmed ${strippedLibs.size} companion native libraries (${strippedLibs.joinToString(", ")}) -> Saved $savedFormatted")
        println("[Remove Tracking Libraries] Warning: trimmed libs are only safe if nothing loads them eagerly; pair with Universal SDK Blocker (blockCrashReporters) and bandwidth protections (Block Trackers service kill + blockBandwidthSharing cascade) or verify no System.loadLibrary references, otherwise UnsatisfiedLinkError boot crashes follow.")
    }
}
