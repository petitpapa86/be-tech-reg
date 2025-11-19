package com.bcbs239.regtech.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the RegTech modular monolith.
 * This module orchestrates all domain modules (IAM, Billing, etc.)
 * and provides centralized configuration and startup.
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
        "com.bcbs239.regtech.iam",
        "com.bcbs239.regtech.billing",
        "com.bcbs239.regtech.ingestion.infrastructure.configuration"
})
@EntityScan(basePackages = {
        "com.bcbs239.regtech.core.infrastructure"
})
@EnableJpaRepositories(basePackages = {
        "com.bcbs239.regtech.core.infrastructure"
})
@EnableAspectJAutoProxy
public class RegtechApplication {
    public static void main(String[] args) {
        SpringApplication.run(RegtechApplication.class, args);
    }
}
