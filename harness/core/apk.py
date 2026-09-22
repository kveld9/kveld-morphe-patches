"""
APK analysis and extraction utilities for the Brave Update Harness.
Manages safe temporary workspace lifecycle and metadata extraction.
"""

from __future__ import annotations

import hashlib
import io
import shutil
import tempfile
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional

try:
    from pyaxmlparser import APK as PyAXML_APK
except ImportError:
    PyAXML_APK = None


@dataclass
class ApkMetadata:
    package_name: str
    version_name: str
    version_code: int
    sha256: str
    file_size: int
    dex_files: List[str]
    has_arm64_libchrome: bool
    libchrome_abis: List[str] = None

    def __post_init__(self):
        if self.libchrome_abis is None:
            self.libchrome_abis = ["arm64-v8a"] if self.has_arm64_libchrome else []


class ApkContext:
    """Context manager that extracts necessary APK entries into a temporary directory
    and guarantees clean destruction on exit.
    """

    def __init__(self, apk_path: str | Path):
        self.apk_path = Path(apk_path).resolve()
        if not self.apk_path.exists():
            raise FileNotFoundError(f"APK file not found: {self.apk_path}")
        self.temp_dir: Optional[Path] = None
        self._metadata: Optional[ApkMetadata] = None

    def __enter__(self) -> ApkContext:
        self.temp_dir = Path(tempfile.mkdtemp(prefix="morphe_brave_harness_"))
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.temp_dir and self.temp_dir.exists():
            shutil.rmtree(self.temp_dir, ignore_errors=True)
            self.temp_dir = None

    def get_metadata(self) -> ApkMetadata:
        if self._metadata:
            return self._metadata

        sha256_digest = self._compute_sha256()
        dex_files, has_libchrome, libchrome_abis = self._inspect_zip_entries()
        pkg_name, ver_name, ver_code = self._parse_manifest_info()

        self._metadata = ApkMetadata(
            package_name=pkg_name,
            version_name=ver_name,
            version_code=ver_code,
            sha256=sha256_digest,
            file_size=self.apk_path.stat().st_size,
            dex_files=sorted(dex_files),
            has_arm64_libchrome=has_libchrome,
            libchrome_abis=libchrome_abis,
        )
        return self._metadata

    def _compute_sha256(self) -> str:
        hasher = hashlib.sha256()
        with open(self.apk_path, "rb") as f:
            while chunk := f.read(65536):
                hasher.update(chunk)
        return hasher.hexdigest()

    def _find_base_apk_name(self, zf: zipfile.ZipFile) -> Optional[str]:
        namelist = zf.namelist()
        if "base.apk" in namelist:
            return "base.apk"
        if "manifest.json" in namelist:
            try:
                import json
                m = json.loads(zf.read("manifest.json").decode("utf-8"))
                for split in m.get("split_apks", []):
                    if split.get("id") == "base":
                        return split.get("file")
            except Exception:
                pass
        apk_candidates = [n for n in namelist if n.endswith(".apk") and "/" not in n]
        if not apk_candidates:
            return None
        best_cand = None
        best_size = -1
        for cand in apk_candidates:
            info = zf.getinfo(cand)
            if info.file_size > best_size:
                best_size = info.file_size
                best_cand = cand
        return best_cand

    def _inspect_zip_entries(self) -> tuple[List[str], bool, List[str]]:
        dex_files: List[str] = []
        libchrome_abis: set[str] = set()
        arm_abis = {"arm64-v8a", "armeabi-v7a"}

        with zipfile.ZipFile(self.apk_path, "r") as zf:
            namelist = zf.namelist()
            inner_apks = sorted([n for n in namelist if n.endswith(".apk") and "/" not in n])
            if inner_apks:
                for inner in inner_apks:
                    with zipfile.ZipFile(io.BytesIO(zf.read(inner)), "r") as izf:
                        for name in izf.namelist():
                            if name.endswith(".dex") and ("classes" in name or "assets" in name):
                                dex_files.append(f"{inner}_{name}")
                            for abi in arm_abis:
                                if name == f"lib/{abi}/libchrome.so":
                                    libchrome_abis.add(abi)
            else:
                for name in namelist:
                    if name.endswith(".dex") and ("classes" in name or "assets" in name):
                        dex_files.append(name)
                    for abi in arm_abis:
                        if name == f"lib/{abi}/libchrome.so":
                            libchrome_abis.add(abi)

        sorted_abis = sorted(libchrome_abis)
        return dex_files, ("arm64-v8a" in sorted_abis), sorted_abis

    def _parse_manifest_info(self) -> tuple[str, str, int]:
        with zipfile.ZipFile(self.apk_path, "r") as zf:
            base_apk_name = self._find_base_apk_name(zf)
            if base_apk_name:
                if not self.temp_dir:
                    self.temp_dir = Path(tempfile.mkdtemp(prefix="morphe_brave_harness_"))
                base_path = self.temp_dir / base_apk_name
                with open(base_path, "wb") as f:
                    f.write(zf.read(base_apk_name))
                return self._parse_manifest_from_path(base_path)
        return self._parse_manifest_from_path(self.apk_path)

    def _parse_manifest_from_path(self, path: Path) -> tuple[str, str, int]:
        pkg_name, ver_name, ver_code = self._try_pyaxml_manifest(path)
        if not pkg_name or not ver_name:
            pkg_name, ver_name, ver_code = self._try_androguard_manifest(path)
        return pkg_name, ver_name, ver_code

    def _try_pyaxml_manifest(self, path: Path) -> tuple[str, str, int]:
        if PyAXML_APK is None:
            return "", "", 0
        try:
            apk_obj = PyAXML_APK(str(path))
            pkg_name = apk_obj.package or ""
            ver_name = apk_obj.version_name or ""
            ver_code = int(apk_obj.version_code) if apk_obj.version_code else 0
            return pkg_name, ver_name, ver_code
        except Exception:
            return "", "", 0

    def _try_androguard_manifest(self, path: Path) -> tuple[str, str, int]:
        try:
            from androguard.core.apk import APK as Androguard_APK
            apk_obj = Androguard_APK(str(path))
            pkg_name = apk_obj.get_package() or ""
            ver_name = apk_obj.get_androidversion_name() or ""
            ver_code = int(apk_obj.get_androidversion_code() or 0)
            return pkg_name, ver_name, ver_code
        except Exception:
            return "", "", 0

    def extract_dex_bytes(self) -> List[tuple[str, bytes]]:
        """Extract all DEX files as (name, bytes) in memory."""
        results = []
        with zipfile.ZipFile(self.apk_path, "r") as zf:
            namelist = zf.namelist()
            inner_apks = sorted([n for n in namelist if n.endswith(".apk") and "/" not in n])
            if inner_apks:
                for inner in inner_apks:
                    with zipfile.ZipFile(io.BytesIO(zf.read(inner)), "r") as izf:
                        for name in sorted(izf.namelist()):
                            if name.endswith(".dex") and not name.startswith("META-INF/"):
                                results.append((f"{inner}_{name}", izf.read(name)))
            else:
                for name in sorted(namelist):
                    if name.endswith(".dex") and not name.startswith("META-INF/"):
                        results.append((name, zf.read(name)))
        return results

    def extract_libchrome_path(
        self,
        preferred_abis: tuple[str, ...] = ("arm64-v8a", "armeabi-v7a"),
    ) -> Optional[Path]:
        """Extract libchrome.so for the first available preferred ARM ABI."""
        if not self.temp_dir:
            raise RuntimeError("ApkContext must be entered via 'with' before extracting files.")

        target_path = self.temp_dir / "libchrome.so"

        def extract_from_zip(zf: zipfile.ZipFile) -> Optional[Path]:
            for abi in preferred_abis:
                name = f"lib/{abi}/libchrome.so"
                if name in zf.namelist():
                    with zf.open(name) as src, open(target_path, "wb") as dst:
                        shutil.copyfileobj(src, dst)
                    return target_path
            return None

        with zipfile.ZipFile(self.apk_path, "r") as zf:
            namelist = zf.namelist()
            inner_apks = sorted([n for n in namelist if n.endswith(".apk") and "/" not in n])
            if inner_apks:
                for inner in inner_apks:
                    try:
                        with zipfile.ZipFile(io.BytesIO(zf.read(inner)), "r") as izf:
                            extracted = extract_from_zip(izf)
                            if extracted:
                                return extracted
                    except zipfile.BadZipFile:
                        continue
                return None

            return extract_from_zip(zf)

    def get_all_entry_names(self) -> List[str]:
        """Returns all entry names contained within the APK or split APK bundle."""
        entries = []
        with zipfile.ZipFile(self.apk_path, "r") as zf:
            namelist = zf.namelist()
            entries.extend(namelist)
            inner_apks = sorted([n for n in namelist if n.endswith(".apk") and "/" not in n])
            if inner_apks:
                for inner in inner_apks:
                    try:
                        with zipfile.ZipFile(io.BytesIO(zf.read(inner)), "r") as izf:
                            entries.extend(izf.namelist())
                    except Exception:
                        pass
        return entries
