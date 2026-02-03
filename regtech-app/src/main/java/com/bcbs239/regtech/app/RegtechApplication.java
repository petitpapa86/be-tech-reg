package com.bcbs239.regtech.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the RegTech modular monolith.
 * This module orchestrates all domain modules (IAM, Billing, etc.)
 * and provides centralized configuration and startup.
 * 
 * Includes observability configuration for OpenTelemetry.
 * Requirements: 10.1, 10.2, 10.3
 */
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {
        "com.bcbs239.regtech.app",
        // include only the 'application' slice of core to avoid scanning domain/infrastructure modules that
        // may declare colliding beans (e.g. SagaConfiguration, health indicators, processors)
        "com.bcbs239.regtech.core.application",
        // include specific infrastructure packages needed at runtime (logger implementation)
        "com.bcbs239.regtech.core.infrastructure.logging",
        // ensure core.infrastructure package is scanned so ModularJpaConfiguration (and other @Configuration classes)
        // are picked up. This allows module-level @EnableJpaRepositories declared in core.infrastructure to register
        // repositories located in other modules (e.g., iam, billing).
        // Exclude outbox package to avoid conflict with application layer OutboxProcessingConfiguration
        "com.bcbs239.regtech.core.infrastructure.persistence",
        "com.bcbs239.regtech.core.infrastructure.eventprocessing",
        "com.bcbs239.regtech.core.infrastructure.commandprocessing",
        "com.bcbs239.regtech.core.infrastructure.logging",
        "com.bcbs239.regtech.core.infrastructure.securityauthorization",
        "com.bcbs239.regtech.core.infrastructure.authorization",
        "com.bcbs239.regtech.core.infrastructure.saga",
        "com.bcbs239.regtech.core.infrastructure.systemservices",
        "com.bcbs239.regtech.core.infrastructure.s3",
        "com.bcbs239.regtech.core.infrastructure.filestorage",
        "com.bcbs239.regtech.core.infrastructure.storage",
        "com.bcbs239.regtech.core.infrastructure.recommendations",
        "com.bcbs239.regtech.iam",
        "com.bcbs239.regtech.billing",
        "com.bcbs239.regtech.ingestion.infrastructure.configuration",
        "com.bcbs239.regtech.dataquality",
        "com.bcbs239.regtech.riskcalculation.infrastructure.config",
        "com.bcbs239.regtech.riskcalculation.presentation",
        "com.bcbs239.regtech.reportgeneration",
        "com.bcbs239.regtech.signal",
        "com.bcbs239.regtech.metrics"
        // Ensure module presentation packages are scanned so functional RouterFunction beans are registered
}, excludeFilters = {
})
@EntityScan(basePackages = {
    "com.bcbs239.regtech.core.infrastructure",
    "com.bcbs239.regtech.metrics.infrastructure.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.bcbs239.regtech.core.infrastructure",
    "com.bcbs239.regtech.metrics.infrastructure.repository"
})
@EnableAspectJAutoProxy
public class RegtechApplication {
    

    public static void main(String[] args) {
        SpringApplication.run(RegtechApplication.class, args);
    }
}
