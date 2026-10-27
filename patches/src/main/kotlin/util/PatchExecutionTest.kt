package util

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.dex.BytecodeMode
import app.morphe.patcher.dex.NoOpDexVerifier
import app.morphe.patcher.patch.Patch
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
import java.security.MessageDigest

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
        appName = "TikTok",
        packageName = Constants.TIKTOK_PACKAGE_NAME,
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
    GBOARD(
        id = "gboard",
        appName = "Gboard Lite",
        packageName = Constants.GBOARD_PACKAGE_NAME,
        candidateFilenames = listOf(
            "gboard-${Constants.GBOARD_TARGET_VERSION}.apk",
            "gboard-${Constants.GBOARD_TARGET_VERSION_V7A}.apk",
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
    );

    companion object {
        fun fromId(raw: String): TargetApp? {
            val normalized = raw.trim().lowercase().replace("-", "_")
            return entries.firstOrNull { it.id == normalized }
                ?: when (normalized) {
                    "gboard_lite" -> GBOARD
                    else -> null
                }
        }

        fun fromFileName(fileName: String): TargetApp? {
            val lower = fileName.lowercase()
            return when {
                lower.contains("tiktok") -> TIKTOK
                lower.contains("gboard") -> GBOARD
                lower.contains("brave") -> BRAVE
                else -> entries.firstOrNull { it.filePattern.containsMatchIn(fileName) }
            }
        }
    }
}

// Tuning default for the warning only (TikTok's heaviest patch measured ~27 s isolated, the rest under 10 s).
private const val DEFAULT_SLOW_PATCH_SECONDS = 15.0

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
        // Never fall back to an output of a previous run: re-patching a patched APK hides fingerprint drift.
        val matched = dir.listFiles { f ->
            f.isFile && target.filePattern.containsMatchIn(f.name) && !f.name.contains("patched", ignoreCase = true)
        }
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

private fun findAndroidBuildTool(toolName: String): File? {
    System.getenv("PATH")?.split(File.pathSeparator)?.forEach { dir ->
        val file = File(dir, toolName)
        if (file.isFile && file.canExecute()) return file
    }
    val userHome = System.getProperty("user.home") ?: "."
    val sdkRoots = listOfNotNull(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT"),
        File(userHome, "Android/Sdk").takeIf { it.exists() }?.absolutePath,
        File(userHome, "Library/Android/sdk").takeIf { it.exists() }?.absolutePath,
    )
    for (sdk in sdkRoots) {
        val buildToolsDir = File(sdk, "build-tools")
        if (buildToolsDir.isDirectory) {
            val latestTools = buildToolsDir.listFiles()?.filter { it.isDirectory }?.maxByOrNull { dir ->
                val parts = dir.name.split('.').mapNotNull { it.toIntOrNull() }
                parts.getOrElse(0) { 0 } * 10000 + parts.getOrElse(1) { 0 } * 100 + parts.getOrElse(2) { 0 }
            }
            if (latestTools != null) {
                val tool = File(latestTools, toolName)
                if (tool.isFile && tool.canExecute()) return tool
            }
        }
    }
    return null
}

