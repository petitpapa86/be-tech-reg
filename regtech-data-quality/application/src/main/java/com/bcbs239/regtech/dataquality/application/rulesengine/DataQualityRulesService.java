package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;

import com.bcbs239.regtech.dataquality.rulesengine.domain.*;
import com.bcbs239.regtech.dataquality.rulesengine.engine.DefaultRuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleExecutionResult;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

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
 * 
 * <p><strong>Note:</strong> This class is configured as a @Bean in RulesEngineConfiguration,
 * not as a @Service, to allow conditional creation based on rules-engine.enabled property.</p>
 */
@Slf4j
@Service
public class DataQualityRulesService {
    
    private final RulesEngine rulesEngine;
    private final IBusinessRuleRepository ruleRepository;
    private final IRuleViolationRepository violationRepository;
    private final IRuleExecutionLogRepository executionLogRepository;
    private final IRuleExemptionRepository exemptionRepository;
    
    // Configuration thresholds - using default values that can be overridden
    private final int warnThresholdMs;
    private final int maxExecutionTimeMs;
    private final boolean logExecutions;
    private final boolean logViolations;
    private final boolean logSummary;
    
    /**
     * Constructor with default configuration values.
     * Used when configuration properties are not available.
     */
    public DataQualityRulesService(
            RulesEngine rulesEngine,
            IBusinessRuleRepository ruleRepository,
            IRuleViolationRepository violationRepository,
            IRuleExecutionLogRepository executionLogRepository,
            IRuleExemptionRepository exemptionRepository) {
        this(rulesEngine, ruleRepository, violationRepository, executionLogRepository, 
             exemptionRepository, 100, 5000, true, true, true);
    }
    
    /**
     * Constructor with configurable thresholds.
     * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
     * 
     * @param rulesEngine The rules engine
     * @param ruleRepository Repository for business rules
     * @param violationRepository Repository for violations
     * @param executionLogRepository Repository for execution logs
     * @param exemptionRepository Repository for exemptions
     * @param warnThresholdMs Threshold for slow rule warning (Requirement 6.5)
     * @param maxExecutionTimeMs Threshold for slow validation warning (Requirement 6.5)
     * @param logExecutions Whether to log individual rule executions (Requirement 6.1)
     * @param logViolations Whether to log violation details (Requirement 6.2)
     * @param logSummary Whether to log summary statistics (Requirement 6.4)
     */
    public DataQualityRulesService(
            RulesEngine rulesEngine,
            IBusinessRuleRepository ruleRepository,
            IRuleViolationRepository violationRepository,
            IRuleExecutionLogRepository executionLogRepository,
            IRuleExemptionRepository exemptionRepository,
            int warnThresholdMs,
            int maxExecutionTimeMs,
            boolean logExecutions,
            boolean logViolations,
            boolean logSummary) {
        this.rulesEngine = rulesEngine;
        this.ruleRepository = ruleRepository;
        this.violationRepository = violationRepository;
        this.executionLogRepository = executionLogRepository;
        this.exemptionRepository = exemptionRepository;
        this.warnThresholdMs = warnThresholdMs;
        this.maxExecutionTimeMs = maxExecutionTimeMs;
        this.logExecutions = logExecutions;
        this.logViolations = logViolations;
        this.logSummary = logSummary;
        
        log.info("DataQualityRulesService initialized with logging configuration: " +
                "logExecutions={}, logViolations={}, logSummary={}, warnThresholdMs={}, maxExecutionTimeMs={}",
            logExecutions, logViolations, logSummary, warnThresholdMs, maxExecutionTimeMs);
    }
    
