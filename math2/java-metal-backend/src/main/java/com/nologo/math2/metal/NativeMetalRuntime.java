package com.nologo.math2.metal;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Guarded JNI bridge loaded only after an enabled planner creates the provider. */
final class NativeMetalRuntime implements MetalRuntime {
    static final String LIBRARY_NAME = "math2_metal";

    private final long handle;
    private final MetalDevice device;
    private final AtomicBoolean closed = new AtomicBoolean();

    private NativeMetalRuntime(long handle, MetalDevice device) {
        this.handle = handle;
        this.device = device;
    }

    static NativeMetalRuntime open() {
        String operatingSystem = System.getProperty("os.name", "");
        if (!isMacOs(operatingSystem)) {
            throw new UnsupportedOperationException("Metal backend requires macOS");
        }
        System.loadLibrary(LIBRARY_NAME);
        long handle = openDefaultDevice();
        if (handle == 0) {
            throw new IllegalStateException("Metal bridge returned no device");
        }
        try {
            return new NativeMetalRuntime(
                    handle,
                    new MetalDevice(
                            registryId(handle),
                            deviceName(handle),
                            memoryLimitBytes(handle)));
        } catch (RuntimeException | Error failure) {
            closeDevice(handle);
            throw failure;
        }
    }

    static boolean isMacOs(String operatingSystem) {
        return operatingSystem != null
                && operatingSystem.toLowerCase(Locale.ROOT).contains("mac");
    }

    @Override
    public MetalDevice device() {
        if (closed.get()) {
            throw new IllegalStateException("Metal runtime is closed");
        }
        return device;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            closeDevice(handle);
        }
    }

    private static native long openDefaultDevice();

    private static native String registryId(long handle);

    private static native String deviceName(long handle);

    private static native long memoryLimitBytes(long handle);

    private static native void closeDevice(long handle);
}
