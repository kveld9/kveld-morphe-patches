"""
Formal Declarative Contracts for Gboard Patches.
Specifies target classes, fingerprints, required invariants, and forbidden regressions.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Callable, List, Optional, Tuple

from harness.core.fingerprints import FingerprintQuery


@dataclass
class GboardPatchContract:
    patch_id: str
    name: str
    description: str
    source_file: str
    queries: List[FingerprintQuery]
    required_assets: List[str] = field(default_factory=list)
    semantic_invariants: List[str] = field(default_factory=list)
    forbidden_regressions: List[str] = field(default_factory=list)


def get_all_gboard_contracts() -> List[GboardPatchContract]:
    """Returns the formal contract list for all 18 Gboard patches."""
    return [
        GboardPatchContract(
            patch_id="gboard_amoled",
            name="Add AMOLED Theme",
            description="Adds Pure Black AMOLED theme using color_black theme package.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardAmoledPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="amoled_theme_listing",
                    defining_class="Lcom/google/android/apps/inputmethod/libs/theme/listing/ThemeListingFragment;",
                    method_name="f",
                    return_type="V",
                    parameters=["Landroid/os/Bundle;"],
                ),
            ],
            required_assets=[
                "assets/theme/style_sheet_color_black.binarypb",
                "assets/theme/style_sheet_color_common.binarypb",
            ],
            semantic_invariants=[
                "Exactly 1 AMOLED theme entry injected into ThemeListingFragment list",
                "Rewrites style_sheet_color_black.binarypb and style_sheet_color_common.binarypb palette bytes",
            ],
            forbidden_regressions=[
                "Duplicate AMOLED theme entries in ThemeListingFragment",
                "Overwriting Dark/Light/Dynamic color theme definitions",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_signature_bypass",
            name="Allow Modified APK",
            description="Bypasses internal signature check to allow custom APK execution.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardSignatureBypassPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="signature_check_method",
                    return_type="V",
                    strings=["APK is signed by unrecognized certificates: "],
                ),
            ],
            semantic_invariants=[
                "Returns immediately (return-void) before throwing SecurityException",
            ],
            forbidden_regressions=[
                "Altering signature validation on unrelated package verification routines",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_block_telemetry",
            name="Block Telemetry",
            description="Disables background metrics dispatch, daily pings, and crash reporting.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardBlockTelemetryPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="primes_lifeboat_receiver",
                    defining_class="Lcom/google/android/libraries/performance/primes/transmitter/LifeboatReceiver;",
                    method_name="onReceive",
                    return_type="V",
                    parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
                ),
                FingerprintQuery(
                    name_id="daily_ping_worker",
                    defining_class="Lcom/google/android/libraries/inputmethod/dailyping/DailyPingWorker;",
                    method_name="c",
                    return_type="Lagjs;",
                    parameters=[],
                ),
            ],
            semantic_invariants=[
                "LifeboatReceiver.onReceive returns immediately",
                "DailyPingWorker returns Result.success() without network dispatch",
            ],
            forbidden_regressions=[
                "Breaking ListenableWorker lifecycle contracts",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_disable_diagnostics",
            name="Disable Diagnostics",
            description="Disables Google's diagnostic and recovery telemetry.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardDisableAppDoctorPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="app_doctor_init",
                    defining_class="Lcom/google/android/libraries/inputmethod/appdoctor/initializer/AppDoctorInitializer;",
                    method_name="a",
                    return_type="Ljava/lang/Object;",
                    parameters=["Landroid/content/Context;"],
                ),
                FingerprintQuery(
                    name_id="app_doctor_receiver",
                    defining_class="Lcom/google/android/libraries/appdoctor/AppDoctorReceiver;",
                    method_name="onReceive",
                    return_type="V",
                    parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
                ),
            ],
            semantic_invariants=[
                "AppDoctorInitializer returns input without starting background telemetry",
            ],
            forbidden_regressions=[
                "Returning null on non-nullable Initializer return",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_disable_primes",
            name="Disable Google Primes",
            description="Neutralizes Google Primes performance profiling and crash monitoring.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardDisablePrimesPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="native_crash_handler",
                    defining_class="Lcom/google/android/libraries/performance/primes/metrics/crash/NativeCrashHandlerImpl;",
                    method_name="a",
                    return_type="V",
                ),
                FingerprintQuery(
                    name_id="primes_lifeboat",
                    defining_class="Lcom/google/android/libraries/performance/primes/transmitter/LifeboatReceiver;",
                    method_name="onReceive",
                    return_type="V",
                    parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
                ),
            ],
            semantic_invariants=[
                "Neutralizes NativeCrashHandler initialization",
                "Returns main Looper handler instead of Primes executor",
            ],
            forbidden_regressions=[
                "Crashing main UI thread during Primes provider startup",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_disable_in_app_training",
            name="Disable In-App Training",
            description="Neutralizes federated learning and training cache generation.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardDisableInAppTrainingPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="latin5_periodic_worker",
                    defining_class="Lcom/google/android/apps/inputmethod/libs/latin5/PeriodicTaskWorker;",
                    method_name="k",
                    return_type="V",
                    parameters=["Landroid/content/Context;"],
                ),
            ],
            semantic_invariants=[
                "PeriodicTaskWorker.k returns immediately without scheduling federated learning",
            ],
            forbidden_regressions=[
                "Disabling active on-screen keyboard predictions",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_disable_mdd_sync",
            name="Disable MDD Background Sync",
            description="Neutralizes Mobile Data Download automated background polling.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardDisableMddBackgroundSyncPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="mdd_task_scheduler_worker",
                    defining_class="Lcom/google/android/libraries/inputmethod/mdd/MDDTaskScheduler$Worker;",
                    method_name="c",
                    return_type="Lagjs;",
                    parameters=[],
                ),
                FingerprintQuery(
                    name_id="mdd_metadata_cleanup_worker",
                    defining_class="Lcom/google/android/libraries/inputmethod/mdd/cleanup/MddMetadataCleanupWorker;",
                    method_name="k",
                    return_type="Lciu;",
                    parameters=[],
                ),
                FingerprintQuery(
                    name_id="mdd_foreground_download_worker",
                    defining_class="Lcom/google/android/libraries/inputmethod/mdd/ForegroundDownloadTaskWorker;",
                    method_name="c",
                    return_type="Lagjs;",
                    parameters=[],
                ),
            ],
            semantic_invariants=[
                "MDD workers return success immediately without polling remote servers",
            ],
            forbidden_regressions=[
                "Breaking on-demand user-initiated language downloads",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_disable_phenotype",
            name="Disable Remote Configuration",
            description="Disables periodic remote experiment flag synchronization.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardDisablePhenotypeSyncPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="phenotype_update_bg_receiver",
                    defining_class="Lcom/google/android/libraries/phenotype/client/stable/PhenotypeUpdateBackgroundBroadcastReceiver;",
                    method_name="onReceive",
                    return_type="V",
                    parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
                ),
                FingerprintQuery(
                    name_id="phenotype_account_removed_receiver",
                    defining_class="Lcom/google/android/libraries/phenotype/client/stable/AccountRemovedBroadcastReceiver;",
                    method_name="onReceive",
                    return_type="V",
                    parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
                ),
            ],
            semantic_invariants=[
                "Phenotype background broadcast receivers return void without sync",
            ],
            forbidden_regressions=[
                "Corrupting local phenotype SQLite cache tables",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_disable_superpacks_eager_sync",
            name="Disable Superpacks Eager Sync",
            description="Neutralizes eager Superpacks background sync on startup.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardDisableSuperpacksEagerSyncPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="superpacks_sync_task_1",
                    defining_class="Lgvk;",
                    method_name="n",
                    return_type="V",
                    parameters=[],
                ),
                FingerprintQuery(
                    name_id="superpacks_sync_task_2",
                    defining_class="Lgrp;",
                    method_name="n",
                    return_type="V",
                    parameters=[],
                ),
            ],
            semantic_invariants=[
                "Eager synchronization tasks return early on startup",
            ],
            forbidden_regressions=[
                "Disabling Superpacks pack unpacking when explicitly requested",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_disable_tenor_tracking",
            name="Disable Tenor Share Tracking",
            description="Disables Tenor GIF selection and share tracking telemetry.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardDisableTenorRegisterSharePatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="tenor_share_tracker",
                    defining_class="Limg;",
                    method_name="K",
                    parameters=["Lafsc;", "Lidb;"],
                    return_type="V",
                ),
            ],
            semantic_invariants=[
                "GIF share telemetry reporting returns void immediately",
            ],
            forbidden_regressions=[
                "Breaking GIF insertion into editor text fields",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_disable_workmanager",
            name="Disable WorkManager",
            description="Neutralizes background WorkManager initialization and workers.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardDisableWorkManagerPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="workmanager_initializer",
                    defining_class="Landroidx/work/WorkManagerInitializer;",
                    method_name="a",
                    return_type="Ljava/lang/Object;",
                    parameters=["Landroid/content/Context;"],
                ),
                FingerprintQuery(
                    name_id="workmanager_system_job_service_onstart",
                    defining_class="Landroidx/work/impl/background/systemjob/SystemJobService;",
                    method_name="onStartJob",
                    return_type="Z",
                    parameters=["Landroid/app/job/JobParameters;"],
                ),
                FingerprintQuery(
                    name_id="workmanager_reschedule_receiver",
                    defining_class="Landroidx/work/impl/background/systemalarm/RescheduleReceiver;",
                    method_name="onReceive",
                    return_type="V",
                    parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
                ),
            ],
            semantic_invariants=[
                "WorkManagerInitializer returns input without building SQLite DB",
                "SystemJobService returns false (job finished immediately)",
            ],
            forbidden_regressions=[
                "Throwing IllegalStateException on getWorkManager calls",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_access_points_redesign",
            name="Enable Access Points Menu Redesign",
            description="Enables the redesigned access points menu bar and customization panel.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardEnableAccessPointsMenuRedesignPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="access_points_flag_clinit",
                    defining_class="Lpxs;",
                    method_name="<clinit>",
                    return_type="V",
                    strings=["enable_access_points_menu_redesign"],
                ),
            ],
            semantic_invariants=[
                "Forces enable_access_points_menu_redesign flag to true",
            ],
            forbidden_regressions=[
                "Hiding access points bar completely",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_key_shape_selection",
            name="Enable Key Shape Selection",
            description="Enables key border shape selection UI in theme customization.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardEnableKeyShapeSelectionPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="key_shape_predicate",
                    defining_class="Lxgy;",
                    method_name="i",
                    return_type="Z",
                    parameters=["Landroid/content/Context;"],
                ),
            ],
            semantic_invariants=[
                "Returns true for key shape border capability check",
            ],
            forbidden_regressions=[
                "Crashing theme selector activity",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_force_incognito",
            name="Force Incognito Mode",
            description="Forces Gboard to always operate in incognito mode.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardForceIncognitoPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="incognito_editor_check",
                    defining_class="Lsew;",
                    method_name="H",
                    return_type="Z",
                    parameters=["Landroid/view/inputmethod/EditorInfo;"],
                ),
                FingerprintQuery(
                    name_id="incognito_flag_check",
                    defining_class="Lfoh;",
                    method_name="F",
                    return_type="Z",
                    parameters=[],
                ),
            ],
            semantic_invariants=[
                "Returns true for all incognito mode queries",
            ],
            forbidden_regressions=[
                "Disabling keyboard dictionary lookups completely",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_hardened_intent_security",
            name="Hardened Intent Security",
            description="Enables internal external intent protection against unauthorized hijacking.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardHardenedIntentSecurityPatch.kt",
            queries=[
                FingerprintQuery(
                    name_id="intent_security_flag_clinit",
                    defining_class="Luev;",
                    method_name="<clinit>",
                    return_type="V",
                    strings=["prevent_external_intents"],
                ),
            ],
            semantic_invariants=[
                "Forces prevent_external_intents flag to true",
            ],
            forbidden_regressions=[
                "Blocking legitimate OS IME switcher intents",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_clone",
            name="Clone Gboard",
            description="Changes the package name to allow installing Gboard alongside the original application.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardClonePatch.kt",
            queries=[],
            semantic_invariants=[
                "Rewrites manifest package name to isolated custom package identity",
                "Rewrites provider authorities and permissions to prevent OS collision",
                "Fully qualifies relative component declarations against base package",
            ],
            forbidden_regressions=[
                "Colliding with stock Gboard package authority namespaces",
                "Unqualified relative component references causing ClassNotFoundException",
            ],
        ),
        GboardPatchContract(
            patch_id="gboard_resource_slimmer",
            name="Resource Slimmer",
            description="Strips embedded third-party license text, onboarding tutorial Lottie animations, and promotional GIFs.",
            source_file="patches/src/main/kotlin/app/morphe/patches/gboard/GboardResourceSlimmerPatch.kt",
            queries=[],
            semantic_invariants=[
                "Replaces GIF and Lottie tutorial assets with empty valid stubs",
                "Replaces third-party license text blocks with minimal headers",
            ],
            forbidden_regressions=[
                "Corrupting essential layout XMLs or binary drawables",
            ],
        ),
    ]
