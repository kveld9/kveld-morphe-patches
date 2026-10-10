#!/usr/bin/env bash
# sync_shared_skills.sh - keep canonical shared skills byte-identical across sibling repos.
#
# Canonical skills: new-telemetry-app, telemetry-blocking.
# Edit in either repo, then run this script from either repo to propagate.
# Newest copy (mtime) wins per file. No absolute paths or machine names:
# the sibling repo resolves via --with, SIBLING_REPO, or the default
# sibling directory layout.
#
# Usage:
#   scripts/sync_shared_skills.sh [--check] [--with <sibling-repo-path>]
#
# --check reports drift and exits 1 without modifying anything (CI-safe).
set -euo pipefail

CHECK=0
SIBLING="${SIBLING_REPO:-}"
while [ $# -gt 0 ]; do
    case "$1" in
        --check) CHECK=1; shift ;;
        --with) SIBLING="${2:?--with requires a path}"; shift 2 ;;
        *) echo "[SYNC] Unknown argument: $1" >&2; exit 2 ;;
    esac
done

OWN_ROOT="$(git rev-parse --show-toplevel)"
OWN_NAME="$(basename "$OWN_ROOT")"
case "$OWN_NAME" in
    kveld-extra-morphe-patches) DEFAULT_SIBLING="brave-origin-patches" ;;
    brave-origin-patches) DEFAULT_SIBLING="kveld-extra-morphe-patches" ;;
    *) echo "[SYNC] Unknown repo layout: $OWN_NAME" >&2; exit 2 ;;
esac
if [ -z "$SIBLING" ]; then
    SIBLING="$(dirname "$OWN_ROOT")/$DEFAULT_SIBLING"
fi
[ -d "$SIBLING/.git" ] || { echo "[SYNC] Sibling repo not found: $SIBLING (use --with or SIBLING_REPO)" >&2; exit 2; }

SHARED_SKILLS="new-telemetry-app telemetry-blocking"
drift=0

link_claude() {
    # $1 = repo root, $2 = skill dir name. Keep .claude hardlinked to .agents.
    local root="$1" skill="$2"
    local agents="$root/.agents/skills/$skill/SKILL.md"
    local claude="$root/.claude/skills/$skill/SKILL.md"
    [ -f "$agents" ] || return 0
    if [ ! -f "$claude" ]; then
        mkdir -p "$(dirname "$claude")"
        if ln "$agents" "$claude" 2>/dev/null; then
            echo "[SYNC] Linked $skill .claude copy in $(basename "$root")."
        else
            cp -p "$agents" "$claude"
            echo "[SYNC] Copied $skill .claude copy in $(basename "$root") (link unavailable)."
        fi
        return 0
    fi
    if [ "$agents" -ef "$claude" ]; then
        return 0
    fi
    if ln -f "$agents" "$claude" 2>/dev/null; then
        echo "[SYNC] Re-linked $skill .claude copy in $(basename "$root")."
    else
        cp -p "$agents" "$claude"
        echo "[SYNC] Re-copied $skill .claude copy in $(basename "$root") (link unavailable)."
    fi
}

for skill in $SHARED_SKILLS; do
    own="$OWN_ROOT/.agents/skills/$skill/SKILL.md"
    sib="$SIBLING/.agents/skills/$skill/SKILL.md"
    if [ ! -f "$own" ] && [ ! -f "$sib" ]; then
        echo "[SYNC] $skill missing in both repos; skipping."
        continue
    fi
    if [ ! -f "$own" ]; then
        echo "[SYNC] $skill only in sibling; copying to own repo."
        [ "$CHECK" -eq 1 ] && { drift=1; continue; }
        mkdir -p "$(dirname "$own")"
        cp -p "$sib" "$own"
        link_claude "$OWN_ROOT" "$skill"
        continue
    fi
    if [ ! -f "$sib" ]; then
        echo "[SYNC] $skill only in own repo; copying to sibling."
        [ "$CHECK" -eq 1 ] && { drift=1; continue; }
        mkdir -p "$(dirname "$sib")"
        cp -p "$own" "$sib"
        link_claude "$SIBLING" "$skill"
        continue
    fi
    if cmp -s "$own" "$sib"; then
        echo "[SYNC] $skill in sync."
        continue
    fi
    echo "[SYNC] $skill drift detected."
    if [ "$CHECK" -eq 1 ]; then
        drift=1
        continue
    fi
    if [ "$own" -nt "$sib" ]; then
        cp -p "$own" "$sib"
        echo "[SYNC] $skill propagated own -> sibling."
        link_claude "$SIBLING" "$skill"
    else
        cp -p "$sib" "$own"
        echo "[SYNC] $skill propagated sibling -> own."
        link_claude "$OWN_ROOT" "$skill"
    fi
done

if [ "$CHECK" -eq 1 ]; then
    if [ "$drift" -ne 0 ]; then
        echo "[SYNC] FAIL: shared skills drifted." >&2
        exit 1
    fi
    echo "[SYNC] PASS: shared skills identical."
fi
