package util

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.dex.BytecodeMode
import app.morphe.patcher.dex.NoOpDexVerifier
import app.morphe.patcher.patch.loadPatchesFromJar
import app.morphe.patcher.resource.CpuArchitecture
import kotlinx.coroutines.runBlocking
import app.morphe.patches.shared.Constants
import java.io.File

private fun getDownloadDirectory(userHome: String): File? {
    return try {
        val process = ProcessBuilder("xdg-user-dir", "DOWNLOAD").start()
        val dir = process.inputStream.bufferedReader().readText().trim()
        if (dir.isNotEmpty() && File(dir).isDirectory) File(dir) else null
    } catch (_: Exception) {
        null
    }
}

fun main(args: Array<String>) {
    val userHome = System.getProperty("user.home") ?: "."
    val apkName = "tiktok_global_${Constants.TIKTOK_TARGET_VERSION}.apk"
    val defaultCandidates = listOfNotNull(
        System.getenv("TARGET_APK_PATH")?.let { File(it) },
        getDownloadDirectory(userHome)?.let { File(it, apkName) },
        File(userHome, "Downloads/$apkName"),
        File(apkName)
    )
    val apkFile = if (args.isNotEmpty()) {
        File(args[0])
    } else {
        defaultCandidates.firstOrNull { it.exists() }
            ?: defaultCandidates.first()
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

    val targetPackage = Constants.TIKTOK_GLOBAL_PACKAGE_NAME
    val tiktokPatches = allPatches.filter { patch ->
        val cp = patch.compatibility
        cp != null && cp.any { it.packageName == targetPackage }
    }.toSet()

    println("Loaded ${tiktokPatches.size} TikTok patches from ${patchFiles.first().name}:")
    tiktokPatches.sortedBy { it.name }.forEach { println("  • ${it.name}") }

    val tempDir = File("build/tmp/patcher-test-workspace")
    tempDir.deleteRecursively()
    tempDir.mkdirs()

    val config = PatcherConfig(
        apkFile = apkFile,
        temporaryFilesPath = tempDir,
        aaptBinaryPath = null,
        frameworkFileDirectory = null,
        useArsclib = false,
        keepArchitectures = setOf(CpuArchitecture.ARM64_V8A),
        useBytecodeMode = BytecodeMode.FULL,
        verifier = NoOpDexVerifier
    )

    println("\n🚀 Initializing Morphe Patcher engine...")
    val patcher = Patcher(config)
    patcher += tiktokPatches

    println("⚡ Executing patch pipeline on ${apkFile.name}...")
    var totalPatches = 0
    var successfulPatches = 0
    var failedPatches = 0
    val failures = mutableListOf<String>()

    runBlocking {
        patcher().collect { result ->
            totalPatches++
            val patchName = result.patch.name ?: "Unknown"
            if (result.exception == null) {
                successfulPatches++
                println("✅ [PASS] $patchName")
            } else {
                failedPatches++
                val err = result.exception?.message ?: "Unknown error"
                println("❌ [FAIL] $patchName -> $err")
                result.exception?.printStackTrace()
                failures.add("$patchName: $err")
            }
        }
    }

    println("\n========================================")
    println("🎯 FINAL PATCHING RESULT")
    println("========================================")
    println("Total patches: $totalPatches")
    println("Successful:    $successfulPatches")
    println("Failed:        $failedPatches")

    patcher.close()
    tempDir.deleteRecursively()

    if (failedPatches > 0) {
        println("\nFailure details:")
        failures.forEach { println("  - $it") }
        error("Patcher finished with $failedPatches failure(s)")
    } else {
        println("\n✨ 100% OF TIKTOK PATCHES APPLIED WITH ZERO ERRORS!")
    }
}
