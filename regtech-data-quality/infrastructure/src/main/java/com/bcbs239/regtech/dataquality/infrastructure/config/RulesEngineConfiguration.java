package com.bcbs239.regtech.dataquality.infrastructure.config;

import com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import com.bcbs239.regtech.dataquality.rulesengine.evaluator.ExpressionEvaluator;
import com.bcbs239.regtech.dataquality.rulesengine.repository.RuleExemptionRepository;
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
    matchIfMissing = false  // Requirement 7.1: Must be explicitly enabled, no default
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
    
    /**
     * Creates the DataQualityRulesService bean with logging configuration.
     * 
     * <p>The logging settings are injected from DataQualityProperties:</p>
     * <ul>
     *   <li>logExecutions: Whether to log individual rule executions (Requirement 6.1)</li>
     *   <li>logViolations: Whether to log violation details (Requirement 6.2)</li>
     *   <li>logSummary: Whether to log summary statistics (Requirement 6.4)</li>
     *   <li>warnThresholdMs: Threshold for slow rule warning (Requirement 6.5)</li>
     *   <li>maxExecutionTimeMs: Threshold for slow validation warning (Requirement 6.5)</li>
     * </ul>
     * 
     * @param rulesEngine The rules engine
     * @param ruleRepository Repository for business rules
     * @param violationRepository Repository for violations
     * @param executionLogRepository Repository for execution logs
     * @param exemptionRepository Repository for exemptions
     * @param properties Data quality configuration properties
     * @return Configured DataQualityRulesService instance
     */
    @Bean
    public DataQualityRulesService dataQualityRulesService(
            RulesEngine rulesEngine,
            BusinessRuleRepository ruleRepository,
            RuleViolationRepository violationRepository,
            RuleExecutionLogRepository executionLogRepository,
            RuleExemptionRepository exemptionRepository,
            DataQualityProperties properties) {
        
        DataQualityProperties.RulesEngineProperties.LoggingProperties logging = 
            properties.getRulesEngine().getLogging();
        DataQualityProperties.RulesEngineProperties.PerformanceProperties performance = 
            properties.getRulesEngine().getPerformance();
        
        log.info("✓ DataQualityRulesService configured with logging:");
        log.info("  - Log executions: {}", logging.isLogExecutions());
        log.info("  - Log violations: {}", logging.isLogViolations());
        log.info("  - Log summary: {}", logging.isLogSummary());
        log.info("  - Warn threshold: {}ms", performance.getWarnThresholdMs());
        log.info("  - Max execution time: {}ms", performance.getMaxExecutionTimeMs());
        
        return new DataQualityRulesService(
            rulesEngine,
            ruleRepository,
            violationRepository,
            executionLogRepository,
            exemptionRepository,
            performance.getWarnThresholdMs(),
            performance.getMaxExecutionTimeMs(),
            logging.isLogExecutions(),
            logging.isLogViolations(),
            logging.isLogSummary()
        );
    }
}
