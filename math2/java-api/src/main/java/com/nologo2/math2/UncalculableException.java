package com.nologo2.math2;

import java.util.Objects;
import java.util.OptionalInt;

/** Indicates input or a calculation that cannot produce a valid Math2 number. */
public class UncalculableException extends RuntimeException {
    private final CalculationError reason;
    private final Integer position;

    public UncalculableException(CalculationError reason, String message) {
        this(reason, message, null, null);
    }

    public UncalculableException(CalculationError reason, String message, Throwable cause) {
        this(reason, message, null, cause);
    }

    public UncalculableException(CalculationError reason, String message, int position) {
        this(reason, message, position, null);
    }

    private UncalculableException(
            CalculationError reason, String message, Integer position, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason");
        if (position != null && position < 0) {
            throw new IllegalArgumentException("position must not be negative");
        }
        this.position = position;
    }

    public CalculationError getReason() {
        return reason;
    }

    public OptionalInt getPosition() {
        return position == null ? OptionalInt.empty() : OptionalInt.of(position);
    }
}
