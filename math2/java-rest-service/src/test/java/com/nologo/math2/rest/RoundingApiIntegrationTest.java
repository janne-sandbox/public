package com.nologo.math2.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nologo.math2.Number;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for {@code GET /api/v1/rounding} and
 * {@code PUT /api/v1/rounding/{code}}.
 */
@SpringBootTest
class RoundingApiIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        Number.setDefaultRoundingModeCode(0); // reset to HALF_EVEN before each test
    }

    @AfterEach
    void tearDown() {
        Number.setDefaultRoundingModeCode(0); // restore after each test
    }

    @Test
    void getDefaultRoundingModeReturnsHalfEvenByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/rounding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.name").value("HALF_EVEN"));
    }

    @Test
    void setDefaultRoundingModeReturnsNewMode() throws Exception {
        mockMvc.perform(post("/api/v1/rounding/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.name").value("HALF_UP"));
    }

    @Test
    void setDefaultRoundingModeAffectsSubsequentRoundCalculation() throws Exception {
        // Default HALF_EVEN: round(2.5) → 2 (2 is even)
        mockMvc.perform(get("/api/v1/calculate").param("equation", "round(2.5)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("2"));

        // Switch to HALF_UP: round(2.5) → 3
        mockMvc.perform(post("/api/v1/rounding/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/calculate").param("equation", "round(2.5)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("3"));
    }

    @Test
    void getReflectsSetValue() throws Exception {
        mockMvc.perform(post("/api/v1/rounding/3").contentType(MediaType.APPLICATION_JSON)) // CEILING
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/rounding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3))
                .andExpect(jsonPath("$.name").value("CEILING"));
    }

    @Test
    void allSevenModesAreAccepted() throws Exception {
        String[] names = {"HALF_EVEN", "HALF_UP", "HALF_DOWN", "CEILING", "FLOOR", "UP", "DOWN"};
        for (int code = 0; code < names.length; code++) {
            mockMvc.perform(post("/api/v1/rounding/" + code).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(code))
                    .andExpect(jsonPath("$.name").value(names[code]));
        }
    }

    @Test
    void invalidModeCodeReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/rounding/7").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/rounding/-1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
