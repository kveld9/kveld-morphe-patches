# AGENTS.md

Autonomous agent execution harness and engineering governance guide for **Morphe Patches** (`com.kveld9.morphe`).

---

## 1. Stack & Environment Detection

The repository uses declarative configurations. Never assume or hardcode version numbers; always inspect the authoritative source of truth:

| Component | Technology | Authoritative Source of Truth |
| :--- | :--- | :--- |
| **Binary Targets & Versions** | Brave, Gboard Lite, Hevy, TikTok, NokoPrint, Xiaomi Earbuds | `app.morphe.patches.shared.Constants` (`Constants.kt`) |
| **Patcher Runtime** | Morphe Patcher Engine | `gradle/libs.versions.toml` (`versions.morphe-patcher`, `versions.smali`) |
| **Gradle Plugin** | `app.morphe.patches` | `settings.gradle.kts` (`plugins { id(...) }`) |
| **Build Tool** | Gradle Wrapper | `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl`) |
| **Languages & Tooling** | Kotlin (`-Xcontext-parameters`), Java, Smali, Python | JVM 17+ (CI: Temurin JDK 21 in `release.yml`), Python in `./venv/` (`requirements.txt`) |
| **CI / Release Toolchain** | `semantic-release` ecosystem | `package.json` & `.releaserc` |


---

## 2. Architecture & Design Patterns

The repository is organized into two Gradle subprojects with distinct responsibilities:

```
morphe-patches/
├── patches/                 # MPP (Morphe Patch Package) Engine
│   └── src/main/kotlin/
│       ├── app/morphe/patches/
│       │   ├── brave/       # Specific Brave Browser patch implementations
│       │   ├── chromium/    # Shared Chromium engine patch implementations
│       │   ├── gboard/      # Specific Gboard Lite patch implementations
│       │   ├── hevy/        # Specific Hevy patch implementations
│       │   ├── nokoprint/   # Specific NokoPrint patch implementations
│       │   ├── tiktok/      # Specific TikTok patch implementations
│       │   ├── xiaomi/      # Specific Xiaomi Earbuds patch implementations
│       │   ├── shared/      # Centralized Compatibility contracts (Constants.kt) and shared helpers
│       │   └── universal/   # Universal patches (no compatibleWith), applicable to any APK
│       └── util/            # Patch list metadata generator (PatchListGenerator.kt)
├── extensions/              # MPE (Morphe Patch Extension) DEX Payloads
│   └── extension/src/main/  # Companion Java runtime hooks (compiled to extension.mpe)
├── harness/                 # Python Automated RE & Update Harness
├── validation/              # Physical & Runtime ADB Test Harness
├── gradle/                  # Version catalogs and wrapper config
├── .agents/skills/          # Canonical agent runtime skills
└── .github/                 # Actions CI/CD workflows and README generators
```

### Core Architectural Contracts

1. **Declarative Metadata & Single Source of Truth**:
   - `app.morphe.patches.shared.Constants`: Every patch must strictly consume centralized constants (`Constants.COMPATIBILITY_BRAVE`, `Constants.COMPATIBILITY_GBOARD`, `Constants.COMPATIBILITY_HEVY`, `Constants.COMPATIBILITY_TIKTOK`, `Constants.COMPATIBILITY_NOKOPRINT`, `Constants.COMPATIBILITY_XIAOMI_EARBUDS`) instead of instantiating redundant inline `Compatibility(...)` objects.
   - Target versions, app colors, package names, and download source hints for all active targets are maintained exclusively in `Constants.kt`.

   - **Single Target Version Invariant**: Every supported application MUST target strictly ONE version (the latest supported release) in `targets = listOf(AppTarget(...))`. Never retain multiple version targets or legacy fallback code for older versions. When bumping an application target, completely replace previous version targets and synchronize all documentation references.
   - **Universal Patches**: Omitting `compatibleWith(...)` produces a universal patch applicable across any target APK in Morphe Manager / CLI (e.g. `LocaleResourceSlimmerPatch`, `DpiResourceSlimmerPatch`).
   - **Multi-Target Varargs**: Patches targeting multiple package variants declare them via varargs, e.g. `compatibleWith(targetA, targetB)`.

