# Class specification — CalculationService

`CalculationService` validates expression and batch limits, canonicalizes
equations, creates one ordered execution plan for all cache misses, restores
cached and calculated values to input order, maintains versioned cache keys,
updates access time, and enforces the configured LRU bound transactionally.
Single calculation delegates to the same batch path. The configured GPU mode is
copied into `CalculationContext`; CPU remains the default.
