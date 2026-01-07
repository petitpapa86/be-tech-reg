package com.bcbs239.regtech.dataquality.infrastructure.rulesengine;

import com.bcbs239.regtech.dataquality.application.rulesengine.RuleExecutionPort;
import com.bcbs239.regtech.dataquality.application.validation.ValidationExecutionStats;
import com.bcbs239.regtech.dataquality.domain.rules.*;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleExecutionResult;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

    // Configuration thresholds
    private final int warnThresholdMs;
    private final boolean logViolations;


    public RulesEngineAdapter(
            RulesEngine rulesEngine,
            @Value("${data-quality.rules-engine.performance.warn-threshold-ms:100}") int warnThresholdMs,
            @Value("${data-quality.rules-engine.logging.log-violations:true}") boolean logViolations) {
        this.rulesEngine = rulesEngine;
        this.warnThresholdMs = warnThresholdMs;
        this.logViolations = logViolations;
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
}