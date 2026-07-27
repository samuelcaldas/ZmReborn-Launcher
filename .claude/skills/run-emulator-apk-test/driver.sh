#!/usr/bin/env bash
# driver.sh — Zeam APK emulator test driver
# All commands verified against zeam-docker-emulator:android35 on 2026-07-27.
set -Eeuo pipefail

CONTAINER="${CONTAINER:-zeam-runtime}"
OUT_DIR="${OUT_DIR:-/tmp/zeam-captures}"
EXEC="env -u DOCKER_HOST docker --context docker-dev exec ${CONTAINER}"
APK_PATH="${1:-}"

mkdir -p "${OUT_DIR}"

# ─── helpers ──────────────────────────────────────────────────────────────────

shot() {
    local name="$1"
    local dest="${OUT_DIR}/${name}.png"
    ${EXEC} adb exec-out screencap -p > "${dest}"
    echo "→ ${dest}"
}

tap()       { ${EXEC} adb shell input tap "$1" "$2"; }
back()      { ${EXEC} adb shell input keyevent 4; }
home()      { ${EXEC} adb shell input keyevent 3; }
longpress() { ${EXEC} adb shell input swipe "$1" "$2" "$1" "$2" 2000; }

ui_dump() {
    ${EXEC} adb shell uiautomator dump /data/local/tmp/ui.xml 2>/dev/null
    ${EXEC} adb exec-out cat /data/local/tmp/ui.xml
}

wait_boot() {
    echo "Waiting for emulator boot…"
    until ${EXEC} adb shell getprop sys.boot_completed 2>/dev/null | grep -q "^1$"; do
        sleep 3
    done
    ${EXEC} adb shell wm dismiss-keyguard 2>/dev/null || true
    echo "Boot complete."
}

restore_launcher() {
    # After a crash the system shows "Select a Home app". Select ZM Reborn.
    local dump
    dump=$(ui_dump)
    if echo "${dump}" | grep -q "Select a Home app"; then
        echo "⚠ Launcher crashed — restoring ZM Reborn…"
        # Tap ZM Reborn row (upper half of chooser list, y≈1448 on 1080×1920)
        tap 540 1448
        sleep 1
        # Tap Always button (bounds [853,1694][1033,1836])
        tap 943 1765
        sleep 2
    fi
}

launch_prefs() {
    ${EXEC} adb shell am start -n org.zmreborn/.Preferences
    sleep 3
    restore_launcher
}

# ─── install ──────────────────────────────────────────────────────────────────

cmd_install() {
    [[ -z "${APK_PATH}" ]] && { echo "Usage: $0 <apk-path>"; exit 1; }
    local remote="/data/local/tmp/zeam-test.apk"
    echo "Copying APK…"
    docker --context docker-dev cp "${APK_PATH}" "${CONTAINER}:${remote}"
    ${EXEC} adb install -r "${remote}"
    echo "Installed."
}

# ─── screenshot all screens ──────────────────────────────────────────────────

cmd_screenshot_all() {
    ${EXEC} adb root 2>/dev/null || true
    sleep 1

    # Homescreen
    home; sleep 2
    shot "01-homescreen"

    # App drawer (button at 464,1820 on pixel_3a 1080×1920)
    tap 464 1820; sleep 1
    shot "02-app-drawer"
    ${EXEC} adb shell input swipe 540 800 540 200 500; sleep 1
    shot "03-app-drawer-scrolled"
    home; sleep 1

    # Long-press workspace context menu
    longpress 540 900; sleep 1
    shot "04-longpress-menu"

    # Add submenu (row center y=792 from UI dump)
    tap 540 792; sleep 1
    shot "05-add-submenu"
    back; sleep 1

    # Long-press menu → Wallpaper (row center y=961)
    longpress 540 900; sleep 1
    tap 540 961; sleep 2
    shot "06-wallpaper-chooser"
    back; sleep 1

    # Preferences root (use adb root + am start — not exported)
    echo "Opening Preferences…"
    launch_prefs
    shot "07-prefs-root"

    # Workspace sub-screen (bounds [0,168][1080,336], center y=252)
    tap 540 252; sleep 2
    shot "08-prefs-workspace"
    ${EXEC} adb shell input swipe 540 1400 540 600 500; sleep 1
    shot "09-prefs-workspace-scrolled"
    back; sleep 2

    # Applications grid sub-screen (bounds [0,336][1080,504], center y=420)
    launch_prefs
    tap 540 420; sleep 2
    shot "10-prefs-apps-grid"
    back; sleep 1

    # Action bindings sub-screen (bounds [0,504][1080,672], center y=588)
    launch_prefs
    tap 540 588; sleep 2
    shot "11-prefs-action-bindings"
    back; sleep 1

    # Dock sub-screen (bounds [0,672][1080,840], center y=756)
    launch_prefs
    tap 540 756; sleep 2
    shot "12-prefs-dock"
    back; sleep 1; back; sleep 1

    echo "Done. Screenshots in ${OUT_DIR}/"
}

# ─── single shot ─────────────────────────────────────────────────────────────

cmd_shot() {
    shot "${1:-manual}"
}

# ─── dispatch ─────────────────────────────────────────────────────────────────

case "${1:-screenshot_all}" in
    install)        cmd_install ;;
    screenshot_all) cmd_screenshot_all ;;
    shot)           cmd_shot "${2:-}" ;;
    *)              echo "Commands: install <apk> | screenshot_all | shot <name>"; exit 1 ;;
esac
