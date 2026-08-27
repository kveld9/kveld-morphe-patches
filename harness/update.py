#!/usr/bin/env python3
"""
Morphe Patches Automated Update & Reverse Engineering Harness.
Modular pipeline orchestrator for Brave Browser, Gboard Lite, and Vivaldi Browser Snapshot.

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
from harness.core.pipeline import PipelineRegistry
import harness.pipelines  # Registers all target pipelines


def run_pipeline(apk_path: str, mode: str = "audit", output_report: str | None = None) -> int:
    print(f"🚀 Starting Morphe Patches Update Harness in [{mode.upper()}] mode on '{apk_path}'...")
    with ApkContext(apk_path) as apk_ctx:
        meta = apk_ctx.get_metadata()
        print(f"📦 Identified Package: {meta.package_name} v{meta.version_name} (versionCode: {meta.version_code})")
        print(f"🔑 SHA-256: {meta.sha256}")

        return PipelineRegistry.dispatch(
            apk_ctx=apk_ctx,
            mode=mode,
            output_report=output_report,
            repo_root=REPO_ROOT,
        )


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
