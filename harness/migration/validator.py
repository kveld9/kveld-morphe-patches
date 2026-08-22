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
                parameters=["Lorg/chromium/chrome/browser/profiles/Profile;", "Ljava/lang/String;"],
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
                if m.return_type == "I" and len(m.parameters) == 3 and m.parameters[0] == "Landroid/content/Context;"
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
            patch_name="Brave In-Product Notification Scheduler Optimization",
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

    def run_gradle_build_verification(self) -> Tuple[bool, str]:
        """Runs gradle check, buildAndroid, generatePatchesList and readme generator."""
        gradle_cmd = str(self.repo_root / ("gradlew.bat" if sys.platform.startswith("win") else "gradlew"))
        cmd = [gradle_cmd, "check", "buildAndroid", "generatePatchesList"]
        res = subprocess.run(cmd, cwd=str(self.repo_root), capture_output=True, text=True, shell=sys.platform.startswith("win"))
        if res.returncode != 0:
            return False, f"Gradle build failed:\n{res.stdout}\n{res.stderr}"

        # Run README sync
        readme_cmd = [
            "python", ".github/scripts/generate_patches_readme.py",
            "kveld9/morphe-patches", "main", "patches-list.json", "README.md"
        ]
        res_readme = subprocess.run(readme_cmd, cwd=str(self.repo_root), capture_output=True, text=True)
        if res_readme.returncode != 0:
            return False, f"README sync script failed:\n{res_readme.stdout}\n{res_readme.stderr}"

        return True, "All Gradle build and metadata verification checks passed successfully."
