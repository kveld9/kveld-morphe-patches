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
        pool, err_res = self._get_candidate_pool(query)
        if err_res:
            return err_res

        matches = [m for m in pool if self._method_matches_query(m, query)]
        if len(matches) == 1:
            return self._build_verified_resolution(query, matches[0], matches)
        elif len(matches) == 0:
            return self._build_not_found_resolution(query)
        return self._build_ambiguous_resolution(query, matches)

    def _get_candidate_pool(self, query: FingerprintQuery) -> Tuple[List[IndexedMethod], Optional[FingerprintResolution]]:
        if query.defining_class:
            cls = self.index.find_class(query.defining_class)
            if not cls:
                return [], FingerprintResolution(
                    query=query,
                    status=FingerprintStatus.BLOCKED_NOT_FOUND,
                    evidence=[f"Class '{query.defining_class}' not found in any DEX file."],
                )
            return cls.methods, None

        if query.strings:
            first_str = query.strings[0]
            return [m for m in self.index.methods if first_str in m.referenced_strings], None
        return self.index.methods, None

    @staticmethod
    def _build_verified_resolution(
        query: FingerprintQuery,
        matched: IndexedMethod,
        matches: List[IndexedMethod]
    ) -> FingerprintResolution:
        evidence_notes = [f"Exact unique match in {matched.dex_name}: {matched.full_name}"]
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

    def _build_not_found_resolution(self, query: FingerprintQuery) -> FingerprintResolution:
        candidate_pool = self._search_structural_candidates(query)
        evidence_notes = [f"0 exact matches found for fingerprint '{query.name_id}'."]
        if candidate_pool:
            evidence_notes.append(f"Found {len(candidate_pool)} structural candidates: " +
                                 ", ".join([c.full_name for c in candidate_pool[:3]]))
        return FingerprintResolution(
            query=query,
            status=FingerprintStatus.BLOCKED_NOT_FOUND,
            candidates=candidate_pool,
            evidence=evidence_notes,
        )

    @staticmethod
    def _build_ambiguous_resolution(
        query: FingerprintQuery,
        matches: List[IndexedMethod]
    ) -> FingerprintResolution:
        evidence_notes = [f"Multiple candidate matches found ({len(matches)}) for '{query.name_id}':"]
        for m in matches[:5]:
            evidence_notes.append(f"  - {m.full_name} ({m.dex_name})")
        return FingerprintResolution(
            query=query,
            status=FingerprintStatus.BLOCKED_AMBIGUOUS,
            candidates=matches,
            evidence=evidence_notes,
        )

    def _method_matches_query(self, method: IndexedMethod, query: FingerprintQuery) -> bool:
        if not self._matches_signature(method, query):
            return False
        if query.strings and not self._matches_strings(method, query.strings):
            return False
        if query.called_methods and not self._matches_called_methods(method, query.called_methods):
            return False
        if query.opcode_filters and not self._matches_opcodes(method, query.opcode_filters):
            return False
        if query.custom_predicate and not query.custom_predicate(method):
            return False
        return True

    @staticmethod
    def _matches_signature(method: IndexedMethod, query: FingerprintQuery) -> bool:
        if query.defining_class and method.class_name != query.defining_class:
            return False
        if query.method_name and method.name != query.method_name:
            return False
        if query.return_type and method.return_type != query.return_type:
            return False
        if query.parameters is not None and method.parameters != query.parameters:
            return False
        return True

    @staticmethod
    def _matches_strings(method: IndexedMethod, required_strings: List[str]) -> bool:
        m_strings = method.referenced_strings
        return all(s in m_strings for s in required_strings)

    @staticmethod
    def _matches_called_methods(
        method: IndexedMethod,
        required_calls: List[Tuple[Optional[str], Optional[str], Optional[str]]]
    ) -> bool:
        m_calls = method.called_methods
        for exp_cls, exp_name, _ in required_calls:
            matched = any(
                (exp_cls is None or exp_cls == c_cls) and (exp_name is None or exp_name == c_name)
                for c_cls, c_name, _ in m_calls
            )
            if not matched:
                return False
        return True

    @staticmethod
    def _matches_opcodes(method: IndexedMethod, opcode_filters: List[str]) -> bool:
        insn_names = [i.opcode_name for i in method.get_instructions()]
        return all(any(exp_op in name for name in insn_names) for exp_op in opcode_filters)

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
