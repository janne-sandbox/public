package com.nologo.math2.cuda;

/** Immutable identity and memory bound for one discovered CUDA device. */
public record CudaDevice(String id, String name, long totalMemoryBytes) {
    public CudaDevice {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("CUDA device ID must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("CUDA device name must not be blank");
        }
        if (totalMemoryBytes < 1) {
            throw new IllegalArgumentException("CUDA device memory must be positive");
        }
    }
}
