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
        self.assertIn('Download Bravemonoarm64.apk or BraveMonoarm.apk (v1.95.100) from github.com/brave/brave-browser/releases', plan.modified_content)

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
        self.assertIn('const val GBOARD_TARGET_VERSION_V7A = "18.1.0.999999999-lite_beta-armeabi-v7a"', plan.modified_content)

    # 13b. Gboard bump must only touch the two Gboard version constants
    def test_gboard_version_bump_leaves_other_lines_untouched(self):
        plan = self.migrator.plan_gboard_constants_update("18.1.0.999999999-lite_beta-armeabi-v7a")
        changed = [
            (old, new)
            for old, new in zip(plan.original_content.splitlines(), plan.modified_content.splitlines())
            if old != new
        ]
        self.assertEqual(len(changed), 2)
        for old, _ in changed:
            self.assertRegex(old, r'const val GBOARD_TARGET_VERSION(_V7A)? = ')

    # 13c. TikTok new version -> constant bumped, interpolated description untouched
    def test_tiktok_version_bump(self):
        plan = self.migrator.plan_tiktok_constants_update("99.9.9")
        self.assertIn('const val TIKTOK_TARGET_VERSION = "99.9.9"', plan.modified_content)
        self.assertEqual(plan.changes, ["Updated TIKTOK_TARGET_VERSION to '99.9.9'"])
        self.assertEqual(len(plan.original_content.splitlines()), len(plan.modified_content.splitlines()))

    # 16. Origin pref key migration -> updated
    def test_origin_pref_key_migration(self):
        symbols = BraveOriginSymbols(
            locked_field=ResolvedSymbol("origin_locked_field", "", "N0:Z", "O0:Z", "field", SymbolConfidence.VERIFIED),
            key_mapping_method=ResolvedSymbol("origin_key_mapping", "", "b5", "j5(Ljava/lang/String;)Ljava/lang/String;", "method", SymbolConfidence.VERIFIED),
            context_getter_method=ResolvedSymbol("origin_context_getter", "", "B4", "S3()Landroid/content/Context;", "method", SymbolConfidence.VERIFIED),
            update_prefs_method=ResolvedSymbol("origin_update_prefs", "", "e5", "i5()V", "method", SymbolConfidence.VERIFIED),
            find_pref_method=ResolvedSymbol("origin_find_pref", "", "P4", "W4(Ljava/lang/CharSequence;)Landroidx/preference/Preference;", "method", SymbolConfidence.VERIFIED),
            pref_listener_field=ResolvedSymbol("origin_pref_listener", "", "y", "y", "field", SymbolConfidence.VERIFIED),
            pref_key_field=ResolvedSymbol("origin_pref_key_field", "", "G:Ljava/lang/String;", "H:Ljava/lang/String;", "field", SymbolConfidence.VERIFIED),
        )
        plan = self.migrator.plan_origin_symbols_update(symbols)
        self.assertTrue(plan.has_changes)
        self.assertIn("Updated Preference key field to 'H'", plan.changes)

    # 17. Scheduler onStartTask rename -> fingerprint name migrated, identical name -> NO-OP
    def test_scheduler_on_start_task_rename(self):
        from harness.core.symbols import BraveNotificationSchedulerSymbols

        def plan_for(name):
            sym = ResolvedSymbol("scheduler_on_start_task", "", "c", name, "method", SymbolConfidence.VERIFIED)
            return self.migrator.plan_scheduler_symbols_update(BraveNotificationSchedulerSymbols(sym))

        renamed = plan_for("zq")
        self.assertTrue(renamed.has_changes)
        self.assertIn('name = "zq",\n            returnType = "I",', renamed.modified_content)
        self.assertEqual(len(renamed.original_content.splitlines()), len(renamed.modified_content.splitlines()))

    # 18. Scheduler resolver must not index missing parameters
    def test_scheduler_resolver_single_param_method(self):
        from harness.core.symbols import SymbolResolver

        method = MagicMock(return_type="I", parameters=["Landroid/content/Context;"], full_name="X->c(Landroid/content/Context;)I")
        method.name = "c"
        index = MagicMock()
        index.find_class.return_value = MagicMock(methods=[method])
        resolved = SymbolResolver(index).resolve_notification_scheduler_symbols()
        self.assertEqual(resolved.on_start_task_method.new_symbol, "c")

    # 19. Current telemetry offsets are read per ABI from the patch source
    def test_current_telemetry_offsets(self):
        arm64 = self.migrator.current_telemetry_offsets(is_arm32=False)
        arm32 = self.migrator.current_telemetry_offsets(is_arm32=True)
        self.assertEqual(len(arm64["crashpad.chromium.org"]), 2)
        self.assertEqual(set(arm64), set(arm32))
        self.assertNotEqual(arm64["cr.brave.com"], arm32["cr.brave.com"])


if __name__ == "__main__":
    unittest.main()
