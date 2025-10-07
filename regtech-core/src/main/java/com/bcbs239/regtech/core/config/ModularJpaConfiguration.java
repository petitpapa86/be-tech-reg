package com.bcbs239.regtech.core.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
    "com.bcbs239.regtech.iam.domain",
    "com.bcbs239.regtech.billing.domain"
})
@EnableJpaRepositories(basePackages = {
    "com.bcbs239.regtech.iam.infrastructure.repository",
    "com.bcbs239.regtech.billing.infrastructure.repository"
})
public class ModularJpaConfiguration {
}