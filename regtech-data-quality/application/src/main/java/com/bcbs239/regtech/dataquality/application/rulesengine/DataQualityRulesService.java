package com.bcbs239.regtech.dataquality.application.rulesengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.transaction.annotation.Transactional;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.rulesengine.domain.BusinessRuleDto;
import com.bcbs239.regtech.dataquality.rulesengine.domain.IBusinessRuleRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.ParameterType;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleExecutionLogDto;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleViolation;
import com.bcbs239.regtech.dataquality.rulesengine.engine.DefaultRuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;

import com.bcbs239.regtech.dataquality.application.validation.ExposureRuleValidator;
import com.bcbs239.regtech.dataquality.application.validation.ValidationResults;

import lombok.extern.slf4j.Slf4j;

/**
 * Service that implements ExposureRuleValidator for data quality validation.
 * Delegates to RuleExecutionService for pure rule execution logic.
 */
@Slf4j
public class DataQualityRulesService implements ExposureRuleValidator {

    // Rate-limit slow warnings to avoid log spam on very large batches.
    private static final long SLOW_RULE_WARN_MIN_INTERVAL_MS = 60_000;
    private static final long SLOW_VALIDATION_WARN_MIN_INTERVAL_MS = 60_000;

    private final ConcurrentHashMap<String, Long> lastSlowRuleWarnAtMsByRuleId = new ConcurrentHashMap<>();
    private final AtomicLong lastSlowValidationWarnAtMs = new AtomicLong(0);

    private final IBusinessRuleRepository ruleRepository;
    private final RuleViolationRepository violationRepository;
    private final RuleExecutionLogRepository executionLogRepository;
    private final RuleExecutionService ruleExecutionService;

