"""Check that relative markdown links in the docs point at headings that exist.

GitHub renders headings with its own slugger: lowercase, punctuation removed, spaces
turned into hyphens (so "A & B" becomes "a--b", and duplicate headings get -1/-2).
A dead anchor still renders as ordinary link text, so nothing catches it unless we do.
"""

from __future__ import annotations

import re
import sys
import unicodedata
import urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LINK_RE = re.compile(r"\[[^\]]*\]\(([^)\s]+)\)")
HEADING_RE = re.compile(r"^(#{1,6})\s+(.*?)\s*$")
EXPLICIT_ANCHOR_RE = re.compile(r'<a\s+name="([^"]+)"')
FENCE_RE = re.compile(r"^\s*```")


def github_slug(text: str) -> str:
    """GitHub's heading slug, punctuation dropped and spaces kept as hyphens."""
    out: list[str] = []
    for ch in text.strip().lower():
        if ch in (" ", "-", "_") or ch.isalnum():
            out.append(ch)
        elif unicodedata.category(ch)[0] in ("L", "N"):
            out.append(ch)
    return "".join(out).replace(" ", "-")


def heading_anchors(path: Path) -> set[str]:
    anchors: set[str] = set()
    seen: dict[str, int] = {}
    in_fence = False
    for line in path.read_text(encoding="utf-8").splitlines():
        if FENCE_RE.match(line):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        heading = HEADING_RE.match(line)
        if heading:
            base = github_slug(heading.group(2))
            count = seen.get(base, 0)
            seen[base] = count + 1
            anchors.add(base if count == 0 else f"{base}-{count}")
        anchors.update(EXPLICIT_ANCHOR_RE.findall(line))
    return anchors


def markdown_files() -> list[Path]:
    files = sorted(ROOT.glob("docs/**/*.md"))
    readme = ROOT / "README.md"
    if readme.exists():
        files.insert(0, readme)
    return files


def main() -> int:
    failures: list[str] = []
    cache: dict[Path, set[str]] = {}

    for source in markdown_files():
        for target in LINK_RE.findall(source.read_text(encoding="utf-8")):
            if target.startswith(("http://", "https://", "mailto:")):
                continue
            path_part, _, fragment = target.partition("#")
            destination = (source.parent / urllib.parse.unquote(path_part)).resolve() if path_part else source.resolve()
            shown = source.relative_to(ROOT)

            if not destination.exists():
                failures.append(f"{shown}: {target} -> file does not exist")
                continue
            if not fragment or destination.suffix != ".md":
                continue

            if destination not in cache:
                cache[destination] = heading_anchors(destination)
            if urllib.parse.unquote(fragment) not in cache[destination]:
                failures.append(f"{shown}: {target} -> heading does not exist")

    if failures:
        print(f"Broken documentation links: {len(failures)}")
        for failure in failures:
            print(f"  {failure}")
        return 1

    print(f"Documentation links OK ({len(markdown_files())} files checked).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
