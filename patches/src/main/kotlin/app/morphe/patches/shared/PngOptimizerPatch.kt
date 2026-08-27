package app.morphe.patches.shared

import app.morphe.patcher.patch.rawResourcePatch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater

private val PNG_MAGIC = byteArrayOf(
    0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(),
    0x0D, 0x0A, 0x1A, 0x0A,
)

// Non-rendering informational chunks that are safe to drop on Android
private val STRIPPABLE_CHUNKS = setOf(
    "tEXt", "zTXt", "iTXt", "tIME", "pHYs", "hIST", "sPLT",
)

private class Chunk(val type: String, val data: ByteArray)

private fun readInt(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0xFF) shl 24) or
    ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
    ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
    (bytes[offset + 3].toInt() and 0xFF)

private fun writeInt(out: ByteArrayOutputStream, value: Int) {
    out.write((value ushr 24) and 0xFF)
    out.write((value ushr 16) and 0xFF)
    out.write((value ushr 8) and 0xFF)
    out.write(value and 0xFF)
}

private fun parsePngChunks(bytes: ByteArray): List<Chunk>? {
    if (bytes.size < 8 || !PNG_MAGIC.contentEquals(bytes.copyOfRange(0, 8))) return null

    val chunks = mutableListOf<Chunk>()
    var offset = 8
    while (offset + 12 <= bytes.size) {
        val length = readInt(bytes, offset)
        if (length < 0) return null

        val type = String(bytes, offset + 4, 4, Charsets.US_ASCII)
        val dataStart = offset + 8
        val dataEnd = dataStart + length
        if (dataEnd + 4 > bytes.size) return null

        val storedCrc = readInt(bytes, dataEnd)
        val crcCalculator = CRC32()
        crcCalculator.update(bytes, offset + 4, 4 + length)
        if (storedCrc != crcCalculator.value.toInt()) {
            return null // CRC mismatch / corrupted chunk
        }

        chunks.add(Chunk(type, bytes.copyOfRange(dataStart, dataEnd)))
        offset = dataEnd + 4
    }
    return chunks
}

private fun writePngChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
    writeInt(out, data.size)
    val headerAndData = ByteArrayOutputStream(4 + data.size).use { baos ->
        baos.write(type.toByteArray(Charsets.US_ASCII))
        baos.write(data)
        baos.toByteArray()
    }
    out.write(headerAndData)
    val crc = CRC32().apply { update(headerAndData) }.value.toInt()
    writeInt(out, crc)
}

private fun decompressData(data: ByteArray): ByteArray {
    val inflater = Inflater()
    inflater.setInput(data)
    return ByteArrayOutputStream(data.size * 3).use { out ->
        val buffer = ByteArray(8192)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
            out.write(buffer, 0, count)
        }
        inflater.end()
        out.toByteArray()
    }
}

private fun recompressData(data: ByteArray): ByteArray {
    val deflater = Deflater(Deflater.BEST_COMPRESSION, false)
    deflater.setInput(data)
    deflater.finish()
    return ByteArrayOutputStream(data.size).use { out ->
        val buffer = ByteArray(8192)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            out.write(buffer, 0, count)
        }
        deflater.end()
        out.toByteArray()
    }
}

private fun optimizePngBytes(original: ByteArray): ByteArray? {
    val chunks = parsePngChunks(original) ?: return null

    val rawIdat = ByteArrayOutputStream().use { out ->
        chunks.filter { it.type == "IDAT" }.forEach { out.write(it.data) }
        out.toByteArray()
    }
    if (rawIdat.isEmpty()) return null

    val decompressed = try {
        decompressData(rawIdat)
    } catch (_: Exception) {
        return null
    }

    val recompressed = recompressData(decompressed)

    val optimized = ByteArrayOutputStream(original.size).use { baos ->
        baos.write(PNG_MAGIC)
        var idatWritten = false

        for (chunk in chunks) {
            when {
                chunk.type == "IDAT" -> {
                    if (!idatWritten) {
                        writePngChunk(baos, "IDAT", recompressed)
                        idatWritten = true
                    }
                }
                chunk.type in STRIPPABLE_CHUNKS -> Unit // Drop non-rendering metadata
                else -> writePngChunk(baos, chunk.type, chunk.data) // Retain IHDR, PLTE, 9-patch npTc/npLc, IEND
            }
        }
        baos.toByteArray()
    }

    return if (optimized.size < original.size) optimized else null
}

val pngOptimizerPatch = rawResourcePatch(
    name = "PNG Asset Optimizer",
    description = "Losslessly recompresses PNG assets with maximum zlib compression and strips non-rendering metadata chunks (pHYs, tEXt, tIME) while preserving 9-patch structures and pixel accuracy.",
    default = false,
) {
    compatibleWith(
        Constants.COMPATIBILITY_BRAVE,
        Constants.COMPATIBILITY_GBOARD,
        Constants.COMPATIBILITY_VIVALDI,
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

        val pngFiles = rootDirs.flatMap { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.extension.equals("png", ignoreCase = true) && it.length() in 512..15_000_000 }
                .toList()
        }

        if (pngFiles.isEmpty()) return@execute

        val optimizedCount = AtomicInteger(0)
        val alreadyOptimalCount = AtomicInteger(0)
        val savedBytes = AtomicLong(0L)

        pngFiles.parallelStream().forEach { file ->
            try {
                val originalBytes = file.readBytes()
                val optimizedBytes = optimizePngBytes(originalBytes)
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
        println("[PNG Asset Optimizer] Recompressed ${optimizedCount.get()} / ${pngFiles.size} PNGs (${alreadyOptimalCount.get()} already optimal) -> Saved $savedFormatted")
    }
}
