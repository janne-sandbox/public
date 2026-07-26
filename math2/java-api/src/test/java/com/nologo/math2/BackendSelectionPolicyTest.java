package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BackendSelectionPolicyTest {
    private final BackendSelectionPolicy policy = new BackendSelectionPolicy();

    @Test
    void keepsScalarAndSmallBatchOnCpu() {
        assertFalse(policy.mayAccelerate(plan(List.of("1+1"), 1000)));
        assertFalse(policy.mayAccelerate(plan(List.of("1", "2"), 10)));
    }

    @Test
    void selectsOnlySupportedAcceleratedBackendForEnoughDeterministicWork() {
        ExecutionPlan plan = plan(List.of("1+1", "2+2"), 200);
        BackendCapabilities accelerated = capabilities(true, 1000);
        BackendCapabilities cpu = capabilities(false, 1000);
        BackendCapabilities tooSmall = capabilities(true, 1);

        assertTrue(policy.mayAccelerate(plan));
        assertTrue(policy.isSuitable(plan, accelerated));
        assertFalse(policy.isSuitable(plan, cpu));
        assertFalse(policy.isSuitable(plan, tooSmall));
    }

    private static ExecutionPlan plan(List<String> expressions, int precision) {
        return ExecutionPlan.create(
                expressions,
                CalculationContext.builder()
                        .precision(precision)
                        .gpuMode(GpuMode.DETECT)
                        .build());
    }

    private static BackendCapabilities capabilities(boolean accelerated, int maxBatch) {
        return new BackendCapabilities(
                accelerated ? "gpu" : "cpu",
                "1",
                accelerated,
                Set.of(1),
                1,
                Integer.MAX_VALUE,
                Set.of(0, 1, 2, 3, 4, 5, 6),
                maxBatch,
                Long.MAX_VALUE);
    }
}
