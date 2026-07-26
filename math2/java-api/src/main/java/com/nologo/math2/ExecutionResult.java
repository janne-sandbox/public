package com.nologo.math2;

import java.util.List;
import java.util.Objects;

/** Immutable ordered output and diagnostics returned by an execution backend. */
public final class ExecutionResult {
    private final String planId;
    private final int engineVersion;
    private final String backendId;
    private final List<Number> results;
    private final long executionDurationNanos;
    private final long transferDurationNanos;

    public ExecutionResult(
            String planId,
            int engineVersion,
            String backendId,
            List<Number> results,
            long executionDurationNanos,
            long transferDurationNanos) {
        this.planId = requireText(planId, "planId");
        if (engineVersion < 1) {
            throw new IllegalArgumentException("Engine version must be positive");
        }
        this.engineVersion = engineVersion;
        this.backendId = requireText(backendId, "backendId");
        Objects.requireNonNull(results, "results");
        if (results.isEmpty() || results.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Results must be nonempty and contain no nulls");
        }
        this.results = List.copyOf(results);
        if (executionDurationNanos < 0 || transferDurationNanos < 0) {
            throw new IllegalArgumentException("Durations must not be negative");
        }
        this.executionDurationNanos = executionDurationNanos;
        this.transferDurationNanos = transferDurationNanos;
    }

    public String planId() {
        return planId;
    }

    public int engineVersion() {
        return engineVersion;
    }

    public String backendId() {
        return backendId;
    }

    public List<Number> results() {
        return results;
    }

    public long executionDurationNanos() {
        return executionDurationNanos;
    }

    public long transferDurationNanos() {
        return transferDurationNanos;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
