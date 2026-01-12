package com.bcbs239.regtech.dataquality.application.validation.uniqueness;

import lombok.Builder;

import java.util.List;

/**
 * Result of a single uniqueness check (exposureId, instrumentId, or content hash).
 * Provides summary of duplicates found and list of duplicate IDs/hashes.
 */
@Builder
public record UniquenessCheckResult(
    UniquenessCheckType checkType,
    boolean passed,
    String summary,
    List<String> duplicateIds,  // IDs or hashes of duplicates
    int duplicateCount           // Total number of duplicate exposures
) {
    /**
     * Creates an empty result (no duplicates found).
     */
    public static UniquenessCheckResult empty(UniquenessCheckType checkType) {
        return builder()
            .checkType(checkType)
            .passed(true)
            .summary("No duplicates found - ✓ Univoco")
            .duplicateIds(List.of())
            .duplicateCount(0)
            .build();
    }
}
