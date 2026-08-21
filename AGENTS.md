# AGENTS.md

Autonomous AI agent execution harness and engineering guide for **Morphe Patches** (`com.kveld9.morphe`).

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

## 3. Operational Workflow

All autonomous tasks must strictly follow this 4-step execution lifecycle:

1. **`INSPECT`**:
   - Check working tree with `git status -u` to verify no accidental loss of uncommitted local files.
   - Inspect AST fingerprints, target Dex files, or XML resource schemas before modifying code.
2. **`PLAN`**:
   - Detail changes in modular isolation (DEX hooks vs XML defaults vs native offsets).
   - Verify compatibility against `Constants.COMPATIBILITY_BRAVE` or `Constants.COMPATIBILITY_GBOARD`.
3. **`MODIFY`**:
   - Execute exact, minimal code adjustments adhering to Kotlin DSL and Smali conventions.
   - Ensure native binary writes include boundary and original-byte assertions.
4. **`VERIFY`**:
   - Execute build and validation checks (`./gradlew check`, `./gradlew buildAndroid`, `./gradlew generatePatchesList`).
   - Ensure zero unhandled exceptions and strict conventional commit compliance.

---

## 4. Guardrails & Strict Constraints (What NOT to Do)

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
   - Do not introduce external libraries or Gradle plugins without explicit architectural need.
6. **DO NOT Break Semantic Commit Conventions**:
   - Commits must strictly use Angular/Conventional Commits format (`feat:`, `fix:`, `perf:`, `chore:`, `bump:`, `docs:`) to avoid breaking automated semver calculation.

---

## 5. Verification Commands

Always run these commands locally before submitting code or completing tasks:

### Compile Extension & Patch Engine
```bash
# Build Android extension DEX + Morphe Patch Package (.mpp)
./gradlew.bat build

# Compile standalone .mpp bundle to patches/build/libs/
./gradlew.bat buildAndroid
```

### Validate Patch Metadata & Sync Catalogs
```bash
# Generate updated patches-list.json from compiled .mpp
./gradlew.bat generatePatchesList

# Re-inject markdown tables and sync README.md
python .github/scripts/generate_patches_readme.py kveld9/morphe-patches main patches-list.json README.md
```

### Unit & Integration Tests
```bash
# Run unit and integration tests
./gradlew.bat test
```

### Linting & Code Health
```bash
# Run AGP lint and Kotlin compile checks
./gradlew.bat check
```

### Code Formatter
- `None` (No automatic formatting plugin configured in Gradle; enforce standard Kotlin coding conventions manually).

---

## 6. Automated Patches Update Harness (Brave & Gboard)

When updating Morphe patches for a newly released APK:

```bash
# 1. Non-destructive audit and reverse engineering analysis
python harness/update.py <path-to-apk> --audit

# 2. Minimal source update, build, and catalog sync
python harness/update.py <path-to-apk> --update

# 3. Run harness unit & integration tests
python -m unittest discover harness/tests
```

The harness automatically identifies the package (`com.brave.browser` vs `com.google.android.inputmethod.latin`), applies the corresponding contracts, enforces AMOLED theme duplication invariants, validates AST fingerprints, updates `Constants.kt`, and runs full Gradle and metadata verification.



