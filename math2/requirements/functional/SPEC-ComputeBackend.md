# Class specification — ComputeBackend

`ComputeBackend` is the vendor-independent execution SPI. It exposes immutable
capabilities, performs a complete-plan support check, and executes a supported
immutable plan to an `ExecutionResult`. Implementations reject nulls and must
not partially execute unsupported plans. `close()` is idempotent and defaults
to no work for backends without resources.
