# AGENTS.md

Autonomous AI agent execution harness and engineering governance guide for **Morphe Patches** (`com.kveld9.morphe`).

---

## 1. Stack & Environment Detection

| Component | Technology / Tool | Version / Spec |
| :--- | :--- | :--- |
| **Patcher Runtime** | Morphe Patcher | `1.8.0` (`app.morphe.patcher`) |
| **Gradle Plugin** | `app.morphe.patches` | `1.3.3` |
| **Build Tool** | Gradle Wrapper | `9.6.1` (Bin distribution) |
| **Languages** | Kotlin (Compiler flag: `-Xcontext-parameters`), Java (Extension SDK), Smali (dexlib2 `d92701d947`), Python 3.x | JVM 17+ Target (CI: Temurin JDK 21) |
| **Binary Targets** | ARM64-v8a (`libchrome.so`, Dex APKs), Android APK / APKM | Chromium 130+ / Brave Core v1.93.x, Gboard Lite v18.0.x |
| **CI / Release Toolchain** | `semantic-release` (v25.0.9), `gradle-semantic-release-plugin` (v1.10.3), `@MorpheApp/changelog` | Conventional Commits |

---

## 2. Architecture & Design Patterns

The repository is organized into two Gradle subprojects with distinct responsibilities:

```
morphe-patches/
├── patches/                 # MPP (Morphe Patch Package) Engine
│   └── src/main/kotlin/
│       ├── app/morphe/patches/
│       │   ├── brave/       # Specific Brave Browser patch implementations
│       │   ├── gboard/      # Specific Gboard Lite patch implementations
│       │   └── shared/      # Centralized Compatibility contracts (Constants.kt)
│       └── util/            # Patch list metadata generator (PatchListGenerator.kt)
├── extensions/              # MPE (Morphe Patch Extension) DEX Payloads
│   └── extension/src/main/  # Optional companion Java/Kotlin runtime hooks
├── harness/                 # Python Automated RE & Update Harness
├── validation/              # Physical & Runtime ADB Test Harness
├── gradle/                  # Version catalogs and wrapper config
└── .github/                 # Actions CI/CD workflows and README generators
```

### Core Architectural Contracts

1. **Declarative Metadata & Single Source of Truth**:
   - `app.morphe.patches.shared.Constants`: Every patch must strictly consume centralized constants (`Constants.COMPATIBILITY_BRAVE`, `Constants.COMPATIBILITY_GBOARD`) instead of instantiating redundant inline `Compatibility(...)` objects.
   - Target versions, app colors, package names, and download source hints are maintained exclusively in `Constants.kt`.

2. **Patch Typology & Delegation**:
   - **`bytecodePatch`**: High-level Dalvik AST manipulation using `dexlib2` fingerprints, instruction registers extraction (`OneRegisterInstruction`, `TwoRegisterInstruction`), and inline Smali injection.
   - **`resourcePatch`**: Android XML DOM tree transforms (`res/xml/*.xml`, `AndroidManifest.xml`) executed prior to bytecode patching.
   - **`rawResourcePatch`**: Deterministic byte-level ELF binary modification of `lib/arm64-v8a/libchrome.so` with strict offset validation, pre-patch fingerprint assertion, and null-padded ASCII redirection.
   - **Dependency Chaining**: Composite patches must declare execution hierarchies explicitly via `dependsOn(subPatch1, subPatch2)`.

3. **Smali Hook Conventions**:
   - Hooks must maintain register stability (`p0`, `p1`, `v0`, `v1`).
   - Obfuscated class fields must be verified against current target Dex files before modification.
   - Reflection bridges (e.g., `setAccessible(true)`) are used when accessing internal cross-DEX preference listeners to avoid `IllegalAccessError`.

---

## 3. Session Governance & Modes of Operation

Every agent session must start by explicitly determining the session mode:

```text
MODE = INSTALL/VALIDATE  |  MODE = OPERATE
```

### Mode Definitions:
1. **`INSTALL/VALIDATE`**: Active when setting up, repairing, or auditing the harness framework itself.
   - **Strict Invariant**: No business logic modifications, no feature implementations, and no patch updates during this session.
