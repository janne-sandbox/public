package com.nologo.math2;

/** Stable categories for calculation and input failures. */
public enum CalculationError {
    INVALID_NUMBER,
    INVALID_EXPRESSION,
    PRECISION_EXCEEDED,
    DIVISION_BY_ZERO,
    DOMAIN_ERROR,
    BACKEND_UNAVAILABLE,
    BACKEND_EXECUTION_FAILED,
    INVALID_BACKEND_RESULT
}
