#!/usr/bin/env python3
"""
Syncs the Credits & Contributors section in README.md from .github/contributors.json
and audits or synchronizes closed feature requests from GitHub issues.

Usage:
  # Regenerate README.md table from .github/contributors.json:
  python3 .github/scripts/sync_contributors.py

  # Audit for implemented feature requests not yet credited:
  python3 .github/scripts/sync_contributors.py --audit

  # Automatically sync any uncredited completed feature requests:
  python3 .github/scripts/sync_contributors.py --sync

  # Add a specific issue to contributors:
  python3 .github/scripts/sync_contributors.py --add-issue 35 [--title "Custom title"]
"""

import argparse
import json
import os
import re
import subprocess
import sys
import urllib.request
from pathlib import Path

START_MARKER = "<!-- CONTRIBUTORS_START -->"
END_MARKER = "<!-- CONTRIBUTORS_END -->"


def get_default_repo() -> str:
    env_repo = os.environ.get("GITHUB_REPOSITORY")
    if env_repo and "/" in env_repo:
        return env_repo

    try:
        url = subprocess.check_output(
            ["git", "config", "--get", "remote.origin.url"],
            stderr=subprocess.DEVNULL,
            text=True,
        ).strip()
        match = re.search(r"[:/]([a-zA-Z0-9_\-]+/[a-zA-Z0-9_\-]+?)(?:\.git)?$", url)
        if match:
            return match.group(1)
    except Exception:
        pass

    return "kveld9/kveld-morphe-patches"


def fetch_github_json(endpoint: str, repo: str) -> object:
    url = f"https://api.github.com/repos/{repo}/{endpoint}"
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "Morphe-Contributors-Sync",
    }
    token = os.environ.get("GITHUB_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"

    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))


def load_contributors_data(path: Path) -> dict:
    if not path.exists():
        return {"contributors": []}
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_contributors_data(path: Path, data: dict) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def format_avatar(username: str) -> str:
    return (
        f'<a href="https://github.com/{username}">'
        f'<img src="https://github.com/{username}.png" width="48" height="48" style="border-radius: 50%;" />'
        f"<br><b>@{username}</b></a>"
    )


def format_contribution_items(items: list, repo: str) -> str:
    if not items:
        return ""

    if len(items) == 1:
        it = items[0]
        return f'💡 Proposed {it["title"]} feature request ([#{it["issue"]}](https://github.com/{repo}/issues/{it["issue"]})).'

    links = [
        f'{it["title"]} ([#{it["issue"]}](https://github.com/{repo}/issues/{it["issue"]}))'
        for it in items
    ]

    if len(links) == 2:
        return f"💡 Proposed {links[0]} and {links[1]} feature requests."

    head = ", ".join(links[:-1])
    return f"💡 Proposed {head}, and {links[-1]} feature requests."


def format_contributor_row(entry: dict, repo: str) -> str:
    avatar = format_avatar(entry["username"])
    if "customText" in entry and entry["customText"]:
        return f"| {avatar} | {entry['customText']} |"

    items = entry.get("contributions", [])
    text = format_contribution_items(items, repo)
    return f"| {avatar} | {text} |"


def generate_markdown_table(data: dict, repo: str) -> str:
    rows = [
        "| Contributor | Role & Contributions |",
        "| :--- | :--- |",
    ]
    for c in data.get("contributors", []):
        rows.append(format_contributor_row(c, repo))
    return "\n".join(rows)


def inject_table_into_readme(readme_path: Path, table_content: str) -> bool:
    if not readme_path.exists():
        print(f"[FAIL] README file not found: {readme_path}")
        return False

    content = readme_path.read_text(encoding="utf-8")

    if START_MARKER in content and END_MARKER in content:
        pattern = re.compile(
            re.escape(START_MARKER) + r".*?" + re.escape(END_MARKER),
            re.DOTALL,
        )
        replacement = f"{START_MARKER}\n{table_content}\n{END_MARKER}"
        new_content = pattern.sub(replacement, content)
    else:
        # Fallback: locate ## Credits & Contributors and wrap existing table
        credits_pattern = re.compile(
            r"(##\s*[^\n]*Credits & Contributors[^\n]*\n\n)(\| Contributor \| Role & Contributions \|.*?\n\n)",
            re.DOTALL,
        )
        if not credits_pattern.search(content):
            print("[FAIL] Could not find Credits & Contributors section in README.md")
            return False
        replacement = r"\g<1>" + f"{START_MARKER}\n{table_content}\n{END_MARKER}\n\n"
        new_content = credits_pattern.sub(replacement, content)

    if content != new_content:
        readme_path.write_text(new_content, encoding="utf-8")
        print(f"[SYNC] Updated Credits & Contributors in {readme_path.name}")
        return True

    print(f"[OK] {readme_path.name} is already up to date.")
    return True


