package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import java.io.File
import java.io.RandomAccessFile
import org.w3c.dom.Element

private const val PT_GNU_PROPERTY = 0x6474e553
private const val NT_GNU_PROPERTY_TYPE_0 = 5
private const val GNU_PROPERTY_AARCH64_FEATURE_1_AND = 0xC0000000L
private const val GNU_PROPERTY_AARCH64_FEATURE_1_BTI = 1

private fun RandomAccessFile.readShortLe(): Short {
    val b0 = read() and 0xFF
    val b1 = read() and 0xFF
    return ((b1 shl 8) or b0).toShort()
}

private fun RandomAccessFile.readIntLe(): Int {
    val b0 = read() and 0xFF
    val b1 = read() and 0xFF
    val b2 = read() and 0xFF
    val b3 = read() and 0xFF
    return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
}

private fun RandomAccessFile.readLongLe(): Long {
    var result = 0L
    for (i in 0 until 8) {
        val b = read().toLong() and 0xFF
        result = result or (b shl (i * 8))
    }
    return result
}

private fun RandomAccessFile.writeIntLe(v: Int) {
    write(v and 0xFF)
    write((v ushr 8) and 0xFF)
    write((v ushr 16) and 0xFF)
    write((v ushr 24) and 0xFF)
}

private fun findGnuPropertyOffset(raf: RandomAccessFile, ePhOff: Long, ePhEntSize: Int, ePhNum: Int): Long {
    for (i in 0 until ePhNum) {
        raf.seek(ePhOff + i * ePhEntSize)
        val pType = raf.readIntLe()
        if (pType == PT_GNU_PROPERTY) {
            raf.seek(ePhOff + i * ePhEntSize + 8)
            return raf.readLongLe()
        }
    }
    return -1L
}

/**
 * Returns 1 when a BTI flag was cleared and verified, -1 when a cleared flag
 * failed read-back verification, 0 when there was nothing to clear.
 */
private fun clearBtiInGnuProperty(raf: RandomAccessFile, gnuPropertyOffset: Long): Int {
    if (gnuPropertyOffset + 12 > raf.length()) return 0
    raf.seek(gnuPropertyOffset)
    val nameSz = raf.readIntLe()
    val descSz = raf.readIntLe()
    val nType = raf.readIntLe()
    if (nType != NT_GNU_PROPERTY_TYPE_0 || nameSz < 4 || gnuPropertyOffset + 12 + nameSz > raf.length()) return 0

    val nameBytes = ByteArray(nameSz)
    raf.readFully(nameBytes)
    val name = String(nameBytes).trimEnd('\u0000')
    if (name != "GNU") return 0

    val descOffset = gnuPropertyOffset + 12 + ((nameSz + 3) and -4)
    if (descOffset + descSz > raf.length()) return 0

    var curOffset = descOffset
    val endOffset = descOffset + descSz
    var result = 0

    while (curOffset + 8 <= endOffset && curOffset + 8 <= raf.length()) {
        raf.seek(curOffset)
        val prType = raf.readIntLe().toLong() and 0xFFFFFFFFL
        val prDataSz = raf.readIntLe()
        if (prDataSz < 0 || curOffset + 8 + prDataSz > raf.length()) break

        if (prType == GNU_PROPERTY_AARCH64_FEATURE_1_AND && prDataSz >= 4) {
            val flagsOffset = curOffset + 8
            raf.seek(flagsOffset)
            val flags = raf.readIntLe()
            if ((flags and GNU_PROPERTY_AARCH64_FEATURE_1_BTI) != 0) {
                val clearedFlags = flags and GNU_PROPERTY_AARCH64_FEATURE_1_BTI.inv()
                raf.seek(flagsOffset)
                raf.writeIntLe(clearedFlags)
                raf.seek(flagsOffset)
                if (raf.readIntLe() != clearedFlags) {
                    println("[Brave Compatibility] WARNING: BTI flag read-back mismatch at offset $flagsOffset; treating as unmodified.")
                    result = -1
                } else if (result == 0) {
                    result = 1
                }
            }
        }

        val itemSize = 8 + ((prDataSz + 7) and -8)
        if (itemSize <= 0) break
        curOffset += itemSize
    }

    return result
}

