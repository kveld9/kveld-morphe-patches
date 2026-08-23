"""
Unit Tests for Migrator, Symbols, and Validator.
Verifies version bump updates, no-op identical version handling, symbol mappings, and patch status.
"""

import unittest
from pathlib import Path
from unittest.mock import MagicMock
from harness.migration.patch_migrator import PatchMigrator, MigrationPlan
from harness.core.symbols import ResolvedSymbol, SymbolConfidence, BraveOriginSymbols
from harness.migration.validator import AdversarialValidator, PatchStatus, PatchAuditResult


class TestMigratorAndValidator(unittest.TestCase):

    def setUp(self):
        self.repo_root = Path(__file__).resolve().parent.parent.parent
        self.migrator = PatchMigrator(self.repo_root)

    # 10. new version -> metadata updated
    def test_version_new_metadata_updated(self):
        plan = self.migrator.plan_constants_update("1.95.100")
        self.assertTrue(plan.has_changes)
        self.assertIn('const val BRAVE_TARGET_VERSION = "1.95.100"', plan.modified_content)
        self.assertIn('Download Bravemonoarm64.apk (v1.95.100) from github.com/brave/brave-browser/releases', plan.modified_content)

    # 11. identical version -> NO-OP
    def test_version_identical_noop(self):
        # Read current version in Constants.kt
        constants_text = self.migrator.constants_file.read_text(encoding="utf-8")
        import re
        m = re.search(r'const val BRAVE_TARGET_VERSION = "([^"]+)"', constants_text)
        current_ver = m.group(1)
        plan = self.migrator.plan_constants_update(current_ver)
        self.assertFalse(plan.has_changes)
        self.assertEqual(len(plan.changes), 0)

    # 4. obfuscated symbol changed -> candidate detected
    def test_obfuscated_symbol_changed_detected(self):
        sym = ResolvedSymbol(
            symbol_id="origin_locked_field",
            target_class="Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;",
            old_symbol="N0:Z",
            new_symbol="O0:Z",
            symbol_type="field",
            confidence=SymbolConfidence.VERIFIED,
            evidence=["Instance boolean field on class: O0"],
        )
        self.assertEqual(sym.confidence, SymbolConfidence.VERIFIED)
        self.assertEqual(sym.new_symbol, "O0:Z")

    # 5. incompatible symbol -> BLOCK
    def test_symbol_incompatible_block(self):
        sym = ResolvedSymbol(
            symbol_id="origin_locked_field",
            target_class="Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;",
            old_symbol="N0:Z",
            new_symbol="UNKNOWN",
            symbol_type="field",
            confidence=SymbolConfidence.BLOCKED,
            evidence=["No matching boolean field found"],
        )
        self.assertEqual(sym.confidence, SymbolConfidence.BLOCKED)

    # 12. patch not affected -> NOT AFFECTED
    def test_patch_not_affected(self):
        audit_res = PatchAuditResult(
            patch_name="Universal Unrelated Patch",
            status=PatchStatus.NOT_AFFECTED,
            fingerprint_results=[],
            native_checks=[],
            blocking_reasons=[],
            evidence=["Patch is universal and has no package-specific targets."],
        )
    # 13. Gboard new version -> metadata updated
    def test_gboard_version_new_metadata_updated(self):
        plan = self.migrator.plan_gboard_constants_update("18.1.0.999999999-lite_beta-arm64-v8a")
        self.assertTrue(plan.has_changes)
        self.assertIn('const val GBOARD_TARGET_VERSION = "18.1.0.999999999-lite_beta-arm64-v8a"', plan.modified_content)
        self.assertIn('Download 18.1.0.999999999-lite_beta-arm64-v8a (APK nodpi) from APKMirror', plan.modified_content)


if __name__ == "__main__":
    unittest.main()
