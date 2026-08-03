#!/usr/bin/env bash
# Behavioral contract for API, diagnostics, and successful-logcat validation.
set -Eeuo pipefail

readonly ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly DRIVER="${ROOT}/run_ci_emulator_tests.sh"
readonly FIXTURE_DIR="$(mktemp -d)"
export EXPECTED_API_LEVEL=36
export DIAGNOSTICS_ROOT="${FIXTURE_DIR}"
export DIAGNOSTICS_DIR="${FIXTURE_DIR}/diagnostics"
trap 'rm -rf -- "${FIXTURE_DIR}"' EXIT

fail() {
    printf 'CI emulator runtime contract failed: %s\n' "$1" >&2
    exit 1
}

assert_failure() {
    local context="$1"
    shift
    set +e
    "$@" >/dev/null 2>&1
    local status=$?
    set -e
    [[ "${status}" -eq 1 ]] || fail "${context}: expected status 1, got ${status}"
}

source "${DRIVER}"
FAKE_API_LEVEL=36

run_adb() {
    local seconds="$1"
    shift
    [[ "${seconds}" -gt 0 ]] || return 98
    if [[ "$*" == "shell getprop ro.build.version.sdk" ]]; then
        printf '%s\n' "${FAKE_API_LEVEL}"
        return
    fi
    return 97
}

validate_expected_api_level 36
for invalid_api in '' 0 android-36 '36 '; do
    assert_failure "invalid expected API ${invalid_api:-empty}" \
        validate_expected_api_level "${invalid_api}"
done

verify_device_api_level 36
FAKE_API_LEVEL=35
assert_failure "mismatched runtime API" verify_device_api_level 36
FAKE_API_LEVEL=36

validate_diagnostics_path "${FIXTURE_DIR}" "${FIXTURE_DIR}/api36/e2e-diagnostics"
for unsafe_path in "${FIXTURE_DIR}" "${FIXTURE_DIR}/../escaped" /; do
    assert_failure "unsafe diagnostics path ${unsafe_path}" \
        validate_diagnostics_path "${FIXTURE_DIR}" "${unsafe_path}"
done

readonly LOGCAT_FIXTURE="${FIXTURE_DIR}/success-logcat.log"
printf '%s\n' 'ActivityTaskManager: Displayed org.zmreborn/.Launcher' > "${LOGCAT_FIXTURE}"
logcat_has_app_failure "${LOGCAT_FIXTURE}" && fail "clean app logcat was rejected"

for failure_line in \
    'AndroidRuntime: org.zmreborn FATAL EXCEPTION: main' \
    'ActivityManager: ANR in org.zmreborn' \
    'org.zmreborn VerifyError while loading Launcher' \
    'org.zmreborn NoClassDefFoundError: android.example.NewApi' \
    'org.zmreborn NoSuchMethodError: android.example.Api.call' \
    'org.zmreborn UnsupportedOperationException in Launcher' \
    'AppWidgetService: failed binding org.zmreborn test provider'; do
    printf '%s\n' "${failure_line}" > "${LOGCAT_FIXTURE}"
    logcat_has_app_failure "${LOGCAT_FIXTURE}" \
        || fail "app failure marker was accepted: ${failure_line}"
done
printf '%s\n' 'AndroidRuntime: com.example FATAL EXCEPTION: main' > "${LOGCAT_FIXTURE}"
logcat_has_app_failure "${LOGCAT_FIXTURE}" && fail "unrelated package failure was rejected"

mkdir -p "${DIAGNOSTICS_DIR}"
ACTUAL_API_LEVEL=36
record_status passed 0
grep -Fq 'expected_api_level=36' "${DIAGNOSTICS_DIR}/run-status.txt" \
    || fail "run status omitted expected API"
grep -Fq 'actual_api_level=36' "${DIAGNOSTICS_DIR}/run-status.txt" \
    || fail "run status omitted actual API"
[[ "$(format_success_message 36)" == 'API 36 instrumentation and Launcher smoke checks passed' ]] \
    || fail "success message did not use configured API"

printf 'CI emulator runtime behavioral contract passed\n'
