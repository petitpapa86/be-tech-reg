package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import com.bcbs239.regtech.dataquality.rulesengine.domain.*;
import com.bcbs239.regtech.dataquality.rulesengine.engine.DefaultRuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleExecutionResult;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleExemption;
import com.bcbs239.regtech.dataquality.rulesengine.repository.BusinessRuleRepository;
import com.bcbs239.regtech.dataquality.rulesengine.repository.RuleViolationRepository;
import com.bcbs239.regtech.dataquality.rulesengine.repository.RuleExecutionLogRepository;
import com.bcbs239.regtech.dataquality.rulesengine.repository.RuleExemptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bridge service that connects the validation pipeline with the Rules Engine
 * for configurable business rules.
 * 
 * <p>This service provides:</p>
 * <ul>
 *   <li>Validation of configurable rules for exposures</li>
 *   <li>Access to configurable parameters (thresholds, lists)</li>
 *   <li>Conversion between rule violations and validation errors</li>
 *   <li>Persistence of violations and execution logs</li>
 * </ul>
 * 
 * <p><strong>Design Pattern:</strong> Bridge Pattern - decouples abstraction (ValidationResult)
 * from implementation (Rules Engine) allowing both to vary independently.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityRulesService {
    
    private final RulesEngine rulesEngine;
    private final BusinessRuleRepository ruleRepository;
    private final RuleViolationRepository violationRepository;
    private final RuleExecutionLogRepository executionLogRepository;
    private final RuleExemptionRepository exemptionRepository;
    
    /**
     * Validates configurable rules for a single exposure.
     * This method executes all enabled rules, persists violations and execution logs,
     * and returns validation errors for integration with the validation pipeline.
     * 
     * @param exposure The exposure record to validate
     * @return List of validation errors from rules engine
     */
    public List<ValidationError> validateConfigurableRules(ExposureRecord exposure) {
        
        // Get all active rules (all enabled rules, not just DATA_QUALITY type)
        List<BusinessRule> rules = ruleRepository.findByEnabledTrue();
        
        if (rules.isEmpty()) {
            log.debug("No configurable rules found, skipping");
            return Collections.emptyList();
        }
        
        // Create rule context from exposure
        RuleContext context = createContextFromExposure(exposure);
        
        List<ValidationError> errors = new ArrayList<>();
        
        // Execute each rule
        for (BusinessRule rule : rules) {
            if (!rule.isApplicableOn(LocalDate.now())) {
                log.debug("Rule {} not applicable on current date, skipping", rule.getRuleId());
                continue;
            }
            
            long startTime = System.currentTimeMillis();
            RuleExecutionResult result = null;
            String errorMessage = null;
            
            try {
                result = rulesEngine.executeRule(rule.getRuleId(), context);
                
                if (!result.isSuccess() && result.hasViolations()) {
                    // Check for exemptions before reporting violations
                    String entityType = (String) context.get("entity_type");
                    String entityId = (String) context.get("entity_id");
                    
                    boolean hasActiveExemption = checkExemption(
                        rule.getRuleId(), 
                        entityType, 
                        entityId
                    );
                    
                    if (hasActiveExemption) {
                        log.debug("Active exemption found for rule {} and entity {}/{}. Skipping violation reporting.", 
                            rule.getRuleId(), entityType, entityId);
                        // Don't report violations for exempted entities
                        continue;
                    }
                    
                    // Convert rule violations to ValidationErrors
                    for (RuleViolation violation : result.getViolations()) {
                        // Persist violation
                        violationRepository.save(violation);
                        
                        // Convert to ValidationError
                        ValidationError validationError = convertToValidationError(violation, rule);
                        errors.add(validationError);
                    }
                }
                
            } catch (Exception e) {
                log.error("Error executing rule {}: {}", rule.getRuleId(), e.getMessage(), e);
                errorMessage = e.getMessage();
            } finally {
                long executionTime = System.currentTimeMillis() - startTime;
                
                // Persist execution log
                RuleExecutionLog executionLog = createExecutionLog(
                    rule, 
                    exposure, 
                    result, 
                    executionTime, 
                    errorMessage
                );
                executionLogRepository.save(executionLog);
            }
        }
        
        return errors;
    }
    
    /**
     * Validates threshold rules (amounts, dates, counts).
     * This method is kept for backward compatibility but delegates to validateConfigurableRules.
     * 
     * @deprecated Use validateConfigurableRules instead
     */
    @Deprecated
    public List<ValidationError> validateThresholdRules(ExposureRecord exposure) {
        // Delegate to the main validation method which handles all rules including thresholds
        return validateConfigurableRules(exposure);
    }
    
    /**
     * Gets configurable parameter value for a specific validation.
     * This allows specifications to use dynamic configuration instead of hardcoded values.
     * 
     * <p><strong>Example:</strong></p>
     * <pre>
     * // Instead of hardcoded: BigDecimal.valueOf(10_000_000_000)
     * // Use configurable:
     * BigDecimal maxAmount = rulesService
     *     .getConfigurableParameter("ACCURACY_MAX_AMOUNT", "max_reasonable_amount", BigDecimal.class)
     *     .orElse(DEFAULT_MAX_AMOUNT);
     * </pre>
     * 
     * @param ruleCode The rule code (e.g., "ACCURACY_MAX_AMOUNT")
     * @param parameterName The parameter name (e.g., "max_reasonable_amount")
     * @param type The expected type
     * @param <T> Type parameter
     * @return Optional containing the parameter value if found
     */
    public <T> Optional<T> getConfigurableParameter(
            String ruleCode, 
            String parameterName, 
            Class<T> type) {
        
        return ruleRepository.findByRuleCode(ruleCode)
            .flatMap(rule -> {
                return rule.getParameters().stream()
                    .filter(p -> p.getParameterName().equals(parameterName))
                    .findFirst()
                    .map(p -> p.getTypedValue(type));
            });
    }
    
    /**
     * Checks if a value list is configurable and retrieves it.
     * Useful for validating against dynamic lists (currencies, countries, etc.)
     * 
     * <p><strong>Example:</strong></p>
     * <pre>
     * Optional&lt;List&lt;String&gt;&gt; validCurrencies = rulesService
     *     .getConfigurableList("ACCURACY_VALID_CURRENCIES", "valid_currency_codes");
     * 
     * if (validCurrencies.isPresent()) {
     *     if (!validCurrencies.get().contains(exposure.currency())) {
     *         // Handle invalid currency
     *     }
     * }
     * </pre>
     * 
     * @param ruleCode The rule code
     * @param listName The list parameter name
     * @return Optional containing the list if found
     */
    public Optional<List<String>> getConfigurableList(String ruleCode, String listName) {
        return ruleRepository.findByRuleCode(ruleCode)
            .flatMap(rule -> {
                return rule.getParameters().stream()
                    .filter(p -> p.getParameterName().equals(listName))
                    .filter(p -> p.getParameterType() == ParameterType.LIST)
                    .findFirst()
                    .map(p -> Arrays.asList(p.getParameterValue().split(",")));
            });
    }
    
    /**
     * Checks if a configurable rule exists and is active.
     * 
     * @param ruleCode The rule code
     * @return true if the rule exists and is active
     */
    public boolean hasActiveRule(String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode)
            .map(BusinessRule::isActive)
            .orElse(false);
    }
    
    // ====================================================================
    // Private Helper Methods
    // ====================================================================
    
    /**
     * Creates a rule context from an exposure record.
     * Maps exposure fields to context variables for rule evaluation.
     */
    private RuleContext createContextFromExposure(ExposureRecord exposure) {
        Map<String, Object> data = new HashMap<>();
        
        // Map exposure fields to context
        data.put("exposure_id", exposure.exposureId());
        data.put("amount", exposure.amount());
        data.put("currency", exposure.currency());
        data.put("country", exposure.country());
        data.put("sector", exposure.sector());
        data.put("counterparty_id", exposure.counterpartyId());
        data.put("counterparty_type", exposure.counterpartyType());
        data.put("lei_code", exposure.leiCode());
        data.put("product_type", exposure.productType());
        data.put("internal_rating", exposure.internalRating());
        data.put("risk_category", exposure.riskCategory());
        data.put("reporting_date", exposure.reportingDate());
        data.put("valuation_date", exposure.valuationDate());
        data.put("maturity_date", exposure.maturityDate());
        data.put("reference_number", exposure.referenceNumber());
        
        // Add helper flags
        data.put("is_corporate_exposure", exposure.isCorporateExposure());
        data.put("is_term_exposure", exposure.isTermExposure());
        
        // Add entity metadata for exemption checking
        data.put("entity_type", "EXPOSURE");
        data.put("entity_id", exposure.exposureId());
        
        return new DefaultRuleContext(data);
    }
    
    /**
     * Converts a single RuleViolation to a ValidationError.
     * Maps the Rules Engine domain to the Data Quality domain.
     * 
     * @param violation The rule violation to convert
     * @param rule The business rule that was violated
     * @return ValidationError for use in validation pipeline
     */
    private ValidationError convertToValidationError(RuleViolation violation, BusinessRule rule) {
        QualityDimension dimension = mapToQualityDimension(rule.getRuleType());
        ValidationError.ErrorSeverity severity = mapToValidationSeverity(violation.getSeverity());
        String fieldName = extractFieldFromDetails(violation.getViolationDetails());
        
        return new ValidationError(
            violation.getViolationType(),
            violation.getViolationDescription(),
            fieldName,
            dimension,
            violation.getEntityId(),
            severity
        );
    }
    
    /**
     * Maps RuleType to QualityDimension.
     * This mapping ensures that rule violations are correctly categorized
     * into BCBS 239 quality dimensions.
     * 
     * @param ruleType The rule type from the business rule
     * @return The corresponding quality dimension
     */
    private QualityDimension mapToQualityDimension(RuleType ruleType) {
        if (ruleType == null) {
            log.warn("Rule type is null, defaulting to ACCURACY dimension");
            return QualityDimension.ACCURACY;
        }
        
        return switch (ruleType) {
            case COMPLETENESS -> QualityDimension.COMPLETENESS;
            case ACCURACY -> QualityDimension.ACCURACY;
            case CONSISTENCY -> QualityDimension.CONSISTENCY;
            case TIMELINESS -> QualityDimension.TIMELINESS;
            case UNIQUENESS -> QualityDimension.UNIQUENESS;
            case VALIDITY -> QualityDimension.VALIDITY;
            // For non-dimension specific rule types, map to ACCURACY as default
            case VALIDATION, CALCULATION, TRANSFORMATION, DATA_QUALITY, 
                 THRESHOLD, BUSINESS_LOGIC -> QualityDimension.ACCURACY;
        };
    }
    
    /**
     * Maps Rules Engine Severity to ValidationError ErrorSeverity.
     * 
     * @param severity The severity from the rule violation
     * @return The corresponding validation error severity
     */
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
    
    /**
     * Creates a RuleExecutionLog entry for persistence.
     * 
     * @param rule The business rule that was executed
     * @param exposure The exposure record that was validated
     * @param result The execution result (may be null if exception occurred)
     * @param executionTimeMs The execution time in milliseconds
     * @param errorMessage The error message if execution failed
     * @return RuleExecutionLog ready for persistence
     */
    private RuleExecutionLog createExecutionLog(
            BusinessRule rule,
            ExposureRecord exposure,
            RuleExecutionResult result,
            long executionTimeMs,
            String errorMessage) {
        
        ExecutionResult executionResult;
        int violationCount = 0;
        
        if (errorMessage != null) {
            executionResult = ExecutionResult.ERROR;
        } else if (result != null && result.isSuccess()) {
            executionResult = ExecutionResult.SUCCESS;
        } else if (result != null && result.hasViolations()) {
            executionResult = ExecutionResult.FAILURE;
            violationCount = result.getViolations().size();
        } else {
            executionResult = ExecutionResult.SUCCESS;
        }
        
        return RuleExecutionLog.builder()
            .ruleId(rule.getRuleId())
            .executionTimestamp(Instant.now())
            .entityType("EXPOSURE")
            .entityId(exposure.exposureId())
            .executionResult(executionResult)
            .violationCount(violationCount)
            .executionTimeMs(executionTimeMs)
            .errorMessage(errorMessage)
            .executedBy("SYSTEM") // Could be enhanced to track actual user
            .build();
    }
    
    /**
     * Extracts field name from violation details.
     */
    private String extractFieldFromDetails(Map<String, Object> details) {
        if (details != null && details.containsKey("field")) {
            return String.valueOf(details.get("field"));
        }
        return null;
    }
    
    /**
     * Checks if an active exemption exists for a specific rule and entity.
     * 
     * <p>An exemption is considered active if:</p>
     * <ul>
     *   <li>The exemption status is ACTIVE (not EXPIRED or REVOKED)</li>
     *   <li>The current date is on or after the effective_date</li>
     *   <li>The current date is before the expiration_date (or expiration_date is null)</li>
     *   <li>The exemption applies to the specified entity type and ID</li>
     * </ul>
     * 
     * <p>This method implements Requirements 10.1, 10.2, 10.3, 10.4, 10.5:</p>
     * <ul>
     *   <li>10.1: Check exemption validity before reporting violations</li>
     *   <li>10.2: Skip violation reporting for active exemptions</li>
     *   <li>10.3: Report violations for expired exemptions</li>
     *   <li>10.4: Report violations for revoked exemptions (handled by not finding them)</li>
     *   <li>10.5: Validate exemption dates (valid_from, valid_to)</li>
     * </ul>
     * 
     * @param ruleId The rule ID to check exemptions for
     * @param entityType The entity type (e.g., "EXPOSURE")
     * @param entityId The entity ID
     * @return true if an active exemption exists, false otherwise
     */
    private boolean checkExemption(String ruleId, String entityType, String entityId) {
        if (ruleId == null || entityType == null || entityId == null) {
            log.debug("Cannot check exemption: ruleId={}, entityType={}, entityId={}", 
                ruleId, entityType, entityId);
            return false;
        }
        
        try {
            LocalDate currentDate = LocalDate.now();
            
            // Find active exemptions for this rule and entity
            List<RuleExemption> activeExemptions = exemptionRepository.findActiveExemptions(
                ruleId, 
                entityType, 
                entityId, 
                currentDate
            );
            
            if (!activeExemptions.isEmpty()) {
                // Log exemption details for audit trail
                for (RuleExemption exemption : activeExemptions) {
                    log.info("Active exemption found: exemptionId={}, ruleId={}, entityType={}, entityId={}, " +
                            "exemptionType={}, effectiveDate={}, expirationDate={}, approvedBy={}, reason={}", 
                        exemption.getExemptionId(),
                        ruleId,
                        entityType,
                        entityId,
                        exemption.getExemptionType(),
                        exemption.getEffectiveDate(),
                        exemption.getExpirationDate(),
                        exemption.getApprovedBy(),
                        exemption.getExemptionReason()
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
}
