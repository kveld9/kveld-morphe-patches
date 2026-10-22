#!/usr/bin/env python3
"""Scheduled upstream app update checker for Morphe Patches.

Parses target versions from Constants.kt, queries upstream public sources
(APKMirror and APKPure), and reports whether updates are available.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import tempfile
import unittest
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional, Tuple

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/124.0.0.0 Safari/537.36"
)

HTTP_TIMEOUT_SECONDS = 15

APP_SPECS: List[Dict[str, Any]] = [
    # ==============================================================================
    # ADD NEW APP HERE - TEMPLATE
    # ==============================================================================
    # To monitor a new app, copy-paste-fill this template into APP_SPECS below.
    # A maintainer adding an app touches ONLY this block and adds one fixture test
    # in TestUpdateChecker (see test_apkmirror_extraction_and_beta_filter).
    #
    # {
    #     "id": "my-app",                           # 1. Unique runner/issue identifier (matches PatchExecutionTest id)
    #     "name": "My App",                         # 2. Display name shown in logs, issue titles, and alerts
    #     "package_name": "com.example.app",        # 3. Android application package name
    #     "constant_key": "MY_APP_TARGET_VERSION",  # 4. Target version constant defined in Constants.kt
    #     "source_type": "apkmirror",               # 5. Upstream host type: "apkmirror" or "apkpure"
    #     "source_name": "APKMirror",               # 6. Display name of upstream host, must match the entry's source
    #     "source_url": "https://www.apkmirror.com/apk/slug/app/",  # 7. Canonical listing URL (HTTP 200 verified live)
    #     "path_prefix": "/apk/slug/app/app-",      # URL path prefix for release links (apkmirror). Release-level vs directory-level prefix both work with startswith (Brave uses directory-level).
    #     "allow_beta": False,                      # Pre-release filter (typically False for stable patch releases)
    #     # "require_variant_marker": "...",        # Optional: set for variant-pinned listings (e.g. Gboard); omitted otherwise (consumer reads via .get)
    #     # "variant_match": {                      # Optional: exact variant criteria to resolve direct download link
    #     #     "arch": "arm64-v8a",                # Pinned architecture token (exact or composite)
    #     #     "dpi": "nodpi",                     # Pinned screen DPI (case-insensitive)
    #     #     "type": "APK",                      # Package type ("APK" or "BUNDLE"/"APKM")
    #     # },
    # },
    # ==============================================================================
    {
        "id": "brave",
        "name": "Brave",
        "package_name": "com.brave.browser",
        "constant_key": "BRAVE_TARGET_VERSION",
        "source_type": "apkmirror",
        "source_name": "APKMirror",
        "source_url": "https://www.apkmirror.com/apk/brave-software/brave-browser/",
        "path_prefix": "/apk/brave-software/brave-browser/",
        "allow_beta": False,
        # variant_match decision: Constants.kt pins "Bravemonoarm64.apk (arm64) or
        # BraveMonoarm.apk (arm32)" from GitHub releases without a DPI marker.
        # APKMirror release pages have multiple candidate APKs with dual architectures
        # and no single disambiguated target variant. Leaving variant_match unconfigured
        # avoids guessing links and uses the safe fallback (no Direct download line).
    },
    {
        "id": "gboard",
        "name": "Gboard",
        "package_name": "com.google.android.inputmethod.latin",
        "constant_key": "GBOARD_TARGET_VERSION",
        "source_type": "apkmirror",
        "source_name": "APKMirror",
        "source_url": "https://www.apkmirror.com/apk/google-inc/gboard/",
        "path_prefix": "/apk/google-inc/gboard/gboard-the-google-keyboard-",
        "allow_beta": True,
        "require_variant_marker": "lite_beta",
        "variant_match": {
            "arch": "arm64-v8a",
            "dpi": "nodpi",
            "type": "APK",
        },
    },
    {
        "id": "tiktok",
        "name": "TikTok",
        "package_name": "com.zhiliaoapp.musically",
        "constant_key": "TIKTOK_TARGET_VERSION",
        "source_type": "apkmirror",
        "source_name": "APKMirror",
        "source_url": "https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/",
        "path_prefix": "/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-",
        "allow_beta": False,
        "variant_match": {
            "arch": "arm64-v8a",
            "dpi": "nodpi",
            "type": "APK",
        },
    },
]

# Build dependencies (toolchain components, not apps)
TOOLCHAIN_SPECS: List[Dict[str, Any]] = [
    {
        "id": "morphe-patcher",
        "name": "Morphe Patcher",
        "local_file": "gradle/libs.versions.toml",
        "local_pattern": r'^morphe-patcher\s*=\s*"([^"]+)"',
        "api_url": "https://api.github.com/repos/MorpheApp/morphe-patcher/releases/latest",
        "source_url": "https://github.com/MorpheApp/morphe-patcher/releases",
    },
    {
        "id": "morphe-gradle-plugin",
        "name": "Morphe Gradle Plugin",
        "local_file": "settings.gradle.kts",
        "local_pattern": r'id\("app\.morphe\.patches"\)\s+version\s+"([^"]+)"',
        "api_url": "https://api.github.com/repos/MorpheApp/morphe-patches-gradle-plugin/tags?per_page=100",
        "source_url": "https://github.com/MorpheApp/morphe-patches-gradle-plugin/tags",
    },
    {
        "id": "gradle-wrapper",
        "name": "Gradle Wrapper",
        "local_file": "gradle/wrapper/gradle-wrapper.properties",
        "local_pattern": r'^distributionUrl=.*/gradle-(\d+(?:\.\d+)*)-(?:bin|all)\.zip\s*$',
        "api_url": "https://api.github.com/repos/gradle/gradle/releases/latest",
        "source_url": "https://github.com/gradle/gradle/releases",
    },
]


def resolve_repo_root() -> Path:
    """Resolve repository root directory from this script's location."""
    return Path(__file__).resolve().parent.parent


def resolve_default_constants_path() -> Path:
    """Resolve default path to Constants.kt relative to repository root."""
    return resolve_repo_root() / "patches" / "src" / "main" / "kotlin" / "app" / "morphe" / "patches" / "shared" / "Constants.kt"


