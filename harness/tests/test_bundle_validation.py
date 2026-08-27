"""
Unit Tests for MPP Bundle Integrity and Dalvik DEX Packaging.
Verifies that compiled .mpp artifacts contain classes.dex (Dalvik bytecode), extensions.mpe,
and valid manifest metadata so they never fail to load in Android Morphe Manager.
"""

import unittest
import zipfile
from pathlib import Path
from harness.migration.validator import AdversarialValidator
from harness.core.dex import DexIndex


class TestMppBundleIntegrity(unittest.TestCase):

    def setUp(self):
        self.repo_root = Path(__file__).resolve().parent.parent.parent
        self.libs_dir = self.repo_root / "patches" / "build" / "libs"

    def test_mpp_bundle_contains_classes_dex(self):
        """Ensures every compiled .mpp has a valid classes.dex with >10KB size."""
        mpp_files = [f for f in self.libs_dir.glob("*.mpp") if not f.name.endswith("-sources.mpp") and not f.name.endswith("-javadoc.mpp")]
        self.assertGreater(len(mpp_files), 0, f"No .mpp bundle found in {self.libs_dir}. Run './gradlew buildAndroid'")

        for mpp in mpp_files:
            with zipfile.ZipFile(mpp, "r") as zf:
                namelist = zf.namelist()
                self.assertIn("classes.dex", namelist, f"{mpp.name} is missing classes.dex! Always run 'buildAndroid'.")
                dex_size = zf.getinfo("classes.dex").file_size
                self.assertGreater(dex_size, 10000, f"classes.dex in {mpp.name} is suspiciously small: {dex_size} bytes")

    def test_mpp_bundle_contains_extension_mpe(self):
        """Ensures every compiled .mpp has extension.mpe included."""
        mpp_files = [f for f in self.libs_dir.glob("*.mpp") if not f.name.endswith("-sources.mpp") and not f.name.endswith("-javadoc.mpp")]
        for mpp in mpp_files:
            with zipfile.ZipFile(mpp, "r") as zf:
                self.assertIn("extensions/extension.mpe", zf.namelist(), f"{mpp.name} is missing extensions/extension.mpe")

    def test_validator_integrity_method(self):
        """Tests that AdversarialValidator.assert_mpp_bundle_integrity() returns True for valid builds."""
        dex_index = DexIndex()
        validator = AdversarialValidator(self.repo_root, dex_index)
        passed, msg = validator.assert_mpp_bundle_integrity()
        self.assertTrue(passed, f"Bundle integrity assertion failed: {msg}")

    def test_all_patches_have_dynamic_logging(self):
        """Ensures every Kotlin patch file has diagnostic/verbose logging."""
        dex_index = DexIndex()
        validator = AdversarialValidator(self.repo_root, dex_index)
        passed, msg = validator.assert_patches_dynamic_logging()
        self.assertTrue(passed, msg)


if __name__ == "__main__":
    unittest.main()
