# REG-001 — Number

## Purpose

`com.nologo2.math2.Number` is the immutable public value type for decimal calculation results and operands.

## Requirements

1. Construct values with `fromString(String)`, `fromInteger(int)`, `fromLong(long)`, `fromFloat(float)`, and `fromDouble(double)`.
2. `fromString` accepts an optional sign, decimal digits, an optional decimal point, and optional scientific notation. It rejects blank, non-finite, and malformed values.
3. Floating-point factories use the canonical decimal representation of the supplied primitive; they do not expose its binary expansion.
4. `zero()` returns a value numerically equal to zero.
5. Values support `equals`, `hashCode`, `compareTo`, `lessThan`, and `greaterThan`. Numerically equal values compare equal regardless of scale (`1`, `1.0`, and `1.00`).
6. `toString()` returns a canonical non-scientific decimal representation with insignificant trailing zeroes removed and zero rendered as `0`.
7. The class exposes the active global digit limit through `getMaxLength()` and changes it through `setMaxLength(int)`. The default is 1000 and the minimum accepted value is 1.
8. A value whose precision exceeds the active digit limit is rejected with `UncalculableException`.
9. The canonical decimal value is retained as a `String`; `Number` must not retain a `BigDecimal` or another fixed representation as instance state. Evaluation code may create temporary arbitrary-precision arithmetic values internally.
10. Fluent arithmetic and function methods on `Number` return immutable `Equation` values rather than eagerly calculating or mutating the operand.
11. `pi()` returns pi rounded to the active global significant-digit limit. Increasing the configured limit produces a correspondingly longer value.
12. Instances are immutable and safe for concurrent use.

## Acceptance examples

- `Number.fromString("1.00").equals(Number.fromInteger(1))` is true.
- `Number.fromString("1e3").toString()` is `1000`.
- `Number.zero().toString()` is `0`.
- `Number.fromInteger(2).add(Number.fromInteger(3)).calculate()` returns `5`.
- `Number.pi()` starts with `3.14159265358979323846` and contains the configured number of significant digits.
- Adding one to an operand containing 1000 nines returns one followed by 1000 zeroes.
- Setting maximum length to zero fails without changing the current limit.
