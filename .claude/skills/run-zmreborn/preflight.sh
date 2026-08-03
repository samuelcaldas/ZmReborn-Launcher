#!/usr/bin/env bash
# Validates Docker emulator inputs and daemon-host prerequisites.

validate_port() {
    [[ "$1" =~ ^[1-9][0-9]{0,4}$ ]] && (( 10#$1 <= 65535 )) \
        || fail "$2 must be an integer from 1 to 65535: $1"
}

validate_output_directory() {
    local artifacts_root
    local resolved_output
    [[ "${OUT_DIR}" == /* ]] || fail "OUT_DIR must be an absolute path: ${OUT_DIR}"
    artifacts_root="$(realpath -m "${ROOT_DIR}/.android-emulator-artifacts")"
    resolved_output="$(realpath -m "${OUT_DIR}")"
    [[ "${resolved_output}" == "${artifacts_root}/"* ]] \
        || fail "OUT_DIR must stay under ${artifacts_root}: ${OUT_DIR}"
}

validate_identity_inputs() {
    [[ "${API_LEVEL}" == 35 || "${API_LEVEL}" == 36 ]] \
        || fail "API_LEVEL must be 35 or 36: ${API_LEVEL}"
    [[ "${SYSTEM_IMAGE}" == "system-images;android-${API_LEVEL};"* ]] \
        || fail "SYSTEM_IMAGE does not match API_LEVEL ${API_LEVEL}: ${SYSTEM_IMAGE}"
    [[ "${CONTAINER}" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]*$ ]] \
        || fail "Invalid container name: ${CONTAINER}"
    [[ "${AVD_NAME}" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]*$ ]] \
        || fail "Invalid AVD name: ${AVD_NAME}"
}

validate_local_prerequisites() {
    command -v realpath >/dev/null 2>&1 || fail "realpath command not found"
    validate_output_directory
    [[ -f "${ENTRYPOINT}" ]] || fail "Emulator entrypoint not found: ${ENTRYPOINT}"
    validate_port "${ADB_HOST_PORT}" ADB_HOST_PORT
    validate_port "${NOVNC_HOST_PORT}" NOVNC_HOST_PORT
    [[ "${ADB_HOST_PORT}" != "${NOVNC_HOST_PORT}" ]] \
        || fail "ADB_HOST_PORT and NOVNC_HOST_PORT must differ"
    command -v docker >/dev/null 2>&1 || fail "docker command not found"
    command -v sha256sum >/dev/null 2>&1 || fail "sha256sum command not found"
}

validate_docker_access() {
    docker_cli context inspect "${DOCKER_CONTEXT}" >/dev/null 2>&1 \
        || fail "Docker context unavailable: ${DOCKER_CONTEXT}"
    docker_cli info >/dev/null 2>&1 \
        || fail "Docker daemon unavailable: ${DOCKER_CONTEXT}"
    mkdir -p "${OUT_DIR}"
}

validate_inputs() {
    validate_identity_inputs
    validate_local_prerequisites
    validate_docker_access
}

validate_test_kvm() {
    local device="${KVM_DEVICE:-/dev/kvm}"
    local image_id
    image_id="$(runtime_image_id)"
    docker_cli run --rm --pull=never \
        --device "${device}:/dev/kvm" --entrypoint /bin/bash "${image_id}" \
        -ec '[[ -c /dev/kvm && -r /dev/kvm && -w /dev/kvm ]]' \
        >/dev/null 2>&1 \
        || fail "Docker daemon cannot expose usable KVM device: ${device}"
}

run_daemon_mount_probe() {
    local image_id="$1" runner_hash="$2" entrypoint_hash="$3" relative_probe="$4"
    docker_cli run --rm --pull=never \
        -e "EXPECTED_RUNNER_HASH=${runner_hash}" \
        -e "EXPECTED_ENTRYPOINT_HASH=${entrypoint_hash}" \
        -e "PROBE_PATH=/artifacts/${relative_probe}" \
        -e "RESPONSE_PATH=/artifacts/${relative_probe}.daemon" \
        -v "${ROOT_DIR}:/workspace:ro" -v "${OUT_DIR}:/artifacts" \
        -v "${ENTRYPOINT}:/entrypoint.sh:ro" \
        --entrypoint /bin/bash "${image_id}" \
        -ec 'read -r runner _ < <(sha256sum /workspace/tools/run_ci_emulator_tests.sh); read -r entrypoint _ < <(sha256sum /entrypoint.sh); [[ "$runner" == "$EXPECTED_RUNNER_HASH" && "$entrypoint" == "$EXPECTED_ENTRYPOINT_HASH" && -f "$PROBE_PATH" && -w /artifacts ]] && printf daemon > "$RESPONSE_PATH"' \
        >/dev/null 2>&1
}

validate_daemon_mounts() {
    local image_id runner_hash entrypoint_hash probe response relative_probe status=0
    image_id="$(runtime_image_id)"
    read -r runner_hash _ < <(sha256sum "${ROOT_DIR}/tools/run_ci_emulator_tests.sh")
    read -r entrypoint_hash _ < <(sha256sum "${ENTRYPOINT}")
    probe="$(mktemp "${OUT_DIR}/.workspace-probe.XXXXXX")"
    response="${probe}.daemon"
    relative_probe="${probe#${OUT_DIR}/}"
    run_daemon_mount_probe "${image_id}" "${runner_hash}" \
        "${entrypoint_hash}" "${relative_probe}" || status=$?
    [[ -f "${response}" ]] || status=1
    rm -f -- "${probe}" "${response}"
    (( status == 0 )) || fail "Docker daemon cannot share current source and writable artifacts"
}
