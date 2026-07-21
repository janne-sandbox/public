package com.nologo2.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EquationTest {
    @Test
    void parserHonorsPrecedenceGroupingAndRightAssociativePower() {
        assertCalculation("14", "2 + 3 * 4");
        assertCalculation("20", "(2 + 3) * 4");
        assertCalculation("512", "2 ^ 3 ^ 2");
        assertCalculation("-4", "-2^2");
        assertCalculation("0.25", "2^-2");
    }

    @Test
    void parsesFunctionsScientificNotationAndWhitespace() {
        assertCalculation("5", "sqrt(9) + abs(-2)");
        assertCalculation("3", "max(1e0, min(3, 4))");
        assertCalculation("2", " cos(0) + sin(0) + log(1) + tan(0) + 1 ");
    }

    @Test
    void parsesPiAsAPrecisionAwareConstant() {
        Equation pi = Equation.parse("PI");
        Number value = pi.calculate(25);

        assertEquals("pi", pi.canonicalExpression());
        assertTrue(value.toString().startsWith("3.14159265358979323846"));
        assertEquals(25, value.toString().replace(".", "").length());
        assertEquals("6.283185307179586476925286", Equation.parse("2*pi").calculate(25).toString());
    }

    @Test
    void fluentEquationsAreImmutableAndCanonical() {
        Equation two = Equation.of(Number.fromInteger(2));
        Equation expression = two.add(Equation.of(Number.fromInteger(3))).sqrt();

        assertEquals("2", two.canonicalExpression());
        assertEquals("sqrt((2+3))", expression.canonicalExpression());
        assertEquals(Number.fromString("2.236067977"), expression.calculate(10));
    }

    @Test
    void malformedExpressionsReportPositions() {
        for (String expression : new String[] {"", "(1 + 2", "1 +", "unknown(1)", "min(1)"}) {
            UncalculableException exception = assertThrows(
                    UncalculableException.class, () -> Equation.parse(expression));
            assertEquals(CalculationError.INVALID_EXPRESSION, exception.getReason());
            assertTrue(exception.getPosition().isPresent());
        }
    }

    @Test
    void calculationPrecisionIsValidated() {
        Equation equation = Equation.parse("1 / 3");
        assertEquals("0.33333", equation.calculate(5).toString());
        assertThrows(UncalculableException.class, () -> equation.calculate(0));
        assertThrows(UncalculableException.class,
                () -> equation.calculate(Number.getMaxLength() + 1));
    }

    @Test
    void evaluationCanBeRepeated() {
        Equation equation = Equation.parse("6 * 7");
        assertEquals(equation.calculate(), equation.calculate());
    }

    @Test
    void calculatesWithThousandDigitStringBackedOperands() {
        String thousandNines = "9".repeat(1000);
        Number result = Number.fromString(thousandNines).add(Number.fromInteger(1)).calculate(1000);

        assertEquals("1" + "0".repeat(1000), result.toString());
    }

    @Test
    void calculatesBeyondTheDefaultPrecisionWithoutADecimalScaleLimit() {
        Number.setMaxLength(5000);
        try {
            String fiveThousandNines = "9".repeat(5000);
            Number result = Number.fromString(fiveThousandNines)
                    .add(Number.fromInteger(1))
                    .calculate(5000);

            assertEquals("1" + "0".repeat(5000), result.toString());
        } finally {
            Number.setMaxLength(Number.DEFAULT_MAX_LENGTH);
        }
    }

    @Test
    void preservesSignificantDigitsAtLargeNegativeDecimalExponents() {
        assertEquals("0." + "0".repeat(2000) + "5",
                Equation.parse("1e-2000 / 2").calculate(20).toString());
        assertEquals("0." + "0".repeat(1999) + "1",
                Equation.parse("1e-1000 * 1e-1000").calculate(20).toString());
    }

    private static void assertCalculation(String expected, String expression) {
        assertEquals(Number.fromString(expected), Equation.parse(expression).calculate(40));
    }
}
