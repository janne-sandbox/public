package com.nologo.math2.cuda;

/** Acquired native CUDA runtime/device boundary. */
public interface CudaRuntime extends AutoCloseable {
    CudaDevice device();

    @Override
    void close();
}
