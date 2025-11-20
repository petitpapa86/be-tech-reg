package com.bcbs239.regtech.core.infrastructure.persistence;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
    // Domain packages (value objects, enums) and persistence entity packages
    "com.bcbs239.regtech.iam.domain",
    "com.bcbs239.regtech.billing.domain",
    "com.bcbs239.regtech.iam.infrastructure.database.entities",
    "com.bcbs239.regtech.billing.infrastructure.database.entities",
    "com.bcbs239.regtech.billing.infrastructure.outbox",
    "com.bcbs239.regtech.core.inbox",
    // data-quality domain and reporting entities
    "com.bcbs239.regtech.dataquality.rulesengine.domain",
    "com.bcbs239.regtech.dataquality.domain",
    "com.bcbs239.regtech.dataquality.infrastructure.reporting"
})
@EnableJpaRepositories(basePackages = {
    // Repository packages used by modules (consolidated JPA repositories)
    "com.bcbs239.regtech.iam.infrastructure.database.repositories",
    "com.bcbs239.regtech.billing.infrastructure.database.repositories",
    "com.bcbs239.regtech.billing.infrastructure.outbox",
    "com.bcbs239.regtech.core.inbox"
})
public class ModularJpaConfiguration {
}
