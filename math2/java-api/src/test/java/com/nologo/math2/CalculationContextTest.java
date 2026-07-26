package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CalculationContextTest {
    @Test
    void capturesSafeDefaultsAtBuildTime() {
        CalculationContext context = CalculationContext.builder().build();

        assertEquals(Number.getMaxLength(), context.precision());
        assertEquals(GpuMode.DISABLED, context.gpuMode());
        assertEquals(Number.getDefaultRoundingModeCode(), context.roundingModeCode());
        assertEquals(CalculationContext.DEFAULT_MAX_OPERATIONS, context.maxOperations());
        assertEquals(CalculationContext.DEFAULT_MAX_MEMORY_BYTES, context.maxMemoryBytes());
        assertEquals(CalculationContext.DEFAULT_TIMEOUT, context.timeout());
    }

    @Test
    void preservesEveryExplicitSetting() {
        CalculationContext context = CalculationContext.builder()
                .precision(64)
                .gpuMode(GpuMode.DETECT)
                .roundingModeCode(1)
                .maxOperations(42)
                .maxMemoryBytes(4096)
                .timeout(Duration.ofMillis(250))
                .build();

        assertEquals(64, context.precision());
        assertEquals(GpuMode.DETECT, context.gpuMode());
        assertEquals(1, context.roundingModeCode());
        assertEquals(42, context.maxOperations());
        assertEquals(4096, context.maxMemoryBytes());
        assertEquals(Duration.ofMillis(250), context.timeout());
    }

    @Test
    void rejectsInvalidExternallyControlledLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculationContext.builder().precision(0).build());
        assertThrows(IllegalArgumentException.class,
                () -> CalculationContext.builder()
                        .precision(Number.getMaxLength() + 1).build());
        assertThrows(IllegalArgumentException.class,
                () -> CalculationContext.builder().roundingModeCode(-1).build());
        assertThrows(IllegalArgumentException.class,
                () -> CalculationContext.builder()
                        .roundingModeCode(Number.ROUNDING_MODE_COUNT).build());
        assertThrows(IllegalArgumentException.class,
                () -> CalculationContext.builder().maxOperations(0).build());
        assertThrows(IllegalArgumentException.class,
                () -> CalculationContext.builder().maxMemoryBytes(0).build());
        assertThrows(IllegalArgumentException.class,
                () -> CalculationContext.builder().timeout(Duration.ZERO).build());
        assertThrows(IllegalArgumentException.class,
                () -> CalculationContext.builder().timeout(Duration.ofNanos(1)).build());
        assertThrows(NullPointerException.class,
                () -> CalculationContext.builder().gpuMode(null));
    }
}
