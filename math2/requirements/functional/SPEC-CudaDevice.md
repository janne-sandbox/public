# Class specification — CudaDevice

`CudaDevice` is immutable discovered-device metadata. It retains a nonblank
stable device ID, nonblank diagnostic name, and positive total-memory bound.
It contains no native handles and performs no discovery.