2. **`OPERATE`**: Active when the harness is intact and a business requirement (new patch, bugfix, version bump) is being executed.
   - Operates under strict **Scope Lock**, baseline checks, and quality-left verification.

---

## 4. Operational Workflow & Scope Control

Tasks in `MODE = OPERATE` must strictly follow this lifecycle:

```text
INSPECT & BASELINE ➜ SCOPE LOCK ➜ MODIFY (Engineer) ➜ RISK GATE (Auditor) ➜ E2E VERIFY
```

### Step 1: `INSPECT & BASELINE`
- Inspect working tree (`git status -u`). Identify pre-existing modifications.
- Run baseline verification proportional to the scope (e.g., `unittest`, `check`).
- Classify any pre-existing failures (`PREEXISTING`, `ENVIRONMENT`).

### Step 2: `SCOPE LOCK`
ROOT defines the initial boundary before any modification:
- Target files and symbols.
- Expected behavioral delta vs. preserved invariants.
- **Scope Expansion Rule**: If Engineer hits an out-of-scope dependency:
  `STOP ➜ REPORT ➜ ROOT DECISION (APPROVE / REJECT / HUMAN)`.

### Step 3: `MODIFY` (Minimalist Engineer)
- Search/replace discipline with contextual lines.
- No whole-file regenerations.
- Classify changes: `SEMANTIC`, `FORMATTER`, `TOOLING-INDUCED`, `PREEXISTING`.

### Step 4: `RISK GATE & AUDIT`
Evaluate blast radius. Trigger mandatory **Auditor** review (`.agents/agents/auditor.md`) for:
- Bytecode / Smali instructions or Dalvik register changes.
- Native ELF binary patching (`libchrome.so`).
- Changes in shared contracts (`Constants.kt`).
- AMOLED theme injections or preference listener bridges.

### Step 5: `E2E VERIFICATION & SMOKE LAUNCH GATES`
When adding or updating any patch, the following gates are **MANDATORY**:
1. **Full-Suite Patching (`E2E Patch Execution`)**: Build and apply the **entire set of available patches** against the target APK.
2. **Code Injection Verification**: Assert that the modified bytecode/resources/ELF offsets were correctly injected into the final APK.
3. **Smoke Launch Verification (Zero-Crash Baseline)**: Verify that the patched APK launches cleanly without runtime crashes or uncaught startup exceptions.

---

## 5. Guardrails & Strict Constraints (What NOT to Do)

### ⛔ Critical Anti-Patterns & Prohibitions

1. **DO NOT Edit Generated Release Artifacts Manually**:
   - Never manually modify or commit `patches-list.json`, `patches-bundle.json`, or `CHANGELOG.md`. These are automatically managed by `release.yml` and `semantic-release`.
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
6. **DO NOT Modify Protected Harness Governance Files**:
   - `AGENTS.md`, `.agents/agents/engineer.md`, and `.agents/agents/auditor.md` cannot be modified as a side effect of a product task.
7. **Anti-Loop Prohibition**:
   - If a proposed fix fails two consecutive times, halt immediately, re-evaluate the root cause, or request human decision.

---

## 6. Deterministic & Inferential Verification Commands

### A. Fast Local / Unit Checks (Quality-Left)
```bash
# Run Python harness unit tests (RE engine, AST contracts)
python -m unittest discover harness/tests

# Run AGP lint and Kotlin compile checks
./gradlew.bat check

# Run Kotlin unit and integration tests
./gradlew.bat test
```

### B. Patch Build & Artifact Generation
```bash
# Build Android extension DEX + Morphe Patch Package (.mpp)
./gradlew.bat build

# Compile standalone .mpp bundle to patches/build/libs/
./gradlew.bat buildAndroid

# Generate updated patches-list.json from compiled .mpp
./gradlew.bat generatePatchesList
```

### C. Reverse Engineering & Automated APK Audit
```bash
# Audit an APK non-destructively
python harness/update.py <path-to-apk> --audit

# Execute minimal source update, build, and catalog sync
python harness/update.py <path-to-apk> --update
```

### D. Physical Device Runtime Harness (ADB)
```bash
# Run automated on-device test suite (battery, sync, PTR, smoke launch)
python validation/physical_harness/run_harness.py
```
