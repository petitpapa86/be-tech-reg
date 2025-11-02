package com.bcbs239.regtech.modules.ingestion.infrastructure.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main configuration class for the Ingestion module.
 * 
 * This configuration class sets up the modular structure and enables
 * the necessary Spring features for the ingestion module.
 */
@Configuration
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {
    "com.bcbs239.regtech.modules.ingestion.application",
    "com.bcbs239.regtech.modules.ingestion.infrastructure",
    "com.bcbs239.regtech.modules.ingestion.presentation"
})
@EntityScan(basePackages = "com.bcbs239.regtech.modules.ingestion.infrastructure.*.persistence")
@EnableJpaRepositories(basePackages = "com.bcbs239.regtech.modules.ingestion.infrastructure.*.persistence")
public class IngestionModuleConfiguration {
    
    // Configuration beans will be added here as we migrate components
}