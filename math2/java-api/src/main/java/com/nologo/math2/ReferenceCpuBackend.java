package com.nologo.math2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Reference execution backend backed by Math2's existing Java decimal engine. */
public final class ReferenceCpuBackend implements ComputeBackend {
    public static final String BACKEND_ID = "math2-cpu";
    public static final String IMPLEMENTATION_VERSION = "1";

    private static final Set<Integer> ROUNDING_MODES = Set.of(0, 1, 2, 3, 4, 5, 6);
    private static final BackendCapabilities CAPABILITIES = new BackendCapabilities(
            BACKEND_ID,
            IMPLEMENTATION_VERSION,
            false,
            Set.of(ExecutionPlan.CURRENT_FORMAT_VERSION),
            1,
            Integer.MAX_VALUE,
            ROUNDING_MODES,
            Integer.MAX_VALUE,
            Long.MAX_VALUE);

    @Override
    public BackendCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public ExecutionResult execute(ExecutionPlan plan) {
        PlanSupport support = supports(plan);
        if (!support.isSupported()) {
            throw new IllegalArgumentException(
                    support.reason().orElse("Execution plan is unsupported"));
        }

        long started = System.nanoTime();
        List<Number> results = new ArrayList<>(plan.canonicalExpressions().size());
        for (String expression : plan.canonicalExpressions()) {
            results.add(Equation.parse(expression).calculateReference(plan.precision()));
        }
        long duration = System.nanoTime() - started;
        return new ExecutionResult(
                plan.planId(),
                plan.engineVersion(),
                BACKEND_ID,
                results,
                duration,
                0);
    }
}
