"""
Adversarial Validation Engine for Gboard Patches.
Executes contract assertions, theme duplication checks, and regression tests.
"""

from __future__ import annotations

import os
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from harness.core.apk import ApkMetadata
from harness.core.dex import DexIndex
from harness.core.fingerprints import FingerprintQuery, FingerprintResolver, FingerprintStatus
from harness.gboard.contracts import GboardPatchContract, get_all_gboard_contracts
from harness.gboard.invariants import GboardInvariantsChecker, InvariantsReport
from harness.gboard.themes import GboardThemeAuditor, ThemeAuditReport
from harness.migration.validator import PatchAuditResult, PatchStatus


class GboardAdversarialValidator:
    """Rigorous validator ensuring all 18 Gboard patch contracts are verified."""

    def __init__(self, repo_root: str | Path, metadata: ApkMetadata, dex_index: DexIndex, apk_path: str | Path):
        self.repo_root = Path(repo_root).resolve()
        self.metadata = metadata
        self.dex_index = dex_index
        self.apk_path = Path(apk_path).resolve()
        self.fp_resolver = FingerprintResolver(dex_index)
        self.contracts = get_all_gboard_contracts()

    def audit_all_patches(self) -> Tuple[Dict[str, PatchAuditResult], ThemeAuditReport, InvariantsReport]:
        patch_results: Dict[str, PatchAuditResult] = {}

        # 1. Audit each contract
        for contract in self.contracts:
            fp_results = []
            blocking = []
            evidence = []

            for query in contract.queries:
                res = self.fp_resolver.resolve(query)
                matched_name = res.matched_method.full_name if res.matched_method else "NONE"
                fp_results.append((query.name_id, res.status.value, matched_name))
                if res.status != FingerprintStatus.VERIFIED:
                    blocking.append(f"Fingerprint '{query.name_id}' failed: {res.status.value}")
                else:
                    evidence.extend(res.evidence)

            status = PatchStatus.VERIFIED if not blocking else PatchStatus.BLOCKED
            patch_results[contract.patch_id] = PatchAuditResult(
                patch_name=contract.name,
                status=status,
                fingerprint_results=fp_results,
                blocking_reasons=blocking,
                evidence=evidence,
            )

        # 2. Theme Duplication Audit
        theme_auditor = GboardThemeAuditor(self.apk_path, self.dex_index)
        theme_report = theme_auditor.audit_theme_pipeline()
        if not theme_report.passed:
            if "gboard_amoled" in patch_results:
                patch_results["gboard_amoled"].status = PatchStatus.BLOCKED
                patch_results["gboard_amoled"].blocking_reasons.extend(theme_report.blocking_reasons)

        # 3. Invariants & Safety Audit
        invariants_checker = GboardInvariantsChecker(self.metadata, self.dex_index, self.apk_path)
        invariants_report = invariants_checker.check_all()

        return patch_results, theme_report, invariants_report

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
