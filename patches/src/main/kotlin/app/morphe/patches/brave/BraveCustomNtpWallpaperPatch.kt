package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.ImageSize
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.imageOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream

// ════════════════════════════════════════════════════════════════════════════════
// 1. CONFIGURATION & CONSTANTS
// ════════════════════════════════════════════════════════════════════════════════

private const val NTP_WALLPAPER_RESOURCE_NAME = "morphe_custom_ntp_wallpaper"
private const val NTP_BACKGROUND_IMAGES_BRIDGE =
    "Lorg/chromium/chrome/browser/ntp_background_images/NTPBackgroundImagesBridge;"
private const val BACKGROUND_IMAGE_MODEL =
    "Lorg/chromium/chrome/browser/ntp_background_images/model/BackgroundImage;"
private const val WALLPAPER_MODEL =
    "Lorg/chromium/chrome/browser/ntp_background_images/model/Wallpaper;"

private const val MIN_WALLPAPER_DIMENSION = 480
private const val MAX_WALLPAPER_DIMENSION = 8192
private const val MAX_WALLPAPER_FILE_SIZE = 8L * 1024L * 1024L

private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
)
private val JPEG_SOI = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
private val IHDR_CHUNK = byteArrayOf(0x49, 0x48, 0x44, 0x52)

private enum class ImageFormat(val extension: String) {
    PNG("png"),
    JPEG("jpg"),
}

private data class WallpaperDimensions(
    val width: Int,
    val height: Int,
)

// ════════════════════════════════════════════════════════════════════════════════
// 2. FINGERPRINTS
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Ambient Java catalog accessor: static no-arg factory returning BackgroundImage,
 * owning class <clinit> IPUTs a drawable resource id into BackgroundImage.
 * Unique anchor independent of obfuscated class names across Brave releases.
 */
private object AmbientCatalogAccessorFingerprint : Fingerprint(
    returnType = BACKGROUND_IMAGE_MODEL,
    parameters = emptyList(),
    custom = { _, classDef ->
        classDef.methods.any { method ->
            if (method.name != "<clinit>") return@any false
            val impl = method.implementation ?: return@any false
            impl.instructions.any { ins ->
                val field = (ins as? ReferenceInstruction)?.reference as? FieldReference
                field != null &&
                    field.definingClass == BACKGROUND_IMAGE_MODEL &&
                    field.type == "I"
            }
        }
    },
)

private object CreateWallpaperFingerprint : Fingerprint(
    definingClass = NTP_BACKGROUND_IMAGES_BRIDGE,
    name = "createWallpaper",
    returnType = BACKGROUND_IMAGE_MODEL,
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
)

private object CreateBrandedWallpaperFingerprint : Fingerprint(
    definingClass = NTP_BACKGROUND_IMAGES_BRIDGE,
    name = "createBrandedWallpaper",
    returnType = WALLPAPER_MODEL,
    parameters = listOf(
        "Ljava/lang/String;",
        "I",
        "I",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "I",
    ),
)

// ════════════════════════════════════════════════════════════════════════════════
// 3. SMALI TEMPLATES & CODE GENERATORS
// ════════════════════════════════════════════════════════════════════════════════

private fun backgroundImageResourceIdField(classDef: ClassDef): String {
    val clinit = classDef.methods.firstOrNull { it.name == "<clinit>" && it.implementation != null }
        ?: throw PatchException("Ambient wallpaper catalog class has no <clinit>")
    val field = clinit.implementation!!.instructions
        .mapNotNull { (it as? ReferenceInstruction)?.reference as? FieldReference }
        .firstOrNull { it.definingClass == BACKGROUND_IMAGE_MODEL && it.type == "I" }
        ?: throw PatchException("BackgroundImage resource-id field not found in catalog <clinit>")
    return "${field.definingClass}->${field.name}:${field.type}"
}

private fun forceAmbientCatalogAccessorSmali(resourceIdField: String): String = """
    invoke-static {}, ${Constants.BRAVE_EXTENSION_CLASS}->getCustomNtpWallpaperDrawableId()I
    move-result v0
    new-instance v1, $BACKGROUND_IMAGE_MODEL
    invoke-direct {v1}, $BACKGROUND_IMAGE_MODEL-><init>()V
    iput v0, v1, $resourceIdField
    return-object v1
""".trimIndent()

