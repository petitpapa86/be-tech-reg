package com.bcbs239.regtech.core.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ModularHealthIndicator implements HealthIndicator {

    private final List<ModuleHealthIndicator> moduleHealthIndicators;

    public ModularHealthIndicator(List<ModuleHealthIndicator> moduleHealthIndicators) {
        this.moduleHealthIndicators = moduleHealthIndicators;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        for (ModuleHealthIndicator indicator : moduleHealthIndicators) {
            Health moduleHealth = indicator.health();
            builder.withDetail(indicator.getModuleName(), moduleHealth.getStatus());
        }

        return builder.build();
    }
}