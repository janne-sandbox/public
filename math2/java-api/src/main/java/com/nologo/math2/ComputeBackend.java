package com.nologo.math2;

import java.util.Objects;

/** Vendor-independent execution backend SPI. */
public interface ComputeBackend extends AutoCloseable {
    BackendCapabilities capabilities();

    default PlanSupport supports(ExecutionPlan plan) {
        return capabilities().supportFor(Objects.requireNonNull(plan, "plan"));
    }

    ExecutionResult execute(ExecutionPlan plan);

    @Override
    default void close() {
        // Most backends own no resources. Native backends override idempotently.
    }
}
