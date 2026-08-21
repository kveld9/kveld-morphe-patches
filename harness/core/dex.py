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

        bc = code.get_bc()
        for ins in bc.get_instructions():
            op_name = ins.get_name()
            str_val = None
            m_ref = None
            f_ref = None

            if "const-string" in op_name:
                try:
                    str_val = ins.get_string()
                    if str_val is not None:
                        self._referenced_strings.add(str_val)
                except Exception:
                    pass

            elif "invoke-" in op_name:
                try:
                    # method format: Lclass/name;->methodName(params)returnType
                    target_cls = ins.cm.get_type(ins.get_ref_kind() if hasattr(ins, 'get_ref_kind') else 0)
                except Exception:
                    target_cls = None
                try:
                    raw_str = str(ins)
                    # parse invoke target e.g. invoke-virtual {v0, v1}, Lcls;->method(args)Ret
                    if "->" in raw_str:
                        target_part = raw_str.split("->", 1)[1]
                        m_name = target_part.split("(", 1)[0].strip()
                        # target class
                        c_part = raw_str.split("->", 1)[0].split()[-1]
                        self._called_methods.add((c_part, m_name, ""))
                except Exception:
                    pass

            self._instructions.append(DexInstructionWrapper(
                offset=ins.get_op_value(),
                opcode_name=op_name,
                raw_insn=ins,
                string_value=str_val,
                method_ref=m_ref,
                field_ref=f_ref,
            ))
        return self._instructions

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

    @staticmethod
    def _parse_descriptor(desc: str) -> Tuple[List[str], str]:
        if not desc.startswith("(") or ")" not in desc:
            return [], desc
        param_part, ret_type = desc[1:].split(")", 1)
        params = []
        i = 0
        while i < len(param_part):
            c = param_part[i]
            if c in "ZBSCIJFD":
                params.append(c)
                i += 1
            elif c == "L":
                semi = param_part.find(";", i)
                if semi != -1:
                    params.append(param_part[i:semi + 1])
                    i = semi + 1
                else:
                    break
            elif c == "[":
                # Array type
                dim = 0
                while i < len(param_part) and param_part[i] == "[":
                    dim += 1
                    i += 1
                if i < len(param_part):
                    if param_part[i] in "ZBSCIJFD":
                        params.append("[" * dim + param_part[i])
                        i += 1
                    elif param_part[i] == "L":
                        semi = param_part.find(";", i)
                        if semi != -1:
                            params.append("[" * dim + param_part[i:semi + 1])
                            i = semi + 1
                        else:
                            break
            else:
                i += 1
        return params, ret_type
