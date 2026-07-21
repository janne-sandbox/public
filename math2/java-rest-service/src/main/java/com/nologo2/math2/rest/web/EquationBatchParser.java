package com.nologo2.math2.rest.web;

import java.util.ArrayList;
import java.util.List;

final class EquationBatchParser {
    private EquationBatchParser() {
    }

    static List<String> split(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Equations must not be blank");
        }
        List<String> values = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;
            } else if (character == ',' && depth == 0) {
                add(values, input.substring(start, index));
                start = index + 1;
            }
        }
        add(values, input.substring(start));
        return List.copyOf(values);
    }

    private static void add(List<String> values, String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Batch contains an empty equation");
        }
        values.add(trimmed);
    }
}
