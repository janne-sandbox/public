package com.nologo2.math2.rest.cache;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "calculation_cache")
public class CalculationCacheEntry {
    @Id
    @Column(name = "cache_key", length = 64, nullable = false, updatable = false)
    private String cacheKey;

    @Column(name = "canonical_expression", nullable = false, length = 10_000)
    private String canonicalExpression;

    @Column(name = "calculation_precision", nullable = false)
    private int precision;

    @Column(name = "engine_version", nullable = false, length = 32)
    private String engineVersion;

    @Column(name = "result_value", nullable = false, columnDefinition = "CLOB")
    private String result;

    @Column(name = "calculation_duration_ms", nullable = false)
    private long calculationDurationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_accessed_at", nullable = false)
    private Instant lastAccessedAt;

    protected CalculationCacheEntry() {
    }

    public CalculationCacheEntry(
            String cacheKey,
            String canonicalExpression,
            int precision,
            String engineVersion,
            String result,
            long calculationDurationMs,
            Instant now) {
        this.cacheKey = cacheKey;
        this.canonicalExpression = canonicalExpression;
        this.precision = precision;
        this.engineVersion = engineVersion;
        this.result = result;
        this.calculationDurationMs = calculationDurationMs;
        this.createdAt = now;
        this.lastAccessedAt = now;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public String getCanonicalExpression() {
        return canonicalExpression;
    }

    public int getPrecision() {
        return precision;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public String getResult() {
        return result;
    }

    public long getCalculationDurationMs() {
        return calculationDurationMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void touch(Instant now) {
        lastAccessedAt = now;
    }
}
