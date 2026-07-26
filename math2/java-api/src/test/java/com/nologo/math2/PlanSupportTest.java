package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlanSupportTest {
    @Test
    void representsSupportedAndUnsupportedDecisions() {
        PlanSupport supported = PlanSupport.supported();
        PlanSupport unsupported = PlanSupport.unsupported("precision");

        assertTrue(supported.isSupported());
        assertTrue(supported.reason().isEmpty());
        assertFalse(unsupported.isSupported());
        assertEquals("precision", unsupported.reason().orElseThrow());
        assertEquals(PlanSupport.unsupported("precision"), unsupported);
    }

    @Test
    void rejectsMissingDiagnosticReason() {
        assertThrows(IllegalArgumentException.class, () -> PlanSupport.unsupported(""));
        assertThrows(IllegalArgumentException.class, () -> PlanSupport.unsupported(null));
    }
}
