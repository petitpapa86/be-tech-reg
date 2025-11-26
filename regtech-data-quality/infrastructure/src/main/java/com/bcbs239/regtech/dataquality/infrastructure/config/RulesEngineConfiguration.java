package com.bcbs239.regtech.dataquality.infrastructure.config;

import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import com.bcbs239.regtech.dataquality.rulesengine.evaluator.ExpressionEvaluator;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.engine.DefaultRulesEngine;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.BusinessRuleRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleExecutionLogRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleViolationRepository;
import com.bcbs239.regtech.modules.dataquality.infrastructure.config.DataQualityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Rules Engine feature with caching support.
 * 
 * <p>The Rules Engine is optional and can be disabled via configuration:
 * <pre>
 * data-quality.rules-engine.enabled=false
 * </pre>
 * 
 * <p>Caching configuration:</p>
 * <pre>
 * data-quality.rules-engine.cache-enabled=true
 * data-quality.rules-engine.cache-ttl=300  # seconds
 * </pre>
 * 
 * <p><strong>Requirements:</strong> 3.3, 3.4, 5.1, 5.2, 5.3, 5.4, 5.5</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DataQualityProperties.class)
@ConditionalOnProperty(
    prefix = "data-quality.rules-engine",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true  // Enabled by default
)
public class RulesEngineConfiguration {
    
    /**
     * Creates the DefaultRulesEngine bean with caching configuration.
     * 
     * <p>The cache settings are injected from DataQualityProperties:</p>
     * <ul>
     *   <li>cacheEnabled: Whether to cache rules in memory</li>
     *   <li>cacheTtl: Time-to-live for cached rules in seconds</li>
     * </ul>
     * 
     * @param ruleRepository Repository for loading rules
     * @param executionLogRepository Repository for logging executions
     * @param violationRepository Repository for storing violations
     * @param expressionEvaluator Evaluator for SpEL expressions
     * @param properties Data quality configuration properties
     * @return Configured RulesEngine instance
     */
    @Bean
    public RulesEngine rulesEngine(
            BusinessRuleRepository ruleRepository,
            RuleExecutionLogRepository executionLogRepository,
            RuleViolationRepository violationRepository,
            ExpressionEvaluator expressionEvaluator,
            DataQualityProperties properties) {
        
        boolean cacheEnabled = properties.getRulesEngine().isCacheEnabled();
        int cacheTtl = properties.getRulesEngine().getCacheTtl();
        
        log.info("✓ Rules Engine ENABLED - Using configurable business rules");
        log.info("  - Cache enabled: {}", cacheEnabled);
        log.info("  - Cache TTL: {}s", cacheTtl);
        
        return new DefaultRulesEngine(
            ruleRepository,
            executionLogRepository,
            violationRepository,
            expressionEvaluator,
            cacheEnabled,
            cacheTtl
        );
    }
}
