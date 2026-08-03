#!/usr/bin/env bash
# Shared Launcher focus, ANR, and hierarchy helpers for emulator smoke checks.

remaining_smoke_seconds() {
    local deadline="$1"
    local remaining=$((deadline - SECONDS))
    (( remaining > SMOKE_KILL_GRACE_SECONDS )) || {
        die "Launcher smoke deadline expired"
        return 1
    }
    remaining=$((remaining - SMOKE_KILL_GRACE_SECONDS))
    (( remaining <= SMOKE_COMMAND_TIMEOUT_SECONDS )) \
        || remaining="${SMOKE_COMMAND_TIMEOUT_SECONDS}"
    printf '%s\n' "${remaining}"
}

run_smoke_adb() {
    local deadline="$1"
    shift
    local seconds
    seconds="$(remaining_smoke_seconds "${deadline}")" || return $?
    run_adb "${seconds}" "$@"
}

smoke_sleep() {
    local deadline="$1"
    local seconds="$2"
    local remaining=$((deadline - SECONDS))
    (( remaining > 0 )) || {
        die "Launcher smoke deadline expired"
        return 1
    }
    (( seconds <= remaining )) || seconds="${remaining}"
    sleep "${seconds}"
}

current_anr_package() {
    local focus_log="$1"
    local line
    local package_name
    while IFS= read -r line; do
        [[ "${line}" == *"mCurrentFocus="*"Application Not Responding: "* ]] || continue
        package_name="${line#*Application Not Responding: }"
        package_name="${package_name%%\}*}"
        [[ "${package_name}" =~ ^[A-Za-z0-9_]+([.][A-Za-z0-9_]+)*(:[A-Za-z0-9_.]+)?$ ]] \
            || return 2
        printf '%s\n' "${package_name}"
        return
    done < "${focus_log}"
    return 1
}

is_launcher_process() {
    local process_name="$1"
    [[ "${process_name}" == "${APP_ID}" || "${process_name}" == "${APP_ID}:"* ]]
}

dismiss_foreign_anr() {
    local deadline="$1"
    local focus_log="${2:-${DIAGNOSTICS_DIR}/pre-smoke-focused-window.log}"
    local package_name
    local parser_status
    local attempt
    for attempt in {1..3}; do
        run_smoke_adb "${deadline}" shell dumpsys window > "${focus_log}" || return $?
        parser_status=0
        package_name="$(current_anr_package "${focus_log}")" || parser_status=$?
        if [[ "${parser_status}" -ne 0 ]]; then
            [[ "${parser_status}" -eq 1 ]] && return 0
            die "focused ANR process name is malformed"
            return 1
        fi
        if is_launcher_process "${package_name}"; then
            die "Launcher is blocked by its own Application Not Responding dialog"
            return 1
        fi
        printf 'Dismissing foreign ANR dialog from %s...\n' "${package_name}"
        run_smoke_adb "${deadline}" shell input keyevent KEYCODE_ENTER || return $?
        smoke_sleep "${deadline}" 2 || return $?
    done
    die "foreign Application Not Responding dialog remained focused"
}

verify_launcher_focus() {
    local deadline="$1"
    local focus_log="${2:-${DIAGNOSTICS_DIR}/focused-window.log}"
    local escaped_app_id="${APP_ID//./\\.}"
    local component_pattern="${escaped_app_id}/(\\.Launcher|${escaped_app_id}\\.Launcher)([[:space:]}]|$)"
    while (( SECONDS < deadline )); do
        dismiss_foreign_anr "${deadline}" "${focus_log}" || return $?
        run_smoke_adb "${deadline}" shell dumpsys window > "${focus_log}" || return $?
        grep -Eq "^[[:space:]]*mCurrentFocus=.*${component_pattern}" "${focus_log}" && return
        smoke_sleep "${deadline}" 1 || return $?
    done
    printf 'Expected current Launcher focus %s; accepted default %s\n' \
        "${LAUNCHER_COMPONENT}" "${DEFAULT_LAUNCHER_COMPONENT}" >&2
    grep -E '^[[:space:]]*(mCurrentFocus|mFocusedApp)=' "${focus_log}" >&2 || true
    return 1
}

wait_for_workspace_hierarchy() {
    local deadline="$1"
    local hierarchy="${2:-${DIAGNOSTICS_DIR}/window_dump.xml}"
    local anr_focus_log
    anr_focus_log="$(dirname -- "${hierarchy}")/workspace-poll-focused-window.log"
    while (( SECONDS < deadline )); do
        dismiss_foreign_anr "${deadline}" "${anr_focus_log}" || return $?
        run_smoke_adb "${deadline}" shell uiautomator dump /sdcard/window_dump.xml || return $?
        run_smoke_adb "${deadline}" pull /sdcard/window_dump.xml "${hierarchy}" || return $?
        grep -Fq "${APP_ID}:id/workspace" "${hierarchy}" && return
        smoke_sleep "${deadline}" 1 || return $?
    done
    die "workspace id missing from UI hierarchy"
}
