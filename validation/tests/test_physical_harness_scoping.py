"""
Unit tests for the physical harness log scoping helpers.

Regression guards for the review findings on the #146 scoping change:
filter_logcat_by_pids() only parses strings, so the PID and registration
logic is covered here instead of needing a device.
"""

import sys
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1] / "physical_harness" / "scripts"
if str(SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SCRIPTS))

import common  # noqa: E402
import test_background_sync  # noqa: E402


def _device_with_pid_output(output: str) -> common.AdbDevice:
    device = common.AdbDevice.__new__(common.AdbDevice)
    device.shell = lambda command: output
    return device


class TestFilterLogcatByPids(unittest.TestCase):

    def test_single_process_lines_are_kept(self):
        device = _device_with_pid_output("1234")
        text = "\n".join([
            "10-17 01:23:45.678  1234  5678 I Brave   : target line",
            "10-17 01:23:45.679  9999  1111 I Other   : foreign line",
        ])
        kept = device.filter_logcat_by_pids(text)
        self.assertEqual(len(kept), 1)
        self.assertIn("target line", kept[0])

    def test_multiple_processes_are_all_kept(self):
        device = _device_with_pid_output("1234 5678")
        text = "\n".join([
            "10-17 01:23:45.678  1234  5678 I Brave   : browser process",
            "10-17 01:23:45.679  5678  5679 I Brave   : renderer process",
            "10-17 01:23:45.680  9999  1111 I Other   : foreign line",
        ])
        kept = device.filter_logcat_by_pids(text)
        self.assertEqual(len(kept), 2)
        self.assertTrue(all("Brave" in line for line in kept))

    def test_not_running_keeps_nothing(self):
        device = _device_with_pid_output("")
        text = "10-17 01:23:45.678  9999  1111 I Other   : foreign line"
        self.assertEqual(device.filter_logcat_by_pids(text), [])

    def test_non_logcat_lines_are_ignored(self):
        device = _device_with_pid_output("1234")
        text = "\n".join([
            "--------- beginning of main",
            "short",
            "10-17 01:23:45.678  1234  5678 I Brave   : target line",
        ])
        kept = device.filter_logcat_by_pids(text)
        self.assertEqual(len(kept), 1)
        self.assertIn("target line", kept[0])

    def test_dump_logcat_enforces_threadtime(self):
        seen = []
        device = common.AdbDevice.__new__(common.AdbDevice)

        class _Result:
            stdout = ""

        def fake_cmd(args, check=True):
            seen.append(args)
            return _Result()

        device.cmd = fake_cmd
        device.dump_logcat()
        device.dump_logcat("Brave")
        self.assertEqual(seen[0][:4], ["logcat", "-d", "-v", "threadtime"])
        self.assertEqual(seen[1][:4], ["logcat", "-d", "-v", "threadtime"])
        self.assertEqual(seen[1][-2:], ["-s", "Brave"])


class TestRegistrationState(unittest.TestCase):

    def test_accepted_when_evidence_exists(self):
        state = test_background_sync.derive_registration_state(["sw-sync: tick"])
        self.assertEqual(state, "REGISTRATION_ACCEPTED")

    def test_rejected_wins_over_other_lines(self):
        state = test_background_sync.derive_registration_state([
            "sw-sync: tick",
            "periodicsync rejected: permission denied",
        ])
        self.assertEqual(state, "REGISTRATION_REJECTED")

    def test_unknown_without_evidence(self):
        self.assertEqual(test_background_sync.derive_registration_state([]), "REGISTRATION_UNKNOWN")


if __name__ == "__main__":
    unittest.main()
