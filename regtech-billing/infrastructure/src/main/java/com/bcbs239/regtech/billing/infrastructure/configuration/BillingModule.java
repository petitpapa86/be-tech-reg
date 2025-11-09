package com.bcbs239.regtech.billing.infrastructure.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@Configuration
@ComponentScan(basePackages = "com.bcbs239.regtech.billing")
@EntityScan(basePackages = "com.bcbs239.regtech.billing.infrastructure.database.entities")
@EnableJpaRepositories(basePackages = "com.bcbs239.regtech.billing.infrastructure.database.repositories")
public class BillingModule {
}

