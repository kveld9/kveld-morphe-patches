"""
DEX Multi-file Parsing and Indexing Engine using Androguard and Bytecode Extraction.
Provides unified indexing for classes, methods, fields, strings, opcodes, and call graphs.
"""

from __future__ import annotations

import bisect
import logging
import struct
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Set, Tuple

try:
    from loguru import logger as loguru_logger
    loguru_logger.disable("androguard")
except Exception:
    pass

logging.getLogger("androguard").setLevel(logging.ERROR)

from androguard.core.dex import DEX, EncodedMethod, ClassDefItem, ClassManager
try:
    from androguard.core.dex import TypeMapItem
except ImportError:
    from androguard.core.dex.dex_types import TypeMapItem


# Androguard 4.1.4 ClassManager resolves items by offset using a linear scan
# over internal lists (O(classes x items)), causing quadratic DEX build cost.
# Replace those scans with an on-demand offset dictionary cache:
# - setdefault preserves first-match semantics and returns None when absent.
# - Cache rebuilds if list identity or length changes.
# - If _ClassManager__manage_item is missing or lacks the key, delegates to the
#   original method so layout changes degrade gracefully.

_FAST_LOOKUP_TARGETS = (
    ("ENCODED_ARRAY_ITEM", "get_encoded_array_item"),
    ("ANNOTATIONS_DIRECTORY_ITEM", "get_annotations_directory_item"),
    ("ANNOTATION_SET_ITEM", "get_annotation_set_item"),
    ("ANNOTATION_ITEM", "get_annotation_item"),
    ("HIDDENAPI_CLASS_DATA_ITEM", "get_hiddenapi_class_data_item"),
)


def _build_offset_table(items: Any) -> Dict[int, Any]:
    table: Dict[int, Any] = {}
    for item in items:
        table.setdefault(item.get_off(), item)
    return table


def _get_or_build_offset_table(
    cache: Dict[Any, Tuple[int, int, Dict[int, Any]]],
    item_kind: Any,
    items: Any,
) -> Dict[int, Any]:
    cached = cache.get(item_kind)
    if cached is not None and cached[0] == id(items) and cached[1] == len(items):
        return cached[2]
    table = _build_offset_table(items)
    cache[item_kind] = (id(items), len(items), table)
    return table


def _make_fast_lookup(item_kind: Any, method_name: str, original_method: Any = None) -> Any:
    if original_method is None:
        original_method = getattr(ClassManager, method_name)

    def replacement(self: Any, off: int) -> Any:
        manage_items = self.__dict__.get("_ClassManager__manage_item")
        if not isinstance(manage_items, dict) or item_kind not in manage_items:
            return original_method(self, off)

        items = manage_items[item_kind]
        cache = self.__dict__.setdefault("_morphe_offset_cache", {})
        table = _get_or_build_offset_table(cache, item_kind, items)
        return table.get(off)

    replacement.__name__ = method_name
    replacement._morphe_fast_lookup = True  # type: ignore[attr-defined]
    return replacement


def _patch_class_manager_method(item_attr: str, method_name: str) -> None:
    item_kind = getattr(TypeMapItem, item_attr, None)
    original_method = getattr(ClassManager, method_name, None)
    if item_kind is None or original_method is None:
        return
    if getattr(original_method, "_morphe_fast_lookup", False):
        return
    replacement = _make_fast_lookup(item_kind, method_name, original_method)
    setattr(ClassManager, method_name, replacement)


def _install_fast_class_manager_lookups() -> None:
    for item_attr, method_name in _FAST_LOOKUP_TARGETS:
        _patch_class_manager_method(item_attr, method_name)


_install_fast_class_manager_lookups()


@dataclass
class DexInstructionWrapper:
    offset: int
    opcode_name: str
    raw_insn: Any
    string_value: Optional[str] = None


