package com.nologo2.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.MathContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuiltInOperatorTest {
    private static final MathContext CONTEXT = new MathContext(30);

    @Test
    void arithmeticAndSelectionOperatorsCalculate() {
        assertResult("5", BuiltInOperator.ADD, "2", "3");
        assertResult("-1", BuiltInOperator.SUBTRACT, "2", "3");
        assertResult("6", BuiltInOperator.MULTIPLY, "2", "3");
        assertResult("2.5", BuiltInOperator.DIVIDE, "5", "2");
        assertResult("8", BuiltInOperator.POWER, "2", "3");
        assertResult("2", BuiltInOperator.MIN, "2", "3");
        assertResult("3", BuiltInOperator.MAX, "2", "3");
    }

    @Test
    void unaryOperatorsCalculate() {
        assertResult("3", BuiltInOperator.SQRT, "9");
        assertResult("2", BuiltInOperator.ABS, "-2");
        assertResult("0", BuiltInOperator.LOG, "1");
        assertResult("0", BuiltInOperator.SIN, "0");
        assertResult("1", BuiltInOperator.COS, "0");
        assertResult("0", BuiltInOperator.TAN, "0");
    }

    @Test
    void internallyImplementedFunctionsRetainRequestedPrecision() {
        assertResultAt(20, "1.4142135623730950488", BuiltInOperator.SQRT, "2");
        assertResultAt(20, "0.69314718055994530942", BuiltInOperator.LOG, "2");
        assertResultAt(20, "0.84147098480789650665", BuiltInOperator.SIN, "1");
        assertResultAt(20, "0.5403023058681397174", BuiltInOperator.COS, "1");
        assertResultAt(20, "1.5574077246549022305", BuiltInOperator.TAN, "1");
        assertResultAt(20, "1.4142135623730950488", BuiltInOperator.POWER, "2", "0.5");
    }

    @Test
    void invalidDomainsHaveStableReasons() {
        assertReason(CalculationError.DIVISION_BY_ZERO, BuiltInOperator.DIVIDE, "1", "0");
        assertReason(CalculationError.DOMAIN_ERROR, BuiltInOperator.SQRT, "-1");
        assertReason(CalculationError.DOMAIN_ERROR, BuiltInOperator.LOG, "0");
        assertReason(CalculationError.DOMAIN_ERROR, BuiltInOperator.POWER, "-2", "0.5");
    }

    @Test
    void validatesArity() {
        assertThrows(IllegalArgumentException.class,
                () -> BuiltInOperator.ADD.apply(List.of(Number.fromInteger(1)), CONTEXT));
    }

    private static void assertResult(String expected, BuiltInOperator operator, String... values) {
        assertResultAt(CONTEXT.getPrecision(), expected, operator, values);
    }

    private static void assertResultAt(
            int precision, String expected, BuiltInOperator operator, String... values) {
        List<Number> operands = java.util.Arrays.stream(values).map(Number::fromString).toList();
        assertEquals(Number.fromString(expected), operator.apply(operands, new MathContext(precision)));
    }

    private static void assertReason(
            CalculationError reason, BuiltInOperator operator, String... values) {
        List<Number> operands = java.util.Arrays.stream(values).map(Number::fromString).toList();
        assertEquals(reason, assertThrows(
                UncalculableException.class, () -> operator.apply(operands, CONTEXT)).getReason());
    }
}