private fun setBinaryXmlVersionCode(data: ByteArray, newVersionCode: Int): ByteArray {
    if (data.size < 40) return data
    val bb = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN)

    // 1. Parse string pool to find the string pool index of "versionCode"
    var pos = 8
    val spType = bb.getInt(pos)
    val spSize = bb.getInt(pos + 4)
    if (spType != 0x001c0001 || spSize <= 0 || pos + spSize > data.size) return data

    val stringCount = bb.getInt(pos + 8)
    val flags = bb.getInt(pos + 16)
    val stringsStart = pos + bb.getInt(pos + 20)
    val isUtf8 = (flags and (1 shl 8)) != 0

    var versionCodeStringIdx = -1
    for (i in 0 until stringCount) {
        if (pos + 28 + (i + 1) * 4 > data.size) break
        val strOff = bb.getInt(pos + 28 + i * 4)
        val sAddr = stringsStart + strOff
        if (sAddr < 0 || sAddr >= data.size) continue

        val str = if (isUtf8) {
            var p = sAddr
            if (p >= data.size) "" else {
                val len1 = data[p].toInt() and 0xFF
                p += if (len1 and 0x80 != 0) 2 else 1
                if (p >= data.size) "" else {
                    val len2 = data[p].toInt() and 0xFF
                    p += if (len2 and 0x80 != 0) 2 else 1
                    var end = p
                    while (end < data.size && data[end] != 0.toByte()) end++
                    String(data, p, (end - p).coerceAtLeast(0), Charsets.UTF_8)
                }
            }
        } else {
            if (sAddr + 2 > data.size) "" else {
                val u16len = bb.getShort(sAddr).toInt() and 0xFFFF
                val p = if (u16len and 0x8000 != 0) sAddr + 4 else sAddr + 2
                val byteLen = (u16len and 0x7FFF) * 2
                if (p + byteLen <= data.size && byteLen >= 0) {
                    String(data, p, byteLen, Charsets.UTF_16LE)
                } else ""
            }
        }

        if (str == "versionCode") {
            versionCodeStringIdx = i
            break
        }
    }

    // Check optional resource map chunk (0x00080180) following string pool
    pos += spSize
    var versionCodeResIdx = -1
    if (pos + 8 <= data.size) {
        val resMapType = bb.getInt(pos)
        val resMapSize = bb.getInt(pos + 4)
        if (resMapType == 0x00080180 && resMapSize > 8) {
            val resCount = (resMapSize - 8) / 4
            for (i in 0 until resCount) {
                if (pos + 8 + i * 4 + 4 > data.size) break
                if (bb.getInt(pos + 8 + i * 4) == 0x0101021b) { // android.R.attr.versionCode
                    versionCodeResIdx = i
                    break
                }
            }
            pos += resMapSize
        }
    }

    // 2. Scan START_TAG chunks for <manifest> and update the matching attribute
    while (pos + 36 <= data.size) {
        val chunkType = bb.getInt(pos)
        val chunkSize = bb.getInt(pos + 4)
        if (chunkType == 0x00100102) { // START_TAG
            val attrStart = bb.getShort(pos + 24).toInt() and 0xFFFF
            val attrSize = (bb.getShort(pos + 26).toInt() and 0xFFFF).coerceAtLeast(20)
            val attrCount = bb.getShort(pos + 28).toInt() and 0xFFFF
            var attrOffset = pos + 16 + attrStart
            for (i in 0 until attrCount) {
                if (attrOffset + 20 > data.size) break
                val nameIdx = bb.getInt(attrOffset + 4)
                val matchesName = (versionCodeStringIdx != -1 && nameIdx == versionCodeStringIdx) ||
                        (versionCodeResIdx != -1 && nameIdx == versionCodeResIdx)
                val aType = bb.getInt(attrOffset + 12)
                val isIntType = (aType and 0x10000000) != 0 || aType == 0x10000008

                if (matchesName || (versionCodeStringIdx == -1 && versionCodeResIdx == -1 && isIntType)) {
                    bb.putInt(attrOffset + 16, newVersionCode)
                    return data
                }
                attrOffset += attrSize
            }
            break
        }
        if (chunkSize <= 0) break
        pos += chunkSize
    }
    return data
}

private fun persistentKeyStoreFile(): File {
    val xdg = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
    val base = if (xdg != null) File(xdg, "morphe") else File(System.getProperty("user.home") ?: ".", ".config/morphe")
    return File(base, "morphe-device.p12")
}

private fun ensurePkcs12KeyStore(keystoreFile: File) {
    if (keystoreFile.exists() && keystoreFile.length() > 0L) return
    keystoreFile.parentFile?.mkdirs()
    val keytoolBin = File(System.getProperty("java.home"), "bin/keytool").takeIf { it.canExecute() }
        ?: System.getenv("PATH")?.split(File.pathSeparator)?.map { File(it, "keytool") }?.firstOrNull { it.canExecute() }
        ?: File("/usr/bin/keytool").takeIf { it.canExecute() }
    if (keytoolBin != null) {
        val proc = ProcessBuilder(
            keytoolBin.absolutePath,
            "-genkeypair",
            "-keystore", keystoreFile.absolutePath,
            "-storetype", "PKCS12",
            "-alias", "morphe",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-storepass", "morphepassword",
            "-keypass", "morphepassword",
            "-dname", "CN=Morphe",
        ).start()
        val exit = proc.waitFor()
        if (exit != 0) {
            println("[WARN] keytool failed with exit code $exit; keystore may not be valid.")
        }
    }
}

