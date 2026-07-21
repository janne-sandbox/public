# Math2 Java calculation API

Standalone Java 21 arbitrary-precision calculation library.

```java
Equation equation = Equation.parse("sqrt(9) + abs(-2)");
Number result = equation.calculate(100);
Number pi = Number.pi();
```

`Number` uses canonical decimal strings, exact primitive conversions, numerical equality independent of scale, a configurable 1000-significant-digit default, and `Number.pi()` at that configured precision. `Equation` supports both parsing and immutable fluent composition; parsed expressions can use `pi`, such as `2*pi`. Evaluation uses Math2's own decimal string and limb-array engine and does not delegate to Apfloat, `BigDecimal`, `BigInteger`, or another numeric engine. Undefined domains and malformed inputs throw `UncalculableException` with stable `CalculationError` reasons.

Build and test from the repository root with `mvn verify`.
