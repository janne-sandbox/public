package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ComputeBackendProviderTest {
    @Test
    void metadataDoesNotForceBackendCreation() {
        CountingProvider provider = new CountingProvider();

        assertEquals("counting", provider.id());
        assertFalse(provider.accelerated());
        assertEquals(0, provider.creations);
    }

    private static final class CountingProvider implements ComputeBackendProvider {
        private int creations;

        @Override
        public String id() {
            return "counting";
        }

        @Override
        public boolean accelerated() {
            return false;
        }

        @Override
        public ComputeBackend create() {
            creations++;
            throw new UnsupportedOperationException("Not needed by this test");
        }
    }
}
