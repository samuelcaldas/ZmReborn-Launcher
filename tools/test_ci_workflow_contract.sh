#!/usr/bin/env bash
# Static contract for hosted API 24 instrumentation and diagnostics.
set -Eeuo pipefail

readonly ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
readonly WORKFLOW="${ROOT_DIR}/.github/workflows/ci.yml"
readonly DRIVER="${ROOT_DIR}/tools/run_ci_emulator_tests.sh"
readonly DRIVER_TEST="${ROOT_DIR}/tools/test_ci_emulator_driver.sh"

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
require_file "${DRIVER_TEST}"
require_executable "${DRIVER}"
require_executable "${DRIVER_TEST}"

require_text "${WORKFLOW}" "bash tools/test_ci_workflow_contract.sh"
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
require_text "${DRIVER}" "mCurrentFocus"
require_text "${DRIVER}" "mFocusedApp"
require_text "${DRIVER}" "Application Not Responding:"
require_text "${DRIVER}" "KEYCODE_ENTER"
require_text "${DRIVER}" "dismiss_foreign_anr"
require_text "${DRIVER}" "launcher-restart.log"
require_text "${DRIVER}" "wait_for_workspace_hierarchy"
require_text "${DRIVER}" "id/workspace"
require_text "${DRIVER}" "collect_diagnostics"
require_text "${DRIVER}" "primary_status"
require_text "${DRIVER}" "SMOKE_KILL_GRACE_SECONDS"
require_text "${DRIVER}" "smoke_sleep"

bash "${DRIVER_TEST}"
printf 'CI workflow static contract passed\n'