    public DataQualityRulesService(
            IBusinessRuleRepository ruleRepository,
            RuleViolationRepository violationRepository,
            RuleExecutionLogRepository executionLogRepository,
            RuleExecutionService ruleExecutionService) {
        this.ruleRepository = ruleRepository;
        this.violationRepository = violationRepository;
        this.executionLogRepository = executionLogRepository;
        this.ruleExecutionService = ruleExecutionService;
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
    @Transactional
    public List<ValidationError> validateConfigurableRules(ExposureRecord exposure) {
        ValidationResultsDto results = validateConfigurableRulesNoPersist(exposure);
        persistValidationResults(results);
        return results.validationErrors();
    }

    /**
     * Validates configurable rules WITHOUT database writes.
     * Returns results for persistence later (e.g., batch insertion).
     *
     * <p>Designed for parallel processing: worker threads perform pure computation,
     * and the caller persists once after all validations complete.</p>
     */
    public ValidationResultsDto validateConfigurableRulesNoPersist(ExposureRecord exposure) {
        ValidationResults results = validateNoPersist(exposure);
        return new ValidationResultsDto(
            results.exposureId(),
            results.validationErrors(),
            results.ruleViolations(),
            results.executionLogs(),
            results.stats()
        );
    }

    @Override
    public ValidationResults validateNoPersist(ExposureRecord exposure) {
        List<BusinessRuleDto> rules = ruleRepository.findByEnabledTrue();
        return ruleExecutionService.execute(exposure, rules);
    }

    /**
     * Batch-persist validation results.
     * Call this AFTER parallel validation is complete.
     */
    @Transactional
    public void batchPersistValidationResults(List<ValidationResultsDto> allResults) {
        batchPersistValidationResults(null, allResults);
    }

    /**
     * Batch-persist validation results for a specific batch.
     *
     * <p>If {@code batchId} is non-null, repository adapters may populate the underlying
     * {@code batch_id} column for both execution logs and violations.</p>
     */
    @Transactional
    public void batchPersistValidationResults(String batchId, List<ValidationResultsDto> allResults) {
        if (allResults == null || allResults.isEmpty()) {
            log.debug("No validation results to persist");
            return;
        }

        log.info("Batch-persisting validation results for {} exposures", allResults.size());

        List<RuleViolation> allViolations = new ArrayList<>();
        List<RuleExecutionLogDto> allExecutionLogs = new ArrayList<>();

        for (ValidationResultsDto result : allResults) {
            if (result == null) {
                continue;
            }
            if (result.ruleViolations() != null && !result.ruleViolations().isEmpty()) {
                allViolations.addAll(result.ruleViolations());
            }
            if (result.executionLogs() != null && !result.executionLogs().isEmpty()) {
                allExecutionLogs.addAll(result.executionLogs());
            }
        }

        log.debug(
            "Collected {} violations and {} execution logs from {} exposures",
            allViolations.size(),
            allExecutionLogs.size(),
            allResults.size()
        );

        // NOTE: execution_id is now optional (nullable). We still persist logs first to preserve
        // prior semantics and because adapters may build best-effort links for diagnostics.
        if (!allExecutionLogs.isEmpty()) {
            executionLogRepository.saveAllForBatch(batchId, allExecutionLogs);
            executionLogRepository.flush();
        }

        if (!allViolations.isEmpty()) {
            violationRepository.saveAllForBatch(batchId, allViolations);
        }

        // Always flush (and let adapters clear any thread-scoped mapping).
        violationRepository.flush();
    }

    private void persistValidationResults(ValidationResultsDto results) {
        if (results == null) {
            return;
        }

        // IMPORTANT: execution logs must be persisted BEFORE violations.
        // `rule_violations.execution_id` has a NOT NULL FK to `rule_execution_log.execution_id`.

        List<RuleExecutionLogDto> executionLogs = results.executionLogs();
        if (executionLogs != null) {
            for (RuleExecutionLogDto executionLog : executionLogs) {
                executionLogRepository.save(executionLog);
            }
            executionLogRepository.flush();
        }

        List<RuleViolation> violations = results.ruleViolations();
        if (violations != null) {
            for (RuleViolation violation : violations) {
                violationRepository.save(violation);
            }
        }

        // Flush even if there were no violations, to allow adapters to clear any thread-scoped mapping.
        violationRepository.flush();
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

    private boolean shouldLogSlowValidationWarn() {
        long now = System.currentTimeMillis();
        long last = lastSlowValidationWarnAtMs.get();
        if (now - last < SLOW_VALIDATION_WARN_MIN_INTERVAL_MS) {
            return false;
        }
        return lastSlowValidationWarnAtMs.compareAndSet(last, now);
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
        
        // Map exposure fields to context.
        // Performance note: keep ONE canonical key per field (no aliasing / normalization).
        putIfNotNull(data, "exposureId", exposure.exposureId());
        putIfNotNull(data, "amount", exposure.amount());
        putIfNotNull(data, "currency", exposure.currency());
        putIfNotNull(data, "country", exposure.country());
        putIfNotNull(data, "sector", exposure.sector());
        putIfNotNull(data, "counterpartyId", exposure.counterpartyId());
        putIfNotNull(data, "counterpartyType", exposure.counterpartyType());
        putIfNotNull(data, "leiCode", exposure.leiCode());
        putIfNotNull(data, "productType", exposure.productType());
        putIfNotNull(data, "internalRating", exposure.internalRating());
        putIfNotNull(data, "riskCategory", exposure.riskCategory());
        putIfNotNull(data, "riskWeight", exposure.riskWeight());
        putIfNotNull(data, "reportingDate", exposure.reportingDate());
        putIfNotNull(data, "valuationDate", exposure.valuationDate());
        putIfNotNull(data, "maturityDate", exposure.maturityDate());
        putIfNotNull(data, "referenceNumber", exposure.referenceNumber());
        
        // Add helper flags
        data.put("isCorporateExposure", exposure.isCorporateExposure());
        data.put("isTermExposure", exposure.isTermExposure());
        
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
}
