package com.nologo2.math2.rest.web;

import java.util.List;

public record BatchCalculationResponse(List<String> results) {
    public BatchCalculationResponse {
        results = List.copyOf(results);
    }
}
