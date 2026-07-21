package com.nologo2.math2.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nologo2.math2.rest.cache.CalculationCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class CalculationApiIntegrationTest {
    private MockMvc mockMvc;
    @Autowired
    private CalculationCacheRepository repository;
    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void clearCache() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        repository.deleteAll();
    }

    @Test
    void calculatesSingleEquationAndReusesCache() throws Exception {
        mockMvc.perform(get("/api/v1/calculate").param("equation", "2 + 3 * 4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("14"));
        mockMvc.perform(get("/api/v1/calculate").param("equation", "2+3*4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("14"));

        org.junit.jupiter.api.Assertions.assertEquals(1, repository.count());
        org.junit.jupiter.api.Assertions.assertTrue(
                repository.findAll().getFirst().getCalculationDurationMs() >= 0);
    }

    @Test
    void calculatesBatchInOrderIncludingFunctionCommas() throws Exception {
        mockMvc.perform(get("/api/v1/calculate/batch")
                        .param("equations", "1+1,max(2,3),sqrt(16)")
                        .param("precision", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(3)))
                .andExpect(jsonPath("$.results[0]").value("2"))
                .andExpect(jsonPath("$.results[1]").value("3"))
                .andExpect(jsonPath("$.results[2]").value("4"));
    }

    @Test
    void calculatesThousandDigitOperandsWithoutJsonPrecisionLoss() throws Exception {
        String thousandNines = "9".repeat(1000);

        mockMvc.perform(get("/api/v1/calculate")
                        .param("equation", thousandNines + "+1")
                        .param("precision", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("1" + "0".repeat(1000)));
    }

    @Test
    void calculatesPiAtOneThousandDigitPrecision() throws Exception {
        mockMvc.perform(get("/api/v1/calculate")
                        .param("equation", "pi")
                        .param("precision", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(startsWith("3.14159265358979323846")))
                .andExpect(jsonPath("$.result").value(hasLength(1001)));

        mockMvc.perform(get("/api/v1/calculate")
                        .param("equation", "pi")
                        .param("precision", "1200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(hasLength(1201)));
    }

    @Test
    void returnsStructuredValidationErrors() throws Exception {
        mockMvc.perform(get("/api/v1/calculate").param("equation", "(1+2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EXPRESSION"))
                .andExpect(jsonPath("$.position").isNumber());

        mockMvc.perform(get("/api/v1/calculate").param("equation", "1/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DIVISION_BY_ZERO"));

        mockMvc.perform(get("/api/v1/calculate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void enforcesBatchAndPrecisionLimits() throws Exception {
        mockMvc.perform(get("/api/v1/calculate/batch")
                        .param("equations", "1,2,3,4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/v1/calculate")
                        .param("equation", "1+1")
                        .param("precision", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PRECISION_EXCEEDED"));
    }

    @Test
    void evictsLeastRecentlyUsedEntries() throws Exception {
        calculate("1+1");
        calculate("2+2");
        calculate("1+1");
        calculate("3+3");

        org.junit.jupiter.api.Assertions.assertEquals(2, repository.count());
        org.junit.jupiter.api.Assertions.assertTrue(repository.findAll().stream()
                .anyMatch(entry -> entry.getResult().equals("2")));
        org.junit.jupiter.api.Assertions.assertTrue(repository.findAll().stream()
                .anyMatch(entry -> entry.getResult().equals("6")));
    }

    @Test
    void exposesOpenApiAndHealthWithoutH2Console() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().isNotFound());
    }

    private void calculate(String equation) throws Exception {
        mockMvc.perform(get("/api/v1/calculate").param("equation", equation))
                .andExpect(status().isOk());
    }
}
