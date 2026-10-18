"""
Unit tests for the docs patch-catalog checker.

The catalog tables in docs/apps/*.md are hand-written, so these tests pin the drift
detection that keeps them in sync with the Kotlin patch display names.
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


checker = _load("check_docs_patch_names")


class TestDocsPatchNames(unittest.TestCase):

    def _repo(self, code=("Probe Patch",), docs=("Probe Patch",), app="brave"):
        tmp = Path(tempfile.mkdtemp(prefix="docs_names_"))
        source = tmp / "patches/src/main/kotlin/app/morphe/patches" / app
        source.mkdir(parents=True)
        (tmp / "docs/apps").mkdir(parents=True)
        declarations = "\n".join(
            f'val probe{i} = bytecodePatch(\n    name = "{name}",\n)'
            for i, name in enumerate(code)
        )
        (source / "Probe.kt").write_text(declarations, encoding="utf-8")
        rows = "\n".join(f"| **{name}** | `bytecodePatch` | `true` |" for name in docs)
        doc = (
            "# Guide\n\n## 2. Applied Patches Catalog\n\n"
            "| Patch Name | Typology | Default State |\n"
            "| :--- | :--- | :--- |\n"
            f"{rows}\n\n---\n"
        )
        (tmp / "docs/apps" / f"{app}.md").write_text(doc, encoding="utf-8")
        return tmp

    def test_matching_catalog_passes(self):
        self.assertEqual(checker.check(self._repo(), ("brave",)), [])

    def test_stale_doc_row_is_reported(self):
        failures = checker.check(self._repo(docs=("Old Patch",)), ("brave",))
        self.assertTrue(any("guide lists a patch that does not exist" in f for f in failures))

    def test_undocumented_patch_is_reported(self):
        failures = checker.check(
            self._repo(code=("Probe Patch", "Hidden Patch"), docs=("Probe Patch",)), ("brave",)
        )
        self.assertTrue(any("patch not listed in the guide" in f for f in failures))

    def test_duplicate_patch_names_are_reported(self):
        failures = checker.check(
            self._repo(code=("Probe Patch", "Probe Patch"), docs=("Probe Patch",)), ("brave",)
        )
        self.assertTrue(any("declared more than once" in f for f in failures))


if __name__ == "__main__":
    unittest.main()
