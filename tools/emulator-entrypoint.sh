#!/usr/bin/env bash
set -Eeuo pipefail

readonly SDK="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
readonly API_LEVEL="${API_LEVEL:-35}"
readonly AVD_NAME="${AVD_NAME:-zeam_avd}"
readonly SYSTEM_IMAGE="${SYSTEM_IMAGE:-system-images;android-35;google_apis;x86_64}"
readonly DEVICE="${EMULATOR_DEVICE:-pixel_3a}"
readonly REQUIRE_KVM="${REQUIRE_KVM:-0}"

X_SERVER_PID=""
VNC_SERVER_PID=""
NOVNC_SERVER_PID=""
EMULATOR_PID=""

validate_runtime_configuration() {
    [[ "${API_LEVEL}" =~ ^[1-9][0-9]*$ ]] || {
        printf 'Invalid API_LEVEL: %s\n' "${API_LEVEL}" >&2
        exit 1
    }
    [[ "${SYSTEM_IMAGE}" == "system-images;android-${API_LEVEL};"* ]] || {
        printf 'SYSTEM_IMAGE does not match API_LEVEL %s: %s\n' \
            "${API_LEVEL}" "${SYSTEM_IMAGE}" >&2
        exit 1
    }
    [[ "${REQUIRE_KVM}" == 0 || "${REQUIRE_KVM}" == 1 ]] || {
        printf 'REQUIRE_KVM must be 0 or 1: %s\n' "${REQUIRE_KVM}" >&2
        exit 1
    }
}

kvm_available() {
    [[ -c /dev/kvm && -r /dev/kvm && -w /dev/kvm ]]
}

create_avd() {
    if "${SDK}/cmdline-tools/latest/bin/avdmanager" list avd 2>/dev/null | grep -q "Name: ${AVD_NAME}"; then
        printf 'AVD %s already exists\n' "${AVD_NAME}"
        return
    fi
    printf 'Creating AVD %s...\n' "${AVD_NAME}"
    printf 'no\n' | "${SDK}/cmdline-tools/latest/bin/avdmanager" create avd \
        --name "${AVD_NAME}" --package "${SYSTEM_IMAGE}" --device "${DEVICE}" --force
}

set_ini() {
    local file="$1" key="$2" val="$3"
    if grep -q "^${key}=" "${file}" 2>/dev/null; then
        sed -i "s|^${key}=.*|${key}=${val}|" "${file}"
        return
    fi
    printf '%s=%s\n' "${key}" "${val}" >> "${file}"
}

configure_avd() {
    local config_file="${ANDROID_AVD_HOME:-/root/.android/avd}/${AVD_NAME}.avd/config.ini"
    [[ -f "${config_file}" ]] || return
    set_ini "${config_file}" hw.ramSize 2048
    set_ini "${config_file}" hw.gpu.enabled yes
    set_ini "${config_file}" hw.gpu.mode swiftshader_indirect
    set_ini "${config_file}" hw.lcd.width 1080
    set_ini "${config_file}" hw.lcd.height 1920
    set_ini "${config_file}" hw.lcd.density 420
    set_ini "${config_file}" disk.dataPartition.size 2048m
}

wait_for_x_server() {
    local attempt
    for attempt in $(seq 1 20); do
        [[ -S /tmp/.X11-unix/X0 ]] && return
        sleep 1
    done
    printf 'Xvfb did not expose display :0\n' >&2
    exit 1
}

start_x_server() {
    Xvfb :0 -screen 0 1080x1920x24 -nolisten tcp &
    X_SERVER_PID=$!
    wait_for_x_server
}

start_vnc_server() {
    x11vnc -display :0 -localhost -rfbport 5900 -forever -shared -nopw &
    VNC_SERVER_PID=$!
}

start_novnc_server() {
    [[ -d /usr/share/novnc ]] || {
        printf 'noVNC web root is missing: /usr/share/novnc\n' >&2
        exit 1
    }
    websockify --web=/usr/share/novnc 6080 localhost:5900 &
    NOVNC_SERVER_PID=$!
}

start_emulator() {
    local -a acceleration=(-accel on)
    if ! kvm_available; then
        if [[ "${REQUIRE_KVM}" == 1 ]]; then
            printf 'KVM is required but /dev/kvm is unavailable or unusable\n' >&2
            exit 1
        fi
        printf 'KVM unavailable, falling back to software acceleration\n' >&2
        acceleration=(-accel off)
    fi
    printf 'Starting emulator %s...\n' "${AVD_NAME}"
    DISPLAY=:0 "${SDK}/emulator/emulator" \
        -avd "${AVD_NAME}" -no-audio -no-boot-anim "${acceleration[@]}" \
        -memory 2048 -gpu swiftshader_indirect -no-snapshot -partition-size 2048 \
        -no-metrics -port 5554 -verbose 2>&1 &
    EMULATOR_PID=$!
}

cleanup() {
    local status="$1"
    for process_id in "${EMULATOR_PID}" "${NOVNC_SERVER_PID}" "${VNC_SERVER_PID}" "${X_SERVER_PID}"; do
        [[ -n "${process_id}" ]] && kill "${process_id}" 2>/dev/null || true
    done
    wait 2>/dev/null || true
    exit "${status}"
}

monitor_processes() {
    while kill -0 "${EMULATOR_PID}" 2>/dev/null \
        && kill -0 "${NOVNC_SERVER_PID}" 2>/dev/null \
        && kill -0 "${VNC_SERVER_PID}" 2>/dev/null \
        && kill -0 "${X_SERVER_PID}" 2>/dev/null; do
        sleep 1
    done
    printf 'Emulator display service exited unexpectedly\n' >&2
    cleanup 1
}

handle_signal() {
    cleanup 0
}

main() {
    trap handle_signal INT TERM
    validate_runtime_configuration
    mkdir -p "${ANDROID_AVD_HOME:-/root/.android/avd}"
    create_avd
    configure_avd
    start_x_server
    start_vnc_server
    start_novnc_server
    start_emulator
    printf 'noVNC available at http://127.0.0.1:6080/vnc.html\n'
    monitor_processes
}

main "$@"
