#!/usr/bin/env bash
# Verifies DEX bytecode of a patched APK using device-native ART dex2oat.
# Operates via /data/local/tmp without installing or modifying packages.
# Only the base APK is needed as split APKs do not carry DEX code.

set -euo pipefail

TAG="[verify-art]"
REMOTE_BASE="/data/local/tmp/morphe-art-verify-$$"

cleanup() {
    adb shell rm -f "$REMOTE_BASE".* >/dev/null 2>&1 || true
}
trap cleanup EXIT

select_device() {
    if ! command -v adb >/dev/null 2>&1; then
        echo "$TAG ERROR: adb command not found in PATH." >&2
        exit 2
    fi

    if [[ -n "${ANDROID_SERIAL:-}" ]]; then
        export ANDROID_SERIAL
        return 0
    fi

    local devices=()
    mapfile -t devices < <(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1}')
    local count="${#devices[@]}"

    if [[ "$count" -eq 1 ]]; then
        export ANDROID_SERIAL="${devices[0]}"
        return 0
    fi

    echo "$TAG ERROR: Expected 1 connected device in 'device' state, found $count." >&2
    echo "$TAG Connected devices:" >&2
    adb devices -l 2>/dev/null | awk -v tag="$TAG" 'NR>1 && NF {print tag "   " $0}' >&2
    echo "$TAG Please set ANDROID_SERIAL=<serial> and re-run." >&2
    exit 2
}

find_verifier() {
    local candidates=(
        "/apex/com.android.art/bin/dex2oat64"
        "/apex/com.android.art/bin/dex2oat"
        "/system/bin/dex2oat64"
        "/system/bin/dex2oat"
    )
    local bin
    for bin in "${candidates[@]}"; do
        if adb shell "test -e '$bin'" 2>/dev/null; then
            echo "$bin"
            return 0
        fi
    done

    echo "$TAG ERROR: No ART dex2oat verifier binary found on device." >&2
    echo "$TAG Checked: ${candidates[*]}" >&2
    exit 2
}

push_apk() {
    local src="$1"
    local dst="$2"

    if ! adb push "$src" "$dst" >/dev/null 2>&1; then
        echo "$TAG ERROR: Failed to push APK to device path: $dst" >&2
        exit 2
    fi
}

parse_pid() {
    local output="$1"
    local pid
    pid=$(echo "$output" | tr -d '\r' | sed -n 's/^PID=//p' | head -n1)

    if [[ -n "$pid" && "$pid" =~ ^[0-9]+$ ]]; then
        echo "$pid"
        return 0
    fi

    return 1
}

extract_pid() {
    local output="$1"
    local pid
    if ! pid=$(parse_pid "$output"); then
        echo "$TAG ERROR: Failed to extract dex2oat process PID." >&2
        echo "$output" >&2
        exit 1
    fi

    echo "$pid"
}

print_dex2oat_reasons() {
    local pid="$1"
    if [[ -z "$pid" ]]; then
        return 1
    fi

    local pattern='Failed to verify|Failure to verify dex file|Failed to open dex|Verification error in|Rejecting class'
    local reasons
    reasons=$(adb logcat -d --pid="$pid" 2>/dev/null | tr -d '\r' | grep -E "$pattern" || true)

    if [[ -z "$reasons" ]]; then
        return 1
    fi

    while IFS= read -r line; do
        echo "$TAG $line" >&2
    done <<< "$reasons"
    return 0
}

run_verifier() {
    local bin="$1"
    local remote_base="$2"

    # Design: exec replaces the subshell with dex2oat in the same PID, so PID=$$
    # matches dex2oat for logcat, while dex2oat's exit code is directly returned by adb shell.
    local output rc=0
    set +e
    output=$(adb shell "sh -c 'echo PID=\$\$; exec $bin --dex-file=$remote_base.apk --dex-location=/data/app/morphe-art-verify.apk --oat-file=$remote_base.odex --compiler-filter=verify -j4; '" 2>&1)
    rc=$?
    set -e

    if [[ "$rc" -ne 0 ]]; then
        local pid
        pid=$(parse_pid "$output" || true)
        if print_dex2oat_reasons "$pid"; then
            echo "$TAG FAIL: ART refused to open the APK DEX files (dex2oat exit $rc)" >&2
        else
            echo "$TAG ERROR: dex2oat execution failed with exit code $rc." >&2
            echo "$output" >&2
        fi
        exit 1
    fi

    extract_pid "$output"
}

get_device_info() {
    local model sdk
    model=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r\n')
    sdk=$(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r\n')
    echo "${model:-unknown}, API ${sdk:-unknown}"
}

report_results() {
    local pid="$1"
    local pattern='Verification error in|Failed to verify|failed to verify|Rejecting class'
    local raw_log
    raw_log=$(adb logcat -d --pid="$pid" 2>/dev/null | tr -d '\r' | grep -E "$pattern" || true)

    if [[ -n "$raw_log" ]]; then
        local count
        count=$(echo "$raw_log" | grep -c -E 'Verification error in|Rejecting class' || true)
        while IFS= read -r line; do
            echo "$TAG $line"
        done <<< "$raw_log"
        echo "$TAG FAIL: ART rejected $count method(s)"
        exit 1
    fi

    local dev_info
    dev_info=$(get_device_info)
    echo "$TAG PASS: no ART verification errors ($dev_info)"
    exit 0
}

main() {
    if [[ $# -ne 1 ]]; then
        echo "$TAG ERROR: Expected exactly one argument." >&2
        echo "$TAG Usage: $0 <patched-base.apk>" >&2
        exit 2
    fi

    local apk_path="$1"
    if [[ ! -f "$apk_path" ]]; then
        echo "$TAG ERROR: APK file not found: $apk_path" >&2
        exit 2
    fi

    select_device

    local verifier_bin
    verifier_bin=$(find_verifier)

    push_apk "$apk_path" "$REMOTE_BASE.apk"

    local pid
    pid=$(run_verifier "$verifier_bin" "$REMOTE_BASE")

    report_results "$pid"
}

main "$@"
