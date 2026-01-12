package com.bcbs239.regtech.dataquality.infrastructure.rules;

import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.rules.IRulesValidationService;
import com.bcbs239.regtech.dataquality.domain.rules.RuleViolation;
import com.bcbs239.regtech.dataquality.domain.rules.Severity;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.BusinessRuleEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.BusinessRuleRepository;
import com.bcbs239.regtech.dataquality.domain.rules.ParameterType;
import com.bcbs239.regtech.dataquality.domain.rules.RuleType;
import com.bcbs239.regtech.dataquality.rulesengine.engine.DefaultRuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleExecutionResult;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RulesEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Infrastructure implementation of rules-based validation service.
 * 
 * <p>This implementation uses the Rules Engine infrastructure to execute
 * database-driven business rules and convert results to domain validation errors.</p>
 * 
 * <p><strong>Location Rationale:</strong> This class belongs in infrastructure because:</p>
 * <ul>
 *   <li>It depends on infrastructure concerns (RulesEngine, Spring Data repositories)</li>
 *   <li>It provides technical implementation details for the domain interface</li>
 *   <li>It handles data mapping between Rules Engine and Domain models</li>
 *   <li>It manages external dependencies (database, Spring configuration)</li>
 * </ul>
 * 
 * <p><strong>Design Pattern:</strong> Bridge Pattern Implementation</p>
 * <p>Acts as the concrete implementor side of the bridge, providing
 * technical implementation for the domain abstraction.</p>
 * 
 * @see IRulesValidationService Domain interface this implements
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "regtech.dataquality.rules-engine",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class RulesValidationServiceImpl implements IRulesValidationService {
    
    private final RulesEngine rulesEngine;
    private final BusinessRuleRepository ruleRepository;
    
    @Override
    public List<ValidationError> validateConfigurableRules(ExposureRecord exposure) {
        
        // Get all active data quality rules
        List<BusinessRuleEntity> rules = ruleRepository
            .findByRuleTypeAndEnabledTrueOrderByExecutionOrder(RuleType.DATA_QUALITY);
        
        if (rules.isEmpty()) {
            log.debug("No configurable data quality rules found, skipping");
            return Collections.emptyList();
        }
        
        // Create rule context from exposure
        RuleContext context = createContextFromExposure(exposure);
        
        List<ValidationError> errors = new ArrayList<>();
        
        // Execute each rule
        for (BusinessRuleEntity rule : rules) {
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
    
    @Override
    public List<ValidationError> validateThresholdRules(ExposureRecord exposure) {
        
        List<BusinessRuleEntity> thresholdRules = ruleRepository
            .findByRuleCategoryAndEnabledTrue("THRESHOLDS");
        
        if (thresholdRules.isEmpty()) {
            return Collections.emptyList();
        }
        
        RuleContext context = createContextFromExposure(exposure);
        List<ValidationError> errors = new ArrayList<>();
        
        for (BusinessRuleEntity rule : thresholdRules) {
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
    
    @Override
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
    
    @Override
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
    
    @Override
    public boolean hasActiveRule(String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode)
            .map(BusinessRuleEntity::isActive)
            .orElse(false);
    }
    
    // ====================================================================
    // Private Infrastructure Helper Methods
    // ====================================================================
    
    /**
     * Creates a rule context from an exposure record.
     * Maps exposure fields to context variables for rule evaluation.
     */
    private RuleContext createContextFromExposure(ExposureRecord exposure) {
        Map<String, Object> data = new HashMap<>();
        
        // Map exposure fields to context.
        // Performance note: keep ONE canonical key per field (no aliasing / normalization).
        data.put("exposureId", exposure.exposureId());
        data.put("amount", exposure.exposureAmount());
        data.put("currency", exposure.currency());
        data.put("country", exposure.countryCode());
        data.put("sector", exposure.sector());
        data.put("counterpartyId", exposure.counterpartyId());
        data.put("counterpartyType", exposure.counterpartyType());
        data.put("leiCode", exposure.counterpartyLei());
        data.put("productType", exposure.productType());
        data.put("internalRating", exposure.internalRating());
        data.put("riskCategory", exposure.riskCategory());
        data.put("riskWeight", exposure.riskWeight());
        data.put("reportingDate", exposure.reportingDate());
        data.put("valuationDate", exposure.valuationDate());
        data.put("maturityDate", exposure.maturityDate());
        data.put("referenceNumber", exposure.referenceNumber());
        
        // Add helper flags
        data.put("isCorporateExposure", exposure.isCorporateExposure());
        data.put("isTermExposure", exposure.isTermExposure());
        
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
            List<RuleViolation> violations,
            BusinessRuleEntity rule) {
        
        return violations.stream()
            .map(violation -> {
                QualityDimension dimension = mapRuleToDimension(rule);
                
                return new ValidationError(
                    violation.violationType(),
                    violation.violationDescription(),
                    extractFieldFromDetails(violation.violationDetails()),
                    dimension,
                    violation.entityId(),
                    mapSeverity(violation.severity())
                );
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Maps rule category to quality dimension.
     */
    private QualityDimension mapRuleToDimension(BusinessRuleEntity rule) {
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
            Severity severity) {
        
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
