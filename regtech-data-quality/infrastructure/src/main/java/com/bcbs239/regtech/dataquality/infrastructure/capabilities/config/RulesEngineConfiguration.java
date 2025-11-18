package com.bcbs239.regtech.dataquality.infrastructure.deprecated.config;

import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for the Rules Engine feature.
 */
@Slf4j
@Deprecated
// Deprecated capabilities configuration preserved for backward-compatibility.
// The canonical configuration is available under infrastructure.config.RulesEngineConfiguration
// This class intentionally does not have @Configuration to avoid registering duplicate beans.
public class RulesEngineConfiguration {
    
    public RulesEngineConfiguration(RulesEngine rulesEngine) {
        log.info("✓ Rules Engine ENABLED - Using configurable business rules");
        log.info("  - Rules Engine implementation: {}", rulesEngine.getClass().getSimpleName());
    }
}
