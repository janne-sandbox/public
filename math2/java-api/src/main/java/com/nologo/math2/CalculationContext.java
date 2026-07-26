package com.nologo.math2;

import java.time.Duration;
import java.util.Objects;

/** Immutable limits and execution preferences for one calculation or ordered batch. */
public final class CalculationContext {
    public static final long DEFAULT_MAX_OPERATIONS = 1_000_000L;
    public static final long DEFAULT_MAX_MEMORY_BYTES = 256L * 1024L * 1024L;
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final int precision;
    private final GpuMode gpuMode;
    private final int roundingModeCode;
    private final long maxOperations;
    private final long maxMemoryBytes;
    private final Duration timeout;

    private CalculationContext(Builder builder) {
        this.precision = builder.precision == null ? Number.getMaxLength() : builder.precision;
        this.gpuMode = Objects.requireNonNull(builder.gpuMode, "gpuMode");
        this.roundingModeCode = builder.roundingModeCode == null
                ? Number.getDefaultRoundingModeCode()
                : builder.roundingModeCode;
        this.maxOperations = builder.maxOperations;
        this.maxMemoryBytes = builder.maxMemoryBytes;
        this.timeout = Objects.requireNonNull(builder.timeout, "timeout");
        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int precision() {
        return precision;
    }

    public GpuMode gpuMode() {
        return gpuMode;
    }

    public int roundingModeCode() {
        return roundingModeCode;
    }

    public long maxOperations() {
        return maxOperations;
    }

    public long maxMemoryBytes() {
        return maxMemoryBytes;
    }

    public Duration timeout() {
        return timeout;
    }

    private void validate() {
        if (precision < 1 || precision > Number.getMaxLength()) {
            throw new IllegalArgumentException(
                    "Precision must be between 1 and " + Number.getMaxLength());
        }
        if (roundingModeCode < 0 || roundingModeCode >= Number.ROUNDING_MODE_COUNT) {
            throw new IllegalArgumentException(
                    "Rounding mode code must be between 0 and "
                            + (Number.ROUNDING_MODE_COUNT - 1));
        }
        if (maxOperations < 1) {
            throw new IllegalArgumentException("Maximum operations must be positive");
        }
        if (maxMemoryBytes < 1) {
            throw new IllegalArgumentException("Maximum memory bytes must be positive");
        }
        final long timeoutMillis;
        try {
            timeoutMillis = timeout.toMillis();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Timeout is too large", exception);
        }
        if (timeout.isNegative() || timeout.isZero() || timeoutMillis < 1) {
            throw new IllegalArgumentException(
                    "Timeout must be representable as at least one positive millisecond");
        }
    }

    public static final class Builder {
        private Integer precision;
        private GpuMode gpuMode = GpuMode.DISABLED;
        private Integer roundingModeCode;
        private long maxOperations = DEFAULT_MAX_OPERATIONS;
        private long maxMemoryBytes = DEFAULT_MAX_MEMORY_BYTES;
        private Duration timeout = DEFAULT_TIMEOUT;

        private Builder() {
        }

        public Builder precision(int value) {
            this.precision = value;
            return this;
        }

        public Builder gpuMode(GpuMode value) {
            this.gpuMode = Objects.requireNonNull(value, "gpuMode");
            return this;
        }

        public Builder roundingModeCode(int value) {
            this.roundingModeCode = value;
            return this;
        }

        public Builder maxOperations(long value) {
            this.maxOperations = value;
            return this;
        }

        public Builder maxMemoryBytes(long value) {
            this.maxMemoryBytes = value;
            return this;
        }

        public Builder timeout(Duration value) {
            this.timeout = Objects.requireNonNull(value, "timeout");
            return this;
        }

        public CalculationContext build() {
            return new CalculationContext(this);
        }
    }
}
