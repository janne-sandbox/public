package com.nologo.math2.cuda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CudaDeviceTest {
    @Test
    void validatesAndRetainsDeviceMetadata() {
        CudaDevice device = new CudaDevice("gpu-0", "Fake CUDA", 4096);

        assertEquals("gpu-0", device.id());
        assertEquals("Fake CUDA", device.name());
        assertEquals(4096, device.totalMemoryBytes());
        assertThrows(IllegalArgumentException.class, () -> new CudaDevice("", "GPU", 1));
        assertThrows(IllegalArgumentException.class, () -> new CudaDevice("0", "", 1));
        assertThrows(IllegalArgumentException.class, () -> new CudaDevice("0", "GPU", 0));
    }
}
