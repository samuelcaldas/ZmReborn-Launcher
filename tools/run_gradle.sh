#!/usr/bin/env bash
set -Eeuo pipefail

readonly ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
readonly DOCKER_CONTEXT="${DOCKER_CONTEXT:-docker-dev}"
readonly DOCKER_IMAGE="${DOCKER_IMAGE:-zeam-docker-dev:android35}"
readonly GRADLE_CACHE_VOLUME="${GRADLE_CACHE_VOLUME:-zeam-gradle-cache}"
readonly ANDROID_USER_HOME_VOLUME="${ANDROID_USER_HOME_VOLUME:-zeam-android-user-home}"
readonly JAVA_HOME_IN_CONTAINER="${JAVA_HOME_IN_CONTAINER:-/usr/lib/jvm/java-17-openjdk-amd64}"

docker_cli() {
    env -u DOCKER_HOST docker "$@"
}

image_id="$(docker_cli --context "${DOCKER_CONTEXT}" image inspect --format '{{.Id}}' "${DOCKER_IMAGE}")"

docker_cli --context "${DOCKER_CONTEXT}" run --rm --pull=never --user root \
    -e HOME=/root -e GRADLE_USER_HOME=/root/.gradle \
    -e JAVA_HOME="${JAVA_HOME_IN_CONTAINER}" -e TZ=America/Sao_Paulo \
    -v "${ROOT_DIR}:/workspace" -v "${GRADLE_CACHE_VOLUME}:/root/.gradle" \
    -v "${ANDROID_USER_HOME_VOLUME}:/root/.android" -w /workspace \
    "${image_id}" ./gradlew "$@"
