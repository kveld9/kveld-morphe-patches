"""
Minimal Patch Source Code Migrator.
Performs minimal, surgical AST and regex-anchored source updates on Brave patches and Constants.kt.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from harness.core.symbols import BraveOriginSymbols, BraveNotificationSchedulerSymbols, SymbolConfidence
from harness.core.telemetry import HostAuditResult


@dataclass
class MigrationPlan:
    file_path: Path
    original_content: str
    modified_content: str
    changes: List[str]

    @property
    def has_changes(self) -> bool:
        return self.original_content != self.modified_content


class PatchMigrator:
    """Calculates and applies minimal source modifications to Kotlin patch files."""

    def __init__(self, repo_root: str | Path):
        self.repo_root = Path(repo_root).resolve()
        self.constants_file = self.repo_root / "patches/src/main/kotlin/app/morphe/patches/shared/Constants.kt"
        self.telemetry_patch_file = self.repo_root / "patches/src/main/kotlin/app/morphe/patches/brave/BraveBlockTelemetryPatch.kt"
        self.origin_patch_file = self.repo_root / "patches/src/main/kotlin/app/morphe/patches/brave/BraveOriginPatch.kt"
        self.scheduler_patch_file = self.repo_root / "patches/src/main/kotlin/app/morphe/patches/brave/BraveNotificationSchedulerOptimizationPatch.kt"
        self.perf_patch_file = self.repo_root / "patches/src/main/kotlin/app/morphe/patches/brave/BraveStartupPerformancePatch.kt"

    def plan_constants_update(self, new_version: str) -> MigrationPlan:
        content = self.constants_file.read_text(encoding="utf-8")
        changes = []

        # 1. BRAVE_TARGET_VERSION = "..."
        new_content = re.sub(
            r'const val BRAVE_TARGET_VERSION = "[^"]+"',
            f'const val BRAVE_TARGET_VERSION = "{new_version}"',
            content
        )
        if new_content != content:
            changes.append(f"Updated BRAVE_TARGET_VERSION to '{new_version}'")

        # 2. description = "Download Bravemonoarm64.apk (v...) from github.com/brave/brave-browser/releases"
        new_content2 = re.sub(
            r'description = "Download (?:Bravemonoarm64\.apk \(v[^"]+\)|v[^"]+) from github\.com/brave/brave-browser/releases"',
            f'description = "Download Bravemonoarm64.apk (v{new_version}) from github.com/brave/brave-browser/releases"',
            new_content
        )
        if new_content2 != new_content:
            changes.append(f"Updated AppTarget description to 'Bravemonoarm64.apk (v{new_version})'")

        return MigrationPlan(self.constants_file, content, new_content2, changes)

    def plan_gboard_constants_update(self, new_version: str) -> MigrationPlan:
        content = self.constants_file.read_text(encoding="utf-8")
        changes = []

        # 1. GBOARD_TARGET_VERSION = "..."
        new_content = re.sub(
            r'const val GBOARD_TARGET_VERSION = "[^"]+"',
            f'const val GBOARD_TARGET_VERSION = "{new_version}"',
            content
        )
        if new_content != content:
            changes.append(f"Updated GBOARD_TARGET_VERSION to '{new_version}'")

        # 2. description = "Download ... (APK nodpi) from APKMirror"
        new_content2 = re.sub(
            r'description = "(?:Download [^"]+ from APKMirror|Gboard Lite beta [^"]+)"',
            f'description = "Download {new_version} (APK nodpi) from APKMirror"',
            new_content
        )
        if new_content2 != new_content:
            changes.append(f"Updated Gboard AppTarget description to 'Download {new_version} (APK nodpi) from APKMirror'")

        return MigrationPlan(self.constants_file, content, new_content2, changes)

    def plan_vivaldi_constants_update(self, new_version: str) -> MigrationPlan:
        content = self.constants_file.read_text(encoding="utf-8")
        changes = []

        # 1. VIVALDI_TARGET_VERSION = "..."
        new_content = re.sub(
            r'const val VIVALDI_TARGET_VERSION = "[^"]+"',
            f'const val VIVALDI_TARGET_VERSION = "{new_version}"',
            content
        )
        if new_content != content:
            changes.append(f"Updated VIVALDI_TARGET_VERSION to '{new_version}'")

        # 2. description = "Download Vivaldi..._arm64-v8a.apk from vivaldi.com/blog/android/"
        new_content2 = re.sub(
            r'description = "Download Vivaldi\.[^"]+_arm64-v8a\.apk from vivaldi\.com/blog/android/"',
            f'description = "Download Vivaldi.{new_version}_arm64-v8a.apk from vivaldi.com/blog/android/"',
            new_content
        )
        if new_content2 != new_content:
            changes.append(f"Updated Vivaldi AppTarget description to 'Vivaldi.{new_version}_arm64-v8a.apk'")

        return MigrationPlan(self.constants_file, content, new_content2, changes)

    def plan_telemetry_hosts_update(self, host_results: List[HostAuditResult]) -> MigrationPlan:
        content = self.telemetry_patch_file.read_text(encoding="utf-8")
        changes = []

        # Build HostEntry lines
        entries_lines = []
        for r in host_results:
            for match in r.matches:
                entries_lines.append(f'            HostEntry(0x{match.offset:08x}L, "{match.matched_string}"),')

        replacement_block = "        val hostEntries = listOf(\n" + "\n".join(entries_lines) + "\n        )"

        pattern = r"        val hostEntries = listOf\(.*?\n        \)"
        new_content = re.sub(pattern, replacement_block, content, flags=re.DOTALL)
        if new_content != content:
            changes.append(f"Updated {len(entries_lines)} HostEntry native offsets in libchrome.so")

        return MigrationPlan(self.telemetry_patch_file, content, new_content, changes)

    def plan_origin_symbols_update(self, symbols: BraveOriginSymbols) -> MigrationPlan:
        content = self.origin_patch_file.read_text(encoding="utf-8")
        changes = []
        new_content = content

        # 1. Locked boolean field in onPreferenceChange smali hook
        # iget-boolean v0, p0, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->O0:Z
        locked_field_name = symbols.locked_field.new_symbol.split(":")[0]
        new_content2 = re.sub(
            r"(iget-boolean v0, p0, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->)[A-Za-z0-9_]+(:Z)",
            rf"\g<1>{locked_field_name}\g<2>",
            new_content
        )
        if new_content2 != new_content:
            changes.append(f"Updated Origin locked field to '{locked_field_name}'")
            new_content = new_content2

        # 2. Key mapping method
        # invoke-static {v0}, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->e5(Ljava/lang/String;)Ljava/lang/String;
        key_map_name = symbols.key_mapping_method.new_symbol.split("(")[0]
        new_content2 = re.sub(
            r"(invoke-static \{v0\}, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->)[A-Za-z0-9_]+(\(Ljava/lang/String;\)Ljava/lang/String;)",
            rf"\g<1>{key_map_name}\g<2>",
            new_content
        )
        if new_content2 != new_content:
            changes.append(f"Updated Origin key mapping method to '{key_map_name}'")
            new_content = new_content2

        # 3. Context getter method
        # invoke-virtual {p0}, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->N3()Landroid/content/Context;
        ctx_name = symbols.context_getter_method.new_symbol.split("(")[0]
        new_content2 = re.sub(
            r"(invoke-virtual \{p0\}, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->)[A-Za-z0-9_]+(\(\)Landroid/content/Context;)",
            rf"\g<1>{ctx_name}\g<2>",
            new_content
        )
        if new_content2 != new_content:
            changes.append(f"Updated Origin context getter method to '{ctx_name}'")
            new_content = new_content2

        # 4. Update preferences method
        # invoke-virtual {p0}, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->d5()V
        update_name = symbols.update_prefs_method.new_symbol.split("(")[0]
        new_content2 = re.sub(
            r"(invoke-virtual \{p0\}, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->)[A-Za-z0-9_]+(\(\)V)",
            rf"\g<1>{update_name}\g<2>",
            new_content
        )
        if new_content2 != new_content:
            changes.append(f"Updated Origin update prefs method to '{update_name}'")
            new_content = new_content2

        # 5. Find preference method
        # invoke-virtual {p0, p1}, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->S4(Ljava/lang/CharSequence;)Landroidx/preference/Preference;
        find_name = symbols.find_pref_method.new_symbol.split("(")[0]
        new_content2 = re.sub(
            r"(invoke-virtual \{p0, p1\}, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->)[A-Za-z0-9_]+(\(Ljava/lang/CharSequence;\)Landroidx/preference/Preference;)",
            rf"\g<1>{find_name}\g<2>",
            new_content
        )
        if new_content2 != new_content:
            changes.append(f"Updated Origin find preference method to '{find_name}'")
            new_content = new_content2

        # 6. Preference listener reflection field
        listener_field = symbols.pref_listener_field.new_symbol
        new_content2 = re.sub(
            r'(const-string v1, ")[^"]+("\s*\n\s*const-class v2, Landroidx/preference/Preference;)',
            rf'\g<1>{listener_field}\g<2>',
            new_content
        )
        if new_content2 != new_content:
            changes.append(f"Updated Preference listener reflection field to '{listener_field}'")
            new_content = new_content2

        return MigrationPlan(self.origin_patch_file, content, new_content, changes)

    def plan_scheduler_symbols_update(self, symbols: BraveNotificationSchedulerSymbols) -> MigrationPlan:
        content = self.scheduler_patch_file.read_text(encoding="utf-8")
        changes = []

        # Update parameters = listOf("Landroid/content/Context;", "Lvtj;", "Locc;")
        param_block = f'            parameters = listOf(\n                "Landroid/content/Context;",\n                "{symbols.param2_type}",\n                "{symbols.param3_type}",\n            ),'
        new_content = re.sub(
            r'            parameters = listOf\(\s*"Landroid/content/Context;",\s*"[^"]+",\s*"[^"]+",\s*\),',
            param_block,
            content
        )
        if new_content != content:
            changes.append(f"Updated NotificationScheduler parameters to ['Landroid/content/Context;', '{symbols.param2_type}', '{symbols.param3_type}']")

        return MigrationPlan(self.scheduler_patch_file, content, new_content, changes)

    def apply_plan(self, plan: MigrationPlan):
        if plan.has_changes:
            plan.file_path.write_text(plan.modified_content, encoding="utf-8")
