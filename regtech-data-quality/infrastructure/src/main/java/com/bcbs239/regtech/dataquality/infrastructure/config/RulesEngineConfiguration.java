package com.bcbs239.regtech.dataquality.infrastructure.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService;
import com.bcbs239.regtech.dataquality.application.rulesengine.RuleExecutionService;
import com.bcbs239.regtech.dataquality.application.rulesengine.RuleViolationRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.engine.DefaultRulesEngine;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleExemptionEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleViolationEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.evaluator.ExpressionEvaluator;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleExemptionRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RulesEngineBatchLinkContext;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RulesEngineJdbcBatchInserter;
import com.bcbs239.regtech.dataquality.rulesengine.domain.IBusinessRuleRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.IRuleExemptionRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleExemptionDto;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleViolation;

import lombok.extern.slf4j.Slf4j;


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
        boolean auditPersistenceEnabled = properties.getRulesEngine().isAuditPersistenceEnabled();
        
        log.info("✓ Rules Engine ENABLED - Using configurable business rules");
        log.info("  - Cache enabled: {}", cacheEnabled);
        log.info("  - Cache TTL: {}s", cacheTtl);
        log.info("  - Audit persistence enabled: {}", auditPersistenceEnabled);
        
        return new DefaultRulesEngine(
            ruleRepository,
            auditPersistenceService,
            expressionEvaluator,
            cacheEnabled,
            cacheTtl,
            auditPersistenceEnabled
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
     * Single implementation of the application port for violation persistence.
     *
     * <p>Uses JDBC batch inserts for high throughput. This is intentionally a Bean so there
     * is exactly one wiring for the port across the application.</p>
     */
    @Bean
    public RuleViolationRepository ruleViolationRepositoryPort(
            com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleViolationRepository violationRepository,
            RulesEngineBatchLinkContext linkContext,
            RulesEngineJdbcBatchInserter jdbcBatchInserter) {

        return new RuleViolationRepository() {
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


                if (!violations.isEmpty()) {
                    jdbcBatchInserter.insertViolations(batchId, violations);
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
     * @param ruleRepositoryAdapter Adapter for business rules repository
     * @param violationRepository Repository for violations
     * @param properties Data quality configuration properties
     * @return Configured DataQualityRulesService instance
     */
    @Bean
    public DataQualityRulesService dataQualityRulesService(
            IBusinessRuleRepository ruleRepositoryAdapter,
            RuleViolationRepository violationRepository,
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
        
        return new DataQualityRulesService(
            ruleRepositoryAdapter,
            violationRepository,
            ruleExecutionService
        );
    }

    @Bean
    public IRuleExemptionRepository ruleExemptionRepositoryAdapter(RuleExemptionRepository exemptionRepository) {
        return new IRuleExemptionRepository() {
            @Override
            public java.util.List<RuleExemptionDto> findActiveExemptions(
                String ruleId,
                String entityType,
                String entityId,
                java.time.LocalDate currentDate
            ) {
                return exemptionRepository.findActiveExemptions(ruleId, entityType, entityId, currentDate)
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

            @Override
            public java.util.List<RuleExemptionDto> findAllActiveExemptionsForBatch(
                String entityType,
                java.util.List<String> entityIds,
                java.time.LocalDate currentDate
            ) {
                if (entityIds == null || entityIds.isEmpty()) {
                    return java.util.List.of();
                }

                int chunkSize = 10_000;
                List<RuleExemptionDto> allExemptions = new ArrayList<>();

                log.info("⏱️ Loading exemptions for {} entity IDs in chunks of {}",
                        entityIds.size(), chunkSize);

                for (int i = 0; i < entityIds.size(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, entityIds.size());
                    List<String> chunk = entityIds.subList(i, end);

                    log.debug("Loading chunk {}-{}", i, end);

                    List<RuleExemptionEntity> chunkResults = exemptionRepository.findAllActiveExemptionsForBatch(
                            entityType,
                            chunk,
                            currentDate
                    );

                    List<RuleExemptionDto> chunkDtos = chunkResults.stream()
                            .map(e -> new RuleExemptionDto(
                                e.getExemptionId(),
                                e.getRule() != null ? e.getRule().getRuleId() : null,
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

                    allExemptions.addAll(chunkDtos);
                }

                log.info("⏱️ Loaded {} total exemptions", allExemptions.size());

                return allExemptions;
            }
        };
    }

    // execution_id is now optional (nullable). Treat non-positive IDs as unset.
    private static Long normalizeExecutionId(Long executionId) {
        return (executionId != null && executionId > 0) ? executionId : null;
    }
}
