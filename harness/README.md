# 🔮 Morphe Patches Automated Update & Reverse Engineering Harness

Automated reverse-engineering and patch update harness for **Brave Browser** (`com.brave.browser`), **Gboard Lite** (`com.google.android.inputmethod.latin`), **TikTok** (`com.zhiliaoapp.musically`), and **Xiaomi Earbuds** (`com.mi.earphone`).

> [!NOTE]
> Hevy and NokoPrint have no harness pipeline (`harness/pipelines/`). Update them manually and validate with `./gradlew runPatchTest -Papp=hevy` / `-Papp=nokoprint`.

---

## 🎯 Architecture Overview

The harness automates the reverse-engineering lifecycle when upstream releases new APKs:

```
Target APK / APKM / XAPK (Brave, Gboard, TikTok, or Xiaomi Earbuds)
     ↓
[harness/update.py]            ➜ Package Identification & Pipeline Dispatcher
     ↓
[harness/core/apk.py]          ➜ Context-managed Extraction (Version, VersionCode, SHA-256)
     ↓
[harness/core/dex.py]          ➜ Multi-DEX Class & Method Indexing (28,000+ classes, 100,000+ methods)
     ↓
[harness/core/symbols.py]      ➜ Obfuscated Symbol Resolution (Structural callers, types, opcodes)
     ↓
[harness/<target>/contracts]   ➜ Declarative Patch Contracts & Invariants
     ↓
[harness/<target>/validator]   ➜ Adversarial Validation Engine (DEX, AST, Assets)
     ↓
[harness/migration/migrator]   ➜ Minimal Source Update (Constants.kt, Kotlin AST)
     ↓
[Gradle / Toolchain]           ➜ check, buildAndroid, MPP bundle integrity
     ↓
[harness/reporting/]           ➜ Structured Markdown Reports
```

---

## 🚀 Daily Operational Procedure

### 0. Preflight readiness (fail fast on environment issues)
```bash
./venv/bin/python harness/update.py --doctor
```
Checks Python, androguard, Java, the Gradle wrapper, `Constants.kt`,
and ADB (WARN-only, needed solely for on-device smoke tests). It also
reports `[WARN]` when the optional manual triage aids `jadx` and `frida`
are absent; those never fail the verdict and are never pipeline
dependencies.

### Triage aid: resolving a BLOCKED fingerprint (manual, optional)
When `--audit` reports `BLOCKED` (0 or ambiguous targets after an
upstream rename/obfuscation shift), these external tools speed up
finding the shifted target. Neither replaces the mandatory
`./gradlew runPatchTest -Papp=<targetApp>` gate.

1. **jadx** (static): open the APK in jadx-gui, follow Xrefs from the
   blocking reason's string constant or caller, and locate the shifted
   obfuscated class/method. Then update the fingerprint query in
   `harness/<target>/contracts` and re-run `--audit`.
2. **frida** (dynamic, lab device only): before writing a Smali hook,
   confirm the candidate actually does what the patch assumes, e.g.
   `frida -U -f <package-from-Constants.kt> -l /tmp/probe.js --no-pause`.
   Use `jnitrace` for the Java-to-native boundary (e.g. telemetry paths
   reaching into `libchrome.so`). Requires root or Gadget on the attached
   lab device; the repacked APK smoke test stays the faithful verdict.

### 1. Audit a Target APK / APKM (Non-destructive inspection)
Run this command to inspect fingerprints, obfuscated symbol changes, and invariants without modifying code:

```bash
# For Gboard
./venv/bin/python harness/update.py <path-to-gboard.apk> --audit

# For Brave
./venv/bin/python harness/update.py <path-to-brave.apk> --audit

# For TikTok (nodpi APK)
./venv/bin/python harness/update.py <path-to-tiktok.apk> --audit

# For Xiaomi Earbuds (XAPK bundle required)
./venv/bin/python harness/update.py <path-to-earbuds.xapk> --audit
```

### 2. Update and build for a new version
When ready to migrate patches to the new version:

```bash
# For Gboard
./venv/bin/python harness/update.py <path-to-gboard.apk> --update

# For Brave
./venv/bin/python harness/update.py <path-to-brave.apk> --update

# For TikTok (nodpi APK)
./venv/bin/python harness/update.py <path-to-tiktok.apk> --update

# For Xiaomi Earbuds (XAPK bundle required)
./venv/bin/python harness/update.py <path-to-earbuds.xapk> --update
```


