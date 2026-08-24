"""
Unit Tests for Gboard Patch Contracts and Invariants.
Verifies contract parsing, invariant rules, and adversarial failure cases.
"""

import unittest
from pathlib import Path
from unittest.mock import MagicMock
from harness.gboard.contracts import get_all_gboard_contracts, GboardPatchContract
from harness.gboard.invariants import GboardInvariantsChecker, InvariantsReport, InvariantResult
from harness.core.apk import ApkMetadata
from harness.core.dex import DexIndex, IndexedClass


class TestGboardContracts(unittest.TestCase):

    def test_all_17_contracts_present(self):
        contracts = get_all_gboard_contracts()
        self.assertEqual(len(contracts), 17)
        patch_ids = {c.patch_id for c in contracts}
        self.assertIn("gboard_amoled", patch_ids)
        self.assertIn("gboard_signature_bypass", patch_ids)
        self.assertIn("gboard_block_telemetry", patch_ids)
        self.assertIn("gboard_disable_workmanager", patch_ids)
        self.assertIn("gboard_force_incognito", patch_ids)
        self.assertIn("gboard_clone", patch_ids)
        self.assertIn("gboard_resource_slimmer", patch_ids)

    def test_invariants_package_mismatch_blocks(self):
        meta = ApkMetadata(
            package_name="com.wrong.package",
            version_name="18.0.3",
            version_code=123,
            sha256="abc",
            file_size=1000,
            dex_files=["classes.dex"],
            has_arm64_libchrome=False,
        )
        dex = DexIndex()
        checker = GboardInvariantsChecker(meta, dex, Path("nonexistent.apk"))
        report = checker.check_all()
        self.assertFalse(report.passed)
        self.assertTrue(any("Package mismatch" in r for r in report.blocking_reasons))

    def test_invariants_missing_ime_service_blocks(self):
        meta = ApkMetadata(
            package_name="com.google.android.inputmethod.latin",
            version_name="18.0.3",
            version_code=123,
            sha256="abc",
            file_size=1000,
            dex_files=["classes.dex"],
            has_arm64_libchrome=False,
        )
        dex = DexIndex()  # Empty dex without IME class
        checker = GboardInvariantsChecker(meta, dex, Path("nonexistent.apk"))
        report = checker.check_all()
        self.assertFalse(report.passed)
        self.assertTrue(any("InputMethodService" in r for r in report.blocking_reasons))


if __name__ == "__main__":
    unittest.main()
