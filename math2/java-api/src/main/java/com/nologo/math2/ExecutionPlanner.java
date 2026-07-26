package com.nologo.math2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/** Validates plans, selects a complete-plan backend, and validates backend output. */
public final class ExecutionPlanner {
    private static final ExecutionPlanner SYSTEM = new ExecutionPlanner(
            new ReferenceCpuBackend(),
            new BackendSelectionPolicy(),
            () -> ServiceLoader.load(ComputeBackendProvider.class));

    private final ComputeBackend cpuBackend;
    private final BackendSelectionPolicy selectionPolicy;
    private final ProviderSource providerSource;

    private ExecutionPlanner(
            ComputeBackend cpuBackend,
            BackendSelectionPolicy selectionPolicy,
            ProviderSource providerSource) {
        this.cpuBackend = Objects.requireNonNull(cpuBackend, "cpuBackend");
        this.selectionPolicy = Objects.requireNonNull(selectionPolicy, "selectionPolicy");
        this.providerSource = Objects.requireNonNull(providerSource, "providerSource");
    }

    static ExecutionPlanner forTesting(
            ComputeBackend cpuBackend, Iterable<ComputeBackendProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        return new ExecutionPlanner(cpuBackend, new BackendSelectionPolicy(), () -> providers);
    }

    public static ExecutionPlanner system() {
        return SYSTEM;
    }

    public ExecutionResult execute(ExecutionPlan plan) {
        Objects.requireNonNull(plan, "plan");
        return switch (plan.gpuMode()) {
            case DISABLED -> executeAndValidate(cpuBackend, plan);
            case DETECT -> executeDetected(plan);
            case ENABLED -> executeRequiredAccelerated(plan);
        };
    }

    private ExecutionResult executeDetected(ExecutionPlan plan) {
        if (!selectionPolicy.mayAccelerate(plan)) {
            return executeAndValidate(cpuBackend, plan);
        }
        for (ComputeBackendProvider provider : discoverProviderMetadata(false)) {
            if (!provider.accelerated()) {
                continue;
            }
            ComputeBackend backend = null;
            try {
                backend = Objects.requireNonNull(provider.create(), "provider backend");
                if (!selectionPolicy.isSuitable(plan, backend.capabilities())) {
                    continue;
                }
                return executeAndValidate(backend, plan);
            } catch (UncalculableException executionStartedOrInvalid) {
                throw executionStartedOrInvalid;
            } catch (RuntimeException | LinkageError unavailableBeforeExecution) {
                // Detection mode isolates provider creation/capability failures.
            } finally {
                closeQuietly(backend);
            }
        }
        return executeAndValidate(cpuBackend, plan);
    }

    private ExecutionResult executeRequiredAccelerated(ExecutionPlan plan) {
        List<String> failures = new ArrayList<>();
        for (ComputeBackendProvider provider : discoverProviderMetadata(true)) {
            if (!provider.accelerated()) {
                continue;
            }
            ComputeBackend backend = null;
            try {
                backend = Objects.requireNonNull(provider.create(), "provider backend");
                PlanSupport support = backend.supports(plan);
                if (!support.isSupported()) {
                    failures.add(provider.id() + ": "
                            + support.reason().orElse("unsupported plan"));
                    continue;
                }
                return executeAndValidate(backend, plan);
            } catch (UncalculableException exception) {
                throw exception;
            } catch (RuntimeException | LinkageError exception) {
                failures.add(safeProviderId(provider) + ": "
                        + exception.getClass().getSimpleName());
            } finally {
                closeQuietly(backend);
            }
        }
        String detail = failures.isEmpty() ? "no accelerated provider"
                : String.join(", ", failures);
        throw new UncalculableException(
                CalculationError.BACKEND_UNAVAILABLE,
                "GPU execution is required but unavailable: " + detail);
    }

    private List<ComputeBackendProvider> discoverProviderMetadata(boolean required) {
        List<ComputeBackendProvider> providers = new ArrayList<>();
        try {
            for (ComputeBackendProvider provider : providerSource.providers()) {
                if (provider != null) {
                    // Accessing metadata must be side-effect free by SPI contract.
                    String id = provider.id();
                    if (id == null || id.isBlank()) {
                        if (required) {
                            throw new IllegalStateException("Provider ID is blank");
                        }
                        continue;
                    }
                    providers.add(provider);
                }
            }
        } catch (RuntimeException | LinkageError
                | java.util.ServiceConfigurationError exception) {
            if (required) {
                throw new UncalculableException(
                        CalculationError.BACKEND_UNAVAILABLE,
                        "GPU backend discovery failed",
                        exception);
            }
        }
        providers.sort(Comparator.comparing(ExecutionPlanner::safeProviderId));
        return providers;
    }

    private ExecutionResult executeAndValidate(ComputeBackend backend, ExecutionPlan plan) {
        BackendCapabilities capabilities = Objects.requireNonNull(
                backend.capabilities(), "backend capabilities");
        PlanSupport support = backend.supports(plan);
        if (!support.isSupported()) {
            throw new UncalculableException(
                    CalculationError.BACKEND_UNAVAILABLE,
                    "Backend " + capabilities.backendId() + " does not support plan: "
                            + support.reason().orElse("unspecified reason"));
        }

        final ExecutionResult result;
        try {
            result = Objects.requireNonNull(backend.execute(plan), "backend result");
        } catch (UncalculableException exception) {
            throw exception;
        } catch (RuntimeException | LinkageError exception) {
            throw new UncalculableException(
                    CalculationError.BACKEND_EXECUTION_FAILED,
                    "Backend " + capabilities.backendId() + " execution failed",
                    exception);
        }
        validateResult(capabilities, plan, result);
        return result;
    }

    private static void validateResult(
            BackendCapabilities capabilities, ExecutionPlan plan, ExecutionResult result) {
        boolean valid = plan.planId().equals(result.planId())
                && plan.engineVersion() == result.engineVersion()
                && capabilities.backendId().equals(result.backendId())
                && plan.canonicalExpressions().size() == result.results().size();
        if (!valid) {
            throw new UncalculableException(
                    CalculationError.INVALID_BACKEND_RESULT,
                    "Backend returned an invalid result envelope");
        }
    }

    private static String safeProviderId(ComputeBackendProvider provider) {
        try {
            String id = provider.id();
            return id == null || id.isBlank() ? "unknown-provider" : id;
        } catch (RuntimeException exception) {
            return "unknown-provider";
        }
    }

    private static void closeQuietly(ComputeBackend backend) {
        if (backend == null) {
            return;
        }
        try {
            backend.close();
        } catch (RuntimeException | LinkageError ignored) {
            // The calculation result/failure remains authoritative.
        }
    }

    @FunctionalInterface
    private interface ProviderSource {
        Iterable<ComputeBackendProvider> providers();
    }
}
