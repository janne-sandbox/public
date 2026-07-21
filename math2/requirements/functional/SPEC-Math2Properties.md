# Class specification — Math2Properties

`Math2Properties` binds and validates cache size, batch size, expression-length limits, and the service-wide maximum significant-digit precision. It applies the configured precision to the Java `Number` API at startup. Defaults match REG-006, REG-007, and NFR-001.
