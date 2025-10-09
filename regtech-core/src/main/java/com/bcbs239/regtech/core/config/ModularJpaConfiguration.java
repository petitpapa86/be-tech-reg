package com.bcbs239.regtech.core.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
    // Domain packages (value objects, enums) and persistence entity packages
    "com.bcbs239.regtech.iam.domain",
    "com.bcbs239.regtech.billing.domain",
    "com.bcbs239.regtech.iam.infrastructure.database.entities",
    "com.bcbs239.regtech.billing.infrastructure.database.entities"
})
@EnableJpaRepositories(basePackages = {
    // Repository packages used by modules (consolidated JPA repositories)
    "com.bcbs239.regtech.iam.infrastructure.database.repositories",
    "com.bcbs239.regtech.billing.infrastructure.database.repositories"
})
public class ModularJpaConfiguration {
}