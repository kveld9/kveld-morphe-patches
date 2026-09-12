"""
Target Pipeline for TikTok (com.zhiliaoapp.musically / com.ss.android.ugc.trill).
"""

from __future__ import annotations

from typing import Any, Dict, List, Optional, Tuple

from harness.core.pipeline import BaseTargetPipeline, PipelineRegistry
from harness.reporting.reporter import HarnessReportData
from harness.tiktok.validator import TikTokValidator


class TikTokPipeline(BaseTargetPipeline):
    app_name = "TikTok"
    default_report_filename = "TIKLITE_HARNESS_REPORT.md"
    target_version_const = "TIKTOK_TARGET_VERSION"

    @classmethod
    def matches_package(cls, package_name: str) -> bool:
        pkg = package_name.lower()
        return "musically" in pkg or "trill" in pkg or "tiktok" in pkg

    def validate_apk_sanity(self):
        print(f"📦 Validating TikTok APK architecture and packaging...")
        all_entries = self.apk_ctx.get_all_entry_names() if hasattr(self.apk_ctx, "get_all_entry_names") else []
        has_arm64 = any(e.startswith("lib/arm64-v8a/") for e in all_entries)
        has_v7a = any(e.startswith("lib/armeabi-v7a/") for e in all_entries)
        dex_count = sum(1 for e in all_entries if e.endswith(".dex"))

        print(f"  • MultiDEX count: {dex_count} DEX files")
        print(f"  • ARM64-v8a support: {'Yes' if has_arm64 else 'No'}")
        print(f"  • Legacy ARMv7a included: {'Yes (Eligible for ABI Slimmer)' if has_v7a else 'No'}\n")

    def execute_audit_and_validation(self) -> Tuple[Dict[str, Any], Any]:
        print("🛡️ Running adversarial validation on all TikLite patch targets...")
        validator = TikTokValidator(self.repo_root, self.dex_index, self.apk_ctx)
        patch_results = validator.audit_all_patches()

        return patch_results, {}

    def create_migration_plans(self, extra_data: Any) -> List[Any]:
        # Minimal version update plan for Constants.kt if migrator is present
        return []

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


PipelineRegistry.register(TikTokPipeline)
