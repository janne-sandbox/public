package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReferenceCpuBackendTest {
    @Test
    void executesOrderedBatchThroughReferenceEngine() {
        ExecutionPlan plan = ExecutionPlan.create(
                List.of("1+1", "max(2,3)", "sqrt(16)", "round(1.235,2,1)"),
                CalculationContext.builder().precision(20).build());
        ReferenceCpuBackend backend = new ReferenceCpuBackend();

        ExecutionResult result = backend.execute(plan);

        assertTrue(backend.supports(plan).isSupported());
        assertEquals(plan.planId(), result.planId());
        assertEquals(ReferenceCpuBackend.BACKEND_ID, result.backendId());
        assertEquals(List.of(
                        Number.fromInteger(2),
                        Number.fromInteger(3),
                        Number.fromInteger(4),
                        Number.fromString("1.24")),
                result.results());
        assertEquals(0, result.transferDurationNanos());
    }

    @Test
    void preservesHighPrecisionAndRationalReduction() {
        ExecutionPlan plan = ExecutionPlan.create(
                List.of("pi", "(1/3)*3"),
                CalculationContext.builder().precision(1000).build());

        ExecutionResult result = new ReferenceCpuBackend().execute(plan);

        assertEquals(1001, result.results().get(0).toString().length());
        assertEquals("1", result.results().get(1).toString());
    }
}
