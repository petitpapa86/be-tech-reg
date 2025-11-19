package com.bcbs239.regtech.core.application.eventprocessing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for core application services.
 */
@Configuration
public class CoreApplicationConfiguration {

    @Bean
    public CoreIntegrationEventDeserializer coreIntegrationEventDeserializer(ObjectMapper objectMapper) {
        return new CoreIntegrationEventDeserializer(objectMapper);
    }
}