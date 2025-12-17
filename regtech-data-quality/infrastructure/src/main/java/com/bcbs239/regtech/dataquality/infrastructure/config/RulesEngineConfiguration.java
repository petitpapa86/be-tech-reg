package com.bcbs239.regtech.dataquality.infrastructure.config;

import com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService;
import com.bcbs239.regtech.dataquality.application.rulesengine.RuleExecutionLogRepository;
import com.bcbs239.regtech.dataquality.application.rulesengine.RuleExecutionService;
import com.bcbs239.regtech.dataquality.application.rulesengine.RuleViolationRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.engine.DefaultRulesEngine;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleExecutionLogEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleViolationEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleExemptionRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RulesEngineBatchLinkContext;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RulesEngineJdbcBatchInserter;
import com.bcbs239.regtech.dataquality.rulesengine.domain.*;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.evaluator.ExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public DefaultRulesEngine rulesEngine(
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
            IBusinessRuleRepository ruleRepositoryAdapter,
            com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleViolationRepository violationRepository,
            com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleExecutionLogRepository executionLogRepository,
            RulesEngineBatchLinkContext linkContext,
            RulesEngineJdbcBatchInserter jdbcBatchInserter,
            RuleExecutionService ruleExecutionService,
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
        
        RuleExecutionLogRepository executionLogRepoAdapter = new RuleExecutionLogRepository() {
            @Override
            public void save(RuleExecutionLogDto executionLog) {
                if (executionLog == null) {
                    return;
                }
                RuleExecutionLogEntity entity = RuleExecutionLogEntity.builder()
                    .ruleId(executionLog.ruleId())
                    .executionTimestamp(executionLog.executionTimestamp())
                    .entityType(executionLog.entityType())
                    .entityId(executionLog.entityId())
                    .executionResult(executionLog.executionResult())
                    .violationCount(executionLog.violationCount())
                    .executionTimeMs(executionLog.executionTimeMs())
                    .contextData(executionLog.contextData())
                    .errorMessage(executionLog.errorMessage())
                    .executedBy(executionLog.executedBy())
                    .build();

                RuleExecutionLogEntity saved = executionLogRepository.save(entity);
                if (saved != null) {
                    linkContext.putExecutionId(saved.getRuleId(), saved.getEntityType(), saved.getEntityId(), saved.getExecutionId());
                }
            }

            @Override
            public void saveAllForBatch(String batchId, List<RuleExecutionLogDto> executionLogs) {
                if (executionLogs == null) {
                    return;
                }

                // New batch: clear mapping for this thread/request.
                linkContext.clear();

                // Fast path: JDBC batch insert (doesn't require returning generated IDs).
                java.util.List<RuleExecutionLogDto> dtoList = new java.util.ArrayList<>();
                for (RuleExecutionLogDto executionLog : executionLogs) {
                    if (executionLog != null) {
                        dtoList.add(executionLog);
                    }
                }
                if (!dtoList.isEmpty()) {
                    jdbcBatchInserter.insertExecutionLogs(batchId, dtoList);
                }
                return;
            }

            @Override
            public void flush() {
                executionLogRepository.flush();
            }
        };

        RuleViolationRepository violationRepoAdapter = new RuleViolationRepository() {
            @Override
            public void save(RuleViolation violation) {
                if (violation == null) {
                    return;
                }
                Long executionId = normalizeExecutionId(
                    violation.executionId() != null
                        ? violation.executionId()
                        : linkContext.getExecutionId(violation.ruleId(), violation.entityType(), violation.entityId())
                );

                RuleViolationEntity entity = RuleViolationEntity.builder()
                    .ruleId(violation.ruleId())
                    .executionId(executionId)
                    .entityType(violation.entityType())
                    .entityId(violation.entityId())
                    .violationType(violation.violationType())
                    .violationDescription(violation.violationDescription())
                    .severity(violation.severity())
                    .detectedAt(violation.detectedAt())
                    .violationDetails(violation.violationDetails())
                    .resolutionStatus(violation.resolutionStatus())
                    .build();

                violationRepository.save(entity);
            }

            @Override
            public void saveAllForBatch(String batchId, List<RuleViolation> violations) {
                if (violations == null) {
                    return;
                }

                // Fast path: JDBC batch insert.
                java.util.List<RuleViolation> vList = new java.util.ArrayList<>();
                for (RuleViolation v : violations) {
                    if (v != null) {
                        vList.add(v);
                    }
                }
                if (!vList.isEmpty()) {
                    jdbcBatchInserter.insertViolations(batchId, vList);
                }
            }

            @Override
            public void flush() {
                try {
                    violationRepository.flush();
                } finally {
                    // Avoid leaking mappings across requests.
                    linkContext.clear();
                }
            }
        };

        return new DataQualityRulesService(
            ruleRepositoryAdapter,
            violationRepoAdapter,
            executionLogRepoAdapter,
            ruleExecutionService
        );
    }

    @Bean
    public IRuleExemptionRepository ruleExemptionRepositoryAdapter(RuleExemptionRepository exemptionRepository) {
        return (ruleId, entityType, entityId, currentDate) ->
            exemptionRepository.findActiveExemptions(ruleId, entityType, entityId, currentDate)
                .stream()
                .map(e -> new RuleExemptionDto(
                    e.getExemptionId(),
                    e.getRule() != null ? e.getRule().getRuleId() : ruleId,
                    e.getEntityType(),
                    e.getEntityId(),
                    e.getExemptionReason(),
                    e.getExemptionType(),
                    e.getApprovedBy(),
                    e.getApprovalDate(),
                    e.getEffectiveDate(),
                    e.getExpirationDate(),
                    e.getConditions(),
                    e.getCreatedAt()
                ))
                .toList();
    }

    // execution_id is now optional (nullable). Treat non-positive IDs as unset.
    private static Long normalizeExecutionId(Long executionId) {
        return (executionId != null && executionId > 0) ? executionId : null;
    }
}
