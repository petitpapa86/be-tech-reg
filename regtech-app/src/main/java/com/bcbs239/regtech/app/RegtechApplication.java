package com.bcbs239.regtech.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for the RegTech modular monolith.
 * This module orchestrates all domain modules (IAM, Billing, etc.)
 * and provides centralized configuration and startup.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.bcbs239.regtech.core",
        "com.bcbs239.regtech.iam.infrastructure.config",
        // "com.bcbs239.regtech.billing",
        "com.bcbs239.regtech.app"
})
// Include the broader infrastructure package so JPA picks up SagaEntity and other entities
@EntityScan(basePackages = {
        "com.bcbs239.regtech.core.infrastructure",
        "com.bcbs239.regtech.core.infrastructure.outbox",
})
@EnableJpaRepositories(basePackages = {
        "com.bcbs239.regtech.core.infrastructure",
        "com.bcbs239.regtech.core.infrastructure.outbox",
})
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
public class RegtechApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegtechApplication.class, args);
    }

}

