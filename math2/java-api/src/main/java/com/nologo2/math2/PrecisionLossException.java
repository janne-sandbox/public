package com.nologo2.math2;

import java.util.Objects;

/** Reports a requested primitive conversion that would lose information. */
public final class PrecisionLossException extends UncalculableException {
    private final Number source;
    private final Class<?> targetType;
    private final java.lang.Number nearestValue;

    public PrecisionLossException(
            Number source, Class<?> targetType, java.lang.Number nearestValue) {
        super(
                CalculationError.PRECISION_EXCEEDED,
                "Cannot convert " + source + " to " + targetType.getSimpleName()
                        + " without precision loss; nearest value is " + nearestValue);
        this.source = Objects.requireNonNull(source, "source");
        this.targetType = Objects.requireNonNull(targetType, "targetType");
        this.nearestValue = Objects.requireNonNull(nearestValue, "nearestValue");
    }

    public Number getSource() {
        return source;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    public java.lang.Number getNearestValue() {
        return nearestValue;
    }
}
