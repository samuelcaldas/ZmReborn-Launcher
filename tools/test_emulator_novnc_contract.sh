#!/usr/bin/env bash
# Static contract for local-only QEMU VNC/noVNC emulator support.
set -Eeuo pipefail

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly DOCKERFILE="${ROOT_DIR}/tools/Dockerfile.emulator"
readonly ENTRYPOINT="${ROOT_DIR}/tools/emulator-entrypoint.sh"
readonly BUILD_WRAPPER="${ROOT_DIR}/tools/build_apk.sh"
readonly TEST_RUNNER="${ROOT_DIR}/tools/run_ci_emulator_tests.sh"
readonly DRIVER="${ROOT_DIR}/.claude/skills/run-zmreborn/driver.sh"
readonly RUNTIME="${ROOT_DIR}/.claude/skills/run-zmreborn/runtime.sh"
readonly PREFLIGHT="${ROOT_DIR}/.claude/skills/run-zmreborn/preflight.sh"
readonly RUNTIME_TEST="${ROOT_DIR}/tools/test_run_zmreborn_runtime_contract.sh"

require_text() {
    local file="$1"
    local expected="$2"
    grep -Fq -- "${expected}" "${file}" || {
        printf 'Missing `%s` in %s\n' "${expected}" "${file}" >&2
        exit 1
    }
}

reject_text() {
    local file="$1"
    local forbidden="$2"
    if grep -Fq -- "${forbidden}" "${file}"; then
        printf 'Forbidden `%s` in %s\n' "${forbidden}" "${file}" >&2
        exit 1
    fi
}

require_occurrences() {
    local file="$1"
    local expected="$2"
    local minimum="$3"
    local count
    count="$(grep -Fc -- "${expected}" "${file}")"
    (( count >= minimum )) || {
        printf 'Expected at least %s occurrences of `%s` in %s\n' \
            "${minimum}" "${expected}" "${file}" >&2
        exit 1
    }
}

require_before() {
    local file="$1"
    local first="$2"
    local second="$3"
    local first_line
    local second_line
    first_line="$(grep -nF -- "${first}" "${file}" | cut -d: -f1 | grep -m1 .)"
    second_line="$(grep -nF -- "${second}" "${file}" | cut -d: -f1 | grep -m1 .)"
    (( first_line < second_line )) || {
        printf '`%s` must precede `%s` in %s\n' "${first}" "${second}" "${file}" >&2
        exit 1
    }
}

require_text "${DOCKERFILE}" "ARG BASE_IMAGE=zeam-docker-dev:android35"
require_text "${DOCKERFILE}" 'FROM ${BASE_IMAGE}'
require_text "${DOCKERFILE}" "ARG API_LEVEL=35"
require_text "${DOCKERFILE}" "ARG SYSTEM_IMAGE=system-images;android-35;google_apis;x86_64"
require_text "${DOCKERFILE}" "curl"
require_text "${DOCKERFILE}" "novnc"
require_text "${DOCKERFILE}" "websockify"
require_text "${DOCKERFILE}" "x11vnc"
require_text "${DOCKERFILE}" "xvfb"
require_text "${DOCKERFILE}" "EXPOSE 5555 6080"
require_text "${ENTRYPOINT}" "Xvfb :0 -screen 0 1080x1920x24 -nolisten tcp"
require_text "${ENTRYPOINT}" "x11vnc -display :0 -localhost -rfbport 5900 -forever -shared -nopw"
require_text "${ENTRYPOINT}" "websockify --web=/usr/share/novnc 6080 localhost:5900"
require_text "${ENTRYPOINT}" "REQUIRE_KVM"
require_text "${ENTRYPOINT}" "API_LEVEL"
reject_text "${ENTRYPOINT}" "-no-window"
require_text "${BUILD_WRAPPER}" "--with-android-test"
require_text "${BUILD_WRAPPER}" ":app:assembleDebugAndroidTest"
require_text "${TEST_RUNNER}" "EXPECTED_API_LEVEL"
require_text "${RUNTIME}" 'readonly RUNTIME_SIGNATURE="${API_LEVEL}|${SYSTEM_IMAGE}|${EMULATOR_IMAGE}|${AVD_NAME}|${ADB_HOST_PORT}|${NOVNC_HOST_PORT}|${ROOT_DIR}|${OUT_DIR}"'
require_text "${RUNTIME}" '127.0.0.1:${ADB_HOST_PORT}:5555'
require_text "${RUNTIME}" '127.0.0.1:${NOVNC_HOST_PORT}:6080'
require_text "${RUNTIME}" 'container_exec curl --fail'
reject_text "${RUNTIME}" 'curl --fail --silent --show-error --max-time 2 "${NOVNC_URL}"'
require_text "${RUNTIME}" '${ROOT_DIR}:/workspace:ro'
require_text "${RUNTIME}" '${OUT_DIR}:/artifacts'
require_text "${RUNTIME}" '.android-emulator-artifacts'
reject_text "${RUNTIME}" '/tmp/zeam'
require_text "${RUNTIME}" 'source "${SKILL_DIR}/preflight.sh"'
require_text "${PREFLIGHT}" 'validate_test_kvm'
require_text "${PREFLIGHT}" 'docker_cli run --rm --pull=never'
require_text "${PREFLIGHT}" '--device "${device}:/dev/kvm"'
reject_text "${PREFLIGHT}" '[[ -c "${device}"'
require_text "${PREFLIGHT}" 'validate_daemon_mounts'
require_text "${PREFLIGHT}" 'EXPECTED_RUNNER_HASH'
require_text "${PREFLIGHT}" '-v "${ROOT_DIR}:/workspace:ro"'
require_text "${PREFLIGHT}" 'probe="$(mktemp "${OUT_DIR}/.workspace-probe.XXXXXX")"'
require_text "${PREFLIGHT}" 'relative_probe="${probe#${OUT_DIR}/}"'
require_text "${PREFLIGHT}" 'PROBE_PATH=/artifacts/${relative_probe}'
require_text "${PREFLIGHT}" 'RESPONSE_PATH=/artifacts/${relative_probe}.daemon'
reject_text "${PREFLIGHT}" '${ROOT_DIR}:/workspace-probe'
reject_text "${PREFLIGHT}" '/workspace-probe'
require_text "${BUILD_WRAPPER}" 'verify_workspace_mount'
require_text "${BUILD_WRAPPER}" 'app/build/.docker-workspace-probe'
require_text "${BUILD_WRAPPER}" 'run_workspace_probe'
require_occurrences "${BUILD_WRAPPER}" '--user root' 2
require_text "${RUNTIME}" "sys.boot_completed"
require_text "${DRIVER}" "--with-android-test"
require_text "${DRIVER}" "EXPECTED_API_LEVEL"
require_text "${DRIVER}" 'command_start 1'
require_before "${DRIVER}" 'validate_existing_runtime 1' 'command_build --with-android-test'
require_text "${DRIVER}" "recreate)"
require_text "${DRIVER}" "test)"

bash "${RUNTIME_TEST}"
printf 'noVNC emulator static contract passed\n'
