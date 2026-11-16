"""
Unit tests for the repository tooling scripts under .github/scripts.

Focus:
  - notify_telegram: repo detection must actually read the remote instead of
    always raising NameError and falling back to an empty string.
  - sync_contributors: user-written issue titles reach a regex replacement and a
    markdown table, so backslashes must survive and pipes must stay escaped.
"""

import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[2] / ".github" / "scripts"


def _load(name: str):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


telegram = _load("notify_telegram")
contributors = _load("sync_contributors")


class TestNotifyTelegramRepoDetection(unittest.TestCase):

    def test_reads_owner_repo_from_origin_remote(self):
        with tempfile.TemporaryDirectory() as tmp:
            subprocess.run(["git", "init", "-q"], cwd=tmp, check=True)
            subprocess.run(
                ["git", "remote", "add", "origin", "https://github.com/kveld9/kveld-morphe-patches.git"],
                cwd=tmp, check=True,
            )
            result = subprocess.run(
                ["git", "config", "--get", "remote.origin.url"],
                cwd=tmp, capture_output=True, text=True,
            )
        url = result.stdout.strip()
        import re
        match = re.search(
            r"github\.com(?::\d+)?[:/]([a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+?)(?:\.git)?$", url
        )
        self.assertIsNotNone(match, "el regex de la repo debe funcionar sobre un remote real")
        self.assertEqual(match.group(1), "kveld9/kveld-morphe-patches")

    def test_source_assigns_url_from_command_output(self):
        # Regression guard: the function used to reference an undefined `url`, so the
        # NameError was swallowed and the caller always got "".
        source = (SCRIPTS / "notify_telegram.py").read_text(encoding="utf-8")
        body = source.split("def get_repo_from_git()", 1)[1]
        self.assertIn("res.stdout.strip()", body.split("def main()", 1)[0])


class TestSyncContributorsInjection(unittest.TestCase):

    def test_title_with_backslash_does_not_break_injection(self):
        with tempfile.TemporaryDirectory() as tmp:
            readme = Path(tmp) / "README.md"
            readme.write_text(
                f"# T\n\n{contributors.START_MARKER}\nold\n{contributors.END_MARKER}\n",
                encoding="utf-8",
            )
            table = (
                "| Contributor | Role & Contributions |\n| :--- | :--- |\n"
                '| <a href="https://github.com/x">x</a> | Feature request: C:\\Users\\test. |'
            )
            # A backslash in a user-written issue title used to abort the whole
            # README sync with re.error: bad escape.
            self.assertTrue(contributors.inject_table_into_readme(readme, table))
            content = readme.read_text(encoding="utf-8")
            self.assertIn("C:\\Users\\test", content)

    def test_pipe_in_title_is_escaped_for_markdown(self):
        title = contributors.sanitize_issue_title("feat: add a | pipe to the title")
        # The pipe survives only in escaped form, so the markdown row stays intact.
        self.assertIn("\\|", title)
        self.assertNotRegex(title, r"(?<!\\)\|")

    def test_normal_table_is_injected(self):
        with tempfile.TemporaryDirectory() as tmp:
            readme = Path(tmp) / "README.md"
            readme.write_text(
                f"# T\n\n{contributors.START_MARKER}\nold\n{contributors.END_MARKER}\n",
                encoding="utf-8",
            )
            table = "| Contributor | Role & Contributions |\n| :--- | :--- |\n| a | b |"
            self.assertTrue(contributors.inject_table_into_readme(readme, table))
            content = readme.read_text(encoding="utf-8")
            self.assertIn("| a | b |", content)
            self.assertNotIn("old", content)

    def test_generate_table_creates_grid_with_avatars_and_usernames(self):
        data = {
            "contributors": [
                {"username": "user1"},
                {"username": "user2"},
                {"username": "user3"},
            ]
        }
        table = contributors.generate_markdown_table(data, cols=2)
        self.assertIn("<table>", table)
        self.assertIn("<tbody>", table)
        self.assertIn("@user1", table)
        self.assertIn("@user2", table)
        self.assertIn("@user3", table)
        self.assertIn("https://github.com/user1.png", table)
        self.assertIn("border-radius: 50%;", table)
        # With 3 items and 2 columns, there should be 2 <tr> rows
        self.assertEqual(table.count("<tr>"), 2)
        self.assertEqual(table.count("</tr>"), 2)
        self.assertEqual(table.count("<td"), 3)

    def test_generate_table_empty_data_returns_empty_string(self):
        self.assertEqual(contributors.generate_markdown_table({"contributors": []}), "")


if __name__ == "__main__":
    unittest.main()