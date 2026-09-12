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
        description="Strips embedded Microblink card/document OCR scanning models, C2PA AI content origin verification libraries, and non-Latin fonts.",
        required_classes=[],
        required_strings=["assets/microblink", "libtt_c2pa_sdk.so", "assets/fonts"],
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
        patch_id="disable_in_app_update_nags",
        name="Update Prompt Suppressor",
        target_type="bytecode",
        description="Neutralizes background update polling tasks and device ID check routines to prevent forced update popups.",
        required_classes=["Lcom/ss/android/ugc/aweme/legoImp/task/CheckUpdateChangeDeviceIDTaskHolder$Background;"],
        required_strings=[],
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
        patch_id="runtime_memory_governor",
        name="Runtime Memory Governor",
        target_type="bytecode",
        description="Caps Fresco animated and frame bitmap memory cache retention to reduce RAM footprint.",
        required_classes=[
            "Lcom/facebook/fresco/animation/bitmap/cache/FrescoFrameCache;",
        ],
        required_strings=["LIZLLL", "LJ"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="network_traffic_governor",
        name="Network & Background Traffic Governor",
        target_type="bytecode",
        description="Throttles aggressive video preload buffering and disables background push polling alarms to save cellular data and battery.",
        required_classes=[
            "Lcom/ss/android/ugc/aweme/video/preload/model/PreloadStrategyConfig;",
            "Lcom/ss/android/ugc/aweme/legoImp/task/InitPushTask;",
        ],
        required_strings=["isEnableBufferPreload"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="live_stream_suite_optimizer",
        name="Live Stream Suite Optimizer",
        target_type="raw_resource",
        description="Strips multi-mic live interaction native SDKs (liblink_mic_sdk.so) and live battle minigames, and silences 3D gift particle animations.",
        required_classes=["Lcom/bytedance/android/livesdk/gift/base/platform/business/effect/LiveGiftEffectWidget;"],
        required_strings=["liblink_mic_sdk.so"],
        criticality="HIGH",
    ),
]
