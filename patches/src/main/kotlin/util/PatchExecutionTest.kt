package util

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.dex.BytecodeMode
import app.morphe.patcher.dex.NoOpDexVerifier
import app.morphe.patcher.patch.loadPatchesFromJar
import app.morphe.patcher.resource.CpuArchitecture
import app.morphe.patches.shared.Constants
import app.morphe.patcher.apk.ApkUtils
import app.morphe.patcher.apk.ApkUtils.applyTo
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.io.PrintStream

enum class TargetApp(
    val id: String,
    val appName: String,
    val packageName: String,
    val candidateFilenames: List<String>,
    val filePattern: Regex,
    val patchDirectoryPart: String,
) {
    TIKTOK(
        id = "tiktok",
        appName = "TikTok Global",
        packageName = Constants.TIKTOK_GLOBAL_PACKAGE_NAME,
        candidateFilenames = listOf(
            "tiktok_${Constants.TIKTOK_TARGET_VERSION}_orig.apk",
            "tiktok_global_${Constants.TIKTOK_TARGET_VERSION}.apk",
            "tiktok_${Constants.TIKTOK_TARGET_VERSION}.apk",
            "tiktok_orig.apk",
            "tiktok_global.apk",
            "tiktok.apk",
        ),
        filePattern = Regex("(?i).*tiktok.*\\.apk$"),
        patchDirectoryPart = "tiktok",
    ),
    TIKTOK_ASIA(
        id = "tiktok_asia",
        appName = "TikTok Asia",
        packageName = Constants.TIKTOK_ASIA_PACKAGE_NAME,
        candidateFilenames = listOf(
            "com.ss.android.ugc.trill_${Constants.TIKTOK_TARGET_VERSION}.apk",
            "trill_${Constants.TIKTOK_TARGET_VERSION}_orig.apk",
            "trill_${Constants.TIKTOK_TARGET_VERSION}.apk",
            "trill.apk",
        ),
        filePattern = Regex("(?i).*trill.*\\.apk$"),
        patchDirectoryPart = "tiktok",
    ),
    GBOARD(
        id = "gboard",
        appName = "Gboard Lite",
        packageName = Constants.GBOARD_PACKAGE_NAME,
        candidateFilenames = listOf(
            "gboard-${Constants.GBOARD_TARGET_VERSION}.apk",
            "gboard-18.2.4.969776716-lite_beta-arm64-v8a.apk",
            "gboard-18.2.4.969776716-lite_beta-armeabi-v7a.apk",
        ),
        filePattern = Regex("(?i).*gboard.*\\.apk$"),
        patchDirectoryPart = "gboard",
    ),
    BRAVE(
        id = "brave",
        appName = "Brave Browser",
        packageName = Constants.BRAVE_PACKAGE_NAME,
        candidateFilenames = listOf(
            "BraveMonoarm64_${Constants.BRAVE_TARGET_VERSION}_orig.apk",
            "BraveMonoarm_${Constants.BRAVE_TARGET_VERSION}_orig.apk",
            "Bravemonoarm64_v${Constants.BRAVE_TARGET_VERSION}.apk",
            "BraveMonoarm64.apk",
            "BraveMonoarm.apk",
        ),
        filePattern = Regex("(?i).*brave.*\\.apk$"),
        patchDirectoryPart = "brave",
    ),
    VIVALDI(
        id = "vivaldi",
        appName = "Vivaldi Browser",
        packageName = Constants.VIVALDI_PACKAGE_NAME,
        candidateFilenames = listOf(
            "Vivaldi.${Constants.VIVALDI_TARGET_VERSION}_arm64-v8a.apkm",
            "com.vivaldi.browser_${Constants.VIVALDI_TARGET_VERSION}-541470077_1feat_0705103ed141e76c0c95ecc38009481f_apkmirror.com.apkm",
            "Vivaldi.${Constants.VIVALDI_TARGET_VERSION}_arm64-v8a.apk",
            "Vivaldi.8.2.4147.58_arm64-v8a.apk",
        ),
        filePattern = Regex("(?i).*vivaldi.*\\.(?:apk|apkm)$"),
        patchDirectoryPart = "vivaldi",
    ),
    HEVY(
        id = "hevy",
        appName = "Hevy",
        packageName = Constants.HEVY_PACKAGE_NAME,
        candidateFilenames = listOf(
            "com.hevy_${Constants.HEVY_TARGET_VERSION}.apk",
            "com.hevy_${Constants.HEVY_TARGET_VERSION}.apkm",
        ),
        filePattern = Regex("(?i).*hevy.*\\.(?:apk|apkm)$"),
        patchDirectoryPart = "hevy",
    ),
    NOKOPRINT(
        id = "nokoprint",
        appName = "NokoPrint",
        packageName = Constants.NOKOPRINT_PACKAGE_NAME,
        candidateFilenames = listOf(
            "com.nokoprint_${Constants.NOKOPRINT_TARGET_VERSION}-318_minAPI21(nodpi)_apkmirror.com.apk",
            "com.nokoprint_${Constants.NOKOPRINT_TARGET_VERSION}.apk",
            "nokoprint_${Constants.NOKOPRINT_TARGET_VERSION}.apk",
            "nokoprint.apk",
        ),
        filePattern = Regex("(?i).*nokoprint.*\\.apk$"),
        patchDirectoryPart = "nokoprint",
    ),
    XIAOMI_EARBUDS(
        id = "xiaomi_earbuds",
        appName = "Xiaomi Earbuds",
        packageName = Constants.XIAOMI_EARBUDS_PACKAGE_NAME,
        candidateFilenames = listOf(
            "Xiaomi+Earbuds_${Constants.XIAOMI_EARBUDS_TARGET_VERSION}_APKPure.xapk",
            "Xiaomi+Earbuds_${Constants.XIAOMI_EARBUDS_TARGET_VERSION}.xapk",
            "com.mi.earphone_${Constants.XIAOMI_EARBUDS_TARGET_VERSION}.xapk",
            "com.mi.earphone_${Constants.XIAOMI_EARBUDS_TARGET_VERSION}.apkm",
            "com.mi.earphone_${Constants.XIAOMI_EARBUDS_TARGET_VERSION}.apk",
            "xiaomi_earbuds.xapk",
            "xiaomi_earbuds.apkm",
            "xiaomi_earbuds.apk",
            "earphone.xapk",
            "earphone.apkm",
            "earphone.apk",
        ),
        filePattern = Regex("(?i).*(?:earphone|earbuds).*\\.(?:apk|apkm|xapk)$"),
        patchDirectoryPart = "xiaomi",
    );

    companion object {
        fun fromId(raw: String): TargetApp? {
            val normalized = raw.trim().lowercase().replace("-", "_")
            return entries.firstOrNull { it.id == normalized }
                ?: when (normalized) {
                    "trill" -> TIKTOK_ASIA
                    "gboard_lite" -> GBOARD
                    "earbuds", "earphone", "xiaomi" -> XIAOMI_EARBUDS
                    else -> null
                }
        }

        fun fromFileName(fileName: String): TargetApp? {
            val lower = fileName.lowercase()
            return when {
                lower.contains("trill") -> TIKTOK_ASIA
                lower.contains("tiktok") -> TIKTOK
                lower.contains("gboard") -> GBOARD
                lower.contains("brave") -> BRAVE
                lower.contains("vivaldi") -> VIVALDI
                lower.contains("hevy") -> HEVY
                lower.contains("nokoprint") -> NOKOPRINT
                lower.contains("earphone") || lower.contains("earbuds") -> XIAOMI_EARBUDS
                else -> entries.firstOrNull { it.filePattern.containsMatchIn(fileName) }
            }
        }
    }
}

