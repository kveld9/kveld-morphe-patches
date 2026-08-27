"""
Modular Target Pipeline Architecture and Transactional Execution Engine for Morphe Patches.
Provides base lifecycle abstraction, transactional rollback during migrations, and dynamic dispatch.
"""

from __future__ import annotations

import abc
import re
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple, Type

from harness.core.apk import ApkContext, ApkMetadata
from harness.core.dex import DexIndex
from harness.core.elf import Elf64Analyzer
from harness.migration.patch_migrator import MigrationPlan, PatchMigrator
from harness.migration.validator import AdversarialValidator, PatchStatus
from harness.reporting.reporter import HarnessReportData, HarnessReporter


def _safe_print(msg: str, file=None):
    """Safely prints text handling windows cp1252 terminal encodings."""
    target_file = file or sys.stdout
    try:
        print(msg, file=target_file)
    except UnicodeEncodeError:
        # Fallback to ascii replacement
        ascii_msg = msg.encode(getattr(target_file, "encoding", "ascii") or "ascii", errors="replace").decode("ascii")
        print(ascii_msg, file=target_file)


class TransactionalMigrationContext:
    """
    Context manager that tracks all file modifications during migrations.
    Automatically rolls back changes if a build fails or an exception occurs.
    """

    def __init__(self, migrator: PatchMigrator):
        self.migrator = migrator
        self._snapshots: Dict[Path, str] = {}
        self._applied_plans: List[MigrationPlan] = []
        self._committed = False

    def apply_plan(self, plan: MigrationPlan) -> bool:
        if not plan.has_changes:
            return False
        # Record original snapshot if not already recorded
        if plan.file_path not in self._snapshots:
            self._snapshots[plan.file_path] = plan.original_content
        self.migrator.apply_plan(plan)
        self._applied_plans.append(plan)
        return True

    def commit(self):
        """Finalizes the changes, disabling automatic rollback."""
        self._committed = True
        self._snapshots.clear()

    def rollback(self):
        """Restores all modified files to their original pre-migration state."""
        if self._committed:
            return
        for file_path, original_content in self._snapshots.items():
            try:
                file_path.write_text(original_content, encoding="utf-8")
                _safe_print(f"[Rollback] Restored '{file_path.name}' to original state.")
            except Exception as e:
                _safe_print(f"[Rollback Error] Failed to restore '{file_path.name}': {e}", file=sys.stderr)
        self._snapshots.clear()
        self._applied_plans.clear()

    def __enter__(self) -> "TransactionalMigrationContext":
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if exc_type is not None and not self._committed:
            _safe_print(f"\n[Warning] Exception occurred during update: {exc_val}. Triggering rollback...")
            self.rollback()
        return False


