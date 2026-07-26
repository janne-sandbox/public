#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SIMULATOR_DIR="$SCRIPT_DIR/cuda-simulator"
DEFAULT_IMAGE="math2-gpgpu-sim:cuda-11.7"
DEFAULT_PLATFORM="linux/amd64"
DEFAULT_BASE_IMAGE="tgrogers/accel-sim_regress:Ubuntu-22.04-cuda-11.7@sha256:128088e209122f0f3b6020333a0b44617031057a83f8df4c02ace5f5fede65eb"
DEFAULT_SOURCE_COMMIT="a4ce3feac901c97a4b4601f679e43cf3589c79de"

IMAGE="${MATH2_CUDA_SIMULATOR_IMAGE:-$DEFAULT_IMAGE}"
PLATFORM="${MATH2_CUDA_SIMULATOR_PLATFORM:-$DEFAULT_PLATFORM}"
BASE_IMAGE="${MATH2_CUDA_SIMULATOR_BASE_IMAGE:-$DEFAULT_BASE_IMAGE}"
SOURCE_COMMIT="${MATH2_GPGPU_SIM_COMMIT:-$DEFAULT_SOURCE_COMMIT}"
NO_CACHE=false

usage() {
  cat <<'USAGE'
Usage: ./install-cuda-simulator.sh [--no-cache]

Build and smoke-test the pinned Math2 GPGPU-Sim development image.

Environment overrides:
  MATH2_CUDA_SIMULATOR_IMAGE       Local output image tag
  MATH2_CUDA_SIMULATOR_PLATFORM    Docker platform (default: linux/amd64)
  MATH2_CUDA_SIMULATOR_BASE_IMAGE  Upstream CUDA toolchain image
  MATH2_GPGPU_SIM_COMMIT           GPGPU-Sim source commit

Expect multi-gigabyte downloads and substantially slower builds when Docker
must emulate amd64 on an ARM host. Simulator success is not GPU hardware proof.
USAGE
}

case "${1:-}" in
  "")
    ;;
  --no-cache)
    NO_CACHE=true
    ;;
  --help|-h)
    usage
    exit 0
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required to install the CUDA simulator." >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "Docker is installed, but its daemon is unavailable." >&2
  exit 1
fi

HOST_ARCH="$(docker info --format '{{.Architecture}}')"
if [[ "$PLATFORM" == "linux/amd64" && "$HOST_ARCH" != "x86_64" && "$HOST_ARCH" != "amd64" ]]; then
  echo "Warning: Docker host architecture is $HOST_ARCH; building $PLATFORM through emulation."
fi

echo "Building CUDA simulator image: $IMAGE"
echo "Platform: $PLATFORM"
echo "Base image: $BASE_IMAGE"
echo "GPGPU-Sim commit: $SOURCE_COMMIT"
echo "This build can download and unpack more than 5 GB."

BUILD_OPTIONS=(
  --platform "$PLATFORM"
  --build-arg "GPGPU_SIM_BASE_IMAGE=$BASE_IMAGE"
  --build-arg "GPGPU_SIM_PLATFORM=$PLATFORM"
  --build-arg "GPGPU_SIM_COMMIT=$SOURCE_COMMIT"
  --tag "$IMAGE"
  --file "$SIMULATOR_DIR/Dockerfile"
)
if [[ "$NO_CACHE" == true ]]; then
  BUILD_OPTIONS+=(--no-cache)
fi

docker build "${BUILD_OPTIONS[@]}" "$SIMULATOR_DIR"
docker run --rm \
  --platform "$PLATFORM" \
  --env "MATH2_EXPECTED_GPGPU_SIM_COMMIT=$SOURCE_COMMIT" \
  --entrypoint /bin/bash \
  "$IMAGE" \
  /opt/math2/simulator-smoke.sh

echo "Math2 CUDA simulator image installed and smoke-tested: $IMAGE"
