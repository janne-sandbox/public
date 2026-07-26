# REG-009 — Execution planning and optional GPU backends

## Status

Partially implemented. Phases 1–3 provide immutable calculation context, GPU
mode, execution-plan envelope, capabilities, support/result values, backend
provider SPI, reference CPU routing, deterministic selection, and ordered REST
batch scheduling. Optional CUDA and Metal artifacts provide guarded native
loading, device lifecycle, empty operation capabilities, and deterministic
non-hardware fixtures. Exact CUDA/Metal kernels, memory pooling, and hardware
conformance remain target requirements.

## Configuration and compatibility

1. Existing `Equation.calculate()` and `Equation.calculate(int)` calls remain source- and behavior-compatible.
2. The execution path is selected by a `CalculationContext`. A context carries the requested significant-digit precision, the rounding configuration captured by the equation, and `GpuMode`.
3. `GpuMode` has exactly these externally configurable values:
   - `DISABLED`: use the reference CPU backend and do not probe GPU libraries or hardware.
   - `DETECT`: discover compatible GPU backends and use one only when the complete plan is supported and the planner predicts a benefit; otherwise use CPU.
   - `ENABLED`: require a compatible GPU backend for every submitted plan and fail before execution when none can execute it. No silent CPU fallback is allowed.
4. The default is `DISABLED`. Merely adding an optional backend JAR must not load native libraries or probe hardware in this mode.
5. The service exposes the same setting as `math2.gpu.mode` / `MATH2_GPU_MODE`. Invalid values prevent startup.
6. GPU artifacts are optional. Math2 must build, test, start, and calculate on hosts without GPU hardware, Docker, drivers, or vendor SDKs.

## Planning and backend SPI

7. An immutable `ExecutionPlan` is produced from an equation tree before backend execution. The plan records its format version, canonical expression or typed nodes, result type, precision, rounding behavior, resource limits, and stable operation identifiers.
8. The planner may perform semantics-preserving constant folding, dead-node removal, common-subexpression elimination, kernel fusion, backend selection, and memory scheduling. It must not itself perform user-visible arithmetic.
9. Planning must be deterministic. The same engine version, equation, context, and capability set produce the same plan and backend decision.
10. Backend selection is capability-based and must not rely only on a backend name. Capabilities include supported plan versions, operations, value representations, precision range, rounding modes, maximum batch and buffer sizes, device identity, and backend implementation version.
11. A backend must perform an all-or-nothing `supports(plan)` check before execution. Partial execution, fallback after a partial result, and mixing CPU/GPU results inside one scalar equation are forbidden until a separately specified deterministic partitioning protocol exists.
12. Backend discovery uses the Java service-provider mechanism from an optional backend module. Discovery failures are isolated in `DETECT`, observable through diagnostics, and fatal in `ENABLED`.
13. The CPU backend is the reference implementation and must support every valid plan.
14. CUDA and Metal backends are separate optional modules. The core, planner,
    REST service, and CPU backend have no compile-time or runtime dependency on
    CUDA, Metal, or Docker. Provider metadata access is platform-neutral and
    must not load native code.

## Numerical behavior

15. A backend result must be byte-for-byte identical to the canonical decimal string produced by the reference CPU backend for the same equation, precision, rounding configuration, and engine version. A tolerance, ULP allowance, or primitive floating-point approximation is not acceptable.
16. CUDA and Metal calculations preserve configurable significant-digit
    precision and every supported rounding mode. Precision must not be silently
    capped by device-native numeric widths.
17. GPU arithmetic uses a documented arbitrary-precision representation and deterministic integer operations. Primitive `float` or `double` may be used only for non-semantic scheduling estimates, never as calculation values or intermediates.
18. A backend advertises only operations for which it implements the reference algorithm and exact public-boundary rounding. Unsupported operations make the complete plan unsupported.
19. Backend output is validated for plan identity, engine version, result shape, precision metadata, and canonical decimal form before it becomes a public result or cache entry.
20. Cache identity includes the calculation engine version and every context field that can affect the result. Backend identity is diagnostic metadata, not a reason for different result keys or values.

## Batch behavior

21. The planner accepts ordered batches originating from the REST service as one scheduling unit while retaining one result or one indexed error per input equation.
22. Backend execution preserves input order regardless of kernel, stream, or device completion order.
23. Batch limits, expression-length limits, precision limits, timeouts, and memory budgets are checked before device allocation.
24. In `DETECT`, unsupported batch items may be routed to CPU only when each equation remains an independent, atomic plan and ordering is restored deterministically. In `ENABLED`, any unsupported item rejects the batch before any item executes.
25. Cancellation, timeout, or device failure invalidates all unfinished device outputs and releases or returns pooled buffers. No incomplete result may be cached.

## Observability and failure behavior

26. Diagnostics expose configured mode, selected backend, backend version, device identity, plan version, fallback reason, execution duration, transfer duration, and batch size without exposing equation contents by default.
27. In `DETECT`, unavailability or a failure before execution may fall back once to CPU. A failure after backend execution begins is not retried automatically unless the backend proves that execution is side-effect-free and discards every partial output.
28. In `ENABLED`, discovery, capability, allocation, driver, timeout, and execution failures produce a typed calculation error and never fall back.
29. Device allocations are bounded, pooled where beneficial, cleared before reuse across trust boundaries, and released on normal completion, cancellation, and failure.

## Acceptance

30. Every production class added for this capability has a class specification and focused tests.
31. A backend conformance suite runs unchanged against CPU, CUDA, and Metal implementations.
32. Conformance includes fixed vectors, randomized/property cases, precision and rounding boundaries, batch ordering, unsupported operations, discovery failures, fallback rules, cancellation, resource exhaustion, and canonical output validation.
33. Normal CI runs without a GPU using fake capability providers and deterministic backend fixtures. Declared CUDA and Apple Metal hardware CI are additional required release gates for their respective artifacts, not replacements for normal CI.
34. Native-backend release evidence records the backend kind, device, operating
    system, driver/runtime, backend and engine versions, exact test command,
    test counts, failures, skips, and benchmark conditions. CUDA and Metal
    evidence are reported separately.
35. Performance gates may decide whether `DETECT` selects a GPU, but performance can never relax numerical conformance.
36. A CUDA functional or microarchitecture simulator may provide an additional
    no-hardware verification tier. Simulator evidence records the simulator,
    image/source revision, emulated architecture, CUDA/PTX version, exact
    command, results, failures, and skips. It does not replace the hardware
    release gate because it cannot validate the shipped driver/JNI/device path
    or physical-device behavior.
37. Docker and CUDA simulation are development and verification tools, not
    runtime prerequisites. On Metal-capable macOS, `DETECT` may select a
    conforming Metal provider when the complete plan is suitable. `ENABLED`
    accepts any conforming installed accelerated provider and fails closed when
    none can execute the complete plan.
38. When multiple accelerated providers are installed, discovery and selection
    are deterministic by stable provider ID after capability and suitability
    checks. An unavailable provider does not prevent `DETECT` from considering
    the next provider; `ENABLED` considers every provider before reporting that
    no conforming accelerated backend is available.
39. The optional Metal artifact uses a guarded native bridge on macOS, bounded
    device resources, and exact integer-limb compute kernels. It advertises no
    operation until unchanged CPU/Metal conformance passes; primitive
    floating-point and tolerance-based equality are forbidden for calculation
    values and intermediates.
