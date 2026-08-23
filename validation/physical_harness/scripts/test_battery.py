"""
TEST 1 — Battery Optimization & OS Listener Test Runner (Audited)
"""

from __future__ import annotations

import json
import time
from pathlib import Path
from common import AdbDevice, SERVER_PORT

def run_battery_test(device: AdbDevice, mode: str, output_dir: Path) -> dict:
    print(f"\n[TEST 1 - BATTERY] Running {mode.upper()} mode on device {device.serial} ({device.get_abi()})...")
    
    device.clear_logcat()
    device.force_stop()
    device.setup_reverse_port(SERVER_PORT)

    # 1. Start App with Battery Test page
    test_url = f"http://localhost:{SERVER_PORT}/battery_test.html"
    print(f" -> Launching Brave with diagnostic page: {test_url}")
    device.start_app(test_url)
    time.sleep(3.0)

    # 2. Trigger artificial battery level and status changes
    print(" -> Triggering ADB battery status changes (Level 80 -> 75 -> 70, Charging true -> false)...")
    device.set_battery_status(2) # Charging
    device.set_battery_level(80)
    time.sleep(1.0)

    device.set_battery_level(75)
    time.sleep(1.0)

    device.set_battery_status(3) # Discharging
    device.set_battery_level(70)
    time.sleep(1.5)

    # 3. Capture Logcat & Diagnostics
    logcat = device.dump_logcat()
    device.reset_battery()

    battery_manager_logs = [line for line in logcat.splitlines() if any(k in line.lower() for k in ["batterystatusmanager", "cr_batterystatusmanager", "cr_batterymonitorimpl", "action_battery_changed"])]
    
    # Audit Rule: In release builds of Chromium, BatteryStatusManager does not output verbose logcat on every broadcast by default.
    # We must explicitly separate direct log observability from broadcast delivery.
    has_direct_log_trace = len(battery_manager_logs) > 0 and any("cr_BatteryStatusManager" in l for l in battery_manager_logs)

    metrics = {
        "mode": mode,
        "test": "battery_optimization",
        "timestamp": time.time(),
        "device_abi": device.get_abi(),
        "battery_log_count": len(battery_manager_logs),
        "battery_manager_logs": battery_manager_logs[:20],
        "has_direct_log_trace": has_direct_log_trace,
        "observability_status": "DIRECT_LOG_AVAILABLE" if has_direct_log_trace else "NO_DIRECT_RELEASE_LOG",
        "raw_logcat_path": str(output_dir / f"battery_{mode}_logcat.txt")
    }

    with open(output_dir / f"battery_{mode}_logcat.txt", "w", encoding="utf-8") as f:
        f.write(logcat)

    with open(output_dir / f"battery_{mode}_result.json", "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    print(f" -> Test 1 ({mode.upper()}) complete. Direct log traces observed: {has_direct_log_trace}")
    return metrics

if __name__ == "__main__":
    import sys
    mode_arg = sys.argv[1] if len(sys.argv) > 1 else "vanilla"
    out_dir = Path("results") / mode_arg
    out_dir.mkdir(parents=True, exist_ok=True)
    dev = AdbDevice()
    run_battery_test(dev, mode_arg, out_dir)