def parse_constants(constants_path: Path) -> Dict[str, str]:
    """Parse target version constants from Constants.kt with regex.

    Matches constants such as:
        const val BRAVE_TARGET_VERSION = "1.97.57"
        const val GBOARD_TARGET_VERSION = "18.5.1.993608717-lite_beta-arm64-v8a"
    """
    if not constants_path.is_file():
        print(f"[WARN] Constants file not found: {constants_path}", file=sys.stderr)
        return {}

    content = constants_path.read_text(encoding="utf-8")
    pattern = r'const\s+val\s+([A-Z0-9_]+_TARGET_VERSION(?:_[A-Z0-9_]+)?)\s*=\s*"([^"]+)"'
    return dict(re.findall(pattern, content))


def parse_version(v_str: str) -> Tuple[Tuple[int, ...], str]:
    """Parse a version string into a numeric core tuple and suffix.

    Examples:
        '1.97.57' -> ((1, 97, 57), '')
        '18.5.1.993608717-lite_beta-arm64-v8a' -> ((18, 5, 1, 993608717), 'lite_beta-arm64-v8a')
        '18.5.1.993608717 beta' -> ((18, 5, 1, 993608717), 'beta')
        '1.38.0i' -> ((1, 38, 0), 'i')
        '9.0.278' -> ((9, 0, 278), '')
    """
    v = v_str.strip().lstrip("vV")
    m = re.match(r"^(\d+(?:\.\d+)*)(?:([a-zA-Z]+)|[-_\s]+(.*))?$", v)
    if not m:
        return (), v

    core_str = m.group(1)
    letter_suffix = m.group(2) or ""
    rest_suffix = m.group(3) or ""
    suffix = (letter_suffix + rest_suffix).strip()
    core = tuple(int(x) for x in core_str.split("."))
    return core, suffix


def compare_versions(local_ver: str, remote_ver: str) -> str:
    """Compare local vs remote version.

    Returns:
        'current', 'update-available', or 'unknown'
    """
    if not local_ver or not remote_ver:
        return "unknown"

    local_core, local_sfx = parse_version(local_ver)
    remote_core, remote_sfx = parse_version(remote_ver)

    if not local_core or not remote_core:
        return "unknown"

    max_len = max(len(local_core), len(remote_core))
    padded_local = local_core + (0,) * (max_len - len(local_core))
    padded_remote = remote_core + (0,) * (max_len - len(remote_core))

    if padded_remote > padded_local:
        return "update-available"
    if padded_remote < padded_local:
        return "current"

    # Numeric core parts are equal; check suffixes.
    clean_local_sfx = re.sub(r"-(arm64-v8a|armeabi-v7a|x86_64|x86)", "", local_sfx).strip()
    clean_remote_sfx = re.sub(r"-(arm64-v8a|armeabi-v7a|x86_64|x86)", "", remote_sfx).strip()

    if clean_local_sfx == clean_remote_sfx:
        return "current"

    # If both reference beta channel for the same numeric build:
    if "beta" in clean_local_sfx.lower() and "beta" in clean_remote_sfx.lower():
        return "current"

    if clean_remote_sfx > clean_local_sfx:
        return "update-available"

    return "current"


def fetch_html(url: str) -> Optional[str]:
    """Fetch HTML content from a public URL using standard urllib."""
    headers = {"User-Agent": USER_AGENT}
    if url.startswith("https://api.github.com/"):
        token = os.environ.get("GITHUB_TOKEN")
        if token:
            headers["Authorization"] = f"Bearer {token}"
            headers["Accept"] = "application/vnd.github+json"

    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=HTTP_TIMEOUT_SECONDS) as resp:
            return resp.read().decode("utf-8", errors="ignore")
    except (urllib.error.URLError, urllib.error.HTTPError, OSError) as exc:
        print(f"[WARN] HTTP error fetching {url}: {exc}", file=sys.stderr)
        return None
    except Exception as exc:
        print(f"[WARN] Unexpected error fetching {url}: {exc}", file=sys.stderr)
        return None


def _build_release_url(href: str) -> str:
    """Build absolute URL for a release or download href."""
    if href.startswith(("http://", "https://")):
        return href
    if href.startswith("/"):
        return f"https://www.apkmirror.com{href}"
    return f"https://www.apkmirror.com/{href.lstrip('/')}"


def _release_has_marker(
    release_url: str,
    marker: str,
    fetcher: Callable[[str], Optional[str]],
) -> bool:
    """Check whether a candidate release page contains the variant marker."""
    page_html = fetcher(release_url)
    if not page_html:
        return False
    return marker.lower() in page_html.lower()


def _find_marked_release(
    candidates: List[Tuple[Tuple[int, ...], str, str, str]],
    source_url: str,
    marker: str,
    fetcher: Callable[[str], Optional[str]],
) -> Tuple[Optional[str], Optional[str]]:
    """Walk candidate releases newest-first and return the first matching release."""
    for _, _, raw_v, href in candidates:
        release_url = _build_release_url(href)
        if _release_has_marker(release_url, marker, fetcher):
            return raw_v, release_url
    return None, None


def _is_candidate_beta(href: str, title: str) -> bool:
    """Check whether release candidate link represents a pre-release."""
    return bool(
        re.search(r"\b(beta|alpha|preview|rc)\b", title, re.I)
        or re.search(r"-(beta|alpha|rc)\d*", href, re.I)
    )


def _filter_candidate_link(
    href: str,
    title: str,
    path_prefix: str,
    allow_beta: bool,
) -> bool:
    """Filter release candidate links matching path prefix and beta constraints."""
    if not href.startswith(path_prefix):
        return False
    if not allow_beta and _is_candidate_beta(href, title):
        return False
    return True


def _collect_apkmirror_candidates(
    html: str,
    path_prefix: str,
    allow_beta: bool,
) -> List[Tuple[Tuple[int, ...], str, str, str]]:
    """Collect and parse candidate release version links from APKMirror HTML."""
    candidates: List[Tuple[Tuple[int, ...], str, str, str]] = []
    link_pattern = r'<a[^>]*class="[^"]*fontBlack[^"]*"[^>]*href="([^"]+)"[^>]*>(.*?)</a>'
    for m in re.finditer(link_pattern, html, re.DOTALL):
        href = m.group(1)
        title = m.group(2).strip()
        if not _filter_candidate_link(href, title, path_prefix, allow_beta):
            continue
        v_match = re.search(
            r"(\d+(?:\.\d+)+(?:[a-zA-Z]+)?(?:\s+(?:beta|alpha|rc\d*))?)",
            title,
            re.I,
        )
        if not v_match:
            continue
        raw_v = v_match.group(1).strip()
        core, sfx = parse_version(raw_v)
        if core:
            candidates.append((core, sfx, raw_v, href))
    return candidates


