package com.bcbs239.regtech.dataquality.infrastructure.rulesengine;

import com.bcbs239.regtech.dataquality.application.rulesengine.RuleExecutionPort;
import com.bcbs239.regtech.dataquality.application.validation.ValidationExecutionStats;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.rulesengine.domain.*;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleExecutionResult;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Infrastructure adapter implementing RuleExecutionPort.
 * Handles the actual rule execution using the Rules Engine.
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "data-quality.rules-engine",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class RulesEngineAdapter implements RuleExecutionPort {

    private static final long SLOW_RULE_WARN_MIN_INTERVAL_MS = 60_000;

    private final ConcurrentHashMap<String, Long> lastSlowRuleWarnAtMsByRuleId = new ConcurrentHashMap<>();

    private final RulesEngine rulesEngine;
    private final IRuleExemptionRepository exemptionRepository;

    private volatile ExemptionCache exemptionCache;

    // Configuration thresholds
    private final int warnThresholdMs;
    private final boolean logExecutions;
    private final boolean logViolations;


    public RulesEngineAdapter(
            RulesEngine rulesEngine,
            IRuleExemptionRepository exemptionRepository,
            @Value("${data-quality.rules-engine.performance.warn-threshold-ms:100}") int warnThresholdMs,
            @Value("${data-quality.rules-engine.logging.log-executions:true}") boolean logExecutions,
            @Value("${data-quality.rules-engine.logging.log-violations:true}") boolean logViolations) {
        this.rulesEngine = rulesEngine;
        this.exemptionRepository = exemptionRepository;
        this.warnThresholdMs = warnThresholdMs;
        this.logExecutions = logExecutions;
        this.logViolations = logViolations;
    }

    @Override
    public void preloadExemptionsForBatch(String entityType, List<String> entityIds, LocalDate currentDate) {
        if (entityType == null || entityType.isBlank()) {
            return;
        }
        if (entityIds == null || entityIds.isEmpty()) {
            clearExemptionCache();
            return;
        }

        LocalDate asOfDate = currentDate != null ? currentDate : LocalDate.now();

        List<String> distinctEntityIds = entityIds.stream()
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();

        if (distinctEntityIds.isEmpty()) {
            clearExemptionCache();
            return;
        }

        List<RuleExemptionDto> exemptions = exemptionRepository.findAllActiveExemptionsForBatch(
            entityType,
            distinctEntityIds,
            asOfDate
        );

        ExemptionCache newCache = ExemptionCache.build(entityType, asOfDate, distinctEntityIds, exemptions);
        this.exemptionCache = newCache; // Single atomic write
        log.debug(
            "Preloaded {} active exemptions for entityType={} across {} entities (asOf={})",
            exemptions.size(),
            entityType,
            distinctEntityIds.size(),
            asOfDate
        );
    }

    @Override
    public void clearExemptionCache() {
        this.exemptionCache = null;
    }

    public void execute(
            BusinessRuleDto rule,
            RuleContext context,
            ExposureRecord exposure,
            List<ValidationError> errors,
            List<RuleViolation> violations,
            ValidationExecutionStats stats
    ) {
        if (!rule.isApplicableOn(LocalDate.now())) {
            stats.incrementSkipped();
            return;
        }

        long startTime = System.nanoTime(); // More precise for timing

        try {
            RuleExecutionResult result = rulesEngine.executeRule(rule.ruleId(), context);
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;

            stats.incrementExecuted(executionTimeMs); // Pass timing

            if (executionTimeMs > warnThresholdMs && shouldLogSlowRuleWarn(rule.ruleId())) {
                log.warn("Slow rule execution: rule={}, exposureId={}, time={}ms",
                        rule.ruleId(), exposure.exposureId(), executionTimeMs);
            }

            if (!result.isSuccess() && result.hasViolations()) {
                stats.incrementFailed();
                handleViolations(rule, context, exposure, result, errors, violations, stats);
            }

        } catch (Exception e) {
            stats.incrementFailed(); // Pass timing
            logExecutionError(rule, exposure, context, e);
        }
    }


    private boolean shouldLogSlowRuleWarn(String ruleId) {
        if (ruleId == null) {
            return true;
        }

        long now = System.currentTimeMillis();
        Long last = lastSlowRuleWarnAtMsByRuleId.get(ruleId);
        if (last == null) {
            return lastSlowRuleWarnAtMsByRuleId.putIfAbsent(ruleId, now) == null;
        }

        if (now - last < SLOW_RULE_WARN_MIN_INTERVAL_MS) {
            return false;
        }

        // Best-effort CAS update
        return lastSlowRuleWarnAtMsByRuleId.replace(ruleId, last, now);
    }

    private void handleViolations(
            BusinessRuleDto rule,
            RuleContext context,
            ExposureRecord exposure,
            RuleExecutionResult result,
            List<ValidationError> errors,
            List<RuleViolation> violations,
            ValidationExecutionStats stats
    ) {
        String entityType = (String) context.get("entity_type");
        String entityId = (String) context.get("entity_id");

        if (checkExemption(rule.ruleId(), entityType, entityId)) {
            log.debug("Active exemption found for rule {} and entity {}/{}. Skipping violation.",
                    rule.ruleId(), entityType, entityId);
            return;
        }

        stats.incrementFailed();

        for (RuleViolation violation : result.getViolations()) {
            violations.add(violation);

            if (logViolations) {
                log.debug("Violation: ruleCode={}, exposureId={}, severity={}, type={}, description={}",
                        rule.ruleCode(), exposure.exposureId(), violation.severity(),
                        violation.violationType(), violation.violationDescription());
            }

            errors.add(convertToValidationError(violation, rule));
        }
    }

    private void logExecutionError(BusinessRuleDto rule, ExposureRecord exposure,
                                   RuleContext context, Exception e) {
        log.error("Error executing rule {} for exposure {}: {} - Context: entityType={}, ruleType={}, severity={}",
                rule.ruleId(), exposure.exposureId(), e.getMessage(),
                context.get("entity_type"), rule.ruleType(), rule.severity(), e);
    }

    private ValidationError convertToValidationError(RuleViolation violation, BusinessRuleDto rule) {
        // Copy logic from original service
        com.bcbs239.regtech.dataquality.domain.quality.QualityDimension dimension = mapToQualityDimension(rule.ruleType());
        ValidationError.ErrorSeverity severity = mapToValidationSeverity(violation.severity());
        String fieldName = extractFieldFromDetails(violation.violationDetails());

        return new ValidationError(
            violation.violationType(),
            violation.violationDescription(),
            fieldName,
            dimension,
            violation.entityId(),
            severity
        );
    }

    private com.bcbs239.regtech.dataquality.domain.quality.QualityDimension mapToQualityDimension(RuleType ruleType) {
        if (ruleType == null) {
            log.warn("Rule type is null, defaulting to ACCURACY dimension");
            return com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.ACCURACY;
        }

        return switch (ruleType) {
            case COMPLETENESS -> com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.COMPLETENESS;
            case ACCURACY -> com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.ACCURACY;
            case CONSISTENCY -> com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.CONSISTENCY;
            case TIMELINESS -> com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.TIMELINESS;
            case UNIQUENESS -> com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.UNIQUENESS;
            case VALIDITY -> com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.VALIDITY;
            // For non-dimension specific rule types, map to ACCURACY as default
            case VALIDATION, CALCULATION, TRANSFORMATION, DATA_QUALITY,
                 THRESHOLD, BUSINESS_LOGIC -> com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.ACCURACY;
        };
    }

    private ValidationError.ErrorSeverity mapToValidationSeverity(Severity severity) {
        if (severity == null) {
            log.warn("Severity is null, defaulting to MEDIUM");
            return ValidationError.ErrorSeverity.MEDIUM;
        }

        return switch (severity) {
            case LOW -> ValidationError.ErrorSeverity.LOW;
            case MEDIUM -> ValidationError.ErrorSeverity.MEDIUM;
            case HIGH -> ValidationError.ErrorSeverity.HIGH;
            case CRITICAL -> ValidationError.ErrorSeverity.CRITICAL;
        };
    }


    private String extractFieldFromDetails(java.util.Map<String, Object> details) {
        if (details != null && details.containsKey("field")) {
            return String.valueOf(details.get("field"));
        }
        return null;
    }

    private boolean checkExemption(String ruleId, String entityType, String entityId) {
        if (ruleId == null || entityType == null || entityId == null) {
            log.debug("Cannot check exemption: ruleId={}, entityType={}, entityId={}",
                ruleId, entityType, entityId);
            return false;
        }

        try {
            LocalDate currentDate = LocalDate.now();

            ExemptionCache cache = this.exemptionCache;
            if (cache != null && cache.matches(entityType, currentDate)) {
                Boolean cachedDecision = cache.isExempted(ruleId, entityId);
                if (cachedDecision != null) {
                    if (cachedDecision) {
                        log.debug(
                            "Active exemption found (cache) for rule {} and entity {}/{}. Skipping violation reporting.",
                            ruleId,
                            entityType,
                            entityId
                        );
                    }
                    return cachedDecision;
                }
                // Cache present but does not cover this entityId; fall back to DB.
            }

            // Find active exemptions for this rule and entity
            List<RuleExemptionDto> activeExemptions = exemptionRepository.findActiveExemptions(
                ruleId,
                entityType,
                entityId,
                currentDate
            );

            if (!activeExemptions.isEmpty()) {
                // Log exemption details for audit trail
                for (RuleExemptionDto exemption : activeExemptions) {
                    log.debug("Active exemption found: exemptionId={}, ruleId={}, entityType={}, entityId={}, " +
                            "exemptionType={}, effectiveDate={}, expirationDate={}, approvedBy={}, reason={}",
                        exemption.exemptionId(),
                        ruleId,
                        entityType,
                        entityId,
                        exemption.exemptionType(),
                        exemption.effectiveDate(),
                        exemption.expirationDate(),
                        exemption.approvedBy(),
                        exemption.exemptionReason()
                    );
                }
                return true;
            }

            log.debug("No active exemption found for rule {} and entity {}/{}",
                ruleId, entityType, entityId);
            return false;

        } catch (Exception e) {
            log.error("Error checking exemption for rule {} and entity {}/{}: {}",
                ruleId, entityType, entityId, e.getMessage(), e);
            // On error, fail safe by not granting exemption
            return false;
        }
    }

    private static final class ExemptionCache {
        private final String entityType;
        private final LocalDate asOfDate;
        private final Set<String> loadedEntityIds;
        private final Map<String, RuleIdIndex> byRuleId;

        private ExemptionCache(
            String entityType,
            LocalDate asOfDate,
            Set<String> loadedEntityIds,
            Map<String, RuleIdIndex> byRuleId
        ) {
            this.entityType = entityType;
            this.asOfDate = asOfDate;
            this.loadedEntityIds = loadedEntityIds;
            this.byRuleId = byRuleId;
        }

        boolean matches(String entityType, LocalDate asOfDate) {
            return this.entityType.equals(entityType) && this.asOfDate.equals(asOfDate);
        }

        /**
         * @return Boolean decision if covered by cache; null if entityId isn't covered and DB fallback is required.
         */
        Boolean isExempted(String ruleId, String entityId) {
            if (ruleId == null || ruleId.isBlank()) {
                return Boolean.FALSE;
            }

            RuleIdIndex index = byRuleId.get(ruleId);
            if (index == null) {
                // If we preloaded the entity, this is a definitive "not exempt".
                return loadedEntityIds.contains(entityId) ? Boolean.FALSE : null;
            }

            if (index.appliesToAllEntities) {
                return Boolean.TRUE;
            }

            if (!loadedEntityIds.contains(entityId)) {
                return null;
            }

            return index.entityIds.contains(entityId);
        }

        static ExemptionCache build(
                String entityType,
                LocalDate asOfDate,
                List<String> loadedEntityIds,
                List<RuleExemptionDto> exemptions
        ) {
            Set<String> loaded = Set.copyOf(loadedEntityIds);
            Map<String, RuleIdIndex> byRule = new HashMap<>();

            if (exemptions == null || exemptions.isEmpty()) {
                return new ExemptionCache(entityType, asOfDate, loaded, Map.of());
            }

            // Group by ruleId
            Map<String, List<RuleExemptionDto>> byRuleId = exemptions.stream()
                    .filter(ex -> ex != null && ex.ruleId() != null && !ex.ruleId().isBlank())
                    .collect(Collectors.groupingBy(RuleExemptionDto::ruleId));

            for (Map.Entry<String, List<RuleExemptionDto>> entry : byRuleId.entrySet()) {
                String ruleId = entry.getKey();
                List<RuleExemptionDto> ruleExemptions = entry.getValue();

                // Check if any exemption applies to all entities (null/blank entityId)
                boolean appliesToAll = ruleExemptions.stream()
                        .anyMatch(ex -> ex.entityId() == null || ex.entityId().isBlank());

                Set<String> entityIds = ruleExemptions.stream()
                        .map(RuleExemptionDto::entityId)
                        .filter(id -> id != null && !id.isBlank())
                        .collect(Collectors.toUnmodifiableSet());

                byRule.put(ruleId, new RuleIdIndex(appliesToAll, entityIds));
            }

            return new ExemptionCache(entityType, asOfDate, loaded, Map.copyOf(byRule));
        }
    }

    private static final class RuleIdIndex {
        private final boolean appliesToAllEntities;
        private final Set<String> entityIds;

        private RuleIdIndex(boolean appliesToAllEntities, Set<String> entityIds) {
            this.appliesToAllEntities = appliesToAllEntities;
            this.entityIds = entityIds;
        }
    }
}