package com.nologo2.math2.rest;

import com.nologo2.math2.rest.config.Math2Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(Math2Properties.class)
public class Math2Application {
    public static void main(String[] args) {
        SpringApplication.run(Math2Application.class, args);
    }
}
