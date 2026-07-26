package com.nologo.math2.cuda;

import java.util.concurrent.atomic.AtomicBoolean;

/** Guarded JNI bridge loaded only after an enabled planner creates the provider. */
final class NativeCudaRuntime implements CudaRuntime {
    static final String LIBRARY_NAME = "math2_cuda";

    private final long handle;
    private final CudaDevice device;
    private final AtomicBoolean closed = new AtomicBoolean();

    private NativeCudaRuntime(long handle, CudaDevice device) {
        this.handle = handle;
        this.device = device;
    }

    static NativeCudaRuntime open() {
        System.loadLibrary(LIBRARY_NAME);
        long handle = openDefaultDevice();
        if (handle == 0) {
            throw new IllegalStateException("CUDA bridge returned no device");
        }
        try {
            return new NativeCudaRuntime(
                    handle,
                    new CudaDevice(
                            deviceId(handle),
                            deviceName(handle),
                            totalMemoryBytes(handle)));
        } catch (RuntimeException | Error failure) {
            closeDevice(handle);
            throw failure;
        }
    }

    @Override
    public CudaDevice device() {
        if (closed.get()) {
            throw new IllegalStateException("CUDA runtime is closed");
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

    private static native String deviceId(long handle);

    private static native String deviceName(long handle);

    private static native long totalMemoryBytes(long handle);

    private static native void closeDevice(long handle);
}
