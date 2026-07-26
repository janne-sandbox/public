# Class specification — BackendCapabilities

`BackendCapabilities` is an immutable backend capability descriptor. It records
backend ID and implementation version, whether the backend is accelerated,
supported plan versions, inclusive precision range, supported rounding codes,
maximum batch size, and maximum buffer bytes.

IDs and versions are nonblank; sets are nonempty immutable copies; limits are
positive and internally ordered; rounding codes are validated against
`Number.ROUNDING_MODE_COUNT`.

`supportFor(plan)` deterministically rejects unsupported plan version,
precision, rounding, batch size, or estimated memory budget and otherwise
returns supported.