2. **Patch Typology & Delegation**:
   - **`bytecodePatch`**: High-level Dalvik AST manipulation using `dexlib2` fingerprints, instruction registers extraction (`OneRegisterInstruction`, `TwoRegisterInstruction`), and inline Smali injection.
   - **`resourcePatch`**: Android XML DOM tree transforms (`res/xml/*.xml`, `AndroidManifest.xml`) executed prior to bytecode patching.
   - **`rawResourcePatch`**: Deterministic byte-level ELF and asset binary modifications, including:
     a) `lib/arm64-v8a/libchrome.so`: Strict offset validation, pre-patch fingerprint assertion, multi-candidate offset resolution, and null-padded ASCII host redirection.
     b) Companion `.so` bloat trimming: In-situ zeroing (`writeBytes(byteArrayOf())`) of unused bundled native binaries.
     c) React Native Hermes bytecode: Dynamic parsing and prologue patching of `assets/index.android.bundle` (HBC).
   - **Dependency Chaining**: Composite patches declare execution hierarchies explicitly via `dependsOn(subPatch1, subPatch2)`.
   - **Runtime Extension Payloads**: When bytecode hooks require companion Java/Kotlin runtime logic, patches declare `extendWith("extensions/extension.mpe")`.

3. **Smali Hook Conventions**:
   - Hooks must maintain register stability (`p0`, `p1`, `v0`, `v1`).
   - Obfuscated class fields must be verified against current target Dex files before modification.
   - Reflection bridges (e.g., `setAccessible(true)`) are used when accessing internal cross-DEX preference listeners to avoid `IllegalAccessError`.

4. **Surgical Diagnostic Telemetry (Debugging Contract)**:
   Every patch MUST emit concise, high-signal telemetry to stdout (captured by Morphe Manager / CLI logs `[WARN] [STDIO]: [...]`) so issues are immediately identifiable from user logs without decompiling:
   - **Standardized Prefix**: Every log line must start with the bracketed patch name prefix: `[Patch Name] ...`.
   - **Granular Target Tracking & Zero Zombie Fingerprints**: Track applied hooks dynamically (`var patched = 0`). During development/triage, individual risky/obfuscated hooks may catch exceptions and log notes (`println("[Patch Name] Target note: ${e.message}")`) to pinpoint target shifts without crashing early. However, before declaring completion or committing, all targets must be updated to resolve cleanly or obsolete hooks pruned so that ZERO fingerprint mismatch exceptions are caught or logged.
   - **Consolidated Interpolated Summary**: Emit a single concise conclusion line (`println("[Patch Name] Applied $patched hooks -> feature disabled.")`).
   - **Failure & Guard Transparency**: If an early return occurs (missing feature, unsupported architecture, or optional inputs absent), log an explicit descriptive reason (`println("[Patch Name] Skipped: Reason...")`).
   - **Zero Loop Spam**: Never place `println` inside `walkTopDown()` or high-volume loops; aggregate deltas and report final saved KB/MB, pruned directories, or count metrics.

---

## 3. Operational Workflow & Scope Discipline

All tasks must follow this systematic execution lifecycle:

```text
INSPECT & BASELINE -> SCOPE LOCK -> MINIMAL IMPLEMENTATION -> QUALITY GATES -> ADVERSARIAL AUDIT
```

### Step 1: `INSPECT & BASELINE`
- Inspect working tree (`git status -s`). Distinguish pre-existing modifications from active task work.
- Run baseline verification proportional to scope (e.g. `./venv/bin/python -m unittest discover harness/tests`, `./gradlew check`).
- Classify any pre-existing failures (`PREEXISTING`, `ENVIRONMENT`) before modifying files.

### Step 2: `SCOPE LOCK`
- Define target files, classes, and symbols before making changes.
- Identify the expected behavioral delta and the invariants to preserve.
- **Scope Expansion Rule**: If an unexpected dependency outside the active scope needs changes, pause and confirm with the user before expanding scope.

