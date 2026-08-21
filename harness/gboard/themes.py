"""
Mandatory Theme and AMOLED Duplication Audit Engine for Gboard.
Enforces strict invariants to prevent redundant or conflicting theme registrations.
"""

from __future__ import annotations

import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Optional, Tuple

from harness.core.dex import DexIndex, IndexedClass, IndexedMethod


@dataclass
class ThemeAuditReport:
    passed: bool
    black_sheet_present: bool
    common_sheet_present: bool
    listing_fragment_found: bool
    amoled_injection_count: int
    duplicate_theme_detected: bool
    findings: List[str] = field(default_factory=list)
    blocking_reasons: List[str] = field(default_factory=list)


class GboardThemeAuditor:
    """Audits theme assets and ThemeListingFragment bytecode for AMOLED safety."""

    def __init__(self, apk_path: str | Path, dex_index: DexIndex):
        self.apk_path = Path(apk_path).resolve()
        self.dex_index = dex_index

    def audit_theme_pipeline(self) -> ThemeAuditReport:
        findings: List[str] = []
        blocking: List[str] = []

        # 1. Check Theme Assets in APK
        has_black_sheet = False
        has_common_sheet = False
        with zipfile.ZipFile(self.apk_path, "r") as zf:
            namelist = set(zf.namelist())
            has_black_sheet = "assets/theme/style_sheet_color_black.binarypb" in namelist
            has_common_sheet = "assets/theme/style_sheet_color_common.binarypb" in namelist

        if has_black_sheet:
            findings.append("Found 'assets/theme/style_sheet_color_black.binarypb' in APK.")
        else:
            blocking.append("Missing required asset 'assets/theme/style_sheet_color_black.binarypb'.")

        if has_common_sheet:
            findings.append("Found 'assets/theme/style_sheet_color_common.binarypb' in APK.")
        else:
            blocking.append("Missing required asset 'assets/theme/style_sheet_color_common.binarypb'.")

        # 2. Inspect ThemeListingFragment
        cls_name = "Lcom/google/android/apps/inputmethod/libs/theme/listing/ThemeListingFragment;"
        cls = self.dex_index.find_class(cls_name)
        listing_found = cls is not None

        amoled_injections = 0
        if not listing_found:
            blocking.append(f"Target class '{cls_name}' not found in DEX.")
        else:
            findings.append(f"Found ThemeListingFragment class '{cls_name}'.")
            # Scan all methods for color_black / AMOLED strings
            for m in cls.methods:
                for s in m.referenced_strings:
                    if "color_black" in s or "AMOLED" in s:
                        amoled_injections += 1
                        findings.append(f"Reference to '{s}' in {m.full_name}")

        duplicate_detected = (amoled_injections > 1)
        if duplicate_detected:
            blocking.append(f"Duplicate AMOLED theme references detected ({amoled_injections} occurrences)!")

        passed = (len(blocking) == 0)
        return ThemeAuditReport(
            passed=passed,
            black_sheet_present=has_black_sheet,
            common_sheet_present=has_common_sheet,
            listing_fragment_found=listing_found,
            amoled_injection_count=amoled_injections,
            duplicate_theme_detected=duplicate_detected,
            findings=findings,
            blocking_reasons=blocking,
        )
