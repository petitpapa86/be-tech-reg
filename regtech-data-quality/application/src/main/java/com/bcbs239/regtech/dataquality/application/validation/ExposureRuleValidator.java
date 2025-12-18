package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;

import java.util.List;
import java.util.function.Function;

/**
 * Port for exposure validation functionality.
 * Defines the contract for validating exposure records.
 */
public interface ExposureRuleValidator {

    /**
     * Optional hook to prefetch batch-scoped data before validation begins.
     *
     * <p>Default implementation is a no-op.</p>
     */
    default void prefetchForBatch(List<ExposureRecord> exposures) {
        // no-op
    }

    /**
     * Optional hook to clear any batch-scoped caches after validation completes.
     *
     * <p>Default implementation is a no-op.</p>
     */
    default void onBatchComplete() {
        // no-op
    }

    /**
     * Validates a single exposure record without persisting results.
     *
     * @param exposure The exposure record to validate
     * @return ValidationResults containing errors, violations, logs, and stats
     */
    ValidationResults validateNoPersist(ExposureRecord exposure);

    /**
     * Optional hook to prepare a batch-scoped validator.
     *
     * <p>Default implementation performs no preparation and simply delegates to
     * {@link #validateNoPersist(ExposureRecord)}. Implementations may override to pre-load
     * shared state (e.g., enabled rules) once per batch and return a thread-safe function.
     */
    default Function<ExposureRecord, ValidationResults> prepareForBatch() {
        return this::validateNoPersist;
    }
}