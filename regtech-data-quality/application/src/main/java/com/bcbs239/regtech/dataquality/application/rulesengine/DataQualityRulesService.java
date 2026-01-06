package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.application.validation.ExposureRuleValidator;
import com.bcbs239.regtech.dataquality.application.validation.ValidationResults;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.rules.BusinessRuleDto;
import com.bcbs239.regtech.dataquality.domain.rules.IBusinessRuleRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Service that implements ExposureRuleValidator for data quality validation.
 * Delegates to RuleExecutionService for pure rule execution logic.
 */
@Slf4j
public class DataQualityRulesService implements ExposureRuleValidator {


    private final IBusinessRuleRepository ruleRepository;
    private final RuleExecutionService ruleExecutionService;

    // ✅ Application-level rule caching (lasts entire batch)
    private volatile List<BusinessRuleDto> cachedRules = null;
    private final Object cacheLock = new Object();

    public DataQualityRulesService(
            IBusinessRuleRepository ruleRepository,
            RuleExecutionService ruleExecutionService) {
        this.ruleRepository = ruleRepository;
        this.ruleExecutionService = ruleExecutionService;
    }

    @Override
    public void prefetchForBatch(List<ExposureRecord> exposures) {
        log.info("⏱️ Prefetching for batch: {} exposures", exposures.size());

        // ✅ Pre-load and cache rules
        Instant ruleStart = Instant.now();
        getCachedRules();
        long ruleDuration = Duration.between(ruleStart, Instant.now()).toMillis();
        log.info("⏱️ Rules cached: {}ms", ruleDuration);
    }

    @Override
    public void onBatchComplete() {
        // Rule cache is useful for subsequent batches, so we don't clear it
    }

    /**
     * ✅ Use cached rules instead of loading from DB
     */
    @Override
    public ValidationResults validateNoPersist(ExposureRecord exposure) {
        List<BusinessRuleDto> rules = getCachedRules();
        return ruleExecutionService.execute(exposure, rules);
    }

    /**
     * ✅ Prepare batch validator with cached rules
     */
    @Override
    public Function<ExposureRecord, ValidationResults> prepareForBatch() {
        List<BusinessRuleDto> rules = getCachedRules();

        log.info("✅ Prepared {} enabled rules for batch validation (CACHED)", rules.size());

        // Return function that uses SAME cached rules for all exposures
        return exposure -> ruleExecutionService.execute(exposure, rules);
    }

    /**
     * ✅ Get cached rules (double-checked locking pattern)
     */
    private List<BusinessRuleDto> getCachedRules() {
        // Fast path - no synchronization if already cached
        if (cachedRules == null) {
            synchronized (cacheLock) {
                // Check again after acquiring lock
                if (cachedRules == null) {
                    log.info("🔄 Loading and caching business rules from repository...");
                    Instant start = Instant.now();

                    // Load and make immutable
                    cachedRules = List.copyOf(ruleRepository.findByEnabledTrue());

                    long duration = Duration.between(start, Instant.now()).toMillis();
                    log.info("✅ Cached {} rules in {}ms", cachedRules.size(), duration);
                }
            }
        }
        return cachedRules;
    }


}