private fun getDownloadDirectory(): File? {
    return try {
        val process = ProcessBuilder("xdg-user-dir", "DOWNLOAD").start()
        val dir = process.inputStream.bufferedReader().readText().trim()
        if (dir.isNotEmpty() && File(dir).isDirectory) File(dir) else null
    } catch (_: Exception) {
        null
    }
}

private fun getSearchDirectories(userHome: String): List<File> {
    val dirs = mutableListOf<File>()
    dirs.add(File("candidate_apks"))
    dirs.add(File("../candidate_apks"))
    dirs.add(File("."))
    dirs.add(File(".."))
    getDownloadDirectory()?.let { dirs.add(it) }
    dirs.add(File(userHome, "Downloads"))
    dirs.add(File(userHome, "Descargas"))
    dirs.add(File(userHome, "candidate_apks"))
    return dirs.distinctBy { it.absolutePath }.filter { it.isDirectory }
}

private fun findApkForTarget(target: TargetApp, searchDirs: List<File>): File? {
    for (dir in searchDirs) {
        for (candidateName in target.candidateFilenames) {
            val f = File(dir, candidateName)
            if (f.exists() && f.isFile) return f
        }
    }
    for (dir in searchDirs) {
        val matched = dir.listFiles { f -> f.isFile && target.filePattern.containsMatchIn(f.name) }
            ?.maxByOrNull { it.lastModified() }
        if (matched != null) return matched
    }
    return null
}

