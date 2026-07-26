# Class specification — BackendSelectionPolicy

`BackendSelectionPolicy` is the deterministic phase-3 suitability gate used
only by `GpuMode.DETECT`. It never selects a non-accelerated or unsupported
backend. Scalar plans stay on CPU. An ordered batch is suitable only when it has
at least two expressions and its deterministic work estimate (total canonical
expression characters multiplied by precision) meets the documented minimum.

The policy performs no arithmetic, hardware probing, timing, or mutable
learning. `DISABLED` and `ENABLED` semantics do not depend on this heuristic.
