package com.nologo.math2.rest.service;

import com.nologo.math2.CalculationContext;
import com.nologo.math2.CalculationError;
import com.nologo.math2.Equation;
import com.nologo.math2.ExecutionPlan;
import com.nologo.math2.ExecutionPlanner;
import com.nologo.math2.ExecutionResult;
import com.nologo.math2.Number;
import com.nologo.math2.UncalculableException;
import com.nologo.math2.rest.cache.CalculationCacheEntry;
import com.nologo.math2.rest.cache.CalculationCacheRepository;
import com.nologo.math2.rest.config.Math2Properties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalculationService {
    static final String ENGINE_VERSION = "3";

    private final CalculationCacheRepository repository;
    private final Math2Properties properties;
    private final Clock clock;
    private final LongSupplier nanoTime;

    @Autowired
    public CalculationService(
            ObjectProvider<CalculationCacheRepository> repositoryProvider,
            Math2Properties properties) {
        this(repositoryProvider.getIfAvailable(), properties, Clock.systemUTC(), System::nanoTime);
    }

    CalculationService(
            CalculationCacheRepository repository,
            Math2Properties properties,
            Clock clock,
            LongSupplier nanoTime) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
        this.nanoTime = nanoTime;
    }

    @Transactional
    public synchronized Number calculate(String expression, int precision) {
        return calculateBatch(List.of(expression), precision).getFirst();
    }

    @Transactional
    public synchronized List<Number> calculateBatch(List<String> expressions, int precision) {
        Objects.requireNonNull(expressions, "expressions");
        if (expressions.isEmpty()
                || expressions.size() > properties.getRequest().getMaxBatchSize()) {
            throw new IllegalArgumentException("Batch size is outside configured limits");
        }
        validatePrecision(precision);

        List<String> canonicalExpressions = new ArrayList<>(expressions.size());
        for (String expression : expressions) {
            validateLength(expression);
            canonicalExpressions.add(Equation.parse(expression).canonicalExpression());
        }

        int maximum = properties.getCache().getMaxEntries();
        boolean cacheEnabled = properties.getCache().isEnabled() && maximum > 0;
        if (cacheEnabled) {
            requireRepository();
        }

        List<Number> orderedResults = new ArrayList<>(
                Collections.nCopies(expressions.size(), null));
        List<Integer> missIndexes = new ArrayList<>();
        List<String> missExpressions = new ArrayList<>();
        List<String> missKeys = new ArrayList<>();

        for (int index = 0; index < canonicalExpressions.size(); index++) {
            String canonical = canonicalExpressions.get(index);
            String cacheKey = key(canonical, precision);
            CalculationCacheEntry cached = cacheEnabled
                    ? repository.findById(cacheKey).orElse(null)
                    : null;
            if (cached != null) {
                cached.touch(clock.instant());
                orderedResults.set(index, Number.fromString(cached.getResult()));
            } else {
                missIndexes.add(index);
                missExpressions.add(canonical);
                missKeys.add(cacheKey);
            }
        }

        if (!missExpressions.isEmpty()) {
            CalculationContext context = CalculationContext.builder()
                    .precision(precision)
                    .gpuMode(properties.getGpu().getMode())
                    .build();
            ExecutionPlan plan = ExecutionPlan.create(missExpressions, context);
            long startedAt = nanoTime.getAsLong();
            ExecutionResult execution = ExecutionPlanner.system().execute(plan);
            long calculationDurationMs =
                    Math.max(0, (nanoTime.getAsLong() - startedAt) / 1_000_000);

            for (int miss = 0; miss < missIndexes.size(); miss++) {
                int resultIndex = missIndexes.get(miss);
                Number result = execution.results().get(miss);
                orderedResults.set(resultIndex, result);
                if (cacheEnabled) {
                    Instant now = clock.instant();
                    repository.save(new CalculationCacheEntry(
                            missKeys.get(miss),
                            missExpressions.get(miss),
                            precision,
                            ENGINE_VERSION,
                            result.toString(),
                            calculationDurationMs,
                            now));
                }
            }
        }

        if (cacheEnabled) {
            evictTo(maximum);
        }
        return List.copyOf(orderedResults);
    }

    private void requireRepository() {
        if (repository == null) {
            throw new IllegalStateException(
                    "Database cache is enabled but persistence is not configured");
        }
    }

    private void validateLength(String expression) {
        if (expression != null
                && expression.length() > properties.getRequest().getMaxExpressionLength()) {
            throw new IllegalArgumentException("Expression exceeds configured maximum length");
        }
    }

    private static void validatePrecision(int precision) {
        if (precision < 1 || precision > Number.getMaxLength()) {
            throw new UncalculableException(
                    CalculationError.PRECISION_EXCEEDED,
                    "Precision must be between 1 and " + Number.getMaxLength());
        }
    }

    private void evictTo(int maximum) {
        long excess = repository.count() - maximum;
        if (excess <= 0) {
            return;
        }
        repository.deleteAllInBatch(repository.findAllByOrderByLastAccessedAtAscCreatedAtAsc(
                PageRequest.of(0, Math.toIntExact(excess))));
    }

    private static String key(String canonical, int precision) {
        String material = ENGINE_VERSION + '\n' + precision + '\n' + canonical;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
