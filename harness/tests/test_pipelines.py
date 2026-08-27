"""
Unit tests for PipelineRegistry, BaseTargetPipeline, and TransactionalMigrationContext.
"""

import tempfile
import unittest
from pathlib import Path
from unittest.mock import MagicMock

from harness.core.pipeline import (
    BaseTargetPipeline,
    PipelineRegistry,
    TransactionalMigrationContext,
)
from harness.migration.patch_migrator import MigrationPlan, PatchMigrator
from harness.pipelines.brave import BravePipeline
from harness.pipelines.gboard import GboardPipeline
from harness.pipelines.vivaldi import VivaldiPipeline


class TestPipelineRegistry(unittest.TestCase):

    def test_pipeline_matching(self):
        self.assertEqual(PipelineRegistry.find_pipeline("com.brave.browser"), BravePipeline)
        self.assertEqual(PipelineRegistry.find_pipeline("com.brave.browser_nightly"), BravePipeline)
        self.assertEqual(PipelineRegistry.find_pipeline("com.google.android.inputmethod.latin"), GboardPipeline)
        self.assertEqual(PipelineRegistry.find_pipeline("com.google.android.apps.inputmethod.latin"), GboardPipeline)
        self.assertEqual(PipelineRegistry.find_pipeline("com.vivaldi.browser.snapshot"), VivaldiPipeline)


class TestTransactionalMigrationContext(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.repo_root = Path(self.temp_dir.name)
        self.constants_file = self.repo_root / "patches/src/main/kotlin/app/morphe/patches/shared/Constants.kt"
        self.constants_file.parent.mkdir(parents=True, exist_ok=True)
        self.initial_content = 'const val BRAVE_TARGET_VERSION = "1.70.0"\n'
        self.constants_file.write_text(self.initial_content, encoding="utf-8")
        self.migrator = PatchMigrator(self.repo_root)

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_successful_commit_persists_changes(self):
        plan = MigrationPlan(
            file_path=self.constants_file,
            original_content=self.initial_content,
            modified_content='const val BRAVE_TARGET_VERSION = "1.71.0"\n',
            changes=["Bumped version to 1.71.0"],
        )
        with TransactionalMigrationContext(self.migrator) as tx:
            applied = tx.apply_plan(plan)
            self.assertTrue(applied)
            self.assertEqual(self.constants_file.read_text(encoding="utf-8"), 'const val BRAVE_TARGET_VERSION = "1.71.0"\n')
            tx.commit()

        # After exiting context, content should remain changed
        self.assertEqual(self.constants_file.read_text(encoding="utf-8"), 'const val BRAVE_TARGET_VERSION = "1.71.0"\n')

    def test_rollback_on_failure_restores_file(self):
        plan = MigrationPlan(
            file_path=self.constants_file,
            original_content=self.initial_content,
            modified_content='const val BRAVE_TARGET_VERSION = "1.71.0"\n',
            changes=["Bumped version to 1.71.0"],
        )
        with TransactionalMigrationContext(self.migrator) as tx:
            tx.apply_plan(plan)
            self.assertEqual(self.constants_file.read_text(encoding="utf-8"), 'const val BRAVE_TARGET_VERSION = "1.71.0"\n')
            tx.rollback()

        # Restored to original
        self.assertEqual(self.constants_file.read_text(encoding="utf-8"), self.initial_content)

    def test_exception_triggers_automatic_rollback(self):
        plan = MigrationPlan(
            file_path=self.constants_file,
            original_content=self.initial_content,
            modified_content='const val BRAVE_TARGET_VERSION = "1.71.0"\n',
            changes=["Bumped version to 1.71.0"],
        )
        try:
            with TransactionalMigrationContext(self.migrator) as tx:
                tx.apply_plan(plan)
                raise RuntimeError("Simulated build error")
        except RuntimeError:
            pass

        # Restored to original
        self.assertEqual(self.constants_file.read_text(encoding="utf-8"), self.initial_content)


if __name__ == "__main__":
    unittest.main()
