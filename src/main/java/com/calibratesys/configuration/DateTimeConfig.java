package com.calibratesys.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuration providing a central Spring-managed Clock bean.
 * Allows deterministic date and time evaluation across business services and tests.
 */
@Configuration
public class DateTimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
