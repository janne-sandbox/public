# Class specification — GpuMode

`GpuMode` is the public, stable execution-acceleration policy from REG-009. It
contains exactly `DISABLED`, `DETECT`, and `ENABLED`; enum order has no protocol
meaning. `DISABLED` is the default used by `CalculationContext`.
