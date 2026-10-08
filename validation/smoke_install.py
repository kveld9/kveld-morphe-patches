#!/usr/bin/env python3
"""
Automated ADB installation and smoke launch validation gate.
Selects compatible connected Android devices by ABI and minimum SDK,
safely installs single or split APKs, guards active IMEs, launches
the main activity, and scans privacy-scoped crash logs.
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import re
import subprocess
import sys
import time
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any

try:
    from loguru import logger as loguru_logger
    loguru_logger.disable("androguard")
except Exception:
    pass

logging.getLogger("androguard").setLevel(logging.ERROR)


INSTALL_ERROR_MAPPING: dict[str, str] = {
    "INSTALL_FAILED_UPDATE_INCOMPATIBLE": "SIGNATURE_CONFLICT",
    "INSTALL_FAILED_VERSION_DOWNGRADE": "INSTALL_FAILED",
    "INSTALL_FAILED_INSUFFICIENT_STORAGE": "INSTALL_FAILED",
    "INSTALL_FAILED_INVALID_APK": "INSTALL_FAILED",
}

VERDICT_EXIT_CODES: dict[str, int] = {
    "PASS": 0,
    "PASS_NO_LAUNCHER": 0,
    "FAIL": 1,
    "SKIPPED_ACTIVE_IME": 1,
    "SIGNATURE_CONFLICT": 1,
    "INSTALL_FAILED": 1,
}


@dataclass
class ApkInfo:
    package: str | None
    version_name: str | None
    version_code: int | None
    min_sdk: int | None


@dataclass
class InputMetadata:
    package: str
    version_name: str | None
    version_code: int | None
    min_sdk: int | None
    required_abis: set[str]


@dataclass
class DeviceInfo:
    serial: str
    abilist: list[str]
    sdk: int
    model: str


@dataclass
class InstallResult:
    success: bool
    install_command: str
    uninstalled: bool
    installed_version: str | None
    verdict: str
    reason: str | None


@dataclass
class LaunchOutcome:
    launched: bool
    verdict: str | None
    pid: str | None


@dataclass
class CrashReport:
    fatal_count: int
    fatal_excerpt: list[str]


def log_info(msg: str) -> None:
    sys.stderr.write(f"[INFO] {msg}\n")
    sys.stderr.flush()


def log_warn(msg: str) -> None:
    sys.stderr.write(f"[WARN] {msg}\n")
    sys.stderr.flush()


def log_fail(msg: str) -> None:
    sys.stderr.write(f"[FAIL] {msg}\n")
    sys.stderr.flush()


def run_cmd(cmd: list[str], timeout: int = 60) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        cmd,
        capture_output=True,
        text=True,
        timeout=timeout,
        check=False,
    )


def _abi_from_zip_entry(name: str) -> str | None:
    if not name.startswith("lib/"):
        return None
    parts = name.split("/")
    if len(parts) >= 3 and parts[1]:
        return parts[1]
    return None


def extract_zip_abis(apk_path: Path) -> set[str]:
    abis: set[str] = set()
    try:
        with zipfile.ZipFile(apk_path, "r") as zf:
            for name in zf.namelist():
                abi = _abi_from_zip_entry(name)
                if abi:
                    abis.add(abi)
    except Exception as exc:
        log_fail(f"Invalid zip/APK file '{apk_path}': {exc}")
        sys.exit(2)
    return abis


def _parse_int_or_none(val: Any) -> int | None:
    try:
        return int(val)
    except (TypeError, ValueError):
        return None


def inspect_single_apk(apk_path: Path) -> ApkInfo:
    try:
        from androguard.core.apk import APK
        apk_obj = APK(str(apk_path))
        if not apk_obj.is_valid_APK():
            return ApkInfo(None, None, None, None)
        pkg = apk_obj.get_package()
        ver_name = apk_obj.get_androidversion_name()
        ver_code = _parse_int_or_none(apk_obj.get_androidversion_code())
        min_sdk = _parse_int_or_none(apk_obj.get_min_sdk_version())
        return ApkInfo(pkg, ver_name, ver_code, min_sdk)
    except Exception as exc:
        log_fail(f"Failed to inspect APK '{apk_path}': {exc}")
        sys.exit(2)


def _merge_apk_metadata(info: ApkInfo, current: InputMetadata) -> None:
    if current.version_name is None:
        current.version_name = info.version_name
    if current.version_code is None:
        current.version_code = info.version_code
    if current.min_sdk is None:
        current.min_sdk = info.min_sdk


def _assert_file_exists(p: Path) -> None:
    if not p.is_file():
        log_fail(f"APK file not found: {p}")
        sys.exit(2)


def _validate_packages(packages: set[str]) -> str:
    if not packages:
        log_fail("No valid package name found in input APKs.")
        sys.exit(2)
    if len(packages) > 1:
        log_fail(f"Package mismatch across input APKs: {sorted(packages)}")
        sys.exit(2)
    return next(iter(packages))


def inspect_inputs(apk_paths: list[Path]) -> InputMetadata:
    packages: set[str] = set()
    metadata = InputMetadata("", None, None, None, set())

    for p in apk_paths:
        _assert_file_exists(p)
        metadata.required_abis.update(extract_zip_abis(p))
        info = inspect_single_apk(p)
        if info.package:
            packages.add(info.package)
        _merge_apk_metadata(info, metadata)

    metadata.package = _validate_packages(packages)
    return metadata


def _query_device_prop(serial: str, prop: str) -> str:
    res = run_cmd(["adb", "-s", serial, "shell", "getprop", prop], timeout=60)
    return res.stdout.strip()


def inspect_device(serial: str) -> DeviceInfo:
    abilist_raw = _query_device_prop(serial, "ro.product.cpu.abilist")
    abilist = [a.strip() for a in abilist_raw.split(",") if a.strip()]
    sdk_str = _query_device_prop(serial, "ro.build.version.sdk")
    sdk = _parse_int_or_none(sdk_str) or 0
    model = _query_device_prop(serial, "ro.product.model") or "unknown"
    return DeviceInfo(serial=serial, abilist=abilist, sdk=sdk, model=model)


def list_connected_serials() -> list[str]:
    res = run_cmd(["adb", "devices"], timeout=60)
    serials: list[str] = []
    for line in res.stdout.splitlines():
        parts = line.strip().split()
        if len(parts) >= 2 and parts[1] == "device":
            serials.append(parts[0])
    return serials


def discover_devices() -> list[DeviceInfo]:
    serials = list_connected_serials()
    return [inspect_device(s) for s in serials]


def is_device_compatible(device: DeviceInfo, required_abis: set[str], min_sdk: int | None) -> bool:
    abi_ok = not required_abis or bool(set(device.abilist) & required_abis)
    sdk_ok = min_sdk is None or device.sdk >= min_sdk
    return abi_ok and sdk_ok


def _select_explicit_device(
    target_serial: str,
    devices: list[DeviceInfo],
    required_abis: set[str],
    min_sdk: int | None,
) -> DeviceInfo:
    dev_map = {d.serial: d for d in devices}
    device = dev_map.get(target_serial)
    if device is None:
        log_fail(f"Specified device '{target_serial}' not found in active devices: {list(dev_map.keys())}")
        sys.exit(2)
    if not is_device_compatible(device, required_abis, min_sdk):
        log_fail(
            f"Device '{target_serial}' is incompatible. Device abilist={device.abilist}, sdk={device.sdk}; "
            f"Required ABIs={sorted(required_abis)}, min_sdk={min_sdk}"
        )
        sys.exit(2)
    log_info(f"Selected explicit device '{device.serial}' ({device.model})")
    return device


def _device_sort_key(d: DeviceInfo, required_abis: set[str]) -> tuple[bool, int, str]:
    primary_abi = d.abilist[0] if d.abilist else ""
    primary_match = (primary_abi in required_abis) if required_abis else True
    return (not primary_match, -d.sdk, d.serial)


def _select_automatic_device(
    devices: list[DeviceInfo],
    required_abis: set[str],
    min_sdk: int | None,
) -> DeviceInfo:
    compatible = [d for d in devices if is_device_compatible(d, required_abis, min_sdk)]
    if not compatible:
        log_fail(
            f"No compatible device found for required ABIs {sorted(required_abis)} and min SDK {min_sdk}."
        )
        for d in devices:
            log_fail(f"  - {d.serial} ({d.model}): abilist={d.abilist}, sdk={d.sdk}")
        sys.exit(2)
    compatible.sort(key=lambda d: _device_sort_key(d, required_abis))
    chosen = compatible[0]
    log_info(
        f"Selected device '{chosen.serial}' ({chosen.model}): "
        f"primary ABI='{chosen.abilist[0] if chosen.abilist else 'none'}', SDK={chosen.sdk}"
    )
    return chosen


def select_device(
    devices: list[DeviceInfo],
    explicit_serial: str | None,
    required_abis: set[str],
    min_sdk: int | None,
) -> DeviceInfo:
    target_serial = explicit_serial or os.environ.get("ANDROID_SERIAL")
    if target_serial:
        return _select_explicit_device(target_serial, devices, required_abis, min_sdk)
    return _select_automatic_device(devices, required_abis, min_sdk)


def check_active_ime(serial: str, package: str) -> bool:
    res = run_cmd(["adb", "-s", serial, "shell", "settings", "get", "secure", "default_input_method"], timeout=60)
    current_ime = res.stdout.strip()
    return current_ime.startswith(f"{package}/")


def extract_install_error_token(output: str) -> str:
    match = re.search(r"(INSTALL_FAILED_[A-Z0-9_]+|Failure\s*\[[^\]]+\])", output)
    if match:
        return match.group(1).strip()
    return "INSTALL_FAILED"


def _map_install_failure_verdict(token: str) -> str:
    if "INSTALL_FAILED_UPDATE_INCOMPATIBLE" in token:
        return "SIGNATURE_CONFLICT"
    return INSTALL_ERROR_MAPPING.get(token, "INSTALL_FAILED")


def _execute_adb_install(serial: str, apk_paths: list[Path]) -> tuple[str, str]:
    if len(apk_paths) == 1:
        cmd = ["adb", "-s", serial, "install", "-r", str(apk_paths[0])]
        cmd_type = "install"
    else:
        cmd = ["adb", "-s", serial, "install-multiple", "-r"] + [str(p) for p in apk_paths]
        cmd_type = "install-multiple"
    res = run_cmd(cmd, timeout=600)
    combined_output = f"{res.stdout}\n{res.stderr}".strip()
    return cmd_type, combined_output


def _handle_uninstall_retry(serial: str, package: str, apk_paths: list[Path]) -> tuple[bool, str]:
    log_info(f"Signature conflict detected. Uninstalling '{package}' on '{serial}'...")
    run_cmd(["adb", "-s", serial, "uninstall", package], timeout=60)
    _, retry_output = _execute_adb_install(serial, apk_paths)
    return True, retry_output


def is_install_success(output: str) -> bool:
    return "Success" in output and "Failure" not in output and "INSTALL_FAILED" not in output


def query_installed_version_name(serial: str, package: str) -> str | None:
    res = run_cmd(["adb", "-s", serial, "shell", "dumpsys", "package", package], timeout=60)
    for line in res.stdout.splitlines():
        idx = line.find("versionName=")
        if idx != -1:
            raw_val = line[idx + len("versionName="):].strip()
            return raw_val.split()[0] if raw_val else None
    return None


def _verify_installed_version(input_ver: str | None, installed_ver: str | None) -> tuple[str, str | None]:
    if not input_ver or not installed_ver:
        return "PASS", None
    if installed_ver != input_ver:
        log_fail(f"Version mismatch: input '{input_ver}' vs installed '{installed_ver}'")
        return "FAIL", "VERSION_MISMATCH"
    return "PASS", None


def run_install(
    serial: str,
    package: str,
    apk_paths: list[Path],
    uninstall_on_conflict: bool,
    input_version: str | None,
) -> InstallResult:
    cmd_type, output = _execute_adb_install(serial, apk_paths)
    uninstalled = False

    if "INSTALL_FAILED_UPDATE_INCOMPATIBLE" in output and uninstall_on_conflict:
        uninstalled, output = _handle_uninstall_retry(serial, package, apk_paths)

    if not is_install_success(output):
        token = extract_install_error_token(output)
        verdict = _map_install_failure_verdict(token)
        return InstallResult(False, cmd_type, uninstalled, None, verdict, token)

    installed_ver = query_installed_version_name(serial, package)
    verdict, reason = _verify_installed_version(input_version, installed_ver)
    return InstallResult(True, cmd_type, uninstalled, installed_ver, verdict, reason)


def record_device_epoch(serial: str) -> str:
    res = run_cmd(["adb", "-s", serial, "shell", "date", "+%s"], timeout=60)
    epoch = res.stdout.strip()
    if epoch.isdigit():
        return epoch
    return str(int(time.time()))


def query_package_pid(serial: str, package: str) -> str | None:
    res = run_cmd(["adb", "-s", serial, "shell", "pidof", package], timeout=60)
    pid_str = res.stdout.strip()
    return pid_str if pid_str else None


def execute_app_launch(serial: str, package: str, wait_seconds: float) -> LaunchOutcome:
    run_cmd(["adb", "-s", serial, "shell", "am", "force-stop", package], timeout=60)
    monkey_res = run_cmd(
        ["adb", "-s", serial, "shell", "monkey", "-p", package, "-c", "android.intent.category.LAUNCHER", "1"],
        timeout=60,
    )
    if "No activities found to run" in monkey_res.stdout:
        return LaunchOutcome(launched=False, verdict="PASS_NO_LAUNCHER", pid=None)

    time.sleep(wait_seconds)
    pid = query_package_pid(serial, package)
    return LaunchOutcome(launched=True, verdict=None, pid=pid)


def _split_into_blocks(lines: list[str]) -> list[list[str]]:
    indices = [i for i, line in enumerate(lines) if "FATAL EXCEPTION" in line]
    if not indices:
        return []
    bounds = list(zip(indices, indices[1:] + [len(lines)]))
    return [lines[start:end] for start, end in bounds]


def _package_process_pattern(package: str) -> re.Pattern[str]:
    return re.compile(rf"{re.escape(package)}(?::[^\s,]*)?(?![\w.])")


def _filter_matching_blocks(blocks: list[list[str]], package: str) -> list[list[str]]:
    pattern = _package_process_pattern(package)
    matched: list[list[str]] = []
    for b in blocks:
        for line in b:
            _, sep, after = line.partition("Process: ")
            if sep and pattern.search(after):
                matched.append(b)
                break
    return matched


def _scan_main_fatal_lines(serial: str, epoch: str, package: str) -> list[str]:
    res = run_cmd(
        ["adb", "-s", serial, "logcat", "-d", "-b", "main", "-T", f"{epoch}.000", "*:F"],
        timeout=60,
    )
    pattern = _package_process_pattern(package)
    return [line for line in res.stdout.splitlines() if pattern.search(line)]


def scan_device_crashes(serial: str, epoch: str, package: str) -> CrashReport:
    crash_res = run_cmd(
        ["adb", "-s", serial, "logcat", "-d", "-b", "crash", "-T", f"{epoch}.000"],
        timeout=60,
    )
    raw_blocks = _split_into_blocks(crash_res.stdout.splitlines())
    matched_blocks = _filter_matching_blocks(raw_blocks, package)
    main_fatals = _scan_main_fatal_lines(serial, epoch, package)

    kept_lines: list[str] = []
    for b in matched_blocks:
        kept_lines.extend(b)
    kept_lines.extend(main_fatals)

    fatal_count = len(matched_blocks) + len(main_fatals)
    return CrashReport(fatal_count=fatal_count, fatal_excerpt=kept_lines[:20])


def determine_launch_verdict(pid: str | None, fatal_count: int) -> tuple[str, str | None]:
    if fatal_count > 0:
        return "FAIL", "FATAL"
    if not pid:
        return "FAIL", "NO_PROCESS"
    return "PASS", None


def _build_result_dict(
    verdict: str,
    reason: str | None,
    device: DeviceInfo,
    meta: InputMetadata,
    install_command: str,
    uninstalled: bool,
    installed_version: str | None,
    launched: bool,
    pid: str | None,
    fatal_count: int,
    fatal_excerpt: list[str],
) -> dict[str, Any]:
    return {
        "device_abilist": device.abilist,
        "device_sdk": device.sdk,
        "fatal_count": fatal_count,
        "fatal_excerpt": fatal_excerpt,
        "input_version": meta.version_name,
        "install_command": install_command,
        "installed_version": installed_version,
        "launched": launched,
        "model": device.model,
        "package": meta.package,
        "pid": pid,
        "reason": reason,
        "required_abis": sorted(meta.required_abis),
        "serial": device.serial,
        "uninstalled": uninstalled,
        "verdict": verdict,
    }


def _emit_result_and_exit(result: dict[str, Any]) -> None:
    print(json.dumps(result, sort_keys=True))
    verdict = result.get("verdict", "FAIL")
    exit_code = VERDICT_EXIT_CODES.get(verdict, 1)
    sys.exit(exit_code)


def _guard_active_ime(device: DeviceInfo, meta: InputMetadata, allow_active_ime: bool, count: int) -> None:
    if allow_active_ime:
        return
    if check_active_ime(device.serial, meta.package):
        log_warn(f"Package '{meta.package}' is active IME. Refusing install without --allow-active-ime.")
        cmd_type = "install" if count == 1 else "install-multiple"
        res = _build_result_dict(
            verdict="SKIPPED_ACTIVE_IME",
            reason="ACTIVE_IME",
            device=device,
            meta=meta,
            install_command=cmd_type,
            uninstalled=False,
            installed_version=None,
            launched=False,
            pid=None,
            fatal_count=0,
            fatal_excerpt=[],
        )
        _emit_result_and_exit(res)


def _guard_install_result(device: DeviceInfo, meta: InputMetadata, install_res: InstallResult) -> None:
    if install_res.success and install_res.verdict == "PASS":
        return
    res = _build_result_dict(
        verdict=install_res.verdict,
        reason=install_res.reason,
        device=device,
        meta=meta,
        install_command=install_res.install_command,
        uninstalled=install_res.uninstalled,
        installed_version=install_res.installed_version,
        launched=False,
        pid=None,
        fatal_count=0,
        fatal_excerpt=[],
    )
    _emit_result_and_exit(res)


def _handle_no_launch(device: DeviceInfo, meta: InputMetadata, install_res: InstallResult) -> None:
    res = _build_result_dict(
        verdict="PASS",
        reason=None,
        device=device,
        meta=meta,
        install_command=install_res.install_command,
        uninstalled=install_res.uninstalled,
        installed_version=install_res.installed_version,
        launched=False,
        pid=None,
        fatal_count=0,
        fatal_excerpt=[],
    )
    _emit_result_and_exit(res)


def _execute_and_audit_launch(
    device: DeviceInfo,
    meta: InputMetadata,
    install_res: InstallResult,
    wait_seconds: float,
) -> None:
    epoch = record_device_epoch(device.serial)
    outcome = execute_app_launch(device.serial, meta.package, wait_seconds)

    if outcome.verdict == "PASS_NO_LAUNCHER":
        res = _build_result_dict(
            verdict="PASS_NO_LAUNCHER",
            reason=None,
            device=device,
            meta=meta,
            install_command=install_res.install_command,
            uninstalled=install_res.uninstalled,
            installed_version=install_res.installed_version,
            launched=False,
            pid=None,
            fatal_count=0,
            fatal_excerpt=[],
        )
        _emit_result_and_exit(res)

    crash_report = scan_device_crashes(device.serial, epoch, meta.package)
    verdict, reason = determine_launch_verdict(outcome.pid, crash_report.fatal_count)
    res = _build_result_dict(
        verdict=verdict,
        reason=reason,
        device=device,
        meta=meta,
        install_command=install_res.install_command,
        uninstalled=install_res.uninstalled,
        installed_version=install_res.installed_version,
        launched=True,
        pid=outcome.pid,
        fatal_count=crash_report.fatal_count,
        fatal_excerpt=crash_report.fatal_excerpt,
    )
    _emit_result_and_exit(res)


def _run_launch_and_verdict(
    device: DeviceInfo,
    meta: InputMetadata,
    install_res: InstallResult,
    wait_seconds: float,
    no_launch: bool,
) -> None:
    if no_launch:
        _handle_no_launch(device, meta, install_res)
        return
    _execute_and_audit_launch(device, meta, install_res, wait_seconds)


def run_smoke_test(args: argparse.Namespace) -> None:
    apk_paths = [Path(p) for p in args.apks]
    meta = inspect_inputs(apk_paths)
    devices = discover_devices()
    device = select_device(devices, args.serial, meta.required_abis, meta.min_sdk)

    _guard_active_ime(device, meta, args.allow_active_ime, len(apk_paths))
    install_res = run_install(
        device.serial,
        meta.package,
        apk_paths,
        args.uninstall_on_conflict,
        meta.version_name,
    )
    _guard_install_result(device, meta, install_res)
    _run_launch_and_verdict(device, meta, install_res, args.wait, args.no_launch)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Install and smoke-test patched APK(s) on a connected Android device."
    )
    parser.add_argument("apks", nargs="+", help="Path(s) to APK file(s)")
    parser.add_argument("--serial", default=None, help="Specific target device ADB serial")
    parser.add_argument("--wait", type=float, default=8.0, help="Seconds to wait after launch before pid check")
    parser.add_argument("--uninstall-on-conflict", action="store_true", help="Uninstall existing app if signature mismatch occurs")
    parser.add_argument("--allow-active-ime", action="store_true", help="Allow install even if app is the currently active IME")
    parser.add_argument("--no-launch", action="store_true", help="Skip launching the app after installation")
    return parser.parse_args(argv)


def main() -> None:
    args = parse_args()
    run_smoke_test(args)


if __name__ == "__main__":
    main()