### Step 3: `MINIMAL IMPLEMENTATION`
- Target minimal surgical diffs using contextual replacements; avoid full-file rewrites.
- Respect uncommitted working changes; never run destructive git commands (`git reset --hard`, `git clean -fd`).
- Classify changes clearly: `SEMANTIC`, `FORMATTER`, `TOOLING-INDUCED`, or `PREEXISTING`.

### Step 4: `E2E VERIFICATION & SMOKE LAUNCH GATES`
When adding or updating any patch, the following gates are **MANDATORY**:
1. **Full-Suite Morphe Patcher Execution (`In-Situ Patching Gate`)**:
   After completing ANY modification to any patch (bytecode, resource, or raw binary), you MUST execute the official Morphe Patcher pipeline against the target application APK with **ALL corresponding patches for that app activated**:
   ```bash
   # Execute Morphe Patcher with all patches active for the target app (e.g. gboard, tiktok, brave, hevy, nokoprint)
   ./gradlew runPatchTest -Papp=<targetApp>
   # Or with an explicit APK file path:
   ./gradlew runPatchTest -Papk=/path/to/app.apk
   # When the change touches code behind a patch option, also run with every boolean option forced on:
   ./gradlew runPatchTest -Papp=<targetApp> -PallOptions=true
   ```
   The patching run MUST complete with **100% success** (0 failed patches, 0 fingerprint errors, 0 smali compile errors, 0 exceptions).
   **Quiet Gate Invocation (Agents)**: Agents should send the full log to a git-ignored file and read only the verdict, e.g.:
   ```bash
   ./gradlew runPatchTest -Papp=<targetApp> --console=plain > build/patchtest-<targetApp>.log 2>&1; rc=$?; sed -n '/FINAL PATCHING RESULT/,$p' build/patchtest-<targetApp>.log | grep -v '^\s*at ' || tail -40 build/patchtest-<targetApp>.log; echo "exit=$rc"
   ```
   This is safe because `patches/src/main/kotlin/util/PatchExecutionTest.kt` intercepts the complete stdout/stderr itself, lists every fingerprint mismatch, smali compile error and failed patch inside the `FINAL PATCHING RESULT` block, and exits non-zero; filtering afterwards cannot hide a failure. The verdict is the exit code plus that block, never the filtered text alone. If the block is missing (build or setup failure before patching), the tail is shown instead. Grep the saved log when more context is needed. The same pattern applies to `./gradlew check` (full log to `build/`, show the tail plus `FAILED`/`error:` lines).
   **Zero-Smali-Compile-Error Invariant**: The inline smali compiler silently drops any instruction it cannot assemble (e.g. `[6,8] Invalid register: v22`) instead of failing. Non-range invokes (`invoke-* {...}`) can only address `v0`-`v15`; in large methods `p` registers map above that, so use the `/range` form or copy values into low registers first. The runner fails on these errors (`Detected Smali Compile Errors`).
   **Zero-Fingerprint-Mismatch Invariant (Definitive Completion Gate)**:
   A patch update or the creation of a new patch is **NEVER** complete if there is even a single `Failed to match the fingerprint` or `fingerprint mismatch` in the patcher logs (standard or verbose).
   Wrapping hooks in `try-catch` with diagnostic log lines (`Target note: ${e.message}`) is strictly a temporary development aid to pinpoint shifted targets during initial triage; it is **STRICTLY PROHIBITED** to leave failing fingerprints caught by try-catch in final/committed code. Every single hook target must either:
   a) Be updated with the newly shifted obfuscated class/method in the target APK version.
   b) Or, if the underlying feature was completely removed upstream by the app developers, the obsolete hook/fingerprint MUST be pruned from the patch entirely rather than left as a failing zombie fingerprint.
   Never consider any patch task complete if this gate has not executed or has any failure or fingerprint mismatch.
2. **Code Injection Verification**: Assert that the modified bytecode/resources/ELF offsets were correctly injected into the final APK.
3. **Smoke Launch Verification (Zero-Crash Baseline)**: Verify that the patched APK launches cleanly without runtime crashes or uncaught startup exceptions.

### Step 5: `ADVERSARIAL RISK GATE & AUDIT`
For non-trivial logic, Smali hooks, native ARM64 patching (`libchrome.so`), or shared compatibility changes (`Constants.kt`), perform a rigorous post-change adversarial audit (correctness, integration, security/privacy, and repository hygiene, re-running the mandatory gates above) before declaring completion.

