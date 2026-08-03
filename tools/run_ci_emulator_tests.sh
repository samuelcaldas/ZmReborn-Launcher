#!/usr/bin/env bash
# Runs Android instrumentation and smoke checks before emulator teardown.
set -Eeuo pipefail

readonly ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
readonly APP_ID="${APP_ID:-org.zmreborn}"
readonly DEFAULT_LAUNCHER_COMPONENT="org.zmreborn/.Launcher"
readonly LAUNCHER_COMPONENT="${APP_ID}/.Launcher"
readonly TEST_SELECTOR="${INSTRUMENTATION_TEST_CLASS:-}"
readonly EXPECTED_API_LEVEL="${EXPECTED_API_LEVEL:-24}"
readonly APP_APK="${ROOT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
readonly TEST_APK="${ROOT_DIR}/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
readonly DIAGNOSTICS_ROOT="${DIAGNOSTICS_ROOT:-${ROOT_DIR}}"
readonly DIAGNOSTICS_DIR="${DIAGNOSTICS_DIR:-${DIAGNOSTICS_ROOT}/e2e-diagnostics}"
readonly INSTRUMENTATION_LOG="${DIAGNOSTICS_DIR}/instrumentation.log"
readonly SUCCESS_LOGCAT="${DIAGNOSTICS_DIR}/success-logcat.log"
readonly DEVICE_TIMEOUT_SECONDS=30
readonly INSTALL_TIMEOUT_SECONDS=120
readonly INSTRUMENTATION_TIMEOUT_SECONDS=900
readonly SMOKE_TIMEOUT_SECONDS=180
readonly SMOKE_COMMAND_TIMEOUT_SECONDS=15
readonly SMOKE_KILL_GRACE_SECONDS=5
readonly DIAGNOSTIC_TIMEOUT_SECONDS=30
ACTUAL_API_LEVEL="unavailable"
declare -a ADB_TARGET_ARGS=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    ADB_TARGET_ARGS=(-s "${ANDROID_SERIAL}")
fi
readonly -a ADB_TARGET_ARGS

die() {
    printf 'E2E driver failed: %s\n' "$1" >&2
    return 1
}

source "${ROOT_DIR}/tools/emulator_test_runtime.sh"
source "${ROOT_DIR}/tools/emulator_test_smoke.sh"

validate_boundary() {
    [[ "${APP_ID}" =~ ^[A-Za-z0-9_]+([.][A-Za-z0-9_]+)+$ ]] \
        || die "APP_ID is not a valid package identifier: ${APP_ID}"
    if [[ -n "${TEST_SELECTOR}" ]]; then
        [[ "${TEST_SELECTOR}" =~ ^[A-Za-z0-9_.$]+(#[A-Za-z0-9_$]+)?$ ]] \
            || die "INSTRUMENTATION_TEST_CLASS is invalid: ${TEST_SELECTOR}"
    fi
    validate_expected_api_level "${EXPECTED_API_LEVEL}"
    require_command adb
    require_command grep
    require_command realpath
    require_command tee
    require_command timeout
    validate_diagnostics_path "${DIAGNOSTICS_ROOT}" "${DIAGNOSTICS_DIR}"
    [[ -f "${APP_APK}" ]] || die "production APK not found: ${APP_APK}"
    [[ -f "${TEST_APK}" ]] || die "androidTest APK not found: ${TEST_APK}"
}

run_timed() {
    local seconds="$1"
    shift
    timeout --foreground --kill-after=5s "${seconds}s" "$@"
}

run_adb() {
    local seconds="$1"
    shift
    run_timed "${seconds}" adb "${ADB_TARGET_ARGS[@]}" "$@"
}

run_instrumentation() {
    local -a arguments=(shell am instrument -w -r)
    [[ -z "${TEST_SELECTOR}" ]] || arguments+=(-e class "${TEST_SELECTOR}")
    arguments+=("${APP_ID}.test/android.test.InstrumentationTestRunner")

    set +e
    run_adb "${INSTRUMENTATION_TIMEOUT_SECONDS}" "${arguments[@]}" \
        2>&1 | tee "${INSTRUMENTATION_LOG}"
    local command_status="${PIPESTATUS[0]}"
    set -e

    [[ "${command_status}" -eq 0 ]] || {
        printf 'Instrumentation command failed with status %s\n' "${command_status}" >&2
        return "${command_status}"
    }
    grep -Fq 'INSTRUMENTATION_CODE: -1' "${INSTRUMENTATION_LOG}" \
        || die "instrumentation did not report INSTRUMENTATION_CODE: -1"
    if grep -Eq 'shortMsg=Process crashed|INSTRUMENTATION_FAILED|FAILURES!!!' \
            "${INSTRUMENTATION_LOG}"; then
        die "instrumentation reported a crash or test failure"
        return 1
    fi
}

run_primary_checks() {
    printf 'Checking emulator connection and API level...\n'
    run_adb "${DEVICE_TIMEOUT_SECONDS}" get-state || return $?
    verify_device_api_level "${EXPECTED_API_LEVEL}" || return $?
    run_adb "${DEVICE_TIMEOUT_SECONDS}" logcat -c || return $?

    printf 'Installing production APK...\n'
    run_adb "${INSTALL_TIMEOUT_SECONDS}" install -r "${APP_APK}" || return $?
    printf 'Installing androidTest APK...\n'
    run_adb "${INSTALL_TIMEOUT_SECONDS}" install -r "${TEST_APK}" || return $?
    printf 'Running instrumentation tests...\n'
    run_instrumentation || return $?

    printf 'Launching application smoke test...\n'
    local smoke_deadline=$((SECONDS + SMOKE_TIMEOUT_SECONDS))
    run_smoke_adb "${smoke_deadline}" shell am force-stop "${APP_ID}" || return $?
    run_smoke_adb "${smoke_deadline}" shell am start -n "${LAUNCHER_COMPONENT}" \
        > "${DIAGNOSTICS_DIR}/launcher-start.log" || return $?
    smoke_sleep "${smoke_deadline}" 3 || return $?
    run_smoke_adb "${smoke_deadline}" shell am start -n "${LAUNCHER_COMPONENT}" \
        > "${DIAGNOSTICS_DIR}/launcher-restart.log" || return $?
    verify_launcher_focus "${smoke_deadline}" || return $?
    wait_for_workspace_hierarchy "${smoke_deadline}" || return $?
    verify_success_logcat || return $?
}

main() {
    validate_boundary
    reset_diagnostics
    record_status running 0

    local primary_status=0
    run_primary_checks || primary_status=$?
    if [[ "${primary_status}" -ne 0 ]]; then
        record_status failed "${primary_status}"
        collect_diagnostics || true
        return "${primary_status}"
    fi

    record_status passed 0
    format_success_message "${EXPECTED_API_LEVEL}"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    main "$@"
fi
