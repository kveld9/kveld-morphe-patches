"""
APK Differential Analysis Engine.
Compares two APK releases to calculate additions, removals, and structural changes.
Generates APK_DIFF.md markdown report.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Set

from harness.core.apk import ApkMetadata
from harness.core.dex import DexIndex


@dataclass
class ApkDiffReport:
    old_version: str
    new_version: str
    new_classes: List[str] = field(default_factory=list)
    removed_classes: List[str] = field(default_factory=list)
    new_strings: List[str] = field(default_factory=list)
    removed_strings: List[str] = field(default_factory=list)
    file_size_delta: int = 0


class ApkDiffer:
    """Computes differential delta between two indexed APKs."""

    @staticmethod
    def diff(
        old_meta: ApkMetadata,
        old_dex: DexIndex,
        new_meta: ApkMetadata,
        new_dex: DexIndex,
    ) -> ApkDiffReport:
        old_cls_set = {c.name for c in old_dex.classes}
        new_cls_set = {c.name for c in new_dex.classes}

        new_classes = sorted(list(new_cls_set - old_cls_set))
        removed_classes = sorted(list(old_cls_set - new_cls_set))

        old_strings: Set[str] = set()
        for m in old_dex.methods:
            old_strings.update(m.referenced_strings)

        new_strings: Set[str] = set()
        for m in new_dex.methods:
            new_strings.update(m.referenced_strings)

        added_strings = sorted(list(new_strings - old_strings))[:50]  # Cap sample
        removed_strings = sorted(list(old_strings - new_strings))[:50]

        delta = new_meta.file_size - old_meta.file_size

        return ApkDiffReport(
            old_version=old_meta.version_name,
            new_version=new_meta.version_name,
            new_classes=new_classes,
            removed_classes=removed_classes,
            new_strings=added_strings,
            removed_strings=removed_strings,
            file_size_delta=delta,
        )

    @staticmethod
    def render_markdown(diff_report: ApkDiffReport) -> str:
        lines = []
        lines.append(f"# 📊 APK Differential Report: v{diff_report.old_version} ➔ v{diff_report.new_version}")
        lines.append("")
        lines.append(f"- **File Size Delta**: `{diff_report.file_size_delta:+,} bytes`")
        lines.append(f"- **New Classes**: `{len(diff_report.new_classes)}`")
        lines.append(f"- **Removed Classes**: `{len(diff_report.removed_classes)}`")
        lines.append("")
        if diff_report.new_classes:
            lines.append("## ➕ Sample New Classes")
            for c in diff_report.new_classes[:15]:
                lines.append(f"- `{c}`")
            lines.append("")
        if diff_report.removed_classes:
            lines.append("## ➖ Sample Removed Classes")
            for c in diff_report.removed_classes[:15]:
                lines.append(f"- `{c}`")
            lines.append("")
        return "\n".join(lines)
