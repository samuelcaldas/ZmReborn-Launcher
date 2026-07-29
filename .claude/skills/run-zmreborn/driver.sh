#!/usr/bin/env bash
# Build current APK, deploy it, then expose local noVNC manual interaction.
set -Eeuo pipefail

readonly CONTAINER="${CONTAINER:-zeam-runtime}"
readonly EMULATOR_IMAGE="${EMULATOR_IMAGE:-zeam-docker-emulator:android35}"
readonly OUT_DIR="${OUT_DIR:-/tmp/zeam-captures}"
readonly ENTRYPOINT="/home/samuelcaldas/repos/zmreborn/tools/emulator-entrypoint.sh"
readonly KVM_DEVICE="${KVM_DEVICE:-}"
readonly NOVNC_URL="http://127.0.0.1:6080/vnc.html?autoconnect=true&resize=scale"
readonly DOCKER="env -u DOCKER_HOST docker --context docker-dev"
readonly EXEC="${DOCKER} exec ${CONTAINER}"

mkdir -p "${OUT_DIR}"

fail() {
    printf 'ERROR: %s\n' "$1" >&2
    exit 1
}

container_exists() {
    ${DOCKER} container inspect "${CONTAINER}" >/dev/null 2>&1
}

container_status() {
    ${DOCKER} inspect -f '{{.State.Status}}' "${CONTAINER}"
}

novnc_binding() {
    ${DOCKER} inspect -f '{{with (index .NetworkSettings.Ports "6080/tcp")}}{{with index . 0}}{{.HostIp}}:{{.HostPort}}{{end}}{{end}}' \
        "${CONTAINER}"
}

runtime_image_id() {
    ${DOCKER} image inspect -f '{{.Id}}' "${EMULATOR_IMAGE}" 2>/dev/null || fail "Docker image unavailable: ${EMULATOR_IMAGE}"
}

validate_existing_runtime() {
    if ! container_exists; then
        return
    fi
    [[ "$(novnc_binding)" == '127.0.0.1:6080' ]] || \
        fail "Container ${CONTAINER} lacks local noVNC. Rebuild image, then run: $0 recreate"
    local container_image
    container_image=$(${DOCKER} inspect -f '{{.Image}}' "${CONTAINER}")
    [[ "${container_image}" == "$(runtime_image_id)" ]] || \
        fail "Container ${CONTAINER} uses stale emulator image. Run: $0 recreate"
}

shot() {
    local name="${1:-manual}"
    local destination="${OUT_DIR}/${name}.png"
    ${EXEC} adb exec-out screencap -p > "${destination}"
    printf '→ %s\n' "${destination}"
}

boot_completed() {
    ${EXEC} adb shell getprop sys.boot_completed 2>/dev/null | grep -qx '1' && return
    ${EXEC} adb shell getprop init.svc.bootanim 2>/dev/null | grep -qx 'stopped' || return
    ${EXEC} adb shell cmd package path android >/dev/null 2>&1 || return
    ${EXEC} adb shell service check activity 2>/dev/null | grep -qx 'Service activity: found'
}

wait_boot() {
    local attempt
    printf 'Waiting for emulator boot (up to 5 min)…\n'
    for attempt in $(seq 1 60); do
        if boot_completed; then
            ${EXEC} adb shell wm dismiss-keyguard 2>/dev/null || true
            return
        fi
        sleep 5
    done
    fail 'Emulator boot timed out after 5 min'
}

wait_novnc() {
    local attempt
    for attempt in $(seq 1 30); do
        curl --fail --silent --show-error --max-time 2 "${NOVNC_URL}" >/dev/null && return
        sleep 1
    done
    fail "noVNC did not become ready at ${NOVNC_URL}"
}

start_new_runtime() {
    local -a device=()
    if [[ -n "${KVM_DEVICE}" ]]; then
        device=(--device "${KVM_DEVICE}")
    else
        printf 'KVM disabled; emulator will use software acceleration.\n' >&2
    fi
    printf 'Starting new container %s…\n' "${CONTAINER}"
    ${DOCKER} run -d --name "${CONTAINER}" "${device[@]}" \
        -p 5555:5555 -p 127.0.0.1:6080:6080 \
        --tmpfs /root/.android:exec,size=9g \
        -v "${ENTRYPOINT}:/entrypoint.sh:ro" "${EMULATOR_IMAGE}"
}

cmd_start() {
    validate_existing_runtime
    if ! container_exists; then
        start_new_runtime
    else
        case "$(container_status)" in
            running) printf 'Container %s already running.\n' "${CONTAINER}" ;;
            exited|created|paused) ${DOCKER} start "${CONTAINER}" ;;
            *) fail "Container ${CONTAINER} is in an unexpected state" ;;
        esac
    fi
    wait_boot
    wait_novnc
    printf 'Open noVNC: %s\n' "${NOVNC_URL}"
}

cmd_recreate() {
    if container_exists; then
        ${DOCKER} rm --force "${CONTAINER}" >/dev/null
        printf 'Removed stale container %s.\n' "${CONTAINER}"
    fi
    cmd_start
}

cmd_build() {
    ./tools/build_apk.sh
}

cmd_install() {
    local apk="${1:-app/build/outputs/apk/debug/app-debug.apk}"
    [[ -f "${apk}" ]] || fail "APK not found: ${apk}. Run: $0 build"
    ${DOCKER} cp "${apk}" "${CONTAINER}:/tmp/zeam-test.apk"
    ${EXEC} adb root 2>/dev/null || true
    sleep 1
    ${EXEC} adb install -r /tmp/zeam-test.apk
}

cmd_launch() {
    ${EXEC} adb shell am force-stop org.zmreborn 2>/dev/null || true
    ${EXEC} adb shell am start -n org.zmreborn/.Launcher
    sleep 3
    shot launched
    printf 'Launcher running. Open noVNC: %s\n' "${NOVNC_URL}"
}

cmd_deploy() {
    validate_existing_runtime
    cmd_build
    cmd_start
    cmd_install
    cmd_launch
}

case "${1:-deploy}" in
    deploy) cmd_deploy ;;
    build) cmd_build ;;
    start) cmd_start ;;
    recreate) cmd_recreate ;;
    install) cmd_install "${2:-}" ;;
    launch) cmd_launch ;;
    shot) shot "${2:-manual}" ;;
    *) fail 'Commands: deploy | build | start | recreate | install [apk] | launch | shot [name]' ;;
esac
