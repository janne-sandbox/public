package com.nologo2.math2;

import java.math.MathContext;
import java.util.List;

/** A precision-aware operation over one or more immutable operands. */
public interface Operator {
    String symbol();

    int arity();

    Number apply(List<Number> operands, MathContext context);
}
