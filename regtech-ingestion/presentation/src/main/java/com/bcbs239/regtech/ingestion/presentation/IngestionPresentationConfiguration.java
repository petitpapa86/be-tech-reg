package com.bcbs239.regtech.ingestion.presentation;

import com.bcbs239.regtech.ingestion.infrastructure.configuration.IngestionModuleConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Presentation layer configuration for the Ingestion module.
 * 
 * This configuration class imports the infrastructure configuration
 * and sets up component scanning for the presentation layer.
 */
@Configuration
@Import(IngestionModuleConfiguration.class)
@ComponentScan(basePackages = "com.bcbs239.regtech.modules.ingestion.presentation")
public class IngestionPresentationConfiguration {
    
    // Presentation-specific configuration beans will be added here
}