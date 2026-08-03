#!/usr/bin/env bash
# Shared API, diagnostics, and logcat helpers for emulator test execution.

require_command() {
    command -v "$1" >/dev/null 2>&1 || die "required command not found: $1"
}

validate_expected_api_level() {
    local api_level="$1"
    [[ "${api_level}" =~ ^[1-9][0-9]*$ ]] \
        || die "EXPECTED_API_LEVEL must be a positive integer: ${api_level:-empty}"
}

validate_diagnostics_path() {
    local approved_root="$1"
    local requested_path="$2"
    [[ -d "${approved_root}" ]] || die "diagnostics root is not a directory: ${approved_root}"
    local canonical_root
    local canonical_path
    canonical_root="$(realpath -e -- "${approved_root}")" \
        || die "cannot resolve diagnostics root: ${approved_root}"
    canonical_path="$(realpath -m -- "${requested_path}")" \
        || die "cannot resolve diagnostics path: ${requested_path}"
    [[ "${canonical_root}" != / ]] || die "filesystem root cannot be a diagnostics root"
    [[ "${canonical_path}" == "${canonical_root}/"* ]] \
        || die "diagnostics path escaped approved root: ${requested_path}"
}

verify_device_api_level() {
    local expected_api="$1"
    local actual_api
    actual_api="$(run_adb "${DEVICE_TIMEOUT_SECONDS}" \
        shell getprop ro.build.version.sdk)" || return $?
    actual_api="${actual_api//$'\r'/}"
    [[ "${actual_api}" =~ ^[1-9][0-9]*$ ]] \
        || die "device returned invalid ro.build.version.sdk: ${actual_api:-empty}"
    ACTUAL_API_LEVEL="${actual_api}"
    [[ "${actual_api}" == "${expected_api}" ]] \
        || die "expected Android API ${expected_api}, device reports ${actual_api}"
}

format_success_message() {
    printf 'API %s instrumentation and Launcher smoke checks passed\n' "$1"
}

logcat_has_app_failure() {
    local log_file="$1"
    local escaped_app_id="${APP_ID//./\\.}"
    local markers='FATAL EXCEPTION|ANR in|VerifyError|NoClassDefFoundError|ClassNotFoundException|NoSuchMethodError|UnsupportedOperationException|AppWidget[^: ]*.*(failed|failure|error)'
    grep -Eiq "(${escaped_app_id}.*(${markers})|(${markers}).*${escaped_app_id})" \
        "${log_file}"
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
        printf 'expected_api_level=%s\n' "${EXPECTED_API_LEVEL}"
        printf 'actual_api_level=%s\n' "${ACTUAL_API_LEVEL}"
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
    validate_diagnostics_path "${DIAGNOSTICS_ROOT}" "${DIAGNOSTICS_DIR}"
    rm -rf -- "${DIAGNOSTICS_DIR}"
    mkdir -p "${DIAGNOSTICS_DIR}"
}

collect_diagnostics() {
    printf 'Collecting bounded emulator diagnostics before teardown...\n' >&2
    capture_diagnostic logcat.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" logcat -d -v threadtime
    capture_diagnostic window.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" shell dumpsys window
    capture_diagnostic activities.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" shell dumpsys activity activities
    capture_diagnostic processes.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" shell ps
    capture_diagnostic package.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" shell dumpsys package "${APP_ID}"
    capture_diagnostic hierarchy-dump.log "${DIAGNOSTIC_TIMEOUT_SECONDS}" \
        shell uiautomator dump /sdcard/failure_window_dump.xml
    run_adb "${DIAGNOSTIC_TIMEOUT_SECONDS}" pull /sdcard/failure_window_dump.xml \
        "${DIAGNOSTICS_DIR}/failure_window_dump.xml" \
        > "${DIAGNOSTICS_DIR}/hierarchy-pull.log" 2>&1 || true
    capture_screenshot
}

verify_success_logcat() {
    run_adb "${DIAGNOSTIC_TIMEOUT_SECONDS}" logcat -d -v threadtime \
        > "${SUCCESS_LOGCAT}" 2>&1 || return $?
    if logcat_has_app_failure "${SUCCESS_LOGCAT}"; then
        die "filtered logcat contains an app runtime failure marker"
        return 1
    fi
}
