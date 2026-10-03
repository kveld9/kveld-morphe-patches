package app.morphe.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    const val BRAVE_TARGET_VERSION = "1.96.61"
    const val BRAVE_PACKAGE_NAME = "com.brave.browser"

    val COMPATIBILITY_BRAVE = Compatibility(
        name = "Brave Private Web Browser, VPN",
        packageName = BRAVE_PACKAGE_NAME,
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFF4500,
        targets = listOf(
            AppTarget(
                version = BRAVE_TARGET_VERSION,
                description = "Download Bravemonoarm64.apk or BraveMonoarm.apk (v1.96.61) from github.com/brave/brave-browser/releases"
            )
        )
    )

    const val GBOARD_PACKAGE_NAME = "com.google.android.inputmethod.latin"
    const val GBOARD_TARGET_VERSION = "18.4.1.985164140-lite_beta-arm64-v8a"
    const val GBOARD_TARGET_VERSION_V7A = "18.4.1.985164140-lite_beta-armeabi-v7a"

    val COMPATIBILITY_GBOARD = Compatibility(
        name = "Gboard Lite",
        packageName = GBOARD_PACKAGE_NAME,
        apkFileType = ApkFileType.APK,
        appIconColor = 0x4285F4,
        targets = listOf(
            AppTarget(
                version = GBOARD_TARGET_VERSION,
                description = "Download $GBOARD_TARGET_VERSION (APK nodpi) from APKMirror"
            ),
            AppTarget(
                version = GBOARD_TARGET_VERSION_V7A,
                description = "Download $GBOARD_TARGET_VERSION_V7A (APK nodpi) from APKMirror"
            )
        )
    )


    const val HEVY_PACKAGE_NAME = "com.hevy"
    const val HEVY_TARGET_VERSION = "3.1.14"

    val COMPATIBILITY_HEVY = Compatibility(
        name = "Hevy - Gym Log Workout Tracker",
        packageName = HEVY_PACKAGE_NAME,
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFF4500,
        targets = listOf(
            AppTarget(
                version = HEVY_TARGET_VERSION,
                description = "Download com.hevy v$HEVY_TARGET_VERSION (APKM bundle) from APKMirror"
            )
        )
    )

    const val TIKTOK_PACKAGE_NAME = "com.zhiliaoapp.musically"
    const val TIKTOK_TARGET_VERSION = "47.1.4"

    val COMPATIBILITY_TIKTOK = Compatibility(
        name = "TikTok",
        packageName = TIKTOK_PACKAGE_NAME,
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFE2C55,
        targets = listOf(
            AppTarget(
                version = TIKTOK_TARGET_VERSION,
                description = "Download TikTok v$TIKTOK_TARGET_VERSION (nodpi APK) from APKMirror"
            )
        )
    )

    const val NOKOPRINT_PACKAGE_NAME = "com.nokoprint"
    const val NOKOPRINT_TARGET_VERSION = "5.28.6"

    val COMPATIBILITY_NOKOPRINT = Compatibility(
        name = "NokoPrint - WiFi, Bluetooth, USB",
        packageName = NOKOPRINT_PACKAGE_NAME,
        apkFileType = ApkFileType.APK,
        appIconColor = 0x0288D1,
        targets = listOf(
            AppTarget(
                version = NOKOPRINT_TARGET_VERSION,
                description = "Download com.nokoprint v$NOKOPRINT_TARGET_VERSION (XAPK bundle) from APKPure",
            )
        )
    )

    const val XIAOMI_EARBUDS_PACKAGE_NAME = "com.mi.earphone"
    const val XIAOMI_EARBUDS_TARGET_VERSION = "1.38.0i"

    val COMPATIBILITY_XIAOMI_EARBUDS = Compatibility(
        name = "Xiaomi Earbuds",
        packageName = XIAOMI_EARBUDS_PACKAGE_NAME,
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFF6700,
        targets = listOf(
            AppTarget(
                version = XIAOMI_EARBUDS_TARGET_VERSION,
                description = "Download com.mi.earphone v1.38.0i (XAPK bundle) from APKPure",
            )
        )
    )

    const val TIKTOK_EXTENSION_FILTER_CLASS = "Lcom/kveld9/morphe/extension/tiktok/TikTokFeedAdFilter;"
    const val TIKTOK_EXTENSION_MEDIA_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokMediaHook;"
    const val TIKTOK_EXTENSION_SPEED_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokSpeedHook;"
    const val TIKTOK_EXTENSION_QUALITY_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokVideoQualityHook;"
    const val TIKTOK_EXTENSION_BROWSER_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokBrowserHook;"
    const val TIKTOK_EXTENSION_COMMENT_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokCommentHook;"
    const val TIKTOK_EXTENSION_COMMENT_TRANSLATE_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokCommentTranslateHook;"
    const val TIKTOK_EXTENSION_SEEKBAR_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokSeekbarHook;"
    const val TIKTOK_EXTENSION_PRIVACY_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokPrivacyHook;"
    const val TIKTOK_EXTENSION_REFRESH_RATE_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokRefreshRateHook;"
    const val TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokOfflineVideosHook;"
    const val TIKTOK_EXTENSION_AUTOPAUSE_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokAutoPauseHook;"
    const val TIKTOK_EXTENSION_SEARCH_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokSearchHook;"
    const val TIKTOK_EXTENSION_SHARE_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokShareHook;"
    const val TIKTOK_EXTENSION_LOGIN_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokLoginHook;"
    const val TIKTOK_EXTENSION_SEEN_VIDEO_HOOK = "Lcom/kveld9/morphe/extension/tiktok/TikTokSeenVideoHook;"
    const val BRAVE_EXTENSION_CLASS = "Lcom/kveld9/morphe/extension/BraveExtension;"
    const val CHROMIUM_EXTENSION_CLASS = "Lcom/kveld9/morphe/extension/ChromiumExtension;"
    const val GBOARD_EXTENSION_CLASS = "Lcom/kveld9/morphe/extension/gboard/GboardExtension;"

    object GboardPrefs {
        const val KEY_RESTART_GBOARD = "morphe_restart_gboard"
        const val KEY_ENABLE_IME = "morphe_enable_ime"
        const val KEY_SELECT_IME = "morphe_select_ime"
        const val KEY_AMOLED = "morphe_amoled_enabled"
        const val KEY_ZERO_BOTTOM_INSET = "morphe_zero_bottom_inset"
        const val KEY_BOTTOM_PADDING = "morphe_bottom_padding"
        const val KEY_TOOLBAR_ITEM_COUNT = "morphe_toolbar_item_count"
        const val KEY_EMOJI_SCALE = "morphe_emoji_scale"
        const val KEY_KEY_SHAPE_SELECTION = "morphe_key_shape_selection"
        const val KEY_ACCESS_POINTS_REDESIGN = "morphe_access_points_redesign"
        const val KEY_DISMISS_SUGGESTIONS = "morphe_dismiss_suggestions"
        const val KEY_CURSOR_TRACKPAD = "morphe_cursor_trackpad"
        const val KEY_CLIPBOARD_EXTENDED_RETENTION = "morphe_clipboard_extended_retention"
        const val KEY_CLIPBOARD_RETENTION_HOURS = "morphe_clipboard_retention_hours"
        const val KEY_CLIPBOARD_RAISE_LIMIT = "morphe_clipboard_raise_limit"
        const val KEY_CLIPBOARD_UNPINNED_LIMIT = "morphe_clipboard_unpinned_limit"
        const val KEY_CLIPBOARD_GRID_LAYOUT = "morphe_clipboard_grid_layout"
        const val KEY_CLIPBOARD_GRID_COLUMNS = "morphe_clipboard_grid_columns"
        const val KEY_GRAMMAR_CHECKER = "morphe_grammar_checker"
        const val KEY_BLUETOOTH_MIC = "morphe_bluetooth_mic"
        const val KEY_FORCE_INCOGNITO = "morphe_force_incognito"
        const val KEY_HIDE_INCOGNITO_ICON = "morphe_hide_incognito_icon"
        const val KEY_DECOUPLE_TOUCH_FEEDBACK = "morphe_decouple_touch_feedback"

        const val MIN_BOTTOM_PADDING = 0
        const val MAX_BOTTOM_PADDING = 150
        const val DEFAULT_BOTTOM_PADDING = 0

        const val MIN_TOOLBAR_ITEM_COUNT = 4
        const val MAX_TOOLBAR_ITEM_COUNT = 8
        const val DEFAULT_TOOLBAR_ITEM_COUNT = 5

        const val MIN_CLIPBOARD_RETENTION_HOURS = 1
        const val MAX_CLIPBOARD_RETENTION_HOURS = 168
        const val DEFAULT_CLIPBOARD_RETENTION_HOURS = 24

        const val MIN_CLIPBOARD_UNPINNED_LIMIT = 5
        const val MAX_CLIPBOARD_UNPINNED_LIMIT = 100
        const val DEFAULT_CLIPBOARD_UNPINNED_LIMIT = 50

        const val MIN_CLIPBOARD_GRID_COLUMNS = 1
        const val MAX_CLIPBOARD_GRID_COLUMNS = 3
        const val DEFAULT_CLIPBOARD_GRID_COLUMNS = 2

        const val MIN_EMOJI_SCALE = 50
        const val MAX_EMOJI_SCALE = 150
        const val DEFAULT_EMOJI_SCALE = 100
    }
}


