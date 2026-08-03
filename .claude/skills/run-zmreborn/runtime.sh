#!/usr/bin/env bash
# Docker emulator configuration, lifecycle, and boot helpers.

readonly SKILL_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly ROOT_DIR="$(cd -- "${SKILL_DIR}/../../.." && pwd)"
readonly DOCKER_CONTEXT="${DOCKER_CONTEXT:-docker-dev}"
readonly API_LEVEL="${API_LEVEL:-35}"
if [[ "${API_LEVEL}" == 35 ]]; then
    readonly DEFAULT_CONTAINER=zeam-runtime DEFAULT_AVD_NAME=zeam_avd
    readonly DEFAULT_ADB_PORT=5555 DEFAULT_NOVNC_PORT=6080
    readonly DEFAULT_OUT_DIR="${ROOT_DIR}/.android-emulator-artifacts/api35"
else
    readonly DEFAULT_CONTAINER="zeam-runtime-api${API_LEVEL}"
    readonly DEFAULT_AVD_NAME="zeam_avd_api${API_LEVEL}"
    readonly DEFAULT_ADB_PORT=5556 DEFAULT_NOVNC_PORT=6081
    readonly DEFAULT_OUT_DIR="${ROOT_DIR}/.android-emulator-artifacts/api${API_LEVEL}"
fi
readonly CONTAINER="${CONTAINER:-${DEFAULT_CONTAINER}}"
readonly AVD_NAME="${AVD_NAME:-${DEFAULT_AVD_NAME}}"
readonly SYSTEM_IMAGE="${SYSTEM_IMAGE:-system-images;android-${API_LEVEL};google_apis;x86_64}"
readonly EMULATOR_IMAGE="${EMULATOR_IMAGE:-zeam-docker-emulator:android${API_LEVEL}}"
readonly ADB_HOST_PORT="${ADB_HOST_PORT:-${DEFAULT_ADB_PORT}}"
readonly NOVNC_HOST_PORT="${NOVNC_HOST_PORT:-${DEFAULT_NOVNC_PORT}}"
readonly OUT_DIR="${OUT_DIR:-${DEFAULT_OUT_DIR}}"
readonly KVM_DEVICE="${KVM_DEVICE:-}"
readonly ENTRYPOINT="${ROOT_DIR}/tools/emulator-entrypoint.sh"
readonly NOVNC_URL="http://127.0.0.1:${NOVNC_HOST_PORT}/vnc.html?autoconnect=true&resize=scale"
readonly RUNTIME_SIGNATURE="${API_LEVEL}|${SYSTEM_IMAGE}|${EMULATOR_IMAGE}|${AVD_NAME}|${ADB_HOST_PORT}|${NOVNC_HOST_PORT}|${ROOT_DIR}|${OUT_DIR}"
RUNTIME_DEVICE=()

fail() {
    printf 'ERROR: %s\n' "$1" >&2
    exit 1
}

docker_cli() {
    env -u DOCKER_HOST docker --context "${DOCKER_CONTEXT}" "$@"
}

container_exec() {
    docker_cli exec "${CONTAINER}" "$@"
}

source "${SKILL_DIR}/preflight.sh"

container_exists() {
    docker_cli container inspect "${CONTAINER}" >/dev/null 2>&1
}

runtime_image_id() {
    docker_cli image inspect -f '{{.Id}}' "${EMULATOR_IMAGE}" 2>/dev/null \
        || fail "Docker image unavailable: ${EMULATOR_IMAGE}"
}

container_value() {
    docker_cli inspect -f "$1" "${CONTAINER}"
}

container_has_kvm() {
    local device="${KVM_DEVICE:-/dev/kvm}"
    container_value '{{range .HostConfig.Devices}}{{println .PathOnHost .PathInContainer}}{{end}}' \
        | grep -Fq "${device} /dev/kvm"
}

validate_existing_runtime() {
    local require_kvm="$1"
    container_exists || return 0
    local recreate_command="API_LEVEL=${API_LEVEL} $0 recreate"
    if [[ "${require_kvm}" == 1 ]]; then
        recreate_command="API_LEVEL=${API_LEVEL} KVM_DEVICE=${KVM_DEVICE:-/dev/kvm} $0 recreate"
    fi
    [[ "$(container_value '{{index .Config.Labels "org.zmreborn.runtime"}}')" == "${RUNTIME_SIGNATURE}" ]] \
        || fail "Container ${CONTAINER} has incompatible API, ports, or mounts. Run: ${recreate_command}"
    [[ "$(container_value '{{.Image}}')" == "$(runtime_image_id)" ]] \
        || fail "Container ${CONTAINER} uses stale emulator image. Run: ${recreate_command}"
    [[ "${require_kvm}" != 1 ]] || container_has_kvm \
        || fail "Container ${CONTAINER} lacks KVM. Run: ${recreate_command}"
}

