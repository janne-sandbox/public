# Class specification — Math2Properties

`Math2Properties` binds and validates cache size, batch size, expression-length
limits, service-wide maximum significant-digit precision, rounding default, and
GPU mode. It applies configured numeric defaults to the Java `Number` API at
startup. GPU mode defaults to `DISABLED`; invalid enum values prevent binding.
Defaults match REG-006, REG-007, REG-009, and NFR-001.
