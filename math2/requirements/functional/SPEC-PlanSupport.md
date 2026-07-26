# Class specification — PlanSupport

`PlanSupport` is an immutable result of backend capability validation.
`supported()` has no rejection reason. `unsupported(reason)` requires a
nonblank, diagnostic-only reason. It does not contain equation text.
