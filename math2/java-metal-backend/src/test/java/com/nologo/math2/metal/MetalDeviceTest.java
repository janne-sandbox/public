package com.nologo.math2.metal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MetalDeviceTest {
    @Test
    void validatesAndRetainsDeviceMetadata() {
        MetalDevice device = new MetalDevice("metal-0", "Fake Metal", 4096);

        assertEquals("metal-0", device.registryId());
        assertEquals("Fake Metal", device.name());
        assertEquals(4096, device.memoryLimitBytes());
        assertThrows(IllegalArgumentException.class,
                () -> new MetalDevice("", "GPU", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new MetalDevice("0", "", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new MetalDevice("0", "GPU", 0));
    }
}
