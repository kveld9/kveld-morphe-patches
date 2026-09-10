# 🛠️ Building & Development

Instructions and toolchain requirements for compiling, testing, and developing **Morphe Patches**.

---

## 📋 Prerequisites

- **JDK 17+** (JDK 21 Temurin recommended)
- **Gradle 9.6.1** (included via `./gradlew` wrapper)
- **Python 3.x** (used for automated reverse engineering harness and catalog synchronization)
- **Android SDK** (API Level 34+ build-tools if compiling the companion extension)

---

## 🔨 Build Commands

### 1. Build Standalone Patch Package (.mpp)
Compiles both the companion Android extension DEX and the standalone Morphe Patch Package (`.mpp`):

```bash
# Linux / macOS
./gradlew buildAndroid

# Windows
./gradlew.bat buildAndroid
```

The compiled patch package will be generated at:
```text
patches/build/libs/patches-*.mpp
```

---

### 2. Run Verification & Tests
Execute AGP linting, Kotlin compiler checks, and unit tests:

```bash
# Run lint and compilation checks
./gradlew check

# Run Kotlin unit tests
./gradlew test

# Run Python reverse engineering harness tests
python -m unittest discover harness/tests
```

---

### 3. Generate Metadata & Documentation Sync

```bash
# Generate updated patches-list.json from compiled .mpp
./gradlew generatePatchesList

# Sync README markdown tables with patch metadata
python .github/scripts/generate_patches_readme.py kveld9/kveld-morphe-patches main patches-list.json README.md
```
