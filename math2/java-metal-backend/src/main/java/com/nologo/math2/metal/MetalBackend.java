package com.nologo.math2.metal;

import com.nologo.math2.BackendCapabilities;
import com.nologo.math2.CalculationError;
import com.nologo.math2.ComputeBackend;
import com.nologo.math2.ExecutionPlan;
import com.nologo.math2.ExecutionResult;
import com.nologo.math2.Number;
import com.nologo.math2.PlanSupport;
import com.nologo.math2.UncalculableException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Metal backend lifecycle foundation. It advertises no executable plans until
 * exact arbitrary-precision kernels pass conformance.
 */
public final class MetalBackend implements ComputeBackend {
    public static final String IMPLEMENTATION_VERSION = "0-foundation";
    private static final Set<Integer> ROUNDING_MODES = Set.of(0, 1, 2, 3, 4, 5, 6);

    private final MetalRuntime runtime;
    private final BackendCapabilities capabilities;
    private final AtomicBoolean closed = new AtomicBoolean();

    MetalBackend(MetalRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        MetalDevice device = Objects.requireNonNull(runtime.device(), "runtime device");
        this.capabilities = new BackendCapabilities(
                "math2-metal:" + device.registryId(),
                IMPLEMENTATION_VERSION,
                true,
                Set.of(ExecutionPlan.CURRENT_FORMAT_VERSION),
                1,
                Number.DEFAULT_MAX_LENGTH,
                ROUNDING_MODES,
                1,
                device.memoryLimitBytes());
    }

    @Override
    public BackendCapabilities capabilities() {
        ensureOpen();
        return capabilities;
    }

    @Override
    public PlanSupport supports(ExecutionPlan plan) {
        Objects.requireNonNull(plan, "plan");
        ensureOpen();
        return PlanSupport.unsupported(
                "Exact Metal kernels are not available in the foundation artifact");
    }

    @Override
    public ExecutionResult execute(ExecutionPlan plan) {
        Objects.requireNonNull(plan, "plan");
        ensureOpen();
        throw new UncalculableException(
                CalculationError.BACKEND_UNAVAILABLE,
                "Metal backend has no conforming exact kernels");
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            runtime.close();
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Metal backend is closed");
        }
    }
}
