#!/usr/bin/env bash
# Static contract for local-only QEMU VNC/noVNC emulator support.
set -Eeuo pipefail

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly DOCKERFILE="${ROOT_DIR}/tools/Dockerfile.emulator"
readonly ENTRYPOINT="${ROOT_DIR}/tools/emulator-entrypoint.sh"
readonly DRIVER="${ROOT_DIR}/.claude/skills/run-zmreborn/driver.sh"

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

require_text "${DOCKERFILE}" "novnc"
require_text "${DOCKERFILE}" "websockify"
require_text "${DOCKERFILE}" "x11vnc"
require_text "${DOCKERFILE}" "xvfb"
require_text "${DOCKERFILE}" "EXPOSE 5555 6080"
require_text "${ENTRYPOINT}" "Xvfb :0 -screen 0 1080x1920x24 -nolisten tcp"
require_text "${ENTRYPOINT}" "x11vnc -display :0 -localhost -rfbport 5900 -forever -shared -nopw"
require_text "${ENTRYPOINT}" "websockify --web=/usr/share/novnc 6080 localhost:5900"
reject_text "${ENTRYPOINT}" "-no-window"
require_text "${DRIVER}" "127.0.0.1:6080:6080"
require_text "${DRIVER}" "recreate)"

printf 'noVNC emulator static contract passed\n'
