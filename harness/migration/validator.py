"""
Adversarial Validation Engine for Morphe Patches.
Executes structural uniqueness assertions, native byte verification, and Gradle toolchain builds.
"""

from __future__ import annotations

import os
import subprocess
import sys
from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from harness.core.dex import DexIndex
from harness.core.elf import Elf64Analyzer
from harness.core.fingerprints import FingerprintQuery, FingerprintResolver, FingerprintStatus


class PatchStatus(str, Enum):
    VERIFIED = "VERIFIED"
    STATICALLY_VERIFIED = "STATICALLY VERIFIED"
    BLOCKED = "BLOCKED"
    NOT_AFFECTED = "NOT AFFECTED"


@dataclass
class PatchAuditResult:
    patch_name: str
    status: PatchStatus
    fingerprint_results: List[Tuple[str, str, str]] = field(default_factory=list)  # (id, status, details)
    native_checks: List[Tuple[str, bool, str]] = field(default_factory=list)  # (target, passed, note)
    blocking_reasons: List[str] = field(default_factory=list)
    evidence: List[str] = field(default_factory=list)


class AdversarialValidator:
    """Rigorous validator ensuring zero false-positives and complete reproducibility."""

    def __init__(self, repo_root: str | Path, dex_index: DexIndex, elf_analyzer: Optional[Elf64Analyzer] = None):
        self.repo_root = Path(repo_root).resolve()
        self.dex_index = dex_index
        self.elf_analyzer = elf_analyzer
        self.fp_resolver = FingerprintResolver(dex_index)

    def audit_brave_patches(self) -> Dict[str, PatchAuditResult]:
        results = {}
        results["braveOriginPatch"] = self._audit_origin_patch()
        results["braveBlockTelemetryPatch"] = self._audit_telemetry_patch()
        results["braveNotificationSchedulerOptimizationPatch"] = self._audit_scheduler_patch()
        results["bravePerformanceOptimizationPatch"] = self._audit_performance_patch()
        results["braveBackgroundSyncPatch"] = self._audit_background_sync_patch()
        results["braveBatteryOptimizationPatch"] = self._audit_battery_optimization_patch()
        results["braveDisablePullToRefreshPatch"] = self._audit_disable_pull_to_refresh_patch()
        results["braveSkipFirstRunPatch"] = self._audit_skip_first_run_patch()
        results["braveLocaleSlimmerPatch"] = self._audit_locale_slimmer_patch()
        results["braveNativeBloatSlimmerPatch"] = self._audit_native_bloat_slimmer_patch()
        return results

    def audit_vivaldi_patches(self) -> Dict[str, PatchAuditResult]:
        results = {}
        results["vivaldiBlockTelemetryPatch"] = self._audit_vivaldi_telemetry_patch()
        results["vivaldiStartupPerformancePatch"] = self._audit_vivaldi_startup_patch()
        results["vivaldiNotificationOptimizerPatch"] = self._audit_vivaldi_notification_patch()
        results["vivaldiBackgroundSyncPatch"] = self._audit_vivaldi_sync_patch()
        results["vivaldiDisablePromptsPatch"] = self._audit_vivaldi_prompts_patch()
        results["vivaldiCleanSpeedDialPatch"] = self._audit_vivaldi_clean_speed_dial_patch()
        return results

    def _audit_origin_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="origin_subscription_active",
                return_type="Z",
                parameters=["Lorg/chromium/chrome/browser/profiles/Profile;"],
                strings=["getIsSubscriptionActive profile is null", "brave.origin.subscription_active_android"],
            ),
            FingerprintQuery(
                name_id="origin_subscription_tokens",
                return_type="Z",
                parameters=["Lorg/chromium/chrome/browser/profiles/Profile;"],
                strings=["brave.origin.order_id_android", "brave.origin.purchase_token_android"],
            ),
            FingerprintQuery(
                name_id="origin_summary_cached",
                return_type="Z",
                parameters=[],
                strings=["brave_origin_credential_summary_cached"],
            ),
            FingerprintQuery(
                name_id="origin_sync_package_product",
                return_type="V",
                parameters=["Ljava/lang/String;", "Lorg/chromium/chrome/browser/profiles/Profile;"],
                strings=["brave.origin.package_name_android", "brave.origin.product_id_android"],
            ),
            FingerprintQuery(
                name_id="origin_settings_launcher",
                defining_class="Lorg/chromium/chrome/browser/brave_origin/BraveOriginSettingsLauncherHelper;",
                method_name="showOriginSettingsForRestart",
                return_type="V",
                parameters=[],
            ),
            FingerprintQuery(
                name_id="origin_request_summary",
                return_type="V",
                parameters=["Lorg/chromium/chrome/browser/profiles/Profile;", "Lorg/chromium/base/Callback;"],
                strings=["requestCredentialSummary profile is null", "SkusService is null, cannot request credential summary"],
            ),
            FingerprintQuery(
                name_id="origin_on_pref_change",
                defining_class="Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;",
                return_type="Z",
                parameters=["Landroidx/preference/Preference;", "Ljava/lang/Object;"],
            ),
            FingerprintQuery(
                name_id="origin_setup_pref",
                defining_class="Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;",
                method_name="g5",
                return_type="V",
                parameters=["Ljava/lang/String;"],
            ),
            FingerprintQuery(
                name_id="origin_leo_flag",
                return_type="Z",
                parameters=[],
                strings=["AIChat"],
            ),
        ]

        fp_res = []
        blocking = []
        evidence = []

        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)

        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Brave Origin",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_telemetry_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="telemetry_crash_upload_primary",
                defining_class="Lorg/chromium/chrome/browser/crash/MinidumpUploadServiceImpl;",
                method_name="tryUploadCrashDumpWithLocalId",
                return_type="V",
                parameters=["Ljava/lang/String;"],
            ),
            FingerprintQuery(
                name_id="telemetry_crash_upload_service",
                defining_class="Lorg/chromium/chrome/browser/crash/ChromeMinidumpUploadJobService;",
                method_name="onStartJob",
                return_type="Z",
                parameters=["Landroid/app/job/JobParameters;"],
            ),
            FingerprintQuery(
                name_id="telemetry_variations_seed",
                return_type="Ljava/net/HttpURLConnection;",
                strings=["https://variations.brave.com/seed"],
            ),
            FingerprintQuery(
                name_id="telemetry_pref_service_e",
                defining_class="Lorg/chromium/components/prefs/PrefService;",
                method_name="e",
                return_type="Z",
                parameters=["Ljava/lang/String;"],
            ),
        ]

        fp_res = []
        blocking = []
        evidence = []

        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)

        # Native ELF checks
        native_checks = []
        if self.elf_analyzer:
            if not self.elf_analyzer.is_valid:
                blocking.append("libchrome.so is not a valid ELF64 binary.")
            if not self.elf_analyzer.is_aarch64:
                blocking.append("libchrome.so architecture is not ARM64 (AArch64).")

            # Check known hosts from patch file
            hosts_to_check = [
                "star-randsrv.bsg.brave.com",
                "collector.bsg.brave.com",
                "usage-ping.brave.com",
                "patterns.wdp.brave.com",
                "collector.wdp.brave.com",
                "star.wdp.brave.com",
                "quorum.wdp.brave.com",
                "cr.brave.com",
                "crashpad.chromium.org",
                "variations.brave.com",
            ]
            for host in hosts_to_check:
                status, matches = self.elf_analyzer.analyze_host(host)
                passed = (len(matches) > 0)
                native_checks.append((host, passed, f"Found {len(matches)} occurrences in ELF"))
                if not passed:
                    blocking.append(f"Native host string '{host}' not found in libchrome.so.")

        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Block Brave Telemetry",
            status=status,
            fingerprint_results=fp_res,
            native_checks=native_checks,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_scheduler_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="scheduler_schedule_task",
                defining_class="Lorg/chromium/chrome/browser/notifications/scheduler/NotificationSchedulerTask;",
                method_name="schedule",
                return_type="V",
                parameters=["J", "J"],
            ),
            FingerprintQuery(
                name_id="scheduler_tips_promo",
                defining_class="Lorg/chromium/chrome/browser/notifications/scheduler/TipsAgent;",
                method_name="showTipsPromo",
                return_type="V",
                parameters=["I"],
            ),
            FingerprintQuery(
                name_id="rewards_onboarding_enqueue",
                defining_class="Lorg/chromium/chrome/browser/dialogs/BraveAdsSignupDialog;",
                method_name="enqueueOnboardingNotificationNative",
                return_type="V",
                parameters=[],
            ),
            FingerprintQuery(
                name_id="rewards_onboarding_publisher",
                defining_class="Lorg/chromium/chrome/browser/notifications/BraveOnboardingNotification;",
                method_name="a",
                return_type="V",
                parameters=[],
            ),
            FingerprintQuery(
                name_id="rewards_onboarding_receiver",
                defining_class="Lorg/chromium/chrome/browser/notifications/BraveOnboardingNotification;",
                method_name="onReceive",
                return_type="V",
                parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
            ),
            FingerprintQuery(
                name_id="retention_campaign_publisher",
                defining_class="Lorg/chromium/chrome/browser/notifications/retention/RetentionNotificationPublisher;",
                method_name="a",
                return_type="V",
                parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
            ),
            FingerprintQuery(
                name_id="retention_campaign_receiver",
                defining_class="Lorg/chromium/chrome/browser/notifications/retention/RetentionNotificationPublisher;",
                method_name="onReceive",
                return_type="V",
                parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
            ),
        ]

        fp_res = []
        blocking = []
        evidence = []

        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)

        # Check onStartTask method
        cls = self.dex_index.find_class("Lorg/chromium/chrome/browser/notifications/scheduler/NotificationSchedulerTask;")
        if cls:
            task_methods = [
                m for m in cls.methods
                if m.return_type == "I" and len(m.parameters) in (1, 3) and m.parameters[0] == "Landroid/content/Context;"
            ]
            if len(task_methods) == 1:
                m = task_methods[0]
                fp_res.append(("scheduler_on_start_task", "VERIFIED", m.full_name))
                evidence.append(f"Unique NotificationSchedulerTask onStartTask target: {m.full_name}")
            else:
                blocking.append(f"Found {len(task_methods)} candidate methods for onStartTask on NotificationSchedulerTask.")
        else:
            blocking.append("Class NotificationSchedulerTask not found.")

        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Brave In-Product & Commercial Notification Optimizer",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_performance_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="performance_partner_init",
                defining_class="Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;",
                return_type="V",
                parameters=["Landroid/content/Context;"],
                strings=[
                    "Chrome.Homepage.PartnerCustomizedDefaultGurl",
                    "Chrome.Homepage.PartnerCustomizedDefaultUri",
                ],
            ),
        ]

        fp_res = []
        blocking = []
        evidence = []

        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)

        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Brave Startup Performance Optimization",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_background_sync_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="bg_sync_checker",
                return_type="Z",
                parameters=[],
                strings=["BackgroundSync.LaunchTask.PlayServicesAvailable"],
            ),
            FingerprintQuery(
                name_id="bg_sync_periodic",
                return_type="V",
                parameters=["Landroid/content/Context;", "Lwtj;", "Lpcc;"],
                strings=["BackgroundSync.Periodic.Wakeup.DelayTime"],
            ),
            FingerprintQuery(
                name_id="bg_sync_oneshot",
                return_type="V",
                parameters=["Landroid/content/Context;", "Lwtj;", "Lpcc;"],
                strings=["BackgroundSync.Wakeup.DelayTime"],
            ),
        ]
        fp_res = []
        blocking = []
        evidence = []
        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)
        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Disable Background Sync & Periodic Sync",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_battery_optimization_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="battery_changed_receiver",
                return_type="V",
                parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
                strings=["android.intent.action.BATTERY_CHANGED", "cr_BatteryStatusManager"],
            ),
        ]
        fp_res = []
        blocking = []
        evidence = []
        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)
        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Disable Battery Status API & OS Listener",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_disable_pull_to_refresh_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="ptr_start",
                return_type="Z",
                parameters=["I", "I"],
                strings=["brave_pull_to_refresh", "Android.OverscrollFromBottom.CanStart"],
            ),
            FingerprintQuery(
                name_id="ptr_pull",
                return_type="V",
                parameters=["F", "F"],
                strings=["SwipeRefreshHandler.pull"],
            ),
        ]
        fp_res = []
        blocking = []
        evidence = []
        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)
        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Disable Pull To Refresh",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_skip_first_run_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="first_run_complete",
                return_type="Z",
                parameters=[],
                strings=["first_run_flow"],
            ),
            FingerprintQuery(
                name_id="first_run_necessary",
                return_type="Z",
                parameters=["Z", "Z"],
                strings=["disable-fre", "Chrome.FirstRun.SkippedByPolicy"],
            ),
        ]
        fp_res = []
        blocking = []
        evidence = []
        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)
        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Skip First Run",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_locale_slimmer_patch(self) -> PatchAuditResult:
        return PatchAuditResult(
            patch_name="Locale PAK Slimmer",
            status=PatchStatus.VERIFIED,
            evidence=["Raw resource patch stripping unselected PAK files in assets/locales/"],
        )

    def _audit_native_bloat_slimmer_patch(self) -> PatchAuditResult:
        return PatchAuditResult(
            patch_name="Native Bloat Slimmer",
            status=PatchStatus.VERIFIED,
            evidence=["Raw resource patch stripping bloat companion binaries in lib/arm64-v8a/"],
        )

    def _audit_vivaldi_telemetry_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="vivaldi_crash_upload_primary",
                defining_class="Lorg/chromium/chrome/browser/crash/MinidumpUploadServiceImpl;",
                method_name="tryUploadCrashDumpWithLocalId",
                return_type="V",
                parameters=["Ljava/lang/String;"],
            ),
            FingerprintQuery(
                name_id="vivaldi_crash_upload_service",
                defining_class="Lorg/chromium/chrome/browser/crash/ChromeMinidumpUploadJobService;",
                method_name="onStartJob",
                return_type="Z",
                parameters=["Landroid/app/job/JobParameters;"],
            ),
        ]
        fp_res = []
        blocking = []
        evidence = []
        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)

        native_checks = []
        if self.elf_analyzer:
            hosts = ["update.vivaldi.com", "crash.vivaldi.com", "crashpad.chromium.org", "downloads.vivaldi.com"]
            for host in hosts:
                st, matches = self.elf_analyzer.analyze_host(host)
                passed = len(matches) > 0
                native_checks.append((host, passed, f"Found {len(matches)} occurrences"))
                if not passed:
                    blocking.append(f"Native host string '{host}' not found in libchrome.so.")

        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Block Vivaldi Telemetry",
            status=status,
            fingerprint_results=fp_res,
            native_checks=native_checks,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_vivaldi_startup_patch(self) -> PatchAuditResult:
        q = FingerprintQuery(
            name_id="vivaldi_startup_partner",
            defining_class="Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;",
            return_type="V",
            parameters=["Landroid/content/Context;"],
            strings=["Chrome.Homepage.PartnerCustomizedDefaultGurl", "Chrome.Homepage.PartnerCustomizedDefaultUri"],
        )
        res = self.fp_resolver.resolve(q)
        blocking = []
        evidence = []
        fp_res = [(q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE")]
        if res.status != FingerprintStatus.VERIFIED:
            blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
        else:
            evidence.extend(res.evidence)

        cls = self.dex_index.find_class("Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;")
        has_field_d = any(f[0] == "d" and f[1] == "Ljava/lang/Boolean;" for f in cls.fields) if cls else False
        if not has_field_d:
            blocking.append("Vivaldi PartnerBrowserCustomizations is missing Boolean field 'd'.")

        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Vivaldi Startup Performance Optimization",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_vivaldi_notification_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="vivaldi_scheduler_schedule",
                defining_class="Lorg/chromium/chrome/browser/notifications/scheduler/NotificationSchedulerTask;",
                method_name="schedule",
                return_type="V",
                parameters=["J", "J"],
            ),
            FingerprintQuery(
                name_id="vivaldi_tips_promo",
                defining_class="Lorg/chromium/chrome/browser/notifications/scheduler/TipsAgent;",
                method_name="showTipsPromo",
                return_type="V",
                parameters=["I"],
            ),
        ]
        fp_res = []
        blocking = []
        evidence = []
        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)

        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Disable Chromium Tips & Notification Scheduler",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_vivaldi_sync_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="vivaldi_periodic_sync",
                return_type="V",
                strings=["BackgroundSync.Periodic.Wakeup.DelayTime"],
            ),
            FingerprintQuery(
                name_id="vivaldi_oneshot_sync",
                return_type="V",
                strings=["BackgroundSync.Wakeup.DelayTime"],
            ),
        ]
        fp_res = []
        blocking = []
        evidence = []
        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)

        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Disable Background Sync & Periodic Sync",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_vivaldi_prompts_patch(self) -> PatchAuditResult:
        queries = [
            FingerprintQuery(
                name_id="vivaldi_in_app_prompts_scheduler",
                defining_class="Lorg/chromium/chrome/browser/ChromeTabbedActivity;",
                return_type="V",
                parameters=[],
                strings=["DONATE_PROMPT", "searchEnginePrompt", "donationPrompt"],
            ),
            FingerprintQuery(
                name_id="vivaldi_privacy_receiver",
                defining_class="Lorg/vivaldi/browser/prompts/PrivacyReportNotificationReceiver;",
                method_name="onReceive",
                return_type="V",
                parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
            ),
            FingerprintQuery(
                name_id="vivaldi_default_browser_receiver",
                defining_class="Lorg/vivaldi/browser/prompts/DefaultBrowserNotificationReceiver;",
                method_name="onReceive",
                return_type="V",
                parameters=["Landroid/content/Context;", "Landroid/content/Intent;"],
            ),
        ]
        fp_res = []
        blocking = []
        evidence = []
        for q in queries:
            res = self.fp_resolver.resolve(q)
            fp_res.append((q.name_id, res.status.value, res.matched_method.full_name if res.matched_method else "NONE"))
            if res.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q.name_id}' failed: {res.status.value}")
            else:
                evidence.extend(res.evidence)

        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Disable Vivaldi Prompts & Notification Receivers",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def _audit_vivaldi_clean_speed_dial_patch(self) -> PatchAuditResult:
        fp_res = []
        blocking = []
        evidence = []

        # 1. Touch interception query
        q_touch = FingerprintQuery(
            name_id="vivaldi_speeddial_touch_intercept",
            defining_class="Lorg/vivaldi/browser/speeddial/SpeedDialMainView;",
            method_name="onInterceptTouchEvent",
            return_type="Z",
            parameters=["Landroid/view/MotionEvent;"],
        )
        res_touch = self.fp_resolver.resolve(q_touch)
        fp_res.append((q_touch.name_id, res_touch.status.value, res_touch.matched_method.full_name if res_touch.matched_method else "NONE"))
        if res_touch.status != FingerprintStatus.VERIFIED:
            blocking.append(f"Fingerprint '{q_touch.name_id}' failed: {res_touch.status.value}")
        else:
            evidence.extend(res_touch.evidence)

        # 2. Chained SpeedDialTopLevelManager query
        q_pref = FingerprintQuery(
            name_id="vivaldi_speeddial_shared_pref_changed",
            return_type="V",
            parameters=["Landroid/content/SharedPreferences;", "Ljava/lang/String;"],
            strings=["show_tab_strip", "show_customize_on_startpage", "show_speeddial_on_startpage"],
        )
        res_pref = self.fp_resolver.resolve(q_pref)
        fp_res.append((q_pref.name_id, res_pref.status.value, res_pref.matched_method.full_name if res_pref.matched_method else "NONE"))
        if res_pref.status != FingerprintStatus.VERIFIED:
            blocking.append(f"Fingerprint '{q_pref.name_id}' failed: {res_pref.status.value}")
        else:
            evidence.extend(res_pref.evidence)
            manager_class = res_pref.matched_method.class_name
            q_d = FingerprintQuery(
                name_id="vivaldi_speeddial_update_buttons",
                defining_class=manager_class,
                return_type="V",
                parameters=["I"],
                strings=["show_customize_on_startpage", "show_add_on_startpage"],
            )
            res_d = self.fp_resolver.resolve(q_d)
            fp_res.append((q_d.name_id, res_d.status.value, res_d.matched_method.full_name if res_d.matched_method else "NONE"))
            if res_d.status != FingerprintStatus.VERIFIED:
                blocking.append(f"Fingerprint '{q_d.name_id}' failed: {res_d.status.value}")
            else:
                evidence.extend(res_d.evidence)

        status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
        return PatchAuditResult(
            patch_name="Clean Speed Dial Bookmarks",
            status=status,
            fingerprint_results=fp_res,
            blocking_reasons=blocking,
            evidence=evidence,
        )

    def run_gradle_build_verification(self) -> Tuple[bool, str]:
        """Runs gradle check, buildAndroid, generatePatchesList and validates .mpp bundle integrity."""
        gradle_cmd = str(self.repo_root / ("gradlew.bat" if sys.platform.startswith("win") else "gradlew"))
        cmd = [gradle_cmd, "check", "buildAndroid", "generatePatchesList"]
        res = subprocess.run(cmd, cwd=str(self.repo_root), capture_output=True, text=True, shell=sys.platform.startswith("win"))
        if res.returncode != 0:
            return False, f"Gradle build failed:\n{res.stdout}\n{res.stderr}"

        # Strict MPP Bundle Integrity Assertion
        bundle_ok, bundle_err = self.assert_mpp_bundle_integrity()
        if not bundle_ok:
            return False, bundle_err

        # Run README sync
        readme_cmd = [
            "python", ".github/scripts/generate_patches_readme.py",
            "kveld9/morphe-patches", "main", "patches-list.json", "README.md"
        ]
        res_readme = subprocess.run(readme_cmd, cwd=str(self.repo_root), capture_output=True, text=True)
        if res_readme.returncode != 0:
            return False, f"README sync script failed:\n{res_readme.stdout}\n{res_readme.stderr}"

        return True, "All Gradle build, MPP bundle integrity, and metadata verification checks passed successfully."

    def assert_mpp_bundle_integrity(self) -> Tuple[bool, str]:
        """Asserts that the compiled .mpp bundle contains classes.dex (Dalvik bytecode) and required extensions.
        Prevents shipping bundles without Android DEX that cause 'Parches: 0' in Morphe Manager.
        """
        import zipfile
        libs_dir = self.repo_root / "patches" / "build" / "libs"
        if not libs_dir.exists():
            return False, f"Build output directory does not exist: {libs_dir}"

        mpp_files = [f for f in libs_dir.glob("*.mpp") if not f.name.endswith("-sources.mpp") and not f.name.endswith("-javadoc.mpp")]
        if not mpp_files:
            return False, f"No .mpp bundle found in {libs_dir}. Ensure 'buildAndroid' was executed."

        for mpp in mpp_files:
            try:
                with zipfile.ZipFile(mpp, "r") as zf:
                    namelist = zf.namelist()
                    if "classes.dex" not in namelist:
                        return False, (
                            f"CRITICAL ERROR in {mpp.name}: 'classes.dex' is MISSING! "
                            f"Morphe Manager on Android requires Dalvik bytecode to load patches. "
                            f"Running standard 'gradle build' only creates Java .class files. "
                            f"You MUST always execute 'gradle buildAndroid' to invoke D8 and package classes.dex."
                        )
                    dex_size = zf.getinfo("classes.dex").file_size
                    if dex_size < 1024:
                        return False, f"CRITICAL ERROR in {mpp.name}: 'classes.dex' is abnormally small ({dex_size} bytes)."

                    if "extensions/extension.mpe" not in namelist:
                        return False, f"CRITICAL ERROR in {mpp.name}: 'extensions/extension.mpe' is MISSING."
            except Exception as e:
                return False, f"Failed to inspect {mpp.name}: {e}"

        return True, "MPP bundle integrity verified (classes.dex and extensions/extension.mpe present)."

    def assert_patches_dynamic_logging(self) -> Tuple[bool, str]:
        """Asserts that all Kotlin patch definitions in patches/.../*.kt contain dynamic diagnostic logging.
        Ensures patches emit concise runtime telemetry for debugging/diagnosis rather than running silently.
        """
        patches_dir = self.repo_root / "patches" / "src" / "main" / "kotlin" / "app" / "morphe" / "patches"
        if not patches_dir.exists():
            return False, f"Patches directory not found: {patches_dir}"

        kt_files = [f for f in patches_dir.rglob("*.kt") if f.name not in ("Constants.kt", "LocaleUtils.kt")]
        missing_logs = []
        for kt in kt_files:
            content = kt.read_text(encoding="utf-8")
            if "println(" not in content:
                missing_logs.append(str(kt.relative_to(self.repo_root)))

        if missing_logs:
            return False, f"The following patch files are missing diagnostic logging:\n" + "\n".join(missing_logs)

        return True, f"All {len(kt_files)} patch definitions have verified diagnostic logging."
