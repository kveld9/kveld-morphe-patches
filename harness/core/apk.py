"""
APK analysis and extraction utilities for the Brave Update Harness.
Manages safe temporary workspace lifecycle and metadata extraction.
"""

from __future__ import annotations

import hashlib
import os
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

        # 1. Compute SHA-256
        hasher = hashlib.sha256()
        with open(self.apk_path, "rb") as f:
            while chunk := f.read(65536):
                hasher.update(chunk)
        sha256_digest = hasher.hexdigest()

        # 2. Inspect ZIP entries
        dex_files: List[str] = []
        has_libchrome = False
        with zipfile.ZipFile(self.apk_path, "r") as zf:
            for name in zf.namelist():
                if name.endswith(".dex") and ("classes" in name or "assets" in name):
                    dex_files.append(name)
                if name == "lib/arm64-v8a/libchrome.so":
                    has_libchrome = True

        # 3. Parse manifest
        pkg_name = ""
        ver_name = ""
        ver_code = 0

        if PyAXML_APK is not None:
            try:
                apk_obj = PyAXML_APK(str(self.apk_path))
                pkg_name = apk_obj.package or ""
                ver_name = apk_obj.version_name or ""
                ver_code = int(apk_obj.version_code) if apk_obj.version_code else 0
            except Exception:
                pass

        if not pkg_name or not ver_name:
            # Fallback to direct zip extraction of manifest
            try:
                from androguard.core.apk import APK as Androguard_APK
                apk_obj = Androguard_APK(str(self.apk_path))
                pkg_name = apk_obj.get_package() or ""
                ver_name = apk_obj.get_androidversion_name() or ""
                ver_code = int(apk_obj.get_androidversion_code() or 0)
            except Exception:
                pass

        self._metadata = ApkMetadata(
            package_name=pkg_name,
            version_name=ver_name,
            version_code=ver_code,
            sha256=sha256_digest,
            file_size=self.apk_path.stat().st_size,
            dex_files=sorted(dex_files),
            has_arm64_libchrome=has_libchrome,
        )
        return self._metadata

    def extract_dex_bytes(self) -> List[tuple[str, bytes]]:
        """Extract all DEX files as (name, bytes) in memory."""
        results = []
        with zipfile.ZipFile(self.apk_path, "r") as zf:
            for name in sorted(zf.namelist()):
                if name.endswith(".dex") and not name.startswith("META-INF/"):
                    results.append((name, zf.read(name)))
        return results

    def extract_libchrome_path(self) -> Optional[Path]:
        """Extract lib/arm64-v8a/libchrome.so to the temporary workspace directory."""
        if not self.temp_dir:
            raise RuntimeError("ApkContext must be entered via 'with' before extracting files.")
        target_path = self.temp_dir / "libchrome.so"
        with zipfile.ZipFile(self.apk_path, "r") as zf:
            if "lib/arm64-v8a/libchrome.so" not in zf.namelist():
                return None
            with zf.open("lib/arm64-v8a/libchrome.so") as src, open(target_path, "wb") as dst:
                shutil.copyfileobj(src, dst)
        return target_path
