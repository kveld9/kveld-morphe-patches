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
        name="Remove Extra Languages",
        target_type="raw_resource",
        description="Strips unselected language string bundles from assets/strings#lang_*.",
        required_classes=[],
        required_strings=["assets/strings#lang_"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="core_asset_debloat",
        name="Remove Extra Files",
        target_type="raw_resource",
        description="Strips embedded Microblink card/document OCR scanning models, C2PA AI content origin verification libraries, ByteDance TTWebView engine, non-Latin fonts, and mini-app JavaScript engines (V8 runtimes).",
        required_classes=[],
        required_strings=["assets/microblink", "libtt_c2pa_sdk.so", "assets/fonts", "libminiapp", "libv8"],
        criticality="MEDIUM",
    ),
    PatchContract(
        patch_id="creator_bloat_slimmer",
        name="Remove Editor Files",
        target_type="raw_resource",
        description="Strips camera AR effects engine (libeffect_plugin.so), video editor SDK (libttvesdk_plugin.so), and facial recognition models.",
        required_classes=[],
        required_strings=["libeffect_plugin.so", "libttvesdk_plugin.so"],
        criticality="HIGH",
    ),
    # --- Privacy & 0 Telemetry ---
    PatchContract(
        patch_id="unified_telemetry_tracker_silencer",
        name="Block All Tracking",
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
        name="Privacy Guard",
        target_type="bytecode",
        description="Neutralizes background clipboard snooping routines, purges 33 invasive permissions, blocks screenshot/recording detection, and bypasses Android FLAG_SECURE recording restrictions.",
        required_classes=["Lcom/ss/android/ugc/aweme/feedback/screenshot/ScreenShotFeedbackService;"],
        required_strings=["onShot", "safelyShowDialog"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="in_app_browser_privacy_guard",
        name="Open Links Outside",
        target_type="bytecode",
        description="Redirects external and third-party web links directly to the default system browser and neutralizes inline JavaScript tracking injection, DOM monitoring, and AJAX hooking in residual in-app WebViews.",
        required_classes=[],
        required_strings=["webview_inline_inject_js", "ContainerId", "Context_startActivity_1"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="disable_in_app_update_nags",
        name="No Update Popups",
        target_type="bytecode",
        description="Neutralizes background update polling tasks and device ID check routines to prevent forced update popups.",
        required_classes=["Lcom/ss/android/ugc/aweme/legoImp/task/CheckUpdateChangeDeviceIDTaskHolder$Background;"],
        required_strings=[],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="feed_ad_blocker",
        name="Block Feed Ads",
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
        name="Hide Shop",
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
        name="No Lives in Feed",
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
        name="Cleaner Feed",
        target_type="bytecode",
        description="Removes non-video clutter from the For You, Following, and Friends feeds, including suggested friend cards, mini-games, CapCut/template creation prompts, memories ('On This Day'), surveys, mini-drama paywalls, and Lemon8 promo tasks.",
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
        name="Clean Shared Links",
        target_type="bytecode",
        description="Strips tracking parameters, user IDs, device fingerprints, and marketing tokens from shared TikTok links.",
        required_classes=["Lcom/ss/android/ugc/aweme/feed/model/Aweme;"],
        required_strings=["getShareUrl"],
        criticality="HIGH",
    ),
    # --- Performance, RAM, Battery & Media ---
    PatchContract(
        patch_id="instant_cold_start",
        name="Fast Startup",
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
        name="Remove Live Extras",
        target_type="raw_resource",
        description="Strips live co-host interactive streaming SDKs (liblink_mic_sdk.so) and live battle minigames to save APK storage.",
        required_classes=[],
        required_strings=["liblink_mic_sdk.so"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="live_gift_effect_optimizer",
        name="Lighter Live Gifts",
        target_type="bytecode",
        description="Disables Live 3D gift particle effect engine and widget rendering lifecycle to eliminate frame drops during live streams.",
        required_classes=["Lcom/bytedance/android/livesdk/gift/base/platform/business/effect/LiveGiftEffectWidget;"],
        required_strings=[],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="resource_governor",
        name="Battery Saver",
        target_type="bytecode",
        description="Throttles background sensor polling (gyroscope/accelerometer 3D ads) and prevents aggressive video buffer preloading.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/video/preload/model/PreloadStrategyConfig;",
        ],
        required_strings=["isEnableBufferPreload"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="disable_push_notifications",
        name="No Push Notifications",
        target_type="bytecode",
        description="Neutralizes background push notification tasks and persistent socket wake locks to eliminate background battery drain.",
        required_classes=["Lcom/ss/android/ugc/aweme/legoImp/task/InitPushTask;"],
        required_strings=["run"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="media_enhancements",
        name="Save Without Watermark",
        target_type="bytecode",
        description="Unblocks the download button on creator-restricted videos in the Share panel, routing downloads to clean unwatermarked media streams.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
            "Lcom/ss/android/ugc/aweme/feed/model/Video;",
        ],
        required_strings=["isPreventDownload", "getDownloadWithoutWatermark", "getDownloadAddr"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="disable_post_download_dialog",
        name="No Share Popup",
        target_type="bytecode",
        description="Suppresses the automatic 'Share to' and friend suggestions bottom sheet that pops up after finishing a download.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/internalshare/impl/fragment/DownloadAndShareFragment;",
        ],
        required_strings=["after_video_saved_share_to_nscreen"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="playback_speed_persistence",
        name="Remember Speed",
        target_type="bytecode",
        description="Persists selected video playback speed across all feed videos and application restarts.",
        required_classes=["Lcom/ss/android/ugc/aweme/feed/model/Aweme;"],
        required_strings=["getParameterizedSpeed"],
        criticality="MEDIUM",
    ),
    PatchContract(
        patch_id="video_quality_governor",
        name="Limit Video Quality",
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
        name="Browse Without Login",
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
        name="Skip Setup",
        target_type="bytecode",
        description="Bypasses the entire first-run introduction funnel directly to the feed.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/main/MainPageFragment;",
            "Lcom/ss/android/ugc/aweme/NewUserJourneyService;",
        ],
        required_strings=["did_finish_nuj", "new_user_journey"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="client_ai_governor",
        name="Turn Off AI Features",
        target_type="bytecode",
        description="Neutralizes on-device machine learning inference (Pitaya), Tako AI chatbot entry points and icons, and AI smart search suggestion clutter.",
        required_classes=["Lcom/bytedance/pitaya/api/PitayaBootLoader;"],
        required_strings=["setup", "commitBootTaskBySettings"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="region_bypass",
        name="Change Country",
        target_type="bytecode",
        description="Spoofs the detected SIM and network country ISO code to bypass regional feed restrictions and catalog blocks.",
        required_classes=[],
        required_strings=["TelephonyManager_getSimCountryIso", "TelephonyManager_getNetworkCountryIso"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="p2p_video_relay_blocker",
        name="Stop P2P Uploads",
        target_type="raw_resource",
        description="Strips background Peer-to-Peer CDN distribution binaries (libavmdlp2pv2.so and libp2plivevdp.so) to prevent battery drain, background data upload, and mesh relay.",
        required_classes=[],
        required_strings=["libavmdlp2pv2.so", "libp2plivevdp.so"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="fix_google_login",
        name="Fix Google Login",
        target_type="bytecode",
        description="Restores Google account sign-in after patching by forcing fallback to Web-based OAuth when Google Play Services rejects the modified APK signature.",
        required_classes=["Lcom/bytedance/lobby/google/GoogleAuth;"],
        required_strings=["isAvailable"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="show_seekbar",
        name="Video Seekbar",
        target_type="bytecode",
        description="Restores TikTok's native video seekbar and scrubbing controls where normally hidden or disabled.",
        required_classes=["Lcom/ss/android/ugc/aweme/feed/model/Aweme;"],
        required_strings=["getVideoControl"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="always_show_publish_date",
        name="Show Video Dates",
        target_type="bytecode",
        description="Forces video publish/upload date to remain visible in video author information across all feed types.",
        required_classes=["Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoVM;"],
        required_strings=["paramSync2StateAccept", "v3"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="comment_customizer",
        name="Better Comments",
        target_type="bytecode",
        description="Customizes TikTok's comment section, including native sort controls, clean text copying, disabling suggested emojis bar, hiding comment quick actions, enabling voice comments, and automatic comment translation.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/comment/model/Comment;",
            "Lcom/ss/android/ugc/aweme/comment/model/CommentKeyboardModel;",
            "Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;",
        ],
        required_strings=["getText", "comment_sort_opt_style"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="display_refresh_rate_governor",
        name="Smooth Screen (120Hz)",
        target_type="bytecode",
        description="Forces TikTok to run at peak display refresh rate (120Hz/90Hz/60Hz) and neutralizes video playback framerate downclocking routines.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/main/MainActivity;",
        ],
        required_strings=["onResume", "onWindowFocusChanged"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="custom_offline_videos_limit",
        name="Offline Video Limit",
        target_type="bytecode",
        description="Customizes the maximum number of videos available for offline download caching.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/offlinemode/ui/sheet/OfflineModeSheetPageAssem;",
        ],
        required_strings=["onAssemPostCreate"],
        criticality="HIGH",
    ),

    PatchContract(
        patch_id="disable_story_feed_indicators",
        name="Story rings",
        target_type="bytecode",
        description="Removes creator profile photo story rings from feed videos, ensuring avatar photos remain clean without blue story rings.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/profile/model/User;",
            "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarSocialPublishAssem;",
            "Lcom/ss/android/ugc/aweme/service/SocPubDistributeServiceImpl;",
        ],
        required_strings=["getStoryStatus"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="feed_navigation_declutter",
        name="Clean Top Bar",
        target_type="bytecode",
        description="Removes clutter from the feed navigation and top header bar, including the Nearby feed tab, Community (Explore) tab, top-left LIVE broadcast button, and in-video bottom search suggestion bar.",
        required_classes=[
            "Lcom/ss/android/ugc/nearby/service/NearbyServiceImpl;",
            "Lcom/ss/android/ugc/aweme/explore/service/ExploreFeedServiceImpl;",
            "Lcom/bytedance/tiktok/homepage/mainfragment/toolbar/LiveIconGenerator;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssem;",
        ],
        required_strings=[],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="force_auto_scroll",
        name="Auto-Scroll Videos",
        target_type="bytecode",
        description="Forces the activation of the native video auto-scroll experiment flag for accounts and regions that lack it due to A/B testing.",
        required_classes=[
            "Lcom/ss/android/ugc/feed/platform/panel/autoscroll/FypAutoScrollServiceImpl;",
            "Lcom/ss/android/ugc/aweme/feed/bottombar/FeedBottomBarFacadeImpl;",
        ],
        required_strings=[
            "fyp_auto_scroll",
            "tablet_fyp_auto_scroll",
            "search_auto_scroll",
            "createAutoScrollItem, add auto scroll item to the menu",
        ],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="disable_search_history_recording",
        name="No Search History",
        target_type="bytecode",
        description="Prevents search queries and keywords from being recorded in local history, databases, and analytics stores.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/search/pages/middlepage/history/ManualSearchPvStore;",
            "Lcom/ss/android/ugc/aweme/discover/model/suggest/SuggestWordResponse;",
        ],
        required_strings=["getTopHistoryWords"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="disable_watch_history_recording",
        name="No Watch History",
        target_type="bytecode",
        description="Prevents viewed videos from being recorded in account watch history, playback duration stores, and local history caches.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/api/AwemeStatsApi;",
            "LX/03l3;",
            "LX/0aZr;",
        ],
        required_strings=["/aweme/v1/aweme/stats/"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="disable_double_tap_to_like",
        name="No Accidental Likes",
        target_type="bytecode",
        description="Disables the double tap gesture to like videos in the feed, preventing accidental likes while scrolling or pausing. Videos can still be liked using the like button.",
        required_classes=[
            "Lcom/ss/android/ugc/feed/platform/panel/digg/DiggPanelComponent;",
            "Lcom/ss/android/ugc/aweme/feed/landscape/LandscapeFragmentPanel;",
            "Lcom/ss/android/ugc/aweme/friendstab/ui/feed/cell/component/base/FriendsV3GestureDetectorAssem;",
        ],
        required_strings=["handleDoubleClick"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="hide_suggested_searches",
        name="No Suggested Searches",
        target_type="bytecode",
        description="Removes the 'Podría interesarte' (You may like / Guess Search) suggested keywords section from the search intermediate page.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/discover/model/suggest/RecomDataWrapper;",
            "Lcom/ss/android/ugc/aweme/search/middle/DynamicSingleIntermediateFragmentNew;",
        ],
        required_strings=["pZ"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="hide_popular_lives_in_search",
        name="No Lives in Search",
        target_type="bytecode",
        description="Removes the 'LIVE populares' (Popular LIVEs) recommendation card and stream broadcasts from the search intermediate page.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/discover/model/suggest/RecomDataWrapper;",
        ],
        required_strings=[],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="hide_ai_tagged_content",
        name="Hide AI Videos",
        target_type="bytecode",
        description="Filters and skips videos tagged with native AI-generated metadata, C2PA content credentials, or creator AI disclosure tags across the For You, Following, and Friends feeds.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
            "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            "Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;",
            "Lcom/ss/android/ugc/aweme/feed/AIGCInfo;",
        ],
        required_strings=[],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="auto_pause_first_video",
        name="Pause First Video",
        target_type="bytecode",
        description="Automatically pauses the first video when opening TikTok to allow background initialization.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;",
        ],
        required_strings=[],
        criticality="MEDIUM",
    ),
    PatchContract(
        patch_id="disable_avatar_live_status",
        name="No LIVE Rings",
        target_type="bytecode",
        description="Removes the pulsing LIVE ring badge on creator profile avatars in the video feed.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarLiveAssem;",
        ],
        required_strings=[],
        criticality="MEDIUM",
    ),
    PatchContract(
        patch_id="hide_avatar_follow_button",
        name="Follow badge",
        target_type="bytecode",
        description="Hides the plus (+) follow badge on creator profile avatars in the feed.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarDefaultAssem;",
        ],
        required_strings=[],
        criticality="MEDIUM",
    ),
]