    /**
     * Validates configurable rules for a single exposure.
     * This method executes all enabled rules, persists violations and execution logs,
     * and returns validation errors for integration with the validation pipeline.
     * 
     * <p><strong>Logging:</strong> This method implements comprehensive logging per Requirements 6.1-6.5:</p>
     * <ul>
     *   <li>6.1: Logs rule execution count and duration</li>
     *   <li>6.2: Logs violation details with exposure ID</li>
     *   <li>6.3: Logs errors with rule code and context</li>
     *   <li>6.4: Logs summary statistics after validation</li>
     *   <li>6.5: Emits warnings for slow rule execution</li>
     * </ul>
     * 
     * @param exposure The exposure record to validate
     * @return List of validation errors from rules engine
     */
    public List<ValidationError> validateConfigurableRules(ExposureRecord exposure) {
        long validationStartTime = System.currentTimeMillis();
        
        // Get all active rules (all enabled rules, not just DATA_QUALITY type)
        List<BusinessRuleDto> rules = ruleRepository.findByEnabledTrue();
        
        if (rules.isEmpty()) {
            log.debug("No configurable rules found, skipping validation for exposure {}", 
                exposure.exposureId());
            return Collections.emptyList();
        }
        
        // Requirement 6.1: Log rule execution count at start
        log.info("Starting Rules Engine validation for exposure {} with {} active rules", 
            exposure.exposureId(), rules.size());
        
        // Create rule context from exposure
        RuleContext context = createContextFromExposure(exposure);
        
        List<ValidationError> errors = new ArrayList<>();
        
        // Statistics tracking for summary (Requirement 6.4)
        int rulesExecuted = 0;
        int rulesSkipped = 0;
        int rulesFailed = 0;
        int rulesErrored = 0;
        int totalViolations = 0;
        long totalRuleExecutionTime = 0;
        long slowestRuleTime = 0;
        String slowestRuleId = null;
        
        // Execute each rule
        for (BusinessRuleDto rule : rules) {
            if (!rule.isApplicableOn(LocalDate.now())) {
                log.debug("Rule {} not applicable on current date, skipping", rule.ruleId());
                rulesSkipped++;
                continue;
            }
            
            long ruleStartTime = System.currentTimeMillis();
            RuleExecutionResult result = null;
            String errorMessage = null;
            
            try {
                result = rulesEngine.executeRule(rule.ruleId(), context);
                rulesExecuted++;
                
                long ruleExecutionTime = System.currentTimeMillis() - ruleStartTime;
                totalRuleExecutionTime += ruleExecutionTime;
                
                // Track slowest rule
                if (ruleExecutionTime > slowestRuleTime) {
                    slowestRuleTime = ruleExecutionTime;
                    slowestRuleId = rule.ruleId();
                }
                
                // Requirement 6.5: Emit warnings for slow rule execution
                if (ruleExecutionTime > warnThresholdMs) {
                    log.warn("Slow rule execution detected: rule={}, exposureId={}, executionTime={}ms, threshold={}ms", 
                        rule.ruleId(), exposure.exposureId(), ruleExecutionTime, warnThresholdMs);
                }
                
                // Requirement 6.1: Log individual rule execution
                if (logExecutions) {
                    log.debug("Rule {} executed for exposure {} in {}ms - result: {}", 
                        rule.ruleId(), exposure.exposureId(), ruleExecutionTime, 
                        result.isSuccess() ? "SUCCESS" : "FAILURE");
                }
                
                if (!result.isSuccess() && result.hasViolations()) {
                    // Check for exemptions before reporting violations
                    String entityType = (String) context.get("entity_type");
                    String entityId = (String) context.get("entity_id");
                    
                    boolean hasActiveExemption = checkExemption(
                        rule.ruleId(), 
                        entityType, 
                        entityId
                    );
                    
                    if (hasActiveExemption) {
                        log.debug("Active exemption found for rule {} and entity {}/{}. Skipping violation reporting.", 
                            rule.ruleId(), entityType, entityId);
                        // Don't report violations for exempted entities
                        continue;
                    }
                    
                    rulesFailed++;
                    int violationCount = result.getViolations().size();
                    totalViolations += violationCount;
                    
                    // Requirement 6.2: Log violation details with exposure ID
                    if (logViolations) {
                        log.info("Rule {} violated for exposure {}: {} violation(s) detected", 
                            rule.ruleId(), exposure.exposureId(), violationCount);
                    }
                    
                    // Convert rule violations to ValidationErrors
                    for (RuleViolation violation : result.getViolations()) {
                        // Persist violation
                        violationRepository.save(violation);
                        
                        // Requirement 6.2: Log detailed violation information
                        if (logViolations) {
                            log.debug("Violation details: ruleCode={}, exposureId={}, severity={}, type={}, description={}", 
                                rule.ruleCode(),
                                exposure.exposureId(), 
                                violation.severity(),
                                violation.violationType(),
                                violation.violationDescription());
                        }
                        
                        // Convert to ValidationError
                        ValidationError validationError = convertToValidationError(violation, rule);
                        errors.add(validationError);
                    }
                }
                
            } catch (Exception e) {
                rulesErrored++;
                
                // Requirement 6.3: Log errors with rule code and context
                log.error("Error executing rule {} for exposure {}: {} - Context: entityType={}, ruleType={}, severity={}", 
                    rule.ruleId(), 
                    exposure.exposureId(), 
                    e.getMessage(), 
                    context.get("entity_type"),
                    rule.ruleType(),
                    rule.severity(),
                    e);
                
                errorMessage = e.getMessage();
            } finally {
                long executionTime = System.currentTimeMillis() - ruleStartTime;
                
                // Persist execution log
                RuleExecutionLogDto executionLog = createExecutionLog(
                    rule, 
                    exposure, 
                    result, 
                    executionTime, 
                    errorMessage
                );
                executionLogRepository.save(executionLog);
            }
        }
        
        long totalValidationTime = System.currentTimeMillis() - validationStartTime;
        
        // Requirement 6.4: Log summary statistics after validation
        if (logSummary) {
            log.info("Rules Engine validation completed for exposure {}: " +
                    "totalRules={}, executed={}, skipped={}, failed={}, errored={}, " +
                    "totalViolations={}, totalTime={}ms, avgRuleTime={}ms, slowestRule={} ({}ms)",
                exposure.exposureId(),
                rules.size(),
                rulesExecuted,
                rulesSkipped,
                rulesFailed,
                rulesErrored,
                totalViolations,
                totalValidationTime,
                rulesExecuted > 0 ? totalRuleExecutionTime / rulesExecuted : 0,
                slowestRuleId,
                slowestRuleTime
            );
        }
        
        // Requirement 6.5: Emit warning if overall validation is slow
        if (totalValidationTime > maxExecutionTimeMs) {
            log.warn("Slow validation detected for exposure {}: totalTime={}ms, threshold={}ms, " +
                    "rulesExecuted={}, avgRuleTime={}ms",
                exposure.exposureId(),
                totalValidationTime,
                maxExecutionTimeMs,
                rulesExecuted,
                rulesExecuted > 0 ? totalRuleExecutionTime / rulesExecuted : 0
            );
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
                return rule.parameters().stream()
                    .filter(p -> p.parameterName().equals(parameterName))
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
                return rule.parameters().stream()
                    .filter(p -> p.parameterName().equals(listName))
                    .filter(p -> p.parameterType() == ParameterType.LIST)
                    .findFirst()
                    .map(p -> Arrays.asList(p.parameterValue().split(",")));
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
            .map(BusinessRuleDto::isActive)
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
        
        // Map exposure fields to context (only non-null values)
        putIfNotNull(data, "exposure_id", exposure.exposureId());
        putIfNotNull(data, "amount", exposure.amount());
        putIfNotNull(data, "currency", exposure.currency());
        putIfNotNull(data, "country", exposure.country());
        putIfNotNull(data, "sector", exposure.sector());
        putIfNotNull(data, "counterparty_id", exposure.counterpartyId());
        putIfNotNull(data, "counterparty_type", exposure.counterpartyType());
        putIfNotNull(data, "lei_code", exposure.leiCode());
        putIfNotNull(data, "product_type", exposure.productType());
        putIfNotNull(data, "internal_rating", exposure.internalRating());
        putIfNotNull(data, "risk_category", exposure.riskCategory());
        putIfNotNull(data, "reporting_date", exposure.reportingDate());
        putIfNotNull(data, "valuation_date", exposure.valuationDate());
        putIfNotNull(data, "maturity_date", exposure.maturityDate());
        putIfNotNull(data, "reference_number", exposure.referenceNumber());
        
        // Add helper flags
        data.put("is_corporate_exposure", exposure.isCorporateExposure());
        data.put("is_term_exposure", exposure.isTermExposure());
        
        // Add entity metadata for exemption checking
        data.put("entity_type", "EXPOSURE");
        putIfNotNull(data, "entity_id", exposure.exposureId());
        
        return new DefaultRuleContext(data);
    }
    
    /**
     * Helper method to put a value in a map only if it's not null.
     * Prevents NullPointerException when using ConcurrentHashMap.
     */
    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
    
    /**
     * Converts a single RuleViolation to a ValidationError.
     * Maps the Rules Engine domain to the Data Quality domain.
     * 
     * @param violation The rule violation to convert
     * @param rule The business rule that was violated
     * @return ValidationError for use in validation pipeline
     */
    private ValidationError convertToValidationError(RuleViolation violation, BusinessRuleDto rule) {
        QualityDimension dimension = mapToQualityDimension(rule.ruleType());
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
     * Gets the rule type from the business rule DTO.
     */
    private RuleType getRuleType(BusinessRuleDto rule) {
        return rule.ruleType();
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
     * Creates a RuleExecutionLogDto entry for persistence.
     * 
     * @param rule The business rule that was executed
     * @param exposure The exposure record that was validated
     * @param result The execution result (may be null if exception occurred)
     * @param executionTimeMs The execution time in milliseconds
     * @param errorMessage The error message if execution failed
     * @return RuleExecutionLogDto ready for persistence
     */
    private RuleExecutionLogDto createExecutionLog(
            BusinessRuleDto rule,
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
        
        return RuleExecutionLogDto.builder()
            .ruleId(rule.ruleId())
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
            List<RuleExemptionDto> activeExemptions = exemptionRepository.findActiveExemptions(
                ruleId, 
                entityType, 
                entityId, 
                currentDate
            );
            
            if (!activeExemptions.isEmpty()) {
                // Log exemption details for audit trail
                for (RuleExemptionDto exemption : activeExemptions) {
                    log.info("Active exemption found: exemptionId={}, ruleId={}, entityType={}, entityId={}, " +
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
}