def extract_apkmirror_latest_release(
    html: str,
    path_prefix: str,
    allow_beta: bool = False,
    source_url: Optional[str] = None,
    require_variant_marker: Optional[str] = None,
    fetch_fn: Optional[Callable[[str], Optional[str]]] = None,
) -> Tuple[Optional[str], Optional[str]]:
    """Extract latest version and release URL from APKMirror listing HTML."""
    candidates = _collect_apkmirror_candidates(html, path_prefix, allow_beta)
    if not candidates:
        return None, None

    candidates.sort(key=lambda c: (c[0], c[1]), reverse=True)

    if require_variant_marker:
        if not source_url:
            return None, None
        fetcher = fetch_fn if fetch_fn is not None else fetch_html
        return _find_marked_release(
            candidates, source_url, require_variant_marker, fetcher
        )

    top = candidates[0]
    return top[2], _build_release_url(top[3])


def extract_apkmirror_version(
    html: str,
    path_prefix: str,
    allow_beta: bool = False,
    source_url: Optional[str] = None,
    require_variant_marker: Optional[str] = None,
    fetch_fn: Optional[Callable[[str], Optional[str]]] = None,
) -> Optional[str]:
    """Extract latest remote version from APKMirror listing HTML.

    Host: APKMirror (https://www.apkmirror.com/apk/...)
    Precedence: Release links matching path_prefix sorted descending by numeric
    core and suffix, filtering out beta/alpha/preview/rc when allow_beta=False.
    When require_variant_marker is specified, release candidate pages are inspected
    newest-first for the required marker token.
    Delegates to extract_apkmirror_latest_release.
    Fixture test: test_apkmirror_extraction_and_beta_filter
    """
    version, _ = extract_apkmirror_latest_release(
        html,
        path_prefix,
        allow_beta=allow_beta,
        source_url=source_url,
        require_variant_marker=require_variant_marker,
        fetch_fn=fetch_fn,
    )
    return version


def _normalize_variant_type(raw_type: str) -> str:
    """Normalize APKMirror package badge type to BUNDLE or APK."""
    cleaned = raw_type.strip().upper()
    if cleaned in ("BUNDLE", "APKM"):
        return "BUNDLE"
    return "APK"


def _score_arch_match(row_arch: str, target_arch: str) -> Optional[int]:
    """Score arch match specificity: 2 for exact match, 1 for composite match, None for no match."""
    cleaned_row = row_arch.strip().lower()
    cleaned_target = target_arch.strip().lower()
    if cleaned_row == cleaned_target:
        return 2
    tokens = [tok.strip() for tok in re.split(r"[\+,/]", cleaned_row) if tok.strip()]
    if cleaned_target in tokens:
        return 1
    return None


def _match_type(row_type: str, target_type: Optional[str]) -> bool:
    """Check whether row package type matches configured target type."""
    if not target_type:
        return True
    return _normalize_variant_type(row_type) == _normalize_variant_type(target_type)


def _match_dpi(row_dpi: str, target_dpi: Optional[str]) -> bool:
    """Check whether row screen DPI matches configured target DPI."""
    if not target_dpi:
        return True
    return row_dpi.strip().lower() == target_dpi.strip().lower()


def _match_variant_row(
    row_type: str,
    row_arch: str,
    row_dpi: str,
    variant_match: Dict[str, str],
) -> Optional[int]:
    """Check if row matches criteria and return specificity score, or None."""
    if not _match_type(row_type, variant_match.get("type")):
        return None
    if not _match_dpi(row_dpi, variant_match.get("dpi")):
        return None
    target_arch = variant_match.get("arch")
    if not target_arch:
        return 1
    return _score_arch_match(row_arch, target_arch)


def _extract_download_href(row_html: str) -> Optional[str]:
    """Extract download page href from variant table row HTML."""
    match = re.search(
        r'<a[^>]*class="[^"]*accent_color[^"]*"[^>]*href="([^"]+)"', row_html
    )
    if not match:
        match = re.search(r'<a[^>]*href="([^"]*(?:download|apk)[^"]*)"', row_html)
    if not match:
        return None
    return _build_release_url(match.group(1))


def _extract_row_type(row_html: str) -> str:
    """Extract badge type from variant table row HTML."""
    badge_match = re.search(
        r'<(?:span|div)[^>]*class="[^"]*(?:badge|apkm-badge)[^"]*"[^>]*>([^<]+)',
        row_html,
        re.I,
    )
    if badge_match:
        return badge_match.group(1).strip()
    if re.search(r"\b(BUNDLE|APKM)\b", row_html, re.I):
        return "BUNDLE"
    return "APK"


def _extract_cells_text(row_html: str) -> List[str]:
    """Extract stripped text contents from all cells in a table row."""
    parts = re.split(
        r'<(?:div|td)[^>]*class="[^"]*table-cell[^"]*"[^>]*>|<td[^>]*>', row_html
    )
    if len(parts) <= 1:
        return []
    return [re.sub(r"<[^>]+>", "", p).strip() for p in parts[1:]]


def _find_cell_by_keyword(cells: List[str], keywords: Tuple[str, ...]) -> str:
    """Find first cell containing any of the given keywords."""
    for cell in cells:
        lower = cell.lower()
        if any(k in lower for k in keywords):
            return cell
    return ""


def _extract_arch_and_dpi(cells: List[str]) -> Tuple[str, str]:
    """Extract architecture and screen DPI strings from row cell texts."""
    if len(cells) >= 4:
        return cells[1], cells[3]
    arch = _find_cell_by_keyword(cells, ("arm", "x86", "universal", "noarch"))
    dpi = _find_cell_by_keyword(cells, ("dpi",))
    return arch, dpi


def _find_variants_table_scope(html: str) -> str:
    """Find variants table section HTML or return whole HTML."""
    table_match = re.search(
        r'<[^>]*class="[^"]*variants-table[^"]*"[^>]*>(.*?)(?:<div class="(?:notes|comments|subHeader|appRow)"|<footer|<!--\s*comments|$)',
        html,
        re.DOTALL | re.I,
    )
    if table_match:
        return table_match.group(1)
    return html


