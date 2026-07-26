package com.nologo.math2.metal;

/** Immutable identity and memory bound for one discovered Metal device. */
public record MetalDevice(String registryId, String name, long memoryLimitBytes) {
    public MetalDevice {
        if (registryId == null || registryId.isBlank()) {
            throw new IllegalArgumentException("Metal registry ID must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Metal device name must not be blank");
        }
        if (memoryLimitBytes < 1) {
            throw new IllegalArgumentException("Metal memory limit must be positive");
        }
    }
}
