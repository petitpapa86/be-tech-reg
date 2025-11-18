package com.bcbs239.regtech.ingestion.infrastructure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main configuration class for the Ingestion module.
 * <p>
 * This configuration class sets up the modular structure and enables
 * the necessary Spring features for the ingestion module.
 */
@Configuration("ingestionInfrastructureConfiguration")
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({IngestionProperties.class, S3Properties.class})
@ComponentScan(basePackages = {
        "com.bcbs239.regtech.ingestion.domain",
        "com.bcbs239.regtech.ingestion.application",
        "com.bcbs239.regtech.ingestion.infrastructure",
        "com.bcbs239.regtech.ingestion.presentation"
})
@EntityScan(basePackages = "com.bcbs239.regtech.ingestion.infrastructure")
@EnableJpaRepositories(basePackages = "com.bcbs239.regtech.ingestion.infrastructure")
@ConditionalOnProperty(name = "ingestion.enabled", havingValue = "true", matchIfMissing = true)
public class IngestionModuleConfiguration {

    // Configuration beans will be added here as we migrate components
}



