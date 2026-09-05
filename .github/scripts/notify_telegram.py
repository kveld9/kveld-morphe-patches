#!/usr/bin/env python3
"""
Sends a Telegram release notification with formatted changelog.
"""

import html
import json
import os
import re
import subprocess
import sys
import urllib.parse
import urllib.request

TELEGRAM_MAX_MESSAGE_LENGTH = 4096


def markdown_to_telegram_html(text: str) -> str:
    """Converts basic markdown from semantic-release notes to Telegram-safe HTML."""
    if not text:
        return ""

    text = html.escape(text.strip())
    # Remove redundant top-level version header: ## [1.13.0](...) (...)
    text = re.sub(r"^##\s+\[.*?\].*?$", "", text, flags=re.MULTILINE)
    # Convert headers (### Section -> <b>Section</b>)
    text = re.sub(r"^#{1,6}\s+(.+)$", r"<b>\1</b>", text, flags=re.MULTILINE)
    # Convert bold (**text** -> <b>text</b>)
    text = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", text)
    # Convert markdown links ([text](url) -> <a href="url">text</a>)
    text = re.sub(r"\[(.*?)\]\((https?://[^\s)]+)\)", r'<a href="\2">\1</a>', text)
    # Convert bullet points (* text -> • text)
    text = re.sub(r"^\*\s+", "• ", text, flags=re.MULTILINE)
    # Convert inline code (`code` -> <code>code</code>)
    text = re.sub(r"`([^`]+)`", r"<code>\1</code>", text)
    # Remove excessive blank lines
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    return text


def build_message(
    version: str,
    tag: str,
    notes: str,
    repo: str,
    morphe_source_url: str = "https://morphe.software/add-source?github=kveld9/brave-patches",
) -> str:
    display_tag = tag if tag else f"v{version}"
    header = f"🚀 <b>New Release: Morphe Patches {display_tag}</b>\n\n"

    body = markdown_to_telegram_html(notes)

    links = []
    if repo:
        release_url = f"https://github.com/{repo}/releases/tag/{display_tag}"
        links.append(f'🔗 <a href="{release_url}">View on GitHub</a>')
    if morphe_source_url:
        links.append(f'📲 <a href="{morphe_source_url}">Add to Morphe Manager</a>')

    footer = "\n\n" + "\n".join(links) if links else ""

    # Ensure message does not exceed Telegram limits
    fixed_len = len(header) + len(footer)
    max_body_len = TELEGRAM_MAX_MESSAGE_LENGTH - fixed_len - 100

    if len(body) > max_body_len:
        body = body[:max_body_len] + "...\n\n<i>(Changelog truncated, see GitHub release)</i>"

    return f"{header}{body}{footer}".strip()


def send_telegram_message(token: str, chat_id: str, text: str):
    url = f"https://api.telegram.org/bot{token}/sendMessage"
    payload = {
        "chat_id": chat_id,
        "text": text,
        "parse_mode": "HTML",
        "disable_web_page_preview": True,
    }
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))


def sanitize_chat_id(chat_id: str) -> str:
    chat_id = chat_id.strip()
    match = re.search(r"(?:https?://)?t\.me/([a-zA-Z0-9_]+)/?$", chat_id)
    if match:
        return f"@{match.group(1)}"
    if not chat_id.startswith(("-", "@")) and not chat_id.lstrip("-").isdigit():
        return f"@{chat_id}"
    return chat_id


def extract_latest_from_changelog():
    if not os.path.isfile("CHANGELOG.md"):
        return "", "", ""
    try:
        with open("CHANGELOG.md", "r", encoding="utf-8") as f:
            content = f.read()
        match = re.search(
            r"^##\s+\[([0-9.]+)\](?:\((.*?)\))?.*?\n(.*?)(?=\n##\s+\[|\Z)",
            content,
            re.DOTALL | re.MULTILINE,
        )
        if match:
            version = match.group(1)
            notes = match.group(3).strip()
            tag = f"v{version}"
            return version, tag, notes
    except Exception:
        pass
    return "", "", ""


def get_repo_from_git() -> str:
    try:
        cmd = ["git", "remote", "get-url", "origin"]
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        url = res.stdout.strip()
        m = re.search(r"github\.com[:/]([^/]+/[^/.]+)(?:\.git)?", url)
        if m:
            return m.group(1)
    except Exception:
        pass
    return ""


def main():
    token = os.environ.get("TELEGRAM_BOT_TOKEN")
    chat_id = os.environ.get("TELEGRAM_CHAT_ID")
    version = os.environ.get("RELEASE_VERSION", "")
    tag = os.environ.get("RELEASE_TAG", "")
    notes = os.environ.get("RELEASE_NOTES", "")
    repo = os.environ.get("GITHUB_REPOSITORY", "")
    dry_run = os.environ.get("TELEGRAM_DRY_RUN", "").lower() in ("1", "true", "yes")

    if not version or not notes:
        cl_version, cl_tag, cl_notes = extract_latest_from_changelog()
        if not version:
            version = cl_version
        if not tag:
            tag = cl_tag
        if not notes:
            notes = cl_notes

    if not repo:
        repo = get_repo_from_git()

    if not token or not chat_id:
        print(
            "::warning::TELEGRAM_BOT_TOKEN or TELEGRAM_CHAT_ID not provided. Skipping Telegram notification."
        )
        return

    chat_id = sanitize_chat_id(chat_id)
    morphe_url = os.environ.get(
        "MORPHE_SOURCE_URL",
        "https://morphe.software/add-source?github=kveld9/brave-patches",
    )
    message = build_message(version, tag, notes, repo, morphe_source_url=morphe_url)

    if dry_run:
        print("=== DRY RUN: TELEGRAM MESSAGE ===")
        print(message)
        print("================================")
        return

    try:
        print("Sending Telegram release notification...")
        send_telegram_message(token, chat_id, message)
        print("Telegram notification sent successfully.")
    except Exception as e:
        print(f"Error sending Telegram message: {e}", file=sys.stderr)


if __name__ == "__main__":
    main()
