package com.bcbs239.regtech.ingestion.application.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;

/**
 * Application-level configuration for the Ingestion module.
 * This configuration scans only application packages and intentionally
 * avoids scanning infrastructure configuration to prevent duplicate
 * bean definitions when infra configuration is present and registered separately.
 */
@Configuration
@ComponentScan(basePackages = {
        "com.bcbs239.regtech.ingestion.application",
        "com.bcbs239.regtech.ingestion.domain"
})
public class IngestionApplicationConfiguration {
    // Application-level beans for ingestion go here (command handlers, services, etc.)

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
