package com.bcbs239.regtech.core.domain.monitoring;


import org.springframework.boot.health.contributor.Health;

public interface ModuleHealthIndicator {

    String getModuleName();

    Health health();
}

