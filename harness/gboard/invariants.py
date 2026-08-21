"""
Regression Invariants and Safety Contracts Engine for Gboard.
Enforces structural guarantees on package identity, IME service integrity, and permissions.
"""

from __future__ import annotations

import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Optional

from harness.core.apk import ApkMetadata
from harness.core.dex import DexIndex


@dataclass
class InvariantResult:
    invariant_id: str
    passed: bool
    description: str
    details: str


@dataclass
class InvariantsReport:
    passed: bool
    results: List[InvariantResult] = field(default_factory=list)
    blocking_reasons: List[str] = field(default_factory=list)


class GboardInvariantsChecker:
    """Evaluates regression contracts across APK structure, Manifest, and DEX."""

    EXPECTED_PACKAGE = "com.google.android.inputmethod.latin"

    def __init__(self, metadata: ApkMetadata, dex_index: DexIndex, apk_path: str | Path):
        self.meta = metadata
        self.dex = dex_index
        self.apk_path = Path(apk_path).resolve()

    def check_all(self) -> InvariantsReport:
        results: List[InvariantResult] = []
        blocking: List[str] = []

        # 1. Package Name Invariant
        pkg_ok = (self.meta.package_name == self.EXPECTED_PACKAGE)
        results.append(InvariantResult(
            invariant_id="package_name_match",
            passed=pkg_ok,
            description="Package name must match expected Gboard package ID",
            details=f"Expected: {self.EXPECTED_PACKAGE}, Found: {self.meta.package_name}",
        ))
        if not pkg_ok:
            blocking.append(f"Package mismatch: {self.meta.package_name}")

        # 2. InputMethodService Class Invariant
        ime_class_found = False
        for c in self.dex.classes:
            if "LatinIME" in c.name or "GoogleInputMethodService" in c.name:
                ime_class_found = True
                break
        results.append(InvariantResult(
            invariant_id="ime_service_present",
            passed=ime_class_found,
            description="Gboard core InputMethodService implementation must exist in DEX",
            details=f"Core IME class present: {ime_class_found}",
        ))
        if not ime_class_found:
            blocking.append("Gboard InputMethodService class missing in DEX index.")

        # 3. DEX Partitioning Invariant
        dex_ok = len(self.meta.dex_files) >= 1
        results.append(InvariantResult(
            invariant_id="dex_files_valid",
            passed=dex_ok,
            description="APK contains valid multidex entries",
            details=f"DEX files: {self.meta.dex_files}",
        ))

        # 4. Theme Assets Invariant
        assets_ok = False
        if self.apk_path.exists():
            try:
                with zipfile.ZipFile(self.apk_path, "r") as zf:
                    namelist = set(zf.namelist())
                    assets_ok = "assets/theme/style_sheet_color_black.binarypb" in namelist
            except Exception:
                pass
        results.append(InvariantResult(
            invariant_id="theme_assets_intact",
            passed=assets_ok,
            description="Pure black stylesheet asset exists in theme package directory",
            details=f"Asset present: {assets_ok}",
        ))
        if not assets_ok:
            blocking.append("Required AMOLED theme assets not found in APK.")

        passed = (len(blocking) == 0)
        return InvariantsReport(passed=passed, results=results, blocking_reasons=blocking)
