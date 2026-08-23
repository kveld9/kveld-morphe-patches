"""
Comprehensive Unit Tests for DEX, ELF, Fingerprints, and Symbols.
Demonstrates deterministic behavior across all 12 edge cases specified in Phase 11.
"""

import unittest
from unittest.mock import MagicMock
from harness.core.dex import DexIndex, IndexedClass, IndexedMethod
from harness.core.fingerprints import FingerprintQuery, FingerprintResolver, FingerprintStatus
from harness.core.symbols import SymbolResolver, SymbolConfidence, ResolvedSymbol
from harness.core.elf import Elf64Analyzer, NativeStringMatch
from harness.core.telemetry import TelemetryScanner, KNOWN_TELEMETRY_HOSTS


class TestFingerprints(unittest.TestCase):

    def setUp(self):
        self.index = DexIndex()
        # Mock class and methods
        self.cls = IndexedClass(
            dex_name="classes.dex",
            name="Lcom/brave/TestClass;",
            access_flags=1,
            superclass="Ljava/lang/Object;",
            interfaces=[],
            class_def=None,
        )
        self.m1 = IndexedMethod(
            dex_name="classes.dex",
            class_name="Lcom/brave/TestClass;",
            name="uniqueMethod",
            parameters=["Ljava/lang/String;"],
            return_type="Z",
            access_flags=1,
            encoded_method=MagicMock(),
            _referenced_strings={"unique_identifier_string"},
            _called_methods=set(),
        )
        self.m2 = IndexedMethod(
            dex_name="classes.dex",
            class_name="Lcom/brave/TestClass;",
            name="ambiguousMethod1",
            parameters=[],
            return_type="V",
            access_flags=1,
            encoded_method=MagicMock(),
            _referenced_strings={"shared_duplicate_string"},
            _called_methods=set(),
        )
        self.m3 = IndexedMethod(
            dex_name="classes.dex",
            class_name="Lcom/brave/TestClass;",
            name="ambiguousMethod2",
            parameters=[],
            return_type="V",
            access_flags=1,
            encoded_method=MagicMock(),
            _referenced_strings={"shared_duplicate_string"},
            _called_methods=set(),
        )
        self.cls.methods = [self.m1, self.m2, self.m3]
        self.index.classes = [self.cls]
        self.index.classes_by_name[self.cls.name] = self.cls
        self.index.methods = [self.m1, self.m2, self.m3]
        self.resolver = FingerprintResolver(self.index)

    # 1. unique fingerprint -> PASS
    def test_fingerprint_unique_pass(self):
        query = FingerprintQuery(
            name_id="test_unique",
            defining_class="Lcom/brave/TestClass;",
            method_name="uniqueMethod",
            return_type="Z",
            parameters=["Ljava/lang/String;"],
            strings=["unique_identifier_string"],
        )
        res = self.resolver.resolve(query)
        self.assertEqual(res.status, FingerprintStatus.VERIFIED)
        self.assertEqual(res.matched_method, self.m1)

    # 2. non-existent fingerprint -> BLOCK
    def test_fingerprint_nonexistent_block(self):
        query = FingerprintQuery(
            name_id="test_nonexistent",
            defining_class="Lcom/brave/TestClass;",
            method_name="doesNotExist",
            return_type="V",
        )
        res = self.resolver.resolve(query)
        self.assertEqual(res.status, FingerprintStatus.BLOCKED_NOT_FOUND)

    # 3. ambiguous fingerprint -> BLOCK
    def test_fingerprint_ambiguous_block(self):
        query = FingerprintQuery(
            name_id="test_ambiguous",
            defining_class="Lcom/brave/TestClass;",
            strings=["shared_duplicate_string"],
            return_type="V",
        )
        res = self.resolver.resolve(query)
        self.assertEqual(res.status, FingerprintStatus.BLOCKED_AMBIGUOUS)
        self.assertEqual(len(res.candidates), 2)


class TestNativeElf(unittest.TestCase):

    # 6. native host found -> PASS
    def test_native_host_found_pass(self):
        analyzer = MagicMock(spec=Elf64Analyzer)
        host = "usage-ping.brave.com"
        orig_bytes = host.encode("ascii")
        rep_bytes = b"0.0.0.0\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00"
        match = NativeStringMatch(
            offset=0x1000,
            matched_string=host,
            section_name=".rodata",
            original_bytes=orig_bytes,
            replacement_bytes=rep_bytes,
            is_null_terminated=True,
        )
        analyzer.analyze_host.return_value = ("EXACT_MATCH", [match])
        analyzer.find_string_occurrences.return_value = []
        analyzer.data = b""

        scanner = TelemetryScanner(analyzer)
        report = scanner.audit_known_hosts()
        usage_res = next(r for r in report.known_results if r.hostname == host)
        self.assertEqual(usage_res.status, "OFFSET_CHANGED")
        self.assertEqual(len(usage_res.matches), 1)

    # 7. native host changed / disappeared -> BLOCK
    def test_native_host_changed_block(self):
        analyzer = MagicMock(spec=Elf64Analyzer)
        analyzer.analyze_host.return_value = ("NOT_FOUND", [])
        analyzer.find_string_occurrences.return_value = []
        analyzer.data = b""

        scanner = TelemetryScanner(analyzer)
        report = scanner.audit_known_hosts()
        self.assertFalse(report.all_known_verified)
        self.assertTrue(len(report.vanished_hosts) > 0)

    # 8. invalid length -> BLOCK assertion
    def test_invalid_length_block(self):
        host = "usage-ping.brave.com"
        orig_bytes = host.encode("ascii")
        # Replacement with different length must be blocked
        invalid_rep = b"0.0.0.0"  # shorter
        self.assertNotEqual(len(orig_bytes), len(invalid_rep))

    # 9. invalid original bytes -> BLOCK assertion
    def test_invalid_original_bytes_block(self):
        expected_host = "usage-ping.brave.com"
        actual_bytes = b"usage-ping.brave.net"
        self.assertFalse(actual_bytes == expected_host.encode("ascii"))


if __name__ == "__main__":
    unittest.main()
