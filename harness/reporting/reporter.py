"""
Universal Structured Audit and Update Report Generator for Morphe Patches.
Renders deterministic markdown matrices, diagnostics, symbol resolutions, telemetry,
theme invariants, and execution statuses for both Brave Browser and Gboard.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional

from harness.core.apk import ApkMetadata
from harness.core.symbols import BraveOriginSymbols, BraveNotificationSchedulerSymbols
from harness.core.telemetry import TelemetryReport
from harness.gboard.invariants import InvariantsReport
from harness.gboard.themes import ThemeAuditReport
from harness.migration.validator import PatchAuditResult, PatchStatus


@dataclass
class HarnessReportData:
    app_name: str
    package_name: str
    mode: str  # "AUDIT", "UPDATE", "DRY-RUN"
    old_version: str
    new_version: str
    old_version_code: Optional[int]
    new_version_code: int
    apk_sha256: str
    apk_file_size: int
    patch_results: Dict[str, PatchAuditResult]
    symbols: Dict[str, Any] = field(default_factory=dict)
    telemetry_report: Optional[TelemetryReport] = None
    theme_report: Optional[ThemeAuditReport] = None
    invariants_report: Optional[InvariantsReport] = None
    applied_changes: List[str] = field(default_factory=list)
    rejected_changes: List[str] = field(default_factory=list)
    residual_risks: List[str] = field(default_factory=list)
    build_passed: bool = True
    build_output: str = ""
    final_status: str = "SUCCESS"  # "SUCCESS", "BLOCKED", "FAILED"


class HarnessReporter:
    """Universal renderer for structured patch audit and update reports."""

    @staticmethod
    def render_markdown(data: HarnessReportData) -> str:
        icon = "🦁" if "brave" in data.package_name.lower() else "⌨️"
        lines = []

        # 1. Title & Header
        lines.append(f"# {icon} {data.app_name} Patches Harness Report")
        lines.append("")
        lines.append(f"- **Execution Mode**: `[{data.mode.upper()}]`")
        status_badge = "✅ `SUCCESS`" if data.final_status == "SUCCESS" else "❌ `BLOCKED`"
        lines.append(f"- **Overall Pipeline Status**: {status_badge}")
        lines.append("")
        lines.append("---")
        lines.append("")

        # 2. Target APK Metadata
        lines.append("## 📦 Target APK Metadata")
        lines.append("")
        lines.append(f"- **Application**: `{data.app_name}`")
        lines.append(f"- **Package Name**: `{data.package_name}`")
        lines.append(f"- **Version**: `{data.old_version}` ➔ `{data.new_version}`")
        lines.append(f"- **VersionCode**: `{data.old_version_code or 'N/A'}` ➔ `{data.new_version_code}`")
        lines.append(f"- **APK SHA-256**: `{data.apk_sha256}`")
        lines.append(f"- **APK File Size**: `{data.apk_file_size:,} bytes` ({data.apk_file_size / (1024*1024):.2f} MB)")
        lines.append("")
        lines.append("---")
        lines.append("")

        # 3. Patches Status Matrix
        has_native = any(len(res.native_checks) > 0 for res in data.patch_results.values())
        lines.append(f"## 🩹 Patches Status Matrix ({len(data.patch_results)} Patches)")
        lines.append("")

        if has_native:
            lines.append("| 💊 Patch | 📊 Status | 🎯 Fingerprints | 🛡️ Native Gates | 📝 Structural Assertions & Notes |")
            lines.append("| :--- | :---: | :---: | :---: | :--- |")
        else:
            lines.append("| 💊 Patch | 📊 Status | 🎯 Fingerprints | 📝 Structural Assertions & Notes |")
            lines.append("| :--- | :---: | :---: | :--- |")

        for key, res in sorted(data.patch_results.items(), key=lambda x: x[1].patch_name):
            status_icon = (
                "✅" if res.status == PatchStatus.VERIFIED
                else ("⚠️" if res.status == PatchStatus.STATICALLY_VERIFIED
                else ("ℹ️" if res.status == PatchStatus.NOT_AFFECTED else "❌"))
            )
            fp_pass = sum(1 for fp in res.fingerprint_results if fp[1] == "VERIFIED")
            fp_tot = len(res.fingerprint_results)
            fp_str = f"{fp_pass}/{fp_tot} verified" if fp_tot > 0 else "N/A"

            notes = "; ".join(res.blocking_reasons) if res.blocking_reasons else "All structural assertions satisfied."

            if has_native:
                nat_pass = sum(1 for n in res.native_checks if n[1])
                nat_tot = len(res.native_checks)
                nat_str = f"{nat_pass}/{nat_tot} valid" if nat_tot > 0 else "N/A"
                lines.append(f"| **{res.patch_name}** | {status_icon} `{res.status.value}` | `{fp_str}` | `{nat_str}` | {notes} |")
            else:
                lines.append(f"| **{res.patch_name}** | {status_icon} `{res.status.value}` | `{fp_str}` | {notes} |")

        lines.append("")
        lines.append("---")
        lines.append("")

        # 4. Optional Section: Theme & AMOLED Duplication Audit (Gboard)
        if data.theme_report:
            lines.append("## 🎨 Special Theme & AMOLED Duplication Audit")
            lines.append("")
            t_icon = "✅" if data.theme_report.passed else "❌"
            lines.append(f"- **Theme Safety Status**: {t_icon} `{'PASSED' if data.theme_report.passed else 'FAILED'}`")
            lines.append(f"- **Black Stylesheet Asset Present**: `{'YES' if data.theme_report.black_sheet_present else 'NO'}`")
            lines.append(f"- **Common Stylesheet Asset Present**: `{'YES' if data.theme_report.common_sheet_present else 'NO'}`")
            lines.append(f"- **AMOLED Injection Invocations**: `{data.theme_report.amoled_injection_count}` (Expected: 1)")
            lines.append(f"- **Duplicate Theme Registrations**: `{'YES (BLOCKED)' if data.theme_report.duplicate_theme_detected else 'NONE (Clean)'}`")
            for f in data.theme_report.findings:
                lines.append(f"  - 🔍 {f}")
            lines.append("")
            lines.append("---")
            lines.append("")

        # 5. Optional Section: Regression Invariants (Gboard)
        if data.invariants_report:
            lines.append("## 🛡️ Invariants & Regression Contract Checks")
            lines.append("")
            inv_icon = "✅" if data.invariants_report.passed else "❌"
            lines.append(f"- **Overall Invariants Status**: {inv_icon} `{'PASSED' if data.invariants_report.passed else 'FAILED'}`")
            for inv in data.invariants_report.results:
                i_icon = "✅" if inv.passed else "❌"
                lines.append(f"- {i_icon} **{inv.invariant_id}**: {inv.description} (`{inv.details}`)")
            lines.append("")
            lines.append("---")
            lines.append("")

        # 6. Optional Section: Obfuscated Symbols (Brave)
        if data.symbols:
            lines.append("## 🔍 Obfuscated Symbol Resolution")
            lines.append("")
            lines.append("| Symbol ID | Target Class | Old Reference | New Symbol | Confidence |")
            lines.append("| :--- | :--- | :--- | :--- | :---: |")

            origin_syms: Optional[BraveOriginSymbols] = data.symbols.get("origin")
            if origin_syms:
                for s in [
                    origin_syms.locked_field,
                    origin_syms.key_mapping_method,
                    origin_syms.context_getter_method,
                    origin_syms.update_prefs_method,
                    origin_syms.find_pref_method,
                    origin_syms.pref_listener_field,
                ]:
                    lines.append(f"| `{s.symbol_id}` | `{s.target_class}` | `{s.old_symbol}` | `{s.new_symbol}` | ✅ `{s.confidence.value}` |")

            lines.append("")
            lines.append("---")
            lines.append("")

        # 7. Optional Section: Native Telemetry (Brave libchrome.so)
        if data.telemetry_report:
            lines.append("## 🌐 Native Telemetry Audit (`libchrome.so` ARM64)")
            lines.append("")
            lines.append("| Hostname | Expected | Found | ELF Offset(s) | Status |")
            lines.append("| :--- | :---: | :---: | :--- | :---: |")
            for h in data.telemetry_report.known_results:
                offs = ", ".join([f"`0x{m.offset:08x}`" for m in h.matches]) if h.matches else "None"
                st_icon = "✅" if h.status in ("VERIFIED", "OFFSET_CHANGED") else "❌"
                lines.append(f"| `{h.hostname}` | `{h.expected_count}` | `{h.found_count}` | {offs} | {st_icon} `{h.status}` |")

            if data.telemetry_report.discovered_candidates:
                lines.append("")
                lines.append("### 🔎 Discovered Telemetry Candidates")
                lines.append("")
                for c in data.telemetry_report.discovered_candidates:
                    lines.append(f"- **`{c.hostname}`** at offset `0x{c.offset:08x}` ({c.section or 'ELF'}): {c.classification_reason}")

            lines.append("")
            lines.append("---")
            lines.append("")

        # 8. Changes Applied & Validation Summary
        lines.append("## ⚙️ Changes Applied & Validation Summary")
        lines.append("")
        if data.applied_changes:
            lines.append("### Applied Changes:")
            for ch in data.applied_changes:
                lines.append(f"- ✅ {ch}")
        else:
            lines.append("- *No code modifications required (all targets match current definitions).*")

        if data.rejected_changes:
            lines.append("")
            lines.append("### Rejected / Blocked Changes:")
            for rj in data.rejected_changes:
                lines.append(f"- ❌ {rj}")

        if data.residual_risks:
            lines.append("")
            lines.append("### ⚠️ Residual Risks / Advisories:")
            for risk in data.residual_risks:
                lines.append(f"- ⚠️ {risk}")

        lines.append("")
        build_icon = "✅" if data.build_passed else "❌"
        lines.append(f"### Gradle & Metadata Toolchain Status: {build_icon} `{'PASSED' if data.build_passed else 'FAILED'}`")
        lines.append(f"- {data.build_output.strip()}")
        lines.append("")
        lines.append("---")
        lines.append(f"**Final Pipeline Status**: `{data.final_status}`")

        return "\n".join(lines)


# Backwards compatibility aliases
ReportData = HarnessReportData
AuditReporter = HarnessReporter
GboardReportData = HarnessReportData
GboardAuditReporter = HarnessReporter