class BaseTargetPipeline(abc.ABC):
    """
    Abstract base pipeline defining the standardized lifecycle for target application auditing and updating.
    """

    app_name: str = "Unknown Target"
    default_report_filename: str = "HARNESS_REPORT.md"
    target_version_const: str = "UNKNOWN_TARGET_VERSION"

    def __init__(
        self,
        apk_ctx: ApkContext,
        mode: str = "audit",
        output_report: Optional[str] = None,
        repo_root: Optional[Path] = None,
    ):
        self.apk_ctx = apk_ctx
        self.mode = mode.lower()
        self.output_report = output_report or self.default_report_filename
        self.repo_root = (repo_root or Path(__file__).resolve().parent.parent.parent).resolve()
        self.meta: ApkMetadata = apk_ctx.get_metadata()
        self.migrator = PatchMigrator(self.repo_root)
        self.dex_index: Optional[DexIndex] = None
        self.elf_analyzer: Optional[Elf64Analyzer] = None
        self.start_time: float = 0.0

    @classmethod
    @abc.abstractmethod
    def matches_package(cls, package_name: str) -> bool:
        """Determines if this pipeline handles the given package identifier."""
        pass

    def extract_and_index(self):
        """Indexes DEX files and parses native ELF libraries if present."""
        _safe_print(f"Extracting and indexing {len(self.meta.dex_files)} DEX files...")
        dex_entries = self.apk_ctx.extract_dex_bytes()
        self.dex_index = DexIndex()
        self.dex_index.index_dex_files(dex_entries)
        _safe_print(f"Indexed {len(self.dex_index.classes)} classes, {len(self.dex_index.methods)} methods.")

        libchrome_path = self.apk_ctx.extract_libchrome_path()
        if libchrome_path:
            _safe_print("Extracting and analyzing ARM64 libchrome.so...")
            self.elf_analyzer = Elf64Analyzer(libchrome_path)
            _safe_print(
                f"ELF parsed: valid={self.elf_analyzer.is_valid}, "
                f"aarch64={self.elf_analyzer.is_aarch64}, size={len(self.elf_analyzer.data):,} bytes"
            )

    def get_old_target_version(self) -> str:
        """Reads current target version from Constants.kt."""
        if not self.migrator.constants_file.exists():
            return "UNKNOWN"
        constants_text = self.migrator.constants_file.read_text(encoding="utf-8")
        m_ver = re.search(rf'const val {self.target_version_const} = "([^"]+)"', constants_text)
        return m_ver.group(1) if m_ver else "UNKNOWN"

    def run_gradle_verification(self, validator: AdversarialValidator) -> Tuple[bool, str]:
        """Executes gradle build checks."""
        _safe_print("Executing Gradle build, checks, and metadata generation...")
        return validator.run_gradle_build_verification()

    def is_all_verified(self, patch_results: Dict[str, Any], extra_data: Any) -> bool:
        """Determines if all patches and safety gates passed."""
        return all(
            res.status in (PatchStatus.VERIFIED, PatchStatus.NOT_AFFECTED) for res in patch_results.values()
        )

    def collect_blocked_reasons(self, patch_results: Dict[str, Any], extra_data: Any) -> List[str]:
        """Collects explanation strings for blocked patches and domain safety failures."""
        reasons = []
        for _, p_res in patch_results.items():
            if p_res.status == PatchStatus.BLOCKED:
                reasons.append(f"Blocked update for '{p_res.patch_name}': {'; '.join(p_res.blocking_reasons)}")
        return reasons

    @abc.abstractmethod
    def validate_apk_sanity(self):
        """Sanity checks on APK architecture and package."""
        pass

    @abc.abstractmethod
    def execute_audit_and_validation(self) -> Tuple[Dict[str, Any], Any]:
        """Executes patch validation, returning patch results and extra domain reports."""
        pass

    @abc.abstractmethod
    def create_migration_plans(self, extra_data: Any) -> List[MigrationPlan]:
        """Calculates declarative source migration plans."""
        pass

    @abc.abstractmethod
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
        """Constructs the structured HarnessReportData instance."""
        pass

    def execute(self) -> int:
        """Full lifecycle execution."""
        self.start_time = time.time()
        _safe_print(f"Running {self.app_name} Pipeline for {self.meta.package_name} v{self.meta.version_name}...")
        self.validate_apk_sanity()

        old_version = self.get_old_target_version()
        self.extract_and_index()

        patch_results, extra_data = self.execute_audit_and_validation()
        all_verified = self.is_all_verified(patch_results, extra_data)

        applied_changes: List[str] = []
        rejected_changes: List[str] = []
        build_passed = True
        build_output = "Build skipped in audit/dry-run mode."

        if self.mode == "dry-run":
            plans = self.create_migration_plans(extra_data)
            for plan in plans:
                if plan.has_changes:
                    applied_changes.extend([f"[DRY-RUN] Would apply: {c}" for c in plan.changes])
            if not all_verified:
                rejected_changes = self.collect_blocked_reasons(patch_results, extra_data)

        elif self.mode == "update":
            if all_verified:
                plans = self.create_migration_plans(extra_data)
                with TransactionalMigrationContext(self.migrator) as tx:
                    for plan in plans:
                        if tx.apply_plan(plan):
                            applied_changes.extend(plan.changes)

                    # Verify build inside transaction
                    validator = AdversarialValidator(self.repo_root, self.dex_index, self.elf_analyzer)
                    build_passed, build_output = self.run_gradle_verification(validator)

                    if build_passed:
                        tx.commit()
                        _safe_print("Transaction committed successfully.")
                    else:
                        _safe_print("Gradle verification failed. Rolling back all code changes...")
                        tx.rollback()
                        rejected_changes.append("Gradle build/verification failed; changes rolled back.")
            else:
                rejected_changes = self.collect_blocked_reasons(patch_results, extra_data)

        final_status = "SUCCESS" if (all_verified and build_passed) else "BLOCKED"

        report_data = self.build_report_data(
            old_version=old_version,
            patch_results=patch_results,
            extra_data=extra_data,
            applied_changes=applied_changes,
            rejected_changes=rejected_changes,
            build_passed=build_passed,
            build_output=build_output,
            final_status=final_status,
        )

        md_report = HarnessReporter.render_markdown(report_data)
        _safe_print("\n" + md_report + "\n")
        Path(self.output_report).write_text(md_report, encoding="utf-8")
        _safe_print(f"Report written to {self.output_report}")
        elapsed = time.time() - self.start_time
        _safe_print(f"Pipeline completed in {elapsed:.2f}s with status: {final_status}")

        return 0 if final_status == "SUCCESS" else 1


class PipelineRegistry:
    """Dynamic registry for target pipelines."""

    _pipelines: List[Type[BaseTargetPipeline]] = []

    @classmethod
    def register(cls, pipeline_cls: Type[BaseTargetPipeline]):
        if pipeline_cls not in cls._pipelines:
            cls._pipelines.append(pipeline_cls)

    @classmethod
    def find_pipeline(cls, package_name: str) -> Optional[Type[BaseTargetPipeline]]:
        for pipe_cls in cls._pipelines:
            if pipe_cls.matches_package(package_name):
                return pipe_cls
        return None

    @classmethod
    def dispatch(
        cls,
        apk_ctx: ApkContext,
        mode: str = "audit",
        output_report: Optional[str] = None,
        repo_root: Optional[Path] = None,
    ) -> int:
        meta = apk_ctx.get_metadata()
        pipeline_cls = cls.find_pipeline(meta.package_name)
        if pipeline_cls is None:
            # Fallback to first registered or raise
            if cls._pipelines:
                _safe_print(f"Unrecognized package '{meta.package_name}'. Defaulting to {cls._pipelines[0].app_name} pipeline.")
                pipeline_cls = cls._pipelines[0]
            else:
                raise RuntimeError(f"No pipelines registered to handle package '{meta.package_name}'")

        pipeline = pipeline_cls(apk_ctx=apk_ctx, mode=mode, output_report=output_report, repo_root=repo_root)
        return pipeline.execute()
