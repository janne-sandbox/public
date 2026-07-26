# NFR-001 — Precision and performance

1. Arithmetic must not use Java primitive floating-point as its calculation representation.
2. The default calculation limit is 1000 significant decimal digits and is configurable.
3. Arithmetic results are rounded with `HALF_EVEN` only when an exact result cannot fit the active calculation context.
4. Guard digits must be used for intermediate transcendental calculations and removed only at the public result boundary.
5. A batch request must impose a configurable maximum equation count and expression length to protect service availability.
6. Results must be deterministic for the same engine version, canonical equation, and precision.
7. The calculation engine and mathematical constants must be implemented inside Math2 and must not depend on Apfloat, `BigDecimal`, `BigInteger`, `BigDecimalMath`, or another numeric engine; configured precision may exceed 1000 digits and is bounded operationally by configured limits, available memory, and execution time.
8. Optional execution backends must produce the same canonical decimal result as the CPU reference for the same engine version, equation, precision, and rounding configuration; approximate equality is not sufficient.
9. Backend selection and host/device transfer overhead must be included in suitability decisions and benchmarks. A GPU is selected in detection mode only when a measured, versioned policy predicts a benefit for the complete plan or batch.
10. Performance regression thresholds are benchmark-suite policy, not functional correctness criteria. Benchmark evidence identifies hardware, operating system, runtime, driver, warmup, sample count, precision, input shapes, and transfer inclusion.
