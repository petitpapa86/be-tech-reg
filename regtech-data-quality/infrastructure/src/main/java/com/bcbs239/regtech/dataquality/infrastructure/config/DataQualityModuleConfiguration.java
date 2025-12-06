package com.bcbs239.regtech.dataquality.infrastructure.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Data Quality Module Configuration.
 * 
 * <p>JPA 3.2 and Hibernate 7.x compatible configuration:
 * <ul>
 *   <li>Supports EntityManager injection with @PersistenceContext and @Inject</li>
 *   <li>Compatible with Hibernate ORM 7.1/7.2</li>
 *   <li>Uses Jakarta Persistence API (jakarta.persistence.*)</li>
 * </ul>
 */
@Configuration("dataQualityModuleConfiguration")
@ComponentScan(basePackages = {
        "com.bcbs239.regtech.dataquality.domain",
        "com.bcbs239.regtech.dataquality.application",
        "com.bcbs239.regtech.dataquality.infrastructure",
        "com.bcbs239.regtech.dataquality.presentation",
        "com.bcbs239.regtech.dataquality.rulesengine"
})
// Scan domain and infrastructure reporting entities so JPA sees QualityReportEntity and domain entities
@EntityScan(basePackages = {
        "com.bcbs239.regtech.dataquality.rulesengine.domain",
        "com.bcbs239.regtech.dataquality.domain",
        "com.bcbs239.regtech.dataquality.infrastructure.reporting",
        "com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities"
})
@EnableJpaRepositories(basePackages = "com.bcbs239.regtech.dataquality.infrastructure")
public class DataQualityModuleConfiguration {
}
