#!/usr/bin/env python3
"""
Morphe Patches Automated Update & Reverse Engineering Harness.
Supports Brave Browser (com.brave.browser) and Gboard Lite (com.google.android.inputmethod.latin).

Usage:
    python harness/update.py <path-to-apk> [--audit | --update | --dry-run] [--output <report.md>]

Examples:
    python harness/update.py BraveMonoarm64.apk --audit
    python harness/update.py Gboard_v18.0.3.apk --audit
    python harness/update.py Gboard_v18.0.3.apk --update
"""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
if hasattr(sys.stderr, "reconfigure"):
    try:
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

# Add repo root to sys.path
REPO_ROOT = Path(__file__).resolve().parent.parent
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from harness.core.apk import ApkContext
from harness.core.dex import DexIndex
from harness.core.elf import Elf64Analyzer
from harness.core.symbols import SymbolResolver, SymbolConfidence
from harness.core.telemetry import TelemetryScanner
from harness.gboard.validator import GboardAdversarialValidator
from harness.migration.patch_migrator import PatchMigrator
from harness.migration.validator import AdversarialValidator, PatchStatus
from harness.reporting.reporter import HarnessReporter, HarnessReportData


def run_brave_pipeline(apk_ctx: ApkContext, mode: str, output_report: str | None, t0: float) -> int:
    meta = apk_ctx.get_metadata()
    print(f"🦁 Running Brave Patches Pipeline for {meta.package_name} v{meta.version_name}...")

    apk_filename = apk_ctx.apk_path.name.lower()
    is_mono_arm64 = "monoarm64" in apk_filename or ("monochrome" in apk_filename and "arm64" in apk_filename)
    if not is_mono_arm64:
        print(f"⚠️  WARNING: APK filename '{apk_ctx.apk_path.name}' does not indicate a Monochrome ARM64 build.")
        print("⚠️  Morphe Patches targets 'Bravemonoarm64.apk'.")
        print("⚠️  Other variants (Universal, Modern/Trichrome, x86/x64, 32-bit) have divergent R8 bytecode/offsets and will fail fingerprint checks.\n")

    if not meta.has_arm64_libchrome:
        print("⚠️  WARNING: 'lib/arm64-v8a/libchrome.so' not found in APK.")
        print("⚠️  32-bit or non-ARM64 APKs are incompatible with native Brave patches.\n")

    repo_path = REPO_ROOT
    migrator = PatchMigrator(repo_path)

    constants_text = migrator.constants_file.read_text(encoding="utf-8")
    import re
    m_ver = re.search(r'const val BRAVE_TARGET_VERSION = "([^"]+)"', constants_text)
    old_version = m_ver.group(1) if m_ver else "UNKNOWN"

    print(f"🔍 Extracting and indexing {len(meta.dex_files)} DEX files...")
    dex_entries = apk_ctx.extract_dex_bytes()
    dex_index = DexIndex()
    dex_index.index_dex_files(dex_entries)
    print(f"⚡ Indexed {len(dex_index.classes)} classes, {len(dex_index.methods)} methods.")

    elf_analyzer = None
    libchrome_path = apk_ctx.extract_libchrome_path()
    if libchrome_path:
        print("🛡️ Extracting and analyzing ARM64 libchrome.so...")
        elf_analyzer = Elf64Analyzer(libchrome_path)
        print(f"⚡ ELF parsed: valid={elf_analyzer.is_valid}, aarch64={elf_analyzer.is_aarch64}, size={len(elf_analyzer.data):,} bytes")

    print("🧠 Resolving obfuscated members and structural contracts...")
    sym_resolver = SymbolResolver(dex_index)
    symbols = sym_resolver.resolve_all()

    telemetry_report = None
    if elf_analyzer:
        print("🌐 Auditing native telemetry domain offsets...")
        telemetry_scanner = TelemetryScanner(elf_analyzer)
        telemetry_report = telemetry_scanner.audit_known_hosts()

    print("🛡️ Running adversarial validation on all Brave patches...")
    validator = AdversarialValidator(repo_path, dex_index, elf_analyzer)
    patch_results = validator.audit_brave_patches()

    applied_changes = []
    rejected_changes = []

    all_verified = all(res.status in (PatchStatus.VERIFIED, PatchStatus.NOT_AFFECTED) for res in patch_results.values())

    if mode == "update" and all_verified:
        print("✍️ Applying minimal source migrations...")
        plan_const = migrator.plan_constants_update(meta.version_name)
        if plan_const.has_changes:
            migrator.apply_plan(plan_const)
            applied_changes.extend(plan_const.changes)

        if telemetry_report and telemetry_report.known_results:
            plan_telem = migrator.plan_telemetry_hosts_update(telemetry_report.known_results)
            if plan_telem.has_changes:
                migrator.apply_plan(plan_telem)
                applied_changes.extend(plan_telem.changes)

        origin_syms = symbols.get("origin")
        if origin_syms:
            plan_orig = migrator.plan_origin_symbols_update(origin_syms)
            if plan_orig.has_changes:
                migrator.apply_plan(plan_orig)
                applied_changes.extend(plan_orig.changes)

        sched_syms = symbols.get("scheduler")
        if sched_syms:
            plan_sched = migrator.plan_scheduler_symbols_update(sched_syms)
            if plan_sched.has_changes:
                migrator.apply_plan(plan_sched)
                applied_changes.extend(plan_sched.changes)

    elif mode == "update" and not all_verified:
        for p_key, p_res in patch_results.items():
            if p_res.status == PatchStatus.BLOCKED:
                rejected_changes.append(f"Blocked update for '{p_res.patch_name}': {'; '.join(p_res.blocking_reasons)}")

    build_passed = True
    build_output = "Build skipped in audit mode."
    if mode == "update" and all_verified:
        print("🔨 Executing Gradle build, checks, and metadata generation...")
        build_passed, build_output = validator.run_gradle_build_verification()

    final_status = "SUCCESS" if (all_verified and build_passed) else "BLOCKED"

    report_data = HarnessReportData(
        app_name="Brave Browser",
        package_name=meta.package_name,
        mode=mode.upper(),
        old_version=old_version,
        new_version=meta.version_name,
        old_version_code=None,
        new_version_code=meta.version_code,
        apk_sha256=meta.sha256,
        apk_file_size=meta.file_size,
        patch_results=patch_results,
        symbols=symbols,
        telemetry_report=telemetry_report,
        applied_changes=applied_changes,
        rejected_changes=rejected_changes,
        build_passed=build_passed,
        build_output=build_output,
        final_status=final_status,
    )

    md_report = HarnessReporter.render_markdown(report_data)
    print("\n" + md_report + "\n")

    target_report_file = output_report or "BRAVE_HARNESS_REPORT.md"
    Path(target_report_file).write_text(md_report, encoding="utf-8")
    print(f"📄 Report written to {target_report_file}")

    elapsed = time.time() - t0
    print(f"⏱️ Harness completed in {elapsed:.2f}s with status: {final_status}")
    return 0 if final_status == "SUCCESS" else 1


