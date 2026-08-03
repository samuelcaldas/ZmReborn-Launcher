#!/usr/bin/env bash
# Builds, deploys, and tests ZM Reborn in the local Docker emulator.
set -Eeuo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/runtime.sh"

command_build() {
    "${ROOT_DIR}/tools/build_apk.sh" "$@"
}

command_install() {
    local apk="${1:-${ROOT_DIR}/app/build/outputs/apk/debug/app-debug.apk}"
    [[ -f "${apk}" ]] || fail "APK not found: ${apk}. Run: $0 build"
    docker_cli cp "${apk}" "${CONTAINER}:/tmp/zeam-test.apk"
    container_exec adb install -r /tmp/zeam-test.apk
}

shot() {
    local destination="${OUT_DIR}/${1:-manual}.png"
    container_exec adb exec-out screencap -p > "${destination}"
    printf '→ %s\n' "${destination}"
}

command_launch() {
    container_exec adb shell am force-stop org.zmreborn 2>/dev/null || true
    container_exec adb shell am start -n org.zmreborn/.Launcher
    sleep 3
    shot launched
    printf 'Launcher running.\n'
    print_novnc_access
}

command_deploy() {
    command_build
    command_start 0
    command_install
    command_launch
}

command_test() {
    validate_test_kvm
    validate_daemon_mounts
    validate_existing_runtime 1
    command_build --with-android-test
    command_start 1
    local diagnostics="/artifacts/e2e-diagnostics-api${API_LEVEL}"
    local -a environment=(-e "EXPECTED_API_LEVEL=${API_LEVEL}" \
        -e DIAGNOSTICS_ROOT=/artifacts -e "DIAGNOSTICS_DIR=${diagnostics}")
    if [[ -n "${INSTRUMENTATION_TEST_CLASS:-}" ]]; then
        environment+=(-e "INSTRUMENTATION_TEST_CLASS=${INSTRUMENTATION_TEST_CLASS}")
    fi
    docker_cli exec "${environment[@]}" "${CONTAINER}" \
        /workspace/tools/run_ci_emulator_tests.sh
}

main() {
    validate_inputs
    case "${1:-deploy}" in
        deploy) command_deploy ;;
        build) command_build ;;
        start) command_start 0 ;;
        recreate) command_recreate ;;
        install) command_install "${2:-}" ;;
        launch) command_launch ;;
        shot) shot "${2:-manual}" ;;
        test) command_test ;;
        *) fail 'Commands: deploy | build | start | recreate | install [apk] | launch | shot [name] | test' ;;
    esac
}

main "$@"
