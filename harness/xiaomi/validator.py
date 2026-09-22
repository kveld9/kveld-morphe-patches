"""
Adversarial validator for Xiaomi Earbuds patches against target APK bytecode and assets.
"""

from __future__ import annotations
from typing import Any, Dict
from pathlib import Path

from harness.core.dex import DexIndex
from harness.reporting.reporter import PatchAuditResult, PatchStatus
from harness.xiaomi.contracts import XIAOMI_EARBUDS_PATCH_CONTRACTS


class XiaomiEarbudsValidator:
    def __init__(self, repo_root: Path, dex_index: DexIndex, apk_ctx: Any):
        self.repo_root = repo_root
        self.dex_index = dex_index
        self.apk_ctx = apk_ctx

    def audit_all_patches(self) -> Dict[str, PatchAuditResult]:
        results: Dict[str, PatchAuditResult] = {}

        for contract in XIAOMI_EARBUDS_PATCH_CONTRACTS:
            status = PatchStatus.VERIFIED
            details = []
            blocking_reasons = []
            evidence = []

            found_classes = []
            for cls_desc in contract.required_classes:
                matched = self.dex_index.find_class(cls_desc)
                if matched:
                    evidence.append(f"Found class `{cls_desc}` in `{matched.dex_name}`")
                    found_classes.append(matched)
                else:
                    status = PatchStatus.BLOCKED if contract.criticality in ("CRITICAL", "HIGH") else PatchStatus.STATICALLY_VERIFIED
                    blocking_reasons.append(f"Missing required class `{cls_desc}`")

            for req_str in contract.required_strings:
                if found_classes:
                    matched_methods = [
                        m for cls in found_classes for m in cls.methods
                        if req_str == m.name or req_str in m.referenced_strings
                    ]
                else:
                    matched_methods = [
                        m for m in self.dex_index.methods
                        if req_str == m.name or req_str in m.referenced_strings
                    ]

                if matched_methods:
                    evidence.append(f"Target `{req_str}` found ({len(matched_methods)} occurrence(s))")
                else:
                    if contract.criticality in ("CRITICAL", "HIGH"):
                        status = PatchStatus.BLOCKED
                        blocking_reasons.append(f"Target `{req_str}` not found")
                    else:
                        details.append(f"Target `{req_str}` not found")

            results[contract.patch_id] = PatchAuditResult(
                patch_name=contract.name,
                status=status,
                fingerprint_results=[(c, status.value, "") for c in contract.required_classes],
                blocking_reasons=blocking_reasons,
                evidence=evidence,
            )

        return results