def run_gboard_pipeline(apk_ctx: ApkContext, mode: str, output_report: str | None, t0: float) -> int:
    meta = apk_ctx.get_metadata()
    print(f"⌨️ Running Gboard Lite Patches Pipeline for {meta.package_name} v{meta.version_name}...")

    apk_filename = apk_ctx.apk_path.name.lower()
    ver_lower = meta.version_name.lower()
    is_lite = "lite" in apk_filename or "lite" in ver_lower
    is_arm64 = "arm64" in apk_filename or "arm64" in ver_lower
    if not is_lite:
        print("⚠️  WARNING: APK does not appear to be a Gboard Lite variant.")
        print("⚠️  Morphe Patches targets 'lite_beta-arm64-v8a' (standard Gboard has different DEX layouts).\n")
    if not is_arm64:
        print("⚠️  WARNING: APK does not appear to be ARM64-v8a architecture.\n")

    repo_path = REPO_ROOT
    migrator = PatchMigrator(repo_path)

    constants_text = migrator.constants_file.read_text(encoding="utf-8")
    import re
    m_ver = re.search(r'const val GBOARD_TARGET_VERSION = "([^"]+)"', constants_text)
    old_version = m_ver.group(1) if m_ver else "UNKNOWN"

    print(f"🔍 Extracting and indexing {len(meta.dex_files)} DEX files...")
    dex_entries = apk_ctx.extract_dex_bytes()
    dex_index = DexIndex()
    dex_index.index_dex_files(dex_entries)
    print(f"⚡ Indexed {len(dex_index.classes)} classes, {len(dex_index.methods)} methods.")

    print("🛡️ Running adversarial validation across all 16 Gboard patch contracts...")
    validator = GboardAdversarialValidator(repo_path, meta, dex_index, apk_ctx.apk_path)
    patch_results, theme_report, invariants_report = validator.audit_all_patches()

    applied_changes = []
    rejected_changes = []

    all_verified = all(res.status in (PatchStatus.VERIFIED, PatchStatus.NOT_AFFECTED) for res in patch_results.values()) and theme_report.passed and invariants_report.passed

    if mode == "update" and all_verified:
        print("✍️ Applying minimal source migrations for Gboard...")
        plan_const = migrator.plan_gboard_constants_update(meta.version_name)
        if plan_const.has_changes:
            migrator.apply_plan(plan_const)
            applied_changes.extend(plan_const.changes)

    elif mode == "update" and not all_verified:
        for p_key, p_res in patch_results.items():
            if p_res.status == PatchStatus.BLOCKED:
                rejected_changes.append(f"Blocked update for '{p_res.patch_name}': {'; '.join(p_res.blocking_reasons)}")
        if not theme_report.passed:
            rejected_changes.extend(theme_report.blocking_reasons)
        if not invariants_report.passed:
            rejected_changes.extend(invariants_report.blocking_reasons)

    build_passed = True
    build_output = "Build skipped in audit mode."
    if mode == "update" and all_verified:
        print("🔨 Executing Gradle build, checks, and metadata generation...")
        build_passed, build_output = validator.run_gradle_build_verification()

    final_status = "SUCCESS" if (all_verified and build_passed) else "BLOCKED"

    report_data = HarnessReportData(
        app_name="Gboard Lite",
        package_name=meta.package_name,
        mode=mode.upper(),
        old_version=old_version,
        new_version=meta.version_name,
        old_version_code=None,
        new_version_code=meta.version_code,
        apk_sha256=meta.sha256,
        apk_file_size=meta.file_size,
        patch_results=patch_results,
        theme_report=theme_report,
        invariants_report=invariants_report,
        applied_changes=applied_changes,
        rejected_changes=rejected_changes,
        build_passed=build_passed,
        build_output=build_output,
        final_status=final_status,
    )

    md_report = HarnessReporter.render_markdown(report_data)
    print("\n" + md_report + "\n")

    target_report_file = output_report or "GBOARD_HARNESS_REPORT.md"
    Path(target_report_file).write_text(md_report, encoding="utf-8")
    print(f"📄 Report written to {target_report_file}")

    elapsed = time.time() - t0
    print(f"⏱️ Harness completed in {elapsed:.2f}s with status: {final_status}")
    return 0 if final_status == "SUCCESS" else 1


