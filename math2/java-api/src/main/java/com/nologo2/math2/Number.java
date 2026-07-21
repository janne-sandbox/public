package com.nologo2.math2;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Immutable, string-backed arbitrary-precision decimal operand and result. */
public final class Number implements Comparable<Number> {
    public static final int DEFAULT_MAX_LENGTH = 1000;

    private static final Pattern DECIMAL = Pattern.compile(
            "([+-]?)(?:(\\d+)(?:\\.(\\d*))?|\\.(\\d+))(?:[eE]([+-]?\\d+))?");
    private static final AtomicInteger MAX_LENGTH = new AtomicInteger(DEFAULT_MAX_LENGTH);
    private static final Number ZERO = new Number("0");

    private final String value;

    private Number(String value) {
        this.value = value;
    }

    public static Number fromString(String value) {
        if (value == null || value.isBlank()) {
            throw invalidNumber("Number must not be blank", null);
        }
        try {
            String canonical = canonicalize(value.trim());
            return canonical.equals("0") ? ZERO : new Number(canonical);
        } catch (NumberFormatException exception) {
            throw invalidNumber("Invalid decimal number: " + value, exception);
        }
    }

    public static Number fromInteger(int value) {
        return fromString(Integer.toString(value));
    }

    public static Number fromLong(long value) {
        return fromString(Long.toString(value));
    }

    public static Number fromFloat(float value) {
        if (!Float.isFinite(value)) {
            throw invalidNumber("Float value must be finite", null);
        }
        return fromString(Float.toString(value));
    }

    public static Number fromDouble(double value) {
        if (!Double.isFinite(value)) {
            throw invalidNumber("Double value must be finite", null);
        }
        return fromString(Double.toString(value));
    }

    public static Number zero() {
        return ZERO;
    }

    /** Returns pi rounded to the configured maximum number of significant digits. */
    public static Number pi() {
        return DecimalConstants.pi(getMaxLength());
    }

    public static int getMaxLength() {
        return MAX_LENGTH.get();
    }

    public static void setMaxLength(int maxLength) {
        if (maxLength < 1) {
            throw new IllegalArgumentException("Maximum length must be at least 1");
        }
        MAX_LENGTH.set(maxLength);
    }

    /** Starts an immutable equation with this number as its first operand. */
    public Equation equation() {
        return Equation.of(this);
    }

    public Equation add(Number other) {
        return equation().add(require(other));
    }

    public Equation subtract(Number other) {
        return equation().subtract(require(other));
    }

    public Equation multiply(Number other) {
        return equation().multiply(require(other));
    }

    public Equation divide(Number other) {
        return equation().divide(require(other));
    }

    public Equation power(Number other) {
        return equation().power(require(other));
    }

    public Equation min(Number other) {
        return equation().min(require(other));
    }

    public Equation max(Number other) {
        return equation().max(require(other));
    }

    public Equation sqrt() {
        return equation().sqrt();
    }

    public Equation abs() {
        return equation().abs();
    }

    public Equation log() {
        return equation().log();
    }

    public Equation sin() {
        return equation().sin();
    }

    public Equation cos() {
        return equation().cos();
    }

    public Equation tan() {
        return equation().tan();
    }

    public int toInt() {
        if (isIntegral()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                // Report the nearest primitive value below.
            }
        }
        String nearest = nearestInteger();
        int nearestValue;
        try {
            nearestValue = Integer.parseInt(nearest);
        } catch (NumberFormatException ignored) {
            nearestValue = nearest.charAt(0) == '-' ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        throw new PrecisionLossException(this, int.class, nearestValue);
    }

    public long toLong() {
        if (isIntegral()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                // Report the nearest primitive value below.
            }
        }
        String nearest = nearestInteger();
        long nearestValue;
        try {
            nearestValue = Long.parseLong(nearest);
        } catch (NumberFormatException ignored) {
            nearestValue = nearest.charAt(0) == '-' ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        throw new PrecisionLossException(this, long.class, nearestValue);
    }

    public float toFloat() {
        float candidate = Float.parseFloat(value);
        float nearest = Float.isInfinite(candidate) ? Math.copySign(Float.MAX_VALUE, candidate) : candidate;
        if (Float.isFinite(candidate) && canonicalize(Float.toString(candidate)).equals(value)) {
            return candidate;
        }
        throw new PrecisionLossException(this, float.class, nearest);
    }

    public double toDouble() {
        double candidate = Double.parseDouble(value);
        double nearest = Double.isInfinite(candidate) ? Math.copySign(Double.MAX_VALUE, candidate) : candidate;
        if (Double.isFinite(candidate) && canonicalize(Double.toString(candidate)).equals(value)) {
            return candidate;
        }
        throw new PrecisionLossException(this, double.class, nearest);
    }

    public boolean lessThan(Number other) {
        return compareTo(other) < 0;
    }

