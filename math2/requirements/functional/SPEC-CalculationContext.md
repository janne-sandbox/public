# Class specification — CalculationContext

`CalculationContext` is an immutable public calculation configuration built
through `builder()`. It carries positive significant-digit precision not above
`Number.getMaxLength()`, `GpuMode` (default `DISABLED`), a supported rounding
mode code, a positive operation budget, a positive memory-byte budget, and a
positive timeout.

Builder defaults are captured when `build()` is called: current maximum
precision, current default rounding mode, GPU disabled, 1,000,000 operations,
256 MiB, and 30 seconds. Nulls, invalid rounding codes, non-positive limits,
precision above the configured maximum, and timeout values that cannot be
represented as positive milliseconds are rejected.

The rounding code is plan/capability metadata. Rounding modes already captured
inside an immutable `Equation` remain authoritative.
