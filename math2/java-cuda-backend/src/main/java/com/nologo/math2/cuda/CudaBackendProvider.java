package com.nologo.math2.cuda;

import com.nologo.math2.ComputeBackend;
import com.nologo.math2.ComputeBackendProvider;
import java.util.Objects;

/** Lazy service provider for the optional CUDA backend artifact. */
public final class CudaBackendProvider implements ComputeBackendProvider {
    public static final String PROVIDER_ID = "math2-cuda";

    private final RuntimeFactory runtimeFactory;

    public CudaBackendProvider() {
        this(NativeCudaRuntime::open);
    }

    CudaBackendProvider(RuntimeFactory runtimeFactory) {
        this.runtimeFactory = Objects.requireNonNull(runtimeFactory, "runtimeFactory");
    }

    @Override
    public String id() {
        return PROVIDER_ID;
    }

    @Override
    public boolean accelerated() {
        return true;
    }

    @Override
    public ComputeBackend create() {
        CudaRuntime runtime = Objects.requireNonNull(
                runtimeFactory.open(), "CUDA runtime factory result");
        try {
            return new CudaBackend(runtime);
        } catch (RuntimeException | Error failure) {
            runtime.close();
            throw failure;
        }
    }

    @FunctionalInterface
    interface RuntimeFactory {
        CudaRuntime open();
    }
}
