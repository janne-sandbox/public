# Class specification — ExecutionPlanner

`ExecutionPlanner` is the phase-2 execution boundary. `system()` creates a
planner with the reference CPU backend and lazy Java service-provider discovery.
`execute(plan)` validates the plan and routes `DISABLED` plans to CPU without
enumerating or initializing providers. `DETECT` sorts accelerated providers by
stable ID, applies `BackendSelectionPolicy`, selects the first complete-plan
compatible backend, and otherwise uses CPU.
Provider discovery, construction, capability, and native linkage failures are
isolated before execution in `DETECT`.

`ENABLED` ignores the suitability heuristic and requires an accelerated provider that can create a backend supporting
the complete plan. Provider discovery/creation failures and absence of a
compatible backend produce `BACKEND_UNAVAILABLE`; execution failures produce
`BACKEND_EXECUTION_FAILED`.

Every backend result is validated for plan ID, engine version, backend ID,
ordered result count, and non-null canonical `Number` values before exposure.
Invalid envelopes produce `INVALID_BACKEND_RESULT`. Created backends are closed
on support rejection, success, and failure.
