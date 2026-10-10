---
description: Onboard a new app with telemetry-blocking + debloat patches in parallel via AGY
---

## `/telemetry-app` — Parallel Telemetry + Debloat Onboarding

Target: $ARGUMENTS (APK file in `candidate_apks/`, e.g. `candidate_apks/com.example_1.0.apkm`).

You are the AGY orchestrator (skill `agy-orchestrator`; never edit project code
directly, never use another harness's subagents for implementation). Execute:

### Step 0 — Scope bootstrap
0. Environment bootstrap: `python3 -m venv venv && ./venv/bin/pip install -r requirements.txt`.
1. Record `git status --short`. Isolate any pre-existing dirty files or untracked changes as `FOREIGN_IN_PROGRESS_WORK` (do not require working tree to be clean; never stage or commit foreign files). APK must exist under `candidate_apks/` (gitignored, runner resolves only this directory name without explicit `-Papk`; never commit).
2. Recon (orchestrator shell, read-only): run `mkdir -p scratch` for raw recon dumps, `unzip -l`, badging dump via `${AAPT2:-aapt2}`, manifest xmltree to `$AGY_TMP` (`/tmp/opencode`), telemetry component inventory. Save evidence paths for workers.

### Step 1 — Registration (one AGY worker, `accept-edits`)
New `agy` conversation: add `Constants` entry inside `object Constants` + `TargetApp` entry + README row (matching repository column schema) + `docs/apps/<app_id>.md` skeleton (with mandatory Behavioral Hazards Warning) per `AGENTS.md` section 2. Folding documentation skeleton here avoids Worker A/B collision on docs. Uncommitted; orchestrator commits with the first patch.

### Step 2 — Parallel implementation (TWO AGY workers, exclusive paths)
Launch both in the same turn (background), max two, never the same files:
- Worker A (fresh conversation): skill `telemetry-blocking` including the DEX hosts-rewrite pattern and mandatory method-exclusion list -> `patches/.../<app>/` telemetry patch (manifest purge + verified DEX hooks only, zero zombies, `[Patch Name]` telemetry contract).
- Worker B (fresh conversation): skill `app-debloat` → independent opt-in slimmer patches (one axis per patch, `default=false`).
- Both: `--add-dir` for project + origin reference repo, no `--dangerously-skip-permissions`, native read/edit only (headless denies RunCommand). Same-scope corrections always resume the same conversation; a failed worker is discarded, never integrated by hand.
- On `RESOURCE_EXHAUSTED`: checkpoint, rotate via `~/.gemini/config/skills/agy-orchestrator/scripts/switch_account.py auto` (preauthorized), resume same conversation.

### Step 3 — Gates (orchestrator, writes only to gitignored `build/`)
Run in-situ gates sending output to `build/` (gitignored):
1. Patcher execution requires two passes with `-Pout=build/test-<app_id>.apk` (without this flag the runner will not write the output APK):
   - First pass (defaults): `./gradlew runPatchTest -Papp=<app_id> -Papk=<apk> -Pout=build/test-<app_id>.apk`
   - Second pass (all options forced on): `./gradlew runPatchTest -Papp=<app_id> -Papk=<apk> -PallOptions=true -Pout=build/test-<app_id>.apk`
   (100% success, 0 failed patches, 0 fingerprint mismatches, 0 smali compile errors).
2. RE harness unit tests: `./venv/bin/python -m unittest discover harness/tests`.
3. Project checks: `./gradlew check`.
Injection proof via output-APK manifest dump (`build/test-<app_id>.apk`), never counters alone.

### Step 4 — Device (standing authorization confirmed)
`validation/smoke_install.py` on ABI-matched device points to base plus generated splits for APKM/XAPK targets (glob `build/test-<app_id>*.apk` via `install-multiple`, never base alone when splits exist; `.apk` extension on every split); traffic audit via on-device tcpdump (verifying binary first, packet limit `-c`, non-empty pcap check) with TLS SNI + QUIC Initial inspection; uninstall after. Report hosts + caveats (abort if capture empty, never report false-negative silent telemetry).

### Step 5 — Close
One atomic commit per patch/toggle (`feat(<app>)`/`fix(<app>)`, plain ASCII English, no emojis, no attribution), registration and doc guide folded into the first patch commit. Before closing the task, run `audit-stack` over the pending range (e.g. `origin/<base>..HEAD`, never per-commit auto-audit), verifying `git status` clean of task scope while preserving foreign files. NEVER push or open PRs without explicit user instruction.