private fun forceUseAmbientCatalogSmali(
    catalogClass: String,
    catalogMethod: String,
): String = """
    invoke-static {}, $catalogClass->$catalogMethod()$BACKGROUND_IMAGE_MODEL
    move-result-object p1
""".trimIndent()

private fun ntpWallpaperResourceUri(packageName: String): String =
    "android.resource://$packageName/drawable/$NTP_WALLPAPER_RESOURCE_NAME"

private fun forceCreateWallpaperParamsSmali(packageName: String): String = """
    const-string p0, "$NTP_WALLPAPER_RESOURCE_NAME"
    const-string p1, "${ntpWallpaperResourceUri(packageName)}"
    const-string p2, "Custom"
""".trimIndent()

private fun forceCreateBrandedWallpaperParamsSmali(packageName: String): String = """
    const-string p0, "$NTP_WALLPAPER_RESOURCE_NAME"
    const-string p3, "${ntpWallpaperResourceUri(packageName)}"
    move-object p4, p3
    move-object p6, p3
    move-object p7, p3
""".trimIndent()

// ════════════════════════════════════════════════════════════════════════════════
// 4. RESOURCE VALIDATION & DEPLOYMENT
// ════════════════════════════════════════════════════════════════════════════════

private fun isPngHeader(header: ByteArray, count: Int): Boolean =
    count >= 8 && header.contentEquals(PNG_SIGNATURE)

private fun isJpegHeader(header: ByteArray, count: Int): Boolean =
    count >= 2 && header[0] == JPEG_SOI[0] && header[1] == JPEG_SOI[1]

private fun detectImageFormat(file: File): ImageFormat? =
    runCatching {
        FileInputStream(file).use { stream ->
            val header = ByteArray(8)
            val count = stream.read(header)
            when {
                isPngHeader(header, count) -> ImageFormat.PNG
                isJpegHeader(header, count) -> ImageFormat.JPEG
                else -> null
            }
        }
    }.getOrNull()

private fun validateFileAccessibility(file: File) {
    if (!file.exists()) {
        throw PatchException("Custom NTP wallpaper file does not exist: ${file.absolutePath}")
    }
    if (!file.isFile || !file.canRead()) {
        throw PatchException("Custom NTP wallpaper file is not readable: ${file.absolutePath}")
    }
    if (file.length() > MAX_WALLPAPER_FILE_SIZE) {
        throw PatchException("Custom NTP wallpaper exceeds 8 MiB: ${file.absolutePath}")
    }
}

private fun validateDimensionBounds(dimensions: WallpaperDimensions) {
    val validWidth = dimensions.width in MIN_WALLPAPER_DIMENSION..MAX_WALLPAPER_DIMENSION
    val validHeight = dimensions.height in MIN_WALLPAPER_DIMENSION..MAX_WALLPAPER_DIMENSION
    if (!validWidth || !validHeight) {
        throw PatchException(
            "Custom NTP wallpaper dimensions must be between $MIN_WALLPAPER_DIMENSION and " +
                "$MAX_WALLPAPER_DIMENSION px on each side, received ${dimensions.width}x${dimensions.height}",
        )
    }
}

private fun readDimensionsForFormat(file: File, format: ImageFormat): WallpaperDimensions? =
    when (format) {
        ImageFormat.PNG -> readPngDimensions(file)
        ImageFormat.JPEG -> readJpegDimensions(file)
    }

private fun validateWallpaperFile(file: File): Pair<WallpaperDimensions, ImageFormat> {
    validateFileAccessibility(file)
    val format = detectImageFormat(file)
        ?: throw PatchException("Custom NTP wallpaper must be a valid PNG or JPG file: ${file.absolutePath}")
    val dimensions = readDimensionsForFormat(file, format)
        ?: throw PatchException("Custom NTP wallpaper has invalid or unreadable image dimensions: ${file.absolutePath}")
    validateDimensionBounds(dimensions)
    return dimensions to format
}

private fun parsePngIhdr(input: DataInputStream): WallpaperDimensions? {
    val headerLength = input.readInt()
    val headerType = ByteArray(4)
    input.readFully(headerType)
    if (headerLength != 13 || !headerType.contentEquals(IHDR_CHUNK)) return null

    val width = input.readInt()
    val height = input.readInt()
    if (width <= 0 || height <= 0) return null

    return WallpaperDimensions(width, height)
}

