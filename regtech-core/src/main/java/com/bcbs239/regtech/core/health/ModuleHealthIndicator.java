package com.bcbs239.regtech.core.health;

import org.springframework.boot.actuate.health.Health;

public interface ModuleHealthIndicator {

    String getModuleName();

    Health health();
}