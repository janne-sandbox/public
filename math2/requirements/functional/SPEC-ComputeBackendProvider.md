# Class specification — ComputeBackendProvider

`ComputeBackendProvider` is the Java service-provider discovery boundary.
Metadata access (`id()` and `accelerated()`) must not load native libraries,
probe devices, or allocate backend resources. `create()` performs guarded
initialization only after the execution mode permits discovery and returns one
backend instance or a typed/runtime discovery failure for planner isolation.
