package com.nologo.math2;

import java.util.Objects;

/** Deterministic conservative suitability policy for optional acceleration. */
public final class BackendSelectionPolicy {
    public static final int MINIMUM_BATCH_SIZE = 2;
    public static final long MINIMUM_WORK_UNITS = 1_000L;

    public boolean mayAccelerate(ExecutionPlan plan) {
        Objects.requireNonNull(plan, "plan");
        return plan.canonicalExpressions().size() >= MINIMUM_BATCH_SIZE
                && estimatedWork(plan) >= MINIMUM_WORK_UNITS;
    }

    public boolean isSuitable(ExecutionPlan plan, BackendCapabilities capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        return capabilities.accelerated()
                && mayAccelerate(plan)
                && capabilities.supportFor(plan).isSupported();
    }

    long estimatedWork(ExecutionPlan plan) {
        long characters = 0;
        for (String expression : plan.canonicalExpressions()) {
            try {
                characters = Math.addExact(characters, expression.length());
            } catch (ArithmeticException overflow) {
                return Long.MAX_VALUE;
            }
        }
        try {
            return Math.multiplyExact(characters, (long) plan.precision());
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }
}
