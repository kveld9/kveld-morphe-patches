"""
Unit Tests for Harness Reporting Architecture.
Verifies consistent, deterministic markdown generation for Brave and Gboard.
"""

import unittest
from harness.reporting.reporter import HarnessReporter, HarnessReportData
from harness.migration.validator import PatchAuditResult, PatchStatus
from harness.gboard.themes import ThemeAuditReport
from harness.gboard.invariants import InvariantsReport, InvariantResult


class TestHarnessReporting(unittest.TestCase):

    def test_brave_report_rendering(self):
        data = HarnessReportData(
            app_name="Brave Browser",
            package_name="com.brave.browser",
            mode="AUDIT",
            old_version="1.93.136",
            new_version="1.93.136",
            old_version_code=None,
            new_version_code=1000,
            apk_sha256="abc123sha",
            apk_file_size=80000000,
            patch_results={
                "braveOriginPatch": PatchAuditResult(
                    patch_name="Brave Origin",
                    status=PatchStatus.VERIFIED,
                    fingerprint_results=[("origin_fp_1", "VERIFIED", "Lcom/brave/Origin;->test()V")],
                    native_checks=[("usage-ping.brave.com", True, "Offset 0x1000")],
                    blocking_reasons=[],
                    evidence=["Exact match"],
                )
            },
            applied_changes=[],
            rejected_changes=[],
            build_passed=True,
            build_output="Build skipped in audit mode.",
            final_status="SUCCESS",
        )

        md = HarnessReporter.render_markdown(data)
        self.assertIn("# 🦁 Brave Browser Patches Harness Report", md)
        self.assertIn("- **Execution Mode**: `[AUDIT]`", md)
        self.assertIn("- **Overall Pipeline Status**: ✅ `SUCCESS`", md)
        self.assertIn("- **Package Name**: `com.brave.browser`", md)
        self.assertIn("| **Brave Origin** | ✅ `VERIFIED` | `1/1 verified` | `1/1 valid` | All structural assertions satisfied. |", md)
        self.assertIn("**Final Pipeline Status**: `SUCCESS`", md)

    def test_gboard_report_rendering(self):
        theme_rep = ThemeAuditReport(
            passed=True,
            black_sheet_present=True,
            common_sheet_present=True,
            listing_fragment_found=True,
            amoled_injection_count=1,
            duplicate_theme_detected=False,
            findings=["Found theme assets"],
            blocking_reasons=[],
        )
        inv_rep = InvariantsReport(
            passed=True,
            results=[
                InvariantResult(
                    invariant_id="package_name_match",
                    passed=True,
                    description="Package name match",
                    details="Matches expected",
                )
            ],
            blocking_reasons=[],
        )
        data = HarnessReportData(
            app_name="Gboard Lite",
            package_name="com.google.android.inputmethod.latin",
            mode="AUDIT",
            old_version="18.0.3",
            new_version="18.0.3",
            old_version_code=None,
            new_version_code=175940530,
            apk_sha256="gboardsha",
            apk_file_size=49000000,
            patch_results={
                "gboard_amoled": PatchAuditResult(
                    patch_name="Add AMOLED Theme",
                    status=PatchStatus.VERIFIED,
                    fingerprint_results=[("amoled_fp", "VERIFIED", "Lcom/theme/Listing;->f()V")],
                    native_checks=[],
                    blocking_reasons=[],
                    evidence=["Unique injection point"],
                )
            },
            theme_report=theme_rep,
            invariants_report=inv_rep,
            applied_changes=[],
            rejected_changes=[],
            build_passed=True,
            build_output="Build skipped in audit mode.",
            final_status="SUCCESS",
        )

        md = HarnessReporter.render_markdown(data)
        self.assertIn("# ⌨️ Gboard Lite Patches Harness Report", md)
        self.assertIn("- **Execution Mode**: `[AUDIT]`", md)
        self.assertIn("- **Package Name**: `com.google.android.inputmethod.latin`", md)
        self.assertIn("## 🎨 Special Theme & AMOLED Duplication Audit", md)
        self.assertIn("## 🛡️ Invariants & Regression Contract Checks", md)
        self.assertIn("| **Add AMOLED Theme** | ✅ `VERIFIED` | `1/1 verified` | All structural assertions satisfied. |", md)
        self.assertIn("**Final Pipeline Status**: `SUCCESS`", md)


if __name__ == "__main__":
    unittest.main()
