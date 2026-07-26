# Class specification — CudaRuntime

`CudaRuntime` is the acquired native-runtime boundary used by the optional CUDA
artifact. It exposes immutable device metadata and owns an idempotently
releasable device/runtime lifecycle. Core and REST modules do not depend on it.
