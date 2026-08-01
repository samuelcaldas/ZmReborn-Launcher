#!/usr/bin/env bash
# Runs hosted Android instrumentation and smoke checks before emulator teardown.
set -Eeuo pipefail

readonly ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
readonly APP_ID="${APP_ID:-org.zmreborn}"
readonly DEFAULT_LAUNCHER_COMPONENT="org.zmreborn/.Launcher"
readonly LAUNCHER_COMPONENT="${APP_ID}/.Launcher"
readonly TEST_SELECTOR="${INSTRUMENTATION_TEST_CLASS:-}"
readonly APP_APK="${ROOT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
readonly TEST_APK="${ROOT_DIR}/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
readonly DIAGNOSTICS_DIR="${ROOT_DIR}/e2e-diagnostics"
readonly INSTRUMENTATION_LOG="${DIAGNOSTICS_DIR}/instrumentation.log"
readonly DEVICE_TIMEOUT_SECONDS=30
readonly INSTALL_TIMEOUT_SECONDS=120
readonly INSTRUMENTATION_TIMEOUT_SECONDS=900
readonly SMOKE_TIMEOUT_SECONDS=60
readonly DIAGNOSTIC_TIMEOUT_SECONDS=30
declare -a ADB_TARGET_ARGS=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    ADB_TARGET_ARGS=(-s "${ANDROID_SERIAL}")
fi
readonly -a ADB_TARGET_ARGS

die() {
    printf 'E2E driver failed: %s\n' "$1" >&2
    return 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || die "required command not found: $1"
}

