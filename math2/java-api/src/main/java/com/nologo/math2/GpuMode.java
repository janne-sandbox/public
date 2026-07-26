package com.nologo.math2;

/** Runtime policy for optional GPU discovery and execution. */
public enum GpuMode {
    /** Never discover or use an accelerated backend. */
    DISABLED,
    /** Use an accelerated backend only when it is available, compatible, and suitable. */
    DETECT,
    /** Require an accelerated backend; later planner phases fail instead of falling back. */
    ENABLED
}
