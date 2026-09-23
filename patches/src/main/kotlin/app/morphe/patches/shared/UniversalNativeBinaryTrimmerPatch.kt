package app.morphe.patches.shared

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.rawResourcePatch
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

private val EMPTY_STUB_BYTES = byteArrayOf()

private fun collectCandidateFiles(libDir: File, targetLibs: Set<String>): List<File> {
    return libDir.walkTopDown()
        .filter { it.isFile && it.extension.equals("so", ignoreCase = true) }
        .filter { it.name.lowercase() in targetLibs }
        .toList()
}

private fun zeroBinaryFile(file: File): Long {
    val originalSize = file.length()
    if (originalSize <= 0) return 0L
    file.writeBytes(EMPTY_STUB_BYTES)
    return originalSize
}

@Suppress("unused")
val universalNativeBinaryTrimmerPatch = rawResourcePatch(
    name = "Universal Native Binary Trimmer",
    description = "Strips non-essential tracking, crash reporting, and debug companion native libraries in lib/** (e.g. libcrashlytics, libsentry, libbugly, libgwp-asan) by zeroing bytes in-situ.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val trimCrashReporters by booleanOption(
        key = "trimCrashReporters",
        default = true,
        title = "Trim Crash Reporting Libraries",
        description = "Zero native crash reporting and tracking libraries (Crashlytics, Sentry, Bugly, PLCrashReporter).",
        required = false,
    )

    val trimDebugProfilers by booleanOption(
        key = "trimDebugProfilers",
        default = true,
        title = "Trim Debug & Profiling Libraries",
        description = "Zero debug memory and profiling libraries (GWP-ASan, Simpleperf, Android Studio profilers).",
        required = false,
    )

    execute {
        val libDir = try {
            get("lib")
        } catch (_: Throwable) {
            null
        }

        if (libDir == null || !libDir.exists() || !libDir.isDirectory) {
            println("[Universal Native Binary Trimmer] lib/ directory not found - skipping safely.")
            return@execute
        }

        val targetLibs = mutableSetOf<String>()
        if (trimCrashReporters ?: true) targetLibs.addAll(CRASH_AND_TRACKING_LIBS)
        if (trimDebugProfilers ?: true) targetLibs.addAll(DEBUG_AND_PROFILER_LIBS)

        if (targetLibs.isEmpty()) {
            println("[Universal Native Binary Trimmer] All trimming options disabled - skipping.")
            return@execute
        }

        val candidateFiles = collectCandidateFiles(libDir, targetLibs)
        if (candidateFiles.isEmpty()) {
            println("[Universal Native Binary Trimmer] No candidate tracking or debug native libraries found in lib/ - skipping.")
            return@execute
        }

        var savedBytes = 0L
        val strippedLibs = mutableListOf<String>()
        val apkRoot = libDir.parentFile ?: libDir

        for (file in candidateFiles) {
            val freed = zeroBinaryFile(file)
            if (freed > 0) {
                savedBytes += freed
                strippedLibs.add(file.relativeTo(apkRoot).path.replace('\\', '/'))
            }
        }

        if (strippedLibs.isEmpty()) {
            println("[Universal Native Binary Trimmer] Target native libraries were already zeroed.")
            return@execute
        }

        val savedFormatted = LocaleUtils.formatBytes(savedBytes)
        println("[Universal Native Binary Trimmer] Trimmed ${strippedLibs.size} companion native libraries (${strippedLibs.joinToString(", ")}) -> Saved $savedFormatted")
    }
}
