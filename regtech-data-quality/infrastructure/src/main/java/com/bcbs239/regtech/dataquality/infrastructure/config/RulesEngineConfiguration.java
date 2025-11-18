package com.bcbs239.regtech.dataquality.infrastructure.config;

import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Rules Engine feature.
 * 
 * <p>The Rules Engine is optional and can be disabled via configuration:
 * <pre>
 * regtech.dataquality.rules-engine.enabled=false
 * </pre>
 * 
 * <p>When disabled, the system falls back to hardcoded Specifications.
 * This allows gradual migration and A/B testing.</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = "regtech.dataquality.rules-engine",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true  // Enabled by default
)
public class RulesEngineConfiguration {
    
    public RulesEngineConfiguration(RulesEngine rulesEngine) {
        log.info("✓ Rules Engine ENABLED - Using configurable business rules");
        log.info("  - Rules Engine implementation: {}", rulesEngine.getClass().getSimpleName());
    }
}
