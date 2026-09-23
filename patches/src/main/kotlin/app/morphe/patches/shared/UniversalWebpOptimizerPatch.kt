package app.morphe.patches.shared

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.rawResourcePatch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val RIFF_MAGIC = byteArrayOf('R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte())
private val WEBP_MAGIC = byteArrayOf('W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte())

private class WebpChunk(val type: String, val data: ByteArray)

private fun readIntLE(bytes: ByteArray, offset: Int): Int =
    (bytes[offset].toInt() and 0xFF) or
    ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
    ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
    ((bytes[offset + 3].toInt() and 0xFF) shl 24)

private fun writeIntLE(out: ByteArrayOutputStream, value: Int) {
    out.write(value and 0xFF)
    out.write((value ushr 8) and 0xFF)
    out.write((value ushr 16) and 0xFF)
    out.write((value ushr 24) and 0xFF)
}

private fun isValidWebpHeader(bytes: ByteArray): Boolean {
    if (bytes.size < 12) return false
    for (i in 0 until 4) {
        if (bytes[i] != RIFF_MAGIC[i]) return false
        if (bytes[i + 8] != WEBP_MAGIC[i]) return false
    }
    val riffSize = readIntLE(bytes, 4)
    if (riffSize < 4 || bytes.size < riffSize + 8) return false
    return true
}

private fun parseWebpChunks(bytes: ByteArray): List<WebpChunk>? {
    if (!isValidWebpHeader(bytes)) return null

    val chunks = mutableListOf<WebpChunk>()
    var offset = 12

    while (offset + 8 <= bytes.size) {
        val type = String(bytes, offset, 4, Charsets.US_ASCII)
        val length = readIntLE(bytes, offset + 4)
        if (length < 0) return null

        val dataStart = offset + 8
        val dataEnd = dataStart + length
        if (dataEnd > bytes.size) return null

        chunks.add(WebpChunk(type, bytes.copyOfRange(dataStart, dataEnd)))
        offset = dataEnd + (length % 2)
    }

    // Must cleanly consume all bytes to prevent corrupting truncated or non-spec files
    if (offset != bytes.size) return null

    return chunks
}

private fun writeWebpChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
    out.write(type.toByteArray(Charsets.US_ASCII))
    writeIntLE(out, data.size)
    out.write(data)
    if (data.size % 2 != 0) {
        out.write(0)
    }
}

private fun updateVp8xFlags(
    originalData: ByteArray,
    removedExif: Boolean,
    removedXmp: Boolean,
    removedIcc: Boolean,
): ByteArray {
    val updated = originalData.copyOf()
    if (updated.size >= 10) {
        var flags = updated[0].toInt() and 0xFF
        // Bit masks according to RFC 9649 / libwebp WebPFeatureFlags:
        // EXIF_FLAG = 0x08
        // XMP_FLAG  = 0x04
        // ICCP_FLAG = 0x20
        if (removedExif) flags = flags and 0x08.inv()
        if (removedXmp) flags = flags and 0x04.inv()
        if (removedIcc) flags = flags and 0x20.inv()
        updated[0] = flags.toByte()
    }
    return updated
}

private fun optimizeWebpBytes(
    original: ByteArray,
    stripExif: Boolean,
    stripXmp: Boolean,
    stripIcc: Boolean,
): ByteArray? {
    val chunks = parseWebpChunks(original) ?: return null

    val hasExif = chunks.any { it.type == "EXIF" } && stripExif
    val hasXmp = chunks.any { it.type == "XMP " } && stripXmp
    val hasIcc = chunks.any { it.type == "ICCP" } && stripIcc

    if (!hasExif && !hasXmp && !hasIcc) return null

    val chunkBytesStream = ByteArrayOutputStream()
    for (chunk in chunks) {
        when {
            chunk.type == "EXIF" && stripExif -> Unit
            chunk.type == "XMP " && stripXmp -> Unit
            chunk.type == "ICCP" && stripIcc -> Unit
            chunk.type == "VP8X" -> {
                val updatedData = updateVp8xFlags(chunk.data, hasExif, hasXmp, hasIcc)
                writeWebpChunk(chunkBytesStream, "VP8X", updatedData)
            }
            else -> writeWebpChunk(chunkBytesStream, chunk.type, chunk.data)
        }
    }

    val chunkBytes = chunkBytesStream.toByteArray()
    val finalStream = ByteArrayOutputStream(12 + chunkBytes.size)
    finalStream.write(RIFF_MAGIC)
    writeIntLE(finalStream, 4 + chunkBytes.size)
    finalStream.write(WEBP_MAGIC)
    finalStream.write(chunkBytes)

    val optimized = finalStream.toByteArray()
    return if (optimized.size < original.size) optimized else null
}

@Suppress("unused")
val universalWebpOptimizerPatch = rawResourcePatch(
    name = "Universal WebP Asset Optimizer",
    description = "Losslessly strips non-rendering metadata and ancillary chunks (EXIF, XMP, ICCP) from WebP assets across res/ and assets/ to reduce APK size.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val stripExifOption by booleanOption(
        key = "stripExif",
        default = true,
        title = "Strip EXIF Metadata",
        description = "Remove camera parameters, geolocation, and creator metadata chunks from WebP images.",
        required = false,
    )

    val stripXmpOption by booleanOption(
        key = "stripXmp",
        default = true,
        title = "Strip XMP Metadata",
        description = "Remove Adobe XMP XML metadata chunks from WebP images.",
        required = false,
    )

    val stripIccOption by booleanOption(
        key = "stripIcc",
        default = false,
        title = "Strip ICC Color Profiles",
        description = "Remove ICCP color profile chunks. Default is false to preserve color profile fidelity on wide-gamut displays.",
        required = false,
    )

    execute {
        val rootDirs = mutableListOf<File>()

        try {
            val assetsDir = get("assets")
            if (assetsDir.exists() && assetsDir.isDirectory) rootDirs.add(assetsDir)
        } catch (_: Throwable) {}

        try {
            val resDir = get("res")
            if (resDir.exists() && resDir.isDirectory) rootDirs.add(resDir)
        } catch (_: Throwable) {}

        if (rootDirs.isEmpty()) {
            println("[Universal WebP Asset Optimizer] Neither res/ nor assets/ directory found - skipping safely.")
            return@execute
        }

        val webpFiles = rootDirs.flatMap { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.extension.equals("webp", ignoreCase = true) && it.length() in 20..30_000_000 }
                .toList()
        }

        if (webpFiles.isEmpty()) {
            println("[Universal WebP Asset Optimizer] No candidate WebP assets found in res/ or assets/ - skipping.")
            return@execute
        }

        val shouldStripExif = stripExifOption ?: true
        val shouldStripXmp = stripXmpOption ?: true
        val shouldStripIcc = stripIccOption ?: false

        val optimizedCount = AtomicInteger(0)
        val alreadyOptimalCount = AtomicInteger(0)
        val savedBytes = AtomicLong(0L)

        webpFiles.parallelStream().forEach { file ->
            try {
                val originalBytes = file.readBytes()
                val optimizedBytes = optimizeWebpBytes(
                    original = originalBytes,
                    stripExif = shouldStripExif,
                    stripXmp = shouldStripXmp,
                    stripIcc = shouldStripIcc,
                )
                if (optimizedBytes != null && optimizedBytes.size < originalBytes.size) {
                    file.writeBytes(optimizedBytes)
                    optimizedCount.incrementAndGet()
                    savedBytes.addAndGet((originalBytes.size - optimizedBytes.size).toLong())
                } else {
                    alreadyOptimalCount.incrementAndGet()
                }
            } catch (_: Exception) {
                // Keep original file intact on any I/O or parsing exception
            }
        }

        val savedFormatted = LocaleUtils.formatBytes(savedBytes.get())
        println("[Universal WebP Asset Optimizer] Stripped metadata from ${optimizedCount.get()} / ${webpFiles.size} WebP assets (${alreadyOptimalCount.get()} already optimal) -> Saved $savedFormatted")
    }
}