    public boolean greaterThan(Number other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(Number other) {
        return compareCanonical(value, Objects.requireNonNull(other, "other").value);
    }

    private static int compareCanonical(String left, String right) {
        if (left.equals(right)) {
            return 0;
        }
        boolean leftNegative = left.charAt(0) == '-';
        boolean rightNegative = right.charAt(0) == '-';
        if (leftNegative != rightNegative) {
            return leftNegative ? -1 : 1;
        }
        int absolute = compareAbsolute(leftNegative ? left.substring(1) : left,
                rightNegative ? right.substring(1) : right);
        return leftNegative ? -absolute : absolute;
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof Number other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    private static String canonicalize(String input) {
        Matcher matcher = DECIMAL.matcher(input);
        if (!matcher.matches()) {
            throw new NumberFormatException("Malformed decimal");
        }

        String integer = matcher.group(2) == null ? "" : matcher.group(2);
        String fraction = matcher.group(2) == null ? matcher.group(4) : matcher.group(3);
        fraction = fraction == null ? "" : fraction;
        long exponent = matcher.group(5) == null ? 0 : Long.parseLong(matcher.group(5));
        String digits = integer + fraction;

        int first = 0;
        while (first < digits.length() && digits.charAt(first) == '0') {
            first++;
        }
        if (first == digits.length()) {
            return "0";
        }
        int last = digits.length();
        while (last > first && digits.charAt(last - 1) == '0') {
            last--;
        }

        int precision = last - first;
        if (precision > MAX_LENGTH.get()) {
            throw new UncalculableException(
                    CalculationError.PRECISION_EXCEEDED,
                    "Number has " + precision + " significant digits; maximum is " + MAX_LENGTH.get());
        }

        String significant = digits.substring(first, last);
        long decimalPosition = (long) integer.length() + exponent - first;
        String magnitude;
        try {
            if (decimalPosition <= 0) {
                magnitude = "0." + "0".repeat(Math.toIntExact(-decimalPosition)) + significant;
            } else if (decimalPosition >= significant.length()) {
                magnitude = significant + "0".repeat(Math.toIntExact(decimalPosition - significant.length()));
            } else {
                int point = Math.toIntExact(decimalPosition);
                magnitude = significant.substring(0, point) + "." + significant.substring(point);
            }
        } catch (ArithmeticException | OutOfMemoryError exception) {
            NumberFormatException failure = new NumberFormatException("Decimal exponent is too large");
            failure.initCause(exception);
            throw failure;
        }
        return matcher.group(1).equals("-") ? "-" + magnitude : magnitude;
    }

    private static int compareAbsolute(String left, String right) {
        int leftPoint = point(left);
        int rightPoint = point(right);
        if (leftPoint != rightPoint) {
            return Integer.compare(leftPoint, rightPoint);
        }
        int maximum = Math.max(left.length(), right.length());
        for (int index = 0; index < maximum; index++) {
            char leftDigit = digitAt(left, index);
            char rightDigit = digitAt(right, index);
            if (leftDigit != rightDigit) {
                return Character.compare(leftDigit, rightDigit);
            }
        }
        return 0;
    }

    private static int point(String value) {
        int point = value.indexOf('.');
        return point < 0 ? value.length() : point;
    }

    private static char digitAt(String value, int digitIndex) {
        int point = value.indexOf('.');
        int characterIndex = point < 0 || digitIndex < point ? digitIndex : digitIndex + 1;
        return characterIndex < value.length() ? value.charAt(characterIndex) : '0';
    }

    private boolean isIntegral() {
        return value.indexOf('.') < 0;
    }

    private String nearestInteger() {
        boolean negative = value.charAt(0) == '-';
        String magnitude = negative ? value.substring(1) : value;
        int point = magnitude.indexOf('.');
        String rounded;
        if (point < 0) {
            rounded = value;
        } else {
            String whole = magnitude.substring(0, point);
            String fraction = magnitude.substring(point + 1);
            int halfComparison = compareFractionToHalf(fraction);
            boolean odd = (whole.charAt(whole.length() - 1) - '0') % 2 != 0;
            if (halfComparison > 0 || halfComparison == 0 && odd) {
                whole = increment(whole);
            }
            rounded = negative && !whole.equals("0") ? "-" + whole : whole;
        }
        return rounded;
    }

    private static String increment(String digits) {
        char[] result = digits.toCharArray();
        for (int index = result.length - 1; index >= 0; index--) {
            if (result[index] < '9') {
                result[index]++;
                return new String(result);
            }
            result[index] = '0';
        }
        return "1" + new String(result);
    }

    private static int compareFractionToHalf(String fraction) {
        if (fraction.charAt(0) != '5') {
            return Character.compare(fraction.charAt(0), '5');
        }
        for (int index = 1; index < fraction.length(); index++) {
            if (fraction.charAt(index) != '0') {
                return 1;
            }
        }
        return 0;
    }

    private static Number require(Number value) {
        return Objects.requireNonNull(value, "other");
    }

    private static UncalculableException invalidNumber(String message, Throwable cause) {
        return new UncalculableException(CalculationError.INVALID_NUMBER, message, cause);
    }
}
