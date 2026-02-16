package com.bcbs239.regtech.ingestion.application.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Lightweight application-level module configuration for the Ingestion module.
 *
 * This class exists so that higher-level layers (presentation) can import
 * a configuration class from the application module without depending on
 * infrastructure directly.
 */
@Configuration("ingestionApplicationConfiguration")
@ComponentScan(basePackages = {
    "com.bcbs239.regtech.ingestion.application",
    "com.bcbs239.regtech.ingestion.domain"
})
public class IngestionModuleConfiguration {
    // Intentionally minimal; infra-specific bean registration remains in infra module.
    // Note: bean name changed and infra scanning removed to avoid conflicts with infra configuration.
}
