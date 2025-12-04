package com.bcbs239.regtech.ingestion.infrastructure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
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
 * 
 * <p>JPA 3.2 and Hibernate 7.x compatible configuration:
 * <ul>
 *   <li>Supports EntityManager injection with @PersistenceContext and @Inject</li>
 *   <li>Compatible with Hibernate ORM 7.1/7.2</li>
 *   <li>Uses Jakarta Persistence API (jakarta.persistence.*)</li>
 * </ul>
 */
@Configuration("ingestionInfrastructureConfiguration")
//@EnableAsync
//@EnableScheduling
@EnableConfigurationProperties({IngestionProperties.class})
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
