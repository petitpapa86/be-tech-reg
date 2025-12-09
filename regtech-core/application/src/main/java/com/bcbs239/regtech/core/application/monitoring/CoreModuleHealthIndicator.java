package com.bcbs239.regtech.core.application.monitoring;

import com.bcbs239.regtech.core.domain.monitoring.ModuleHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

@Component("coreApplicationModuleHealthIndicator")
public class CoreModuleHealthIndicator implements ModuleHealthIndicator {

    @Override
    public String getModuleName() {
        return "core";
    }

    @Override
    public Health health() {
        // Add core-specific health checks here
        return Health.up().build();
    }
}

