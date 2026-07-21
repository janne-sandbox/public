package com.nologo2.math2.rest.service;

import com.nologo2.math2.Equation;
import com.nologo2.math2.Number;
import com.nologo2.math2.rest.cache.CalculationCacheEntry;
import com.nologo2.math2.rest.cache.CalculationCacheRepository;
import com.nologo2.math2.rest.config.Math2Properties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
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
        validateLength(expression);
        Equation equation = Equation.parse(expression);
        String canonical = equation.canonicalExpression();
        String key = key(canonical, precision);
        int maximum = properties.getCache().getMaxEntries();
        boolean cacheEnabled = properties.getCache().isEnabled() && maximum > 0;

        if (cacheEnabled) {
            requireRepository();
            CalculationCacheEntry cached = repository.findById(key).orElse(null);
            if (cached != null) {
                cached.touch(clock.instant());
                return Number.fromString(cached.getResult());
            }
        }

        long startedAt = nanoTime.getAsLong();
        Number result = equation.calculate(precision);
        long calculationDurationMs = Math.max(0, (nanoTime.getAsLong() - startedAt) / 1_000_000);
        if (cacheEnabled) {
            Instant now = clock.instant();
            repository.save(new CalculationCacheEntry(
                    key, canonical, precision, ENGINE_VERSION, result.toString(),
                    calculationDurationMs, now));
            evictTo(maximum);
        }
        return result;
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