@dataclass
class IndexedMethod:
    dex_name: str
    class_name: str
    name: str
    parameters: List[str]
    return_type: str
    access_flags: int
    encoded_method: EncodedMethod
    _instructions: Optional[List[DexInstructionWrapper]] = None
    _referenced_strings: Optional[Set[str]] = None
    _called_methods: Optional[Set[Tuple[str, str, str]]] = None

    @property
    def signature(self) -> str:
        return f"({','.join(self.parameters)}){self.return_type}"

    @property
    def full_name(self) -> str:
        return f"{self.class_name}->{self.name}{self.signature}"

    def get_instructions(self) -> List[DexInstructionWrapper]:
        if self._instructions is not None:
            return self._instructions
        self._instructions = []
        self._referenced_strings = set()

        code = self.encoded_method.get_code()
        if not code:
            return self._instructions

        for ins in code.get_bc().get_instructions():
            str_val = self._extract_string_ref(ins)
            if str_val is not None:
                self._referenced_strings.add(str_val)

            self._instructions.append(DexInstructionWrapper(
                offset=ins.get_op_value(),
                opcode_name=ins.get_name(),
                raw_insn=ins,
                string_value=str_val,
            ))
        return self._instructions

    @staticmethod
    def _extract_string_ref(ins: Any) -> Optional[str]:
        if "const-string" in ins.get_name():
            try:
                return ins.get_string()
            except Exception:
                pass
        return None

    @staticmethod
    def _extract_method_call(ins: Any) -> Optional[Tuple[str, str, str]]:
        if "invoke-" not in ins.get_name():
            return None
        try:
            raw_str = str(ins)
            if "->" in raw_str:
                target_part = raw_str.split("->", 1)[1]
                m_name = target_part.split("(", 1)[0].strip()
                c_part = raw_str.split("->", 1)[0].split()[-1]
                return (c_part, m_name, "")
        except Exception:
            pass
        return None

    @property
    def referenced_strings(self) -> Set[str]:
        if self._referenced_strings is None:
            self.get_instructions()
        return self._referenced_strings or set()

    @property
    def called_methods(self) -> Set[Tuple[str, str, str]]:
        # Rendering every invoke to text is expensive, so call targets are only
        # extracted for the few methods a query actually inspects.
        if self._called_methods is None:
            calls = (self._extract_method_call(i.raw_insn) for i in self.get_instructions())
            self._called_methods = {c for c in calls if c is not None}
        return self._called_methods


@dataclass
class IndexedClass:
    dex_name: str
    name: str
    access_flags: int
    superclass: Optional[str]
    interfaces: List[str]
    class_def: ClassDefItem
    methods: List[IndexedMethod] = field(default_factory=list)
    fields: List[Tuple[str, str, int]] = field(default_factory=list)  # (name, type, access_flags)

    def get_all_hierarchy_methods(self, index: DexIndex, max_depth: int = 6) -> List[IndexedMethod]:
        """Returns all methods defined on this class and its superclasses."""
        all_methods = list(self.methods)
        curr = self
        depth = 0
        while curr and curr.superclass and depth < max_depth:
            parent = index.find_class(curr.superclass)
            if not parent:
                break
            all_methods.extend(parent.methods)
            curr = parent
            depth += 1
        return all_methods


class _IndexedDex:
    """Raw DEX buffer plus the lookups needed to resolve const-string references by id."""

    def __init__(self, dex: DEX, raw: bytes):
        self.dex = dex
        self.raw = raw
        self._code_items: Dict[int, Tuple[int, List[IndexedMethod]]] = {}  # insns start -> (end, methods)
        self._starts: Optional[List[int]] = None
        self._string_ids: Optional[Dict[str, int]] = None

    def add_code_range(self, code_off: int, method: IndexedMethod):
        if not code_off:
            return
        # code_item: insns_size (uint32, in 16-bit units) at +12, insns at +16.
        insns_units = struct.unpack_from("<I", self.raw, code_off + 12)[0]
        start = code_off + 16
        # Identical code items may be shared by several methods.
        self._code_items.setdefault(start, (start + insns_units * 2, []))[1].append(method)
        self._starts = None

    def _string_id(self, value: str) -> Optional[int]:
        if self._string_ids is None:
            count = struct.unpack_from("<I", self.raw, 0x38)[0]
            self._string_ids = {self.dex.CM.get_string(i): i for i in range(count)}
        return self._string_ids.get(value)

    def candidates_for_string(self, value: str) -> List[IndexedMethod]:
        idx = self._string_id(value)
        if idx is None:
            return []
        if self._starts is None:
            self._starts = sorted(self._code_items)
        # const-string vAA, string@BBBB (0x1a) and const-string/jumbo vAA, string@BBBBBBBB (0x1b):
        # find the literal id (C-speed bytes.find, every occurrence, overlaps included) and
        # check the opcode two bytes before it.
        encodings = [(0x1B, struct.pack("<I", idx))]
        if idx <= 0xFFFF:
            encodings.append((0x1A, struct.pack("<H", idx)))
        raw = self.raw
        found: Dict[int, IndexedMethod] = {}
        for opcode, needle in encodings:
            pos = raw.find(needle, 2)
            while pos != -1:
                off = pos - 2
                if raw[off] == opcode:
                    slot = bisect.bisect_right(self._starts, off) - 1
                    if slot >= 0:
                        start = self._starts[slot]
                        end, methods = self._code_items[start]
                        if off < end and (off - start) % 2 == 0:
                            for method in methods:
                                found[id(method)] = method
                pos = raw.find(needle, pos + 1)
        return list(found.values())


