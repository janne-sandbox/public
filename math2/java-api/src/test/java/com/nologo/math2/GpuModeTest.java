package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class GpuModeTest {
    @Test
    void exposesOnlyTheThreeStablePolicies() {
        assertArrayEquals(
                new GpuMode[] {GpuMode.DISABLED, GpuMode.DETECT, GpuMode.ENABLED},
                GpuMode.values());
    }
}