configure_runtime_device() {
    local require_kvm="$1"
    RUNTIME_DEVICE=()
    if [[ "${require_kvm}" == 1 ]]; then
        RUNTIME_DEVICE=(--device "${KVM_DEVICE:-/dev/kvm}:/dev/kvm")
        return
    fi
    if [[ -n "${KVM_DEVICE}" ]]; then
        RUNTIME_DEVICE=(--device "${KVM_DEVICE}:/dev/kvm")
        return
    fi
    printf 'KVM disabled; emulator will use software acceleration.\n' >&2
}

run_runtime_container() {
    local require_kvm="$1"
    docker_cli run -d --name "${CONTAINER}" "${RUNTIME_DEVICE[@]}" \
        --label "org.zmreborn.runtime=${RUNTIME_SIGNATURE}" \
        -e API_LEVEL="${API_LEVEL}" -e SYSTEM_IMAGE="${SYSTEM_IMAGE}" \
        -e AVD_NAME="${AVD_NAME}" -e REQUIRE_KVM="${require_kvm}" \
        -p "127.0.0.1:${ADB_HOST_PORT}:5555" \
        -p "127.0.0.1:${NOVNC_HOST_PORT}:6080" \
        --tmpfs /root/.android:exec,size=9g \
        -v "${ROOT_DIR}:/workspace:ro" -v "${OUT_DIR}:/artifacts" \
        -v "${ENTRYPOINT}:/entrypoint.sh:ro" "${EMULATOR_IMAGE}" >/dev/null
}

start_new_runtime() {
    local require_kvm="$1"
    configure_runtime_device "${require_kvm}"
    printf 'Starting new container %s…\n' "${CONTAINER}"
    run_runtime_container "${require_kvm}"
}

boot_completed() {
    container_exec adb shell getprop sys.boot_completed 2>/dev/null | grep -qx 1 || return
    container_exec adb shell cmd package path android >/dev/null 2>&1 || return
    container_exec adb shell service check activity 2>/dev/null | grep -qx 'Service activity: found'
}

wait_boot() {
    printf 'Waiting for emulator boot (up to 5 min)…\n'
    local attempt
    for attempt in $(seq 1 60); do
        if boot_completed; then
            container_exec adb shell wm dismiss-keyguard 2>/dev/null || true
            return
        fi
        sleep 5
    done
    fail 'Emulator boot timed out after 5 min'
}

wait_novnc() {
    local attempt
    for attempt in $(seq 1 30); do
        container_exec curl --fail --silent --show-error --max-time 2 \
            http://127.0.0.1:6080/vnc.html >/dev/null && return
        sleep 1
    done
    fail "noVNC did not become ready inside container ${CONTAINER}"
}

print_novnc_access() {
    printf 'noVNC ready on Docker host loopback port %s.\n' "${NOVNC_HOST_PORT}"
    printf 'Tunnel: ssh -N -L %s:127.0.0.1:%s %s\n' \
        "${NOVNC_HOST_PORT}" "${NOVNC_HOST_PORT}" "${DOCKER_CONTEXT}"
    printf 'Then open: %s\n' "${NOVNC_URL}"
}

resume_existing_runtime() {
    case "$(container_value '{{.State.Status}}')" in
        running) printf 'Container %s already running.\n' "${CONTAINER}" ;;
        paused) docker_cli unpause "${CONTAINER}" >/dev/null ;;
        exited|created) docker_cli start "${CONTAINER}" >/dev/null ;;
        *) fail "Container ${CONTAINER} is in an unexpected state" ;;
    esac
}

finish_runtime_start() {
    wait_boot
    wait_novnc
    print_novnc_access
}

command_start() {
    local require_kvm="${1:-0}"
    validate_daemon_mounts
    validate_existing_runtime "${require_kvm}"
    if container_exists; then
        resume_existing_runtime
        finish_runtime_start
        return
    fi
    start_new_runtime "${require_kvm}"
    finish_runtime_start
}

command_recreate() {
    container_exists && docker_cli rm --force "${CONTAINER}" >/dev/null
    command_start 0
}
