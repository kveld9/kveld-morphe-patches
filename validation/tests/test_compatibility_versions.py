"""
Unit tests for the target-version consistency checker.

The pinned versions live in Constants.kt while docs/compatibility.md and the guides in
docs/apps/*.md restate them by hand; these tests pin the drift detection that keeps all
three in sync.
"""

import importlib.util
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[2] / ".github" / "scripts"


def _load(name: str):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


checker = _load("check_compatibility_versions")

CONSTANTS = (
    "object Constants {\n"
    '    const val BRAVE_TARGET_VERSION = "1.97.59"\n'
    '    const val GBOARD_TARGET_VERSION = "18.5.1.993608717-lite_beta-arm64-v8a"\n'
    '    const val GBOARD_TARGET_VERSION_V7A = "18.5.1.993608717-lite_beta-armeabi-v7a"\n'
    '    const val TIKTOK_TARGET_VERSION = "47.1.4"\n'
    "}\n"
)


def _docs(guide_version: str, compat_version: str) -> tuple[str, str]:
    guide = f"# Guide\n\n| | |\n| :--- | :--- |\n| **Target Version** | {guide_version} |\n"
    compat = (
        "| Application | Supported Version | Architectures |\n"
        "| :--- | :--- | :--- |\n"
        f"| **App** | {compat_version} | `arm64-v8a` | [Guide](apps/brave.md) |\n"
    )
    return guide, compat


class TestCompatibilityVersions(unittest.TestCase):

    def _repo(self, guide_version="`1.97.59`", compat_version="`1.97.59`"):
        tmp = Path(tempfile.mkdtemp(prefix="compat_versions_"))
        shared = tmp / "patches/src/main/kotlin/app/morphe/patches/shared"
        shared.mkdir(parents=True)
        (shared / "Constants.kt").write_text(CONSTANTS, encoding="utf-8")
        (tmp / "docs/apps").mkdir(parents=True)
        guide, compat = _docs(guide_version, compat_version)
        (tmp / "docs/apps/brave.md").write_text(guide, encoding="utf-8")
        (tmp / "docs/compatibility.md").write_text(compat, encoding="utf-8")
        return tmp

    def test_agreed_versions_pass(self):
        self.assertEqual(checker.check(self._repo(), ("brave",)), [])

    def test_version_with_abi_suffix_maps_to_its_base(self):
        tmp = Path(tempfile.mkdtemp(prefix="compat_versions_"))
        shared = tmp / "patches/src/main/kotlin/app/morphe/patches/shared"
        shared.mkdir(parents=True)
        (shared / "Constants.kt").write_text(
            "object Constants {\n"  # noqa: SIM999
            '    const val BRAVE_TARGET_VERSION = "2.0.0-armeabi-v7a"\n'
            '    const val BRAVE_TARGET_VERSION_V64 = "2.0.0-arm64-v8a"\n'
            "}\n",
            encoding="utf-8",
        )
        (tmp / "docs/apps").mkdir(parents=True)
        guide, compat = _docs("`2.0.0-arm64-v8a` / `2.0.0-armeabi-v7a`", "`2.0.0`")
        (tmp / "docs/apps/brave.md").write_text(guide, encoding="utf-8")
        (tmp / "docs/compatibility.md").write_text(compat, encoding="utf-8")
        self.assertEqual(checker.check(tmp, ("brave",)), [])

    def test_stale_compatibility_version_is_reported(self):
        failures = checker.check(self._repo(compat_version="`1.96.12`"), ("brave",))
        self.assertTrue(any("compatibility.md supports" in f for f in failures))

    def test_missing_guide_version_is_reported(self):
        failures = checker.check(self._repo(guide_version="`1.96.0-legacy`"), ("brave",))
        self.assertTrue(any("missing the pinned version" in f for f in failures))


if __name__ == "__main__":
    unittest.main()
