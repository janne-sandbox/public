# Class specification — BuiltInOperator

`BuiltInOperator` is the public enum implementing every operator in REG-003. It maps syntax to Math2's own string-and-limb arbitrary-precision evaluator, whose precision is not represented by a third-party decimal type or `BigDecimal`'s 32-bit scale. The engine applies guard digits and HALF_EVEN output rounding and consistently translates undefined arithmetic into stable Math2 errors.
