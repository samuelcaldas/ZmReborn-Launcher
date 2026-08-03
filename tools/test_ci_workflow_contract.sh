#!/usr/bin/env bash
# Static contract for hosted API 24 instrumentation and diagnostics.
set -Eeuo pipefail

readonly ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
readonly WORKFLOW="${ROOT_DIR}/.github/workflows/ci.yml"
readonly DRIVER="${ROOT_DIR}/tools/run_ci_emulator_tests.sh"
readonly RUNTIME_SUPPORT="${ROOT_DIR}/tools/emulator_test_runtime.sh"
readonly SMOKE_SUPPORT="${ROOT_DIR}/tools/emulator_test_smoke.sh"
readonly DRIVER_TEST="${ROOT_DIR}/tools/test_ci_emulator_driver.sh"
readonly RUNTIME_TEST="${ROOT_DIR}/tools/test_ci_emulator_runtime_contract.sh"

fail() {
    printf 'CI workflow contract failed: %s\n' "$1" >&2
    exit 1
}

require_file() {
    [[ -f "$1" ]] || fail "missing file: $1"
}

require_executable() {
    [[ -x "$1" ]] || fail "file must be executable: $1"
}

require_text() {
    local file="$1"
    local expected="$2"
    grep -Fq -- "${expected}" "${file}" || fail "missing \`${expected}\` in ${file}"
}

reject_text() {
    local file="$1"
    local forbidden="$2"
    if grep -Fq -- "${forbidden}" "${file}"; then
        fail "forbidden \`${forbidden}\` in ${file}"
    fi
}

require_file "${WORKFLOW}"
require_file "${DRIVER}"
require_file "${RUNTIME_SUPPORT}"
require_file "${SMOKE_SUPPORT}"
require_file "${DRIVER_TEST}"
require_file "${RUNTIME_TEST}"
require_executable "${DRIVER}"
require_executable "${DRIVER_TEST}"
require_executable "${RUNTIME_TEST}"

require_text "${WORKFLOW}" "bash tools/test_ci_workflow_contract.sh"
require_text "${WORKFLOW}" "api-level: 24"
require_text "${WORKFLOW}" "EXPECTED_API_LEVEL: 24"
require_text "${WORKFLOW}" "script: ./tools/run_ci_emulator_tests.sh"
require_text "${WORKFLOW}" "path: e2e-diagnostics"
require_text "${WORKFLOW}" "if-no-files-found: warn"
reject_text "${WORKFLOW}" "script: |"
reject_text "${WORKFLOW}" "name: Collect diagnostics on failure"

require_text "${DRIVER}" "set -Eeuo pipefail"
require_text "${DRIVER}" 'ADB_TARGET_ARGS=(-s "${ANDROID_SERIAL}")'
require_text "${DRIVER}" "timeout --foreground"
require_text "${DRIVER}" "INSTRUMENTATION_CODE: -1"
require_text "${DRIVER}" "shortMsg=Process crashed"
require_text "${DRIVER}" "org.zmreborn/.Launcher"
require_text "${DRIVER}" "launcher-restart.log"
require_text "${DRIVER}" "primary_status"
require_text "${DRIVER}" "EXPECTED_API_LEVEL"
require_text "${DRIVER}" "DIAGNOSTICS_ROOT"
require_text "${DRIVER}" "success-logcat.log"
require_text "${RUNTIME_SUPPORT}" "ro.build.version.sdk"
require_text "${RUNTIME_SUPPORT}" "logcat_has_app_failure"
require_text "${RUNTIME_SUPPORT}" "expected_api_level"
require_text "${RUNTIME_SUPPORT}" "actual_api_level"
require_text "${RUNTIME_SUPPORT}" "collect_diagnostics"
require_text "${SMOKE_SUPPORT}" "mCurrentFocus"
require_text "${SMOKE_SUPPORT}" "mFocusedApp"
require_text "${SMOKE_SUPPORT}" "Application Not Responding:"
require_text "${SMOKE_SUPPORT}" "KEYCODE_ENTER"
require_text "${SMOKE_SUPPORT}" "dismiss_foreign_anr"
require_text "${SMOKE_SUPPORT}" "wait_for_workspace_hierarchy"
require_text "${SMOKE_SUPPORT}" "id/workspace"
require_text "${SMOKE_SUPPORT}" "SMOKE_KILL_GRACE_SECONDS"
require_text "${SMOKE_SUPPORT}" "smoke_sleep"

bash "${DRIVER_TEST}"
bash "${RUNTIME_TEST}"
printf 'CI workflow static contract passed\n'
