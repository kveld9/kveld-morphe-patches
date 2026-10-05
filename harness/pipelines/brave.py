"""
Target Pipeline for Brave Browser (com.brave.browser).
"""

from __future__ import annotations

from typing import Any, Dict, List, Tuple

from harness.core.pipeline import BaseTargetPipeline, PipelineRegistry
from harness.core.symbols import ResolvedSymbol, SymbolConfidence, SymbolResolver
from harness.core.telemetry import TelemetryScanner
from harness.migration.patch_migrator import MigrationPlan
from harness.migration.validator import AdversarialValidator


class BravePipeline(BaseTargetPipeline):
    app_name = "Brave Browser"
    default_report_filename = "BRAVE_HARNESS_REPORT.md"
    target_version_const = "BRAVE_TARGET_VERSION"

    @classmethod
    def matches_package(cls, package_name: str) -> bool:
        return "brave" in package_name.lower()

    def validate_apk_sanity(self):
        apk_filename = self.apk_ctx.apk_path.name.lower()
        is_mono_arm = (
            "monoarm64" in apk_filename
            or "monoarm" in apk_filename
            or ("monochrome" in apk_filename and ("arm64" in apk_filename or "arm32" in apk_filename))
        )
        if not is_mono_arm:
            print(f"[WARN] APK filename '{self.apk_ctx.apk_path.name}' does not indicate a Monochrome ARM build.")
            print("[WARN] Expected BraveMonoarm64.apk or BraveMonoarm.apk.\n")

        if not self.meta.libchrome_abis:
            print("[WARN] No ARM libchrome.so found in APK.\n")
        else:
            print(f"[INFO] Detected libchrome.so ABI(s): {', '.join(self.meta.libchrome_abis)}\n")

    def execute_audit_and_validation(self) -> Tuple[Dict[str, Any], Any]:
        print("[AUDIT] Resolving obfuscated members and structural contracts...")
        symbols = SymbolResolver(self.dex_index).resolve_all()

        telemetry_report = None
        if self.elf_analyzer:
            print("[AUDIT] Auditing native telemetry domain offsets...")
            current_offsets = self.migrator.current_telemetry_offsets(is_arm32=self.elf_analyzer.is_arm32)
            telemetry_report = TelemetryScanner(self.elf_analyzer).audit_known_hosts(current_offsets)

        print("[AUDIT] Running adversarial validation on all Brave patches...")
        validator = AdversarialValidator(self.repo_root, self.dex_index, self.elf_analyzer)
        patch_results = validator.audit_brave_patches()

        return patch_results, {"symbols": symbols, "telemetry_report": telemetry_report}

    @staticmethod
    def _blocked_origin_symbols(extra_data: Any) -> List[ResolvedSymbol]:
        origin = (extra_data or {}).get("symbols", {}).get("origin")
        if not origin:
            return []
        return [s for s in vars(origin).values() if s.confidence == SymbolConfidence.BLOCKED]

    def is_all_verified(self, patch_results: Dict[str, Any], extra_data: Any) -> bool:
        # Unresolved obfuscated symbols fall back to stale names; never migrate with them.
        return super().is_all_verified(patch_results, extra_data) and not self._blocked_origin_symbols(extra_data)

    def collect_blocked_reasons(self, patch_results: Dict[str, Any], extra_data: Any) -> List[str]:
        reasons = super().collect_blocked_reasons(patch_results, extra_data)
        for sym in self._blocked_origin_symbols(extra_data):
            reasons.append(f"Unresolved obfuscated symbol '{sym.symbol_id}' (fallback '{sym.new_symbol}' not applied)")
        return reasons

    def create_migration_plans(self, extra_data: Any) -> List[MigrationPlan]:
        symbols = extra_data.get("symbols", {})
        telemetry_report = extra_data.get("telemetry_report")

        plans = [self.migrator.plan_constants_update(self.meta.version_name)]
        if telemetry_report and telemetry_report.known_results:
            is_arm32 = bool(self.elf_analyzer and self.elf_analyzer.is_arm32)
            plans.append(self.migrator.plan_telemetry_hosts_update(telemetry_report.known_results, is_arm32=is_arm32))
        if symbols.get("origin"):
            plans.append(self.migrator.plan_origin_symbols_update(symbols["origin"]))
        if symbols.get("scheduler"):
            plans.append(self.migrator.plan_scheduler_symbols_update(symbols["scheduler"]))

        return [p for p in plans if p is not None]


PipelineRegistry.register(BravePipeline)