class DexIndex:
    """High performance indexing container over all DEX files in an APK."""

    def __init__(self):
        self.classes_by_name: Dict[str, IndexedClass] = {}
        self.classes: List[IndexedClass] = []
        self.methods: List[IndexedMethod] = []
        self._dexes: List[_IndexedDex] = []
        self._string_refs: Dict[str, List[IndexedMethod]] = {}

    def index_dex_files(self, dex_entries: List[Tuple[str, bytes]]):
        for dex_name, raw_bytes in dex_entries:
            d = DEX(raw_bytes)
            indexed_dex = _IndexedDex(d, raw_bytes)
            self._dexes.append(indexed_dex)

            for c in d.get_classes():
                cls_name = c.get_name()
                super_cls = c.get_superclassname()
                ifaces = list(c.get_interfaces())
                access_flags = c.get_access_flags()

                idx_cls = IndexedClass(
                    dex_name=dex_name,
                    name=cls_name,
                    access_flags=access_flags,
                    superclass=super_cls,
                    interfaces=ifaces,
                    class_def=c,
                )

                # Fields
                for f in c.get_fields():
                    idx_cls.fields.append((f.get_name(), f.get_descriptor(), f.get_access_flags()))

                # Methods
                for m in c.get_methods():
                    desc = m.get_descriptor()
                    # parse descriptor: (param1param2...)return
                    params, ret_type = self._parse_descriptor(desc)
                    idx_m = IndexedMethod(
                        dex_name=dex_name,
                        class_name=cls_name,
                        name=m.get_name(),
                        parameters=params,
                        return_type=ret_type,
                        access_flags=m.get_access_flags(),
                        encoded_method=m,
                    )
                    idx_cls.methods.append(idx_m)
                    self.methods.append(idx_m)
                    indexed_dex.add_code_range(m.get_code_off(), idx_m)

                self.classes.append(idx_cls)
                self.classes_by_name[cls_name] = idx_cls

    def find_class(self, name: str) -> Optional[IndexedClass]:
        return self.classes_by_name.get(name)

    def methods_referencing(self, value: str) -> List[IndexedMethod]:
        """Methods holding a const-string of `value`, without decoding every method.

        Raw const-string encodings of the string id are located in each DEX buffer and
        mapped to their code items; only those candidates are decoded to confirm the hit.
        """
        cached = self._string_refs.get(value)
        if cached is not None:
            return cached
        result: List[IndexedMethod] = []
        for indexed_dex in self._dexes:
            for method in indexed_dex.candidates_for_string(value):
                if value in method.referenced_strings:
                    result.append(method)
        self._string_refs[value] = result
        return result

    @classmethod
    def _parse_descriptor(cls, desc: str) -> Tuple[List[str], str]:
        if not desc.startswith("(") or ")" not in desc:
            return [], desc
        param_part, ret_type = desc[1:].split(")", 1)
        param_part = param_part.replace(" ", "")
        ret_type = ret_type.strip()
        params = []
        i = 0
        while i < len(param_part):
            type_str, i = cls._parse_next_type(param_part, i)
            if type_str:
                params.append(type_str)
            else:
                break
        return params, ret_type

    @staticmethod
    def _parse_next_type(param_part: str, i: int) -> Tuple[Optional[str], int]:
        dim = 0
        while i < len(param_part) and param_part[i] == "[":
            dim += 1
            i += 1

        if i >= len(param_part):
            return None, i

        prefix = "[" * dim
        c = param_part[i]
        if c in "ZBSCIJFD":
            return prefix + c, i + 1
        elif c == "L":
            semi = param_part.find(";", i)
            if semi != -1:
                return prefix + param_part[i:semi + 1], semi + 1
        return None, i + 1


def find_contract_target(index: DexIndex, scope: List[IndexedClass], target: str) -> List[IndexedMethod]:
    """Methods satisfying a contract target that is either a method name or a string literal.

    Names are matched inside the contract classes (globally when none are declared). String
    literals are matched across every DEX, like the patch fingerprints that consume them:
    obfuscated helpers outside the contract classes often hold the experiment keys.
    """
    name_pool = [m for cls in scope for m in cls.methods] if scope else index.methods
    matched = {id(m): m for m in name_pool if m.name == target}
    matched.update((id(m), m) for m in index.methods_referencing(target))
    return list(matched.values())
