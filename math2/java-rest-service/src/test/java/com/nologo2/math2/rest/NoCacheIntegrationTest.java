package com.nologo2.math2.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
        "math2.cache.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration"
})
class NoCacheIntegrationTest {
    @Autowired
    private WebApplicationContext webContext;
    @Autowired
    private ApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();
    }

    @Test
    void calculatesWithoutDatabaseInfrastructure() throws Exception {
        org.junit.jupiter.api.Assertions.assertTrue(
                context.getBeansOfType(DataSource.class).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(
                context.getBeansOfType(com.nologo2.math2.rest.cache.CalculationCacheRepository.class)
                        .isEmpty());

        mockMvc.perform(get("/api/v1/calculate").param("equation", "6*7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("42"));
    }
}
