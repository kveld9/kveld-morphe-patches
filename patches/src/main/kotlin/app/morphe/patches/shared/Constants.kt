package app.morphe.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    const val BRAVE_TARGET_VERSION = "1.93.138"
    const val BRAVE_PACKAGE_NAME = "com.brave.browser"

    val COMPATIBILITY_BRAVE = Compatibility(
        name = "Brave Private Web Browser, VPN",
        packageName = BRAVE_PACKAGE_NAME,
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFF4500,
        targets = listOf(
            AppTarget(
                version = BRAVE_TARGET_VERSION,
                description = "Download Bravemonoarm64.apk (v1.93.138) from github.com/brave/brave-browser/releases"
            )
        )
    )

    const val GBOARD_PACKAGE_NAME = "com.google.android.inputmethod.latin"
    const val GBOARD_TARGET_VERSION = "18.0.3.954559732-lite_beta-arm64-v8a"

    val COMPATIBILITY_GBOARD = Compatibility(
        name = "Gboard Lite",
        packageName = GBOARD_PACKAGE_NAME,
        apkFileType = ApkFileType.APK,
        appIconColor = 0x4285F4,
        targets = listOf(
            AppTarget(
                version = GBOARD_TARGET_VERSION,
                description = "Download 18.0.3.954559732-lite_beta-arm64-v8a (APK nodpi) from APKMirror"
            )
        )
    )
}
