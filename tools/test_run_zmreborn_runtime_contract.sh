#!/usr/bin/env bash
# Behavioral contract for Docker runtime reuse boundaries.
set -Eeuo pipefail

readonly REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source "${REPO_ROOT}/.claude/skills/run-zmreborn/runtime.sh"

fail_test() {
    printf 'run-zmreborn runtime contract failed: %s\n' "$1" >&2
    exit 1
}

container_exists() {
    return 1
}

set +e
validate_existing_runtime 1
status=$?
set -e
[[ "${status}" -eq 0 ]] \
    || fail_test "missing container must allow creation, got status ${status}"

assert_invalid_input() {
    local expected="$1"
    shift
    local output
    set +e
    output="$(env "$@" bash -c \
        'source "$1"; validate_inputs' _ \
        "${REPO_ROOT}/.claude/skills/run-zmreborn/runtime.sh" 2>&1)"
    status=$?
    set -e
    [[ "${status}" -ne 0 && "${output}" == "ERROR: ${expected}"* ]] \
        || fail_test "expected invalid input: ${expected}"
}

assert_invalid_input 'ADB_HOST_PORT and NOVNC_HOST_PORT must differ' \
    ADB_HOST_PORT=5555 NOVNC_HOST_PORT=5555
assert_invalid_input 'OUT_DIR must stay under' OUT_DIR=/etc
assert_invalid_input 'ADB_HOST_PORT must be an integer from 1 to 65535' \
    ADB_HOST_PORT=18446744073709551617

printf 'run-zmreborn runtime behavioral contract passed\n'
