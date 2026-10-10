#!/usr/bin/env bash
# Bootstrap and verify Frida/JADX triage lab environment.
# Ensures host tooling, device connection, and running frida-server daemon.

set -euo pipefail

FRIDA_VERSION="17.23.1"
HOST_VENV="${HOST_VENV:-$HOME/.local/share/frida-tools}"
ANDROID_SERIAL="${ANDROID_SERIAL:-df286add}"

TAG="[ensure-lab-frida]"

# 1. Host tooling verification
echo "$TAG Checking host tools..."

HOST_MISSING=0
if [[ ! -x "$HOST_VENV/bin/frida" ]] || ! "$HOST_VENV/bin/frida" --version >/dev/null 2>&1; then
    echo "$TAG ERROR: frida CLI missing or not executable in $HOST_VENV/bin/frida." >&2
    echo "$TAG To install frida-tools in host venv:" >&2
    echo "       python3 -m venv \"$HOST_VENV\"" >&2
    echo "       \"$HOST_VENV/bin/pip\" install frida-tools" >&2
    HOST_MISSING=1
fi

if ! command -v jadx >/dev/null 2>&1; then
    echo "$TAG ERROR: jadx not found in PATH." >&2
    echo "$TAG To install jadx (release v1.5.6 in ~/.local/share/jadx + symlink in ~/.local/bin):" >&2
    echo "       mkdir -p \"$HOME/.local/share/jadx\" \"$HOME/.local/bin\"" >&2
    echo "       curl -sSL https://github.com/skylot/jadx/releases/download/v1.5.6/jadx-1.5.6.zip -o /tmp/jadx-1.5.6.zip" >&2
    echo "       unzip -q /tmp/jadx-1.5.6.zip -d \"$HOME/.local/share/jadx\" && rm /tmp/jadx-1.5.6.zip" >&2
    echo "       ln -sf \"$HOME/.local/share/jadx/bin/jadx\" \"$HOME/.local/bin/jadx\"" >&2
    echo "       ln -sf \"$HOME/.local/share/jadx/bin/jadx-gui\" \"$HOME/.local/bin/jadx-gui\"" >&2
    HOST_MISSING=1
fi

if [[ "$HOST_MISSING" -ne 0 ]]; then
    exit 1
fi
echo "$TAG Host tools verified: frida $("$HOST_VENV/bin/frida" --version), jadx $(jadx --version 2>/dev/null || echo 'available')."

# 2. Android device connectivity check
echo "$TAG Checking Android device ($ANDROID_SERIAL)..."
if ! adb -s "$ANDROID_SERIAL" shell echo ok >/dev/null 2>&1; then
    echo "$TAG ERROR: Device '$ANDROID_SERIAL' unreachable via adb." >&2
    echo "$TAG Verify connection and authorization with 'adb devices'." >&2
    exit 1
fi
echo "$TAG Device '$ANDROID_SERIAL' reachable."

# 3. Root / su privilege verification
echo "$TAG Checking root privileges on device..."
if ! adb -s "$ANDROID_SERIAL" shell "su -c 'echo root_ok'" 2>/dev/null | grep -q "root_ok"; then
    echo "$TAG ERROR: Device '$ANDROID_SERIAL' does not have su/root access." >&2
    echo "$TAG Alternative for non-rooted devices: inject Frida Gadget into the target APK (network/listen mode)." >&2
    exit 2
fi

# 4. Frida server status check and deployment
echo "$TAG Checking frida-server status on device..."
SERVER_PID=$(adb -s "$ANDROID_SERIAL" shell "su -c 'pgrep -x frida-server'" 2>/dev/null | tr -d '\r\n' || true)

if [[ -n "$SERVER_PID" ]]; then
    echo "$TAG frida-server already running on device (PID: $SERVER_PID)."
else
    echo "$TAG frida-server not running. Preparing deployment..."

    CLEANUP_TEMP_DIR=false
    if [[ -d "/tmp/opencode" ]]; then
        WORK_DIR="/tmp/opencode"
    elif [[ -n "${TMPDIR:-}" && -d "$TMPDIR" ]]; then
        WORK_DIR="$TMPDIR"
    else
        WORK_DIR="$(mktemp -d)"
        CLEANUP_TEMP_DIR=true
    fi

    XZ_FILE="$WORK_DIR/frida-server-$FRIDA_VERSION-android-arm64.xz"
    BIN_FILE="$WORK_DIR/frida-server-$FRIDA_VERSION-android-arm64"

    cleanup_local() {
        rm -f "$XZ_FILE" "$BIN_FILE"
        if [[ "$CLEANUP_TEMP_DIR" == "true" ]]; then
            rm -rf "$WORK_DIR"
        fi
    }
    trap cleanup_local EXIT

    DOWNLOAD_URL="https://github.com/frida/frida/releases/download/${FRIDA_VERSION}/frida-server-${FRIDA_VERSION}-android-arm64.xz"
    echo "$TAG Downloading frida-server $FRIDA_VERSION from GitHub..."
    curl -sSL "$DOWNLOAD_URL" -o "$XZ_FILE"

    echo "$TAG Decompressing archive..."
    xz -d -f -k "$XZ_FILE"

    echo "$TAG Pushing binary to /data/local/tmp/frida-server..."
    adb -s "$ANDROID_SERIAL" push "$BIN_FILE" /data/local/tmp/frida-server >/dev/null

    echo "$TAG Setting executable permissions..."
    adb -s "$ANDROID_SERIAL" shell "su -c 'chmod 755 /data/local/tmp/frida-server'"

    echo "$TAG Launching frida-server daemon via nohup..."
    adb -s "$ANDROID_SERIAL" shell "su -c 'nohup /data/local/tmp/frida-server >/dev/null 2>&1 &'"

    echo "$TAG Awaiting daemon readiness (up to 15s)..."
    STARTED=false
    for _ in $(seq 1 15); do
        SERVER_PID=$(adb -s "$ANDROID_SERIAL" shell "su -c 'pgrep -x frida-server'" 2>/dev/null | tr -d '\r\n' || true)
        if [[ -n "$SERVER_PID" ]]; then
            STARTED=true
            break
        fi
        sleep 1
    done

    cleanup_local
    trap - EXIT

    if [[ "$STARTED" != "true" ]]; then
        echo "$TAG ERROR: frida-server failed to start within 15 seconds." >&2
        exit 1
    fi
    echo "$TAG frida-server started successfully (PID: $SERVER_PID)."
fi

# 5. End-to-end Python API verification
echo "$TAG Verifying host Frida API connectivity..."
if ! "$HOST_VENV/bin/python" -c "import sys, frida
serial = sys.argv[1]
try:
    device = frida.get_device(id=serial)
    procs = device.enumerate_processes()[:1]
    name = procs[0].name if len(procs) > 0 else 'none'
    print('[ensure-lab-frida] Frida API active: enumerated processes successfully (sample: %s).' % name)
except Exception as exc:
    print('[ensure-lab-frida] Frida API error: %s' % exc, file=sys.stderr)
    sys.exit(1)
" "$ANDROID_SERIAL"; then
    echo "$TAG ERROR: Frida Python API failed to communicate with device '$ANDROID_SERIAL'." >&2
    exit 1
fi

echo "$TAG Frida lab environment is ready and verified."
exit 0
