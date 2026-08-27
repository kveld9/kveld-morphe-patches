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

    STATUS_ICONS = {
        PatchStatus.VERIFIED: "✅",
        PatchStatus.STATICALLY_VERIFIED: "⚠️",
        PatchStatus.NOT_AFFECTED: "ℹ️",
    }

    @classmethod
    def render_markdown(cls, data: HarnessReportData) -> str:
        sections = [
            cls._render_header(data),
            cls._render_metadata(data),
            cls._render_patches_matrix(data),
        ]
        if data.theme_report:
            sections.append(cls._render_theme_report(data.theme_report))
        if data.invariants_report:
            sections.append(cls._render_invariants_report(data.invariants_report))
        if data.symbols:
            sections.append(cls._render_symbols_report(data.symbols))
        if data.telemetry_report:
            sections.append(cls._render_telemetry_report(data.telemetry_report))
        sections.append(cls._render_changes_and_summary(data))

        flat_lines: List[str] = []
        for sec in sections:
            flat_lines.extend(sec)
        return "\n".join(flat_lines)

    @staticmethod
    def _render_header(data: HarnessReportData) -> List[str]:
        icon = "🦁" if "brave" in data.package_name.lower() else "⌨️"
        status_badge = "✅ `SUCCESS`" if data.final_status == "SUCCESS" else "❌ `BLOCKED`"
        return [
            f"# {icon} {data.app_name} Patches Harness Report",
            "",
            f"- **Execution Mode**: `[{data.mode.upper()}]`",
            f"- **Overall Pipeline Status**: {status_badge}",
            "",
            "---",
            "",
        ]

    @staticmethod
    def _render_metadata(data: HarnessReportData) -> List[str]:
        mb_size = data.apk_file_size / (1024 * 1024)
        return [
            "## 📦 Target APK Metadata",
            "",
            f"- **Application**: `{data.app_name}`",
            f"- **Package Name**: `{data.package_name}`",
            f"- **Version**: `{data.old_version}` ➔ `{data.new_version}`",
            f"- **VersionCode**: `{data.old_version_code or 'N/A'}` ➔ `{data.new_version_code}`",
            f"- **APK SHA-256**: `{data.apk_sha256}`",
            f"- **APK File Size**: `{data.apk_file_size:,} bytes` ({mb_size:.2f} MB)",
            "",
            "---",
            "",
        ]

    @classmethod
    def _render_patches_matrix(cls, data: HarnessReportData) -> List[str]:
        has_native = any(len(res.native_checks) > 0 for res in data.patch_results.values())
        lines = [
            f"## 🩹 Patches Status Matrix ({len(data.patch_results)} Patches)",
            "",
        ]

        if has_native:
            lines.extend([
                "| 💊 Patch | 📊 Status | 🎯 Fingerprints | 🛡️ Native Gates | 📝 Structural Assertions & Notes |",
                "| :--- | :---: | :---: | :---: | :--- |",
            ])
        else:
            lines.extend([
                "| 💊 Patch | 📊 Status | 🎯 Fingerprints | 📝 Structural Assertions & Notes |",
                "| :--- | :---: | :---: | :--- |",
            ])

        for _, res in sorted(data.patch_results.items(), key=lambda x: x[1].patch_name):
            status_icon = cls.STATUS_ICONS.get(res.status, "❌")
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

        lines.extend(["", "---", ""])
        return lines

    @staticmethod
    def _render_theme_report(theme_report: ThemeAuditReport) -> List[str]:
        t_icon = "✅" if theme_report.passed else "❌"
        dup_label = "YES (BLOCKED)" if theme_report.duplicate_theme_detected else "NONE (Clean)"
        lines = [
            "## 🎨 Special Theme & AMOLED Duplication Audit",
            "",
            f"- **Theme Safety Status**: {t_icon} `{'PASSED' if theme_report.passed else 'FAILED'}`",
            f"- **Black Stylesheet Asset Present**: `{'YES' if theme_report.black_sheet_present else 'NO'}`",
            f"- **Common Stylesheet Asset Present**: `{'YES' if theme_report.common_sheet_present else 'NO'}`",
            f"- **AMOLED Injection Invocations**: `{theme_report.amoled_injection_count}` (Expected: 1)",
            f"- **Duplicate Theme Registrations**: `{dup_label}`",
        ]
        for f in theme_report.findings:
            lines.append(f"  - 🔍 {f}")
        lines.extend(["", "---", ""])
        return lines

    @staticmethod
    def _render_invariants_report(invariants_report: InvariantsReport) -> List[str]:
        inv_icon = "✅" if invariants_report.passed else "❌"
        lines = [
            "## 🛡️ Invariants & Regression Contract Checks",
            "",
            f"- **Overall Invariants Status**: {inv_icon} `{'PASSED' if invariants_report.passed else 'FAILED'}`",
        ]
        for inv in invariants_report.results:
            i_icon = "✅" if inv.passed else "❌"
            lines.append(f"- {i_icon} **{inv.invariant_id}**: {inv.description} (`{inv.details}`)")
        lines.extend(["", "---", ""])
        return lines

    @staticmethod
    def _render_symbols_report(symbols: Dict[str, Any]) -> List[str]:
        lines = [
            "## 🔍 Obfuscated Symbol Resolution",
            "",
            "| Symbol ID | Target Class | Old Reference | New Symbol | Confidence |",
            "| :--- | :--- | :--- | :--- | :---: |",
        ]
        origin_syms: Optional[BraveOriginSymbols] = symbols.get("origin")
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
        lines.extend(["", "---", ""])
        return lines

    @staticmethod
    def _render_telemetry_report(telemetry_report: TelemetryReport) -> List[str]:
        lines = [
            "## 🌐 Native Telemetry Audit (`libchrome.so` ARM64)",
            "",
            "| Hostname | Expected | Found | ELF Offset(s) | Status |",
            "| :--- | :---: | :---: | :--- | :---: |",
        ]
        for h in telemetry_report.known_results:
            offs = ", ".join([f"`0x{m.offset:08x}`" for m in h.matches]) if h.matches else "None"
            st_icon = "✅" if h.status in ("VERIFIED", "OFFSET_CHANGED") else "❌"
            lines.append(f"| `{h.hostname}` | `{h.expected_count}` | `{h.found_count}` | {offs} | {st_icon} `{h.status}` |")

        if telemetry_report.discovered_candidates:
            lines.extend([
                "",
                "### 🔎 Discovered Telemetry Candidates",
                "",
            ])
            for c in telemetry_report.discovered_candidates:
                lines.append(f"- **`{c.hostname}`** at offset `0x{c.offset:08x}` ({c.section or 'ELF'}): {c.classification_reason}")

        lines.extend(["", "---", ""])
        return lines

    @staticmethod
    def _render_changes_and_summary(data: HarnessReportData) -> List[str]:
        lines = [
            "## ⚙️ Changes Applied & Validation Summary",
            "",
        ]
        if data.applied_changes:
            lines.append("### Applied Changes:")
            for ch in data.applied_changes:
                lines.append(f"- ✅ {ch}")
        else:
            lines.append("- *No code modifications required (all targets match current definitions).*")

        if data.rejected_changes:
            lines.extend(["", "### Rejected / Blocked Changes:"])
            for rj in data.rejected_changes:
                lines.append(f"- ❌ {rj}")

        if data.residual_risks:
            lines.extend(["", "### ⚠️ Residual Risks / Advisories:"])
            for risk in data.residual_risks:
                lines.append(f"- ⚠️ {risk}")

        lines.append("")
        build_icon = "✅" if data.build_passed else "❌"
        lines.extend([
            f"### Gradle & Metadata Toolchain Status: {build_icon} `{'PASSED' if data.build_passed else 'FAILED'}`",
            f"- {data.build_output.strip()}",
            "",
            "---",
            f"**Final Pipeline Status**: `{data.final_status}`",
        ])
        return lines


# Backwards compatibility aliases
ReportData = HarnessReportData
AuditReporter = HarnessReporter
GboardReportData = HarnessReportData
GboardAuditReporter = HarnessReporter
