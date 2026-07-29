#!/usr/bin/env bash
# driver.sh — build current APK, deploy to emulator, launch for manual testing.
# All commands verified against zeam-docker-emulator:android35 on 2026-07-29.
set -Eeuo pipefail

CONTAINER="${CONTAINER:-zeam-runtime}"
EMULATOR_IMAGE="${EMULATOR_IMAGE:-zeam-docker-emulator:android35}"
OUT_DIR="${OUT_DIR:-/tmp/zeam-captures}"
ENTRYPOINT="/home/samuelcaldas/repos/zmreborn/tools/emulator-entrypoint.sh"

DOCKER="env -u DOCKER_HOST docker --context docker-dev"
EXEC="${DOCKER} exec ${CONTAINER}"

mkdir -p "${OUT_DIR}"

# ─── helpers ──────────────────────────────────────────────────────────────────

shot() {
    local name="${1:-manual}"
    local dest="${OUT_DIR}/${name}.png"
    ${EXEC} adb exec-out screencap -p > "${dest}"
    echo "→ ${dest}"
}

wait_boot() {
    echo "Waiting for emulator boot (up to 5 min)…"
    local i=0
    until ${EXEC} adb shell getprop sys.boot_completed 2>/dev/null | grep -q "^1$"; do
        sleep 5
        i=$((i + 1))
        [[ ${i} -gt 60 ]] && { echo "ERROR: boot timed out after 5 min"; exit 1; }
    done
    ${EXEC} adb shell wm dismiss-keyguard 2>/dev/null || true
    echo "Boot complete."
}

# ─── start emulator container ─────────────────────────────────────────────────

cmd_start() {
    local status
    status=$(${DOCKER} inspect -f '{{.State.Status}}' "${CONTAINER}" 2>/dev/null || echo "absent")

    case "${status}" in
        running)
            echo "Container ${CONTAINER} already running."
            ;;
        exited|created|paused)
            echo "Restarting stopped container ${CONTAINER}…"
            ${DOCKER} start "${CONTAINER}"
            wait_boot
            ;;
        absent)
            echo "Starting new container ${CONTAINER}…"
            ${DOCKER} run -d --name "${CONTAINER}" \
                --device /dev/kvm -p 5555:5555 \
                --tmpfs /root/.android:exec,size=9g \
                -v "${ENTRYPOINT}:/entrypoint.sh:ro" \
                "${EMULATOR_IMAGE}"
            wait_boot
            ;;
        *)
            echo "ERROR: container ${CONTAINER} in unexpected state: ${status}"; exit 1 ;;
    esac
}

# ─── build APK ────────────────────────────────────────────────────────────────

cmd_build() {
    echo "Building APK…"
    ./tools/build_apk.sh
}

# ─── install APK ──────────────────────────────────────────────────────────────

cmd_install() {
    local apk="${1:-app/build/outputs/apk/debug/app-debug.apk}"
    [[ -f "${apk}" ]] || { echo "APK not found: ${apk}. Run: $0 build"; exit 1; }
    echo "Installing ${apk}…"
    # docker cp to /data/local/tmp/ fails; copy to container /tmp/ first
    ${DOCKER} cp "${apk}" "${CONTAINER}:/tmp/zeam-test.apk"
    ${EXEC} adb root 2>/dev/null || true
    sleep 1
    ${EXEC} adb install -r /tmp/zeam-test.apk
    echo "Installed."
}

# ─── launch launcher ──────────────────────────────────────────────────────────

cmd_launch() {
    echo "Launching ZM Reborn…"
    ${EXEC} adb shell am force-stop org.zmreborn 2>/dev/null || true
    sleep 1
    ${EXEC} adb shell am start -n org.zmreborn/.Launcher
    sleep 3
    shot "launched"
    echo "Launcher running. Use ADB commands for manual interaction."
}

# ─── combined: build + start + install + launch ───────────────────────────────

cmd_deploy() {
    cmd_build
    cmd_start
    cmd_install
    cmd_launch
    echo ""
    echo "Manual test session ready."
    echo "  EXEC=\"${DOCKER} exec ${CONTAINER}\""
    echo "  \$EXEC adb shell input tap X Y"
    echo "  \$EXEC adb exec-out screencap -p > ${OUT_DIR}/shot.png"
}

# ─── dispatch ─────────────────────────────────────────────────────────────────

case "${1:-deploy}" in
    deploy)    cmd_deploy ;;
    build)     cmd_build ;;
    start)     cmd_start ;;
    install)   cmd_install "${2:-}" ;;
    launch)    cmd_launch ;;
    shot)      shot "${2:-manual}" ;;
    *)         echo "Commands: deploy | build | start | install [apk] | launch | shot [name]"; exit 1 ;;
esac
