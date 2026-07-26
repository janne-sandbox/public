package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BackendCapabilitiesTest {
    private static final Set<Integer> ALL_ROUNDING = Set.of(0, 1, 2, 3, 4, 5, 6);

    @Test
    void acceptsPlanWithinEveryCapabilityBoundary() {
        BackendCapabilities capabilities = capabilities(1, 1000, 2, 8192);
        ExecutionPlan plan = plan(100, 0, 4096, "1+1", "2+2");

        assertTrue(capabilities.supportFor(plan).isSupported());
        assertEquals(Set.of(1), capabilities.planFormatVersions());
        assertThrows(UnsupportedOperationException.class,
                () -> capabilities.planFormatVersions().add(2));
    }

    @Test
    void rejectsPrecisionRoundingBatchAndMemoryIndependently() {
        BackendCapabilities capabilities =
                new BackendCapabilities("fake", "1", true, Set.of(1),
                        10, 100, Set.of(0), 1, 1000);

        assertFalse(capabilities.supportFor(plan(9, 0, 1000, "1")).isSupported());
        assertFalse(capabilities.supportFor(plan(10, 1, 1000, "1")).isSupported());
        assertFalse(capabilities.supportFor(plan(10, 0, 1000, "1", "2")).isSupported());
        assertFalse(capabilities.supportFor(plan(10, 0, 1001, "1")).isSupported());
    }

    @Test
    void rejectsMalformedCapabilityDescriptors() {
        assertThrows(IllegalArgumentException.class,
                () -> new BackendCapabilities("", "1", false, Set.of(1),
                        1, 10, Set.of(0), 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new BackendCapabilities("cpu", "1", false, Set.of(),
                        1, 10, Set.of(0), 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new BackendCapabilities("cpu", "1", false, Set.of(1),
                        10, 1, Set.of(0), 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new BackendCapabilities("cpu", "1", false, Set.of(1),
                        1, 10, Set.of(99), 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new BackendCapabilities("cpu", "1", false, Set.of(1),
                        1, 10, Set.of(0), 0, 1));
    }

    private static BackendCapabilities capabilities(
            int minPrecision, int maxPrecision, int maxBatch, long maxBytes) {
        return new BackendCapabilities(
                "fake", "1.0", true, Set.of(1), minPrecision, maxPrecision,
                ALL_ROUNDING, maxBatch, maxBytes);
    }

    private static ExecutionPlan plan(
            int precision, int rounding, long memory, String... expressions) {
        CalculationContext context = CalculationContext.builder()
                .precision(precision)
                .roundingModeCode(rounding)
                .maxMemoryBytes(memory)
                .timeout(Duration.ofSeconds(1))
                .build();
        return ExecutionPlan.create(java.util.List.of(expressions), context);
    }
}
