"""
Unit tests for the raw const-string index behind DexIndex.methods_referencing().
"""

import struct
import unittest
from unittest.mock import MagicMock

from harness.core.dex import _IndexedDex

CODE_OFF = 0x100
INSNS = CODE_OFF + 16


def _dex_with_insns(insns: bytes, strings):
    raw = bytearray(0x200)
    struct.pack_into("<I", raw, 0x38, len(strings))  # string_ids_size
    struct.pack_into("<I", raw, CODE_OFF + 12, len(insns) // 2)  # insns_size in code units
    raw[INSNS:INSNS + len(insns)] = insns
    dex = MagicMock()
    dex.CM.get_string.side_effect = lambda i: strings[i]
    return _IndexedDex(dex, bytes(raw))


class TestDexStringIndex(unittest.TestCase):

    def test_overlapping_false_hit_does_not_hide_real_const_string(self):
        # nop-ish unit "00 1a", then const-string v0, string@0 ("1a 00 00 00").
        # An odd-aligned false hit starts at the second byte and overlaps the real one.
        indexed = _dex_with_insns(bytes([0x00, 0x1A, 0x1A, 0x00, 0x00, 0x00]), [""])
        method = MagicMock()
        indexed.add_code_range(CODE_OFF, method)
        self.assertEqual(indexed.candidates_for_string(""), [method])

    def test_jumbo_and_shared_code_items(self):
        # const-string/jumbo v0, string@1 shared by two methods (deduplicated code item).
        indexed = _dex_with_insns(bytes([0x1B, 0x00]) + struct.pack("<I", 1), ["a", "b"])
        first, second = MagicMock(), MagicMock()
        indexed.add_code_range(CODE_OFF, first)
        indexed.add_code_range(CODE_OFF, second)
        self.assertEqual(indexed.candidates_for_string("b"), [first, second])

    def test_unknown_string_and_hit_outside_code(self):
        indexed = _dex_with_insns(bytes([0x00, 0x00]), ["a"])
        indexed.add_code_range(CODE_OFF, MagicMock())
        self.assertEqual(indexed.candidates_for_string("missing"), [])
        # string@0 encoded outside any code item must not match.
        raw = bytearray(indexed.raw)
        raw[0x180:0x184] = bytes([0x1A, 0x00, 0x00, 0x00])
        indexed.raw = bytes(raw)
        self.assertEqual(indexed.candidates_for_string("a"), [])


if __name__ == "__main__":
    unittest.main()
