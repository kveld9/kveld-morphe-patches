package app.morphe.patches.hevy

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val HERMES_MAGIC = 0x1f1903c103bc1fc6L
private const val HERMES_VERSION_HBC96 = 96

// Target properties and functions to unlock Pro capabilities
private val TARGET_PRO_PROPERTIES = listOf(
    "isPro",
    "isPaying",
    "isInGracePeriod",
    "isWithinProOfflineGracePeriod",
)

// Hermes bytecode instructions:
//   78 00  LoadConstTrue r0
//   5C 00  Ret r0
private val FORCE_TRUE_PROLOGUE = byteArrayOf(0x78.toByte(), 0x00, 0x5C.toByte(), 0x00)

private data class HermesHeader(
    val fileLength: Int,
    val functionCount: Int,
    val stringKindCount: Int,
    val identifierCount: Int,
    val stringCount: Int,
    val overflowStringCount: Int,
    val stringStorageSize: Int,
)

private data class HermesTableOffsets(
    val functionHeadersPos: Int,
    val smallStringTablePos: Int,
    val overflowStringTablePos: Int,
    val stringStoragePos: Int,
)

private data class HermesFunction(
    val index: Int,
    val offset: Int,
    val size: Int,
    val name: String,
)

private data class StringEntry(
    val offset: Int,
    val length: Int,
)

private fun align4(pos: Int): Int {
    val rem = pos % 4
    return if (rem == 0) pos else pos + (4 - rem)
}

private fun parseHermesHeader(buffer: ByteBuffer): HermesHeader? {
    if (buffer.capacity() < 128) return null

    val magic = buffer.getLong(0)
    val version = buffer.getInt(8)
    if (magic != HERMES_MAGIC || version != HERMES_VERSION_HBC96) {
        return null
    }

    return HermesHeader(
        fileLength = buffer.getInt(32),
        functionCount = buffer.getInt(40),
        stringKindCount = buffer.getInt(44),
        identifierCount = buffer.getInt(48),
        stringCount = buffer.getInt(52),
        overflowStringCount = buffer.getInt(56),
        stringStorageSize = buffer.getInt(60),
    )
}

private fun calculateTableOffsets(header: HermesHeader): HermesTableOffsets {
    val funcHeadersPos = 128
    var pos = funcHeadersPos + header.functionCount * 16
    pos = align4(pos + header.stringKindCount * 4)
    pos = align4(pos + header.identifierCount * 4)

    val smallStrPos = pos
    pos = align4(pos + header.stringCount * 4)

    val overflowStrPos = pos
    pos = align4(pos + header.overflowStringCount * 8)

    val storagePos = pos
    return HermesTableOffsets(
        functionHeadersPos = funcHeadersPos,
        smallStringTablePos = smallStrPos,
        overflowStringTablePos = overflowStrPos,
        stringStoragePos = storagePos,
    )
}

private fun getStringEntry(
    buffer: ByteBuffer,
    index: Int,
    header: HermesHeader,
    offsets: HermesTableOffsets,
): StringEntry {
    if (index >= header.stringCount) return StringEntry(0, 0)
    val entry = buffer.getInt(offsets.smallStringTablePos + index * 4)
    var strOffset = (entry ushr 1) and 0x007FFFFF
    val strLength = (entry ushr 24) and 0xFF
    if (strLength == 0xFF) {
        val overflowPos = offsets.overflowStringTablePos + strOffset * 8
        strOffset = buffer.getInt(overflowPos)
        val overflowLength = buffer.getInt(overflowPos + 4)
        return StringEntry(strOffset, overflowLength)
    }
    return StringEntry(strOffset, strLength)
}

private fun findExactStringId(
    buffer: ByteBuffer,
    bundleBytes: ByteArray,
    targetName: String,
    header: HermesHeader,
    offsets: HermesTableOffsets,
): Int? {
    val targetBytes = targetName.toByteArray(Charsets.UTF_8)
    val tLen = targetBytes.size

    for (i in 0 until header.stringCount) {
        val entry = getStringEntry(buffer, i, header, offsets)
        if (entry.length != tLen) continue

        val start = offsets.stringStoragePos + entry.offset
        if (start + tLen > bundleBytes.size) continue

        var matches = true
        for (j in 0 until tLen) {
            if (bundleBytes[start + j] != targetBytes[j]) {
                matches = false
                break
            }
        }
        if (matches) return i
    }
    return null
}

private fun getFunctionName(
    buffer: ByteBuffer,
    bundleBytes: ByteArray,
    nameId: Int,
    header: HermesHeader,
    offsets: HermesTableOffsets,
): String {
    val entry = getStringEntry(buffer, nameId, header, offsets)
    if (entry.length == 0) return ""
    val start = offsets.stringStoragePos + entry.offset
    if (start + entry.length > bundleBytes.size) return ""
    return String(bundleBytes, start, entry.length, Charsets.UTF_8)
}

