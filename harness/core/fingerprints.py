"""
Fingerprint Resolution and Verification Engine for Morphe Patches.
Executes exact AST matching, register sniffing, uniqueness verification, and candidate search.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Callable, List, Optional, Set, Tuple

from harness.core.dex import DexIndex, IndexedClass, IndexedMethod


class FingerprintStatus(str, Enum):
    VERIFIED = "VERIFIED"
    BLOCKED_NOT_FOUND = "BLOCKED_NOT_FOUND"
    BLOCKED_AMBIGUOUS = "BLOCKED_AMBIGUOUS"


@dataclass
class FingerprintQuery:
    name_id: str
    defining_class: Optional[str] = None
    method_name: Optional[str] = None
    return_type: Optional[str] = None
    parameters: Optional[List[str]] = None
    strings: List[str] = field(default_factory=list)
    called_methods: List[Tuple[Optional[str], Optional[str], Optional[str]]] = field(default_factory=list)
    opcode_filters: List[str] = field(default_factory=list)
    custom_predicate: Optional[Callable[[IndexedMethod], bool]] = None


@dataclass
class FingerprintResolution:
    query: FingerprintQuery
    status: FingerprintStatus
    matched_method: Optional[IndexedMethod] = None
    candidates: List[IndexedMethod] = field(default_factory=list)
    evidence: List[str] = field(default_factory=list)


class FingerprintResolver:
    """Evaluates FingerprintQueries against a DexIndex with strict uniqueness contracts."""

    def __init__(self, index: DexIndex):
        self.index = index

    def resolve(self, query: FingerprintQuery) -> FingerprintResolution:
        # Determine candidate method pool
        pool: List[IndexedMethod] = []
        if query.defining_class:
            cls = self.index.find_class(query.defining_class)
            if not cls:
                return FingerprintResolution(
                    query=query,
                    status=FingerprintStatus.BLOCKED_NOT_FOUND,
                    evidence=[f"Class '{query.defining_class}' not found in any DEX file."],
                )
            pool = cls.methods
        else:
            # If string filters exist, we can narrow down
            if query.strings:
                first_str = query.strings[0]
                pool = [m for m in self.index.methods if first_str in m.referenced_strings]
            else:
                pool = self.index.methods

        matches: List[IndexedMethod] = []
        evidence_notes: List[str] = []

        for method in pool:
            if not self._method_matches_query(method, query):
                continue
            matches.append(method)

        if len(matches) == 1:
            matched = matches[0]
            evidence_notes.append(f"Exact unique match in {matched.dex_name}: {matched.full_name}")
            if query.strings:
                evidence_notes.append(f"Matched strings: {query.strings}")
            if query.parameters is not None:
                evidence_notes.append(f"Matched parameter signature: ({','.join(matched.parameters)})")
            return FingerprintResolution(
                query=query,
                status=FingerprintStatus.VERIFIED,
                matched_method=matched,
                candidates=matches,
                evidence=evidence_notes,
            )

        elif len(matches) == 0:
            # Structural candidate search
            candidate_pool = self._search_structural_candidates(query)
            evidence_notes.append(f"0 exact matches found for fingerprint '{query.name_id}'.")
            if candidate_pool:
                evidence_notes.append(f"Found {len(candidate_pool)} structural candidates: " +
                                     ", ".join([c.full_name for c in candidate_pool[:3]]))
            return FingerprintResolution(
                query=query,
                status=FingerprintStatus.BLOCKED_NOT_FOUND,
                candidates=candidate_pool,
                evidence=evidence_notes,
            )

        else:
            # Ambiguous (more than 1 match)
            evidence_notes.append(f"Ambiguous fingerprint! Found {len(matches)} matching targets:")
            for m in matches[:5]:
                evidence_notes.append(f"  - {m.full_name} ({m.dex_name})")
            return FingerprintResolution(
                query=query,
                status=FingerprintStatus.BLOCKED_AMBIGUOUS,
                candidates=matches,
                evidence=evidence_notes,
            )

    def _method_matches_query(self, method: IndexedMethod, query: FingerprintQuery) -> bool:
        if query.defining_class and method.class_name != query.defining_class:
            return False

        if query.method_name and method.name != query.method_name:
            return False

        if query.return_type and method.return_type != query.return_type:
            return False

        if query.parameters is not None and method.parameters != query.parameters:
            return False

        # String assertions
        if query.strings:
            m_strings = method.referenced_strings
            if not all(s in m_strings for s in query.strings):
                return False

        # Called methods assertions
        if query.called_methods:
            m_calls = method.called_methods
            for exp_cls, exp_name, _ in query.called_methods:
                match_found = False
                for c_cls, c_name, _ in m_calls:
                    cls_ok = (exp_cls is None or exp_cls == c_cls)
                    name_ok = (exp_name is None or exp_name == c_name)
                    if cls_ok and name_ok:
                        match_found = True
                        break
                if not match_found:
                    return False

        # Opcode / Instruction assertions
        if query.opcode_filters:
            insns = method.get_instructions()
            insn_names = [i.opcode_name for i in insns]
            for exp_op in query.opcode_filters:
                if not any(exp_op in name for name in insn_names):
                    return False

        # Custom predicate
        if query.custom_predicate and not query.custom_predicate(method):
            return False

        return True

    def _search_structural_candidates(self, query: FingerprintQuery) -> List[IndexedMethod]:
        """Search for methods matching a relaxed subset of properties."""
        candidates = []
        # If strings are given, find all methods containing those strings regardless of method name/signature
        if query.strings:
            for m in self.index.methods:
                if all(s in m.referenced_strings for s in query.strings):
                    candidates.append(m)
        elif query.defining_class:
            cls = self.index.find_class(query.defining_class)
            if cls:
                for m in cls.methods:
                    if query.return_type and m.return_type == query.return_type:
                        candidates.append(m)
        return candidates
