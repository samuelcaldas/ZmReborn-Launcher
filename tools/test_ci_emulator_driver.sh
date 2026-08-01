#!/usr/bin/env bash
# Behavioral contract for hosted emulator smoke and ANR handling.
set -Eeuo pipefail

readonly DRIVER="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/run_ci_emulator_tests.sh"
readonly FIXTURE_DIR="$(mktemp -d)"
trap 'rm -rf -- "${FIXTURE_DIR}"' EXIT

fail() {
    printf 'CI emulator driver test failed: %s\n' "$1" >&2
    exit 1
}

assert_status() {
    local expected="$1"
    local actual="$2"
    local context="$3"
    [[ "${actual}" -eq "${expected}" ]] \
        || fail "${context}: expected status ${expected}, got ${actual}"
}

write_focus() {
    printf '%s\n' "$1" > "${FIXTURE_DIR}/focus.log"
}

source "${DRIVER}"

FAKE_FOCUS_OUTPUT=""
FAKE_ADB_STATUS=0
FAKE_DISMISS_COUNT=0
FAKE_KEYEVENT_STATUS=0
FAKE_UIAUTOMATOR_STATUS=0
FAKE_PULL_STATUS=0
FAKE_HIERARCHY_CONTENT=""
FAKE_SLEEP_ADVANCE=0
FAKE_SLEEP_ARG=0

run_adb() {
    local seconds="$1"
    shift
    [[ "${seconds}" -gt 0 ]] || return 98
    if [[ "$*" == "shell dumpsys window" ]]; then
        [[ "${FAKE_ADB_STATUS}" -eq 0 ]] || return "${FAKE_ADB_STATUS}"
        printf '%s\n' "${FAKE_FOCUS_OUTPUT}"
        return
    fi
    if [[ "$*" == "shell input keyevent KEYCODE_ENTER" ]]; then
        [[ "${FAKE_KEYEVENT_STATUS}" -eq 0 ]] || return "${FAKE_KEYEVENT_STATUS}"
        FAKE_DISMISS_COUNT=$((FAKE_DISMISS_COUNT + 1))
        FAKE_FOCUS_OUTPUT='  mCurrentFocus=Window{1 u0 org.zmreborn/org.zmreborn.Launcher}'
        return
    fi
    if [[ "$1" == "shell" && "$2" == "uiautomator" ]]; then
        return "${FAKE_UIAUTOMATOR_STATUS}"
    fi
    if [[ "$1" == "pull" && "$2" == "/sdcard/window_dump.xml" ]]; then
        [[ "${FAKE_PULL_STATUS}" -eq 0 ]] || return "${FAKE_PULL_STATUS}"
        printf '%s' "${FAKE_HIERARCHY_CONTENT}" > "$3"
        return
    fi
    return 97
}

sleep() {
    FAKE_SLEEP_ARG="$1"
    SECONDS=$((SECONDS + FAKE_SLEEP_ADVANCE))
    return 0
}

write_focus '  mCurrentFocus=Window{1 u0 Application Not Responding: org.example:worker}'
parsed="$(current_anr_package "${FIXTURE_DIR}/focus.log")"
[[ "${parsed}" == "org.example:worker" ]] || fail "valid process suffix was not parsed"

write_focus '  mCurrentFocus=Window{1 u0 org.zmreborn/org.zmreborn.Launcher}'
set +e
current_anr_package "${FIXTURE_DIR}/focus.log" >/dev/null
status=$?
set -e
assert_status 1 "${status}" "absent ANR"

write_focus '  mCurrentFocus=Window{1 u0 Application Not Responding: invalid-name!}'
set +e
current_anr_package "${FIXTURE_DIR}/focus.log" >/dev/null
status=$?
set -e
assert_status 2 "${status}" "malformed ANR"

FAKE_FOCUS_OUTPUT='  mCurrentFocus=Window{1 u0 Application Not Responding: com.android.launcher3}'
dismiss_foreign_anr "$((SECONDS + 30))" "${FIXTURE_DIR}/probe.log"
assert_status 1 "${FAKE_DISMISS_COUNT}" "foreign ANR dismissal"

FAKE_FOCUS_OUTPUT='  mCurrentFocus=Window{1 u0 Application Not Responding: org.zmreborn:core}'
FAKE_DISMISS_COUNT=0
if dismiss_foreign_anr "$((SECONDS + 30))" "${FIXTURE_DIR}/probe.log" 2>/dev/null; then
    fail "Launcher subprocess ANR must fail"
fi
assert_status 0 "${FAKE_DISMISS_COUNT}" "Launcher ANR action count"

FAKE_FOCUS_OUTPUT='  mCurrentFocus=Window{1 u0 Application Not Responding: broken-name!}'
if dismiss_foreign_anr "$((SECONDS + 30))" "${FIXTURE_DIR}/probe.log" 2>/dev/null; then
    fail "malformed focused ANR must fail"
fi

FAKE_ADB_STATUS=42
set +e
dismiss_foreign_anr "$((SECONDS + 30))" "${FIXTURE_DIR}/probe.log" 2>/dev/null
status=$?
set -e
assert_status 42 "${status}" "ADB probe failure"

