#!/usr/bin/env bash
set -Eeuo pipefail

readonly ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
readonly DOCKER_CONTEXT="${DOCKER_CONTEXT:-docker-dev}"
readonly DOCKER_IMAGE="${DOCKER_IMAGE:-zeam-docker-dev:android35}"
readonly GRADLE_CACHE_VOLUME="${GRADLE_CACHE_VOLUME:-zeam-gradle-cache}"
readonly ANDROID_USER_HOME_VOLUME="${ANDROID_USER_HOME_VOLUME:-zeam-android-user-home}"
readonly JAVA_HOME_IN_CONTAINER="${JAVA_HOME_IN_CONTAINER:-/usr/lib/jvm/java-17-openjdk-amd64}"
readonly APK_RELATIVE_PATH="app/build/outputs/apk/debug/app-debug.apk"
readonly TEST_APK_RELATIVE_PATH="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
readonly BUILD_LOG="$(mktemp)"
BUILD_ANDROID_TEST=0
WORKSPACE_PROBE=""

cleanup() {
    rm -f -- "${BUILD_LOG}"
    [[ -z "${WORKSPACE_PROBE}" ]] || rm -f -- "${WORKSPACE_PROBE}"
}

fail() {
    printf 'APK build failed: %s\n' "$1" >&2
    exit 1
}

usage() {
    printf '%s\n' \
        'Usage: ./tools/build_apk.sh [--with-android-test]' \
        '' \
        'Builds the debug APK inside Docker.' \
        '--with-android-test also builds the matching instrumentation APK.' \
        'Success output contains artifact paths, byte sizes, and SHA-256 values.' \
        'Optional environment overrides: DOCKER_CONTEXT, DOCKER_IMAGE,' \
        'GRADLE_CACHE_VOLUME, ANDROID_USER_HOME_VOLUME, JAVA_HOME_IN_CONTAINER.'
}

validate_arguments() {
    if [[ $# -eq 0 ]]; then
        return
    fi
    if [[ $# -eq 1 && "$1" == "--help" ]]; then
        usage
        exit 0
    fi
    if [[ $# -eq 1 && "$1" == "--with-android-test" ]]; then
        BUILD_ANDROID_TEST=1
        return
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

run_workspace_probe() {
    local image_id="$1" relative_probe="$2"
    docker_cli --context "${DOCKER_CONTEXT}" run --rm --pull=never --user root \
        -e "PROBE_PATH=/workspace/${relative_probe}" -v "${ROOT_DIR}:/workspace" \
        --entrypoint /bin/bash "${image_id}" \
        -ec 'grep -qx client "$PROBE_PATH" && printf "daemon\n" > "$PROBE_PATH"' \
        >"${BUILD_LOG}" 2>&1
}

verify_workspace_mount() {
    local image_id="$1" relative_probe
    mkdir -p "${ROOT_DIR}/app/build"
    WORKSPACE_PROBE="$(mktemp "${ROOT_DIR}/app/build/.docker-workspace-probe.XXXXXX")"
    relative_probe="${WORKSPACE_PROBE#${ROOT_DIR}/}"
    printf 'client\n' > "${WORKSPACE_PROBE}"
    : > "${BUILD_LOG}"
    run_workspace_probe "${image_id}" "${relative_probe}" \
        && grep -qx daemon "${WORKSPACE_PROBE}" && return
    fail_with_log "Docker daemon does not share current workspace"
}

run_gradle_container() {
    local image_id="$1"
    shift
    docker_cli --context "${DOCKER_CONTEXT}" run --rm --pull=never --user root \
        -e HOME=/root -e GRADLE_USER_HOME=/root/.gradle \
        -e JAVA_HOME="${JAVA_HOME_IN_CONTAINER}" -e TZ=America/Sao_Paulo \
        -v "${ROOT_DIR}:/workspace" -v "${GRADLE_CACHE_VOLUME}:/root/.gradle" \
        -v "${ANDROID_USER_HOME_VOLUME}:/root/.android" -w /workspace \
        "${image_id}" ./gradlew --quiet "$@" --no-daemon --console=plain \
        >"${BUILD_LOG}" 2>&1
}

run_build() {
    local image_id="$1"
    local -a tasks=(:app:assembleDebug)
    [[ "${BUILD_ANDROID_TEST}" -ne 1 ]] || tasks+=(:app:assembleDebugAndroidTest)
    run_gradle_container "${image_id}" "${tasks[@]}" && return
    fail_with_log "Gradle debug APK task failed"
}

print_artifact() {
    local label="$1"
    local relative_path="$2"
    local artifact_path="${ROOT_DIR}/${relative_path}"
    [[ -f "${artifact_path}" ]] || fail "build completed without ${relative_path}"
    local artifact_hash
    local artifact_size
    read -r artifact_hash _ < <(sha256sum "${artifact_path}")
    artifact_size="$(stat -c '%s' "${artifact_path}")"
    printf '%s: %s\nBytes: %s\nSHA-256: %s\n' \
        "${label}" "${relative_path}" "${artifact_size}" "${artifact_hash}"
}

print_artifacts() {
    print_artifact APK "${APK_RELATIVE_PATH}"
    if [[ "${BUILD_ANDROID_TEST}" -eq 1 ]]; then
        print_artifact 'AndroidTest APK' "${TEST_APK_RELATIVE_PATH}"
    fi
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
    verify_workspace_mount "${image_id}"
    run_build "${image_id}"
    print_artifacts
}

main "$@"
