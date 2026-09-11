package app.morphe.patches.hevy

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val HERMES_MAGIC = 0x1f1903c103bc1fc6L
private const val HERMES_VERSION_HBC96 = 96
private const val TARGET_FUNCTION_NAME = "isWithinProOfflineGracePeriod"

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

private fun indexOfSubarray(data: ByteArray, target: ByteArray, start: Int, end: Int): Int {
    if (target.isEmpty() || end - start < target.size) return -1
    val max = end - target.size
    outer@ for (i in start..max) {
        for (j in target.indices) {
            if (data[i + j] != target[j]) continue@outer
        }
        return i
    }
    return -1
}

private fun findStringId(
    buffer: ByteBuffer,
    bundleBytes: ByteArray,
    targetName: String,
    header: HermesHeader,
    offsets: HermesTableOffsets,
): Int? {
    val targetBytes = targetName.toByteArray(Charsets.UTF_8)
    val storageStart = offsets.stringStoragePos
    val storageEnd = storageStart + header.stringStorageSize
    val foundPos = indexOfSubarray(bundleBytes, targetBytes, storageStart, storageEnd)
    if (foundPos == -1) return null

    val targetRelativeOffset = foundPos - storageStart
    for (i in 0 until header.stringCount) {
        val entry = buffer.getInt(offsets.smallStringTablePos + i * 4)
        var strOffset = (entry ushr 1) and 0x007FFFFF
        val strLength = (entry ushr 24) and 0xFF
        if (strLength == 0xFF) {
            strOffset = buffer.getInt(offsets.overflowStringTablePos + strOffset * 8)
        }
        if (strOffset == targetRelativeOffset) {
            return i
        }
    }
    return null
}

private fun findFunctionByNameId(
    buffer: ByteBuffer,
    stringId: Int,
    header: HermesHeader,
    offsets: HermesTableOffsets,
): HermesFunction? {
    for (i in 0 until header.functionCount) {
        val headerBase = offsets.functionHeadersPos + i * 16
        val w0 = buffer.getInt(headerBase)
        val w1 = buffer.getInt(headerBase + 4)
        val funcNameId = (w1 ushr 15) and 0x1FFFF
        if (funcNameId == stringId) {
            val offset = w0 and 0x01FFFFFF
            val size = w1 and 0x7FFF
            return HermesFunction(index = i, offset = offset, size = size)
        }
    }
    return null
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
    description = "Unlocks local Hevy Pro capabilities (unlimited workout routines, routine folders, advanced graphs, and local analytics) by dynamically enabling the offline-Pro grace period in Hermes Bytecode (HBC96).",
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
        val stringId = findStringId(buffer, bundleBytes, TARGET_FUNCTION_NAME, header, offsets)
        if (stringId == null) {
            println("[Unlock Pro] Target string '$TARGET_FUNCTION_NAME' not found in Hermes string storage.")
            return@execute
        }

        val targetFunction = findFunctionByNameId(buffer, stringId, header, offsets)
        if (targetFunction == null) {
            println("[Unlock Pro] Function with string ID #$stringId ($TARGET_FUNCTION_NAME) not found in function table.")
            return@execute
        }

        val applied = patchFunctionPrologue(bundleBytes, targetFunction)
        if (!applied) {
            println("[Unlock Pro] Function '$TARGET_FUNCTION_NAME' (Function #${targetFunction.index}) is already patched or invalid size.")
            return@execute
        }

        bundleFile.writeBytes(bundleBytes)
        val offsetHex = "0x" + targetFunction.offset.toString(16).uppercase()
        println("[Unlock Pro] Successfully patched '$TARGET_FUNCTION_NAME' (Function #${targetFunction.index} at $offsetHex, size: ${targetFunction.size}B) with LoadConstTrue prologue.")
    }
}
