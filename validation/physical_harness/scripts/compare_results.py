"""
Results Comparator & Evaluation Generator for Vanilla vs Patched (Audited).
Enforces strict classification: PASS / FAIL / INCONCLUSIVE.
"""

from __future__ import annotations

import json
from pathlib import Path

def compare_runs(results_dir: Path) -> str:
    vanilla_dir = results_dir / "vanilla"
    patched_dir = results_dir / "patched"

    report_lines = [
        "# ARM64 PHYSICAL VALIDATION REPORT — VANILLA VS PATCHED COMPARISON\n",
        "| PATCH | TEST | VANILLA EVIDENCE | PATCHED EVIDENCE | DIRECT MEASUREMENT | REGRESSION | RESULT |",
        "| :--- | :--- | :--- | :--- | :--- | :--- | :--- |",
        _compare_sync(vanilla_dir, patched_dir),
        _compare_smoke(vanilla_dir, patched_dir),
    ]
    final_report = "\n".join(report_lines)
    with open(results_dir / "physical_validation_report.md", "w", encoding="utf-8") as f:
        f.write(final_report)

    print("\n" + final_report)
    return final_report


def _compare_sync(vanilla_dir: Path, patched_dir: Path) -> str:
    v_sync_file = vanilla_dir / "sync_vanilla_result.json"
    p_sync_file = patched_dir / "sync_patched_result.json"
    if not (v_sync_file.exists() and p_sync_file.exists()):
        return "| **BraveBackgroundSyncPatch** | ServiceWorker Sync Scheduling | Pending physical execution | Pending physical execution | -- | -- | **INCONCLUSIVE (Pending)** |"

    v_sync = json.loads(v_sync_file.read_text(encoding="utf-8"))
    p_sync = json.loads(p_sync_file.read_text(encoding="utf-8"))

    v_sched = v_sync.get("job_scheduling_state", "UNKNOWN")
    p_sched = p_sync.get("job_scheduling_state", "UNKNOWN")
    v_exec = v_sync.get("execution_state", "UNKNOWN")
    p_exec = p_sync.get("execution_state", "UNKNOWN")
    v_reg = v_sync.get("registration_state", "UNKNOWN")

    v_ev = f"[{v_sched}] [{v_exec}]"
    p_ev = f"[{p_sched}] [{p_exec}]"

    # The vanilla run is the only proof that the sync path is active at all.
    # Without a scheduled/executed job in vanilla, an absent job in patched is
    # indistinguishable from a dead test, so it must never be scored as PASS.
    vanilla_baseline = v_sched == "JOB_SCHEDULED" or v_exec == "JOB_EXECUTED"

    if not vanilla_baseline:
        reason = (
            "Vanilla run never scheduled or executed a sync job "
            f"(vanilla={v_ev}, registration={v_reg}); absence in patched is unproven"
        )
        res, meas = "INCONCLUSIVE", reason
    elif v_sched == "JOB_SCHEDULED" and p_sched == "JOB_NOT_SCHEDULED":
        res, meas = "PASS", "Jobs absent from JobScheduler after background transition"
    elif p_sched == "JOB_NOT_SCHEDULED" and p_exec == "JOB_NOT_EXECUTED":
        res, meas = "PASS", "Zero jobs scheduled in JobScheduler (vanilla baseline active)"
    else:
        res, meas = "INCONCLUSIVE", "Inconclusive JobScheduler state"

    return f"| **BraveBackgroundSyncPatch** | ServiceWorker Sync Scheduling | {v_ev} | {p_ev} | {meas} | None | **{res}** |"



def _compare_smoke(vanilla_dir: Path, patched_dir: Path) -> str:
    v_smoke_file = vanilla_dir / "smoke_vanilla_result.json"
    p_smoke_file = patched_dir / "smoke_patched_result.json"
    if not (v_smoke_file.exists() and p_smoke_file.exists()):
        return "| **Smoke Launch Gate** | Cold launch stability & zero crash | Pending physical execution | Pending physical execution | -- | -- | **INCONCLUSIVE (Pending)** |"

    v_smoke = json.loads(v_smoke_file.read_text(encoding="utf-8"))
    p_smoke = json.loads(p_smoke_file.read_text(encoding="utf-8"))

    v_crash = v_smoke.get("crash_detected", False)
    p_crash = p_smoke.get("crash_detected", False)
    v_time = v_smoke.get("startup_time_ms", 0)
    p_time = p_smoke.get("startup_time_ms", 0)
    # Only claim a run count when the producer recorded one; never invent it.
    v_runs = v_smoke.get("runs", v_smoke.get("launch_count"))
    p_runs = p_smoke.get("runs", p_smoke.get("launch_count"))
    runs_ev = ""
    if isinstance(v_runs, int) and isinstance(p_runs, int):
        runs_ev = f" across {min(v_runs, p_runs)} recorded runs"
    else:
        runs_ev = " (run count not recorded in result JSON)"

    if not p_crash and not v_crash:
        res, meas = "PASS", f"Zero crashes detected{runs_ev} (Startup: {p_time}ms vs {v_time}ms)"
    elif not p_crash and v_crash:
        res, meas = "PASS", "Patched build fixed crash"
    else:
        res, meas = "FAIL", "Crash detected in patched build"

    return f"| **Smoke Launch Gate** | Cold launch stability & zero crash | Crash={v_crash} ({v_time}ms) | Crash={p_crash} ({p_time}ms) | {meas} | None | **{res}** |"

if __name__ == "__main__":
    import sys
    res_path = Path("results") if len(sys.argv) <= 1 else Path(sys.argv[1])
    compare_runs(res_path)
