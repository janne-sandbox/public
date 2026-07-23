# REG-002 — Primitive conversion

## Requirements

1. `Number` provides `toInt`, `toLong`, `toFloat`, and `toDouble`.
2. Integer conversion succeeds only when the value is integral and within the target type's range.
3. Floating conversion succeeds only when converting to the target and back yields the original decimal value and the target is finite.
4. A lossy or out-of-range conversion throws `PrecisionLossException`.
5. `PrecisionLossException` identifies the target type and exposes the nearest target value. For integer targets, fractional values are rounded to the nearest value with ties rounded to the even neighbor and range-clamped when necessary.
6. Conversion never silently truncates, overflows, or produces infinity.

## Acceptance examples

- `Number.fromString("42").toInt()` returns `42`.
- Converting `42.5` to `int` throws an exception whose nearest value is `42`.
- Converting `2147483648` to `int` throws an exception whose nearest value is `2147483647`.
