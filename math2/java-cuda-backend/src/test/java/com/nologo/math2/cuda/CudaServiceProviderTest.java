package com.nologo.math2.cuda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nologo.math2.ComputeBackendProvider;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class CudaServiceProviderTest {
    @Test
    void serviceLoaderDiscoversMetadataWithoutOpeningNativeRuntime() {
        ComputeBackendProvider provider = ServiceLoader.load(ComputeBackendProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(candidate -> candidate.id().equals(CudaBackendProvider.PROVIDER_ID))
                .findFirst()
                .orElseThrow();

        assertEquals(CudaBackendProvider.PROVIDER_ID, provider.id());
    }
}