private fun readPngDimensions(file: File): WallpaperDimensions? =
    runCatching {
        DataInputStream(FileInputStream(file).buffered()).use { input ->
            val signature = ByteArray(8)
            input.readFully(signature)
            if (!signature.contentEquals(PNG_SIGNATURE)) return null
            parsePngIhdr(input)
        }
    }.getOrNull()

private fun isSofMarker(marker: Int): Boolean = when (marker) {
    0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7,
    0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF -> true
    else -> false
}

private fun isTerminalMarker(marker: Int): Boolean = when (marker) {
    0xD8, 0xD9, 0xDA -> true
    else -> false
}

private fun nextMarker(input: DataInputStream): Int {
    var b = input.readUnsignedByte()
    while (b != 0xFF) {
        b = input.readUnsignedByte()
    }
    while (b == 0xFF) {
        b = input.readUnsignedByte()
    }
    return b
}

private fun skipBytesFully(input: DataInputStream, totalBytes: Int) {
    var remaining = totalBytes.toLong()
    while (remaining > 0) {
        val skipped = input.skip(remaining)
        if (skipped > 0) {
            remaining -= skipped
        } else {
            input.readByte()
            remaining--
        }
    }
}

private fun readSofDimensions(input: DataInputStream): WallpaperDimensions? {
    val length = input.readUnsignedShort()
    if (length < 7) return null
    input.readUnsignedByte()
    val height = input.readUnsignedShort()
    val width = input.readUnsignedShort()
    if (width <= 0 || height <= 0) return null
    return WallpaperDimensions(width, height)
}

private fun skipSegment(input: DataInputStream): Boolean {
    val length = input.readUnsignedShort()
    if (length < 2) return false
    skipBytesFully(input, length - 2)
    return true
}

private fun findJpegDimensions(input: DataInputStream): WallpaperDimensions? {
    while (true) {
        val marker = nextMarker(input)
        if (isTerminalMarker(marker)) return null
        if (isSofMarker(marker)) return readSofDimensions(input)
        if (!skipSegment(input)) return null
    }
}

private fun readJpegDimensions(file: File): WallpaperDimensions? =
    runCatching {
        DataInputStream(FileInputStream(file).buffered()).use { input ->
            val soi = ByteArray(2)
            input.readFully(soi)
            if (!soi.contentEquals(JPEG_SOI)) return null
            findJpegDimensions(input)
        }
    }.getOrNull()

private fun installWallpaperResource(
    resourceDirectory: File,
    sourceFile: File,
    format: ImageFormat,
) {
    val drawableDirectory = resourceDirectory.resolve("drawable-nodpi").apply(File::mkdirs)
    drawableDirectory.listFiles { file -> file.name.startsWith(NTP_WALLPAPER_RESOURCE_NAME) }
        ?.forEach { it.delete() }
    sourceFile.copyTo(
        target = drawableDirectory.resolve("$NTP_WALLPAPER_RESOURCE_NAME.${format.extension}"),
        overwrite = true,
    )
    println("[Custom NTP Wallpaper] Installed $NTP_WALLPAPER_RESOURCE_NAME.${format.extension} into drawable-nodpi")
}

// ════════════════════════════════════════════════════════════════════════════════
// 5. RESOURCE PATCH
// ════════════════════════════════════════════════════════════════════════════════