private fun detectTargetFromGit(): TargetApp? {
    return try {
        val process = ProcessBuilder("git", "status", "--porcelain").start()
        val lines = process.inputStream.bufferedReader().readLines()
        val modifiedPatches = lines.mapNotNull { line ->
            val path = line.substring(3).trim()
            if (path.contains("patches/src/main/kotlin/app/morphe/patches/")) path else null
        }
        for (target in TargetApp.entries) {
            if (modifiedPatches.any { it.contains("/${target.patchDirectoryPart}/") }) {
                return target
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

fun main(args: Array<String>) {
    val userHome = System.getProperty("user.home") ?: "."
    val searchDirs = getSearchDirectories(userHome)

    val explicitApkArg = args.firstOrNull { it.endsWith(".apk") || it.endsWith(".apkm") || File(it).isFile }
        ?: System.getenv("TARGET_APK_PATH")
        ?: System.getProperty("targetApk")
        ?: System.getProperty("apk")

    val explicitTargetArg = args.firstOrNull { TargetApp.fromId(it) != null }
        ?: System.getenv("TARGET_APP")
        ?: System.getProperty("targetApp")
        ?: System.getProperty("app")

    val explicitApkFile = explicitApkArg?.let { raw ->
        val direct = File(raw)
        if (direct.isFile) direct
        else {
            val fromParent = File("..", raw)
            if (fromParent.isFile) fromParent else direct
        }
    }

    val apkFile: File
    val targetApp: TargetApp

    if (explicitApkFile != null && explicitApkFile.isFile) {
        apkFile = explicitApkFile
        targetApp = explicitTargetArg?.let { TargetApp.fromId(it) }
            ?: TargetApp.fromFileName(apkFile.name)
            ?: error("Could not infer target app for APK: ${apkFile.name}. Specify app via -Papp=<target> or args.")
    } else {
        targetApp = explicitTargetArg?.let { TargetApp.fromId(it) }
            ?: detectTargetFromGit()
            ?: TargetApp.entries.firstOrNull { findApkForTarget(it, searchDirs) != null }
            ?: TargetApp.TIKTOK

        apkFile = findApkForTarget(targetApp, searchDirs)
            ?: error("Target APK not found for ${targetApp.appName} in search dirs: ${searchDirs.joinToString { it.absolutePath }}. Set TARGET_APK_PATH or -Papk=...")
    }

    require(apkFile.exists()) { "Target APK not found at: ${apkFile.absolutePath}" }

    val patchFiles = setOf(
        File("build/libs/").listFiles { file ->
            val fileName = file.name
            !fileName.contains("javadoc") &&
                    !fileName.contains("sources") &&
                    fileName.endsWith(".mpp")
        }!!.maxByOrNull { it.lastModified() }!!
    )
    val allPatches = loadPatchesFromJar(patchFiles)

    val patchNameFilter = System.getProperty("patchName")?.trim()
    val targetPatches = allPatches.filter { patch ->
        val cp = patch.compatibility
        val matchesApp = if (cp == null) {
            !patchNameFilter.isNullOrEmpty() && patch.name.equals(patchNameFilter, ignoreCase = true)
        } else {
            cp.any { it.packageName == targetApp.packageName }
        }
        val matchesName = patchNameFilter.isNullOrEmpty() || patch.name.equals(patchNameFilter, ignoreCase = true)
        matchesApp && matchesName
    }.toSet()

    require(targetPatches.isNotEmpty()) {
        "No patches found in ${patchFiles.first().name} for target ${targetApp.appName} (${targetApp.packageName})" +
            if (!patchNameFilter.isNullOrEmpty()) " matching '$patchNameFilter'" else ""
    }

    println("Loaded ${targetPatches.size} patch(es) for ${targetApp.appName} from ${patchFiles.first().name}:")
    targetPatches.sortedBy { it.name }.forEach { println("  • ${it.name}") }

    val tempDir = File("build/tmp/patcher-test-workspace").absoluteFile
    tempDir.deleteRecursively()
    tempDir.mkdirs()

    val effectiveApkFile = if (apkFile.name.endsWith(".apk", ignoreCase = true)) {
        val companionApkm = File("${apkFile.absolutePath}m")
            .takeIf { it.exists() && it.isFile }
            ?: File(apkFile.parentFile, "${apkFile.nameWithoutExtension}.apkm")
                .takeIf { it.exists() && it.isFile }
        if (companionApkm != null) {
            val isPartialSplit = try {
                java.util.zip.ZipFile(apkFile).use { zip ->
                    zip.getEntry("classes2.dex") == null && zip.getEntry("lib/arm64-v8a/libchrome.so") != null
                }
            } catch (_: Exception) {
                false
            }
            if (isPartialSplit) {
                println("[APKM] Detected partial base APK without split DEX. Redirecting to bundle: ${companionApkm.name}")
                companionApkm
            } else {
                apkFile
            }
        } else {
            apkFile
        }
    } else {
        apkFile
    }

    val actualApkFile = if (effectiveApkFile.name.endsWith(".apkm", ignoreCase = true) || effectiveApkFile.name.endsWith(".xapk", ignoreCase = true)) {
        val apkmExtractDir = File("build/tmp/patcher-apkm-source").absoluteFile
        apkmExtractDir.deleteRecursively()
        apkmExtractDir.mkdirs()
        val extractedBase = File(apkmExtractDir, "base.apk")
        java.util.zip.ZipFile(effectiveApkFile).use { apkmZip ->
            val baseEntry = apkmZip.getEntry("base.apk")
                ?: apkmZip.entries().asSequence().firstOrNull { it.name.endsWith(".apk") && (it.name.contains("base") || it.name.startsWith(targetApp.packageName)) }
                ?: apkmZip.entries().asSequence().filter { it.name.endsWith(".apk") }.maxByOrNull { it.size }
                ?: error("No base APK found in bundle: ${effectiveApkFile.name}")
            val splitApkEntries = apkmZip.entries().asSequence()
                .filter { it.name.endsWith(".apk", ignoreCase = true) && it.name != baseEntry.name }
                .sortedBy { it.name }
                .toList()

            val splitDexes = mutableListOf<ByteArray>()
            for (splitEntry in splitApkEntries) {
                apkmZip.getInputStream(splitEntry).use { splitStream ->
                    java.util.zip.ZipInputStream(splitStream).use { splitZip ->
                        val currentSplitDexes = mutableListOf<Pair<String, ByteArray>>()
                        var entry = splitZip.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory && Regex("^classes\\d*\\.dex$").matches(entry.name)) {
                                currentSplitDexes.add(entry.name to splitZip.readBytes())
                            }
                            entry = splitZip.nextEntry
                        }
                        currentSplitDexes.sortBy { it.first }
                        splitDexes.addAll(currentSplitDexes.map { it.second })
                    }
                }
            }

            apkmZip.getInputStream(baseEntry).use { input ->
                extractedBase.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }

            if (splitDexes.isNotEmpty()) {
                var maxDexIndex = 1
                java.util.zip.ZipFile(extractedBase).use { baseZip ->
                    baseZip.entries().asSequence().forEach { entry ->
                        val m = Regex("^classes(\\d*)\\.dex$").matchEntire(entry.name)
                        if (m != null) {
                            val num = m.groupValues[1]
                            val idx = if (num.isEmpty()) 1 else num.toInt()
                            if (idx > maxDexIndex) maxDexIndex = idx
                        }
                    }
                }

                val uri = java.net.URI.create("jar:" + extractedBase.toURI())
                val env = mapOf("create" to "false")
                java.nio.file.FileSystems.newFileSystem(uri, env).use { fs ->
                    for (dexBytes in splitDexes) {
                        maxDexIndex++
                        val entryPath = fs.getPath("classes$maxDexIndex.dex")
                        java.nio.file.Files.write(entryPath, dexBytes)
                    }
                }
                println("[APKM] Merged ${splitDexes.size} DEX file(s) from split APK(s) into base.apk (total DEX files: $maxDexIndex)")
            }
        }
        extractedBase
    } else {
        effectiveApkFile
    }

    val config = PatcherConfig(
        apkFile = actualApkFile,
        temporaryFilesPath = tempDir,
        aaptBinaryPath = null,
        frameworkFileDirectory = null,
        useArsclib = false,
        keepArchitectures = setOf(CpuArchitecture.ARM64_V8A, CpuArchitecture.ARMEABI_V7A),
        useBytecodeMode = BytecodeMode.STRIP_FAST,
        verifier = NoOpDexVerifier
    )

    println("\n[INIT] Initializing Morphe Patcher engine...")
    val patcher = Patcher(config)
    patcher += targetPatches

    println("[EXEC] Executing patch pipeline on ${effectiveApkFile.name} (target: ${targetApp.appName})...")
    var totalPatches = 0
    var successfulPatches = 0
    var failedPatches = 0
    val failures = mutableListOf<String>()

    val originalOut = System.out
    val originalErr = System.err
    val fingerprintErrors = java.util.concurrent.CopyOnWriteArrayList<String>()

    class InterceptingOutputStream(val delegate: OutputStream) : OutputStream() {
        private val buffer = ByteArrayOutputStream()

        override fun write(b: Int) {
            delegate.write(b)
            if (b == '\n'.code) {
                checkLine(buffer.toString("UTF-8"))
                buffer.reset()
            } else if (b != '\r'.code) {
                buffer.write(b)
            }
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
            for (i in off until off + len) {
                val byte = b[i]
                if (byte == '\n'.code.toByte()) {
                    checkLine(buffer.toString("UTF-8"))
                    buffer.reset()
                } else if (byte != '\r'.code.toByte()) {
                    buffer.write(byte.toInt())
                }
            }
        }

        private fun checkLine(line: String) {
            val lower = line.lowercase()
            if (line.startsWith("Detected Fingerprint Failures:") || line.startsWith("[ERROR]") || line.startsWith("FINAL PATCHING RESULT")) return
            if (lower.contains("failed to match the fingerprint") || (lower.contains("fingerprint mismatch") && !line.startsWith("Detected Fingerprint Failures:"))) {
                fingerprintErrors.add(line.trim())
            }
        }

        override fun flush() {
            delegate.flush()
        }

        override fun close() {
            if (buffer.size() > 0) {
                checkLine(buffer.toString("UTF-8"))
                buffer.reset()
            }
            delegate.close()
        }
    }

    val interceptingOut = PrintStream(InterceptingOutputStream(originalOut), true, "UTF-8")
    val interceptingErr = PrintStream(InterceptingOutputStream(originalErr), true, "UTF-8")
    System.setOut(interceptingOut)
    System.setErr(interceptingErr)

    try {
        runBlocking {
            patcher().collect { result ->
                totalPatches++
                val patchName = result.patch.name ?: "Unknown"
                if (result.exception == null) {
                    successfulPatches++
                    println("[PASS] $patchName")
                } else {
                    failedPatches++
                    val err = result.exception?.message ?: "Unknown error"
                    println("[FAIL] $patchName -> $err")
                    result.exception?.printStackTrace()
                    failures.add("$patchName: $err")
                }
            }
        }

        println("\n========================================")
        println("FINAL PATCHING RESULT")
        println("========================================")
        println("Target App:    ${targetApp.appName} (${targetApp.packageName})")
        println("APK File:      ${effectiveApkFile.name}")
        println("Total patches: $totalPatches")
        println("Successful:    $successfulPatches")
        println("Failed:        $failedPatches")
        println("Detected Fingerprint Failures: ${fingerprintErrors.size}")

        if (failedPatches == 0 && fingerprintErrors.isEmpty()) {
            println("\n[BUILD] Compiling modified bytecode & assets via patcher.get()...")
            val patcherResult = patcher.get()
            println("[BUILD] Compiled ${patcherResult.dexFiles.size} DEX files successfully.")

            val outPath = System.getProperty("outputApk")
            if (outPath != null) {
                val outFile = File(outPath).absoluteFile
                outFile.parentFile?.mkdirs()
                val unsignedApk = File(outFile.parentFile, "unsigned-work.apk")
                actualApkFile.copyTo(unsignedApk, overwrite = true)
                println("\n[PACK] Applying patcher result to APK (source: ${unsignedApk.length()} bytes)...")
                patcherResult.applyTo(unsignedApk)
                patcherResult.resources.resourcesApk?.let { resApk ->
                    java.util.zip.ZipFile(resApk).use { resZip ->
                        val manifestEntry = resZip.getEntry("AndroidManifest.xml")
                        if (manifestEntry != null) {
                            val manifestBytes = resZip.getInputStream(manifestEntry).readBytes()
                            val uri = java.net.URI.create("jar:" + unsignedApk.toURI())
                            val env = mapOf("create" to "false")
                            java.nio.file.FileSystems.newFileSystem(uri, env).use { fs ->
                                val targetManifest = fs.getPath("AndroidManifest.xml")
                                java.nio.file.Files.write(targetManifest, manifestBytes)
                            }
                            println("[PACK] Synchronized patched AndroidManifest.xml from resources.apk (${manifestBytes.size} bytes)")
                        }
                    }
                }
                println("[PACK] After applyTo: unsignedApk exists=${unsignedApk.exists()}, size=${unsignedApk.length()} bytes")
                val keystoreFile = File("build/morphe-debug.keystore").absoluteFile
                val ksDetails = ApkUtils.KeyStoreDetails(
                    keyStore = keystoreFile,
                    alias = "morphe",
                    password = "morphepassword",
                )
                try {
                    ApkUtils.signApk(
                        inputApkFile = unsignedApk,
                        outputApkFile = outFile,
                        signer = "Morphe",
                        keyStoreDetails = ksDetails,
                    )
                    println("[DONE] Patched & signed APK saved at: ${outFile.absolutePath}")
                } finally {
                    unsignedApk.delete()
                }

                if (effectiveApkFile.name.endsWith(".apkm", ignoreCase = true)) {
                    java.util.zip.ZipFile(effectiveApkFile).use { apkmZip ->
                        val splitApkEntries = apkmZip.entries().asSequence()
                            .filter { it.name.endsWith(".apk", ignoreCase = true) && it.name != "base.apk" }
                            .toList()
                        for (splitEntry in splitApkEntries) {
                            val rawSplitFile = File(tempDir, splitEntry.name)
                            apkmZip.getInputStream(splitEntry).use { input ->
                                rawSplitFile.outputStream().buffered().use { output -> input.copyTo(output) }
                            }
                            val signedSplitFile = File(outFile.parentFile, splitEntry.name)
                            println("[SIGN] Signing companion split -> ${signedSplitFile.name}...")
                            ApkUtils.signApk(
                                inputApkFile = rawSplitFile,
                                outputApkFile = signedSplitFile,
                                signer = "Morphe",
                                keyStoreDetails = ksDetails,
                            )
                            println("[DONE] Signed companion split saved at: ${signedSplitFile.absolutePath}")
                        }
                    }
                }
            }
        }
    } finally {
        System.setOut(originalOut)
        System.setErr(originalErr)
        patcher.close()
        tempDir.deleteRecursively()
        File("build/tmp/patcher-apkm-source").deleteRecursively()
    }

    if (fingerprintErrors.isNotEmpty()) {
        println("\n[ERROR] Unresolved fingerprint mismatches detected during patch execution (${fingerprintErrors.size}):")
        fingerprintErrors.forEach { println("  • $it") }
        error("Patcher execution failed: ${fingerprintErrors.size} fingerprint mismatch(es) detected! A patch update or creation is NEVER complete until 100% of fingerprints resolve cleanly.")
    }

    if (failedPatches > 0) {
        println("\nFailure details:")
        failures.forEach { println("  - $it") }
        error("Patcher finished with $failedPatches failure(s)")
    } else {
        println("\n100% OF ${targetApp.appName.uppercase()} PATCHES APPLIED WITH ZERO ERRORS AND ZERO FINGERPRINT MISMATCHES!")
    }
}