FAKE_ADB_STATUS=0
FAKE_FOCUS_OUTPUT='  mCurrentFocus=Window{1 u0 Application Not Responding: com.android.launcher3}'
FAKE_KEYEVENT_STATUS=13
set +e
dismiss_foreign_anr "$((SECONDS + 30))" "${FIXTURE_DIR}/probe.log" 2>/dev/null
status=$?
set -e
FAKE_KEYEVENT_STATUS=0
assert_status 13 "${status}" "failed dismissal key event must propagate"

# remaining_smoke_seconds: reserves kill-after grace and caps to the per-command ceiling.
seconds="$(remaining_smoke_seconds "$((SECONDS + 100))")"
assert_status "${SMOKE_COMMAND_TIMEOUT_SECONDS}" "${seconds}" "command seconds must cap at ceiling"

seconds="$(remaining_smoke_seconds "$((SECONDS + 6))")"
assert_status 1 "${seconds}" "command seconds must reserve kill-after grace"

set +e
remaining_smoke_seconds "$((SECONDS + 5))" >/dev/null 2>&1
status=$?
set -e
assert_status 1 "${status}" "deadline within grace window must fail"

# smoke_sleep: clamps requested duration to the remaining deadline budget.
smoke_sleep "$((SECONDS + 3))" 10
assert_status 3 "${FAKE_SLEEP_ARG}" "smoke_sleep must clamp to remaining budget"

set +e
smoke_sleep "$((SECONDS - 1))" 5 2>/dev/null
status=$?
set -e
assert_status 1 "${status}" "smoke_sleep past its deadline must fail"

# verify_launcher_focus: real pass/reject paths, not just a name grep.
FAKE_FOCUS_OUTPUT='  mCurrentFocus=Window{1 u0 org.zmreborn/org.zmreborn.Launcher}'
verify_launcher_focus "$((SECONDS + 30))" "${FIXTURE_DIR}/vf-pass.log"
grep -Fq 'org.zmreborn/org.zmreborn.Launcher' "${FIXTURE_DIR}/vf-pass.log" \
    || fail "verify_launcher_focus did not capture the accepted focus"

FAKE_FOCUS_OUTPUT='  mCurrentFocus=Window{1 u0 com.other/.Main}'
FAKE_SLEEP_ADVANCE=100
set +e
verify_launcher_focus "$((SECONDS + 5))" "${FIXTURE_DIR}/vf-fail.log" 2>/dev/null
status=$?
set -e
FAKE_SLEEP_ADVANCE=0
assert_status 1 "${status}" "mismatched focus must fail, not accept mFocusedApp"

# wait_for_workspace_hierarchy: real pass/reject paths against pulled hierarchy content.
FAKE_HIERARCHY_CONTENT='<hierarchy><node resource-id="org.zmreborn:id/workspace" /></hierarchy>'
wait_for_workspace_hierarchy "$((SECONDS + 30))" "${FIXTURE_DIR}/hierarchy-pass.xml"
grep -Fq 'org.zmreborn:id/workspace' "${FIXTURE_DIR}/hierarchy-pass.xml" \
    || fail "wait_for_workspace_hierarchy did not pull expected content"

FAKE_HIERARCHY_CONTENT='<hierarchy></hierarchy>'
FAKE_SLEEP_ADVANCE=100
set +e
wait_for_workspace_hierarchy "$((SECONDS + 5))" "${FIXTURE_DIR}/hierarchy-fail.xml" 2>/dev/null
status=$?
set -e
FAKE_SLEEP_ADVANCE=0
assert_status 1 "${status}" "missing workspace id must fail"

# A foreign ANR blocking the poll itself (not just present beforehand) must be dismissed in place,
# reproducing the hosted failure where the ANR appeared before any launcher focus was ever observed.
FAKE_DISMISS_COUNT=0
FAKE_FOCUS_OUTPUT='  mCurrentFocus=Window{1 u0 Application Not Responding: com.android.systemui}'
verify_launcher_focus "$((SECONDS + 30))" "${FIXTURE_DIR}/vf-anr.log"
assert_status 1 "${FAKE_DISMISS_COUNT}" "verify_launcher_focus must dismiss a foreign ANR blocking its own poll"
grep -Fq 'org.zmreborn/org.zmreborn.Launcher' "${FIXTURE_DIR}/vf-anr.log" \
    || fail "verify_launcher_focus did not confirm focus after dismissing the blocking ANR"

FAKE_DISMISS_COUNT=0
FAKE_FOCUS_OUTPUT='  mCurrentFocus=Window{1 u0 Application Not Responding: com.android.systemui}'
FAKE_UIAUTOMATOR_STATUS=0
FAKE_PULL_STATUS=0
FAKE_HIERARCHY_CONTENT='<hierarchy><node resource-id="org.zmreborn:id/workspace" /></hierarchy>'
wait_for_workspace_hierarchy "$((SECONDS + 30))" "${FIXTURE_DIR}/hierarchy-anr.xml"
assert_status 1 "${FAKE_DISMISS_COUNT}" "wait_for_workspace_hierarchy must dismiss a foreign ANR blocking its own poll"

printf 'CI emulator driver behavioral contract passed\n'
