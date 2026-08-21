"""
Gboard Structured Audit and Update Report Generator.
Compatibility re-export pointing to the universal HarnessReporter.
"""

from __future__ import annotations

from harness.reporting.reporter import HarnessReporter, HarnessReportData, GboardAuditReporter, GboardReportData

__all__ = ["HarnessReporter", "HarnessReportData", "GboardAuditReporter", "GboardReportData"]
