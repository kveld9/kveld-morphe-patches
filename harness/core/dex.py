"""
DEX Multi-file Parsing and Indexing Engine using Androguard and Bytecode Extraction.
Provides unified indexing for classes, methods, fields, strings, opcodes, and call graphs.
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Set, Tuple

try:
    from loguru import logger as loguru_logger
    loguru_logger.disable("androguard")
except Exception:
    pass

logging.getLogger("androguard").setLevel(logging.ERROR)

from androguard.core.dex import DEX, EncodedMethod, ClassDefItem


@dataclass
class DexInstructionWrapper:
    offset: int
    opcode_name: str
    raw_insn: any
    string_value: Optional[str] = None
    method_ref: Optional[Tuple[str, str, List[str], str]] = None
    field_ref: Optional[Tuple[str, str, str]] = None


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
        self._called_methods = set()

        code = self.encoded_method.get_code()
        if not code:
            return self._instructions

        for ins in code.get_bc().get_instructions():
            str_val = self._extract_string_ref(ins)
            call_ref = self._extract_method_call(ins)
            if str_val is not None:
                self._referenced_strings.add(str_val)
            if call_ref is not None:
                self._called_methods.add(call_ref)

            self._instructions.append(DexInstructionWrapper(
                offset=ins.get_op_value(),
                opcode_name=ins.get_name(),
                raw_insn=ins,
                string_value=str_val,
                method_ref=None,
                field_ref=None,
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
        if self._called_methods is None:
            self.get_instructions()
        return self._called_methods or set()


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


class DexIndex:
    """High performance indexing container over all DEX files in an APK."""

    def __init__(self):
        self.classes_by_name: Dict[str, IndexedClass] = {}
        self.classes: List[IndexedClass] = []
        self.methods: List[IndexedMethod] = []
        self.string_to_methods: Dict[str, List[IndexedMethod]] = {}
        self.dex_objects: List[Tuple[str, DEX]] = []

    def index_dex_files(self, dex_entries: List[Tuple[str, bytes]]):
        for dex_name, raw_bytes in dex_entries:
            d = DEX(raw_bytes)
            self.dex_objects.append((dex_name, d))

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

                self.classes.append(idx_cls)
                self.classes_by_name[cls_name] = idx_cls

    def find_class(self, name: str) -> Optional[IndexedClass]:
        return self.classes_by_name.get(name)

    @classmethod
    def _parse_descriptor(cls, desc: str) -> Tuple[List[str], str]:
        if not desc.startswith("(") or ")" not in desc:
            return [], desc
        param_part, ret_type = desc[1:].split(")", 1)
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