def _slice_starts(scope_html: str, starts: List[int]) -> List[str]:
    """Slice HTML into row blocks between start offsets."""
    blocks: List[str] = []
    total = len(starts)
    for idx, start in enumerate(starts):
        next_idx = idx + 1
        end = starts[next_idx] if next_idx < total else len(scope_html)
        blocks.append(scope_html[start:end])
    return blocks


def _extract_variant_row_blocks(html: str) -> List[str]:
    """Extract table-row blocks from variants-table section or page HTML."""
    scope_html = _find_variants_table_scope(html)
    starts = [
        m.start()
        for m in re.finditer(
            r'<(?:div|tr)[^>]*class="[^"]*table-row[^"]*"[^>]*>', scope_html
        )
    ]
    if starts:
        return _slice_starts(scope_html, starts)
    return [
        m.group(0)
        for m in re.finditer(r"<tr[^>]*>.*?</tr>", scope_html, re.DOTALL)
    ]


def _parse_candidate_from_block(
    block: str,
    variant_match: Dict[str, str],
) -> Optional[Tuple[int, str]]:
    """Parse a single variant row block and return (score, href) if matched."""
    href = _extract_download_href(block)
    if not href:
        return None
    row_type = _extract_row_type(block)
    row_arch, row_dpi = _extract_arch_and_dpi(_extract_cells_text(block))
    score = _match_variant_row(row_type, row_arch, row_dpi, variant_match)
    if score is None:
        return None
    return score, href


def _filter_top_candidates(
    candidates: List[Tuple[int, str]],
    max_score: int,
) -> List[str]:
    """Filter candidates matching top score."""
    best: List[str] = []
    for score, href in candidates:
        if score == max_score:
            best.append(href)
    return best


def _select_unique_top_match(candidates: List[Tuple[int, str]]) -> Optional[str]:
    """Select unique top-scoring download link or None on tie/empty."""
    if not candidates:
        return None
    top_score = max(c[0] for c in candidates)
    best = _filter_top_candidates(candidates, top_score)
    if len(best) == 1:
        return best[0]
    return None


def resolve_apkmirror_variant_download_url(
    html: str,
    variant_match: Dict[str, str],
) -> Optional[str]:
    """Parse APKMirror release page variants table and resolve exact download URL."""
    if not html or not variant_match:
        return None

    candidates: List[Tuple[int, str]] = []
    for block in _extract_variant_row_blocks(html):
        candidate = _parse_candidate_from_block(block, variant_match)
        if candidate:
            candidates.append(candidate)

    return _select_unique_top_match(candidates)


def _should_resolve_download_url(
    status: str,
    source_type: str,
    variant_match: Optional[Dict[str, str]],
    release_url: Optional[str],
) -> bool:
    """Predicate determining if variant download link resolution is applicable."""
    if status != "update-available":
        return False
    if source_type != "apkmirror":
        return False
    return bool(variant_match and release_url)


def _resolve_download_url_if_available(
    status: str,
    source_type: str,
    variant_match: Optional[Dict[str, str]],
    release_url: Optional[str],
    fetcher: Callable[[str], Optional[str]],
) -> str:
    """Resolve direct variant download URL when an update is available."""
    if not _should_resolve_download_url(status, source_type, variant_match, release_url):
        return ""
    release_html = fetcher(release_url)  # type: ignore[arg-type]
    if not release_html:
        return ""
    resolved = resolve_apkmirror_variant_download_url(release_html, variant_match)  # type: ignore[arg-type]
    return resolved or ""


def format_issue_body(
    local_ver: str,
    remote_ver: str,
    source_url: str,
    download_url: str = "",
) -> str:
    """Format GitHub issue body for an app update."""
    body = (
        f"Current supported version: {local_ver}\n"
        f"Latest upstream version: {remote_ver}\n"
        f"Source link: {source_url}\n"
    )
    if download_url:
        body += f"Direct download: {download_url}\n"
    return body


def extract_apkpure_version(html: str, package_name: str) -> Optional[str]:
    """Extract latest remote version from APKPure page HTML.

    Priority order:
    1. Version from download-URL pattern for the package (/<package>/download/<version>).
       Ground truth for fetchable artifact; wins over JSON-LD when they disagree.
    2. Version from data-dt-version attributes for the package.
    3. Version from schema.org JSON-LD as last resort.
    4. Version from meta description tag fallback.
    """
    # 1. Version from download-URL pattern for this package
    dl_matches = re.findall(
        rf'/{re.escape(package_name)}/download/([a-zA-Z0-9._-]+)', html
    )
    dl_version = dl_matches[0].strip() if dl_matches else None

    # 2. Version from data-dt-version attributes
    attr_match = re.search(
        rf'data-dt-package_name="{re.escape(package_name)}"[^>]*data-dt-version="([^"]+)"',
        html,
    )
    if not attr_match:
        attr_match = re.search(
            rf'data-dt-version="([^"]+)"[^>]*data-dt-package_name="{re.escape(package_name)}"',
            html,
        )
    dt_version = attr_match.group(1).strip() if attr_match else None

    # 3. Version from schema.org JSON-LD
    json_ld_version: Optional[str] = None
    for m in re.finditer(
        r'<script[^>]*type="application/ld\+json"[^>]*>(.*?)</script>',
        html,
        re.DOTALL,
    ):
        try:
            data = json.loads(m.group(1))
            items = data if isinstance(data, list) else [data]
            for item in items:
                v = item.get("version") or item.get("softwareVersion")
                if v and isinstance(v, str):
                    core, _ = parse_version(v)
                    if core:
                        json_ld_version = v.strip()
                        break
            if json_ld_version:
                break
        except Exception:
            pass

    # Resolution:
    # If download-URL version is present, it takes precedence over JSON-LD.
    # When data-dt-version reports a higher numeric version than dl_version,
    # the higher version from the primary download button is preferred.
    if dl_version:
        core_dl, _ = parse_version(dl_version)
        if dt_version:
            core_dt, _ = parse_version(dt_version)
            if core_dt and core_dl and core_dt > core_dl:
                return dt_version
        return dl_version

    if dt_version:
        core, _ = parse_version(dt_version)
        if core:
            return dt_version

    if json_ld_version:
        core, _ = parse_version(json_ld_version)
        if core:
            return json_ld_version

    # 4. Fallback: meta description tag
    meta_match = re.search(
        r'<meta[^>]*name="description"[^>]*content="[^"]*?\s+v?(\d+(?:\.\d+)+[a-zA-Z]?)\s+APK',
        html,
        re.I,
    )
    if meta_match:
        v = meta_match.group(1).strip()
        core, _ = parse_version(v)
        if core:
            return v

    return None


