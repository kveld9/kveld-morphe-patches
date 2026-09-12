"""
Unit tests for TikLite TikTok patch contracts and invariants.
"""

import unittest
from harness.tiktok.contracts import TIKTOK_PATCH_CONTRACTS, PatchContract


class TestTikTokContracts(unittest.TestCase):

    def test_all_contracts_have_unique_ids(self):
        ids = [c.patch_id for c in TIKTOK_PATCH_CONTRACTS]
        self.assertEqual(len(ids), len(set(ids)), "Duplicate patch_id found in TIKTOK_PATCH_CONTRACTS")

    def test_all_contracts_have_valid_target_types(self):
        valid_types = {"bytecode", "resource", "raw_resource"}
        for c in TIKTOK_PATCH_CONTRACTS:
            self.assertIn(c.target_type, valid_types, f"Invalid target_type '{c.target_type}' in {c.name}")

    def test_critical_telemetry_contracts_defined(self):
        required_telemetry = {"unified_telemetry_tracker_silencer"}
        existing_ids = {c.patch_id for c in TIKTOK_PATCH_CONTRACTS}
        for req in required_telemetry:
            self.assertIn(req, existing_ids, f"Required telemetry contract '{req}' missing from suite")

    def test_slimmer_contracts_defined(self):
        required_slimmers = {"locale_slimmer", "core_asset_debloat"}
        existing_ids = {c.patch_id for c in TIKTOK_PATCH_CONTRACTS}
        for req in required_slimmers:
            self.assertIn(req, existing_ids, f"Required slimmer contract '{req}' missing from suite")

    def test_slimmer_patches_logging_invariants(self):
        from pathlib import Path
        slimmer_dir = Path(__file__).resolve().parents[2] / "patches" / "src" / "main" / "kotlin" / "app" / "morphe" / "patches" / "tiktok" / "slimmer"
        self.assertTrue(slimmer_dir.exists(), f"Slimmer directory not found at {slimmer_dir}")

        slimmer_files = list(slimmer_dir.glob("*.kt"))
        self.assertGreaterEqual(len(slimmer_files), 3, "Expected at least 3 slimmer patches")

        for patch_file in slimmer_files:
            content = patch_file.read_text(encoding="utf-8")
            # Invariant 1: Must contain consolidated summary println
            self.assertIn("println(", content, f"{patch_file.name} must emit a completion log")
            self.assertTrue("Saved" in content or "Skipped" in content, f"{patch_file.name} summary log must report saved space or skip status")

            # Invariant 2: Prohibit per-file spam (no println inside forEach)
            lines = content.splitlines()
            inside_loop = False
            for idx, line in enumerate(lines, 1):
                stripped = line.strip()
                if "forEach {" in stripped or ".walkTopDown()" in stripped:
                    inside_loop = True
                if inside_loop and "println(" in stripped:
                    self.fail(f"Violation in {patch_file.name}:{idx}: Found println inside file loop. Slimmers must emit single consolidated log.")
                if inside_loop and stripped == "}":
                    inside_loop = False

    def test_all_bytecode_patches_dynamic_verbose_logging_invariants(self):
        """Ensures 100% of bytecode patches implement dynamic counter tracking and try/catch note logging."""
        from pathlib import Path
        import re

        patches_dir = Path(__file__).resolve().parents[2] / "patches" / "src" / "main" / "kotlin" / "app" / "morphe" / "patches" / "tiktok"
        bytecode_files = [
            p for p in patches_dir.rglob("*Patch.kt")
            if "slimmer" not in str(p)
        ]
        self.assertGreaterEqual(len(bytecode_files), 5, "Expected at least 5 bytecode/UI/privacy/media patches")

        for pfile in bytecode_files:
            content = pfile.read_text(encoding="utf-8")
            # 1. Must track dynamic counters or mutations
            has_counter = bool(re.search(r'var\s+(patched|hooked|patchedTasks|count)', content))
            self.assertTrue(has_counter, f"{pfile.name} MUST track hooks dynamically via 'var patched = 0'")

            # 2. Must capture exceptions with informative notes
            self.assertIn("catch (e: Exception)", content, f"{pfile.name} MUST wrap Fingerprints in try/catch to capture diagnostic notes")

            # 3. Must report summary log with dynamic count
            has_dynamic_summary = bool(re.search(r'println\(.*(\$patched|\$hooked|\$patchedTasks|\$count).*\)', content))
            self.assertTrue(
                has_dynamic_summary,
                f"{pfile.name} MUST report dynamic execution summary interpolating the counter variable (e.g. 'Applied $patched ...')"
            )


if __name__ == "__main__":
    unittest.main()
