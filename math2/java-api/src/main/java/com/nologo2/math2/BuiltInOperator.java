package com.nologo2.math2;

import java.math.MathContext;
import java.util.List;
import java.util.Objects;

/** Operators supplied by Math2. */
public enum BuiltInOperator implements Operator {
    ADD("+", 2),
    SUBTRACT("-", 2),
    MULTIPLY("*", 2),
    DIVIDE("/", 2),
    POWER("^", 2),
    SQRT("sqrt", 1),
    ABS("abs", 1),
    MIN("min", 2),
    MAX("max", 2),
    LOG("log", 1),
    SIN("sin", 1),
    COS("cos", 1),
    TAN("tan", 1);

    private final String symbol;
    private final int arity;

    BuiltInOperator(String symbol, int arity) {
        this.symbol = symbol;
        this.arity = arity;
    }

    @Override
    public String symbol() {
        return symbol;
    }

    @Override
    public int arity() {
        return arity;
    }

    @Override
    public Number apply(List<Number> operands, MathContext context) {
        Objects.requireNonNull(operands, "operands");
        Objects.requireNonNull(context, "context");
        if (operands.size() != arity || operands.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(symbol + " requires " + arity + " non-null operands");
        }

        try {
            return DecimalArithmetic.calculate(
                    this,
                    operands.get(0),
                    arity == 2 ? operands.get(1) : null,
                    context.getPrecision());
        } catch (UncalculableException exception) {
            throw exception;
        } catch (ArithmeticException exception) {
            throw new UncalculableException(
                    CalculationError.DOMAIN_ERROR,
                    "Operator " + symbol + " is undefined for the supplied operands",
                    exception);
        }
    }

}
