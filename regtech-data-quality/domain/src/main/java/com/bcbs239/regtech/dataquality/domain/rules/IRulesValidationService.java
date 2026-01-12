package com.bcbs239.regtech.dataquality.domain.rules;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;

import java.util.List;
import java.util.Optional;

/**
 * Domain service interface for configurable business rules validation.
 * 
 * <p>This service provides the capability to validate exposures against
 * database-driven business rules, allowing dynamic configuration of
 * validation thresholds, lists, and business logic without code changes.</p>
 * 
 * <p><strong>Capability:</strong> Rules-Based Validation</p>
 * <p>This interface belongs to the domain layer because it defines a core
 * business capability - validating data against configurable business rules.
 * The implementation details (database access, rule engine) are hidden in
 * the infrastructure layer.</p>
 * 
 * <p><strong>Design Pattern:</strong> Bridge Pattern - decouples abstraction 
 * (this interface) from implementation (Rules Engine infrastructure) allowing 
 * both to vary independently.</p>
 * 
 * @see ExposureRecord Domain entity being validated
 * @see ValidationError Domain value object representing validation failures
 */
public interface IRulesValidationService {
    
    /**
     * Validates an exposure record against all active configurable rules.
     * 
     * <p>This complements the existing Specification-based validations by
     * adding database-driven rules that can be modified without code deployment.</p>
     * 
     * <p><strong>Example Usage:</strong></p>
     * <pre>
     * // In ValidationResult.validate()
     * List&lt;ValidationError&gt; errors = new ArrayList&lt;&gt;();
     * 
     * // Existing structural validations
     * errors.addAll(AccuracySpecifications.hasValidCurrency().validate(exposure));
     * 
     * // NEW: Configurable business rules
     * if (rulesService != null) {
     *     errors.addAll(rulesService.validateConfigurableRules(exposure));
     * }
     * </pre>
     * 
     * @param exposure The exposure record to validate
     * @return List of validation errors detected by rules engine (empty if all rules pass)
     */
    List<ValidationError> validateConfigurableRules(ExposureRecord exposure);
    
    /**
     * Validates threshold-based rules (amounts, counts, dates).
     * 
     * <p>This is a subset of configurable rules focusing on threshold
     * validations like maximum amounts, age limits, etc.</p>
     * 
     * @param exposure The exposure record to validate
     * @return List of threshold validation errors
     */
    List<ValidationError> validateThresholdRules(ExposureRecord exposure);
    
    /**
     * Retrieves a configurable parameter value for dynamic validation.
     * 
     * <p>Allows Specifications to use database-driven thresholds instead
     * of hardcoded values, enabling business users to adjust thresholds
     * without code changes.</p>
     * 
     * <p><strong>Example Usage in Specification:</strong></p>
     * <pre>
     * public class AccuracySpecifications {
     *     
     *     private final IRulesValidationService rulesService;
     *     
     *     public Specification&lt;ExposureRecord&gt; hasReasonableAmount() {
     *         // Try to get configurable threshold
     *         BigDecimal maxAmount = rulesService
     *             .getConfigurableParameter(
     *                 "ACCURACY_MAX_AMOUNT", 
     *                 "max_reasonable_amount", 
     *                 BigDecimal.class
     *             )
     *             .orElse(BigDecimal.valueOf(10_000_000_000)); // Fallback
     *             
     *         return exposure -&gt; exposure.exposureAmount().compareTo(maxAmount) &lt;= 0;
     *     }
     * }
     * </pre>
     * 
     * @param ruleCode The unique rule identifier (e.g., "ACCURACY_MAX_AMOUNT")
     * @param parameterName The parameter name (e.g., "max_reasonable_amount")
     * @param type The expected Java type
     * @param <T> Type parameter for type-safe return value
     * @return Optional containing the configured value, or empty if not found
     */
    <T> Optional<T> getConfigurableParameter(
        String ruleCode, 
        String parameterName, 
        Class<T> type
    );
    
    /**
     * Retrieves a configurable list for validation against dynamic value sets.
     * 
     * <p>Useful for validating against lists that change over time
     * (valid currencies, countries, sectors, etc.)</p>
     * 
     * <p><strong>Example Usage:</strong></p>
     * <pre>
     * // In hasValidCurrency() specification
     * Optional&lt;List&lt;String&gt;&gt; validCurrencies = rulesService
     *     .getConfigurableList("ACCURACY_VALID_CURRENCIES", "valid_currency_codes");
     *     
     * if (validCurrencies.isPresent()) {
     *     return exposure -&gt; validCurrencies.get().contains(exposure.currency());
     * } else {
     *     // Fallback to hardcoded list
     *     return exposure -&gt; VALID_CURRENCIES.contains(exposure.currency());
     * }
     * </pre>
     * 
     * @param ruleCode The rule identifier
     * @param listName The list parameter name
     * @return Optional containing the configured list, or empty if not found
     */
    Optional<List<String>> getConfigurableList(String ruleCode, String listName);
    
    /**
     * Checks if a configurable rule exists and is currently active.
     * 
     * <p>Useful for determining whether to use configurable validation
     * or fall back to hardcoded logic.</p>
     * 
     * @param ruleCode The rule identifier
     * @return true if the rule exists and is enabled, false otherwise
     */
    boolean hasActiveRule(String ruleCode);
}
