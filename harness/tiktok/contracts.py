"""
Declarative patch contracts and structural invariants for TikLite TikTok patches.

Logging and Telemetry Conventions:
- Raw resource and slimmer patches MUST emit a single consolidated log line on completion.
- Prohibit per-file loop logging (never invoke `println` inside `forEach` or file walks).
- Format: `[<PatchName>] Stripped <count> <target> -> Saved <size> uncompressed (~<est> in APK)`
"""

from __future__ import annotations
from dataclasses import dataclass
from typing import List


@dataclass
class PatchContract:
    patch_id: str
    name: str
    target_type: str  # 'bytecode', 'resource', 'raw_resource'
    description: str
    required_classes: List[str]
    required_strings: List[str]
    criticality: str = "HIGH"  # 'CRITICAL', 'HIGH', 'MEDIUM'
    expected_savings_mb: float = 0.0  # Optional estimated APK savings in MB for slimmers


TIKTOK_PATCH_CONTRACTS: List[PatchContract] = [
    # --- Slimmers & Asset De-bloat ---
    PatchContract(
        patch_id="locale_slimmer",
        name="Language Pack Purger",
        target_type="raw_resource",
        description="Strips unselected language string bundles from assets/strings#lang_*.",
        required_classes=[],
        required_strings=["assets/strings#lang_"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="core_asset_debloat",
        name="Core Asset De-bloat",
        target_type="raw_resource",
        description="Strips embedded Microblink card/document OCR scanning models, C2PA AI content origin verification libraries, ByteDance TTWebView engine, non-Latin fonts, and mini-app JavaScript engines (V8 runtimes).",
        required_classes=[],
        required_strings=["assets/microblink", "libtt_c2pa_sdk.so", "assets/fonts", "libminiapp", "libv8"],
        criticality="MEDIUM",
    ),
    PatchContract(
        patch_id="creator_bloat_slimmer",
        name="Studio & Creation De-bloat",
        target_type="raw_resource",
        description="Strips camera AR effects engine (libeffect_plugin.so), video editor SDK (libttvesdk_plugin.so), and facial recognition models.",
        required_classes=[],
        required_strings=["libeffect_plugin.so", "libttvesdk_plugin.so"],
        criticality="HIGH",
    ),
    # --- Privacy & 0 Telemetry ---
    PatchContract(
        patch_id="unified_telemetry_tracker_silencer",
        name="Unified Telemetry & Tracker Silencer",
        target_type="bytecode",
        description="Neutralizes ByteDance AppLog, APM/Heimdallr, Npth crash handlers, AppsFlyer, and Firebase telemetry.",
        required_classes=[
            "Lcom/ss/android/common/applog/AppLog;",
            "Lcom/ss/android/ugc/aweme/legoImp/task/NpthCoreInitTask;",
            "Lcom/ss/android/ugc/aweme/legoImp/task/InitAppsFlyer;",
        ],
        required_strings=["onEvent", "run"],
        criticality="CRITICAL",
    ),
    PatchContract(
        patch_id="device_privacy_guard",
        name="Device Privacy Guard",
        target_type="bytecode",
        description="Neutralizes background clipboard snooping routines, purges 33 invasive permissions, blocks screenshot/recording detection, and bypasses Android FLAG_SECURE recording restrictions.",
        required_classes=["Lcom/ss/android/ugc/aweme/feedback/screenshot/ScreenShotFeedbackService;"],
        required_strings=["onShot", "safelyShowDialog"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="in_app_browser_privacy_guard",
        name="In-App Browser Privacy Guard",
        target_type="bytecode",
        description="Redirects external and third-party web links directly to the default system browser and neutralizes inline JavaScript tracking injection, DOM monitoring, and AJAX hooking in residual in-app WebViews.",
        required_classes=[],
        required_strings=["webview_inline_inject_js", "ContainerId", "Context_startActivity_1"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="disable_in_app_update_nags",
        name="Update Prompt Suppressor",
        target_type="bytecode",
        description="Neutralizes background update polling tasks and device ID check routines to prevent forced update popups.",
        required_classes=["Lcom/ss/android/ugc/aweme/legoImp/task/CheckUpdateChangeDeviceIDTaskHolder$Background;"],
        required_strings=[],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="feed_ad_blocker",
        name="Feed Ad Blocker",
        target_type="bytecode",
        description="Removes sponsored advertisements, brand promotions, and promotional audio from the For You and Following feeds.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
            "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            "Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;",
        ],
        required_strings=["fetchFeedList", "getItems"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="hide_tiktok_shop_anchors",
        name="Hide TikTok Shop Anchors",
        target_type="bytecode",
        description="Removes product showcase badges, shopping cart tags, and TikTok Shop commercial anchors from video posts in the feed.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
            "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            "Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;",
        ],
        required_strings=["fetchFeedList", "getItems"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="feed_live_stream_blocker",
        name="Feed Live Stream Blocker",
        target_type="bytecode",
        description="Removes live stream broadcast cards and live recommendations from the For You and Following feeds.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
            "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            "Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;",
        ],
        required_strings=["fetchFeedList", "getItems"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="feed_bloat_blocker",
        name="Feed Bloat & Distraction Blocker",
        target_type="bytecode",
        description="Removes non-video clutter from the For You and Following feeds, including suggested friend cards, mini-games, CapCut/template creation prompts, memories ('On This Day'), surveys, mini-drama paywalls, and Lemon8 promo tasks.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
            "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            "Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;",
        ],
        required_strings=["fetchFeedList", "getItems"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="clean_share_url",
        name="Clean Share URL",
        target_type="bytecode",
        description="Strips tracking parameters, user IDs, device fingerprints, and marketing tokens from shared TikTok links.",
        required_classes=["Lcom/ss/android/ugc/aweme/feed/model/Aweme;"],
        required_strings=["getShareUrl"],
        criticality="HIGH",
    ),
    # --- Performance, RAM, Battery & Media ---
    PatchContract(
        patch_id="instant_cold_start",
        name="Instant Launch & Splash Blocker",
        target_type="bytecode",
        description="Eliminates cold startup delays, real-time splash advertisements, and background TopView preloading (<0.4s launch).",
        required_classes=[
            "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/SplashAdManagerPreloadTask;",
            "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashSettingServiceImpl;",
            "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
            "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/realtimesplash/RealTimeSplashManagerImpl;",
        ],
        required_strings=[],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="live_stream_suite_optimizer",
        name="Live Stream SDK & Minigame De-bloat",
        target_type="raw_resource",
        description="Strips live co-host interactive streaming SDKs (liblink_mic_sdk.so) and live battle minigames to save APK storage.",
        required_classes=[],
        required_strings=["liblink_mic_sdk.so"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="live_gift_effect_optimizer",
        name="Live Stream 3D Gift Optimizer",
        target_type="bytecode",
        description="Disables Live 3D gift particle effect engine and widget rendering lifecycle to eliminate frame drops during live streams.",
        required_classes=["Lcom/bytedance/android/livesdk/gift/base/platform/business/effect/LiveGiftEffectWidget;"],
        required_strings=[],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="resource_governor",
        name="Resource & Battery Governor",
        target_type="bytecode",
        description="Throttles background sensor polling (gyroscope/accelerometer 3D ads), prevents aggressive video buffer preloading, and caps Fresco animated image memory retention.",
        required_classes=[
            "Lcom/facebook/fresco/animation/bitmap/cache/FrescoFrameCache;",
            "Lcom/ss/android/ugc/aweme/video/preload/model/PreloadStrategyConfig;",
        ],
        required_strings=["isEnableBufferPreload", "LIZJ", "LJFF"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="disable_push_notifications",
        name="Disable Push Notifications",
        target_type="bytecode",
        description="Neutralizes background push notification tasks and persistent socket wake locks to eliminate background battery drain.",
        required_classes=["Lcom/ss/android/ugc/aweme/legoImp/task/InitPushTask;"],
        required_strings=["run"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="media_enhancements",
        name="Media Usability & Watermark-Free Downloader",
        target_type="bytecode",
        description="Unblocks the download button on creator-restricted videos and Stories in the Share panel, routing downloads to clean unwatermarked media streams.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
            "Lcom/ss/android/ugc/aweme/feed/model/Video;",
        ],
        required_strings=["isPreventDownload", "getDownloadWithoutWatermark", "getDownloadAddr"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="playback_speed_persistence",
        name="Playback Speed Persistence",
        target_type="bytecode",
        description="Persists selected video playback speed across all feed videos and application restarts.",
        required_classes=["Lcom/ss/android/ugc/aweme/feed/model/Aweme;"],
        required_strings=["getParameterizedSpeed"],
        criticality="MEDIUM",
    ),
    PatchContract(
        patch_id="video_quality_governor",
        name="Video Quality Governor",
        target_type="bytecode",
        description="Enforces configurable resolution ceilings for video playback and downloads independently (1080p, 720p, 540p, 480p, or uncapped).",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
            "Lcom/ss/android/ugc/aweme/feed/model/Video;",
            "Lcom/ss/android/ugc/playerkit/simapicommon/model/SimVideoUrlModel;",
        ],
        required_strings=["getVideo", "getBitRate"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="mandatory_login_bypass",
        name="Bypass Mandatory Login",
        target_type="bytecode",
        description="Neutralizes mandatory login walls, dynamic regional forced login gates, and guest mode browsing restrictions.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/services/MandatoryLoginService;",
        ],
        required_strings=["shouldShowForcedLogin", "enableForcedLogin", "shouldShowLoginTabFirst", "tryShowMandatoryLoginPage"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="skip_first_launch_onboarding",
        name="Skip First-Launch Onboarding",
        target_type="bytecode",
        description="Bypasses the entire first-run introduction funnel directly to the feed.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/main/MainPageFragment;",
            "Lcom/ss/android/ugc/aweme/NewUserJourneyService;",
        ],
        required_strings=["JS", "did_finish_nuj", "new_user_journey"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="client_ai_governor",
        name="Client-Side AI & Behavioral Profiling Governor",
        target_type="bytecode",
        description="Neutralizes on-device machine learning inference (Pitaya), Tako AI chatbot entry points and icons, and AI smart search suggestion clutter.",
        required_classes=["Lcom/bytedance/pitaya/api/PitayaBootLoader;"],
        required_strings=["setup", "commitBootTaskBySettings"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="region_bypass",
        name="SIM Region Selector",
        target_type="bytecode",
        description="Spoofs the detected SIM and network country ISO code to bypass regional feed restrictions and catalog blocks.",
        required_classes=[],
        required_strings=["TelephonyManager_getSimCountryIso", "TelephonyManager_getNetworkCountryIso"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="p2p_video_relay_blocker",
        name="P2P Video Relay & Mesh CDN Blocker",
        target_type="raw_resource",
        description="Strips background Peer-to-Peer CDN distribution binaries (libavmdlp2pv2.so and libp2plivevdp.so) to prevent battery drain, background data upload, and mesh relay.",
        required_classes=[],
        required_strings=["libavmdlp2pv2.so", "libp2plivevdp.so"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="fix_google_login",
        name="Fix Google login",
        target_type="bytecode",
        description="Restores Google account sign-in after patching by forcing fallback to Web-based OAuth when Google Play Services rejects the modified APK signature.",
        required_classes=["Lcom/bytedance/lobby/google/GoogleAuth;"],
        required_strings=["isAvailable"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="show_seekbar",
        name="Show seekbar",
        target_type="bytecode",
        description="Restores TikTok's native video seekbar and scrubbing controls where normally hidden or disabled.",
        required_classes=["Lcom/ss/android/ugc/aweme/feed/model/Aweme;"],
        required_strings=["getVideoControl", "seekbar show type change, change to:"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="always_show_publish_date",
        name="Always show publish date",
        target_type="bytecode",
        description="Forces video publish/upload date to remain visible in video author information across all feed types.",
        required_classes=["Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoVM;"],
        required_strings=["paramSync2StateAccept", "v3"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="copy_comments_without_username",
        name="Copy comments without username",
        target_type="bytecode",
        description="Copies only the comment text without prepending the author username.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
        ],
        required_strings=["getText"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="display_refresh_rate_governor",
        name="Display Refresh Rate Governor",
        target_type="bytecode",
        description="Forces TikTok to run at peak display refresh rate (120Hz/90Hz/60Hz) and neutralizes video playback framerate downclocking routines.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/main/MainActivity;",
        ],
        required_strings=["onResume", "onWindowFocusChanged"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="ghost_mode",
        name="Ghost Mode",
        target_type="bytecode",
        description="Enables anonymous profile and story browsing by suppressing outbound view reporting.",
        required_classes=[
            "Lcom/ss/android/ugc/profile/business/ci/viewer/api/ProfileViewerApiService;",
        ],
        required_strings=["reportView", "reportStoryViewed"],
        criticality="HIGH",
    ),
]


