package com.bcbs239.regtech.dataquality.domain.specifications;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.specifications.Specification;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Specifications for Uniqueness quality dimension validation.
 * 
 * Uniqueness ensures that no duplicate records or identifiers exist
 * within a batch of exposure data, maintaining data integrity and
 * preventing double-counting in regulatory reporting.
 * 
 * @deprecated This class is deprecated and will be removed in a future release.
 *             All validation logic has been migrated to the database-driven Rules Engine.
 *             Use {@link com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService}
 *             for configurable, database-driven validation instead.
 *             See the Rules Engine Configuration Guide for migration details.
 */
@Deprecated(since = "2.0", forRemoval = true)
public class UniquenessSpecifications {

    /**
     * Validates that all exposure IDs within a batch are unique.
     * Duplicate exposure IDs indicate data quality issues and can lead
     * to incorrect risk calculations.
     * 
     * @return Specification that validates unique exposure IDs
     */
    public static Specification<List<ExposureRecord>> hasUniqueExposureIds() {
        return exposures -> {
            Set<String> seenIds = new HashSet<>();
            List<String> duplicates = new ArrayList<>();
            
            for (ExposureRecord exposure : exposures) {
                if (exposure.exposureId() != null && !exposure.exposureId().trim().isEmpty()) {
                    String exposureId = exposure.exposureId().trim();
                    if (!seenIds.add(exposureId)) {
                        duplicates.add(exposureId);
                    }
                }
            }
            
            if (!duplicates.isEmpty()) {
                // Remove duplicates from the list and limit to first 10 for readability
                List<String> uniqueDuplicates = duplicates.stream()
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
                
                String duplicateList = String.join(", ", uniqueDuplicates);
                if (duplicates.stream().distinct().count() > 10) {
                    duplicateList += " (and " + (duplicates.stream().distinct().count() - 10) + " more)";
                }
                
                return Result.failure(ErrorDetail.of("UNIQUENESS_DUPLICATE_EXPOSURE_IDS", 
                    ErrorType.VALIDATION_ERROR,
                    "Duplicate exposure IDs found: " + duplicateList, "uniqueness.duplicate.exposure.ids"));
            }
            
            return Result.success();
        };
    }

    /**
     * Validates that counterparty-exposure pairs are unique within a batch.
     * The same counterparty should not have multiple exposures with the same
     * exposure ID, as this indicates data duplication or processing errors.
     * 
     * @return Specification that validates unique counterparty-exposure relationships
     */
    public static Specification<List<ExposureRecord>> hasUniqueCounterpartyExposurePairs() {
        return exposures -> {
            Set<String> seenPairs = new HashSet<>();
            List<String> duplicatePairs = new ArrayList<>();
            
            for (ExposureRecord exposure : exposures) {
                if (exposure.counterpartyId() != null && !exposure.counterpartyId().trim().isEmpty() &&
                    exposure.exposureId() != null && !exposure.exposureId().trim().isEmpty()) {
                    
                    String pair = exposure.counterpartyId().trim() + ":" + exposure.exposureId().trim();
                    if (!seenPairs.add(pair)) {
                        duplicatePairs.add(pair);
                    }
                }
            }
            
            if (!duplicatePairs.isEmpty()) {
                // Remove duplicates from the list and limit to first 10 for readability
                List<String> uniqueDuplicatePairs = duplicatePairs.stream()
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
                
                String duplicateList = String.join(", ", uniqueDuplicatePairs);
                if (duplicatePairs.stream().distinct().count() > 10) {
                    duplicateList += " (and " + (duplicatePairs.stream().distinct().count() - 10) + " more)";
                }
                
                return Result.failure(ErrorDetail.of("UNIQUENESS_DUPLICATE_COUNTERPARTY_EXPOSURE", 
                    ErrorType.VALIDATION_ERROR,
                    "Duplicate counterparty-exposure pairs found: " + duplicateList, "uniqueness.duplicate.counterparty.exposure"));
            }
            
            return Result.success();
        };
    }

    /**
     * Validates that reference numbers are unique within a batch.
     * Reference numbers are used for tracking and audit purposes,
     * and duplicates can cause confusion in regulatory reporting.
     * 
     * @return Specification that validates unique reference numbers
     */
    public static Specification<List<ExposureRecord>> hasUniqueReferenceNumbers() {
        return exposures -> {
            Set<String> seenReferences = new HashSet<>();
            List<String> duplicateReferences = new ArrayList<>();
            
            for (ExposureRecord exposure : exposures) {
                if (exposure.referenceNumber() != null && !exposure.referenceNumber().trim().isEmpty()) {
                    String referenceNumber = exposure.referenceNumber().trim();
                    if (!seenReferences.add(referenceNumber)) {
                        duplicateReferences.add(referenceNumber);
                    }
                }
            }
            
            if (!duplicateReferences.isEmpty()) {
                // Remove duplicates from the list and limit to first 10 for readability
                List<String> uniqueDuplicateReferences = duplicateReferences.stream()
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
                
                String duplicateList = String.join(", ", uniqueDuplicateReferences);
                if (duplicateReferences.stream().distinct().count() > 10) {
                    duplicateList += " (and " + (duplicateReferences.stream().distinct().count() - 10) + " more)";
                }
                
                return Result.failure(ErrorDetail.of("UNIQUENESS_DUPLICATE_REFERENCE_NUMBERS", 
                    ErrorType.VALIDATION_ERROR,
                    "Duplicate reference numbers found: " + duplicateList, "uniqueness.duplicate.reference.numbers"));
            }
            
            return Result.success();
        };
    }
}

