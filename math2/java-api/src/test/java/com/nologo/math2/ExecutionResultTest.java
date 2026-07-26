package com.nologo.math2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutionResultTest {
    @Test
    void preservesOrderedResultsAndDiagnosticsImmutably() {
        List<Number> mutable = new ArrayList<>(List.of(
                Number.fromInteger(2), Number.fromInteger(3)));
        ExecutionResult result = new ExecutionResult(
                "plan", 3, "cpu", mutable, 10, 0);
        mutable.set(0, Number.fromInteger(9));

        assertEquals(List.of(Number.fromInteger(2), Number.fromInteger(3)),
                result.results());
        assertEquals(10, result.executionDurationNanos());
        assertThrows(UnsupportedOperationException.class,
                () -> result.results().add(Number.fromInteger(4)));
    }

    @Test
    void rejectsInvalidEnvelopeFields() {
        List<Number> one = List.of(Number.fromInteger(1));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionResult("", 3, "cpu", one, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionResult("plan", 0, "cpu", one, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionResult("plan", 3, "", one, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionResult("plan", 3, "cpu", List.of(), 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionResult("plan", 3, "cpu", one, -1, 0));
    }
}
