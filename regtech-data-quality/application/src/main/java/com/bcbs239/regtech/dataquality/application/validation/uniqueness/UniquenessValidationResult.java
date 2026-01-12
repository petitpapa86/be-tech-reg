package com.bcbs239.regtech.dataquality.application.validation.uniqueness;

import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import lombok.Builder;

import java.util.List;

/**
 * Result of uniqueness validation containing check results and all duplicate exposure errors.
 * Each check result summarizes duplicates found, and the errors list contains
 * ALL individual exposures that are duplicates (not just summaries).
 */
@Builder
public record UniquenessValidationResult(
    UniquenessCheckResult exposureIdCheck,
    UniquenessCheckResult instrumentIdCheck,
    UniquenessCheckResult contentHashCheck,
    List<ValidationError> errors,
    int totalExposures,
    int uniqueExposures,
    double score
) {
    /**
     * Creates an empty result for zero exposures.
     */
    public static UniquenessValidationResult empty() {
        return builder()
            .exposureIdCheck(UniquenessCheckResult.empty(UniquenessCheckType.EXPOSURE_ID_UNIQUENESS))
            .instrumentIdCheck(UniquenessCheckResult.empty(UniquenessCheckType.INSTRUMENT_ID_UNIQUENESS))
            .contentHashCheck(UniquenessCheckResult.empty(UniquenessCheckType.CONTENT_DUPLICATE))
            .errors(List.of())
            .totalExposures(0)
            .uniqueExposures(0)
            .score(100.0)
            .build();
    }
    
    /**
     * Checks if all uniqueness checks passed.
     */
    public boolean allChecksPassed() {
        return exposureIdCheck.passed() && 
               instrumentIdCheck.passed() && 
               contentHashCheck.passed();
    }
    
    /**
     * Returns true if any duplicates were found.
     */
    public boolean hasDuplicates() {
        return !errors.isEmpty();
    }
    
    /**
     * Gets total number of duplicate exposures across all checks.
     */
    public int getTotalDuplicates() {
        return errors.size();
    }
}
