"""Check that the documented target versions match the versions pinned in Constants.

docs/compatibility.md, the guides in docs/apps/*.md and the COMPATIBILITY_* tables in
Constants.kt each state the supported version by hand. When one side bumps without the
others, users download a build the patcher refuses. This check fails CI until all three
agree on the same pinned versions.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
APPS = ("brave", "gboard", "tiktok")

CONSTANTS_RE = re.compile(
    r"const val (\w+)_TARGET_VERSION(?:_(\w+))? = \"([^\"]+)\"", re.M
)
ABI_SUFFIX_RE = re.compile(r"-(arm64-v8a|armeabi-v7a)$")
BACKTICK_RE = re.compile(r"`([^`]+)`")


def target_versions(root: Path, app: str) -> list[str]:
    constants = (
        root / "patches/src/main/kotlin/app/morphe/patches/shared/Constants.kt"
    ).read_text(encoding="utf-8")
    return [m.group(3) for m in CONSTANTS_RE.finditer(constants) if m.group(1) == app.upper()]


def base_version(version: str) -> str:
    return ABI_SUFFIX_RE.sub("", version)


def guide_version_cell(root: Path, app: str) -> str:
    doc = (root / "docs/apps" / f"{app}.md").read_text(encoding="utf-8")
    match = re.search(r"^\| \*\*Target Version\*\* \| ([^|]+)\|", doc, re.M)
    return match.group(1).strip() if match else ""


def compatibility_row(root: Path, app: str) -> list[str]:
    doc = (root / "docs/compatibility.md").read_text(encoding="utf-8")
    for row in doc.splitlines():
        if f"apps/{app}.md" not in row:
            continue
        return [cell.strip() for cell in row.split("|") if cell.strip()]
    return []


def check(root: Path = ROOT, apps: tuple[str, ...] = APPS) -> list[str]:
    failures: list[str] = []

    for app in apps:
        versions = target_versions(root, app)
        if not versions:
            failures.append(f"{app}: no {app.upper()}_TARGET_VERSION constants found in Constants.kt")
            continue

        guide_cell = guide_version_cell(root, app)
        if not guide_cell:
            failures.append(f"{app}: no '**Target Version**' row in docs/apps/{app}.md")
        else:
            for version in versions:
                if version not in guide_cell:
                    failures.append(f"{app}: guide Target Version row is missing the pinned version {version}")

        row = compatibility_row(root, app)
        if not row:
            failures.append(f"{app}: no row linking apps/{app}.md in docs/compatibility.md")
            continue
        doc_versions = BACKTICK_RE.findall(row[1] if len(row) > 1 else "")
        if not doc_versions:
            failures.append(f"{app}: compatibility.md has no backticked version in the Supported Version cell")
            continue
        doc_bases = {base_version(v) for v in doc_versions}
        code_bases = {base_version(v) for v in versions}
        if doc_bases != code_bases:
            failures.append(
                f"{app}: compatibility.md supports {sorted(doc_bases)} but Constants pin {sorted(code_bases)}"
            )
    return failures


def main() -> int:
    failures = check()
    if failures:
        print(f"Target version mismatches: {len(failures)}")
        for failure in failures:
            print(f"  {failure}")
        return 1
    print(f"Target versions OK ({', '.join(APPS)}).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
