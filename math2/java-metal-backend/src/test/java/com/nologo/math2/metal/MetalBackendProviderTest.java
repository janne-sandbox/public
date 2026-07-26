package com.nologo.math2.metal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nologo.math2.CalculationContext;
import com.nologo.math2.CalculationError;
import com.nologo.math2.ComputeBackend;
import com.nologo.math2.ExecutionPlan;
import com.nologo.math2.UncalculableException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MetalBackendProviderTest {
    @Test
    void metadataDoesNotOpenRuntime() {
        AtomicInteger opens = new AtomicInteger();
        MetalBackendProvider provider = new MetalBackendProvider(() -> {
            opens.incrementAndGet();
            return new FakeRuntime();
        });

        assertEquals(MetalBackendProvider.PROVIDER_ID, provider.id());
        assertTrue(provider.accelerated());
        assertEquals(0, opens.get());
    }

    @Test
    void createsCapabilityOnlyBackendAndClosesRuntimeOnce() {
        FakeRuntime runtime = new FakeRuntime();
        MetalBackendProvider provider = new MetalBackendProvider(() -> runtime);
        ExecutionPlan plan = ExecutionPlan.create(
                "1+1", CalculationContext.builder().build());

        ComputeBackend backend = provider.create();

        assertTrue(backend.capabilities().accelerated());
        assertEquals("math2-metal:fake-metal-0", backend.capabilities().backendId());
        assertFalse(backend.supports(plan).isSupported());
        UncalculableException failure = assertThrows(
                UncalculableException.class, () -> backend.execute(plan));
        assertEquals(CalculationError.BACKEND_UNAVAILABLE, failure.getReason());

        backend.close();
        backend.close();
        assertEquals(1, runtime.closes);
        assertThrows(IllegalStateException.class, backend::capabilities);
    }

    @Test
    void closesRuntimeWhenBackendInitializationFails() {
        FakeRuntime runtime = new FakeRuntime() {
            @Override
            public MetalDevice device() {
                throw new IllegalStateException("probe failed");
            }
        };
        MetalBackendProvider provider = new MetalBackendProvider(() -> runtime);

        assertThrows(IllegalStateException.class, provider::create);
        assertEquals(1, runtime.closes);
    }

    private static class FakeRuntime implements MetalRuntime {
        private int closes;

        @Override
        public MetalDevice device() {
            return new MetalDevice("fake-metal-0", "Deterministic fake Metal", 4096);
        }

        @Override
        public void close() {
            closes++;
        }
    }
}
