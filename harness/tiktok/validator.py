"""
Adversarial validator for TikTok patches against target APK bytecode and assets.
"""

from __future__ import annotations
from typing import Any, Dict, List
from pathlib import Path

from harness.core.dex import DexIndex
from harness.reporting.reporter import PatchAuditResult, PatchStatus
from harness.tiktok.contracts import TIKTOK_PATCH_CONTRACTS, PatchContract


class TikTokValidator:
    def __init__(self, repo_root: Path, dex_index: DexIndex, apk_ctx: Any):
        self.repo_root = repo_root
        self.dex_index = dex_index
        self.apk_ctx = apk_ctx

    def audit_all_patches(self) -> Dict[str, PatchAuditResult]:
        results: Dict[str, PatchAuditResult] = {}
        all_entries = self.apk_ctx.get_all_entry_names() if hasattr(self.apk_ctx, "get_all_entry_names") else []

        for contract in TIKTOK_PATCH_CONTRACTS:
            status = "VERIFIED"
            details = []

            # 1. Bytecode target validation
            if contract.target_type == "bytecode":
                for cls_desc in contract.required_classes:
                    matched = self.dex_index.find_class(cls_desc)
                    if matched:
                        details.append(f"Found class `{cls_desc}` in `{matched.dex_name}`")
                    else:
                        status = "BLOCKED" if contract.criticality == "CRITICAL" else "WARNING"
                        details.append(f"Missing class `{cls_desc}`")

                for req_str in contract.required_strings:
                    # Fast check in indexed method names
                    matched_methods = [m for m in self.dex_index.methods if req_str in m.name]
                    if matched_methods:
                        details.append(f"Target method `{req_str}` found ({len(matched_methods)} occurrence(s))")
                    else:
                        if not contract.required_classes:
                            status = "WARNING"
                            details.append(f"Symbol `{req_str}` not directly declared")

            # 2. Resource / Asset / ABI validation
            elif contract.target_type in ("raw_resource", "resource"):
                for req_entry in contract.required_strings:
                    matches = [e for e in all_entries if req_entry in e]
                    if matches:
                        details.append(f"Found {len(matches)} entries matching `{req_entry}`")
                    else:
                        details.append(f"No entries matching `{req_entry}` found in APK")

            results[contract.name] = PatchAuditResult(
                status=status,
                target_type=contract.target_type,
                criticality=contract.criticality,
                description=contract.description,
                details=details,
            )

        return results
