package com.nologo.math2;

import java.util.Objects;
import java.util.Set;

/** Immutable capabilities used for deterministic complete-plan validation. */
public final class BackendCapabilities {
    private final String backendId;
    private final String implementationVersion;
    private final boolean accelerated;
    private final Set<Integer> planFormatVersions;
    private final int minimumPrecision;
    private final int maximumPrecision;
    private final Set<Integer> roundingModeCodes;
    private final int maximumBatchSize;
    private final long maximumBufferBytes;

    public BackendCapabilities(
            String backendId,
            String implementationVersion,
            boolean accelerated,
            Set<Integer> planFormatVersions,
            int minimumPrecision,
            int maximumPrecision,
            Set<Integer> roundingModeCodes,
            int maximumBatchSize,
            long maximumBufferBytes) {
        this.backendId = requireText(backendId, "backendId");
        this.implementationVersion = requireText(implementationVersion, "implementationVersion");
        this.accelerated = accelerated;
        this.planFormatVersions = requirePositiveSet(planFormatVersions, "planFormatVersions");
        this.minimumPrecision = minimumPrecision;
        this.maximumPrecision = maximumPrecision;
        this.roundingModeCodes = requireRoundingModes(roundingModeCodes);
        this.maximumBatchSize = maximumBatchSize;
        this.maximumBufferBytes = maximumBufferBytes;
        validateLimits();
    }

    public String backendId() {
        return backendId;
    }

    public String implementationVersion() {
        return implementationVersion;
    }

    public boolean accelerated() {
        return accelerated;
    }

    public Set<Integer> planFormatVersions() {
        return planFormatVersions;
    }

    public int minimumPrecision() {
        return minimumPrecision;
    }

    public int maximumPrecision() {
        return maximumPrecision;
    }

    public Set<Integer> roundingModeCodes() {
        return roundingModeCodes;
    }

    public int maximumBatchSize() {
        return maximumBatchSize;
    }

    public long maximumBufferBytes() {
        return maximumBufferBytes;
    }

    public PlanSupport supportFor(ExecutionPlan plan) {
        Objects.requireNonNull(plan, "plan");
        if (!planFormatVersions.contains(plan.formatVersion())) {
            return PlanSupport.unsupported(
                    "Unsupported execution-plan format " + plan.formatVersion());
        }
        if (plan.precision() < minimumPrecision || plan.precision() > maximumPrecision) {
            return PlanSupport.unsupported(
                    "Precision " + plan.precision() + " is outside backend range");
        }
        if (!roundingModeCodes.contains(plan.roundingModeCode())) {
            return PlanSupport.unsupported(
                    "Rounding mode " + plan.roundingModeCode() + " is unsupported");
        }
        if (plan.canonicalExpressions().size() > maximumBatchSize) {
            return PlanSupport.unsupported(
                    "Batch size " + plan.canonicalExpressions().size() + " exceeds backend limit");
        }
        if (plan.maxMemoryBytes() > maximumBufferBytes) {
            return PlanSupport.unsupported(
                    "Plan memory budget exceeds backend buffer limit");
        }
        return PlanSupport.supported();
    }

    private void validateLimits() {
        if (minimumPrecision < 1 || maximumPrecision < minimumPrecision) {
            throw new IllegalArgumentException(
                    "Precision range must be positive and ordered");
        }
        if (maximumBatchSize < 1) {
            throw new IllegalArgumentException("Maximum batch size must be positive");
        }
        if (maximumBufferBytes < 1) {
            throw new IllegalArgumentException("Maximum buffer bytes must be positive");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static Set<Integer> requirePositiveSet(Set<Integer> values, String name) {
        Objects.requireNonNull(values, name);
        if (values.isEmpty() || values.stream().anyMatch(value -> value == null || value < 1)) {
            throw new IllegalArgumentException(name + " must contain positive values");
        }
        return Set.copyOf(values);
    }

    private static Set<Integer> requireRoundingModes(Set<Integer> values) {
        Objects.requireNonNull(values, "roundingModeCodes");
        if (values.isEmpty() || values.stream().anyMatch(value -> value == null
                || value < 0 || value >= Number.ROUNDING_MODE_COUNT)) {
            throw new IllegalArgumentException(
                    "Rounding modes must contain only supported mode codes");
        }
        return Set.copyOf(values);
    }
}
