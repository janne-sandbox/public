package com.nologo2.math2.rest.config;

import com.nologo2.math2.Number;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "math2")
public class Math2Properties {
    @Valid
    private final Cache cache = new Cache();
    @Valid
    private final Request request = new Request();
    @Valid
    private final Precision precision = new Precision();

    public Cache getCache() {
        return cache;
    }

    public Request getRequest() {
        return request;
    }

    public Precision getPrecision() {
        return precision;
    }

    @PostConstruct
    void configureNumberPrecision() {
        Number.setMaxLength(precision.getMaxLength());
    }

    public static class Cache {
        private boolean enabled = true;
        @Min(0)
        private int maxEntries = 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxEntries() {
            return maxEntries;
        }

        public void setMaxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
        }
    }

    public static class Request {
        @Min(1)
        @Max(10_000)
        private int maxBatchSize = 100;
        @Min(1)
        @Max(1_000_000)
        private int maxExpressionLength = 10_000;

        public int getMaxBatchSize() {
            return maxBatchSize;
        }

        public void setMaxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
        }

        public int getMaxExpressionLength() {
            return maxExpressionLength;
        }

        public void setMaxExpressionLength(int maxExpressionLength) {
            this.maxExpressionLength = maxExpressionLength;
        }
    }

    public static class Precision {
        @Min(1)
        @Max(1_000_000)
        private int maxLength = Number.DEFAULT_MAX_LENGTH;

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
    }
}
