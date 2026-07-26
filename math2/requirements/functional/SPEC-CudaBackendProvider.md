# Class specification — CudaBackendProvider

`CudaBackendProvider` is the Java service provider for the optional CUDA
artifact. Its stable ID and accelerated flag are side-effect free. Runtime
loading and device discovery occur only in `create()`. Backend-construction
failure closes the acquired runtime. Tests inject deterministic runtimes and
must require no CUDA library, driver, or hardware.
