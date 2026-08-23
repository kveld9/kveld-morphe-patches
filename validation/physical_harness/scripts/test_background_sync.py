"""
TEST 2 — Background Sync & Periodic Sync Test Runner (Audited)
"""

from __future__ import annotations

import json
import time
from pathlib import Path
from common import AdbDevice, SERVER_PORT

def run_background_sync_test(device: AdbDevice, mode: str, output_dir: Path) -> dict:
    print(f"\n[TEST 2 - BACKGROUND SYNC] Running {mode.upper()} mode on device {device.serial}...")

    device.clear_logcat()
    device.force_stop()
    device.setup_reverse_port(SERVER_PORT)

    # 1. Start App with Background Sync test page
    test_url = f"http://localhost:{SERVER_PORT}/background_sync_test.html"
    print(f" -> Launching Brave with Background Sync test: {test_url}")
    device.start_app(test_url)
    time.sleep(4.0)

    # 2. Inspect initial JobScheduler state
    jobs_initial = device.get_dumpsys_jobscheduler()

    # 3. Send app to background
    print(" -> Sending app to background (HOME key)...")
    device.press_key("KEYCODE_HOME")
    time.sleep(3.0)

    # 4. Check JobScheduler & Alarms when backgrounded
    jobs_bg = device.get_dumpsys_jobscheduler()
    alarms_bg = device.get_dumpsys_alarms()

    # 5. Capture Logcat
    logcat = device.dump_logcat()

    sync_logs = [line for line in logcat.splitlines() if any(k in line.lower() for k in ["backgroundsync", "periodicsync", "cr_playserviceschecker", "sw-sync", "sw-periodic"])]

    # Analysis of job scheduling states:
    # State A: Registration status (from logs/diag)
    registration_state = "REGISTRATION_ACCEPTED" if not any("rejected" in l.lower() for l in sync_logs) else "REGISTRATION_REJECTED"
    
    # State B: Job Scheduling in Android JobScheduler
    job_scheduled = "BackgroundSync" in jobs_bg or "PeriodicBackgroundSync" in jobs_bg or "BackgroundSyncOneShot" in jobs_bg
    job_scheduling_state = "JOB_SCHEDULED" if job_scheduled else "JOB_NOT_SCHEDULED"

    # State C: Job Execution in ServiceWorker
    job_executed = any("sw-sync" in l.lower() or "sw-periodic" in l.lower() for l in sync_logs)
    execution_state = "JOB_EXECUTED" if job_executed else "JOB_NOT_EXECUTED"

    metrics = {
        "mode": mode,
        "test": "background_sync",
        "timestamp": time.time(),
        "device_abi": device.get_abi(),
        "registration_state": registration_state,
        "job_scheduling_state": job_scheduling_state,
        "execution_state": execution_state,
        "sync_logs_count": len(sync_logs),
        "sync_logs": sync_logs[:25],
        "jobscheduler_dump_snippet": jobs_bg[:1000],
        "alarms_dump_snippet": alarms_bg[:1000],
    }

    with open(output_dir / f"sync_{mode}_jobscheduler.txt", "w", encoding="utf-8") as f:
        f.write(jobs_bg)

    with open(output_dir / f"sync_{mode}_logcat.txt", "w", encoding="utf-8") as f:
        f.write(logcat)

    with open(output_dir / f"sync_{mode}_result.json", "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)

    print(f" -> Test 2 ({mode.upper()}) complete. Status: [{registration_state}] -> [{job_scheduling_state}] -> [{execution_state}]")
    return metrics

if __name__ == "__main__":
    import sys
    mode_arg = sys.argv[1] if len(sys.argv) > 1 else "vanilla"
    out_dir = Path("results") / mode_arg
    out_dir.mkdir(parents=True, exist_ok=True)
    dev = AdbDevice()
    run_background_sync_test(dev, mode_arg, out_dir)
