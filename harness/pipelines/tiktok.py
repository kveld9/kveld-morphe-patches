"""
Target Pipeline for TikTok (com.zhiliaoapp.musically).
"""

from __future__ import annotations

from typing import Any, Dict, List, Tuple

from harness.core.pipeline import BaseTargetPipeline, PipelineRegistry
from harness.migration.patch_migrator import MigrationPlan
from harness.tiktok.validator import TikTokValidator


class TikTokPipeline(BaseTargetPipeline):
    app_name = "TikTok"
    default_report_filename = "TIKLITE_HARNESS_REPORT.md"
    target_version_const = "TIKTOK_TARGET_VERSION"

    @classmethod
    def matches_package(cls, package_name: str) -> bool:
        pkg = package_name.lower()
        return "musically" in pkg or "tiktok" in pkg

    def validate_apk_sanity(self):
        print("[AUDIT] Validating TikTok APK architecture and packaging...")
        all_entries = self.apk_ctx.get_all_entry_names()
        has_arm64 = any(e.startswith("lib/arm64-v8a/") for e in all_entries)
        has_v7a = any(e.startswith("lib/armeabi-v7a/") for e in all_entries)
        dex_count = sum(1 for e in all_entries if e.endswith(".dex"))

        print(f"  - MultiDEX count: {dex_count} DEX files")
        print(f"  - ARM64-v8a support: {'Yes' if has_arm64 else 'No'}")
        print(f"  - Legacy ARMv7a included: {'Yes (Eligible for ABI Slimmer)' if has_v7a else 'No'}\n")

    def execute_audit_and_validation(self) -> Tuple[Dict[str, Any], Any]:
        print("[AUDIT] Running adversarial validation on all TikLite patch targets...")
        validator = TikTokValidator(self.repo_root, self.dex_index, self.apk_ctx)
        patch_results = validator.audit_all_patches()

        return patch_results, {}

    def create_migration_plans(self, extra_data: Any) -> List[MigrationPlan]:
        plan_const = self.migrator.plan_tiktok_constants_update(self.meta.version_name)
        return [plan_const] if plan_const else []


PipelineRegistry.register(TikTokPipeline)