This will:
1. Validate all fingerprints, contracts, and safety gates.
2. If all checks pass (`VERIFIED`), apply minimal edits to `Constants.kt` and patch source files.
3. Automatically execute `./gradlew check buildAndroid` and assert MPP bundle integrity, rolling back on failure. Release artifacts (`patches-list.json`, the README patch catalog) are left to CI.
4. Output the complete report.

After any patch source change, the in-situ patching gate is mandatory (see `AGENTS.md`):

```bash
./gradlew runPatchTest -Papp=<targetApp>
```

### 3. Run the Harness Test Suite
```bash
./venv/bin/python -m unittest discover harness/tests
```

---

## 📊 Patch Status Definitions

| Status | Definition | Next Action |
| :--- | :--- | :--- |
| **`VERIFIED`** | All fingerprints have exactly 1 target, signatures match, and structural invariants are satisfied. | Ready for release. |
| **`STATICALLY VERIFIED`** | AST/Smali matches verified statically, but optional companion native library was absent. | Review if APK is universal. |
| **`BLOCKED`** | A fingerprint matched 0 targets or was ambiguous (>1 matches), or a theme duplication was detected. | **HALT.** Inspect structural candidates and update fingerprint. |
| **`NOT AFFECTED`** | Patch is universal or target is unaffected. | No action required. |

---

## 🎨 AMOLED Theme Safety Guarantees

To prevent regressions in Gboard theme customization:
1. **Asset Verification**: Asserts `assets/theme/style_sheet_color_black.binarypb` and `style_sheet_color_common.binarypb` exist and have valid palette bytes.
2. **Duplication Guard**: Asserts `ThemeListingFragment` receives exactly **one** registration call for `assets:theme_package_metadata_color_black.binarypb`. If duplicate registrations or entries are detected, the patch is immediately marked **`BLOCKED`**.
3. **No Blind Fallback**: Never replaces system Light/Dark/Dynamic theme assets.

---

## 🔄 Mandatory Full-Repository Sync Workflow

Whenever a patch is **created, modified, renamed, or updated**, the following repository-wide synchronization workflow is strictly mandatory:

1. **Kotlin Patch Definition (`patches/.../*.kt`)**:
   - Maintain declarative metadata, centralized constants from `Constants.kt`, and register-stable Smali instructions.
   - **Dynamic Diagnostic Logging Contract (MANDATORY)**: Every patch MUST emit concise, single-line dynamic telemetry at runtime during execution (e.g. `println("[Patch Name] ...")`). Static hardcoded strings are prohibited; the log must include runtime counters and resolved symbols (e.g. hooked method count, resolved class names, modified XML attributes, redirected binary endpoints, or saved MB/KB).
2. **Harness Adversarial Contracts (`harness/migration/validator.py` or `harness/gboard/contracts.py`)**:
   - Update or register fingerprint queries matching the exact method signatures and class targets so the harness validates them against the target APK.
3. **Catalog Generation (`patches-list.json`)**:
   - Execute `./gradlew.bat generatePatchesList` (or `./gradlew generatePatchesList`) to re-export the standalone Morphe Patch Package metadata.
4. **Documentation & Table Synchronization (`README.md`)**:
   - The README patch catalog (`PATCHES_START` / `PATCHES_END`) is regenerated automatically by the release pipeline (`.releaserc`); do not edit it by hand. To preview it locally, run `python .github/scripts/generate_patches_readme.py <owner>/<repo> main patches-list.json README.md` and do not commit the result.
   - Update high-level summary highlights (e.g. *Key Highlights*) if the scope of features changed.
5. **Harness Verification & Report Generation**:
   - Re-run `./venv/bin/python harness/update.py <apk> --audit` to assert zero regressions and regenerate the pipeline report (`BRAVE_HARNESS_REPORT.md`, `GBOARD_HARNESS_REPORT.md`, `TIKLITE_HARNESS_REPORT.md`, or `XIAOMI_EARBUDS_HARNESS_REPORT.md`; written to the current directory unless `--output` is given; git-ignored).
6. **In-Situ Patching Gate**:
   - Execute `./gradlew runPatchTest -Papp=<targetApp>` (add `-PallOptions=true` when the change sits behind a patch option) and confirm 0 failed patches, 0 fingerprint mismatches, and 0 smali compile errors.
7. **Harness Integrity & Logging Test Suite**:
   - Execute `./venv/bin/python -m unittest discover harness/tests` to verify that `AdversarialValidator.assert_patches_dynamic_logging()` passes. It only asserts that every patch source contains a `println(`; the dynamic-content rule above is enforced by review, not by the test.

