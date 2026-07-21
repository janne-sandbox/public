package com.nologo2.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UncalculableExceptionTest {
    @Test
    void positionIsOptional() {
        UncalculableException withoutPosition = new UncalculableException(
                CalculationError.DOMAIN_ERROR, "undefined");
        UncalculableException withPosition = new UncalculableException(
                CalculationError.INVALID_EXPRESSION, "bad token", 3);

        assertFalse(withoutPosition.getPosition().isPresent());
        assertTrue(withPosition.getPosition().isPresent());
        assertEquals(3, withPosition.getPosition().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> new UncalculableException(
                CalculationError.INVALID_EXPRESSION, "bad token", -1));
    }
}
