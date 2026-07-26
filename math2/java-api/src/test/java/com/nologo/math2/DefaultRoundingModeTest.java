package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Number#getDefaultRoundingModeCode()},
 * {@link Number#setDefaultRoundingModeCode(int)}, and the interaction with
 * the {@code round()} function in {@link Equation}.
 *
 * <p>The default rounding mode is captured by the parser at the moment {@code round(x)} or
 * {@code round(x,&nbsp;scale)} is parsed without an explicit mode argument.  Existing
 * {@link Equation} instances are immutable and always use the mode that was current when
 * they were parsed.</p>
 */
class DefaultRoundingModeTest {

    /** Reset to HALF_EVEN (0) before and after every test so tests are independent. */
    @BeforeEach
    @AfterEach
    void resetDefaultMode() {
        Number.setDefaultRoundingModeCode(0);
    }

    // ---- get / set ----

    @Test
    void defaultIsHalfEven() {
        assertEquals(0, Number.getDefaultRoundingModeCode());
        assertEquals("HALF_EVEN", Number.getRoundingModeName(0));
    }

    @Test
    void setAndGetRoundTrips() {
        for (int code = 0; code < Number.ROUNDING_MODE_COUNT; code++) {
            Number.setDefaultRoundingModeCode(code);
            assertEquals(code, Number.getDefaultRoundingModeCode());
        }
    }

    @Test
    void negativeCodeThrows() {
        assertThrows(IllegalArgumentException.class, () -> Number.setDefaultRoundingModeCode(-1));
    }

    @Test
    void tooLargeCodeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Number.setDefaultRoundingModeCode(Number.ROUNDING_MODE_COUNT));
    }

    @Test
    void modeNamesAreCorrect() {
        assertEquals("HALF_EVEN",  Number.getRoundingModeName(0));
        assertEquals("HALF_UP",    Number.getRoundingModeName(1));
        assertEquals("HALF_DOWN",  Number.getRoundingModeName(2));
        assertEquals("CEILING",    Number.getRoundingModeName(3));
        assertEquals("FLOOR",      Number.getRoundingModeName(4));
        assertEquals("UP",         Number.getRoundingModeName(5));
        assertEquals("DOWN",       Number.getRoundingModeName(6));
    }

    @Test
    void invalidModeNameLookupThrows() {
        assertThrows(IllegalArgumentException.class, () -> Number.getRoundingModeName(-1));
        assertThrows(IllegalArgumentException.class, () -> Number.getRoundingModeName(7));
    }

    // ---- integration with Equation.parse("round(x)") ----

    @Test
    void roundOneArgUsesDefaultAtParseTime() {
        // Default = HALF_EVEN (0): 2.5 → 2 (2 is even)
        Number.setDefaultRoundingModeCode(0);
        assertEquals("2", Equation.parse("round(2.5)").calculate(10).toString());

        // Switch to HALF_UP (1): 2.5 → 3 (half up)
        Number.setDefaultRoundingModeCode(1);
        assertEquals("3", Equation.parse("round(2.5)").calculate(10).toString());
    }

    @Test
    void roundTwoArgsUsesDefaultAtParseTime() {
        // 1.235 at scale=2 with HALF_EVEN: 123.5 → 123 is odd → 124 → 1.24
        Number.setDefaultRoundingModeCode(0);
        assertEquals("1.24", Equation.parse("round(1.235, 2)").calculate(10).toString());

        // With HALF_DOWN (2): 123.5 → ties go toward zero → 123 → 1.23
        Number.setDefaultRoundingModeCode(2);
        assertEquals("1.23", Equation.parse("round(1.235, 2)").calculate(10).toString());
    }

    @Test
    void existingEquationIsImmutableAfterDefaultChange() {
        // Parse while default is HALF_EVEN (0)
        Number.setDefaultRoundingModeCode(0);
        Equation parsed = Equation.parse("round(2.5)");

        // Change default — must NOT affect already-parsed equation
        Number.setDefaultRoundingModeCode(1); // HALF_UP
        assertEquals("2", parsed.calculate(10).toString(),
                "Pre-parsed equation must continue to use the mode captured at parse time");
    }

    @Test
    void explicitModeOverridesDefault() {
        // Even if default is HALF_UP (1), explicit 0 → HALF_EVEN
        Number.setDefaultRoundingModeCode(1);
        assertEquals("2", Equation.parse("round(2.5, 0, 0)").calculate(10).toString());
    }

    @Test
    void canonicalEmbedsCapturedModeCode() {
        Number.setDefaultRoundingModeCode(1); // HALF_UP
        Equation e = Equation.parse("round(2.5)");
        assertEquals("round(2.5,0,1)", e.canonicalExpression());
    }
}