private fun patchBtiInElf(raf: RandomAccessFile): Int {
    if (raf.length() < 0x40) return 0
    val magic = ByteArray(4)
    raf.seek(0)
    raf.readFully(magic)
    val expectedMagic = byteArrayOf(0x7f, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())
    if (!magic.contentEquals(expectedMagic)) return 0
    if (raf.readByte().toInt() != 2) return 0 // 64-bit ELF

    raf.seek(0x20)
    val ePhOff = raf.readLongLe()
    raf.seek(0x36)
    val ePhEntSize = raf.readShortLe().toInt()
    val ePhNum = raf.readShortLe().toInt()
    if (ePhOff <= 0 || ePhEntSize < 56 || ePhNum <= 0) return 0
    if (ePhOff + ePhNum.toLong() * ePhEntSize > raf.length()) return 0

    val gnuPropOffset = findGnuPropertyOffset(raf, ePhOff, ePhEntSize, ePhNum)
    if (gnuPropOffset < 0) return 0

    return clearBtiInGnuProperty(raf, gnuPropOffset)
}

private data class NativeTrapHook(
    val offset: Long,
    val expected: ByteArray,
    val replacement: ByteArray,
    val description: String,
)

private val ARM64_RET = byteArrayOf(0xc0.toByte(), 0x03, 0x5f.toByte(), 0xd6.toByte())

private val LIBCHROME_TRAP_HOOKS = listOf(
    // Constructor 0: __init_cpu_features_constructor detecting LSE atomics. Neutralizing to RET prevents
    // illegal opcode execution on ARMv8.0 cores (e.g. Kryo 240 / Cortex-A73) and forces safe baseline LDXR/STLXR.
    NativeTrapHook(
        offset = 0x029ef9a4L,
        expected = byteArrayOf(0x3f, 0x23, 0x03, 0xd5.toByte()),
        replacement = ARM64_RET,
        description = "LSE atomics feature constructor",
    ),
    // Constructor 1: __init_cpu_features detecting ARMv8.1+ extensions. Neutralizing to RET enforces baseline
    // ARMv8.0 dispatch, preventing illegal instruction faults on legacy ARM64 cores.
    NativeTrapHook(
        offset = 0x029efbf8L,
        expected = byteArrayOf(0x5f, 0x24, 0x03, 0xd5.toByte()),
        replacement = ARM64_RET,
        description = "CPU extensions feature constructor",
    ),
    // Constructor 2: Brave Promo banner & histogram static initialization calling atomic helpers via range extension thunks.
    // Neutralizing to RET bypasses the thunk pool, eliminating startup SIGILL.
    NativeTrapHook(
        offset = 0x08d5fe1cL,
        expected = byteArrayOf(0x3f, 0x23, 0x03, 0xd5.toByte()),
        replacement = ARM64_RET,
        description = "Promo banner static initialization constructor",
    ),
    // Constructor 11: Brave wallet/rewards fee static initialization calling atomic helpers via range extension thunks.
    // Neutralizing to RET bypasses the thunk pool, eliminating startup SIGILL.
    NativeTrapHook(
        offset = 0x0afbd8e4L,
        expected = byteArrayOf(0x3f, 0x23, 0x03, 0xd5.toByte()),
        replacement = ARM64_RET,
        description = "Brave wallet fee static initialization constructor",
    ),
)

private fun applyTrapHook(raf: RandomAccessFile, hook: NativeTrapHook): Boolean {
    val len = hook.expected.size
    if (hook.offset + len > raf.length()) return false
    val buf = ByteArray(len)
    raf.seek(hook.offset)
    raf.readFully(buf)
    if (buf.contentEquals(hook.replacement)) return true
    if (!buf.contentEquals(hook.expected)) return false
    raf.seek(hook.offset)
    raf.write(hook.replacement)
    val verify = ByteArray(len)
    raf.seek(hook.offset)
    raf.readFully(verify)
    if (!verify.contentEquals(hook.replacement)) {
        println("[Brave Compatibility] WARNING: read-back mismatch at offset ${hook.offset} (${hook.description}); not counted.")
        return false
    }
    return true
}