private fun getHermesFunction(
    buffer: ByteBuffer,
    bundleBytes: ByteArray,
    index: Int,
    header: HermesHeader,
    offsets: HermesTableOffsets,
): HermesFunction {
    val base = offsets.functionHeadersPos + index * 16
    val w0 = buffer.getInt(base)
    val w1 = buffer.getInt(base + 4)
    val offset = w0 and 0x01FFFFFF
    val size = w1 and 0x7FFF
    val nameId = (w1 ushr 15) and 0x1FFFF
    val name = getFunctionName(buffer, bundleBytes, nameId, header, offsets)
    return HermesFunction(index = index, offset = offset, size = size, name = name)
}

private fun patchFunctionPrologue(bundleBytes: ByteArray, function: HermesFunction): Boolean {
    if (function.size < FORCE_TRUE_PROLOGUE.size || function.offset + FORCE_TRUE_PROLOGUE.size > bundleBytes.size) {
        return false
    }

    val isAlreadyPatched = FORCE_TRUE_PROLOGUE.indices.all { i ->
        bundleBytes[function.offset + i] == FORCE_TRUE_PROLOGUE[i]
    }
    if (isAlreadyPatched) return false

    FORCE_TRUE_PROLOGUE.forEachIndexed { i, b ->
        bundleBytes[function.offset + i] = b
    }
    return true
}

@Suppress("unused")
val hevyUnlockProPatch = rawResourcePatch(
    name = "Unlock Pro",
    description = "Unlocks local Hevy Pro capabilities (unlimited workout routines, routine folders, advanced graphs, and local analytics) by dynamically enabling Pro getters in Hermes Bytecode (HBC96).",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_HEVY)

    execute {
        val bundleFile = get("assets/index.android.bundle")
        if (!bundleFile.exists() || !bundleFile.isFile) {
            println("[Unlock Pro] assets/index.android.bundle not found - skipping Hermes unlock.")
            return@execute
        }

        val bundleBytes = bundleFile.readBytes()
        val buffer = ByteBuffer.wrap(bundleBytes).order(ByteOrder.LITTLE_ENDIAN)

        val header = parseHermesHeader(buffer)
        if (header == null) {
            println("[Unlock Pro] Bundle is not a valid Hermes HBC96 binary - skipping.")
            return@execute
        }

        val offsets = calculateTableOffsets(header)
        val targetsToPatch = mutableMapOf<Int, String>()

        for (prop in TARGET_PRO_PROPERTIES) {
            val stringId = findExactStringId(buffer, bundleBytes, prop, header, offsets) ?: continue

            // 1. Scan for functions named exactly as the property
            for (i in 0 until header.functionCount) {
                val fn = getHermesFunction(buffer, bundleBytes, i, header, offsets)
                if (fn.name == prop) {
                    targetsToPatch[i] = "named '$prop'"
                }
            }

            // 2. Scan for property getter closures in class definitions
            if (stringId < 65536) {
                val low = (stringId and 0xFF).toByte()
                val high = ((stringId ushr 8) and 0xFF).toByte()

                for (i in 0 until header.functionCount) {
                    val fn = getHermesFunction(buffer, bundleBytes, i, header, offsets)
                    val end = fn.offset + fn.size - 13
                    if (end <= fn.offset || fn.offset + fn.size > bundleBytes.size) continue

                    for (k in fn.offset..end) {
                        val op = bundleBytes[k]
                        if ((op == 0x73.toByte() || op == 0x7a.toByte()) &&
                            bundleBytes[k + 2] == low &&
                            bundleBytes[k + 3] == high &&
                            bundleBytes[k + 4] == 0x3f.toByte()
                        ) {
                            val funcIdx = (bundleBytes[k + 11].toInt() and 0xFF) or
                                ((bundleBytes[k + 12].toInt() and 0xFF) shl 8)

                            if (funcIdx < header.functionCount) {
                                val getterFn = getHermesFunction(buffer, bundleBytes, funcIdx, header, offsets)
                                if (getterFn.name == "get" || getterFn.name.isEmpty()) {
                                    targetsToPatch[funcIdx] = "getter for '$prop'"
                                }
                            }
                        }
                    }
                }
            }
        }

        if (targetsToPatch.isEmpty()) {
            println("[Unlock Pro] No Pro getter or function targets found in Hermes bundle - skipping.")
            return@execute
        }

        var patchedCount = 0
        for ((funcIdx, reason) in targetsToPatch) {
            val fn = getHermesFunction(buffer, bundleBytes, funcIdx, header, offsets)
            if (patchFunctionPrologue(bundleBytes, fn)) {
                val offsetHex = "0x" + fn.offset.toString(16).uppercase()
                println("[Unlock Pro] Patched $reason (Func #$funcIdx at $offsetHex, size: ${fn.size}B) with LoadConstTrue.")
                patchedCount++
            }
        }

        if (patchedCount > 0) {
            bundleFile.writeBytes(bundleBytes)
            println("[Unlock Pro] Successfully unlocked Hevy Pro across $patchedCount functions in assets/index.android.bundle.")
        } else {
            println("[Unlock Pro] All ${targetsToPatch.size} Pro targets are already patched.")
        }
    }
}