def run_pipeline(apk_path: str, mode: str = "audit", output_report: str | None = None) -> int:
    t0 = time.time()
    print(f"🚀 Starting Morphe Patches Update Harness in [{mode.upper()}] mode on '{apk_path}'...")

    with ApkContext(apk_path) as apk_ctx:
        meta = apk_ctx.get_metadata()
        print(f"📦 Identified Package: {meta.package_name} v{meta.version_name} (versionCode: {meta.version_code})")
        print(f"🔑 SHA-256: {meta.sha256}")

        if "latin" in meta.package_name or "inputmethod" in meta.package_name:
            return run_gboard_pipeline(apk_ctx, mode, output_report, t0)
        elif "brave" in meta.package_name:
            return run_brave_pipeline(apk_ctx, mode, output_report, t0)
        else:
            print(f"⚠️ Unrecognized package '{meta.package_name}'. Defaulting to Brave analysis pipeline.")
            return run_brave_pipeline(apk_ctx, mode, output_report, t0)


def main():
    parser = argparse.ArgumentParser(description="Morphe Patches Automated Update Harness")
    parser.add_argument("apk", help="Path to target Android APK")
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--audit", action="store_true", default=True, help="Run non-destructive audit (default)")
    group.add_argument("--update", action="store_true", help="Apply minimal migrations and run build")
    group.add_argument("--dry-run", action="store_true", help="Show proposed changes without writing")
    parser.add_argument("--output", help="Save markdown report to file")

    args = parser.parse_args()

    mode = "update" if args.update else ("dry-run" if args.dry_run else "audit")
    sys.exit(run_pipeline(args.apk, mode=mode, output_report=args.output))


if __name__ == "__main__":
    main()