---

## 4. Guardrails & Strict Constraints (What NOT to Do)

### Critical Anti-Patterns & Prohibitions

1. **DO NOT Edit Generated Release Artifacts Manually**:
   - Never manually modify or commit `patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`, or the README patch tables between `<!-- PATCHES_START -->` and `<!-- PATCHES_END -->`. These are automatically managed by `release.yml` and `semantic-release` (`.releaserc.js` regenerates the README tables via `.github/scripts/generate_patches_readme.py`).
2. **DO NOT Inline Hardcoded `Compatibility` Declarations**:
   - Avoid creating new `Compatibility(...)` blocks inside individual `.kt` patch files. Always reference or extend `app.morphe.patches.shared.Constants`.
3. **DO NOT Perform Unvalidated Native Binary Writes**:
   - In `rawResourcePatch`, never write replacement bytes without first asserting:
     a) File existence (`if (!soFile.exists()) return@execute`).
     b) Bounds safety (`offset + length <= raf.length()`).
     c) Original byte fingerprint verification (`buf.contentEquals(expectedOriginal)`).
4. **DO NOT Destroy Uncommitted Working Changes**:
   - Never run destructive git commands (`git reset --hard`, `git clean -fd`, `git checkout .`) on local modifications.
5. **DO NOT Add Unjustified Dependencies**:
   - Do not introduce external libraries, frameworks, or agent infrastructure without explicit architectural necessity.
6. **DO NOT Modify Harness Governance Files Arbitrarily**:
   - `AGENTS.md` and governance contracts cannot be modified as an unintended side effect of a product task.
7. **Anti-Loop Prohibition**:
   - Outside `/audit-stack`: if a proposed fix fails two consecutive times, halt immediately, re-evaluate the root cause, or request human decision. Inside `/audit-stack`, its autonomous remediation loop and circuit breaker (maximum 5 cycles per issue) apply instead.
8. **DO NOT Hardcode Usernames, Device Serials, Local Paths, or Repository Slugs**:
   - Never embed developer usernames, adb device serials, absolute machine paths, or hardcoded repository slugs. Derive paths and repository slugs dynamically from runtime contexts or environment variables (e.g. `GITHUB_REPOSITORY`), autodetect connected ADB devices when possible, and supply sensible generic fallbacks.
9. **Strict Privacy, PII & Diagnostic Output Sanitization**:
   - Never commit raw device diagnostic outputs, logcats, dumpsys logs, tombstones, screenshots, or crash traces to version control.
   - All runtime diagnostic dumps (`dumpsys jobscheduler`, `dumpsys alarm`) must be strictly filtered to the target package name (`PACKAGE_NAME`) to prevent leaking user Google accounts, installed third-party apps, or device hardware serials.
   - Diagnostic HTTP servers and test runners must strictly bind to loopback (`127.0.0.1`) and never expose ports on `0.0.0.0` or local network interfaces.
10. **Strict Secret & Environment Containment**:
    - Never commit `.env`, `local.properties`, private keys (`*.key`, `*.pem`), or signing keystores.
    - All validation runtime outputs (`validation/runtime/`, `validation/physical_harness/results/`) must remain strictly excluded via `.gitignore` and sanitized by `scripts/clean_workspace.sh`.
11. **Metadata Synchronization Integrity**:
    - Do not run `./gradlew generatePatchesList` in the repository checkout: it rewrites the tracked `patches-list.json`, which the release pipeline regenerates (see item 1). When patch names, options, default values, or descriptions change, verify catalog registration by running the patch list generator against the built `.mpp` from a temporary working directory outside the repository, and confirm the expected entries appear.
