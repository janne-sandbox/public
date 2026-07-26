ARG GPGPU_SIM_BASE_IMAGE=tgrogers/accel-sim_regress:Ubuntu-22.04-cuda-11.7@sha256:128088e209122f0f3b6020333a0b44617031057a83f8df4c02ace5f5fede65eb
ARG GPGPU_SIM_PLATFORM=linux/amd64
FROM --platform=${GPGPU_SIM_PLATFORM} ${GPGPU_SIM_BASE_IMAGE}

ARG GPGPU_SIM_REPOSITORY=https://github.com/gpgpu-sim/gpgpu-sim_distribution.git
ARG GPGPU_SIM_COMMIT=a4ce3feac901c97a4b4601f679e43cf3589c79de

RUN git clone --filter=blob:none "${GPGPU_SIM_REPOSITORY}" /opt/gpgpu-sim \
    && git -C /opt/gpgpu-sim checkout --detach "${GPGPU_SIM_COMMIT}" \
    && git -C /opt/gpgpu-sim rev-parse HEAD

WORKDIR /opt/gpgpu-sim

RUN /bin/bash -lc 'source ./setup_environment release && make -j"$(nproc)"'

COPY simulator-smoke.sh /opt/math2/simulator-smoke.sh

LABEL org.opencontainers.image.title="Math2 GPGPU-Sim development environment" \
      org.opencontainers.image.description="Pinned CUDA 11.7 functional-simulation toolchain; not hardware evidence" \
      org.opencontainers.image.source="https://github.com/gpgpu-sim/gpgpu-sim_distribution" \
      org.opencontainers.image.revision="${GPGPU_SIM_COMMIT}"

CMD ["/bin/bash"]
