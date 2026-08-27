# 🔮 Morphe Patches Automated Update & Reverse Engineering Harness

Automated reverse-engineering and patch update harness for **Brave Browser** (`com.brave.browser`), **Vivaldi Browser Snapshot** (`com.vivaldi.browser.snapshot`), and **Gboard Lite** (`com.google.android.inputmethod.latin`).

---

## 🎯 Architecture Overview

The harness automates the reverse-engineering lifecycle when upstream releases new APKs:

```
Target APK (Brave, Vivaldi, or Gboard)
     ↓
[harness/update.py]            ➜ Package Identification & Pipeline Dispatcher
     ↓
[harness/core/apk.py]          ➜ Context-managed Extraction (Version, VersionCode, SHA-256)
     ↓
[harness/core/dex.py]          ➜ Multi-DEX Class & Method Indexing (28,000+ classes, 100,000+ methods)
     ↓
[harness/core/symbols.py]      ➜ Obfuscated Symbol Resolution (Structural callers, types, opcodes)
     ↓
[harness/gboard/contracts.py]  ➜ 18 Gboard Declarative Patch Contracts & Invariants
     ↓
[harness/migration/validator]  ➜ Adversarial Validation Engine (Brave & Vivaldi Contracts)
     ↓
[harness/gboard/themes.py]     ➜ AMOLED & Theme Duplication Safety Auditor
     ↓
[harness/gboard/invariants.py] ➜ Regression Contracts (IME service, multidex, asset integrity)
     ↓
[harness/migration/migrator]   ➜ Minimal Source Update (Constants.kt, Kotlin AST)
     ↓
[Gradle / Toolchain]           ➜ check, buildAndroid, generatePatchesList, README sync
     ↓
[harness/reporting/]           ➜ Structured Markdown Reports (BRAVE / VIVALDI / GBOARD)
```

---

## 🚀 Daily Operational Procedure

### 1. Audit a Target APK (Non-destructive inspection)
Run this command to inspect fingerprints, obfuscated symbol changes, and invariants without modifying code:

```bash
# For Gboard
python harness/update.py <path-to-gboard.apk> --audit

# For Brave
python harness/update.py <path-to-brave.apk> --audit

# For Vivaldi
python harness/update.py <path-to-vivaldi.apk> --audit
```

### 2. Update and build for a new version
When ready to migrate patches to the new version:

```bash
# For Gboard
python harness/update.py <path-to-gboard.apk> --update

# For Brave
python harness/update.py <path-to-brave.apk> --update

# For Vivaldi
python harness/update.py <path-to-vivaldi.apk> --update
```

This will:
1. Validate all fingerprints, contracts, and safety gates.
2. If all checks pass (`VERIFIED`), apply minimal edits to `Constants.kt` and patch source files.
3. Automatically execute `./gradlew buildAndroid`, `./gradlew generatePatchesList`, and sync `README.md`.
4. Output the complete report.

### 3. Run the Harness Test Suite
```bash
python -m unittest discover harness/tests
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
   - Run `python .github/scripts/generate_patches_readme.py kveld9/kveld-morphe-patches main patches-list.json README.md` to refresh the Markdown tables and patch counts.
   - Update high-level summary highlights (e.g. *Key Highlights*) if the scope of features changed.
5. **Harness Verification & Report Generation**:
   - Re-run `python harness/update.py <apk> --audit` to assert zero regressions and update `BRAVE_HARNESS_REPORT.md` or `GBOARD_HARNESS_REPORT.md`.
6. **Harness Integrity & Logging Test Suite**:
   - Execute `python -m unittest discover harness/tests` to verify that `AdversarialValidator.assert_patches_dynamic_logging()` passes across all patch source files.

