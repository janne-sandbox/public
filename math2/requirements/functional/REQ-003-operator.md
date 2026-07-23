# REG-003 — Operator model

## Purpose

Operators use abstractions so callers can compose equations without concatenating expression strings.

## Requirements

1. Every operator implements a common `Operator` interface declaring its symbol, arity, and evaluation contract.
2. Supported binary operators are addition (`+`), subtraction (`-`), multiplication (`*`), division (`/`), power (`^`), minimum (`min`), and maximum (`max`).
3. Supported unary operators are square root (`sqrt`), absolute value (`abs`), natural logarithm (`log`), sine (`sin`), cosine (`cos`), and tangent (`tan`).
4. Parentheses control grouping in parsed equations but are not evaluatable operators.
5. Normal precedence is: functions, power (right associative), multiplication/division, addition/subtraction. `min` and `max` use function-call syntax with two arguments.
6. Division by zero, square root of a negative value, logarithm of a non-positive value, undefined tangent, and unsupported real power results throw `UncalculableException`.
7. Operator evaluation honors the equation precision and returns an immutable `Number`.
8. Parsed equations recognize the case-insensitive constant `pi` without function-call parentheses.
