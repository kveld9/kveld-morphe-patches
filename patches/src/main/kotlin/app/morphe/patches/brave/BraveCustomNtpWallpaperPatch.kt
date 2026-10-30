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
private val IHDR_CHUNK = byteArrayOf(0x49, 0x48, 0x44, 0x52)

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

private fun validateWallpaperFile(file: File): WallpaperDimensions {
    if (!file.exists()) {
        throw PatchException("Custom NTP wallpaper file does not exist: ${file.absolutePath}")
    }
    if (!file.isFile || !file.canRead()) {
        throw PatchException("Custom NTP wallpaper file is not readable: ${file.absolutePath}")
    }
    if (file.length() > MAX_WALLPAPER_FILE_SIZE) {
        throw PatchException("Custom NTP wallpaper exceeds 8 MiB: ${file.absolutePath}")
    }
    val dimensions = readPngDimensions(file)
        ?: throw PatchException("Custom NTP wallpaper must be a valid PNG file: ${file.absolutePath}")
    if (dimensions.width !in MIN_WALLPAPER_DIMENSION..MAX_WALLPAPER_DIMENSION ||
        dimensions.height !in MIN_WALLPAPER_DIMENSION..MAX_WALLPAPER_DIMENSION
    ) {
        throw PatchException(
            "Custom NTP wallpaper dimensions must be between $MIN_WALLPAPER_DIMENSION and " +
                "$MAX_WALLPAPER_DIMENSION px on each side, received ${dimensions.width}x${dimensions.height}",
        )
    }
    return dimensions
}

private fun readPngDimensions(file: File): WallpaperDimensions? =
    runCatching {
        DataInputStream(FileInputStream(file).buffered()).use { input ->
            val signature = ByteArray(8)
            input.readFully(signature)
            if (!signature.contentEquals(PNG_SIGNATURE)) return null

            val headerLength = input.readInt()
            val headerType = ByteArray(4)
            input.readFully(headerType)
            if (headerLength != 13 || !headerType.contentEquals(IHDR_CHUNK)) return null

            val width = input.readInt()
            val height = input.readInt()
            if (width <= 0 || height <= 0) return null

            WallpaperDimensions(width, height)
        }
    }.getOrNull()

private fun installWallpaperResource(resourceDirectory: File, sourceFile: File) {
    val drawableDirectory = resourceDirectory.resolve("drawable-nodpi").apply(File::mkdirs)
    sourceFile.copyTo(
        target = drawableDirectory.resolve("$NTP_WALLPAPER_RESOURCE_NAME.png"),
        overwrite = true,
    )
}

// ════════════════════════════════════════════════════════════════════════════════
// 5. RESOURCE PATCH
// ════════════════════════════════════════════════════════════════════════════════

private val braveCustomNtpWallpaperResourcePatch: ResourcePatch = resourcePatch(
    name = "Custom NTP wallpaper (Setup)",
    description = "Installs the patch-time wallpaper PNG as a Brave drawable.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val rawPath = runCatching { braveCustomNtpWallpaperPatch.options["customWallpaper"].value as? String }
            .getOrNull()
            ?.trim()
        if (rawPath.isNullOrBlank()) {
            throw PatchException(
                "Custom NTP wallpaper requires selecting a valid PNG file in the customWallpaper option (480-8192 px, max 8 MiB)",
            )
        }
        val sourceFile = File(rawPath)
        validateWallpaperFile(sourceFile)
        installWallpaperResource(get("res"), sourceFile)
        println("[Custom NTP Wallpaper] Installed $NTP_WALLPAPER_RESOURCE_NAME.png into drawable-nodpi")
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// 6. BYTECODE PATCH
// ════════════════════════════════════════════════════════════════════════════════

@Suppress("unused")
val braveCustomNtpWallpaperPatch: BytecodePatch = bytecodePatch(
    name = "Custom NTP wallpaper",
    description = "Forces the Brave new-tab background to a custom PNG chosen at patch time. " +
        "Rewrites the Java ambient wallpaper catalog (BackgroundImage drawable resource id) and " +
        "makes wallpaper callbacks use it instead of native branded/URL images. Crop the image to " +
        "your screen resolution before selecting it. Default off.",
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
        description = "PNG wallpaper used as the Brave new-tab background. " +
            "Crop it to your current screen resolution first, then select that file here. " +
            "480-8192 px on each side, maximum 8 MiB.",
        required = true,
        allowedExtensions = listOf("png"),
        recommendedSize = ImageSize(1920, 1080),
        validator = { value -> value == null || value.trim().endsWith(".png", ignoreCase = true) },
    )

    execute {
        val rawPath = customWallpaperPath?.trim()
        if (rawPath.isNullOrBlank()) {
            throw PatchException(
                "Custom NTP wallpaper requires selecting a valid PNG file in the customWallpaper option (480-8192 px, max 8 MiB)",
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
