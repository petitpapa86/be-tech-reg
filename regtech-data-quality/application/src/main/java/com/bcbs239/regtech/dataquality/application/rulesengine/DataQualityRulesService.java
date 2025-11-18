package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.rulesengine.domain.BusinessRule;
import com.bcbs239.regtech.dataquality.rulesengine.domain.ParameterType;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleType;
import com.bcbs239.regtech.dataquality.rulesengine.engine.DefaultRuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleExecutionResult;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import com.bcbs239.regtech.dataquality.rulesengine.repository.BusinessRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bridge service that connects the existing Specifications-based validation
 * with the new Rules Engine for configurable business rules.
 * 
 * <p>This service allows gradual migration from hardcoded specifications
 * to database-driven business rules. It provides:</p>
 * <ul>
 *   <li>Validation of configurable rules for exposures</li>
 *   <li>Access to configurable parameters (thresholds, lists)</li>
 *   <li>Conversion between rule violations and validation errors</li>
 * </ul>
 * 
 * <p><strong>Design Pattern:</strong> Bridge Pattern - decouples abstraction (Specifications)
 * from implementation (Rules Engine) allowing both to vary independently.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityRulesService {
    
    private final RulesEngine rulesEngine;
    private final BusinessRuleRepository ruleRepository;
    
    /**
     * Validates configurable threshold rules for a single exposure.
     * This complements the existing Specification-based validations.
     * 
     * @param exposure The exposure record to validate
     * @return List of validation errors from rules engine
     */
    public List<ValidationError> validateConfigurableRules(ExposureRecord exposure) {
        
        // Get all active data quality rules
        List<BusinessRule> rules = ruleRepository
            .findByRuleTypeAndEnabledTrueOrderByExecutionOrder(RuleType.DATA_QUALITY);
        
        if (rules.isEmpty()) {
            log.debug("No configurable data quality rules found, skipping");
            return Collections.emptyList();
        }
        
        // Create rule context from exposure
        RuleContext context = createContextFromExposure(exposure);
        
        List<ValidationError> errors = new ArrayList<>();
        
        // Execute each rule
        for (BusinessRule rule : rules) {
            if (!rule.isApplicableOn(LocalDate.now())) {
                continue;
            }
            
            try {
                RuleExecutionResult result = rulesEngine.executeRule(rule.getRuleId(), context);
                
                if (!result.isSuccess() && result.hasViolations()) {
                    // Convert rule violations to ValidationErrors
                    errors.addAll(convertViolationsToErrors(result.getViolations(), rule));
                }
                
            } catch (Exception e) {
                log.error("Error executing rule {}: {}", rule.getRuleId(), e.getMessage());
            }
        }
        
        return errors;
    }
    
    /**
     * Validates threshold rules (amounts, dates, counts)
     */
    public List<ValidationError> validateThresholdRules(ExposureRecord exposure) {
        
        List<BusinessRule> thresholdRules = ruleRepository
            .findByRuleCategoryAndEnabledTrue("THRESHOLDS");
        
        if (thresholdRules.isEmpty()) {
            return Collections.emptyList();
        }
        
        RuleContext context = createContextFromExposure(exposure);
        List<ValidationError> errors = new ArrayList<>();
        
        for (BusinessRule rule : thresholdRules) {
            try {
                RuleExecutionResult result = rulesEngine.executeRule(rule.getRuleId(), context);
                
                if (!result.isSuccess() && result.hasViolations()) {
                    errors.addAll(convertViolationsToErrors(result.getViolations(), rule));
                }
                
            } catch (Exception e) {
                log.error("Error executing threshold rule {}: {}", rule.getRuleId(), e.getMessage());
            }
        }
        
        return errors;
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
     * Converts rule violations to validation errors.
     * Maps the Rules Engine domain to the Data Quality domain.
     */
    private List<ValidationError> convertViolationsToErrors(
            List<com.bcbs239.regtech.dataquality.rulesengine.domain.RuleViolation> violations,
            BusinessRule rule) {
        
        return violations.stream()
            .map(violation -> {
                QualityDimension dimension = mapRuleToDimension(rule);
                
                return new ValidationError(
                    violation.getViolationType(),
                    violation.getViolationDescription(),
                    extractFieldFromDetails(violation.getViolationDetails()),
                    dimension,
                    violation.getEntityId(),
                    mapSeverity(violation.getSeverity())
                );
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Maps rule category to quality dimension.
     */
    private QualityDimension mapRuleToDimension(BusinessRule rule) {
        // Map rule category to quality dimension
        if (rule.getRuleCategory() == null) {
            return QualityDimension.ACCURACY;
        }
        
        return switch (rule.getRuleCategory().toUpperCase()) {
            case "COMPLETENESS" -> QualityDimension.COMPLETENESS;
            case "ACCURACY" -> QualityDimension.ACCURACY;
            case "CONSISTENCY" -> QualityDimension.CONSISTENCY;
            case "TIMELINESS" -> QualityDimension.TIMELINESS;
            case "UNIQUENESS" -> QualityDimension.UNIQUENESS;
            default -> QualityDimension.ACCURACY;
        };
    }
    
    /**
     * Maps Rules Engine severity to ValidationError severity.
     */
    private ValidationError.ErrorSeverity mapSeverity(
            com.bcbs239.regtech.dataquality.rulesengine.domain.Severity severity) {
        
        return switch (severity) {
            case LOW -> ValidationError.ErrorSeverity.LOW;
            case MEDIUM -> ValidationError.ErrorSeverity.MEDIUM;
            case HIGH, CRITICAL -> ValidationError.ErrorSeverity.CRITICAL;
        };
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
}
