"""
Declarative patch contracts and structural invariants for Xiaomi Earbuds patches.
"""

from __future__ import annotations
from dataclasses import dataclass
from typing import List


@dataclass
class PatchContract:
    patch_id: str
    name: str
    target_type: str  # 'bytecode', 'resource', 'composite'
    description: str
    required_classes: List[str]
    required_strings: List[str]
    criticality: str = "HIGH"  # 'CRITICAL', 'HIGH', 'MEDIUM'


XIAOMI_EARBUDS_PATCH_CONTRACTS: List[PatchContract] = [
    PatchContract(
        patch_id="anti_tamper_bypass",
        name="Anti-Tamper Bypass",
        target_type="bytecode",
        description="Neutralizes anti-tamper, hook detection (Xposed/maps), VPN/ADB detection, and root/emulator checks.",
        required_classes=["Lg9/a;", "Lg9/b;", "Lg9/c;", "Lg9/d;", "Lg9/e;"],
        required_strings=["a", "b", "c"],
        criticality="CRITICAL",
    ),
    PatchContract(
        patch_id="background_optimizer",
        name="Background Optimizer",
        target_type="composite",
        description="Neutralizes persistent foreground keepalive, nearby BLE scanning, and companion services.",
        required_classes=[
            "Lcom/mi/earphone/keepalive/KeepAliveForegroundService;",
            "Lcom/mi/earphone/keepalive/KeepAliveImpl;",
            "Lcom/mi/earphone/keepalive/google/ConnectivityCompanionDeviceService;",
        ],
        required_strings=["startForegroundServiceInternal", "isStarted", "setActiveDevice"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="block_telemetry",
        name="Block Telemetry & Trackers",
        target_type="composite",
        description="Disables Xiaomi OneTrack, crash dump providers, bug reports, and analytics dispatchers.",
        required_classes=[
            "Lcom/mi/earphone/statistics/OnetrackImpl;",
            "Lcom/xiaomi/accountsdk/account/stat/OneTrackStatImpl;",
            "Lcom/xiaomi/passport/ui/onetrack/Analytics;",
        ],
        required_strings=["reportData", "track"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="device_privacy_guard",
        name="Device Privacy Guard",
        target_type="composite",
        description="Anonymizes device IDs, blinded hardware privacy getters, and removes location requirements.",
        required_classes=[
            "Lcom/xiaomi/account/privacy_data/master/PrivacyDataMaster;",
            "Lcom/xiaomi/accountsdk/hasheddeviceidlib/AccountDeviceIdManager;",
            "Lcom/xiaomi/passport/PassportUserEnvironment;",
            "Lcom/mi/earphone/bluetoothsdk/util/CheckPermissionUtilKt;",
            "Lcom/mi/earphone/device/manager/ui/add/AddDeviceFragment;",
        ],
        required_strings=["getDeviceId", "getHashedDeviceId", "syncGetDeviceId", "getEnvInfoArray", "getLocationPermissions", "hasScanCondition"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="disable_promos_and_nags",
        name="Disable Promos & Nags",
        target_type="bytecode",
        description="Bypasses startup agreements, onboarding guides, region prompts, banners, and Bluetooth permission modal nags.",
        required_classes=[
            "Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;",
            "Lcom/mi/earphone/device/manager/ui/devicelist/DeviceListFragment;",
            "Lcom/mi/earphone/settings/ui/cloudconfig/InviteReviewManager;",
            "Lcom/xiaomi/fitness/account/region/RegionPreference;",
            "Lcom/xiaomi/fitness/login/preference/ModePreference;",
        ],
        required_strings=["loadBanner", "loadActivities", "requestBluetoothPermissionForS", "isShowDialog", "getPRIVACY_AGREE", "getWelcomeFinish"],
        criticality="CRITICAL",
    ),
    PatchContract(
        patch_id="guest_ota_unlock",
        name="Guest OTA Unlock",
        target_type="bytecode",
        description="Bypasses mandatory Xiaomi account login checks for firmware updates.",
        required_classes=[
            "Lcom/mi/earphone/settings/di/SettingListItemModule$provideUpgradeSettingItem$1;",
            "Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;",
        ],
        required_strings=["invoke", "initFunctionListener$lambda$26"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="network_security",
        name="Network Security & TLS Inspection",
        target_type="composite",
        description="Configures network security config for HTTPS and neutralizes OkHttp CertificatePinner.",
        required_classes=[
            "Lokhttp3/CertificatePinner;",
        ],
        required_strings=["check"],
        criticality="MEDIUM",
    ),
    PatchContract(
        patch_id="offline_only",
        name="Offline Only",
        target_type="composite",
        description="Completely isolates app from network, revokes internet permissions, and spoofs offline status.",
        required_classes=[
            "Lcom/xiaomi/accountsdk/request/NetworkUtils;",
            "Lcom/xiaomi/fitness/common/utils/NetworkStateUtil;",
            "Lokhttp3/Dns$Companion$DnsSystem;",
        ],
        required_strings=["isNetworkConnected", "updateNetworkState", "lookup"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="model_catalog_unlock",
        name="Model Catalog Unlock",
        target_type="bytecode",
        description="Forces DeviceInfoListCache.isShowProduct to return true, preventing version gating from hiding device models.",
        required_classes=[
            "Lcom/mi/earphone/device/manager/cache/DeviceInfoListCache;",
        ],
        required_strings=["isShowProduct"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="sound_features_unlock",
        name="Sound Features Unlock",
        target_type="bytecode",
        description="Unlocks Spatial Audio on aptX Adaptive 96kHz and bypasses server restriction items.",
        required_classes=[
            "Lcom/mi/earphone/device/manager/manager/DeviceManagerImpl;",
            "Lcom/mi/earphone/settings/ui/spatialaudio/SpatialAudioVM;",
            "Lcom/mi/earphone/settings/ui/soundeffect/SoundEffectVM;",
        ],
        required_strings=["queryRestrictItem", "isNotSupport96K", "isEnableSpatialAudio"],
        criticality="HIGH",
    ),
    PatchContract(
        patch_id="surgical_oem_unlock",
        name="Surgical OEM Unlock",
        target_type="bytecode",
        description="Bypasses Xiaomi OEM hardware gating for SuperAivs (5009) and suppresses MIUI Spatial Audio dialog nags.",
        required_classes=[
            "Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;",
            "Lcom/mi/earphone/settings/ui/spatialaudio/SpatialAudioVM;",
        ],
        required_strings=["initFunctionItem", "isShowDialog"],
        criticality="HIGH",
    ),
]
