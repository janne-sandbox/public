package com.nologo.math2;

import java.util.Objects;
import java.util.Optional;

/** Complete-plan capability decision with a diagnostic-only rejection reason. */
public final class PlanSupport {
    private static final PlanSupport SUPPORTED = new PlanSupport(true, null);

    private final boolean supported;
    private final String reason;

    private PlanSupport(boolean supported, String reason) {
        this.supported = supported;
        this.reason = reason;
    }

    public static PlanSupport supported() {
        return SUPPORTED;
    }

    public static PlanSupport unsupported(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Unsupported plan reason must not be blank");
        }
        return new PlanSupport(false, reason);
    }

    public boolean isSupported() {
        return supported;
    }

    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof PlanSupport that
                && supported == that.supported
                && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supported, reason);
    }

    @Override
    public String toString() {
        return supported ? "supported" : "unsupported: " + reason;
    }
}
