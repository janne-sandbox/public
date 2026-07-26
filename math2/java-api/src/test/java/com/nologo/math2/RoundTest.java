package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@code round(x)}, {@code round(x, scale)}, and
 * {@code round(x, scale, mode)} functions.
 *
 * <p>Rounding mode codes:</p>
 * <ul>
 *   <li>0 — HALF_EVEN (banker's rounding, default)</li>
 *   <li>1 — HALF_UP   (round half away from zero)</li>
 *   <li>2 — HALF_DOWN (round half toward zero)</li>
 *   <li>3 — CEILING   (toward positive infinity)</li>
 *   <li>4 — FLOOR     (toward negative infinity)</li>
 *   <li>5 — UP        (always away from zero)</li>
 *   <li>6 — DOWN      (always toward zero / truncate)</li>
 * </ul>
 */
class RoundTest {

    // ---- round(x) — one argument, rounds to integer, HALF_EVEN ----

    @Test
    void roundToIntegerDefaultHalfEven() {
        // Ties go to even: 0.5 → 0 (even), 1.5 → 2 (even), 2.5 → 2 (even), 3.5 → 4 (even)
        assertEquals("0",  Equation.parse("round(0.5)").calculate(10).toString());
        assertEquals("2",  Equation.parse("round(1.5)").calculate(10).toString());
        assertEquals("2",  Equation.parse("round(2.5)").calculate(10).toString());
        assertEquals("4",  Equation.parse("round(3.5)").calculate(10).toString());
        // Non-ties
        assertEquals("1",  Equation.parse("round(1.4)").calculate(10).toString());
        assertEquals("2",  Equation.parse("round(1.6)").calculate(10).toString());
        assertEquals("-2", Equation.parse("round(-1.5)").calculate(10).toString());
        assertEquals("0",  Equation.parse("round(0)").calculate(10).toString());
    }

    // ---- round(x, scale) — two arguments, HALF_EVEN ----

    @Test
    void roundToDecimalPlacesHalfEven() {
        assertEquals("1.23",  Equation.parse("round(1.234, 2)").calculate(10).toString());
        assertEquals("1.24",  Equation.parse("round(1.236, 2)").calculate(10).toString());
        // 1.235 → 12.35/10 → quotient 12 (even) → round down → 1.24? No:
        // 1.235 * 100 = 123.5, quotient 123 (odd) → round up to 124 → 1.24
        assertEquals("1.24",  Equation.parse("round(1.235, 2)").calculate(10).toString());
        // 1.245 * 100 = 124.5, quotient 124 (even) → round down to 124 → 1.24
        assertEquals("1.24",  Equation.parse("round(1.245, 2)").calculate(10).toString());
        assertEquals("0",     Equation.parse("round(0.001, 0)").calculate(10).toString());
        assertEquals("1000",  Equation.parse("round(1000, 0)").calculate(10).toString());
        assertEquals("3.142", Equation.parse("round(3.1415, 3)").calculate(10).toString());
    }

    @Test
    void roundNegativeValuesHalfEven() {
        assertEquals("-1.23",  Equation.parse("round(-1.234, 2)").calculate(10).toString());
        assertEquals("-1.24",  Equation.parse("round(-1.235, 2)").calculate(10).toString());
        // -1.245 * 100 = -124.5, magnitude quotient 124 (even) → down → -1.24
        assertEquals("-1.24",  Equation.parse("round(-1.245, 2)").calculate(10).toString());
    }

    // ---- round(x, scale, mode) — three arguments ----

    @Test
    void halfUpMode() {
        // mode=1: HALF_UP — round half away from zero
        assertEquals("2",   Equation.parse("round(1.5, 0, 1)").calculate(10).toString());
        assertEquals("3",   Equation.parse("round(2.5, 0, 1)").calculate(10).toString());
        assertEquals("-2",  Equation.parse("round(-1.5, 0, 1)").calculate(10).toString());
        assertEquals("1.24", Equation.parse("round(1.235, 2, 1)").calculate(10).toString());
        assertEquals("1.25", Equation.parse("round(1.245, 2, 1)").calculate(10).toString());
    }

    @Test
    void halfDownMode() {
        // mode=2: HALF_DOWN — round half toward zero
        assertEquals("1",   Equation.parse("round(1.5, 0, 2)").calculate(10).toString());
        assertEquals("2",   Equation.parse("round(2.5, 0, 2)").calculate(10).toString());
        assertEquals("-1",  Equation.parse("round(-1.5, 0, 2)").calculate(10).toString());
        assertEquals("1.23", Equation.parse("round(1.235, 2, 2)").calculate(10).toString());
    }

    @Test
    void ceilingMode() {
        // mode=3: CEILING — toward +∞
        assertEquals("2",  Equation.parse("round(1.1, 0, 3)").calculate(10).toString());
        assertEquals("2",  Equation.parse("round(1.9, 0, 3)").calculate(10).toString());
        assertEquals("-1", Equation.parse("round(-1.9, 0, 3)").calculate(10).toString());
        assertEquals("-1", Equation.parse("round(-1.1, 0, 3)").calculate(10).toString());
        // Exact integer unchanged
        assertEquals("2",  Equation.parse("round(2, 0, 3)").calculate(10).toString());
    }

    @Test
    void floorMode() {
        // mode=4: FLOOR — toward −∞
        assertEquals("1",  Equation.parse("round(1.9, 0, 4)").calculate(10).toString());
        assertEquals("1",  Equation.parse("round(1.1, 0, 4)").calculate(10).toString());
        assertEquals("-2", Equation.parse("round(-1.1, 0, 4)").calculate(10).toString());
        assertEquals("-2", Equation.parse("round(-1.9, 0, 4)").calculate(10).toString());
    }

    @Test
    void upMode() {
        // mode=5: UP — always away from zero
        assertEquals("2",  Equation.parse("round(1.1, 0, 5)").calculate(10).toString());
        assertEquals("-2", Equation.parse("round(-1.1, 0, 5)").calculate(10).toString());
        // Exact integer: no fraction → no rounding
        assertEquals("2",  Equation.parse("round(2, 0, 5)").calculate(10).toString());
    }

    @Test
    void downMode() {
        // mode=6: DOWN — always toward zero (truncate)
        assertEquals("1",  Equation.parse("round(1.9, 0, 6)").calculate(10).toString());
        assertEquals("-1", Equation.parse("round(-1.9, 0, 6)").calculate(10).toString());
        assertEquals("1",  Equation.parse("round(1.1, 0, 6)").calculate(10).toString());
    }

    // ---- Rational operand gets exact rounding ----

    @Test
    void rationalOperandRoundedExactly() {
        // 1/3 + 2/3 = 1 exactly; round(1, 0) = 1
        assertEquals("1", Equation.parse("round(1/3 + 2/3, 0)").calculate(5).toString());
        // round(1/3, 2) = 0.33 (exact rational 1/3 rounds down at 2dp)
        assertEquals("0.33", Equation.parse("round(1/3, 2)").calculate(5).toString());
        // round(2/3, 2) = 0.67 (exact rational 2/3 rounds up at 2dp)
        assertEquals("0.67", Equation.parse("round(2/3, 2)").calculate(5).toString());
    }

    @Test
    void rationalRoundingIndependentOfPrecision() {
        // round(1/3, 2) must give 0.33 regardless of calculation precision
        assertEquals("0.33", Equation.parse("round(1/3, 2)").calculate(3).toString());
        assertEquals("0.33", Equation.parse("round(1/3, 2)").calculate(10).toString());
        assertEquals("0.33", Equation.parse("round(1/3, 2)").calculate(100).toString());
    }

    // ---- round() inside larger expressions ----

    @Test
    void roundInsideExpression() {
        // round(pi, 4) + 0 should equal pi rounded to 4dp
        Number piRound4 = Equation.parse("round(pi, 4)").calculate(20);
        assertEquals("3.1416", piRound4.toString());
        // Using round result in arithmetic
        Number result = Equation.parse("round(1.555, 2) * 2").calculate(10);
        assertEquals("3.12", result.toString()); // round(1.555,2)=1.56, *2=3.12
    }

    @Test
    void scaleCanBeAnExpression() {
        // round(1.23456, 1+1) → round(1.23456, 2) = 1.23
        assertEquals("1.23", Equation.parse("round(1.23456, 1+1)").calculate(10).toString());
    }

    // ---- Canonical expression ----

    @Test
    void canonicalFormAlwaysThreeArgs() {
        // All three forms produce the same canonical (scale and mode defaults shown)
        Equation e1 = Equation.parse("round(1.5)");
        Equation e2 = Equation.parse("round(1.5, 0)");
        Equation e3 = Equation.parse("round(1.5, 0, 0)");
        assertEquals("round(1.5,0,0)", e1.canonicalExpression());
        assertEquals("round(1.5,0,0)", e2.canonicalExpression());
        assertEquals("round(1.5,0,0)", e3.canonicalExpression());
        // Canonical re-parse must produce the same result
        assertEquals(e1.calculate(10), Equation.parse(e1.canonicalExpression()).calculate(10));
    }

    // ---- Error cases ----

    @Test
    void negativeScaleThrows() {
        UncalculableException ex = assertThrows(UncalculableException.class,
                () -> Equation.parse("round(1.5, -1)").calculate(10));
        assertEquals(CalculationError.DOMAIN_ERROR, ex.getReason());
    }

    @Test
    void fractionalScaleThrows() {
        UncalculableException ex = assertThrows(UncalculableException.class,
                () -> Equation.parse("round(1.5, 1.5)").calculate(10));
        assertEquals(CalculationError.DOMAIN_ERROR, ex.getReason());
    }

    @Test
    void unknownModeCodeThrows() {
        UncalculableException ex = assertThrows(UncalculableException.class,
                () -> Equation.parse("round(1.5, 0, 99)").calculate(10));
        assertEquals(CalculationError.DOMAIN_ERROR, ex.getReason());
    }

    @Test
    void malformedRoundExpressionThrows() {
        assertThrows(UncalculableException.class, () -> Equation.parse("round("));
        assertThrows(UncalculableException.class, () -> Equation.parse("round(1.5, 2, 0, 1)"));
    }
}
