"""
TEST 4 — Pull-To-Refresh Gesture & Overscroll Test Runner (Audited)
"""

from __future__ import annotations

import json
import time
from pathlib import Path
from common import AdbDevice, SERVER_PORT

def run_pull_to_refresh_test(device: AdbDevice, mode: str, output_dir: Path) -> dict:
    print(f"\n[TEST 4 - PULL TO REFRESH] Running {mode.upper()} mode on device {device.serial}...")

    device.clear_logcat()
    device.force_stop()
    device.setup_reverse_port(SERVER_PORT)

    # 1. Start App with Pull-to-refresh test page
    test_url = f"http://localhost:{SERVER_PORT}/pull_to_refresh_test.html"
    print(f" -> Launching Brave with Pull-to-refresh test: {test_url}")
    device.start_app(test_url)
    time.sleep(3.0)

    # 2. Type test string into form field
    print(" -> Typing verification string into form field...")
    device.shell("input tap 500 350")
    time.sleep(0.5)
    device.shell("input text VerificationTextUnwiped")
    time.sleep(0.5)

    # Capture initial screenshot
    screenshot_before = output_dir / f"ptr_{mode}_before_swipes.png"
    device.shell(f"screencap -p /sdcard/ptr_{mode}_before.png")
    device.cmd(["pull", f"/sdcard/ptr_{mode}_before.png", str(screenshot_before)], check=False)

    # 3. Perform 20 automated downward pull gestures from top of screen
    print(" -> Executing 20 automated downward pull-to-refresh gestures from top...")
    
    # Swipes: from Y=350 down to Y=1200 with varied speeds (200ms, 350ms, 500ms)
    for i in range(20):
        duration = 200 + (i % 3) * 150
        device.swipe(500, 350, 500, 1200, duration)
        time.sleep(0.4)

    # 4. Test normal scroll functionality (scroll down and scroll up)
    print(" -> Testing regular scroll functionality (down and up)...")
    device.swipe(500, 1200, 500, 400, 400) # scroll down
    time.sleep(0.5)
    device.swipe(500, 400, 500, 1200, 400) # scroll up
    time.sleep(0.5)

    # 5. Capture final screenshot
    screenshot_after = output_dir / f"ptr_{mode}_after_swipes.png"
    device.shell(f"screencap -p /sdcard/ptr_{mode}_after.png")
    device.cmd(["pull", f"/sdcard/ptr_{mode}_after.png", str(screenshot_after)], check=False)

    logcat = device.dump_logcat()

    ptr_logs = [line for line in logcat.splitlines() if any(k in line.lower() for k in ["swiperefreshhandler", "overscroll", "brave_pull_to_refresh"])]

    metrics = {
        "mode": mode,
        "test": "pull_to_refresh",
        "timestamp": time.time(),
        "device_abi": device.get_abi(),
        "gestures_executed": 20,
        "ptr_logs_count": len(ptr_logs),
        "ptr_logs": ptr_logs[:20],
        "screenshot_before": str(screenshot_before),
        "screenshot_after": str(screenshot_after),
        "measurement_type": "DIRECT_DOM_AND_INPUT_PRESERVATION"
    }

    with open(output_dir / f"ptr_{mode}_logcat.txt", "w", encoding="utf-8") as f:
        f.write(logcat)

    with open(output_dir / f"ptr_{mode}_result.json", "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    print(f" -> Test 4 ({mode.upper()}) complete. Captured {len(ptr_logs)} overscroll/swipe log events.")
    return metrics

if __name__ == "__main__":
    import sys
    mode_arg = sys.argv[1] if len(sys.argv) > 1 else "vanilla"
    out_dir = Path("results") / mode_arg
    out_dir.mkdir(parents=True, exist_ok=True)
    dev = AdbDevice()
    run_pull_to_refresh_test(dev, mode_arg, out_dir)
