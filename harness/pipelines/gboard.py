"""
Target Pipeline for Gboard Lite (com.google.android.inputmethod.latin).
"""

from __future__ import annotations

from typing import Any, Dict, List, Optional, Tuple

from harness.core.pipeline import BaseTargetPipeline, PipelineRegistry
from harness.gboard.invariants import InvariantsReport
from harness.gboard.themes import ThemeAuditReport
from harness.gboard.validator import GboardAdversarialValidator
from harness.migration.patch_migrator import MigrationPlan
from harness.migration.validator import PatchStatus
from harness.reporting.reporter import HarnessReportData


class GboardPipeline(BaseTargetPipeline):
    app_name = "Gboard Lite"
    default_report_filename = "GBOARD_HARNESS_REPORT.md"
    target_version_const = "GBOARD_TARGET_VERSION"

    @classmethod
    def matches_package(cls, package_name: str) -> bool:
        pkg = package_name.lower()
        return "latin" in pkg or "inputmethod" in pkg

    def validate_apk_sanity(self):
        apk_filename = self.apk_ctx.apk_path.name.lower()
        ver_lower = self.meta.version_name.lower()
        if "lite" not in apk_filename and "lite" not in ver_lower:
            print("⚠️  WARNING: APK does not appear to be a Gboard Lite variant.\n")
        if "arm64" not in apk_filename and "arm64" not in ver_lower:
            print("⚠️  WARNING: APK does not appear to be ARM64-v8a architecture.\n")

    def execute_audit_and_validation(self) -> Tuple[Dict[str, Any], Any]:
        print("🛡️ Running adversarial validation across all 18 Gboard patch contracts...")
        validator = GboardAdversarialValidator(
            self.repo_root,
            self.meta,
            self.dex_index,
            self.apk_ctx.apk_path,
        )
        patch_results, theme_report, invariants_report = validator.audit_all_patches()
        return patch_results, {
            "theme_report": theme_report,
            "invariants_report": invariants_report,
        }

    def is_all_verified(self, patch_results: Dict[str, Any], extra_data: Any) -> bool:
        base_verified = super().is_all_verified(patch_results, extra_data)
        theme_report: Optional[ThemeAuditReport] = extra_data.get("theme_report")
        invariants_report: Optional[InvariantsReport] = extra_data.get("invariants_report")

        theme_ok = theme_report.passed if theme_report else True
        invariants_ok = invariants_report.passed if invariants_report else True
        return base_verified and theme_ok and invariants_ok

    def collect_blocked_reasons(self, patch_results: Dict[str, Any], extra_data: Any) -> List[str]:
        reasons = super().collect_blocked_reasons(patch_results, extra_data)
        theme_report: Optional[ThemeAuditReport] = extra_data.get("theme_report")
        invariants_report: Optional[InvariantsReport] = extra_data.get("invariants_report")

        if theme_report and not theme_report.passed:
            reasons.extend(theme_report.blocking_reasons)
        if invariants_report and not invariants_report.passed:
            reasons.extend(invariants_report.blocking_reasons)
        return reasons

    def create_migration_plans(self, extra_data: Any) -> List[MigrationPlan]:
        plan_const = self.migrator.plan_gboard_constants_update(self.meta.version_name)
        return [plan_const] if plan_const else []

    def build_report_data(
        self,
        old_version: str,
        patch_results: Dict[str, Any],
        extra_data: Any,
        applied_changes: List[str],
        rejected_changes: List[str],
        build_passed: bool,
        build_output: str,
        final_status: str,
    ) -> HarnessReportData:
        return HarnessReportData(
            app_name=self.app_name,
            package_name=self.meta.package_name,
            mode=self.mode.upper(),
            old_version=old_version,
            new_version=self.meta.version_name,
            old_version_code=None,
            new_version_code=self.meta.version_code,
            apk_sha256=self.meta.sha256,
            apk_file_size=self.meta.file_size,
            patch_results=patch_results,
            theme_report=extra_data.get("theme_report"),
            invariants_report=extra_data.get("invariants_report"),
            applied_changes=applied_changes,
            rejected_changes=rejected_changes,
            build_passed=build_passed,
            build_output=build_output,
            final_status=final_status,
        )


PipelineRegistry.register(GboardPipeline)
