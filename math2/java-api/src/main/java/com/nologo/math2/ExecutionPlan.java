package com.nologo.math2;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Immutable, backend-independent plan envelope for an ordered calculation batch. */
public final class ExecutionPlan {
    public static final int CURRENT_FORMAT_VERSION = 1;
    public static final int CURRENT_ENGINE_VERSION = 3;

    private final int formatVersion;
    private final int engineVersion;
    private final String planId;
    private final List<String> canonicalExpressions;
    private final int precision;
    private final GpuMode gpuMode;
    private final int roundingModeCode;
    private final long maxOperations;
    private final long maxMemoryBytes;
    private final long timeoutMillis;

    private ExecutionPlan(List<String> canonicalExpressions, CalculationContext context) {
        this.formatVersion = CURRENT_FORMAT_VERSION;
        this.engineVersion = CURRENT_ENGINE_VERSION;
        this.canonicalExpressions = validateExpressions(canonicalExpressions);
        this.precision = context.precision();
        this.gpuMode = context.gpuMode();
        this.roundingModeCode = context.roundingModeCode();
        this.maxOperations = context.maxOperations();
        this.maxMemoryBytes = context.maxMemoryBytes();
        this.timeoutMillis = context.timeout().toMillis();
        this.planId = digest(serialize());
    }

    public static ExecutionPlan create(
            List<String> canonicalExpressions, CalculationContext context) {
        return new ExecutionPlan(
                Objects.requireNonNull(canonicalExpressions, "canonicalExpressions"),
                Objects.requireNonNull(context, "context"));
    }

    public static ExecutionPlan create(String canonicalExpression, CalculationContext context) {
        return create(List.of(Objects.requireNonNull(canonicalExpression, "canonicalExpression")),
                context);
    }

    public int formatVersion() {
        return formatVersion;
    }

    public int engineVersion() {
        return engineVersion;
    }

    public String planId() {
        return planId;
    }

    public List<String> canonicalExpressions() {
        return canonicalExpressions;
    }

    public int precision() {
        return precision;
    }

    public int roundingModeCode() {
        return roundingModeCode;
    }

    public GpuMode gpuMode() {
        return gpuMode;
    }

    public long maxOperations() {
        return maxOperations;
    }

    public long maxMemoryBytes() {
        return maxMemoryBytes;
    }

    public long timeoutMillis() {
        return timeoutMillis;
    }

    private static List<String> validateExpressions(List<String> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Execution plan requires at least one expression");
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "Canonical expressions must not contain null or blank values");
            }
        }
        return List.copyOf(values);
    }

    private byte[] serialize() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(formatVersion);
                output.writeInt(engineVersion);
                output.writeInt(precision);
                output.writeInt(gpuMode.ordinal());
                output.writeInt(roundingModeCode);
                output.writeLong(maxOperations);
                output.writeLong(maxMemoryBytes);
                output.writeLong(timeoutMillis);
                output.writeInt(canonicalExpressions.size());
                for (String expression : canonicalExpressions) {
                    byte[] encoded = expression.getBytes(StandardCharsets.UTF_8);
                    output.writeInt(encoded.length);
                    output.write(encoded);
                }
            }
            return bytes.toByteArray();
        } catch (IOException impossibleForByteArray) {
            throw new IllegalStateException(
                    "Unable to serialize execution plan", impossibleForByteArray);
        }
    }

    private static String digest(byte[] serialized) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(serialized));
        } catch (NoSuchAlgorithmException impossibleOnJava21) {
            throw new IllegalStateException("SHA-256 is unavailable", impossibleOnJava21);
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof ExecutionPlan that
                && formatVersion == that.formatVersion
                && engineVersion == that.engineVersion
                && precision == that.precision
                && gpuMode == that.gpuMode
                && roundingModeCode == that.roundingModeCode
                && maxOperations == that.maxOperations
                && maxMemoryBytes == that.maxMemoryBytes
                && timeoutMillis == that.timeoutMillis
                && planId.equals(that.planId)
                && canonicalExpressions.equals(that.canonicalExpressions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formatVersion, engineVersion, planId, canonicalExpressions,
                precision, gpuMode, roundingModeCode, maxOperations, maxMemoryBytes,
                timeoutMillis);
    }
}
