package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifies the rational arithmetic reduction pass in {@link Equation}.
 *
 * <p>Expressions whose operands and operators are closed over the rationals are evaluated
 * using exact {@code BigInteger} fractions before any decimal conversion.  This eliminates
 * intermediate precision loss that would otherwise accumulate when individual sub-results
 * (such as {@code 1/3}) are rounded to the requested decimal precision.</p>
 *
 * <p>Typical cases where the rational path yields an <em>exact</em> result that the pure
 * decimal path cannot guarantee:</p>
 * <ul>
 *   <li>{@code (1/3 + 2/3) = 1} – fractions cancel to an exact integer</li>
 *   <li>{@code (1/3) * 3 = 1} – fractional multiplication cancels exactly</li>
 *   <li>{@code 2^-3 = 0.125} – negative-integer power yields a terminating decimal</li>
 * </ul>
 */
class RationalReductionTest {

    // ---- Cancellation to exact integer ----

    @Test
    void thirdsPlusThirdsEqualsExactOne() {
        // 1/3 + 2/3 = 1 exactly, regardless of precision
        for (int precision : new int[]{1, 5, 10, 100}) {
            Number result = Equation.parse("1/3 + 2/3").calculate(precision);
            assertEquals("1", result.toString(),
                    "Expected exact 1 at precision " + precision);
        }
    }

    @Test
    void thirdTimesThreeEqualsExactOne() {
        // (1/3) * 3 = 1 exactly
        assertEquals("1", Equation.parse("(1/3) * 3").calculate(10).toString());
        assertEquals("1", Equation.parse("3 * (1/3)").calculate(10).toString());
    }

    @Test
    void seventhsSumToExactInteger() {
        // 1/7 + 2/7 + 3/7 + 4/7 + 5/7 + 6/7 + 7/7 = 4
        assertEquals("4", Equation.parse("1/7 + 2/7 + 3/7 + 4/7 + 5/7 + 6/7 + 7/7").calculate(10).toString());
    }

    @Test
    void fifthsPlusThreeFifthsEqualsExactOne() {
        assertEquals("1", Equation.parse("2/5 + 3/5").calculate(5).toString());
    }

    @Test
    void rationalCancellationWithMultiplication() {
        // (2/3) * (3/2) = 1 exactly
        assertEquals("1", Equation.parse("(2/3) * (3/2)").calculate(20).toString());
    }

    @Test
    void deepNestedCancellation() {
        // ((1/3 + 1/6) * 2) = (1/2 * 2) = 1
        assertEquals("1", Equation.parse("((1/3 + 1/6) * 2)").calculate(10).toString());
    }

    // ---- Exact terminating-decimal results ----

    @Test
    void negativeIntegerPowerYieldsTerminatingDecimal() {
        // 2^-3 = 1/8 = 0.125
        assertEquals("0.125", Equation.parse("2^-3").calculate(10).toString());
    }

    @Test
    void simpleFractionToExactTerminatingDecimal() {
        // 3/4 = 0.75
        assertEquals("0.75", Equation.parse("3/4").calculate(10).toString());
    }

    @Test
    void integerPowerIsExact() {
        // 2^10 = 1024
        assertEquals("1024", Equation.parse("2^10").calculate(10).toString());
    }

    // ---- Rational reduction feeds precision-sensitive operations ----

    @Test
    void trigFunctionReceivesExactOperandFromRationalReduction() {
        // sin(1/3 + 2/3) must equal sin(1)
        Number direct  = Equation.parse("sin(1)").calculate(20);
        Number reduced = Equation.parse("sin(1/3 + 2/3)").calculate(20);
        assertEquals(direct.toString(), reduced.toString(),
                "sin(1/3 + 2/3) should match sin(1) when rational reduction is applied");
    }

    @Test
    void sqrtReceivesExactOperandFromRationalReduction() {
        // sqrt(1/3 + 2/3) = sqrt(1) = 1 exactly
        assertEquals("1", Equation.parse("sqrt(1/3 + 2/3)").calculate(20).toString());
    }

    @Test
    void logReceivesExactOperandFromRationalReduction() {
        // log(1/3 + 2/3) = log(1) = 0 exactly
        assertEquals("0", Equation.parse("log(1/3 + 2/3)").calculate(20).toString());
    }

    @Test
    void piExpressionStillUsesDecimalEngine() {
        // pi is irrational; expressions involving pi must still use the decimal engine
        Number twoPi = Equation.parse("2 * pi").calculate(25);
        assertTrue(twoPi.toString().startsWith("6.28318"),
                "2*pi should start with 6.28318, got: " + twoPi);
    }

    // ---- ABS and MIN / MAX with rational operands ----

    @Test
    void absOfRationalSubtractionIsExact() {
        // abs(1/3 - 2/3) = abs(-1/3) = 1/3; exact decimal rendering at precision 5
        assertEquals("0.33333", Equation.parse("abs(1/3 - 2/3)").calculate(5).toString());
    }

    @Test
    void minAndMaxWithRationalOperandsAreExact() {
        assertEquals("1", Equation.parse("max(1/3 + 2/3, 1/2)").calculate(5).toString());
        assertEquals("0.5",  Equation.parse("min(1/3 + 2/3, 1/2)").calculate(5).toString());
    }

    // ---- Edge cases ----

    @Test
    void zeroNumeratorReducesToZero() {
        assertEquals("0", Equation.parse("0/5").calculate(10).toString());
        assertEquals("0", Equation.parse("3/7 - 3/7").calculate(10).toString());
    }

    @Test
    void negativeRationalCancellation() {
        // -1/3 + 1/3 = 0
        assertEquals("0", Equation.parse("-1/3 + 1/3").calculate(10).toString());
    }

    @Test
    void largeIntegerPowerIsExact() {
        // 3^20 = 3486784401
        assertEquals("3486784401", Equation.parse("3^20").calculate(20).toString());
    }

    @Test
    void rationalReductionDoesNotAffectExistingEquationBehaviour() {
        // Existing behaviour must not regress
        assertEquals("14",  Equation.parse("2 + 3 * 4").calculate(10).toString());
        assertEquals("20",  Equation.parse("(2 + 3) * 4").calculate(10).toString());
        assertEquals("512", Equation.parse("2 ^ 3 ^ 2").calculate(10).toString());
        assertEquals("-4",  Equation.parse("-2^2").calculate(10).toString());
        assertEquals("5",   Equation.parse("sqrt(9) + abs(-2)").calculate(10).toString());
    }
}
