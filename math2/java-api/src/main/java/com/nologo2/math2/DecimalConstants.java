package com.nologo2.math2;

/** Internal precision-aware construction of mathematical constants. */
final class DecimalConstants {
    private DecimalConstants() {
    }

    static Number pi(int precision) {
        if (precision < 1 || precision > Number.getMaxLength()) {
            throw new UncalculableException(
                    CalculationError.PRECISION_EXCEEDED,
                    "Precision must be between 1 and " + Number.getMaxLength());
        }
        return DecimalArithmetic.pi(precision);
    }
}
