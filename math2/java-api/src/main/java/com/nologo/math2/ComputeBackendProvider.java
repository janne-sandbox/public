package com.nologo.math2;

/** Lazy Java service-provider boundary for optional execution backends. */
public interface ComputeBackendProvider {
    /** Stable provider ID. Access must not initialize native code or probe hardware. */
    String id();

    /** Whether instances represent accelerated rather than reference CPU execution. */
    boolean accelerated();

    /** Creates and initializes one backend after the configured mode permits discovery. */
    ComputeBackend create();
}
