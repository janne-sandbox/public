package com.nologo2.math2.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nologo2.math2.rest.cache.CalculationCacheEntry;
import com.nologo2.math2.rest.cache.CalculationCacheRepository;
import com.nologo2.math2.rest.service.CalculationService;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Produces the H2-backed sample measurements published by the documentation site. */
@SpringBootTest(properties = "math2.cache.max-entries=10")
class DocumentationPerformanceTest {
    @Autowired
    private CalculationService service;

    @Autowired
    private CalculationCacheRepository repository;

    @Test
    void recordsRepresentativeCalculationDurationsInH2() {
        service.calculate("1+1", 100); // Warm class loading before collecting the sample.
        repository.deleteAll();

        service.calculate("2+3*4", 1000);
        service.calculate("1/7", 1000);
        service.calculate("sqrt(2)", 1000);
        service.calculate("pi", 1000);

        List<CalculationCacheEntry> samples = repository.findAll().stream()
                .sorted(Comparator.comparing(CalculationCacheEntry::getCanonicalExpression))
                .toList();
        assertEquals(4, samples.size());
        samples.forEach(entry -> System.out.printf(
                "MATH2_PERFORMANCE|%s|%d|%d|%d|%s%n",
                entry.getCanonicalExpression(),
                entry.getPrecision(),
                entry.getCalculationDurationMs(),
                entry.getResult().length(),
                entry.getEngineVersion()));
    }
}
