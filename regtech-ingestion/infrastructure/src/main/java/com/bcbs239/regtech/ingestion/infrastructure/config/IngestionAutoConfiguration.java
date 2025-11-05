package com.bcbs239.regtech.ingestion.infrastructure.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for the Ingestion Module.
 * This configuration is automatically loaded when the ingestion module is on the classpath.
 */
@AutoConfiguration
@Configuration
@EnableConfigurationProperties({
    IngestionProperties.class,
    S3Properties.class,
    BankRegistryProperties.class
})
@ComponentScan(basePackages = {
    "com.bcbs239.regtech.modules.ingestion.domain",
    "com.bcbs239.regtech.modules.ingestion.application",
    "com.bcbs239.regtech.modules.ingestion.infrastructure",
    "com.bcbs239.regtech.modules.ingestion.presentation"
})
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(name = "ingestion.enabled", havingValue = "true", matchIfMissing = true)
public class IngestionAutoConfiguration {
    
    // Configuration beans will be defined by individual configuration classes
    // This class serves as the main entry point for auto-configuration
}

