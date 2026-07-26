package com.nologo.math2.metal;

/** Acquired native Metal runtime/device boundary. */
public interface MetalRuntime extends AutoCloseable {
    MetalDevice device();

    @Override
    void close();
}
