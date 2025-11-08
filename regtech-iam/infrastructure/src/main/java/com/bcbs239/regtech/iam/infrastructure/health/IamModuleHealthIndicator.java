package com.bcbs239.regtech.iam.infrastructure.health;

import com.bcbs239.regtech.core.domain.monitoring.ModuleHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
public class IamModuleHealthIndicator implements ModuleHealthIndicator {

    @Override
    public String getModuleName() {
        return "iam";
    }

    @Override
    public Health health() {
        // Add IAM-specific health checks here (e.g., user service, auth service)
        return Health.up().build();
    }
}

