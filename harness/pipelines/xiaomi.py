"""
Target Pipeline for Xiaomi Earbuds (com.mi.earphone).
"""

from __future__ import annotations

from typing import Any, Dict, List, Optional, Tuple

from harness.core.pipeline import BaseTargetPipeline, PipelineRegistry
from harness.migration.patch_migrator import MigrationPlan
from harness.reporting.reporter import HarnessReportData
from harness.xiaomi.validator import XiaomiEarbudsValidator


class XiaomiEarbudsPipeline(BaseTargetPipeline):
    app_name = "Xiaomi Earbuds"
    default_report_filename = "XIAOMI_EARBUDS_HARNESS_REPORT.md"
    target_version_const = "XIAOMI_EARBUDS_TARGET_VERSION"

    @classmethod
    def matches_package(cls, package_name: str) -> bool:
        pkg = package_name.lower()
        return "earphone" in pkg or "earbuds" in pkg or pkg == "com.mi.earphone"

    def validate_apk_sanity(self):
        print("[AUDIT] Validating Xiaomi Earbuds APK packaging and components...")
        all_entries = self.apk_ctx.get_all_entry_names() if hasattr(self.apk_ctx, "get_all_entry_names") else []
        dex_count = sum(1 for e in all_entries if e.endswith(".dex"))
        print(f"  - MultiDEX count: {dex_count} DEX files")

    def execute_audit_and_validation(self) -> Tuple[Dict[str, Any], Any]:
        print("[AUDIT] Running adversarial validation on all Xiaomi Earbuds patch targets...")
        validator = XiaomiEarbudsValidator(self.repo_root, self.dex_index, self.apk_ctx)
        patch_results = validator.audit_all_patches()
        return patch_results, {}

    def create_migration_plans(self, extra_data: Any) -> List[MigrationPlan]:
        plan_const = self.migrator.plan_xiaomi_earbuds_constants_update(self.meta.version_name)
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
            symbols={},
            telemetry_report=None,
            applied_changes=applied_changes,
            rejected_changes=rejected_changes,
            build_passed=build_passed,
            build_output=build_output,
            final_status=final_status,
        )


PipelineRegistry.register(XiaomiEarbudsPipeline)
