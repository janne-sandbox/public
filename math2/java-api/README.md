# Math2 Java calculation API

Standalone Java 21 arbitrary-precision calculation library.

```java
Equation equation = Equation.parse("sqrt(9) + abs(-2)");
Number result = equation.calculate(100);
Number pi = Number.pi();
```

`Number` uses canonical decimal strings, exact primitive conversions, numerical equality independent of scale, a configurable 1000-significant-digit default, and `Number.pi()` at that configured precision. `Equation` supports both parsing and immutable fluent composition; parsed expressions can use `pi`, such as `2*pi`. Evaluation uses Math2's own decimal string and limb-array engine and does not delegate to Apfloat, `BigDecimal`, `BigInteger`, or another numeric engine. Undefined domains and malformed inputs throw `UncalculableException` with stable `CalculationError` reasons.

## Execution context and backend SPI

The optional execution-backend architecture provides a calculation context,
deterministic plan, reference CPU backend, planner, and extension boundary:

```java
CalculationContext context = CalculationContext.builder()
    .precision(128)
    .gpuMode(GpuMode.DISABLED)
    .build();

ExecutionPlan plan = ExecutionPlan.create(
    equation.canonicalExpression(), context);

Number contextualResult = equation.calculate(context);
```

`CalculationContext` validates precision, rounding metadata, operation, memory,
and timeout limits. `ExecutionPlan` is an immutable, ordered, versioned envelope
with a deterministic SHA-256 ID. `ComputeBackend`,
`ComputeBackendProvider`, `BackendCapabilities`, `PlanSupport`, and
`ExecutionResult` define the vendor-independent SPI.

All `Equation.calculate` overloads route through the planner and reference CPU
backend. `GpuMode.DISABLED` is the default and performs no provider discovery.
`DETECT` considers lazily discovered providers only for suitable workloads and
falls back to CPU when discovery or capability checks fail. `ENABLED` requires
a compatible accelerated backend and fails closed. The optional CUDA artifact
provides guarded discovery and lifecycle foundations but advertises no
operations until exact kernels pass hardware conformance.

Build and test from the repository root with `mvn verify`.