def sanitize_issue_title(title: str) -> str:
    clean = re.sub(r"^(feat|feature)(\([^)]+\))?:\s*", "", title, flags=re.IGNORECASE).strip()
    clean = clean.rstrip(".")
    if clean and clean[0].islower():
        clean = clean[0].upper() + clean[1:]
    return clean


def collect_credited_and_ignored_issues(data: dict) -> set:
    known = set()
    for c in data.get("contributors", []):
        for item in c.get("contributions", []):
            if "issue" in item:
                known.add(item["issue"])

    for item in data.get("ignoredIssues", []):
        if isinstance(item, int):
            known.add(item)
        elif isinstance(item, dict) and "issue" in item:
            known.add(item["issue"])

    return known


def add_or_update_contributor(data: dict, username: str, title: str, issue_num: int) -> None:
    contributors = data.setdefault("contributors", [])
    target = next((c for c in contributors if c.get("username") == username), None)

    if target is None:
        target = {
            "username": username,
            "contributions": [],
        }
        contributors.append(target)

    contributions = target.setdefault("contributions", [])
    if any(item.get("issue") == issue_num for item in contributions):
        return

    contributions.append({"title": title, "issue": issue_num})
    contributions.sort(key=lambda x: x.get("issue", 0))


def fetch_completed_feature_requests(repo: str) -> list:
    endpoint = "issues?state=closed&per_page=100"
    issues = fetch_github_json(endpoint, repo)
    candidates = []

    for i in issues:
        if "pull_request" in i:
            continue
        if i.get("state_reason") != "completed":
            continue

        title = i.get("title", "")
        labels = [l.get("name", "").lower() for l in i.get("labels", [])]
        is_feature = title.lower().startswith("feat") or any(
            l in ["feature", "enhancement"] for l in labels
        )

        if is_feature:
            candidates.append(i)

    return candidates


def run_audit(data: dict, repo: str) -> list:
    print(f"[AUDIT] Checking GitHub closed issues for {repo}...")
    completed_features = fetch_completed_feature_requests(repo)
    known = collect_credited_and_ignored_issues(data)

    uncredited = []
    for issue in completed_features:
        num = issue["number"]
        if num not in known:
            uncredited.append(issue)

    if not uncredited:
        print("[AUDIT] All completed feature requests are credited or accounted for.")
        return []

    print(f"[AUDIT] Found {len(uncredited)} uncredited implemented feature request(s):")
    for u in uncredited:
        print(f"  • #{u['number']} by @{u['user']['login']}: \"{u['title']}\"")

    return uncredited


def main() -> None:
    repo_root = Path(__file__).resolve().parent.parent.parent
    default_json = repo_root / ".github" / "contributors.json"
    default_readme = repo_root / "README.md"

    parser = argparse.ArgumentParser(description="Synchronize contributors and credits.")
    parser.add_argument("--repo", default=get_default_repo(), help="GitHub repo (owner/name)")
    parser.add_argument("--data", type=Path, default=default_json, help="Path to contributors.json")
    parser.add_argument("--readme", type=Path, default=default_readme, help="Path to README.md")
    parser.add_argument("--audit", action="store_true", help="Audit for uncredited feature requests")
    parser.add_argument("--sync", action="store_true", help="Automatically fetch and sync missing feature requests")
    parser.add_argument("--add-issue", type=int, help="Fetch issue number and add to contributors")
    parser.add_argument("--title", help="Custom title when adding an issue")

    args = parser.parse_args()
    data = load_contributors_data(args.data)

    if args.add_issue:
        issue = fetch_github_json(f"issues/{args.add_issue}", args.repo)
        username = issue["user"]["login"]
        title = args.title or sanitize_issue_title(issue["title"])
        add_or_update_contributor(data, username, title, args.add_issue)
        save_contributors_data(args.data, data)
        print(f"[ADD] Added #{args.add_issue} by @{username} to {args.data.name}")

    elif args.sync:
        uncredited = run_audit(data, args.repo)
        if uncredited:
            for u in uncredited:
                user = u["user"]["login"]
                title = sanitize_issue_title(u["title"])
                add_or_update_contributor(data, user, title, u["number"])
                print(f"[SYNC] Auto-credited #{u['number']} by @{user}: \"{title}\"")
            save_contributors_data(args.data, data)

    elif args.audit:
        uncredited = run_audit(data, args.repo)
        if uncredited:
            sys.exit(1)
        return

    table = generate_markdown_table(data, args.repo)
    inject_table_into_readme(args.readme, table)


if __name__ == "__main__":
    main()
