package com.bcbs239.regtech.dataquality.infrastructure.config;

import com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.engine.DefaultRulesEngine;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleExemptionRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.*;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import com.bcbs239.regtech.dataquality.rulesengine.evaluator.ExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

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
         * @param auditPersistenceService Persists execution logs/violations in isolated transactions
     * @param expressionEvaluator Evaluator for SpEL expressions
     * @param properties Data quality configuration properties
     * @return Configured RulesEngine instance
     */
    @Bean
    public RulesEngine rulesEngine(
            com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.BusinessRuleRepository ruleRepository,
             com.bcbs239.regtech.dataquality.infrastructure.rulesengine.engine.RulesEngineAuditPersistenceService auditPersistenceService,
            ExpressionEvaluator expressionEvaluator,
            DataQualityProperties properties) {
        
        boolean cacheEnabled = properties.getRulesEngine().isCacheEnabled();
        int cacheTtl = properties.getRulesEngine().getCacheTtl();
        
        log.info("✓ Rules Engine ENABLED - Using configurable business rules");
        log.info("  - Cache enabled: {}", cacheEnabled);
        log.info("  - Cache TTL: {}s", cacheTtl);
        
        return new DefaultRulesEngine(
            ruleRepository,
            auditPersistenceService,
            expressionEvaluator,
            cacheEnabled,
            cacheTtl
        );
    }
    
    /**
     * Creates adapter implementations for domain repository interfaces.
     */
    @Bean
    public IBusinessRuleRepository businessRuleRepositoryAdapter(
            com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.BusinessRuleRepository infraRepo,
            DefaultRulesEngine rulesEngine,
            DataQualityProperties properties) {

        boolean cacheEnabled = properties.getRulesEngine().isCacheEnabled();
        int cacheTtl = properties.getRulesEngine().getCacheTtl();

        return new BusinessRuleRepositoryAdapter(infraRepo, rulesEngine, cacheEnabled, cacheTtl);
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
     * @param ruleRepositoryAdapter Adapter for business rules repository
     * @param violationRepository Repository for violations
     * @param executionLogRepository Repository for execution logs
     * @param exemptionRepository Repository for exemptions
     * @param properties Data quality configuration properties
     * @return Configured DataQualityRulesService instance
     */
    @Bean
    public DataQualityRulesService dataQualityRulesService(
            RulesEngine rulesEngine,
            IBusinessRuleRepository ruleRepositoryAdapter,
            com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleViolationRepository violationRepository,
            com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleExecutionLogRepository executionLogRepository,
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
        
        // Create stub implementations for other repositories (they're not used yet)
        IRuleViolationRepository violationRepoAdapter = new IRuleViolationRepository() {
            @Override
            public void save(RuleViolation violation) {
                // Stub - not used by DataQualityRulesService yet
            }
        };
        IRuleExecutionLogRepository executionLogRepoAdapter = new IRuleExecutionLogRepository() {
            @Override
            public void save(RuleExecutionLogDto executionLog) {
                // Stub - not used by DataQualityRulesService yet
            }
        };
        IRuleExemptionRepository exemptionRepoAdapter = new IRuleExemptionRepository() {
            @Override
            public List<RuleExemptionDto> findActiveExemptions(String ruleId, String entityType, String entityId, LocalDate currentDate) {
                // Stub - not used by DataQualityRulesService yet
                return List.of();
            }
        };
        
        return new DataQualityRulesService(
            rulesEngine,
            ruleRepositoryAdapter,
            violationRepoAdapter,
            executionLogRepoAdapter,
            exemptionRepoAdapter,
            performance.getWarnThresholdMs(),
            performance.getMaxExecutionTimeMs(),
            logging.isLogExecutions(),
            logging.isLogViolations(),
            logging.isLogSummary()
        );
    }
}
