package app.morphe.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    const val BRAVE_TARGET_VERSION = "1.93.137"
    const val BRAVE_PACKAGE_NAME = "com.brave.browser"

    val COMPATIBILITY_BRAVE = Compatibility(
        name = "Brave Browser",
        packageName = BRAVE_PACKAGE_NAME,
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFF4500,
        targets = listOf(
            AppTarget(
                version = BRAVE_TARGET_VERSION
            )
        )
    )
}
