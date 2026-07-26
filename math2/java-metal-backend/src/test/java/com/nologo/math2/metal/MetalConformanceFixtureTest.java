package com.nologo.math2.metal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nologo.math2.BackendCapabilities;
import com.nologo.math2.CalculationContext;
import com.nologo.math2.ComputeBackend;
import com.nologo.math2.ExecutionPlan;
import com.nologo.math2.ExecutionResult;
import com.nologo.math2.ReferenceCpuBackend;
import com.nologo.math2.UncalculableException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MetalConformanceFixtureTest {
    private static final List<String> OPERATION_VECTORS = List.of(
            "7+5",
            "7-5",
            "7*5",
            "7/5",
            "2^10",
            "sqrt(2)",
            "abs(-7)",
            "min(7,5)",
            "max(7,5)",
            "log(2)",
            "sin(0.5)",
            "cos(0.5)",
            "tan(0.5)",
            "round(1/3,8,0)",
            "pi");

    @Test
    void deterministicMetalDoubleMatchesCpuExactlyAcrossContextMatrix() {
        ComputeBackend cpu = new ReferenceCpuBackend();
        ComputeBackend accelerated = new ExactMetalDouble(cpu);

        for (int precision : List.of(20, 50)) {
            for (int roundingMode = 0; roundingMode < 7; roundingMode++) {
                ExecutionPlan plan = ExecutionPlan.create(
                        OPERATION_VECTORS,
                        CalculationContext.builder()
                                .precision(precision)
                                .roundingModeCode(roundingMode)
                                .build());

                ExecutionResult expected = cpu.execute(plan);
                ExecutionResult actual = accelerated.execute(plan);

                assertEquals(
                        expected.results().stream().map(Object::toString).toList(),
                        actual.results().stream().map(Object::toString).toList());
                assertEquals(OPERATION_VECTORS.size(), actual.results().size());
            }
        }
    }

    @Test
    void deterministicMetalDoublePreservesReferenceFailures() {
        ExecutionPlan plan = ExecutionPlan.create(
                "1/0", CalculationContext.builder().build());
        ComputeBackend accelerated = new ExactMetalDouble(
                new ReferenceCpuBackend());

        UncalculableException expected = assertThrows(
                UncalculableException.class,
                () -> new ReferenceCpuBackend().execute(plan));
        UncalculableException actual = assertThrows(
                UncalculableException.class,
                () -> accelerated.execute(plan));

        assertEquals(expected.getReason(), actual.getReason());
    }

    private static final class ExactMetalDouble implements ComputeBackend {
        private static final String BACKEND_ID = "deterministic-metal-double";
        private final ComputeBackend reference;
        private final BackendCapabilities capabilities = new BackendCapabilities(
                BACKEND_ID,
                "test",
                true,
                Set.of(ExecutionPlan.CURRENT_FORMAT_VERSION),
                1,
                Integer.MAX_VALUE,
                Set.of(0, 1, 2, 3, 4, 5, 6),
                Integer.MAX_VALUE,
                Long.MAX_VALUE);

        private ExactMetalDouble(ComputeBackend reference) {
            this.reference = reference;
        }

        @Override
        public BackendCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public ExecutionResult execute(ExecutionPlan plan) {
            ExecutionResult cpu = reference.execute(plan);
            return new ExecutionResult(
                    plan.planId(),
                    plan.engineVersion(),
                    BACKEND_ID,
                    cpu.results(),
                    cpu.executionDurationNanos(),
                    0);
        }
    }
}
