package com.nologo.math2.metal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NativeMetalRuntimeTest {
    @Test
    void usesDedicatedNativeLibraryName() {
        assertEquals("math2_metal", NativeMetalRuntime.LIBRARY_NAME);
    }

    @Test
    void recognizesOnlyMacOsAsMetalHost() {
        assertTrue(NativeMetalRuntime.isMacOs("Mac OS X"));
        assertTrue(NativeMetalRuntime.isMacOs("macOS"));
        assertFalse(NativeMetalRuntime.isMacOs("Linux"));
        assertFalse(NativeMetalRuntime.isMacOs(null));
    }
}
