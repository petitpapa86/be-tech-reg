package com.bcbs239.regtech.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
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
        "com.bcbs239.regtech.core",
        "com.bcbs239.regtech.iam",
        "com.bcbs239.regtech.billing",
        "com.bcbs239.regtech.ingestion.infrastructure.configuration",
        "com.bcbs239.regtech.riskcalculation"
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.bcbs239.regtech.core.health.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.bcbs239.regtech.core.capabilities.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.bcbs239.regtech.core.domain.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.bcbs239.regtech.core.inboxprocessing.*")
})
@EntityScan(basePackages = {
        "com.bcbs239.regtech.core.infrastructure",
        "com.bcbs239.regtech.riskcalculation.infrastructure"
})
@EnableJpaRepositories(basePackages = {
        "com.bcbs239.regtech.core.infrastructure",
        "com.bcbs239.regtech.riskcalculation.infrastructure"
})
@EnableAspectJAutoProxy
public class RegtechApplication {
    public static void main(String[] args) {
        SpringApplication.run(RegtechApplication.class, args);
    }
}

