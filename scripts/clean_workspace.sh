#!/usr/bin/env bash
# Workspace cleanup script for Morphe Patches repository
# Removes local build artifacts, temporary files, test reports, and bytecode caches.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "🧹 Cleaning up workspace in $REPO_ROOT..."

# 1. Remove temporary reports & diffs
echo "==> Removing generated reports..."
rm -f ./*_HARNESS_REPORT.md ./APK_DIFF.md

# 2. Remove loose APKs & patch bundles in root
echo "==> Removing loose APKs and signatures..."
rm -f ./*.apk ./*.apkm ./*.xapk ./*.mpp ./*.mpe ./*.idsig

# 3. Clean temporary directories if they exist
echo "==> Removing temporary cache directories..."
rm -rf morphe-temporary-files/ morphe-data/ scratch/ apks-ultima-version/

# 4. Clean Python bytecode caches
echo "==> Cleaning Python caches..."
find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
find . -type f -name "*.py[cod]" -delete 2>/dev/null || true
rm -rf .pytest_cache/

# 5. Clean local validation results while keeping directory structure
echo "==> Cleaning uncommitted validation results & crash dumps..."
rm -rf validation/physical_harness/results/patched/* \
       validation/physical_harness/results/vanilla/* \
       validation/physical_harness/results/*.md \
       validation/physical_harness/results/*.txt \
       validation/runtime/crashes/* \
       validation/combined/* 2>/dev/null || true

echo "✨ Workspace cleanup complete!"
