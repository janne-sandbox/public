package com.nologo.math2.cuda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nologo.math2.CalculationContext;
import com.nologo.math2.CalculationError;
import com.nologo.math2.Equation;
import com.nologo.math2.ExecutionPlan;
import com.nologo.math2.ExecutionPlanner;
import com.nologo.math2.GpuMode;
import com.nologo.math2.ReferenceCpuBackend;
import com.nologo.math2.UncalculableException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CudaDisabledIntegrationTest {
    @Test
    void defaultCalculationUsesCpuWithoutLoadingCudaBridge() {
        assertEquals("42", Equation.parse("6*7").calculate().toString());
    }

    @Test
    void detectFallsBackToCpuWhenNativeBridgeIsUnavailableOrHasNoKernels() {
        ExecutionPlan plan = ExecutionPlan.create(
                List.of("1+1", "2+2"),
                CalculationContext.builder()
                        .precision(200)
                        .gpuMode(GpuMode.DETECT)
                        .build());

        assertEquals(
                ReferenceCpuBackend.BACKEND_ID,
                ExecutionPlanner.system().execute(plan).backendId());
    }

    @Test
    void enabledFailsClosedWhenNativeBridgeIsUnavailableOrHasNoKernels() {
        ExecutionPlan plan = ExecutionPlan.create(
                "1+1",
                CalculationContext.builder()
                        .gpuMode(GpuMode.ENABLED)
                        .build());

        UncalculableException failure = assertThrows(
                UncalculableException.class,
                () -> ExecutionPlanner.system().execute(plan));

        assertEquals(CalculationError.BACKEND_UNAVAILABLE, failure.getReason());
    }
}
