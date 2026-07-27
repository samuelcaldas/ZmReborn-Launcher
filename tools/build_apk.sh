#!/usr/bin/env bash
set -Eeuo pipefail

readonly ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
readonly DOCKER_CONTEXT="${DOCKER_CONTEXT:-docker-dev}"
readonly DOCKER_IMAGE="${DOCKER_IMAGE:-zeam-docker-dev:android35}"
readonly GRADLE_CACHE_VOLUME="${GRADLE_CACHE_VOLUME:-zeam-gradle-cache}"
readonly APK_RELATIVE_PATH="app/build/outputs/apk/debug/app-debug.apk"
readonly APK_PATH="${ROOT_DIR}/${APK_RELATIVE_PATH}"
readonly BUILD_LOG="$(mktemp)"

cleanup() {
    rm -f -- "${BUILD_LOG}"
}

fail() {
    printf 'APK build failed: %s\n' "$1" >&2
    exit 1
}

usage() {
    printf '%s\n' \
        'Usage: ./tools/build_apk.sh' \
        '' \
        'Builds the debug APK inside Docker.' \
        'Success output contains only APK path, byte size, and SHA-256.' \
        'Optional environment overrides: DOCKER_CONTEXT, DOCKER_IMAGE,' \
        'GRADLE_CACHE_VOLUME.'
}

validate_arguments() {
    if [[ $# -eq 0 ]]; then
        return
    fi
    if [[ $# -eq 1 && "$1" == "--help" ]]; then
        usage
        exit 0
    fi
    fail "unsupported arguments; use --help"
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "required command not found: $1"
}

validate_repository() {
    [[ -x "${ROOT_DIR}/gradlew" ]] || fail "Gradle wrapper is not executable"
    [[ -f "${ROOT_DIR}/app/build.gradle" ]] || fail "app/build.gradle not found"
}

docker_cli() {
    env -u DOCKER_HOST docker "$@"
}

run_preflight() {
    local failure_message="$1"
    shift
    : > "${BUILD_LOG}"
    if "$@" >"${BUILD_LOG}" 2>&1; then
        return
    fi
    fail_with_log "${failure_message}"
}

validate_docker() {
    run_preflight "Docker context unavailable: ${DOCKER_CONTEXT}" \
        docker_cli context inspect "${DOCKER_CONTEXT}"
    run_preflight "Docker daemon unavailable for context: ${DOCKER_CONTEXT}" \
        docker_cli --context "${DOCKER_CONTEXT}" info
}

resolve_docker_image() {
    local image_id
    : > "${BUILD_LOG}"
    if image_id="$(docker_cli --context "${DOCKER_CONTEXT}" image inspect \
        --format '{{.Id}}' "${DOCKER_IMAGE}" 2>"${BUILD_LOG}")"; then
        printf '%s\n' "${image_id}"
        return
    fi
    fail_with_log "Docker image unavailable: ${DOCKER_IMAGE}"
}

print_build_log() {
    if [[ ! -s "${BUILD_LOG}" ]]; then
        return
    fi
    printf '\nBuild diagnostics:\n' >&2
    while IFS= read -r line; do
        printf '%s\n' "${line}" >&2
    done < "${BUILD_LOG}"
}

fail_with_log() {
    printf 'APK build failed: %s\n' "$1" >&2
    print_build_log
    exit 1
}

run_build() {
    local image_id="$1"
    if docker_cli --context "${DOCKER_CONTEXT}" run --rm --pull=never \
        -e TZ=America/Sao_Paulo \
        -v "${ROOT_DIR}:/workspace" \
        -v "${GRADLE_CACHE_VOLUME}:/root/.gradle" \
        -w /workspace \
        "${image_id}" \
        ./gradlew --quiet :app:assembleDebug --no-daemon --console=plain \
        >"${BUILD_LOG}" 2>&1; then
        return
    fi
    fail_with_log "Gradle debug APK task failed"
}

print_artifact() {
    [[ -f "${APK_PATH}" ]] || fail "build completed without ${APK_RELATIVE_PATH}"
    local artifact_hash
    local artifact_size
    read -r artifact_hash _ < <(sha256sum "${APK_PATH}")
    artifact_size="$(stat -c '%s' "${APK_PATH}")"
    printf 'APK: %s\nBytes: %s\nSHA-256: %s\n' \
        "${APK_RELATIVE_PATH}" "${artifact_size}" "${artifact_hash}"
}

main() {
    local image_id
    trap cleanup EXIT
    validate_arguments "$@"
    require_command docker
    require_command sha256sum
    require_command stat
    validate_repository
    validate_docker
    image_id="$(resolve_docker_image)"
    run_build "${image_id}"
    print_artifact
}

main "$@"
