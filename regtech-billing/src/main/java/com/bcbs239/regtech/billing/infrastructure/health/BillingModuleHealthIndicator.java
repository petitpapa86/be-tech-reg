package com.bcbs239.regtech.billing.infrastructure.health;

import com.bcbs239.regtech.core.health.ModuleHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
public class BillingModuleHealthIndicator implements ModuleHealthIndicator {

    @Override
    public String getModuleName() {
        return "billing";
    }

    @Override
    public Health health() {
        // Add billing-specific health checks here (e.g., payment service, invoice service)
        return Health.up().build();
    }
}