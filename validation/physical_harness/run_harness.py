"""
Master Physical Test Suite Orchestrator for ARM64 Android Device (Audited).
Usage:
    python run_harness.py --mode vanilla --patch pull-to-refresh
    python run_harness.py --mode patched --all
    python run_harness.py --compare
"""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent / "scripts"
SERVER_DIR = Path(__file__).parent / "server"
RESULTS_DIR = Path(__file__).parent / "results"
sys.path.insert(0, str(SCRIPT_DIR))

from common import AdbDevice, LocalTestServer
from test_battery import run_battery_test
from test_background_sync import run_background_sync_test
from test_pull_to_refresh import run_pull_to_refresh_test
from compare_results import compare_runs

def _parse_and_validate_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Morphe Patches Physical ARM64 Validation Suite (Audited)")
    parser.add_argument("--mode", choices=["vanilla", "patched"], help="Target testing mode (vanilla or patched)")
    parser.add_argument("--device", "--serial", dest="serial", default="df286add", help="Specific ADB device serial (default: df286add)")
    parser.add_argument("--patch", choices=["battery", "background-sync", "pull-to-refresh"], help="Specific individual patch to test")
    parser.add_argument("--all", action="store_true", help="Run all 3 tests sequentially")
    parser.add_argument("--compare", action="store_true", help="Generate strict comparison report (PASS / FAIL / INCONCLUSIVE)")
    args = parser.parse_args()

    if args.compare:
        return args

    if not args.mode:
        print("ERROR: Please specify --mode vanilla, --mode patched, or --compare")
        parser.print_help()
        sys.exit(1)

    if not args.patch and not args.all:
        print("ERROR: Please specify either --patch <battery|background-sync|pull-to-refresh> or --all")
        parser.print_help()
        sys.exit(1)

    return args


def _execute_tests(device: AdbDevice, args: argparse.Namespace, mode_out_dir: Path):
    if args.all or args.patch == "battery":
        run_battery_test(device, args.mode, mode_out_dir)
        time.sleep(2)

    if args.all or args.patch == "background-sync":
        run_background_sync_test(device, args.mode, mode_out_dir)
        time.sleep(2)

    if args.all or args.patch == "pull-to-refresh":
        run_pull_to_refresh_test(device, args.mode, mode_out_dir)
        time.sleep(2)


def main():
    args = _parse_and_validate_args()
    if args.compare:
        compare_runs(RESULTS_DIR)
        return

    print("==================================================")
    print(f" MORPHE PATCHES — PHYSICAL TEST SUITE ({args.mode.upper()})")
    print("==================================================")

    device = AdbDevice(serial=args.serial)
    abi = device.get_abi()
    print(f"Connected Device: {device.serial} ({device.get_model()})")
    print(f"Detected ABI: {abi} | Android SDK: {device.get_sdk_version()}")

    if "arm64" not in abi:
        print(f"\n[WARNING] Device ABI '{abi}' is not ARM64! Target BraveMonoarm64 may crash if not native ARM64.")

    mode_out_dir = RESULTS_DIR / args.mode
    mode_out_dir.mkdir(parents=True, exist_ok=True)

    server = LocalTestServer(directory=SERVER_DIR)
    print("\nStarting local diagnostic server on http://localhost:8080...")
    server.start()

    try:
        _execute_tests(device, args, mode_out_dir)
    finally:
        print("\nStopping local diagnostic server...")
        server.stop()

    print("\n==================================================")
    print(f" {args.mode.upper()} RUN COMPLETED!")
    print(f" Artifacts saved in: {mode_out_dir}")
    print(" To compare after running both modes: python run_harness.py --compare")
    print("==================================================")

if __name__ == "__main__":
    main()
