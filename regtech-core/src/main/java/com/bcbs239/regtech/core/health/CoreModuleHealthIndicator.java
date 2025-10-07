package com.bcbs239.regtech.core.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
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