private fun patchNativeTrapHooks(soFile: File): Int {
    if (!soFile.exists() || !soFile.isFile) return 0
    var patched = 0
    RandomAccessFile(soFile, "rw").use { raf ->
        for (hook in LIBCHROME_TRAP_HOOKS) {
            if (applyTrapHook(raf, hook)) {
                patched++
            }
        }
    }
    return patched
}

internal val braveBtiCompatibilityPatch = rawResourcePatch {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val libDir = get("lib/arm64-v8a")
        if (!libDir.exists() || !libDir.isDirectory) {
            println("[Brave Compatibility] Skipped: lib/arm64-v8a not found.")
            return@execute
        }

        val soFiles = libDir.listFiles { file -> file.isFile && file.extension == "so" } ?: emptyArray()
        var neutralizedCount = 0
        var unverifiedCount = 0
        for (soFile in soFiles) {
            RandomAccessFile(soFile, "rw").use { raf ->
                when (patchBtiInElf(raf)) {
                    1 -> neutralizedCount++
                    -1 -> unverifiedCount++
                }
            }
        }

        if (neutralizedCount > 0) {
            println("[Brave Compatibility] Neutralized BTI flag across $neutralizedCount ARM64 native binaries -> Branch Target Exception SIGILL prevented.")
        } else {
            println("[Brave Compatibility] BTI flags in ARM64 native binaries are already clean or absent.")
        }
        if (unverifiedCount > 0) {
            println("[Brave Compatibility] WARNING: $unverifiedCount binary(ies) had BTI writes that failed read-back verification and were left uncounted; check the offsets for this build.")
        }

        val chromeSo = File(libDir, "libchrome.so")
        if (chromeSo.exists()) {
            val hooksPatched = patchNativeTrapHooks(chromeSo)
            if (hooksPatched < LIBCHROME_TRAP_HOOKS.size) {
                throw PatchException("Incomplete trap coverage: only $hooksPatched of ${LIBCHROME_TRAP_HOOKS.size} constructor traps matched in ${chromeSo.name}")
            }
            println("[Brave Compatibility] Neutralized $hooksPatched ARMv8.0/GSI illegal opcode trap(s) in libchrome.so -> SIGILL prevented.")
        }
    }
}

internal val braveNativeExtractionPatch = resourcePatch {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Brave Compatibility] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        document(manifestFile.absolutePath).use { doc ->
            val appElements = doc.getElementsByTagName("application")
            if (appElements.length > 0) {
                val app = appElements.item(0) as? Element
                app?.setAttribute("android:extractNativeLibs", "true")
            }
        }
        println("[Brave Compatibility] Enforced android:extractNativeLibs=true in AndroidManifest.xml")
    }
}

@Suppress("unused")
val bravePerformanceOptimizationPatch = bytecodePatch(
    name = "Faster Startup",
    description = "Speeds up launch by disabling carrier customizations.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)
    dependsOn(braveNativeExtractionPatch, braveBtiCompatibilityPatch)

    execute {
        // Neutralize PartnerBrowserCustomizations.initializeAsync(Context).
        // Immediately marks the component as initialized (Boolean.TRUE) and returns,
        // avoiding main-thread SharedPreferences reads, background ThreadPool tasks,
        // ContentResolver queries, and 10-second timeout task scheduling.
        val fp = Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;",
            returnType = "V",
            parameters = listOf("Landroid/content/Context;"),
            strings = listOf(
                "Chrome.Homepage.PartnerCustomizedDefaultGurl",
                "Chrome.Homepage.PartnerCustomizedDefaultUri",
            ),
        )
        val boolField = fp.originalClassDef.fields.firstOrNull { it.type == "Ljava/lang/Boolean;" }?.name ?: "b"
        fp.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                iput-object v0, p0, Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;->$boolField:Ljava/lang/Boolean;
                return-void
            """,
        )

        val targetClass = fp.originalClassDef.type.substringAfterLast('/').removeSuffix(";")
        println("[Faster Startup] Neutralized async OEM initialization in $targetClass (field $boolField)")
    }
}
