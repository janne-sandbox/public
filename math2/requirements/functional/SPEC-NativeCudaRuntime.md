# Class specification — NativeCudaRuntime

`NativeCudaRuntime` is the guarded JNI lifecycle adapter. It loads the
`math2_cuda` bridge and opens the default device only when
`CudaBackendProvider.create()` is called. Provider construction and metadata
access must not load a library or probe hardware. Failed acquisition releases
any opened handle, and close is idempotent.
