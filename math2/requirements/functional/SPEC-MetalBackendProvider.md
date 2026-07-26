# Class specification — MetalBackendProvider

`MetalBackendProvider` is the Java service provider for the optional Metal
artifact. Its stable ID and accelerated flag are side-effect free. Runtime
loading and device discovery occur only in `create()`. Backend-construction
failure closes the acquired runtime. Tests inject deterministic runtimes and
must require no Metal library or hardware.
