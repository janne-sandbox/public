package com.nologo2.math2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class EngineArchitectureTest {
    @Test
    void calculationClassesDoNotReferenceExternalDecimalEngines() throws IOException {
        assertNoLegacyDecimalDependency(BuiltInOperator.class);
        assertNoLegacyDecimalDependency(DecimalConstants.class);
        assertNoLegacyDecimalDependency(DecimalArithmetic.class);
    }

    private static void assertNoLegacyDecimalDependency(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resource)) {
            String bytecode = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertFalse(bytecode.contains("java/math/BigDecimal"));
            assertFalse(bytecode.contains("ch/obermuhlner/math/big/BigDecimalMath"));
            assertFalse(bytecode.contains("org/apfloat"));
            assertFalse(bytecode.contains("java/math/BigInteger"));
        }
        for (Class<?> nested : type.getDeclaredClasses()) {
            assertNoLegacyDecimalDependency(nested);
        }
    }
}