def run_update_checks(
    constants_path: Path,
    fetch_fn: Optional[Callable[[str], Optional[str]]] = None,
) -> List[Dict[str, Any]]:
    """Run update checks for all configured apps."""
    constants = parse_constants(constants_path)
    results: List[Dict[str, Any]] = []
    fetcher = fetch_fn if fetch_fn is not None else fetch_html

    for spec in APP_SPECS:
        app_id = spec["id"]
        app_name = spec["name"]
        package_name = spec["package_name"]
        constant_key = spec["constant_key"]
        source_type = spec["source_type"]
        source_name = spec["source_name"]
        source_url = spec["source_url"]
        allow_beta = spec["allow_beta"]
        require_variant_marker = spec.get("require_variant_marker")

        local_version = constants.get(constant_key)
        if not local_version:
            print(
                f"[WARN] Constant {constant_key} not found in {constants_path}",
                file=sys.stderr,
            )
            local_version = "unknown"

        remote_version: Optional[str] = None
        release_url: Optional[str] = None
        html = fetcher(source_url)
        if html:
            if source_type == "apkmirror":
                remote_version, release_url = extract_apkmirror_latest_release(
                    html,
                    spec["path_prefix"],
                    allow_beta,
                    source_url=source_url,
                    require_variant_marker=require_variant_marker,
                    fetch_fn=fetcher,
                )
            elif source_type == "apkpure":
                remote_version = extract_apkpure_version(html, package_name)

        if not remote_version:
            status = "unknown"
        else:
            status = compare_versions(local_version, remote_version)

        download_url = _resolve_download_url_if_available(
            status=status,
            source_type=source_type,
            variant_match=spec.get("variant_match"),
            release_url=release_url,
            fetcher=fetcher,
        )

        display_remote = remote_version if remote_version else "unknown"
        print(
            f"[UPDATE-CHECK] {app_name}: local={local_version} remote={display_remote} status={status}"
        )

        results.append(
            {
                "id": app_id,
                "name": app_name,
                "package_name": package_name,
                "local_version": local_version,
                "remote_version": display_remote,
                "status": status,
                "source_name": source_name,
                "source_url": source_url,
                "download_url": download_url,
            }
        )

    return results


def extract_latest_stable_github_tag(payload: str) -> Optional[str]:
    """Extract highest stable tag version from GitHub release or tags JSON payload."""
    if not payload:
        return None

    matches = re.findall(r'"(?:tag_name|name)"\s*:\s*"[vV]?(\d+(?:\.\d+)*)"', payload)
    if not matches:
        return None

    return max(matches, key=lambda v: parse_version(v)[0])


def read_local_toolchain_version(
    repo_root: Path,
    spec: Dict[str, Any],
) -> Optional[str]:
    """Read local version for a toolchain dependency from file using regex."""
    file_path = repo_root / spec["local_file"]
    if not file_path.is_file():
        print(f"[WARN] Toolchain file not found: {file_path}", file=sys.stderr)
        return None

    content = file_path.read_text(encoding="utf-8")
    match = re.search(spec["local_pattern"], content, re.MULTILINE)
    if not match:
        print(f"[WARN] Pattern not found in {file_path}", file=sys.stderr)
        return None

    return match.group(1).strip()


def _check_toolchain_spec(
    repo_root: Path,
    spec: Dict[str, Any],
    fetcher: Callable[[str], Optional[str]],
) -> Dict[str, Any]:
    """Check a single toolchain specification for updates."""
    local_ver = read_local_toolchain_version(repo_root, spec)
    payload = fetcher(spec["api_url"])
    remote_ver = extract_latest_stable_github_tag(payload) if payload else None

    local_display = local_ver or "unknown"
    remote_display = remote_ver or "unknown"
    status = compare_versions(local_display, remote_display)

    print(
        f"[UPDATE-CHECK] {spec['name']}: local={local_display} remote={remote_display} status={status}"
    )
    return {
        "id": spec["id"],
        "name": spec["name"],
        "package_name": "",
        "local_version": local_display,
        "remote_version": remote_display,
        "status": status,
        "source_name": "GitHub",
        "source_url": spec["source_url"],
        "run_patch_test_id": "",
        "download_url": "",
    }


def run_toolchain_checks(
    repo_root: Path,
    fetch_fn: Optional[Callable[[str], Optional[str]]] = None,
) -> List[Dict[str, Any]]:
    """Run update checks for all configured toolchain dependencies."""
    fetcher = fetch_fn if fetch_fn is not None else fetch_html
    return [
        _check_toolchain_spec(repo_root, spec, fetcher)
        for spec in TOOLCHAIN_SPECS
    ]