12. **DO NOT Declare Patch Tasks Complete Without the In-Situ Patching Gate**: see Section 3, Step 4 (`runPatchTest`, 100% success, zero fingerprint mismatches, zero smali compile errors).
13. **Strict Prohibition of Emojis in Code, Scripts & Tooling**:
    - Under no circumstances should emojis or unicode pictographs be used anywhere in codebase source files, including Kotlin, Java, Python, Smali, Bash/Shell scripts, Gradle build files, configuration files, test files, diagnostic telemetry, or CLI/runtime logs.
    - All code, logs, comments, and console outputs MUST strictly use clean, standard ASCII / plain-text formatting (e.g. `[INFO]`, `[WARN]`, `[PASS]`, `[FAIL]`, `[AUDIT]`, `[BUILD]`). Emojis are tolerated exclusively in end-user documentation (such as `README.md`) if already present, but are strictly prohibited in codebase implementation files and tooling.
14. **Mandatory Atomic Commits Policy**:
    - Every new patch, distinct patch option/toggle, patch update, or bugfix MUST be contained within its own dedicated atomic commit (`feat(<target>): ...` or `fix(<target>): ...`). That single commit MUST encapsulate both the patch implementation code (Kotlin, Smali, resources, ELF) AND its accompanying documentation entries in the app-specific docs (`docs/apps/*.md`, `docs/universal-patches.md`) and any hand-maintained `README.md` section outside the generated patch tables.
    - **Strict Prohibition of Multi-Patch & Multi-Feature Grouping & Documentation Batching**:
      a) Never separate patch implementation and patch documentation into separate commits during patch creation, option addition, or updates.
      b) Never bundle multiple independent patches or distinct toggle features together into a single commit. When adding multiple features/toggles to a composite patch, commit each feature/toggle independently along with its documentation to guarantee clean git bisectability and issue tracking.
      c) Any bugfix or behavioral correction targeting a patch MUST be committed in its own standalone `fix(<target>): ...` commit rather than bundled into feature or documentation commits.
      d) Under NO circumstances should documentation for multiple distinct patches or options be grouped or batched into a collective `docs:` commit. Each patch or feature is an autonomous unit: 1 patch / 1 feature toggle / 1 fix = 1 single commit.
    - **Standalone Commits Scope**:
      a) **Tooling & Test Harness** (`test(harness): ...` or `refactor(harness): ...`): Must be isolated from patch logic.
      b) **Cross-Compatibility & Shared Contracts** (`feat(patches): ...` or `feat(shared): ...`): Isolated when bridging shared features across apps outside an individual patch unit.
      c) **Standalone Technical Documentation** (`docs(<app>): ...` or `docs: ...`): Strictly reserved for documentation-only changes that are NOT part of a patch creation or update (e.g. typos, global architecture notes, general README updates).
    - Each commit must adhere strictly to Conventional Commits to ensure clean `@semantic-release` changelog generation and bisectability.
15. **Mandatory Direct Commit, Push-on-Request & Strict No-PR Policy**:
    - Automatically commit every completed, verified unit of work as soon as it is finished. Always commit; never ask whether to commit.
    - **Default Working Branch `dev`**: All routine development happens on `dev`. `main` is the stable release line and is touched ONLY by an explicit user-ordered promotion merge (`dev` into `main`, merge commit, never squash).
    - **Push Only on Explicit Request**: Never push autonomously or propose pushing. Run `git push` only when the user explicitly requests it in the current conversation, after verifying that `git log origin/dev..HEAD` and `git diff origin/dev..HEAD` contain only intended commits and files (use `origin/main` as the base ONLY when promoting to stable).
    - **Push Side Effects (CI Releases)**: Pushing `dev` triggers an experimental pre-release (`vX.Y.Z-experimental.N`); pushing `main` triggers a clean stable release. Never push `main` casually. Avoid back-to-back pushes to the same branch: a running Release workflow pushes its own `chore: Release` commit, and a concurrent push fails it with `fetch first` (self-recovers on the next run, but wastes CI).
    - **Strict No-PR**: This repository and maintainer DO NOT work with Pull Requests (PRs). Work is committed directly or managed locally without PRs. Under NO circumstances should you ask to proceed with push or PR closing sequences, and NEVER generate PR titles or PR descriptions.
    - Commits MUST strictly be atomic, isolated, independent, clean, and concise.
