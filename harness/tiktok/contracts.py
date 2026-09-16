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
        description="Strips embedded Microblink card/document OCR scanning models, C2PA AI content origin verification libraries, non-Latin fonts, and mini-app JavaScript engines (V8 runtimes).",
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
        description="Neutralizes background clipboard snooping routines and blocks screenshot/recording detection listeners to protect user data from unauthorized tracking.",
        required_classes=["Lcom/ss/android/ugc/aweme/feedback/screenshot/ScreenShotFeedbackService;"],
        required_strings=["onShot", "safelyShowDialog"],
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
        description="Enables progress seekbar scrubbing on all videos, unblocks the download button on creator-restricted videos, and routes downloads to clean unwatermarked media streams.",
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
]
