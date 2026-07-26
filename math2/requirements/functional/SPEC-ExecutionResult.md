# Class specification — ExecutionResult

`ExecutionResult` is an immutable backend result envelope containing the plan
ID, engine version, backend ID, ordered non-null `Number` results, execution
duration, and transfer duration. Identifiers are nonblank, versions are
positive, durations are non-negative, and the result list is a nonempty
unmodifiable copy.

Result validation against the originating plan occurs at the planner boundary
in phase 2; constructing an envelope does not make an untrusted backend result
valid.
