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
import java.io.File

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
            "tiktok_global_${Constants.TIKTOK_TARGET_VERSION}.apk",
            "tiktok_global.apk",
        ),
        filePattern = Regex("(?i).*tiktok.*global.*\\.apk$"),
        patchDirectoryPart = "tiktok",
    ),
    TIKTOK_ASIA(
        id = "tiktok_asia",
        appName = "TikTok Asia",
        packageName = Constants.TIKTOK_ASIA_PACKAGE_NAME,
        candidateFilenames = listOf(
            "com.ss.android.ugc.trill_${Constants.TIKTOK_TARGET_VERSION}.apk",
            "com.ss.android.ugc.trill_${Constants.TIKTOK_TARGET_VERSION}-460903_minAPI23(arm64-v8a,armeabi-v7a)(nodpi)_apkmirror.com.apk",
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
            "Bravemonoarm64_v${Constants.BRAVE_TARGET_VERSION}.apk",
            "BraveMonoarm64.apk",
            "Bravemonoarm64_v1.94.121.apk",
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
    );

    companion object {
        fun fromId(raw: String): TargetApp? {
            val normalized = raw.trim().lowercase().replace("-", "_")
            return entries.firstOrNull { it.id == normalized }
                ?: when (normalized) {
                    "trill" -> TIKTOK_ASIA
                    "gboard_lite" -> GBOARD
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
    getDownloadDirectory()?.let { dirs.add(it) }
    dirs.add(File(userHome, "Downloads"))
    dirs.add(File(userHome, "Descargas"))
    dirs.add(File("."))
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

    val apkFile: File
    val targetApp: TargetApp

    if (explicitApkArg != null && File(explicitApkArg).isFile) {
        apkFile = File(explicitApkArg)
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
        cp != null && cp.any { it.packageName == targetApp.packageName } &&
            (patchNameFilter.isNullOrEmpty() || patch.name.equals(patchNameFilter, ignoreCase = true))
    }.toSet()

    require(targetPatches.isNotEmpty()) {
        "No patches found in ${patchFiles.first().name} for target ${targetApp.appName} (${targetApp.packageName})"
    }

    println("Loaded ${targetPatches.size} ${targetApp.appName} patches from ${patchFiles.first().name}:")
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

    val actualApkFile = if (effectiveApkFile.name.endsWith(".apkm", ignoreCase = true)) {
        val apkmExtractDir = File("build/tmp/patcher-apkm-source").absoluteFile
        apkmExtractDir.deleteRecursively()
        apkmExtractDir.mkdirs()
        val extractedBase = File(apkmExtractDir, "base.apk")
        java.util.zip.ZipFile(effectiveApkFile).use { apkmZip ->
            val baseEntry = apkmZip.getEntry("base.apk") ?: error("No base.apk found in APKM bundle: ${effectiveApkFile.name}")
            val splitApkEntries = apkmZip.entries().asSequence()
                .filter { it.name.endsWith(".apk", ignoreCase = true) && it.name != "base.apk" }
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
        keepArchitectures = setOf(CpuArchitecture.ARM64_V8A),
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

        if (failedPatches == 0) {
            println("\n[BUILD] Compiling modified bytecode & assets via patcher.get()...")
            val patcherResult = patcher.get()
            println("[BUILD] Compiled ${patcherResult.dexFiles.size} DEX files successfully.")

            val outPath = System.getProperty("outputApk")
            if (outPath != null) {
                val outFile = File(outPath).absoluteFile
                outFile.parentFile?.mkdirs()
                val unsignedApk = File(tempDir, "unsigned.apk")
                actualApkFile.copyTo(unsignedApk, overwrite = true)
                println("\n[PACK] Applying patcher result to APK...")
                patcherResult.applyTo(unsignedApk)
                val keystoreFile = File("build/morphe-debug.keystore").absoluteFile
                val ksDetails = ApkUtils.KeyStoreDetails(
                    keyStore = keystoreFile,
                    alias = "morphe",
                    password = "morphepassword",
                )
                ApkUtils.signApk(
                    inputApkFile = unsignedApk,
                    outputApkFile = outFile,
                    signer = "Morphe",
                    keyStoreDetails = ksDetails,
                )
                println("[DONE] Patched & signed APK saved at: ${outFile.absolutePath}")

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
        patcher.close()
        tempDir.deleteRecursively()
        File("build/tmp/patcher-apkm-source").deleteRecursively()
    }

    if (failedPatches > 0) {
        println("\nFailure details:")
        failures.forEach { println("  - $it") }
        error("Patcher finished with $failedPatches failure(s)")
    } else {
        println("\n100% OF ${targetApp.appName.uppercase()} PATCHES APPLIED WITH ZERO ERRORS!")
    }
}
