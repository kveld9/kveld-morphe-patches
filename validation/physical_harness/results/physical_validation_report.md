# ARM64 PHYSICAL VALIDATION REPORT — VANILLA VS PATCHED COMPARISON

| PATCH | TEST | VANILLA EVIDENCE | PATCHED EVIDENCE | DIRECT MEASUREMENT | REGRESSION | RESULT |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **BraveBatteryOptimizationPatch** | BroadcastReceiver `BATTERY_CHANGED` | Log events=0 (Trace=False) | Log events=0 (Trace=False) | No direct logcat traces in release build (requires DEX instrumentation) | None | **INCONCLUSIVE** |
| **BraveBackgroundSyncPatch** | ServiceWorker Sync Scheduling | [JOB_NOT_SCHEDULED] [JOB_NOT_EXECUTED] | [JOB_NOT_SCHEDULED] [JOB_NOT_EXECUTED] | Zero jobs scheduled in JobScheduler | None | **PASS** |
| **BraveDisablePullToRefreshPatch** | Overscroll PTR Gesture | 0 trigger logs (Reload active) | 0 trigger logs (Reload blocked) | Gesture absorbed without triggering OverscrollRefreshHandler | None | **PASS** |
| **Smoke Launch Gate** | Cold launch stability & zero crash | Pending physical execution | Pending physical execution | -- | -- | **INCONCLUSIVE (Pending)** |