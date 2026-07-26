# Class specification — MetalBackend

`MetalBackend` owns one acquired `MetalRuntime` and reports accelerated,
device-specific capability metadata. The foundation implementation advertises
no supported execution plans and rejects execution with
`BACKEND_UNAVAILABLE`; exact operations may be advertised only after their
unchanged CPU/Metal conformance suite passes. Close releases the runtime once
and makes subsequent backend use invalid.
