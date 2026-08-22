# 🔮 Morphe Patches Automated Update & Reverse Engineering Harness

Automated reverse-engineering and patch update harness for **Brave Browser** (`com.brave.browser`) and **Gboard Lite** (`com.google.android.inputmethod.latin`).

---

## 🎯 Architecture Overview

The harness automates the reverse-engineering lifecycle when upstream releases new APKs:

```
Target APK (Brave or Gboard)
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
[harness/gboard/themes.py]     ➜ AMOLED & Theme Duplication Safety Auditor
     ↓
[harness/gboard/invariants.py] ➜ Regression Contracts (IME service, multidex, asset integrity)
     ↓
[harness/migration/migrator]   ➜ Minimal Source Update (Constants.kt, Kotlin AST)
     ↓
[Gradle / Toolchain]           ➜ check, buildAndroid, generatePatchesList, README sync
     ↓
[harness/reporting/]           ➜ Structured Markdown Reports (BRAVE_HARNESS_REPORT.md / GBOARD_HARNESS_REPORT.md)
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
```

### 2. Update and build for a new version
When ready to migrate patches to the new version:

```bash
# For Gboard
python harness/update.py <path-to-gboard.apk> --update

# For Brave
python harness/update.py <path-to-brave.apk> --update
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
