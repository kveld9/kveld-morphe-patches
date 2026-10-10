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

### Step 0b — Already-onboarded app (automatic differential mode, never ask)
If Step 0 recon shows the target package is already registered (`Constants.COMPATIBILITY_*` + `TargetApp` entry + existing `patches/.../<app>/` suite for the same version), DO NOT ask the user what to do and DO NOT re-run Step 1. Proceed automatically in differential-audit mode:
1. Run Step 3 gates against the existing suite to establish the green baseline.
2. Differential recon: DEX telemetry-class inventory vs hooked methods, manifest components vs blocked lists, native `.so` strings vs redirections, behavioral checklist (`new-telemetry-app` section 3.J) vs existing patches. Every gap needs file:line evidence before delegating.
3. Launch Step 2 workers ONLY for confirmed gaps (exclusive paths, same rules); skip workers entirely when no gap survives evidence.
4. Continue with Step 4 (device + traffic) and Step 5 (atomic `fix(<app>)` commits only for applied gaps, `audit-stack` over the pending range).
Report `no gaps found` with gate evidence when the suite already covers the surface.

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
`validation/smoke_install.py` on ABI-matched device points to base plus generated splits for APKM/XAPK targets (glob `build/test-<app_id>*.apk` via `install-multiple`, never base alone when splits exist; `.apk` extension on every split); uninstall after.
Traffic audit (mandatory analysis, never a strings-only pass):
1. Capture with on-device tcpdump on the Wi-Fi interface (`-i wlan0`, EN10MB — never `-i any`/SLL2), detached (`nohup ... >/dev/null 2>&1 &`, stop by PID file, never `pkill -f` pattern which self-matches), time-boxed (not `-c`-truncated), SLL/linktype-aware parsing. Two windows: baseline (app force-stopped, 60 s) and exercise (launch + monkey/clicks through first-run screens, 3+ min, fresh install so TLS hellos fall inside the window). Private DNS forced `off` during capture with mandatory restore (`settings put global private_dns_mode off` / restore `hostname`); per-UID `iptables owner` isolation (ACCEPT app UID, DROP 10000-19999) with counter readout and full rule removal after. UDP/443 DROP to force TCP/TLS is optional (effect unmeasured); lifecycle/job forcing (`jobscheduler run -f`, background/foreground cycles) is pending validation. Analyze with scapy-in-venv or tshark, never a hand-rolled parser as sole evidence.
2. Per-UID attribution via `/proc/net/xt_qtaguid/stats` (root) before/after exercise; report the target UID byte delta.
3. Flow table with IN/OUT packet+byte counts per remote peer:port (direction from SLL ptype, never guessed). TLS SNI per flow via ClientHello extension 0x0000 parsing of reassembled client streams; QUIC (UDP/443) Initial count when present.
4. DNS blind-spot check: state whether port 53 or DoT/DoH (853/443) carried resolution; names resolved under Private DNS are unobservable and must be reported as such.
5. Verdict only on observed flows: contacted hosts + owning evidence (SNI/DNS/IP-range), explicit caveats (window length, guest vs logged-in state, ECH/DoH limits). Abort (never a silent-telemetry claim) if the capture is empty or the hellos predate the window.
A `strings` hostname grep alone is NOT an audit and never closes this step.
Optimal capture stack (use when available, in this order): (a) isolate the app (dedicated profile/device, force-stop everything else, or per-UID VPN capture) since pcap alone cannot attribute flows on a noisy device; (b) `settings put global private_dns_mode off` during the audit to force plaintext port-53 DNS logging; (c) MITM via mitmproxy with user CA (the Network Security patch already trusts user CAs) + block outbound UDP/443 to force TLS over TCP, yielding full URLs/payloads instead of SNI-only; (d) analyze with tshark instead of hand-rolled parsers; (e) exercise authenticated/paired states, not just guest, since backend hosts only appear there.

### Step 5 — Close
One atomic commit per patch/toggle (`feat(<app>)`/`fix(<app>)`, plain ASCII English, no emojis, no attribution), registration and doc guide folded into the first patch commit. Before closing the task, run `audit-stack` over the pending range (e.g. `origin/<base>..HEAD`, never per-commit auto-audit), verifying `git status` clean of task scope while preserving foreign files. NEVER push or open PRs without explicit user instruction.
