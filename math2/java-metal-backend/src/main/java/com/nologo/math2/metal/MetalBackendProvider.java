package com.nologo.math2.metal;

import com.nologo.math2.ComputeBackend;
import com.nologo.math2.ComputeBackendProvider;
import java.util.Objects;

/** Lazy service provider for the optional Apple Metal backend artifact. */
public final class MetalBackendProvider implements ComputeBackendProvider {
    public static final String PROVIDER_ID = "math2-metal";

    private final RuntimeFactory runtimeFactory;

    public MetalBackendProvider() {
        this(NativeMetalRuntime::open);
    }

    MetalBackendProvider(RuntimeFactory runtimeFactory) {
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
        MetalRuntime runtime = Objects.requireNonNull(
                runtimeFactory.open(), "Metal runtime factory result");
        try {
            return new MetalBackend(runtime);
        } catch (RuntimeException | Error failure) {
            runtime.close();
            throw failure;
        }
    }

    @FunctionalInterface
    interface RuntimeFactory {
        MetalRuntime open();
    }
}
