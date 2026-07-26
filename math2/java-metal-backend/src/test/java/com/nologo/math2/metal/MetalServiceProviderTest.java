package com.nologo.math2.metal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nologo.math2.ComputeBackendProvider;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class MetalServiceProviderTest {
    @Test
    void serviceLoaderDiscoversMetadataWithoutOpeningNativeRuntime() {
        ComputeBackendProvider provider = ServiceLoader.load(ComputeBackendProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(candidate -> candidate.id().equals(MetalBackendProvider.PROVIDER_ID))
                .findFirst()
                .orElseThrow();

        assertEquals(MetalBackendProvider.PROVIDER_ID, provider.id());
    }
}