validate_boundary() {
    [[ "${APP_ID}" =~ ^[A-Za-z0-9_]+([.][A-Za-z0-9_]+)+$ ]] \
        || die "APP_ID is not a valid package identifier: ${APP_ID}"
    if [[ -n "${TEST_SELECTOR}" ]]; then
        [[ "${TEST_SELECTOR}" =~ ^[A-Za-z0-9_.$]+(#[A-Za-z0-9_$]+)?$ ]] \
            || die "INSTRUMENTATION_TEST_CLASS is invalid: ${TEST_SELECTOR}"
    fi
    [[ -f "${APP_APK}" ]] || die "production APK not found: ${APP_APK}"
    [[ -f "${TEST_APK}" ]] || die "androidTest APK not found: ${TEST_APK}"
    require_command adb
    require_command grep
    require_command tee
    require_command timeout
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

record_status() {
    local state="$1"
    local status="$2"
    {
        printf 'state=%s\n' "${state}"
        printf 'status=%s\n' "${status}"
        printf 'app_id=%s\n' "${APP_ID}"
        printf 'launcher_component=%s\n' "${LAUNCHER_COMPONENT}"
        printf 'test_selector=%s\n' "${TEST_SELECTOR:-full-suite}"
        printf 'android_serial=%s\n' "${ANDROID_SERIAL:-default}"
    } > "${DIAGNOSTICS_DIR}/run-status.txt"
}

capture_diagnostic() {
    local output="$1"
    local seconds="$2"
    shift 2
    if run_adb "${seconds}" "$@" > "${DIAGNOSTICS_DIR}/${output}" 2>&1; then
        return
    fi
    printf 'Diagnostic command failed or timed out: adb %s\n' "$*" \
        >> "${DIAGNOSTICS_DIR}/diagnostic-errors.log"
}

capture_screenshot() {
    if run_adb "${DIAGNOSTIC_TIMEOUT_SECONDS}" exec-out screencap -p \
            > "${DIAGNOSTICS_DIR}/screen.png" 2> "${DIAGNOSTICS_DIR}/screen-error.log"; then
        return
    fi
    printf 'Screenshot capture failed or timed out\n' \
        >> "${DIAGNOSTICS_DIR}/diagnostic-errors.log"
}

reset_diagnostics() {
    [[ "${DIAGNOSTICS_DIR}" == "${ROOT_DIR}/e2e-diagnostics" ]] \
        || die "diagnostics path escaped repository root"
    rm -rf -- "${DIAGNOSTICS_DIR}"
    mkdir -p "${DIAGNOSTICS_DIR}"
}

collect_diagnostics() {
    printf 'Collecting bounded emulator diagnostics before teardown...\n' >&2
    capture_diagnostic logcat.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" logcat -d -v threadtime
    capture_diagnostic window.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" shell dumpsys window
    capture_diagnostic activities.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" \
        shell dumpsys activity activities
    capture_diagnostic processes.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" shell ps
    capture_diagnostic package.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" \
        shell dumpsys package "${APP_ID}"
    capture_diagnostic hierarchy-dump.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" \
        shell uiautomator dump /sdcard/failure_window_dump.xml
    run_adb "${DIAGNOSTIC_TIMEOUT_SECONDS}" pull /sdcard/failure_window_dump.xml \
        "${DIAGNOSTICS_DIR}/failure_window_dump.xml" \
        > "${DIAGNOSTICS_DIR}/hierarchy-pull.log" 2>&1 || true
    capture_screenshot
}

run_instrumentation() {
    local -a arguments=(shell am instrument -w -r)
    if [[ -n "${TEST_SELECTOR}" ]]; then
        arguments+=(-e class "${TEST_SELECTOR}")
    fi
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
    if ! grep -Fq 'INSTRUMENTATION_CODE: -1' "${INSTRUMENTATION_LOG}"; then
        die "instrumentation did not report INSTRUMENTATION_CODE: -1"
        return 1
    fi
    if grep -Eq 'shortMsg=Process crashed|INSTRUMENTATION_FAILED|FAILURES!!!' \
            "${INSTRUMENTATION_LOG}"; then
        die "instrumentation reported a crash or test failure"
        return 1
    fi
}

verify_launcher_focus() {
    local focus_log="${DIAGNOSTICS_DIR}/focused-window.log"
    local escaped_app_id="${APP_ID//./\\.}"
    local component_pattern="${escaped_app_id}/(\\.Launcher|${escaped_app_id}\\.Launcher)([[:space:]}]|$)"
    run_adb "${SMOKE_TIMEOUT_SECONDS}" shell dumpsys window > "${focus_log}"
    if grep -Eq "^[[:space:]]*mCurrentFocus=.*${component_pattern}" "${focus_log}"; then
        return
    fi
    if grep -Eq "^[[:space:]]*mFocusedApp=.*${component_pattern}" "${focus_log}"; then
        return
    fi
    printf 'Expected focused Launcher component %s; accepted default %s\n' \
        "${LAUNCHER_COMPONENT}" "${DEFAULT_LAUNCHER_COMPONENT}" >&2
    return 1
}

verify_workspace_hierarchy() {
    local hierarchy="${DIAGNOSTICS_DIR}/window_dump.xml"
    run_adb "${SMOKE_TIMEOUT_SECONDS}" shell uiautomator dump /sdcard/window_dump.xml
    run_adb "${SMOKE_TIMEOUT_SECONDS}" pull /sdcard/window_dump.xml "${hierarchy}"
    grep -Fq "${APP_ID}:id/workspace" "${hierarchy}" \
        || die "workspace id missing from UI hierarchy"
}

run_primary_checks() {
    printf 'Checking emulator connection...\n'
    run_adb "${DEVICE_TIMEOUT_SECONDS}" get-state || return $?
    run_adb "${DEVICE_TIMEOUT_SECONDS}" logcat -c || return $?

    printf 'Installing production APK...\n'
    run_adb "${INSTALL_TIMEOUT_SECONDS}" install -r "${APP_APK}" || return $?

    printf 'Installing androidTest APK...\n'
    run_adb "${INSTALL_TIMEOUT_SECONDS}" install -r "${TEST_APK}" || return $?

    printf 'Running instrumentation tests...\n'
    run_instrumentation || return $?

    printf 'Launching application smoke test...\n'
    run_adb "${SMOKE_TIMEOUT_SECONDS}" shell am force-stop "${APP_ID}" || return $?
    run_adb "${SMOKE_TIMEOUT_SECONDS}" shell am start -W -n "${LAUNCHER_COMPONENT}" \
        > "${DIAGNOSTICS_DIR}/launcher-start.log" || return $?
    run_timed "${DEVICE_TIMEOUT_SECONDS}" sleep 3 || return $?
    verify_launcher_focus || return $?
    verify_workspace_hierarchy || return $?
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
    printf 'API 24 instrumentation and Launcher smoke checks passed\n'
}

main "$@"
