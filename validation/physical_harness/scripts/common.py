"""
Common ADB, Device and Environment utilities for Physical ARM64 Test Harness.
"""

from __future__ import annotations

import http.server
import os
import socketserver
import subprocess
import threading
import time
from pathlib import Path
from typing import Dict, List, Optional, Tuple

PACKAGE_NAME = "com.brave.browser"
MAIN_ACTIVITY = "com.brave.browser/com.google.android.apps.chrome.Main"
SERVER_PORT = 8080

class AdbDevice:
    def __init__(self, serial: Optional[str] = "df286add"):
        self.serial = serial
        self._ensure_device()

    def _ensure_device(self):
        devices = [d[0] for d in self.list_devices()]
        if not devices:
            raise RuntimeError("No ADB devices connected. Please connect your ARM64 Android device with USB debugging enabled.")
        if self.serial and self.serial not in devices:
            raise RuntimeError(f"Target device '{self.serial}' not found in connected devices: {devices}")
        if not self.serial:
            self.serial = devices[0]

    @staticmethod
    def list_devices() -> List[Tuple[str, str]]:
        res = subprocess.run(["adb", "devices"], capture_output=True, text=True, check=True)
        lines = res.stdout.strip().splitlines()[1:]
        devices = []
        for line in lines:
            parts = line.split()
            if len(parts) >= 2:
                devices.append((parts[0], parts[1]))
        return devices

    def cmd(self, args: List[str], check: bool = True) -> subprocess.CompletedProcess:
        full_cmd = ["adb"]
        if self.serial:
            full_cmd.extend(["-s", self.serial])
        full_cmd.extend(args)
        return subprocess.run(full_cmd, capture_output=True, text=True, check=check)

    def shell(self, command: str) -> str:
        res = self.cmd(["shell", command], check=False)
        return res.stdout

    def get_abi(self) -> str:
        return self.shell("getprop ro.product.cpu.abi").strip()

    def get_sdk_version(self) -> int:
        val = self.shell("getprop ro.build.version.sdk").strip()
        return int(val) if val.isdigit() else 0

    def get_model(self) -> str:
        return self.shell("getprop ro.product.model").strip()

    def is_process_running(self) -> bool:
        pid = self.shell(f"pidof {PACKAGE_NAME}").strip()
        return len(pid) > 0

    def get_pid(self) -> Optional[str]:
        pid = self.shell(f"pidof {PACKAGE_NAME}").strip()
        return pid.split()[0] if pid else None

    def force_stop(self):
        self.shell(f"am force-stop {PACKAGE_NAME}")
        time.sleep(0.5)

    def clear_data(self):
        self.shell(f"pm clear {PACKAGE_NAME}")
        time.sleep(0.5)

    def start_app(self, url: Optional[str] = None):
        if url:
            self.shell(f"am start -S -W -a android.intent.action.VIEW -d \"{url}\" -n {MAIN_ACTIVITY}")
        else:
            self.shell(f"am start -S -W -n {MAIN_ACTIVITY}")
        time.sleep(1.0)

    def press_key(self, keycode: str):
        self.shell(f"input keyevent {keycode}")
        time.sleep(0.3)

    def swipe(self, x1: int, y1: int, x2: int, y2: int, duration_ms: int = 300):
        self.shell(f"input swipe {x1} {y1} {x2} {y2} {duration_ms}")
        time.sleep(0.3)

    def setup_reverse_port(self, port: int = SERVER_PORT):
        self.cmd(["reverse", f"tcp:{port}", f"tcp:{port}"], check=False)

    def get_dumpsys_power(self) -> str:
        return self.shell("dumpsys power")

    def get_dumpsys_jobscheduler(self) -> str:
        return self.shell(f"dumpsys jobscheduler {PACKAGE_NAME}")

    def get_dumpsys_battery(self) -> str:
        return self.shell("dumpsys battery")

    def get_dumpsys_alarms(self) -> str:
        return self.shell(f"dumpsys alarm | grep -E '{PACKAGE_NAME}'")

    def set_battery_level(self, level: int):
        self.shell(f"dumpsys battery set level {level}")

    def set_battery_status(self, status_code: int):
        # 2: Charging, 3: Discharging, 5: Full
        self.shell(f"dumpsys battery set status {status_code}")

    def reset_battery(self):
        self.shell("dumpsys battery reset")

    def clear_logcat(self):
        self.cmd(["logcat", "-c"], check=False)

    def dump_logcat(self, filter_tag: Optional[str] = None) -> str:
        if filter_tag:
            return self.cmd(["logcat", "-d", "-s", filter_tag], check=False).stdout
        return self.cmd(["logcat", "-d"], check=False).stdout


class LocalTestServer:
    """Spawns a local HTTP server serving validation pages and manages port forwarding."""
    def __init__(self, directory: Path, port: int = SERVER_PORT):
        self.directory = directory
        self.port = port
        self.server: Optional[socketserver.TCPServer] = None
        self.thread: Optional[threading.Thread] = None

    def start(self):
        os.chdir(self.directory)
        handler = http.server.SimpleHTTPRequestHandler
        self.server = socketserver.TCPServer(("", self.port), handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def stop(self):
        if self.server:
            self.server.shutdown()
            self.server.server_close()
            self.server = None