private val braveCustomNtpWallpaperResourcePatch: ResourcePatch = resourcePatch(
    name = "Custom NTP wallpaper (Setup)",
    description = "Installs the patch-time wallpaper image into Brave resources.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val rawPath = runCatching { braveCustomNtpWallpaperPatch.options["customWallpaper"].value as? String }
            .getOrNull()
            ?.trim()
        if (rawPath.isNullOrBlank()) {
            throw PatchException(
                "Custom NTP wallpaper requires selecting a valid PNG or JPG file in the customWallpaper option (480-8192 px, max 8 MiB)",
            )
        }
        val sourceFile = File(rawPath)
        val (_, format) = validateWallpaperFile(sourceFile)
        installWallpaperResource(get("res"), sourceFile, format)
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// 6. BYTECODE PATCH
// ════════════════════════════════════════════════════════════════════════════════

@Suppress("unused")
val braveCustomNtpWallpaperPatch: BytecodePatch = bytecodePatch(
    name = "Custom NTP wallpaper",
    description = "Replaces the new tab page background with a custom wallpaper chosen at patch time. " +
        "For best results, select an image matching your device screen resolution.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    dependsOn(
        braveCustomNtpWallpaperResourcePatch,
        sharedExtensionPatch,
    )

    val customWallpaperPath by imageOption(
        key = "customWallpaper",
        title = "Custom NTP wallpaper",
        description = "Wallpaper image used as the Brave new tab page background. " +
            "Crop to your screen resolution before selecting (PNG or JPG, 480-8192 px, up to 8 MiB).",
        required = true,
        allowedExtensions = listOf("png", "jpg", "jpeg"),
        recommendedSize = ImageSize(1920, 1080),
        validator = { value -> value == null || value.trim().let { it.endsWith(".png", ignoreCase = true) || it.endsWith(".jpg", ignoreCase = true) || it.endsWith(".jpeg", ignoreCase = true) } },
    )

    execute {
        val rawPath = customWallpaperPath?.trim()
        if (rawPath.isNullOrBlank()) {
            throw PatchException(
                "Custom NTP wallpaper requires selecting a valid PNG or JPG file in the customWallpaper option (480-8192 px, max 8 MiB)",
            )
        }
        val sourceFile = File(rawPath)
        validateWallpaperFile(sourceFile)

        var hookCount = 0

        // 1. Rewrite ambient catalog accessor
        val accessorMethod = AmbientCatalogAccessorFingerprint.methodOrNull
            ?: throw PatchException("Ambient wallpaper catalog accessor not found")
        val catalogClass = AmbientCatalogAccessorFingerprint.originalClassDef.type
        val catalogMethod = AmbientCatalogAccessorFingerprint.originalMethod.name
        val resourceIdField = backgroundImageResourceIdField(
            AmbientCatalogAccessorFingerprint.originalClassDef,
        )

        accessorMethod.apply {
            removeInstructions(0, implementation!!.instructions.count())
            addInstructions(0, forceAmbientCatalogAccessorSmali(resourceIdField))
        }
        hookCount++
        println("[Custom NTP Wallpaper] Hooked ambient catalog accessor: $catalogClass->$catalogMethod")

        // 2. Force wallpaper callbacks to invoke the ambient catalog accessor
        var forcedCallbacks = 0
        classDefForEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.returnType != "V" ||
                    method.parameterTypes.toList() != listOf("Ljava/lang/Object;") ||
                    method.implementation == null
                ) {
                    return@forEach
                }
                val callsCatalog = method.implementation!!.instructions.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                    ref != null &&
                        ref.definingClass == catalogClass &&
                        ref.name == catalogMethod &&
                        ref.parameterTypes.isEmpty() &&
                        ref.returnType == BACKGROUND_IMAGE_MODEL
                }
                if (!callsCatalog) return@forEach

                mutableClassDefBy(classDef).methods
                    .first { it.name == method.name && it.parameterTypes == method.parameterTypes }
                    .addInstructions(0, forceUseAmbientCatalogSmali(catalogClass, catalogMethod))
                forcedCallbacks++
                hookCount++
            }
        }
        if (forcedCallbacks == 0) {
            throw PatchException("No wallpaper callbacks calling ambient catalog accessor were found")
        }
        println("[Custom NTP Wallpaper] Forced $forcedCallbacks wallpaper callback site(s) to use ambient catalog")

        // 3. Keep JNI factory methods consistent (best-effort)
        val targetPackage = packageMetadata.packageName.takeIf { it.isNotBlank() } ?: Constants.BRAVE_PACKAGE_NAME
        var jniHooks = 0
        CreateWallpaperFingerprint.methodOrNull?.let { method ->
            method.addInstructions(0, forceCreateWallpaperParamsSmali(targetPackage))
            jniHooks++
            hookCount++
        }
        CreateBrandedWallpaperFingerprint.methodOrNull?.let { method ->
            method.addInstructions(0, forceCreateBrandedWallpaperParamsSmali(targetPackage))
            jniHooks++
            hookCount++
        }
        if (jniHooks > 0) {
            println("[Custom NTP Wallpaper] Updated $jniHooks JNI wallpaper factory method(s)")
        }

        println("[Custom NTP Wallpaper] Applied $hookCount custom NTP wallpaper bytecode hook(s)")
    }
}
