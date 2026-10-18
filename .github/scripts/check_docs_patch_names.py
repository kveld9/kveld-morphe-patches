"""Check that each app guide lists the patch names the code actually declares.

The catalog tables in docs/apps/*.md are hand-written, so a display-name rename in
Kotlin silently leaves the guide pointing at names that no longer exist. That drift
is what forced the Brave and TikTok rename passes, and it also rotates every anchor
that points at those rows. This check fails CI until code and guide agree.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
APPS = ("brave", "gboard", "tiktok")

PATCH_NAME_RE = re.compile(
    r"(?:bytecodePatch|resourcePatch|rawResourcePatch)\(\s*name\s*=\s*\"([^\"]+)\"",
    re.S,
)
TABLE_ROW_RE = re.compile(r"^\|\s*\*\*([^*|]+)\*\*\s*\|")
HEADING_RE = re.compile(r"^##\s+")


def code_names(root: Path, app: str) -> list[str]:
    source = root / "patches/src/main/kotlin/app/morphe/patches" / app
    names: list[str] = []
    for path in sorted(source.rglob("*.kt")):
        names.extend(PATCH_NAME_RE.findall(path.read_text(encoding="utf-8")))
    return names


def catalog_names(root: Path, app: str) -> list[str]:
    doc = root / "docs/apps" / f"{app}.md"
    if not doc.exists():
        return []
    names: list[str] = []
    in_catalog = False
    for line in doc.read_text(encoding="utf-8").splitlines():
        if not in_catalog:
            if line.startswith("| Patch Name |"):
                in_catalog = True
            continue
        if HEADING_RE.match(line):
            break
        match = TABLE_ROW_RE.match(line)
        if match:
            names.append(match.group(1).strip())
    return names


def check(root: Path = ROOT, apps: tuple[str, ...] = APPS) -> list[str]:
    failures: list[str] = []
    for app in apps:
        code = code_names(root, app)
        docs = catalog_names(root, app)
        if not code:
            failures.append(f"{app}: no patch declarations found under patches/")
            continue
        if not docs:
            failures.append(f"{app}: no '| Patch Name |' catalog found in docs/apps/{app}.md")
            continue

        duplicates = sorted({name for name in code if code.count(name) > 1})
        for name in duplicates:
            failures.append(f"{app}: patch name declared more than once: {name!r}")

        for name in sorted(set(code) - set(docs)):
            failures.append(f"{app}: patch not listed in the guide: {name!r}")
        for name in sorted(set(docs) - set(code)):
            failures.append(f"{app}: guide lists a patch that does not exist: {name!r}")
    return failures


def main() -> int:
    failures = check()
    if failures:
        print(f"Patch catalog mismatches: {len(failures)}")
        for failure in failures:
            print(f"  {failure}")
        return 1
    checked = ", ".join(APPS)
    print(f"Patch catalogs OK ({checked}).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
