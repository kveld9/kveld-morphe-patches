"""
Target Pipeline for Brave Browser (com.brave.browser).
"""

from __future__ import annotations

from typing import Any, Dict, List, Optional, Tuple

from harness.core.pipeline import BaseTargetPipeline, PipelineRegistry
from harness.core.symbols import SymbolResolver
from harness.core.telemetry import TelemetryScanner
from harness.migration.patch_migrator import MigrationPlan
from harness.migration.validator import AdversarialValidator
from harness.reporting.reporter import HarnessReportData


class BravePipeline(BaseTargetPipeline):
    app_name = "Brave Browser"
    default_report_filename = "BRAVE_HARNESS_REPORT.md"
    target_version_const = "BRAVE_TARGET_VERSION"

    @classmethod
    def matches_package(cls, package_name: str) -> bool:
        return "brave" in package_name.lower()

    def validate_apk_sanity(self):
        apk_filename = self.apk_ctx.apk_path.name.lower()
        is_mono_arm64 = "monoarm64" in apk_filename or ("monochrome" in apk_filename and "arm64" in apk_filename)
        if not is_mono_arm64:
            print(f"⚠️  WARNING: APK filename '{self.apk_ctx.apk_path.name}' does not indicate a Monochrome ARM64 build.")
            print("⚠️  Morphe Patches targets 'Bravemonoarm64.apk'.\n")
        if not self.meta.has_arm64_libchrome:
            print("⚠️  WARNING: 'lib/arm64-v8a/libchrome.so' not found in APK.\n")

    def execute_audit_and_validation(self) -> Tuple[Dict[str, Any], Any]:
        print("🧠 Resolving obfuscated members and structural contracts...")
        symbols = SymbolResolver(self.dex_index).resolve_all()

        telemetry_report = None
        if self.elf_analyzer:
            print("🌐 Auditing native telemetry domain offsets...")
            telemetry_report = TelemetryScanner(self.elf_analyzer).audit_known_hosts()

        print("🛡️ Running adversarial validation on all Brave patches...")
        validator = AdversarialValidator(self.repo_root, self.dex_index, self.elf_analyzer)
        patch_results = validator.audit_brave_patches()

        return patch_results, {"symbols": symbols, "telemetry_report": telemetry_report}

    def create_migration_plans(self, extra_data: Any) -> List[MigrationPlan]:
        symbols = extra_data.get("symbols", {})
        telemetry_report = extra_data.get("telemetry_report")

        plans = [self.migrator.plan_constants_update(self.meta.version_name)]
        if telemetry_report and telemetry_report.known_results:
            plans.append(self.migrator.plan_telemetry_hosts_update(telemetry_report.known_results))
        if symbols.get("origin"):
            plans.append(self.migrator.plan_origin_symbols_update(symbols["origin"]))
        if symbols.get("scheduler"):
            plans.append(self.migrator.plan_scheduler_symbols_update(symbols["scheduler"]))

        return [p for p in plans if p is not None]

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
            symbols=extra_data.get("symbols", {}),
            telemetry_report=extra_data.get("telemetry_report"),
            applied_changes=applied_changes,
            rejected_changes=rejected_changes,
            build_passed=build_passed,
            build_output=build_output,
            final_status=final_status,
        )


PipelineRegistry.register(BravePipeline)