# Built-in fixture test suite
class TestUpdateChecker(unittest.TestCase):
    """Fixture tests covering version comparison, variant markers, and extraction."""

    def test_version_schemes(self) -> None:
        """Verify non-plain version schemes and channel matching."""
        # Standard semver
        self.assertEqual(compare_versions("1.97.57", "1.97.57"), "current")
        self.assertEqual(compare_versions("1.97.57", "1.97.58"), "update-available")
        self.assertEqual(compare_versions("1.97.58", "1.97.57"), "current")

        # Non-plain schemes from reference (twitter prod suffix, microsoft 4-part, instagram 5-part, lightroom)
        self.assertEqual(compare_versions("12.34.0-prod.01", "12.34.0-prod.01"), "current")
        self.assertEqual(compare_versions("12.34.0-prod.01", "12.35.0-prod.01"), "update-available")
        self.assertEqual(compare_versions("12.34.0-prod.01", "12.34.0-prod.02"), "update-available")
        self.assertEqual(compare_versions("12.34.0-prod.02", "12.34.0-prod.01"), "current")

        # Microsoft 4-part builds
        self.assertEqual(compare_versions("16.0.20527.20034", "16.0.20527.20034"), "current")
        self.assertEqual(compare_versions("16.0.20527.20034", "16.0.20527.20035"), "update-available")
        self.assertEqual(compare_versions("16.0.20527.20035", "16.0.20527.20034"), "current")

        # Instagram 5-part builds
        self.assertEqual(compare_versions("447.0.0.55.81", "447.0.0.55.81"), "current")
        self.assertEqual(compare_versions("447.0.0.55.81", "448.0.0.1.1"), "update-available")

        # Trailing point groups (Lightroom)
        self.assertEqual(compare_versions("11.6.01", "11.6.01"), "current")
        self.assertEqual(compare_versions("11.6.01", "11.6.10"), "update-available")

        # Beta channel matching
        self.assertEqual(
            compare_versions(
                "18.5.1.993608717-lite_beta-arm64-v8a",
                "18.5.1.993608717 beta",
            ),
            "current",
        )
        self.assertEqual(
            compare_versions(
                "18.5.1.993608717-lite_beta-arm64-v8a",
                "18.5.2.100000000 beta",
            ),
            "update-available",
        )

    def test_apkmirror_extraction_and_beta_filter(self) -> None:
        """Verify APKMirror listing extraction filters out betas when allow_beta=False."""
        fixture_html = """
        <div>
          <a class="fontBlack" href="/apk/moovit/moovit-bus-train-live-info/moovit-5-201-6-1811-release/">
            Moovit: Your Transit Tracker 5.201.6.1811 beta
          </a>
          <a class="fontBlack" href="/apk/moovit/moovit-bus-train-live-info/moovit-5-201-1-1809-release/">
            Moovit: Your Transit Tracker 5.201.1.1809
          </a>
          <a class="fontBlack" href="/apk/other-dev/other-app/other-release/">
            Other App 1.0.0
          </a>
        </div>
        """
        ver = extract_apkmirror_version(
            fixture_html,
            path_prefix="/apk/moovit/moovit-bus-train-live-info/moovit-",
            allow_beta=False,
        )
        self.assertEqual(ver, "5.201.1.1809")

    def test_extract_latest_stable_github_tag(self) -> None:
        """Verify extraction of latest stable tag excluding prereleases and asset names."""
        tags_fixture = json.dumps([
            {"name": "v1.3.4-dev.1"},
            {"name": "v1.3.3"},
            {"name": "v1.3.4"},
            {"name": "v1.2.10"},
        ])
        self.assertEqual(extract_latest_stable_github_tag(tags_fixture), "1.3.4")

        release_fixture = json.dumps({
            "tag_name": "v1.15.1",
            "name": "v1.15.1",
            "assets": [
                {"name": "morphe-patcher-1.15.1.jar"},
            ],
        })
        self.assertEqual(extract_latest_stable_github_tag(release_fixture), "1.15.1")

        self.assertIsNone(extract_latest_stable_github_tag(""))

    def test_run_toolchain_checks(self) -> None:
        """Verify toolchain update check statuses against local version files."""
        with tempfile.TemporaryDirectory() as tmpdir:
            temp_root = Path(tmpdir)
            gradle_dir = temp_root / "gradle"
            gradle_dir.mkdir(parents=True, exist_ok=True)

            versions_toml = gradle_dir / "libs.versions.toml"
            versions_toml.write_text(
                '[versions]\nmorphe-patcher = "1.8.0"\n',
                encoding="utf-8",
            )

            settings_gradle = temp_root / "settings.gradle.kts"
            settings_gradle.write_text(
                'plugins {\n    id("app.morphe.patches") version "1.3.4"\n}\n',
                encoding="utf-8",
            )

            wrapper_dir = gradle_dir / "wrapper"
            wrapper_dir.mkdir(parents=True, exist_ok=True)
            wrapper_props = wrapper_dir / "gradle-wrapper.properties"
            wrapper_props.write_text(
                "distributionUrl=https\\://services.gradle.org/distributions/gradle-9.6.1-bin.zip\n",
                encoding="utf-8",
            )

            def fake_fetch_success(url: str) -> Optional[str]:
                if "morphe-patcher" in url:
                    return json.dumps({
                        "tag_name": "v1.15.1",
                        "name": "v1.15.1",
                        "assets": [{"name": "morphe-patcher-1.15.1.jar"}],
                    })
                if "morphe-patches-gradle-plugin" in url:
                    return json.dumps([
                        {"name": "v1.3.4"},
                        {"name": "v1.3.3"},
                    ])
                if "gradle/gradle" in url:
                    return json.dumps({
                        "tag_name": "v9.8.2",
                        "name": "9.8.2",
                    })
                return None

            results = run_toolchain_checks(temp_root, fetch_fn=fake_fetch_success)
            self.assertEqual(len(results), 3)

            patcher_entry = next(r for r in results if r["id"] == "morphe-patcher")
            self.assertEqual(patcher_entry["status"], "update-available")
            self.assertEqual(patcher_entry["local_version"], "1.8.0")
            self.assertEqual(patcher_entry["remote_version"], "1.15.1")
            self.assertEqual(patcher_entry["package_name"], "")
            self.assertEqual(patcher_entry["source_name"], "GitHub")
            self.assertEqual(patcher_entry["run_patch_test_id"], "")

            plugin_entry = next(r for r in results if r["id"] == "morphe-gradle-plugin")
            self.assertEqual(plugin_entry["status"], "current")
            self.assertEqual(plugin_entry["local_version"], "1.3.4")
            self.assertEqual(plugin_entry["remote_version"], "1.3.4")

            gradle_entry = next(r for r in results if r["id"] == "gradle-wrapper")
            self.assertEqual(gradle_entry["status"], "update-available")
            self.assertEqual(gradle_entry["local_version"], "9.6.1")
            self.assertEqual(gradle_entry["remote_version"], "9.8.2")

            # Fetch returning None -> "unknown"
            unknown_results = run_toolchain_checks(temp_root, fetch_fn=lambda _: None)
            for entry in unknown_results:
                self.assertEqual(entry["status"], "unknown")
                self.assertEqual(entry["remote_version"], "unknown")

    def test_extract_apkmirror_latest_release(self) -> None:
        """Verify release page URL is extracted alongside version and delegated."""
        fixture_html = """
        <div>
          <a class="fontBlack" href="/apk/moovit/moovit-bus-train-live-info/moovit-5-201-6-1811-release/">
            Moovit: Your Transit Tracker 5.201.6.1811 beta
          </a>
          <a class="fontBlack" href="/apk/moovit/moovit-bus-train-live-info/moovit-5-201-1-1809-release/">
            Moovit: Your Transit Tracker 5.201.1.1809
          </a>
        </div>
        """
        ver, url = extract_apkmirror_latest_release(
            fixture_html,
            path_prefix="/apk/moovit/moovit-bus-train-live-info/moovit-",
            allow_beta=False,
        )
        self.assertEqual(ver, "5.201.1.1809")
        self.assertEqual(
            url,
            "https://www.apkmirror.com/apk/moovit/moovit-bus-train-live-info/moovit-5-201-1-1809-release/",
        )

        # extract_apkmirror_version delegates to extract_apkmirror_latest_release
        delegated_ver = extract_apkmirror_version(
            fixture_html,
            path_prefix="/apk/moovit/moovit-bus-train-live-info/moovit-",
            allow_beta=False,
        )
        self.assertEqual(delegated_ver, "5.201.1.1809")

    def test_resolve_apkmirror_variant_download_url(self) -> None:
        """Verify variant parsing: scoring specificity, composite matching, ties, and fallbacks."""
        # 1. Multi-variant table (TikTok shape): exact arch beats composite arch
        variants_html = """
        <div class="variants-table">
          <div class="table-head">
            <div class="table-cell">Variant</div>
            <div class="table-cell">Architecture</div>
            <div class="table-cell">Minimum Version</div>
            <div class="table-cell">Screen DPI</div>
          </div>
          <div class="table-row">
            <div class="table-cell">
              <span class="apkm-badge">APK</span>
              <a class="accent_color" href="/apk/tiktok-pte-ltd/tik-tok/tiktok-47-4-3-composite-apk-download/">47.4.3</a>
            </div>
            <div class="table-cell">arm64-v8a + armeabi-v7a</div>
            <div class="table-cell">Android 5.0+</div>
            <div class="table-cell">nodpi</div>
          </div>
          <div class="table-row">
            <div class="table-cell">
              <span class="apkm-badge">APK</span>
              <a class="accent_color" href="/apk/tiktok-pte-ltd/tik-tok/tiktok-47-4-3-exact-apk-download/">47.4.3</a>
            </div>
            <div class="table-cell">arm64-v8a</div>
            <div class="table-cell">Android 5.0+</div>
            <div class="table-cell">nodpi</div>
          </div>
          <div class="table-row">
            <div class="table-cell">
              <span class="apkm-badge">BUNDLE</span>
              <a class="accent_color" href="/apk/tiktok-pte-ltd/tik-tok/tiktok-47-4-3-bundle-apk-download/">47.4.3</a>
            </div>
            <div class="table-cell">arm64-v8a</div>
            <div class="table-cell">Android 5.0+</div>
            <div class="table-cell">nodpi</div>
          </div>
          <div class="table-row">
            <div class="table-cell">
              <span class="apkm-badge">APK</span>
              <a class="accent_color" href="/apk/tiktok-pte-ltd/tik-tok/tiktok-47-4-3-v7a-apk-download/">47.4.3</a>
            </div>
            <div class="table-cell">armeabi-v7a</div>
            <div class="table-cell">Android 5.0+</div>
            <div class="table-cell">nodpi</div>
          </div>
        </div>
        """
        exact_url = resolve_apkmirror_variant_download_url(
            variants_html,
            {"arch": "arm64-v8a", "dpi": "nodpi", "type": "APK"},
        )
        self.assertEqual(
            exact_url,
            "https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/tiktok-47-4-3-exact-apk-download/",
        )

        # 2. Composite row wins when no exact arch exists
        composite_only_html = """
        <div class="variants-table">
          <div class="table-row">
            <div class="table-cell">
              <span class="apkm-badge">APK</span>
              <a class="accent_color" href="/apk/app/app-composite-apk-download/">1.0</a>
            </div>
            <div class="table-cell">arm64-v8a + armeabi-v7a</div>
            <div class="table-cell">Android 5.0+</div>
            <div class="table-cell">nodpi</div>
          </div>
        </div>
        """
        composite_url = resolve_apkmirror_variant_download_url(
            composite_only_html,
            {"arch": "arm64-v8a", "dpi": "nodpi", "type": "APK"},
        )
        self.assertEqual(
            composite_url,
            "https://www.apkmirror.com/apk/app/app-composite-apk-download/",
        )

        # 3. Bundle type matching (APKM / BUNDLE normalization)
        bundle_url = resolve_apkmirror_variant_download_url(
            variants_html,
            {"arch": "arm64-v8a", "dpi": "nodpi", "type": "BUNDLE"},
        )
        self.assertEqual(
            bundle_url,
            "https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/tiktok-47-4-3-bundle-apk-download/",
        )

        bundle_apkm_url = resolve_apkmirror_variant_download_url(
            variants_html,
            {"arch": "arm64-v8a", "dpi": "nodpi", "type": "APKM"},
        )
        self.assertEqual(
            bundle_apkm_url,
            "https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/tiktok-47-4-3-bundle-apk-download/",
        )

        # 4. No-match fallback -> None
        no_match_url = resolve_apkmirror_variant_download_url(
            variants_html,
            {"arch": "x86_64", "dpi": "nodpi", "type": "APK"},
        )
        self.assertIsNone(no_match_url)

        # 5. Top-score tie fallback -> None (never emit a guessed link)
        tie_html = """
        <div class="variants-table">
          <div class="table-row">
            <div class="table-cell">
              <span class="apkm-badge">APK</span>
              <a class="accent_color" href="/apk/app/app-variant-1-apk-download/">1.0</a>
            </div>
            <div class="table-cell">arm64-v8a</div>
            <div class="table-cell">Android 5.0+</div>
            <div class="table-cell">nodpi</div>
          </div>
          <div class="table-row">
            <div class="table-cell">
              <span class="apkm-badge">APK</span>
              <a class="accent_color" href="/apk/app/app-variant-2-apk-download/">1.0</a>
            </div>
            <div class="table-cell">arm64-v8a</div>
            <div class="table-cell">Android 5.0+</div>
            <div class="table-cell">nodpi</div>
          </div>
        </div>
        """
        tie_url = resolve_apkmirror_variant_download_url(
            tie_html,
            {"arch": "arm64-v8a", "dpi": "nodpi", "type": "APK"},
        )
        self.assertIsNone(tie_url)

    def test_run_update_checks_variant_plumbing(self) -> None:
        """Verify end-to-end update check plumbing with variant download link resolution."""
        with tempfile.TemporaryDirectory() as tmpdir:
            temp_root = Path(tmpdir)
            constants_file = temp_root / "Constants.kt"
            constants_file.write_text(
                'const val BRAVE_TARGET_VERSION = "1.97.50"\n'
                'const val GBOARD_TARGET_VERSION = "18.5.0.000000000-lite_beta-arm64-v8a"\n'
                'const val TIKTOK_TARGET_VERSION = "47.4.0"\n',
                encoding="utf-8",
            )

            gboard_release_url = "https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-1-993608717-release/"
            tiktok_release_url = "https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-47-4-3-release/"
            brave_release_url = "https://www.apkmirror.com/apk/brave-software/brave-browser/brave-browser-1-97-59-release/"

            gboard_listing = f"""
            <div>
              <a class="fontBlack" href="/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-1-993608717-release/">
                Gboard 18.5.1.993608717 beta
              </a>
            </div>
            """
            tiktok_listing = f"""
            <div>
              <a class="fontBlack" href="/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-47-4-3-release/">
                TikTok 47.4.3
              </a>
            </div>
            """
            brave_listing = f"""
            <div>
              <a class="fontBlack" href="/apk/brave-software/brave-browser/brave-browser-1-97-59-release/">
                Brave Browser 1.97.59
              </a>
            </div>
            """

            gboard_release_page = """
            <div class="variants-table">
              <!-- Marker token for require_variant_marker -->
              <p>Contains lite_beta builds</p>
              <div class="table-row">
                <div class="table-cell">
                  <span class="apkm-badge">APK</span>
                  <a class="accent_color" href="/apk/google-inc/gboard/gboard-download-arm64/">18.5.1</a>
                </div>
                <div class="table-cell">arm64-v8a</div>
                <div class="table-cell">Android 8.0+</div>
                <div class="table-cell">nodpi</div>
              </div>
            </div>
            """

            tiktok_release_page = """
            <div class="variants-table">
              <div class="table-row">
                <div class="table-cell">
                  <span class="apkm-badge">APK</span>
                  <a class="accent_color" href="/apk/tiktok-pte-ltd/tik-tok/tiktok-download-arm64/">47.4.3</a>
                </div>
                <div class="table-cell">arm64-v8a</div>
                <div class="table-cell">Android 5.0+</div>
                <div class="table-cell">nodpi</div>
              </div>
            </div>
            """

            def fake_fetcher(url: str) -> Optional[str]:
                if "brave-browser/" in url and not url.endswith("-release/"):
                    return brave_listing
                if "brave-browser-1-97-59-release" in url:
                    return "<div>Brave release page</div>"
                if "google-inc/gboard/" in url and not url.endswith("-release/"):
                    return gboard_listing
                if url == gboard_release_url:
                    return gboard_release_page
                if "tik-tok-including-musical-ly/" in url and not url.endswith("-release/"):
                    return tiktok_listing
                if url == tiktok_release_url:
                    return tiktok_release_page
                return None

            results = run_update_checks(constants_file, fetch_fn=fake_fetcher)

            tiktok_res = next(r for r in results if r["id"] == "tiktok")
            self.assertEqual(tiktok_res["status"], "update-available")
            self.assertEqual(
                tiktok_res["download_url"],
                "https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/tiktok-download-arm64/",
            )

            gboard_res = next(r for r in results if r["id"] == "gboard")
            self.assertEqual(gboard_res["status"], "update-available")
            self.assertEqual(
                gboard_res["download_url"],
                "https://www.apkmirror.com/apk/google-inc/gboard/gboard-download-arm64/",
            )

            brave_res = next(r for r in results if r["id"] == "brave")
            self.assertEqual(brave_res["status"], "update-available")
            self.assertEqual(brave_res["download_url"], "")

    def test_format_issue_body_direct_download_line(self) -> None:
        """Verify Direct download line presence in issue body template."""
        body_with_link = format_issue_body(
            local_ver="47.4.0",
            remote_ver="47.4.3",
            source_url="https://www.apkmirror.com/apk/tiktok/",
            download_url="https://www.apkmirror.com/apk/tiktok/download/",
        )
        self.assertIn("Direct download: https://www.apkmirror.com/apk/tiktok/download/\n", body_with_link)
        self.assertTrue(body_with_link.endswith("Direct download: https://www.apkmirror.com/apk/tiktok/download/\n"))

        body_without_link = format_issue_body(
            local_ver="1.97.50",
            remote_ver="1.97.59",
            source_url="https://www.apkmirror.com/apk/brave/",
            download_url="",
        )
        self.assertNotIn("Direct download:", body_without_link)
        expected_body = (
            "Current supported version: 1.97.50\n"
            "Latest upstream version: 1.97.59\n"
            "Source link: https://www.apkmirror.com/apk/brave/\n"
        )
        self.assertEqual(body_without_link, expected_body)


