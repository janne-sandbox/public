# Class specification — ExecutionPlan

`ExecutionPlan` is an immutable, versioned, ordered calculation-plan envelope.
`create(expressions, context)` accepts one or more nonblank canonical
expressions, copies them in input order, copies every calculation-context field
(including `GpuMode`),
records Math2 plan format and engine versions, and computes a deterministic
lowercase SHA-256 plan ID from an unambiguous length-prefixed serialization.

The returned expression list is unmodifiable. The plan contains no device or
backend identity. Equal semantic inputs produce equal plans and plan IDs.
