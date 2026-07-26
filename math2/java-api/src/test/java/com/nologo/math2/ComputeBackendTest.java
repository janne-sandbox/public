package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ComputeBackendTest {
    @Test
    void defaultSupportDelegatesToImmutableCapabilities() {
        ComputeBackend backend = new ComputeBackend() {
            @Override
            public BackendCapabilities capabilities() {
                return new BackendCapabilities(
                        "test-cpu", "1", false, Set.of(1), 1,
                        Number.getMaxLength(), Set.of(0, 1, 2, 3, 4, 5, 6),
                        1, CalculationContext.DEFAULT_MAX_MEMORY_BYTES);
            }

            @Override
            public ExecutionResult execute(ExecutionPlan plan) {
                return new ExecutionResult(
                        plan.planId(), plan.engineVersion(), "test-cpu",
                        List.of(Number.fromInteger(2)), 1, 0);
            }
        };
        ExecutionPlan plan = ExecutionPlan.create(
                "1+1", CalculationContext.builder().precision(10).build());

        assertTrue(backend.supports(plan).isSupported());
        assertEquals(Number.fromInteger(2), backend.execute(plan).results().getFirst());
        backend.close();
    }
}
