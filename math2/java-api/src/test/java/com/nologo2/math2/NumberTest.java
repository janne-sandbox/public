package com.nologo2.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NumberTest {
    @AfterEach
    void restoreDefaultLimit() {
        Number.setMaxLength(Number.DEFAULT_MAX_LENGTH);
    }

    @Test
    void factoriesProduceCanonicalDecimalValues() {
        assertEquals("1000", Number.fromString("1.000e3").toString());
        assertEquals("12.5", Number.fromFloat(12.5f).toString());
        assertEquals("0.1", Number.fromDouble(0.1d).toString());
        assertEquals("42", Number.fromInteger(42).toString());
        assertEquals("9223372036854775807", Number.fromLong(Long.MAX_VALUE).toString());
        assertSame(Number.zero(), Number.fromString("-0.000"));
    }

    @Test
    void piUsesTheConfiguredSignificantDigitLength() {
        Number piAtDefaultPrecision = Number.pi();
        assertTrue(piAtDefaultPrecision.toString().startsWith("3.14159265358979323846"));
        assertEquals(1000, decimalDigits(piAtDefaultPrecision));

        Number.setMaxLength(1200);
        assertEquals(1200, decimalDigits(Number.pi()));
    }

    @Test
    void storesCanonicalValuesAsStrings() throws NoSuchFieldException {
        assertEquals(String.class, Number.class.getDeclaredField("value").getType());
        assertEquals("123.45", Number.fromString("001.234500e2").toString());
    }

    @Test
    void rejectsMalformedAndNonFiniteValues() {
        assertEquals(CalculationError.INVALID_NUMBER,
                assertThrows(UncalculableException.class, () -> Number.fromString(" ")).getReason());
        assertThrows(UncalculableException.class, () -> Number.fromString("1.2.3"));
        assertThrows(UncalculableException.class, () -> Number.fromFloat(Float.NaN));
        assertThrows(UncalculableException.class, () -> Number.fromDouble(Double.POSITIVE_INFINITY));
    }

    @Test
    void limitCountsSignificantDigitsAndCanBeConfigured() {
        Number.setMaxLength(3);

        assertEquals("1000", Number.fromString("1e3").toString());
        UncalculableException exception = assertThrows(
                UncalculableException.class, () -> Number.fromString("12.34"));
        assertEquals(CalculationError.PRECISION_EXCEEDED, exception.getReason());
        assertThrows(UncalculableException.class, () -> Number.fromInteger(1234));
        assertThrows(IllegalArgumentException.class, () -> Number.setMaxLength(0));
        assertEquals(3, Number.getMaxLength());
    }

    @Test
    void equalityComparisonAndHashCodeIgnoreScale() {
        Number one = Number.fromString("1.0");
        Number alsoOne = Number.fromString("1.00");

        assertEquals(one, alsoOne);
        assertEquals(one.hashCode(), alsoOne.hashCode());
        assertTrue(one.lessThan(Number.fromString("1.01")));
        assertTrue(Number.fromInteger(2).greaterThan(one));
        assertFalse(one.greaterThan(alsoOne));
    }

    @Test
    void exactIntegerConversionsSucceed() {
        assertEquals(42, Number.fromString("42.0").toInt());
        assertEquals(Integer.MIN_VALUE, Number.fromString("-2147483648").toInt());
        assertEquals(Integer.MAX_VALUE, Number.fromString("2147483647").toInt());
        assertEquals(Long.MIN_VALUE, Number.fromString("-9223372036854775808").toLong());
        assertEquals(Long.MAX_VALUE, Number.fromString("9223372036854775807").toLong());
    }

    @Test
    void lossyIntegerConversionReportsRoundedAndClampedNearestValue() {
        PrecisionLossException fraction = assertThrows(
                PrecisionLossException.class, () -> Number.fromString("42.5").toInt());
        assertEquals(42, fraction.getNearestValue());
        assertEquals(int.class, fraction.getTargetType());
        assertEquals(Number.fromString("42.5"), fraction.getSource());

        PrecisionLossException negativeHalf = assertThrows(
                PrecisionLossException.class, () -> Number.fromString("-43.5").toInt());
        assertEquals(-44, negativeHalf.getNearestValue());

        PrecisionLossException overflow = assertThrows(
                PrecisionLossException.class, () -> Number.fromString("2147483648").toInt());
        assertEquals(Integer.MAX_VALUE, overflow.getNearestValue());

        PrecisionLossException negativeOverflow = assertThrows(
                PrecisionLossException.class, () -> Number.fromString("-2147483649").toInt());
        assertEquals(Integer.MIN_VALUE, negativeOverflow.getNearestValue());
    }

    @Test
    void floatingConversionsRequireDecimalRoundTrip() {
        assertEquals(0.5f, Number.fromString("0.5").toFloat());
        assertEquals(0.1d, Number.fromString("0.1").toDouble());

        PrecisionLossException floatLoss = assertThrows(
                PrecisionLossException.class, () -> Number.fromString("0.10000000000000001").toFloat());
        assertEquals(float.class, floatLoss.getTargetType());

        PrecisionLossException doubleLoss = assertThrows(
                PrecisionLossException.class, () -> Number.fromString("0.10000000000000001").toDouble());
        assertEquals(double.class, doubleLoss.getTargetType());
    }

    @Test
    void fluentOperationsBuildEquations() {
        Number two = Number.fromInteger(2);
        Number three = Number.fromInteger(3);

        assertEquals("10", two.add(three).multiply(two).calculate().toString());
        assertEquals("sqrt((2+3))", two.add(three).sqrt().canonicalExpression());
        assertEquals("2", two.toString());
    }

    private static int decimalDigits(Number number) {
        return number.toString().replace("-", "").replace(".", "").length();
    }
}
