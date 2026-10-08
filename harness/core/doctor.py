"""
Preflight readiness checks for the Morphe Patches harness.

Fails fast (<2s) on missing toolchain pieces before a multi-minute
DEX indexing run, and classifies the failure as ENVIRONMENT instead
of letting it surface mid-pipeline as a confusing audit error.
"""

from __future__ import annotations

import shutil
import subprocess
import sys
from dataclasses import dataclass
from importlib.util import find_spec
from pathlib import Path
from typing import List, Tuple

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
REQUIRED_PYTHON = (3, 10)
CONSTANTS_REL = Path("patches/src/main/kotlin/app/morphe/patches/shared/Constants.kt")


@dataclass
class DoctorCheck:
    check_id: str
    label: str
    status: str  # "PASS", "WARN", "FAIL"
    detail: str


def _run_version(cmd: List[str]) -> str:
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=15)
        out = (proc.stdout or "") + (proc.stderr or "")
        first = next((ln.strip() for ln in out.splitlines() if ln.strip()), "")
        return first[:120] if first else "present (no version line)"
    except Exception as e:
        return f"not runnable: {e}"


def run_doctor(repo_root: Path = REPO_ROOT) -> Tuple[List[DoctorCheck], bool]:
    results: List[DoctorCheck] = []

    if sys.version_info >= REQUIRED_PYTHON:
        results.append(DoctorCheck("python", "Python interpreter", "PASS",
                                   f"{sys.version.split()[0]} (requires >=3.10)"))
    else:
        results.append(DoctorCheck("python", "Python interpreter", "FAIL",
                                   f"{sys.version.split()[0]} (requires >=3.10)"))

    try:
        from importlib.metadata import version
        ag_ver = version("androguard")
        results.append(DoctorCheck("androguard", "Androguard DEX engine", "PASS", ag_ver))
    except Exception:
        if find_spec("androguard") is not None:
            results.append(DoctorCheck("androguard", "Androguard DEX engine", "WARN",
                                       "importable but version unknown"))
        else:
            results.append(DoctorCheck("androguard", "Androguard DEX engine", "FAIL",
                                       "not installed (see requirements.txt)"))

    for pkg in ("lxml", "yaml", "cryptography"):
        if find_spec(pkg) is not None:
            results.append(DoctorCheck(f"pylib:{pkg}", f"Python lib {pkg}", "PASS", "importable"))
        else:
            results.append(DoctorCheck(f"pylib:{pkg}", f"Python lib {pkg}", "WARN",
                                       "missing (only needed by some pipelines)"))

    if shutil.which("java"):
        results.append(DoctorCheck("java", "Java runtime (Gradle)", "PASS", _run_version(["java", "-version"])))
    else:
        results.append(DoctorCheck("java", "Java runtime (Gradle)", "FAIL",
                                   "no 'java' on PATH (Gradle build/verification needs a JVM)"))

    gradlew = repo_root / "gradlew"
    if gradlew.is_file():
        results.append(DoctorCheck("gradlew", "Gradle wrapper", "PASS", str(gradlew)))
    else:
        results.append(DoctorCheck("gradlew", "Gradle wrapper", "FAIL",
                                   f"missing wrapper at {gradlew}"))

    constants = repo_root / CONSTANTS_REL
    if constants.is_file():
        results.append(DoctorCheck("constants", "Compatibility contract (Constants.kt)", "PASS", str(constants)))
    else:
        results.append(DoctorCheck("constants", "Compatibility contract (Constants.kt)", "FAIL",
                                   f"missing at {constants}"))

    if shutil.which("adb"):
        results.append(DoctorCheck("adb", "ADB (physical-device validation)", "PASS",
                                   _run_version(["adb", "version"])))
    else:
        results.append(DoctorCheck("adb", "ADB (physical-device validation)", "WARN",
                                   "not on PATH (only needed for on-device smoke tests)"))

    for tool, label, version_cmd in (
        ("jadx", "jadx decompiler (optional BLOCKED triage)", ["jadx", "--version"]),
        ("frida", "Frida dynamic instrumentation (optional BLOCKED triage)", ["frida", "--version"]),
    ):
        if shutil.which(tool):
            results.append(DoctorCheck(f"opt:{tool}", label, "PASS", _run_version(version_cmd)))
        else:
            results.append(DoctorCheck(f"opt:{tool}", label, "WARN",
                                       f"not on PATH (manual triage aid only, never a pipeline dependency)"))

    ok = all(r.status != "FAIL" for r in results)
    return results, ok


def render_doctor(results: List[DoctorCheck], ok: bool) -> str:
    lines = ["[DOCTOR] Harness readiness:"]
    for r in results:
        lines.append(f"[{r.status}] {r.label}: {r.detail}")
    lines.append(f"[DOCTOR] Verdict: {'READY' if ok else 'NOT READY (environment failure)'}")
    return "\n".join(lines)


def main() -> int:
    results, ok = run_doctor()
    print(render_doctor(results, ok))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
