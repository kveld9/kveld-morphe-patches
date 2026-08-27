"""
ELF64 Parser and Native libchrome.so Binary Analysis Engine.
Analyzes ARM64 (AArch64) shared libraries for exact string locations, section bounds,
original bytes verification, and patch displacement calculations.
"""

from __future__ import annotations

import struct
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple


@dataclass
class ElfSection:
    name: str
    sh_type: int
    sh_flags: int
    sh_addr: int
    sh_offset: int
    sh_size: int
    sh_link: int
    sh_info: int
    sh_addralign: int
    sh_entsize: int


@dataclass
class ElfSegment:
    p_type: int
    p_flags: int
    p_offset: int
    p_vaddr: int
    p_paddr: int
    p_filesz: int
    p_memsz: int
    p_align: int


@dataclass
class NativeStringMatch:
    offset: int
    matched_string: str
    section_name: Optional[str]
    original_bytes: bytes
    replacement_bytes: bytes
    is_null_terminated: bool


class Elf64Analyzer:
    """Parser and validator for ELF64 (AArch64) binaries (libchrome.so)."""

    EM_AARCH64 = 183  # 0xB7

    def __init__(self, binary_path: str | Path):
        self.path = Path(binary_path).resolve()
        if not self.path.exists():
            raise FileNotFoundError(f"ELF binary not found: {self.path}")
        self.data: bytes = b""
        self.sections: List[ElfSection] = []
        self.sections_by_name: Dict[str, ElfSection] = {}
        self.segments: List[ElfSegment] = []
        self.is_valid = False
        self.is_aarch64 = False
        self._load()

    def _load(self):
        with open(self.path, "rb") as f:
            self.data = f.read()

        if not self._is_valid_elf64():
            return

        self.is_valid = True
        (
            e_type, e_machine, e_version, e_entry,
            e_phoff, e_shoff, e_flags, e_ehsize,
            e_phentsize, e_phnum, e_shentsize, e_shnum,
            e_shstrndx,
        ) = struct.unpack_from("<HHIQQQIHHHHHH", self.data, 16)

        self.is_aarch64 = (e_machine == self.EM_AARCH64)
        self._parse_segments(e_phoff, e_phnum, e_phentsize)
        self._parse_sections(e_shoff, e_shnum, e_shentsize, e_shstrndx)

    def _is_valid_elf64(self) -> bool:
        if len(self.data) < 64 or not self.data.startswith(b"\x7fELF"):
            return False
        ei_class = self.data[4]  # 2 = 64-bit
        ei_data = self.data[5]   # 1 = little endian
        return ei_class == 2 and ei_data == 1

    def _parse_segments(self, e_phoff: int, e_phnum: int, e_phentsize: int):
        for i in range(e_phnum):
            ph_offset = e_phoff + i * e_phentsize
            if ph_offset + 56 <= len(self.data):
                (
                    p_type, p_flags, p_offset, p_vaddr,
                    p_paddr, p_filesz, p_memsz, p_align,
                ) = struct.unpack_from("<IIQQQQQQ", self.data, ph_offset)
                self.segments.append(ElfSegment(
                    p_type=p_type,
                    p_flags=p_flags,
                    p_offset=p_offset,
                    p_vaddr=p_vaddr,
                    p_paddr=p_paddr,
                    p_filesz=p_filesz,
                    p_memsz=p_memsz,
                    p_align=p_align,
                ))

    def _parse_sections(self, e_shoff: int, e_shnum: int, e_shentsize: int, e_shstrndx: int):
        if e_shoff == 0 or e_shnum <= 0:
            return

        raw_sections = []
        for i in range(e_shnum):
            sh_offset = e_shoff + i * e_shentsize
            if sh_offset + 64 <= len(self.data):
                raw_sections.append(struct.unpack_from("<IIQQQQIIQQ", self.data, sh_offset))

        shstrtab = self._extract_shstrtab(raw_sections, e_shstrndx)
        for raw in raw_sections:
            name_idx = raw[0]
            sec_name = self._resolve_section_name(shstrtab, name_idx)
            sec = ElfSection(
                name=sec_name,
                sh_type=raw[1],
                sh_flags=raw[2],
                sh_addr=raw[3],
                sh_offset=raw[4],
                sh_size=raw[5],
                sh_link=raw[6],
                sh_info=raw[7],
                sh_addralign=raw[8],
                sh_entsize=raw[9],
            )
            self.sections.append(sec)
            if sec_name:
                self.sections_by_name[sec_name] = sec

    def _extract_shstrtab(self, raw_sections: List[Any], e_shstrndx: int) -> bytes:
        if e_shstrndx < len(raw_sections):
            _, _, _, _, strtab_off, strtab_sz, _, _, _, _ = raw_sections[e_shstrndx]
            if strtab_off + strtab_sz <= len(self.data):
                return self.data[strtab_off:strtab_off + strtab_sz]
        return b""

    @staticmethod
    def _resolve_section_name(shstrtab: bytes, name_idx: int) -> str:
        if name_idx < len(shstrtab):
            end = shstrtab.find(b"\x00", name_idx)
            if end != -1:
                return shstrtab[name_idx:end].decode("ascii", errors="replace")
        return ""

    def find_string_occurrences(self, target_str: str) -> List[int]:
        """Find all file offsets where the exact target ASCII string occurs."""
        needle = target_str.encode("ascii")
        offsets = []
        start = 0
        while True:
            pos = self.data.find(needle, start)
            if pos == -1:
                break
            offsets.append(pos)
            start = pos + 1
        return offsets

    def get_section_at_offset(self, offset: int) -> Optional[str]:
        """Find the section name covering a file offset."""
        for sec in self.sections:
            if sec.sh_offset <= offset < sec.sh_offset + sec.sh_size:
                return sec.name
        return None

    def analyze_host(self, host: str, expected_old_offset: Optional[int] = None) -> Tuple[str, List[NativeStringMatch]]:
        """Analyzes a host string in libchrome.so.

        Returns status ('EXACT_MATCH', 'OFFSET_CHANGED', 'MULTIPLE_MATCHES', 'NOT_FOUND') and matches.
        """
        offsets = self.find_string_occurrences(host)
        if not offsets:
            return "NOT_FOUND", []

        matches: List[NativeStringMatch] = []
        host_bytes = host.encode("ascii")
        len_host = len(host_bytes)

        # Standard redirection IP: "0.0.0.0" null-padded to len_host
        redirection_prefix = b"0.0.0.0"
        replacement = bytearray(len_host)
        replacement[:len(redirection_prefix)] = redirection_prefix
        # remaining bytes are \x00

        for off in offsets:
            sec_name = self.get_section_at_offset(off)
            orig = self.data[off:off + len_host]
            # Check if null-terminated
            is_null = (off + len_host < len(self.data) and self.data[off + len_host] == 0)
            matches.append(NativeStringMatch(
                offset=off,
                matched_string=host,
                section_name=sec_name,
                original_bytes=orig,
                replacement_bytes=bytes(replacement),
                is_null_terminated=is_null,
            ))

        if len(matches) == 1:
            if expected_old_offset is not None and matches[0].offset == expected_old_offset:
                return "EXACT_MATCH", matches
            return "OFFSET_CHANGED", matches
        else:
            return "MULTIPLE_MATCHES", matches
