"""
Unit Tests for Gboard AMOLED and Theme Duplication Audit.
Demonstrates adversarial blocking when duplicate themes, assets, or injections are detected.
"""

import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch
from harness.gboard.themes import GboardThemeAuditor, ThemeAuditReport
from harness.core.dex import DexIndex, IndexedClass, IndexedMethod


class TestGboardThemes(unittest.TestCase):

    def test_theme_duplicate_injection_blocks(self):
        dex = DexIndex()
        # Mock ThemeListingFragment with 2 duplicate references to color_black
        cls = IndexedClass(
            dex_name="classes.dex",
            name="Lcom/google/android/apps/inputmethod/libs/theme/listing/ThemeListingFragment;",
            access_flags=1,
            superclass="Ljava/lang/Object;",
            interfaces=[],
            class_def=None,
        )
        m1 = IndexedMethod(
            dex_name="classes.dex",
            class_name=cls.name,
            name="f",
            parameters=["Landroid/os/Bundle;"],
            return_type="V",
            access_flags=1,
            encoded_method=MagicMock(),
            _referenced_strings={"color_black_1"},
        )
        m2 = IndexedMethod(
            dex_name="classes.dex",
            class_name=cls.name,
            name="anotherMethod",
            parameters=[],
            return_type="V",
            access_flags=1,
            encoded_method=MagicMock(),
            _referenced_strings={"color_black_2"},
        )
        cls.methods = [m1, m2]
        dex.classes_by_name[cls.name] = cls

        with patch("zipfile.ZipFile") as mock_zip:
            mock_zip.return_value.__enter__.return_value.namelist.return_value = [
                "assets/theme/style_sheet_color_black.binarypb",
                "assets/theme/style_sheet_color_common.binarypb",
            ]
            auditor = GboardThemeAuditor(Path("dummy.apk"), dex)
            report = auditor.audit_theme_pipeline()

            self.assertFalse(report.passed)
            self.assertTrue(report.duplicate_theme_detected)
            self.assertTrue(any("Duplicate AMOLED" in r for r in report.blocking_reasons))

    def test_theme_missing_assets_blocks(self):
        dex = DexIndex()
        with patch("zipfile.ZipFile") as mock_zip:
            mock_zip.return_value.__enter__.return_value.namelist.return_value = []
            auditor = GboardThemeAuditor(Path("dummy.apk"), dex)
            report = auditor.audit_theme_pipeline()

            self.assertFalse(report.passed)
            self.assertFalse(report.black_sheet_present)
            self.assertTrue(any("style_sheet_color_black.binarypb" in r for r in report.blocking_reasons))


if __name__ == "__main__":
    unittest.main()