def main() -> int:
    """CLI entrypoint."""
    parser = argparse.ArgumentParser(
        description="Check upstream app versions against Morphe target constants."
    )
    parser.add_argument(
        "--check",
        action="store_true",
        default=True,
        help="Report update check status (default: True)",
    )
    parser.add_argument(
        "--json",
        dest="json_path",
        type=str,
        default=None,
        help="Path to write machine-readable JSON report",
    )
    parser.add_argument(
        "--constants",
        dest="constants_path",
        type=str,
        default=None,
        help="Path to Constants.kt (default: auto-detected)",
    )
    parser.add_argument(
        "--test",
        action="store_true",
        default=False,
        help="Run built-in unit and fixture tests",
    )

    args = parser.parse_args()

    if args.test:
        suite = unittest.TestLoader().loadTestsFromTestCase(TestUpdateChecker)
        runner = unittest.TextTestRunner(verbosity=2)
        result = runner.run(suite)
        return 0 if result.wasSuccessful() else 1

    repo_root = resolve_repo_root()
    constants_path = (
        Path(args.constants_path)
        if args.constants_path
        else resolve_default_constants_path()
    )

    results = run_update_checks(constants_path) + run_toolchain_checks(repo_root)

    if args.json_path:
        out_path = Path(args.json_path)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        report_data = {"apps": results}
        out_path.write_text(json.dumps(report_data, indent=2), encoding="utf-8")
        print(f"[INFO] Saved JSON report to {args.json_path}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
