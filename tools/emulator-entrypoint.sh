#!/usr/bin/env bash
set -Eeuo pipefail

readonly SDK="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
readonly AVD_NAME="${AVD_NAME:-zeam_avd}"
readonly SYSTEM_IMAGE="${SYSTEM_IMAGE:-system-images;android-35;google_apis;x86_64}"
readonly DEVICE="${EMULATOR_DEVICE:-pixel_3a}"

create_avd() {
    if "${SDK}/cmdline-tools/latest/bin/avdmanager" list avd 2>/dev/null | grep -q "Name: ${AVD_NAME}"; then
        printf 'AVD %s already exists\n' "${AVD_NAME}"
        return
    fi
    printf 'Creating AVD %s...\n' "${AVD_NAME}"
    echo "no" | "${SDK}/cmdline-tools/latest/bin/avdmanager" create avd \
        --name "${AVD_NAME}" \
        --package "${SYSTEM_IMAGE}" \
        --device "${DEVICE}" \
        --force
}

set_ini() {
    local file="$1" key="$2" val="$3"
    if grep -q "^${key}=" "${file}" 2>/dev/null; then
        sed -i "s|^${key}=.*|${key}=${val}|" "${file}"
    else
        echo "${key}=${val}" >> "${file}"
    fi
}

configure_avd() {
    local config_file="${ANDROID_AVD_HOME:-/root/.android/avd}/${AVD_NAME}.avd/config.ini"
    if [[ ! -f "${config_file}" ]]; then
        return
    fi
    # Low-memory headless config with reduced data partition for disk-constrained hosts
    set_ini "${config_file}" hw.ramSize 2048
    set_ini "${config_file}" hw.gpu.enabled yes
    set_ini "${config_file}" hw.gpu.mode swiftshader_indirect
    set_ini "${config_file}" hw.lcd.width 1080
    set_ini "${config_file}" hw.lcd.height 1920
    set_ini "${config_file}" hw.lcd.density 420
    set_ini "${config_file}" disk.dataPartition.size 2048m
}

start_emulator() {
    printf 'Starting emulator %s...\n' "${AVD_NAME}"
    exec "${SDK}/emulator/emulator" \
        -avd "${AVD_NAME}" \
        -no-window \
        -no-audio \
        -no-boot-anim \
        -accel on \
        -memory 2048 \
        -gpu swiftshader_indirect \
        -no-snapshot \
        -partition-size 2048 \
        -no-metrics \
        -port 5554 \
        -verbose 2>&1
}

main() {
    mkdir -p "${ANDROID_AVD_HOME:-/root/.android/avd}"
    create_avd
    configure_avd
    start_emulator
}

main "$@"
