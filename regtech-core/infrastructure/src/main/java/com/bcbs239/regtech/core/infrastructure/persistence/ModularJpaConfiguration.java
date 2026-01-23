package com.bcbs239.regtech.core.infrastructure.persistence;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Configuration for RegTech Platform.
 * 
 * <p>This configuration is compatible with:
 * <ul>
 *   <li>JPA 3.2 (Jakarta Persistence API)</li>
 *   <li>Hibernate ORM 7.1/7.2</li>
 *   <li>Spring Boot 4.x</li>
 *   <li>Spring Framework 7.x</li>
 * </ul>
 * 
 * <h2>JPA 3.2 Features Supported:</h2>
 * <ul>
 *   <li>PersistenceConfiguration API for programmatic configuration</li>
 *   <li>EntityManager injection with @Inject and qualifiers</li>
 *   <li>Enhanced @PersistenceContext support</li>
 *   <li>StatelessSession support for transactional operations</li>
 * </ul>
 * 
 * <h2>Hibernate 7.x Features:</h2>
 * <ul>
 *   <li>Uses org.springframework.orm.jpa.hibernate package for Hibernate-specific features</li>
 *   <li>SpringPersistenceUnitInfo with asStandardPersistenceUnitInfo() adapter</li>
 *   <li>Improved performance and memory efficiency</li>
 *   <li>Enhanced query optimization</li>
 * </ul>
 * 
 * <h2>EntityManager Injection:</h2>
 * <p>EntityManager can be injected using either @PersistenceContext or @Inject:
 * <pre>
 * // Standard JPA injection
 * {@literal @}PersistenceContext
 * private EntityManager entityManager;
 * 
 * // CDI-style injection (JPA 3.2+)
 * {@literal @}Inject
 * private EntityManager entityManager;
 * 
 * // With qualifier (JPA 3.2+)
 * {@literal @}Inject
 * {@literal @}Named("myPersistenceUnit")
 * private EntityManager entityManager;
 * </pre>
 * 
 * @see jakarta.persistence.EntityManager
 * @see jakarta.persistence.PersistenceContext
 * @see jakarta.inject.Inject
 */
@Configuration
@EntityScan(basePackages = {
    // Domain packages (value objects, enums) and persistence entity packages
    "com.bcbs239.regtech.iam.domain",
    "com.bcbs239.regtech.billing.domain",
    "com.bcbs239.regtech.iam.infrastructure.database.entities",
    "com.bcbs239.regtech.iam.infrastructure.persistence.bankprofile",
    "com.bcbs239.regtech.billing.infrastructure.database.entities",
    "com.bcbs239.regtech.billing.infrastructure.outbox",
    "com.bcbs239.regtech.core.inbox",
    // data-quality domain and entities
    "com.bcbs239.regtech.dataquality.rulesengine.domain",
    "com.bcbs239.regtech.dataquality.domain",
    "com.bcbs239.regtech.dataquality.infrastructure.database.entities",
    "com.bcbs239.regtech.dataquality.infrastructure.reporting",
    // report-generation entities
    "com.bcbs239.regtech.reportgeneration.infrastructure.database.entities",
    "com.bcbs239.regtech.reportgeneration.infrastructure.persistence.configuration",
    // risk-calculation entities
    "com.bcbs239.regtech.riskcalculation.infrastructure.database.entities",
    // ingestion entities
    "com.bcbs239.regtech.ingestion.infrastructure",
    // core infrastructure entities
    "com.bcbs239.regtech.core.infrastructure.eventprocessing",
    "com.bcbs239.regtech.core.infrastructure.saga"
})
@EnableJpaRepositories(basePackages = {
    // Repository packages used by modules (consolidated JPA repositories)
    "com.bcbs239.regtech.iam.infrastructure.database.repositories",
    "com.bcbs239.regtech.iam.infrastructure.persistence.bankprofile",
    "com.bcbs239.regtech.billing.infrastructure.database.repositories",
    "com.bcbs239.regtech.billing.infrastructure.outbox",
    "com.bcbs239.regtech.core.inbox",
    // report-generation repositories
    "com.bcbs239.regtech.reportgeneration.infrastructure.database.repositories",
    "com.bcbs239.regtech.reportgeneration.infrastructure.persistence.configuration",
    // risk-calculation repositories
    "com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories",
    // ingestion repositories
    "com.bcbs239.regtech.ingestion.infrastructure",
    // data-quality repositories
    "com.bcbs239.regtech.dataquality.infrastructure",
    // core infrastructure repositories
    "com.bcbs239.regtech.core.infrastructure.eventprocessing",
    "com.bcbs239.regtech.core.infrastructure.saga"
})
public class ModularJpaConfiguration {
    
    /**
     * Note: Spring Boot 4.x automatically configures:
     * - LocalContainerEntityManagerFactoryBean with JPA 3.2 support
     * - HibernateJpaVendorAdapter for Hibernate 7.x
     * - PersistenceConfiguration support
     * - EntityManager injection with @Inject and qualifiers
     * 
     * No additional configuration is required unless custom behavior is needed.
     */
}