16. **Prohibition of Multi-Version Target Retentions**: see the Single Target Version Invariant in Section 2; this also covers `README.md` and every other documentation reference.
17. **Strict Prohibition of In-App Settings Screens & Dynamic UI Panels**:
    - Never propose or implement in-app settings activities, preference menus, or overlay panels to toggle patches dynamically at runtime. Dynamic toggles introduce extreme ProGuard/DexGuard fragility across weekly upstream bumps and disk I/O overhead on performance-critical paths. All configurable parameters must be compile/patch-time options via Morphe Manager / CLI (`stringOption`), except for declarative AndroidX XML preference injections authorized in `docs/out-of-scope.md` (such as Gboard Enhancements). Authoritative boundary: `docs/out-of-scope.md`.
18. **Strict Prohibition of Server-Side Bypasses, DRM, and Account Exploits**:
    - Never attempt to bypass server-side subscription paywalls, unlock cloud-restricted content, access private accounts, or defeat DRM protections. Patches operate strictly on client-side bytecode and local application assets. Authoritative boundary: `docs/out-of-scope.md`.
19. **Strict Prohibition of Embedded Download Managers & Feature Bloat**:
    - Never embed third-party media download engines, torrent clients, or custom UI skins inside host applications. Patches strictly unlock native capabilities (e.g. watermark-free saves) and debloat assets without bloating target APKs with complex third-party subsystems. Authoritative boundary: `docs/out-of-scope.md`.

---

## 5. Deterministic & Inferential Verification Commands

### A. Fast Local / Unit Checks (Quality-Left)
```bash
# Run Python harness unit tests (using project virtualenv)
./venv/bin/python -m unittest discover harness/tests

# Run AGP lint and Kotlin compile checks (use ./gradlew on Linux/macOS, gradlew.bat on Windows)
./gradlew check

# Run Kotlin unit and integration tests
./gradlew test
```

### B. Patch Build, In-Situ Patcher Verification & Artifact Generation
```bash
# Build Android extension DEX + Morphe Patch Package (.mpp)
./gradlew build

# Compile standalone .mpp bundle to patches/build/libs/
./gradlew buildAndroid

# Execute Morphe Patcher against target APK with 100% patch activation
./gradlew runPatchTest -Papp=gboard
./gradlew runPatchTest -Papp=tiktok
./gradlew runPatchTest -Papp=brave
./gradlew runPatchTest -Papp=hevy
./gradlew runPatchTest -Papp=nokoprint
./gradlew runPatchTest -Papp=xiaomi_earbuds
# Or auto-detect target app from git diff / candidate downloads:
./gradlew runPatchTest
# Force every boolean patch option on (covers hooks behind disabled-by-default toggles):
./gradlew runPatchTest -Papp=tiktok -PallOptions=true

# Generate updated patches-list.json from compiled .mpp
./gradlew generatePatchesList
```

### C. Reverse Engineering & Automated APK Audit
```bash
# Audit an APK non-destructively
./venv/bin/python harness/update.py <path-to-apk> --audit

# Execute minimal source update, build, and catalog sync
./venv/bin/python harness/update.py <path-to-apk> --update
```

### D. Physical Device Runtime Harness (ADB)
```bash
# Run automated on-device test suite (battery, sync, smoke launch)
./venv/bin/python validation/physical_harness/run_harness.py
```

#### Standing Authorization: Autonomous Physical-Device Smoke Testing
On-device install/launch verification via attached ADB devices is pre-authorized
standing (user grant, no per-step confirmation required). When a task requires
validating a patched APK on hardware, the agent must perform the device test
itself instead of asking the user to run it. Permitted without asking:
`adb devices` discovery, pushing test APKs, `adb install` / `install -r`,
launching the test package (`am start` / `monkey`), `logcat` capture scoped to
the test package, `dumpsys package` reads, and screenshots of the test app.
Destructive actions are limited to the package under active validation on the
attached lab device: `adb uninstall` only when a signature or split conflict
blocks installing the test build, never for unrelated packages, never
factory reset, never touching other apps' data. Prefer reinstall (`-r`) to
preserve data. Always report device model, package, actions taken, and the
launch verdict (alive PID vs FATAL) as evidence in the final report.
Triage lab bootstrap (Frida/JADX) is pre-authorized via `scripts/ensure_lab_frida.sh` (idempotent; supports `ANDROID_SERIAL` override; server does not survive reboot).

