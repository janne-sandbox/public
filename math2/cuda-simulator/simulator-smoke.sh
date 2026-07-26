#!/usr/bin/env bash
set -euo pipefail

cd /opt/gpgpu-sim
source ./setup_environment release >/tmp/math2-gpgpu-sim-environment.log

EXPECTED_COMMIT="${MATH2_EXPECTED_GPGPU_SIM_COMMIT:-a4ce3feac901c97a4b4601f679e43cf3589c79de}"
ACTUAL_COMMIT="$(git rev-parse HEAD)"
if [[ "$ACTUAL_COMMIT" != "$EXPECTED_COMMIT" ]]; then
  echo "Unexpected GPGPU-Sim commit: $ACTUAL_COMMIT" >&2
  exit 1
fi

if ! command -v nvcc >/dev/null 2>&1; then
  echo "CUDA compiler is unavailable inside the simulator image." >&2
  exit 1
fi

SIMULATED_CUDART="$(find "$GPGPUSIM_ROOT/lib" -type f -name 'libcudart.so*' -print -quit)"
if [[ -z "$SIMULATED_CUDART" ]]; then
  echo "GPGPU-Sim CUDA runtime library was not built." >&2
  exit 1
fi

echo "GPGPU-Sim commit: $ACTUAL_COMMIT"
nvcc --version
echo "Simulated CUDA runtime: $SIMULATED_CUDART"
echo "GPGPU-Sim installation smoke test passed"
