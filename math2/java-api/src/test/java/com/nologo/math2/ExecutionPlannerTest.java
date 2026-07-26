package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExecutionPlannerTest {
    @Test
    void disabledUsesCpuWithoutEnumeratingProviders() {
        Iterable<ComputeBackendProvider> forbidden = () -> {
            throw new AssertionError("DISABLED must not enumerate providers");
        };
        ExecutionPlanner planner = ExecutionPlanner.forTesting(
                new ReferenceCpuBackend(), forbidden);

        ExecutionResult result = planner.execute(plan(GpuMode.DISABLED));

        assertEquals(ReferenceCpuBackend.BACKEND_ID, result.backendId());
        assertEquals("2", result.results().getFirst().toString());
    }

    @Test
    void detectKeepsUnsuitableScalarOnCpuWithoutInitializingBackend() {
        FakeProvider provider = new FakeProvider(true, new FakeBackend(false));
        ExecutionPlanner planner = ExecutionPlanner.forTesting(
                new ReferenceCpuBackend(), List.of(provider));

        ExecutionResult result = planner.execute(plan(GpuMode.DETECT));

        assertEquals(ReferenceCpuBackend.BACKEND_ID, result.backendId());
        assertFalse(provider.metadataRead);
        assertFalse(provider.created);
    }

    @Test
    void detectSelectsSuitableAcceleratedBatchAndPreservesOrder() {
        FakeBackend backend = new FakeBackend(false);
        FakeProvider provider = new FakeProvider(true, backend);
        ExecutionPlanner planner = ExecutionPlanner.forTesting(
                new ReferenceCpuBackend(), List.of(provider));
        ExecutionPlan batch = ExecutionPlan.create(
                List.of("1+1", "2+2"),
                CalculationContext.builder()
                        .precision(200)
                        .gpuMode(GpuMode.DETECT)
                        .build());

        ExecutionResult result = planner.execute(batch);

        assertEquals("fake-gpu", result.backendId());
        assertEquals(List.of(Number.fromInteger(2), Number.fromInteger(4)),
                result.results());
        assertTrue(provider.created);
        assertTrue(backend.closed);
    }

    @Test
    void enabledFailsClosedWithoutAcceleratedProvider() {
        ExecutionPlanner planner = ExecutionPlanner.forTesting(
                new ReferenceCpuBackend(), List.of());

        UncalculableException failure = assertThrows(
                UncalculableException.class,
                () -> planner.execute(plan(GpuMode.ENABLED)));

        assertEquals(CalculationError.BACKEND_UNAVAILABLE, failure.getReason());
    }

    @Test
    void enabledExecutesAndClosesCompatibleAcceleratedBackend() {
        FakeBackend backend = new FakeBackend(false);
        FakeProvider provider = new FakeProvider(true, backend);
        ExecutionPlanner planner = ExecutionPlanner.forTesting(
                new ReferenceCpuBackend(), List.of(provider));

        ExecutionResult result = planner.execute(plan(GpuMode.ENABLED));

        assertEquals("fake-gpu", result.backendId());
        assertEquals("2", result.results().getFirst().toString());
        assertTrue(provider.created);
        assertTrue(backend.closed);
    }

    @Test
    void rejectsInvalidBackendEnvelope() {
        FakeBackend backend = new FakeBackend(true);
        ExecutionPlanner planner = ExecutionPlanner.forTesting(
                new ReferenceCpuBackend(), List.of(new FakeProvider(true, backend)));

        UncalculableException failure = assertThrows(
                UncalculableException.class,
                () -> planner.execute(plan(GpuMode.ENABLED)));

        assertEquals(CalculationError.INVALID_BACKEND_RESULT, failure.getReason());
        assertTrue(backend.closed);
    }

    @Test
    void providerCreationFailureIsReportedAsUnavailable() {
        ComputeBackendProvider broken = new ComputeBackendProvider() {
            @Override
            public String id() {
                return "broken";
            }

            @Override
            public boolean accelerated() {
                return true;
            }

            @Override
            public ComputeBackend create() {
                throw new IllegalStateException("driver");
            }
        };
        ExecutionPlanner planner = ExecutionPlanner.forTesting(
                new ReferenceCpuBackend(), List.of(broken));

        UncalculableException failure = assertThrows(
                UncalculableException.class,
                () -> planner.execute(plan(GpuMode.ENABLED)));

        assertEquals(CalculationError.BACKEND_UNAVAILABLE, failure.getReason());
    }

    @Test
    void detectFallsBackWhenNativeProviderCannotLink() {
        ExecutionPlanner planner = ExecutionPlanner.forTesting(
                new ReferenceCpuBackend(), List.of(linkageFailureProvider()));
        ExecutionPlan batch = ExecutionPlan.create(
                List.of("1+1", "2+2"),
                CalculationContext.builder()
                        .precision(200)
                        .gpuMode(GpuMode.DETECT)
                        .build());

        ExecutionResult result = planner.execute(batch);

        assertEquals(ReferenceCpuBackend.BACKEND_ID, result.backendId());
        assertEquals(List.of(Number.fromInteger(2), Number.fromInteger(4)),
                result.results());
    }

    @Test
    void enabledReportsNativeLinkFailureAsUnavailable() {
        ExecutionPlanner planner = ExecutionPlanner.forTesting(
                new ReferenceCpuBackend(), List.of(linkageFailureProvider()));

        UncalculableException failure = assertThrows(
                UncalculableException.class,
                () -> planner.execute(plan(GpuMode.ENABLED)));

        assertEquals(CalculationError.BACKEND_UNAVAILABLE, failure.getReason());
    }

    @Test
    void enabledUsesDeterministicProviderOrderAndContinuesAfterUnavailableProvider() {
        List<String> creations = new ArrayList<>();
        ComputeBackendProvider unavailableCuda = new ComputeBackendProvider() {
            @Override
            public String id() {
                return "a-cuda";
            }

            @Override
            public boolean accelerated() {
                return true;
            }

            @Override
            public ComputeBackend create() {
                creations.add(id());
                throw new UnsatisfiedLinkError("CUDA unavailable");
            }
        };
        ComputeBackendProvider compatibleMetal = new ComputeBackendProvider() {
            @Override
            public String id() {
                return "z-metal";
            }

            @Override
            public boolean accelerated() {
                return true;
            }

            @Override
            public ComputeBackend create() {
                creations.add(id());
                return new FakeBackend(false);
            }
        };
        ExecutionPlanner planner = ExecutionPlanner.forTesting(
                new ReferenceCpuBackend(),
                List.of(compatibleMetal, unavailableCuda));

        ExecutionResult result = planner.execute(plan(GpuMode.ENABLED));

        assertEquals("fake-gpu", result.backendId());
        assertEquals(List.of("a-cuda", "z-metal"), creations);
    }

    private static ComputeBackendProvider linkageFailureProvider() {
        return new ComputeBackendProvider() {
            @Override
            public String id() {
                return "unlinked-native";
            }

            @Override
            public boolean accelerated() {
                return true;
            }

            @Override
            public ComputeBackend create() {
                throw new UnsatisfiedLinkError("no native runtime");
            }
        };
    }

    private static ExecutionPlan plan(GpuMode mode) {
        return ExecutionPlan.create(
                "1+1",
                CalculationContext.builder()
                        .precision(20)
                        .gpuMode(mode)
                        .build());
    }

    private static final class FakeProvider implements ComputeBackendProvider {
        private final boolean accelerated;
        private final ComputeBackend backend;
        private boolean metadataRead;
        private boolean created;

        private FakeProvider(boolean accelerated, ComputeBackend backend) {
            this.accelerated = accelerated;
            this.backend = backend;
        }

        @Override
        public String id() {
            metadataRead = true;
            return "fake-provider";
        }

        @Override
        public boolean accelerated() {
            return accelerated;
        }

        @Override
        public ComputeBackend create() {
            created = true;
            return backend;
        }
    }

    private static final class FakeBackend implements ComputeBackend {
        private final boolean invalidEnvelope;
        private boolean closed;

        private FakeBackend(boolean invalidEnvelope) {
            this.invalidEnvelope = invalidEnvelope;
        }

        @Override
        public BackendCapabilities capabilities() {
            return new BackendCapabilities(
                    "fake-gpu", "1", true, Set.of(1), 1, Integer.MAX_VALUE,
                    Set.of(0, 1, 2, 3, 4, 5, 6), 100, Long.MAX_VALUE);
        }

        @Override
        public ExecutionResult execute(ExecutionPlan plan) {
            return new ExecutionResult(
                    invalidEnvelope ? "wrong-plan" : plan.planId(),
                    plan.engineVersion(),
                    "fake-gpu",
                    plan.canonicalExpressions().stream()
                            .map(expression -> expression.equals("2+2")
                                    ? Number.fromInteger(4)
                                    : Number.fromInteger(2))
                            .toList(),
                    1,
                    1);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