private fun verifyInputVersion(
    patcher: Patcher,
    targetPatches: Set<Patch<*>>,
    targetApp: TargetApp,
    inputFile: File,
) {
    val packageMetadata = patcher.context.packageMetadata
    val versionName = packageMetadata.versionName
    val versionCode = packageMetadata.versionCode

    val expectedVersions = targetPatches
        .flatMap { it.compatibility.orEmpty() }
        .filter { it.packageName == targetApp.packageName }
        .flatMap { it.targets }
        .mapNotNull { it.version }
        .toSet()

    if (expectedVersions.isEmpty()) {
        println("[INFO] Version check skipped: selected patches declare no target version.")
        return
    }

    if (versionName in expectedVersions) {
        println("[INFO] Input version $versionName (versionCode $versionCode) matches target.")
        return
    }

    if (System.getProperty("allowVersionMismatch").toBoolean()) {
        println("[WARN] Version mismatch allowed: input $versionName, expected $expectedVersions.")
        return
    }

    patcher.close()
    error("Input APK ${inputFile.name} version '$versionName' does not match expected target version(s) $expectedVersions. Pass -Papk=<path to the target version> or -PallowVersionMismatch=true for differential runs on other versions.")
}

private fun sanitizedSplitEntryName(entryName: String): String? {
    if (entryName.isBlank()) return null
    if (entryName.contains("..")) return null
    if (entryName.contains('/') || entryName.contains('\\')) return null
    if (entryName.startsWith("/") || entryName.contains(":")) return null
    val base = File(entryName).name
    if (base.isBlank() || base == "." || base == "..") return null
    if (base != entryName) return null
    if (!base.endsWith(".apk", ignoreCase = true)) return null
    return base
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

    val explicitTarget = explicitTargetArg?.let { raw -> TargetApp.fromId(raw) ?: error("Unknown target app '$raw'. Valid values: ${TargetApp.entries.joinToString { it.id }}.") }

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
        targetApp = explicitTarget
            ?: TargetApp.fromFileName(apkFile.name)
            ?: error("Could not infer target app for APK: ${apkFile.name}. Specify app via -Papp=<target> or args.")
    } else {
        targetApp = explicitTarget
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

    if (System.getProperty("allOptions").toBoolean()) {
        var forced = 0
        targetPatches.forEach { patch ->
            patch.options.values.filter { it.default is Boolean }.forEach { option ->
                patch.options[option.name] = true
                forced++
            }
        }
        println("[INFO] allOptions: forced $forced boolean patch option(s) to true.")
    }

    applyPatchOptions(System.getProperty("patchOptions"), targetPatches)

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
        keepArchitectures = setOf(CpuArchitecture.ARM64_V8A, CpuArchitecture.ARMEABI_V7A),
        useBytecodeMode = BytecodeMode.STRIP_FAST,
        verifier = NoOpDexVerifier
    )

    println("\n[INIT] Initializing Morphe Patcher engine...")
    val patcher = Patcher(config)
    verifyInputVersion(patcher, targetPatches, targetApp, effectiveApkFile)
    patcher += targetPatches

    println("[EXEC] Executing patch pipeline on ${effectiveApkFile.name} (target: ${targetApp.appName})...")
    var totalPatches = 0
    var successfulPatches = 0
    var failedPatches = 0
    val failures = mutableListOf<String>()

    val originalOut = System.out
    val originalErr = System.err
    val fingerprintErrors = java.util.concurrent.CopyOnWriteArrayList<String>()
    // The inline smali tree walker only reports semantic errors (e.g. "[6,8] Invalid register: v22")
    // to stderr and silently drops the offending instruction, so they must be caught here.
    val smaliErrorPattern = Regex("""^\[\d+,\d+] .+""")
    val pendingSmaliErrors = java.util.concurrent.CopyOnWriteArrayList<String>()
    val pendingSkipLines = java.util.concurrent.CopyOnWriteArrayList<String>()
    val smaliCompileErrors = mutableListOf<String>()
    val skipWarnings = mutableListOf<String>()

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
            if (smaliErrorPattern.matches(line.trim())) {
                pendingSmaliErrors.add(line.trim())
                return
            }
            val lower = line.lowercase()
            if (line.startsWith("Detected Fingerprint Failures:") || line.startsWith("[ERROR]") || line.startsWith("FINAL PATCHING RESULT")) return
            if (lower.contains("failed to match the fingerprint") || (lower.contains("fingerprint mismatch") && !line.startsWith("Detected Fingerprint Failures:"))) {
                fingerprintErrors.add(line.trim())
            }
            if (isPatchSkipLine(line)) {
                pendingSkipLines.add(line.trim())
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
        val patchTimings = mutableListOf<Pair<String, Long>>()
        var lastResultNanos = System.nanoTime()
        runBlocking {
            patcher().collect { result ->
                totalPatches++
                val patchName = result.patch.name ?: "Unknown"
                val now = System.nanoTime()
                // The patcher runs patches sequentially and emits one result per patch,
                // so the gap between results is that patch's execution time (the first entry also includes pipeline startup,
                // and an entry also includes any dependency patch that runs for the first time right before it).
                val elapsedMs = (now - lastResultNanos) / 1_000_000
                lastResultNanos = now
                patchTimings.add(patchName to elapsedMs)
                if (pendingSmaliErrors.isNotEmpty()) {
                    pendingSmaliErrors.forEach { smaliCompileErrors.add("$patchName: $it") }
                    pendingSmaliErrors.clear()
                }
                if (result.exception == null) {
                    successfulPatches++
                    println("[PASS] $patchName")
                    if (pendingSkipLines.isNotEmpty()) {
                        println("[WARN] $patchName passed with skipped steps:")
                        val lines = pendingSkipLines.toList()
                        lines.forEach { println("    $it") }
                        val suffix = if (lines.size > 1) " (+${lines.size - 1} more)" else ""
                        skipWarnings.add("$patchName: ${lines.first()}$suffix")
                    }
                } else {
                    failedPatches++
                    val err = result.exception?.message ?: "Unknown error"
                    val location = result.exception?.let(::patchSourceLocation)
                    val detail = if (location != null) "$err (at $location)" else err
                    println("[FAIL] $patchName -> $detail")
                    result.exception?.printStackTrace()
                    failures.add("$patchName: $detail")
                }
                pendingSkipLines.clear()
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
        println("Detected Smali Compile Errors: ${smaliCompileErrors.size}")
        println("Patches with skipped steps: ${skipWarnings.size}")
        if (skipWarnings.isNotEmpty()) {
            skipWarnings.forEach { println("  - $it") }
        }
        printPatchTimings(patchTimings, resolveSlowPatchThreshold(), patchTimings.firstOrNull()?.first)

        if (failedPatches == 0 && fingerprintErrors.isEmpty() && smaliCompileErrors.isEmpty()) {
            println("\n[BUILD] Compiling modified bytecode & assets via patcher.get()...")
            File(tempDir, "patched/dex").mkdirs()
            File(tempDir, "patched/originalDex").mkdirs()
            val patcherResult = patcher.get()
            println("[BUILD] Compiled ${patcherResult.dexFiles.size} DEX files successfully.")

            val outPath = System.getProperty("outputApk")
            val dexDigestOption = resolveDexDigestOption()
            if (outPath != null) {
                val outFile = resolveOutputPath(outPath)
                outFile.parentFile?.mkdirs()
                val unsignedApk = File(tempDir, "unsigned-work.apk")
                actualApkFile.copyTo(unsignedApk, overwrite = true)
                println("\n[PACK] Applying patcher result to APK (source: ${unsignedApk.length()} bytes)...")
                val maxVersionCode = System.getProperty("maxVersionCode") == "true" || System.getenv("MAX_VERSION_CODE") == "true"
                var manifestSynced = false
                patcherResult.applyTo(unsignedApk)
                patcherResult.resources.resourcesApk?.let { resApk ->
                    java.util.zip.ZipFile(resApk).use { resZip ->
                        val manifestEntry = resZip.getEntry("AndroidManifest.xml")
                        if (manifestEntry != null) {
                            val rawManifestBytes = resZip.getInputStream(manifestEntry).readBytes()
                            // Apply the versionCode override in the same pass so the APK zip is rewritten only once.
                            val manifestBytes = if (maxVersionCode) setBinaryXmlVersionCode(rawManifestBytes, Int.MAX_VALUE) else rawManifestBytes
                            val uri = java.net.URI.create("jar:" + unsignedApk.toURI())
                            val env = mapOf("create" to "false")
                            java.nio.file.FileSystems.newFileSystem(uri, env).use { fs ->
                                val targetManifest = fs.getPath("AndroidManifest.xml")
                                java.nio.file.Files.write(targetManifest, manifestBytes)
                            }
                            manifestSynced = true
                            println("[PACK] Synchronized patched AndroidManifest.xml from resources.apk (${manifestBytes.size} bytes)")
                            if (maxVersionCode) println("[PACK] Overrode AndroidManifest.xml versionCode -> 2147483647 (Int.MAX_VALUE)")
                        }
                    }
                }
                if (maxVersionCode && !manifestSynced) {
                    val uri = java.net.URI.create("jar:" + unsignedApk.toURI())
                    val env = mapOf("create" to "false")
                    java.nio.file.FileSystems.newFileSystem(uri, env).use { fs ->
                        val targetManifest = fs.getPath("AndroidManifest.xml")
                        if (java.nio.file.Files.exists(targetManifest)) {
                            val rawBytes = java.nio.file.Files.readAllBytes(targetManifest)
                            val patchedBytes = setBinaryXmlVersionCode(rawBytes, Int.MAX_VALUE)
                            java.nio.file.Files.write(targetManifest, patchedBytes)
                            println("[PACK] Overrode AndroidManifest.xml versionCode -> 2147483647 (Int.MAX_VALUE)")
                        }
                    }
                }
                println("[PACK] After applyTo: unsignedApk exists=${unsignedApk.exists()}, size=${unsignedApk.length()} bytes")
                dexDigestOption?.let { printDexDigest(unsignedApk, it) }
                val zipalignBin = findAndroidBuildTool("zipalign")
                val apksignerBin = findAndroidBuildTool("apksigner")
                val buildToolsMajor = zipalignBin?.parentFile?.name?.split('.')?.firstOrNull()?.toIntOrNull() ?: 0
                val supports16k = buildToolsMajor >= 35

                val keystoreFile = persistentKeyStoreFile()
                ensurePkcs12KeyStore(keystoreFile)

                if (zipalignBin != null && apksignerBin != null && keystoreFile.exists() && keystoreFile.length() > 0L) {
                    try {
                        val alignMode = if (supports16k) "16 KB page alignment (-P 16 4)" else "4-byte alignment"
                        println("\n[ALIGN] Running $alignMode via zipalign...")
                        val alignArgs = if (supports16k) {
                            listOf(zipalignBin.absolutePath, "-f", "-P", "16", "4", unsignedApk.absolutePath, outFile.absolutePath)
                        } else {
                            listOf(zipalignBin.absolutePath, "-f", "4", unsignedApk.absolutePath, outFile.absolutePath)
                        }
                        val alignProc = ProcessBuilder(alignArgs).inheritIO().start()
                        val alignExit = alignProc.waitFor()
                        if (alignExit != 0) {
                            error("zipalign failed with exit code $alignExit")
                        }

                        println("[SIGN] Signing APK via apksigner...")
                        val signArgs = mutableListOf(
                            apksignerBin.absolutePath,
                            "sign",
                            "--ks", keystoreFile.absolutePath,
                            "--ks-type", "PKCS12",
                            "--ks-pass", "pass:morphepassword",
                        )
                        if (supports16k) {
                            signArgs.add("--alignment-preserved")
                        }
                        signArgs.add(outFile.absolutePath)

                        val signProc = ProcessBuilder(signArgs).inheritIO().start()
                        val signExit = signProc.waitFor()
                        if (signExit != 0) {
                            error("apksigner failed with exit code $signExit")
                        }
                        println("[DONE] Patched, aligned & signed APK saved at: ${outFile.absolutePath}")
                    } finally {
                        unsignedApk.delete()
                    }
                } else {
                    println("\n[WARN] Android SDK zipalign/apksigner not found; falling back to ApkUtils.signApk...")
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
                }

                if (effectiveApkFile.name.endsWith(".apkm", ignoreCase = true) || effectiveApkFile.name.endsWith(".xapk", ignoreCase = true)) {
                    java.util.zip.ZipFile(effectiveApkFile).use { apkmZip ->
                        val baseEntry = apkmZip.getEntry("base.apk")
                            ?: apkmZip.entries().asSequence().firstOrNull { it.name.endsWith(".apk") && (it.name.contains("base") || it.name.startsWith(targetApp.packageName)) }
                            ?: apkmZip.entries().asSequence().filter { it.name.endsWith(".apk") }.maxByOrNull { it.size }
                        val splitApkEntries = apkmZip.entries().asSequence()
                            .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) && it.name != baseEntry?.name }
                            .toList()
                        splitApkEntries.parallelStream().forEach { splitEntry ->
                            val safeName = sanitizedSplitEntryName(splitEntry.name)
                            if (safeName == null) {
                                println("[WARN] Skipping split entry '${splitEntry.name}': unsafe entry name.")
                                return@forEach
                            }
                            val canonicalDir = tempDir.canonicalPath + File.separator
                            val rawSplitFile = File(tempDir, safeName)
                            if (!rawSplitFile.canonicalPath.startsWith(canonicalDir)) {
                                println("[WARN] Skipping split entry '${splitEntry.name}' resolving outside workspace: ${rawSplitFile.canonicalPath}")
                                return@forEach
                            }
                            apkmZip.getInputStream(splitEntry).use { input ->
                                rawSplitFile.outputStream().buffered().use { output -> input.copyTo(output) }
                            }

                            if (maxVersionCode) {
                                try {
                                    val uri = java.net.URI.create("jar:" + rawSplitFile.toURI())
                                    java.nio.file.FileSystems.newFileSystem(uri, emptyMap<String, Any>()).use { fs ->
                                        val splitManifest = fs.getPath("AndroidManifest.xml")
                                        if (java.nio.file.Files.exists(splitManifest)) {
                                            val rawBytes = java.nio.file.Files.readAllBytes(splitManifest)
                                            val patchedBytes = setBinaryXmlVersionCode(rawBytes, Int.MAX_VALUE)
                                            java.nio.file.Files.write(splitManifest, patchedBytes)
                                            println("[PACK] Overrode companion split ${splitEntry.name} versionCode -> 2147483647 (Int.MAX_VALUE)")
                                        }
                                    }
                                } catch (e: Exception) {
                                    println("[WARN] Could not override split ${splitEntry.name} versionCode: ${e.message}")
                                }
                            }

                            // Prefix splits with the output name so bundles of different apps never overwrite
                            // each other and `adb install-multiple -r <out-stem>*.apk` installs the whole set.
                            val signedSplitFile = File(outFile.parentFile, "${outFile.nameWithoutExtension}.$safeName")
                            println("[SIGN] Signing companion split -> ${signedSplitFile.name}...")
                            if (zipalignBin != null && apksignerBin != null && keystoreFile.exists() && keystoreFile.length() > 0L) {
                                val splitAlignArgs = if (supports16k) {
                                    listOf(zipalignBin.absolutePath, "-f", "-P", "16", "4", rawSplitFile.absolutePath, signedSplitFile.absolutePath)
                                } else {
                                    listOf(zipalignBin.absolutePath, "-f", "4", rawSplitFile.absolutePath, signedSplitFile.absolutePath)
                                }
                                val splitAlignProc = ProcessBuilder(splitAlignArgs).inheritIO().start()
                                val splitAlignExit = splitAlignProc.waitFor()
                                if (splitAlignExit != 0) {
                                    error("zipalign failed for split ${splitEntry.name} with exit code $splitAlignExit")
                                }

                                val splitSignArgs = mutableListOf(
                                    apksignerBin.absolutePath,
                                    "sign",
                                    "--ks", keystoreFile.absolutePath,
                                    "--ks-type", "PKCS12",
                                    "--ks-pass", "pass:morphepassword",
                                )
                                if (supports16k) {
                                    splitSignArgs.add("--alignment-preserved")
                                }
                                splitSignArgs.add(signedSplitFile.absolutePath)

                                val splitSignProc = ProcessBuilder(splitSignArgs).inheritIO().start()
                                val splitSignExit = splitSignProc.waitFor()
                                if (splitSignExit != 0) {
                                    error("apksigner failed for split ${splitEntry.name} with exit code $splitSignExit")
                                }
                            } else {
                                val ksDetails = ApkUtils.KeyStoreDetails(
                                    keyStore = keystoreFile,
                                    alias = "morphe",
                                    password = "morphepassword",
                                )
                                ApkUtils.signApk(
                                    inputApkFile = rawSplitFile,
                                    outputApkFile = signedSplitFile,
                                    signer = "Morphe",
                                    keyStoreDetails = ksDetails,
                                )
                            }
                            println("[DONE] Signed companion split saved at: ${signedSplitFile.absolutePath}")
                        }
                    }
                }
            } else if (dexDigestOption != null) {
                // Without -Pout an unsigned work APK is still assembled so the digest covers exactly what -Pout would ship.
                val digestApk = File(tempDir, "digest-work.apk")
                actualApkFile.copyTo(digestApk, overwrite = true)
                patcherResult.applyTo(digestApk)
                printDexDigest(digestApk, dexDigestOption)
                digestApk.delete()
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

    if (smaliCompileErrors.isNotEmpty()) {
        println("\n[ERROR] Inline smali compile errors detected; the offending instructions were dropped (${smaliCompileErrors.size}):")
        smaliCompileErrors.forEach { println("  • $it") }
        error("Patcher execution failed: ${smaliCompileErrors.size} inline smali compile error(s) detected! Non-range invokes cannot address registers above v15; use the /range form or move values into low registers.")
    }

    if (failedPatches > 0) {
        println("\nFailure details:")
        failures.forEach { println("  - $it") }
        error("Patcher finished with $failedPatches failure(s)")
    } else {
        println("\n100% OF ${targetApp.appName.uppercase()} PATCHES APPLIED WITH ZERO ERRORS AND ZERO FINGERPRINT MISMATCHES!")
    }
}

private fun resolveSlowPatchThreshold(): Double {
    val raw = System.getProperty("slowPatchSeconds") ?: return DEFAULT_SLOW_PATCH_SECONDS
    val parsed = raw.toDoubleOrNull()
    if (parsed == null || parsed <= 0.0) {
        println("[WARN] Invalid slowPatchSeconds '$raw'; using default $DEFAULT_SLOW_PATCH_SECONDS s.")
        return DEFAULT_SLOW_PATCH_SECONDS
    }
    return parsed
}

private fun printPatchTimings(
    timings: List<Pair<String, Long>>,
    thresholdSeconds: Double,
    firstPatchName: String? = timings.firstOrNull()?.first,
) {
    if (timings.isEmpty()) return

    val sorted = timings.sortedByDescending { it.second }
    println("Slowest patches:")
    for ((name, elapsedMs) in sorted.take(5)) {
        val seconds = elapsedMs / 1000.0
        val formatted = String.format(java.util.Locale.ROOT, "%.1f", seconds)
        val startupNote = if (name == firstPatchName) " (includes pipeline startup)" else ""
        println("  $formatted s  $name$startupNote")
    }

    val totalSeconds = timings.sumOf { it.second } / 1000.0
    val formattedTotal = String.format(java.util.Locale.ROOT, "%.1f", totalSeconds)
    println("Total patch time: $formattedTotal s")

    val formattedThreshold = String.format(java.util.Locale.ROOT, "%.1f", thresholdSeconds)
    for ((name, elapsedMs) in sorted) {
        val seconds = elapsedMs / 1000.0
        if (seconds > thresholdSeconds) {
            val formattedSeconds = String.format(java.util.Locale.ROOT, "%.1f", seconds)
            val startupNote = if (name == firstPatchName) " (includes pipeline startup)" else ""
            println("[WARN] Slow patch: $name took $formattedSeconds s (threshold $formattedThreshold s)$startupNote")
        }
    }
}

private fun resolveOutputPath(path: String): File {
    val directFile = File(path)
    if (directFile.isAbsolute) return directFile
    val fromParent = File("..", path)
    return if (fromParent.parentFile?.isDirectory == true) {
        fromParent.canonicalFile
    } else {
        directFile.absoluteFile
    }
}

private fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { String.format(java.util.Locale.ROOT, "%02x", it) }
}

private fun writeDexDigestFile(file: File, content: String) {
    file.parentFile?.mkdirs()
    file.writeText(content, Charsets.UTF_8)
    println("[DIGEST] Per-file digests written to ${file.absolutePath}")
}

private fun resolveDexDigestOption(): String? {
    val raw = System.getProperty("dexDigest") ?: return null
    if (raw.isBlank()) {
        println("[WARN] -PdexDigest has no value; use true or a file path. Digest skipped.")
        return null
    }
    if (raw.equals("false", ignoreCase = true)) return null
    return raw
}

private fun printDexDigest(apk: File, option: String) {
    val dexPattern = Regex("""^classes\d*\.dex$""")
    val fileDigests = java.util.zip.ZipFile(apk).use { zip ->
        zip.entries().asSequence()
            .filter { !it.isDirectory && dexPattern.matches(it.name) }
            .map { entry ->
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                entry.name to sha256Hex(bytes)
            }
            .toList()
    }

    val linesText = fileDigests
        .sortedBy { it.first }
        .joinToString("") { "${it.first} ${it.second}\n" }
    val aggregate = sha256Hex(linesText.toByteArray(Charsets.UTF_8))
    println("[DIGEST] Patched DEX set SHA-256: $aggregate (${fileDigests.size} files)")

    if (!option.equals("true", ignoreCase = true)) {
        writeDexDigestFile(resolveOutputPath(option), linesText)
    }
}

/** Matches a stack frame from a patch and returns its source location. */
private fun patchFrameLocation(frame: StackTraceElement): String? {
    if (!frame.className.startsWith("app.morphe.patches.")) return null
    val file = frame.fileName ?: return null
    if (frame.lineNumber <= 0) return null
    return "$file:${frame.lineNumber}"
}

/** Finds the first patch source location in the error cause chain. */
private fun patchSourceLocation(error: Throwable): String? {
    val visited = mutableSetOf<Throwable>()
    var current: Throwable? = error
    var depth = 0
    while (current != null && depth < 10 && visited.add(current)) {
        val location = current.stackTrace.firstNotNullOfOrNull(::patchFrameLocation)
        if (location != null) return location
        current = current.cause
        depth++
    }
    return null
}

private val RUNNER_SKIP_PREFIXES = listOf(
    "[PASS]",
    "[FAIL]",
    "[WARN]",
    "[INFO]",
    "[EXEC]",
    "[BUILD]",
    "[PACK]",
    "[DONE]",
    "Loaded ",
    "Detected ",
)

private val PATCH_SKIP_PATTERN = Regex("""(?<!\d )\bskipped\b|\bskipping\b|\bnot found\b""", RegexOption.IGNORE_CASE)

private fun isPatchSkipLine(line: String): Boolean {
    if (RUNNER_SKIP_PREFIXES.any { line.startsWith(it) }) return false
    val lower = line.lowercase()
    if (lower.contains("fingerprint")) return false
    return PATCH_SKIP_PATTERN.containsMatchIn(line)
}

private fun applyPatchOptions(rawOptions: String?, targetPatches: Set<Patch<*>>) {
    if (rawOptions.isNullOrBlank()) return

    val entries = parsePatchOptionEntries(rawOptions)
    if (entries.isEmpty()) return

    val setCount = applyPatchOptionEntries(entries, targetPatches)
    println("[INFO] patchOptions: set $setCount option value(s).")
}

private fun parsePatchOptionEntries(raw: String): List<Pair<String, String>> {
    return raw.split(';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { parsePatchOptionPair(it) }
}

private fun parsePatchOptionPair(entry: String): Pair<String, String> {
    val eqIndex = entry.indexOf('=')
    if (eqIndex <= 0) {
        error("Invalid patchOptions entry '$entry': expected <key>=<value>")
    }
    val key = entry.substring(0, eqIndex).trim()
    val value = entry.substring(eqIndex + 1).trim()
    return Pair(key, value)
}

private fun applyPatchOptionEntries(
    entries: List<Pair<String, String>>,
    targetPatches: Set<Patch<*>>,
): Int {
    var setCount = 0
    for ((key, rawValue) in entries) {
        setCount += applySingleOptionToPatches(key, rawValue, targetPatches)
    }
    return setCount
}

private fun applySingleOptionToPatches(
    key: String,
    rawValue: String,
    targetPatches: Set<Patch<*>>,
): Int {
    val matchingPatches = targetPatches.filter { it.options.containsKey(key) }
    if (matchingPatches.isEmpty()) {
        error("Unknown patch option key '$key': does not match any target patch option")
    }
    for (patch in matchingPatches) {
        val option = patch.options[key]!!
        val convertedValue = parseOptionValue(key, rawValue, option.default)
        patch.options[key] = convertedValue
    }
    return matchingPatches.size
}

private fun parseOptionValue(key: String, rawValue: String, default: Any?): Any {
    if (default == null || default is String) return rawValue
    return parseTypedOptionValue(key, rawValue, default)
}

private fun parseTypedOptionValue(key: String, rawValue: String, default: Any): Any {
    return when (default) {
        is Boolean -> parseBooleanOption(key, rawValue)
        is Int -> parseIntOption(key, rawValue)
        is Long -> parseLongOption(key, rawValue)
        else -> error("Unsupported option type '${default::class.qualifiedName}' for key '$key'")
    }
}

private fun parseBooleanOption(key: String, rawValue: String): Boolean {
    return try {
        rawValue.toBooleanStrict()
    } catch (_: IllegalArgumentException) {
        error("Invalid boolean value '$rawValue' for option key '$key'")
    }
}

private fun parseIntOption(key: String, rawValue: String): Int {
    return try {
        rawValue.toInt()
    } catch (_: NumberFormatException) {
        error("Invalid int value '$rawValue' for option key '$key'")
    }
}

private fun parseLongOption(key: String, rawValue: String): Long {
    return try {
        rawValue.toLong()
    } catch (_: NumberFormatException) {
        error("Invalid long value '$rawValue' for option key '$key'")
    }
}
