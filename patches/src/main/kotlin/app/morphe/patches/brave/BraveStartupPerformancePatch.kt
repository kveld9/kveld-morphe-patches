package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element
import java.io.RandomAccessFile

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

private fun clearBtiInGnuProperty(raf: RandomAccessFile, gnuPropertyOffset: Long): Boolean {
    raf.seek(gnuPropertyOffset)
    val nameSz = raf.readIntLe()
    val descSz = raf.readIntLe()
    val nType = raf.readIntLe()
    if (nType != NT_GNU_PROPERTY_TYPE_0 || nameSz < 4) return false

    val nameBytes = ByteArray(nameSz)
    raf.readFully(nameBytes)
    val name = String(nameBytes).trimEnd('\u0000')
    if (name != "GNU") return false

    val descOffset = gnuPropertyOffset + 12 + ((nameSz + 3) and -4)
    raf.seek(descOffset)
    val prType = raf.readIntLe().toLong() and 0xFFFFFFFFL
    val prDataSz = raf.readIntLe()
    if (prType != GNU_PROPERTY_AARCH64_FEATURE_1_AND || prDataSz < 4) return false

    val flagsOffset = descOffset + 8
    raf.seek(flagsOffset)
    val flags = raf.readIntLe()
    if ((flags and GNU_PROPERTY_AARCH64_FEATURE_1_BTI) == 0) return false

    val clearedFlags = flags and GNU_PROPERTY_AARCH64_FEATURE_1_BTI.inv()
    raf.seek(flagsOffset)
    raf.writeIntLe(clearedFlags)
    return true
}

private fun patchBtiInElf(raf: RandomAccessFile): Boolean {
    if (raf.length() < 0x40) return false
    val magic = ByteArray(4)
    raf.seek(0)
    raf.readFully(magic)
    val expectedMagic = byteArrayOf(0x7f, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())
    if (!magic.contentEquals(expectedMagic)) return false
    if (raf.readByte().toInt() != 2) return false // 64-bit ELF

    raf.seek(0x20)
    val ePhOff = raf.readLongLe()
    raf.seek(0x36)
    val ePhEntSize = raf.readShortLe().toInt()
    val ePhNum = raf.readShortLe().toInt()
    if (ePhOff <= 0 || ePhEntSize < 56 || ePhNum <= 0) return false

    val gnuPropOffset = findGnuPropertyOffset(raf, ePhOff, ePhEntSize, ePhNum)
    if (gnuPropOffset < 0) return false

    return clearBtiInGnuProperty(raf, gnuPropOffset)
}

internal val braveBtiCompatibilityPatch = rawResourcePatch {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val soFile = get("lib/arm64-v8a/libchrome.so")
        if (!soFile.exists() || !soFile.isFile) {
            println("[Brave Compatibility] Skipped BTI fix: lib/arm64-v8a/libchrome.so not found.")
            return@execute
        }

        RandomAccessFile(soFile, "rw").use { raf ->
            val modified = patchBtiInElf(raf)
            if (modified) {
                println("[Brave Compatibility] Neutralized BTI flag in lib/arm64-v8a/libchrome.so -> Branch Target Exception SIGILL prevented.")
            } else {
                println("[Brave Compatibility] BTI flag in lib/arm64-v8a/libchrome.so is already clean or absent.")
            }
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
    name = "Brave Startup Performance Optimization",
    description = "Optimizes startup time and eliminates background CPU/disk overhead by disabling unused OEM carrier partner customizations.",
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
        println("[Startup Performance] Neutralized async OEM initialization in $targetClass (field $boolField)")
    }
}
