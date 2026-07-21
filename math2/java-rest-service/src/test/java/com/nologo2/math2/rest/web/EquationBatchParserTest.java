package com.nologo2.math2.rest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class EquationBatchParserTest {
    @Test
    void splitsOnlyTopLevelCommas() {
        assertEquals(List.of("1+1", "max(2,3)", "sqrt(4)"),
                EquationBatchParser.split("1+1, max(2,3),sqrt(4)"));
    }

    @Test
    void rejectsBlankAndEmptyEntries() {
        assertThrows(IllegalArgumentException.class, () -> EquationBatchParser.split(" "));
        assertThrows(IllegalArgumentException.class, () -> EquationBatchParser.split("1,,2"));
    }
}
