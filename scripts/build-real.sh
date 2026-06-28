#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

IMAGE_NAME="${J2ME_BUILD_IMAGE:-monomicro-j2me-build}"
PLATFORM="${J2ME_BUILD_PLATFORM:-linux/amd64}"

docker build --platform "${PLATFORM}" -f Dockerfile.j2me-build -t "${IMAGE_NAME}" .
docker run --rm --platform "${PLATFORM}" -v "$(pwd):/workspace" "${IMAGE_NAME}" ant clean real-jar
