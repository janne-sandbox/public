package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutionPlanTest {
    private final CalculationContext context = CalculationContext.builder()
            .precision(100)
            .roundingModeCode(0)
            .maxOperations(1000)
            .maxMemoryBytes(8192)
            .timeout(Duration.ofSeconds(2))
            .build();

    @Test
    void createsDeterministicVersionedPlanAndPreservesBatchOrder() {
        ExecutionPlan first = ExecutionPlan.create(List.of("1+1", "sqrt(4)"), context);
        ExecutionPlan second = ExecutionPlan.create(List.of("1+1", "sqrt(4)"), context);

        assertEquals(ExecutionPlan.CURRENT_FORMAT_VERSION, first.formatVersion());
        assertEquals(ExecutionPlan.CURRENT_ENGINE_VERSION, first.engineVersion());
        assertEquals(List.of("1+1", "sqrt(4)"), first.canonicalExpressions());
        assertEquals(GpuMode.DISABLED, first.gpuMode());
        assertEquals(64, first.planId().length());
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void lengthPrefixesPreventAmbiguousBatchDigests() {
        ExecutionPlan splitOneWay = ExecutionPlan.create(List.of("a", "bc"), context);
        ExecutionPlan splitAnotherWay = ExecutionPlan.create(List.of("ab", "c"), context);

        assertNotEquals(splitOneWay.planId(), splitAnotherWay.planId());
    }

    @Test
    void gpuModeParticipatesInPlanIdentity() {
        CalculationContext detected = CalculationContext.builder()
                .precision(100)
                .gpuMode(GpuMode.DETECT)
                .roundingModeCode(0)
                .maxOperations(1000)
                .maxMemoryBytes(8192)
                .timeout(Duration.ofSeconds(2))
                .build();

        assertNotEquals(
                ExecutionPlan.create("1+1", context).planId(),
                ExecutionPlan.create("1+1", detected).planId());
    }

    @Test
    void defensivelyCopiesAndRejectsInvalidExpressions() {
        List<String> mutable = new ArrayList<>(List.of("1+1"));
        ExecutionPlan plan = ExecutionPlan.create(mutable, context);
        mutable.set(0, "2+2");

        assertEquals(List.of("1+1"), plan.canonicalExpressions());
        assertThrows(UnsupportedOperationException.class,
                () -> plan.canonicalExpressions().add("3+3"));
        assertThrows(IllegalArgumentException.class,
                () -> ExecutionPlan.create(List.of(), context));
        assertThrows(IllegalArgumentException.class,
                () -> ExecutionPlan.create(List.of(" "), context));
    }
}
