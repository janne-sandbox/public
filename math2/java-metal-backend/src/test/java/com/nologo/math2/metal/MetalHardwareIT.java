package com.nologo.math2.metal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nologo.math2.CalculationContext;
import com.nologo.math2.ComputeBackend;
import com.nologo.math2.ExecutionPlan;
import org.junit.jupiter.api.Test;

/**
 * Explicit macOS hardware discovery test. The default Surefire patterns do not
 * execute this class; run it only after building the native bridge.
 */
class MetalHardwareIT {
    @Test
    void opensDefaultMetalDeviceAndReleasesIt() {
        MetalBackendProvider provider = new MetalBackendProvider();

        try (ComputeBackend backend = provider.create()) {
            assertTrue(backend.capabilities().accelerated());
            assertTrue(backend.capabilities().backendId().startsWith("math2-metal:"));
            assertTrue(backend.capabilities().maximumBufferBytes() > 0);
            assertFalse(backend.supports(ExecutionPlan.create(
                    "1+1", CalculationContext.builder().build())).isSupported());
        }
    }
}
