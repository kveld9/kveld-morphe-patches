"""
Target Pipeline for Vivaldi Browser Snapshot (com.vivaldi.browser.snapshot).
"""

from __future__ import annotations

from typing import Any, Dict, List, Optional, Tuple

from harness.core.pipeline import BaseTargetPipeline, PipelineRegistry
from harness.migration.patch_migrator import MigrationPlan
from harness.migration.validator import AdversarialValidator
from harness.reporting.reporter import HarnessReportData


class VivaldiPipeline(BaseTargetPipeline):
    app_name = "Vivaldi Browser Snapshot"
    default_report_filename = "VIVALDI_HARNESS_REPORT.md"
    target_version_const = "VIVALDI_TARGET_VERSION"

    @classmethod
    def matches_package(cls, package_name: str) -> bool:
        return "vivaldi" in package_name.lower()

    def validate_apk_sanity(self):
        apk_filename = self.apk_ctx.apk_path.name.lower()
        if "vivaldi" not in apk_filename:
            print(f"⚠️  WARNING: APK filename '{self.apk_ctx.apk_path.name}' does not indicate Vivaldi Browser.")

    def execute_audit_and_validation(self) -> Tuple[Dict[str, Any], Any]:
        print("🛡️ Running adversarial validation across Vivaldi patch contracts...")
        validator = AdversarialValidator(self.repo_root, self.dex_index, self.elf_analyzer)
        patch_results = validator.audit_vivaldi_patches()
        return patch_results, {}

    def create_migration_plans(self, extra_data: Any) -> List[MigrationPlan]:
        plan_const = self.migrator.plan_vivaldi_constants_update(self.meta.version_name)
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
            applied_changes=applied_changes,
            rejected_changes=rejected_changes,
            build_passed=build_passed,
            build_output=build_output,
            final_status=final_status,
        )


PipelineRegistry.register(VivaldiPipeline)
