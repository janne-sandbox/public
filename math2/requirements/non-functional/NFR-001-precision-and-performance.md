# NFR-001 — Precision and performance

1. Arithmetic must not use Java primitive floating-point as its calculation representation.
2. The default calculation limit is 1000 significant decimal digits and is configurable.
3. Arithmetic results are rounded with `HALF_EVEN` only when an exact result cannot fit the active calculation context.
4. Guard digits must be used for intermediate transcendental calculations and removed only at the public result boundary.
5. A batch request must impose a configurable maximum equation count and expression length to protect service availability.
6. Results must be deterministic for the same engine version, canonical equation, and precision.
7. The calculation engine and mathematical constants must be implemented inside Math2 and must not depend on Apfloat, `BigDecimal`, `BigInteger`, `BigDecimalMath`, or another numeric engine; configured precision may exceed 1000 digits and is bounded operationally by configured limits, available memory, and execution time.
