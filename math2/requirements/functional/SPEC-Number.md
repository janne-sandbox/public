# Class specification — Number

`Number` is the immutable, thread-safe, string-backed public decimal value type specified by REG-001 and REG-002. Construction is through named factories, `zero`, and the configured-precision `pi` constant; its instance state retains the canonical decimal string and exposes no mutable decimal representation. Arithmetic methods create immutable `Equation` trees for deferred evaluation.
