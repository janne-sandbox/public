package com.nologo2.math2.rest.config;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Removes database infrastructure when the calculation cache is disabled. */
public class OptionalCacheEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {
    private static final String EXCLUSIONS = String.join(",",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
            "org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration");

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("math2.cache.enabled", Boolean.class, true)) {
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "math2OptionalCacheExclusions",
                    Map.of("spring.autoconfigure.exclude", EXCLUSIONS)));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
