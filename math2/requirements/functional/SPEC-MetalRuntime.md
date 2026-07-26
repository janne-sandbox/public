# Class specification — MetalRuntime

`MetalRuntime` is the acquired native-runtime boundary used by the optional
Metal artifact. It exposes immutable device metadata and owns an idempotently
releasable device/runtime lifecycle. Core and REST modules do not depend on it.
