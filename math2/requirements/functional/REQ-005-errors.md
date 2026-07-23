# REG-005 — Error types

## Requirements

1. `UncalculableException` is the common unchecked failure for invalid input, exceeded precision, and mathematically undefined calculations.
2. Its stable machine-readable reason is one of `INVALID_NUMBER`, `INVALID_EXPRESSION`, `PRECISION_EXCEEDED`, `DIVISION_BY_ZERO`, or `DOMAIN_ERROR`.
3. Parser failures may expose a zero-based character position.
4. `PrecisionLossException` extends `UncalculableException` and exposes the source value, requested target type, and nearest representable value.
5. Exceptions must not expose stack traces in REST responses.
