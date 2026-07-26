# Class specification — ReferenceCpuBackend

`ReferenceCpuBackend` is the complete reference implementation of
`ComputeBackend`. It supports every valid current plan version, configured
precision and rounding code, ordered batch size, and memory budget accepted by
the Java API. It evaluates each canonical expression through the existing Math2
equation engine at the plan precision, preserves order, reports zero transfer
time, and returns an envelope matching the plan and backend identities.

Unsupported plans are rejected before evaluation. The backend has no native
resources and closing it is harmless.
