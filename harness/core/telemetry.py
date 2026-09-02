"""
Telemetry Domain Detector and Classification Engine.
Analyzes native ELF binaries and DEX constants to classify known, shifted, vanished,
and newly discovered telemetry endpoints.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Set, Tuple

from harness.core.elf import Elf64Analyzer, NativeStringMatch


# Standard known telemetry host entries from BraveBlockTelemetryPatch
KNOWN_TELEMETRY_HOSTS: List[str] = [
    "star-randsrv.bsg.brave.com",
    "collector.bsg.brave.com",
    "usage-ping.brave.com",
    "patterns.wdp.brave.com",
    "collector.wdp.brave.com",
    "star.wdp.brave.com",
    "quorum.wdp.brave.com",
    "cr.brave.com",
    "crashpad.chromium.org",
    "variations.brave.com",
]

# False-positive or non-telemetry hostnames ignored during candidate discovery
IGNORED_TELEMETRY_CANDIDATES: Set[str] = {
    "ai-chat.bsg.brave.com",
}


@dataclass
class HostAuditResult:
    hostname: str
    expected_count: int
    found_count: int
    matches: List[NativeStringMatch]
    status: str  # 'VERIFIED', 'OFFSET_CHANGED', 'VANISHED', 'AMBIGUOUS'
    old_offsets: List[int] = field(default_factory=list)
    new_offsets: List[int] = field(default_factory=list)


@dataclass
class TelemetryCandidate:
    hostname: str
    offset: int
    section: Optional[str]
    context_snippet: str
    frequency: int
    classification_reason: str


@dataclass
class TelemetryReport:
    known_results: List[HostAuditResult]
    all_known_verified: bool
    vanished_hosts: List[str]
    shifted_hosts: List[HostAuditResult]
    discovered_candidates: List[TelemetryCandidate]


class TelemetryScanner:
    """Scans and verifies native binary telemetry endpoints."""

    def __init__(self, elf_analyzer: Elf64Analyzer):
        self.elf = elf_analyzer

    def audit_known_hosts(self, current_patch_offsets: Optional[Dict[str, List[int]]] = None) -> TelemetryReport:
        known_results: List[HostAuditResult] = []
        vanished: List[str] = []
        shifted: List[HostAuditResult] = []
        all_verified = True

        expected_counts = {
            "crashpad.chromium.org": 2,
            "variations.brave.com": 2,
        }

        for host in KNOWN_TELEMETRY_HOSTS:
            exp_count = expected_counts.get(host, 1)
            old_offs = (current_patch_offsets or {}).get(host, [])
            status, matches = self.elf.analyze_host(host)

            new_offs = [m.offset for m in matches]
            found_cnt = len(matches)

            if found_cnt == 0:
                host_status = "VANISHED"
                vanished.append(host)
                all_verified = False
            elif found_cnt == exp_count:
                if old_offs and sorted(old_offs) == sorted(new_offs):
                    host_status = "VERIFIED"
                else:
                    host_status = "OFFSET_CHANGED"
                    shifted.append(HostAuditResult(
                        hostname=host,
                        expected_count=exp_count,
                        found_count=found_cnt,
                        matches=matches,
                        status=host_status,
                        old_offsets=old_offs,
                        new_offsets=new_offs,
                    ))
            else:
                host_status = "AMBIGUOUS"
                all_verified = False

            known_results.append(HostAuditResult(
                hostname=host,
                expected_count=exp_count,
                found_count=found_cnt,
                matches=matches,
                status=host_status,
                old_offsets=old_offs,
                new_offsets=new_offs,
            ))

        # Scan for potential new telemetry candidates in ELF
        candidates = self.discover_new_telemetry_candidates()

        return TelemetryReport(
            known_results=known_results,
            all_known_verified=all_verified and len(vanished) == 0,
            vanished_hosts=vanished,
            shifted_hosts=shifted,
            discovered_candidates=candidates,
        )

    def discover_new_telemetry_candidates(self) -> List[TelemetryCandidate]:
        """Search ELF for unknown brave/telemetry endpoints."""
        candidates: List[TelemetryCandidate] = []
        # Find ASCII strings matching brave subdomains or analytics keywords
        pattern = re.compile(rb"([a-zA-Z0-9_-]+\.(?:bsg|wdp|p3a|telemetry|analytics|stats)\.brave\.com)")
        seen_hosts: Set[str] = set()

        for match in pattern.finditer(self.elf.data):
            raw_host = match.group(1).decode("ascii", errors="replace")
            if raw_host in KNOWN_TELEMETRY_HOSTS or raw_host in seen_hosts or raw_host in IGNORED_TELEMETRY_CANDIDATES:
                continue
            seen_hosts.add(raw_host)

            offset = match.start()
            sec = self.elf.get_section_at_offset(offset)
            start_ctx = max(0, offset - 20)
            end_ctx = min(len(self.elf.data), offset + len(raw_host) + 20)
            snippet = self.elf.data[start_ctx:end_ctx].decode("ascii", errors="replace")

            candidates.append(TelemetryCandidate(
                hostname=raw_host,
                offset=offset,
                section=sec,
                context_snippet=snippet,
                frequency=len(self.elf.find_string_occurrences(raw_host)),
                classification_reason="Matches telemetry domain heuristic in ELF",
            ))

        return candidates
