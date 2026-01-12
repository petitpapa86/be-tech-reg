package com.bcbs239.regtech.dataquality.application.validation.uniqueness;

import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service for executing uniqueness checks on exposure records.
 * Detects duplicate exposures based on three different criteria:
 * 
 * DIMENSIONE: UNICITÀ (UNIQUENESS)
 * Definizione: Non esistono record duplicati nei dati
 * 
 * Three checks performed:
 * 1. ExposureId Univocità - No duplicate exposure IDs
 * 2. ReferenceNumber Univocità - No duplicate reference numbers (instrument references)
 * 3. Esposizioni Duplicate - No content-based duplicates (hash comparison)
 */
@Component
public class UniquenessValidator {
    
    /**
     * Validates uniqueness across all exposure records in the batch.
     * For each duplicate found, ALL occurrences (not just the duplicates after the first)
     * are added to the errors list.
     * 
     * @param exposures List of all exposure records in the batch
     * @return UniquenessValidationResult with all duplicate exposure errors
     */
    public UniquenessValidationResult validate(List<ExposureRecord> exposures) {
        if (exposures == null || exposures.isEmpty()) {
            return UniquenessValidationResult.empty();
        }
        
        List<ValidationError> errors = new ArrayList<>();
        UniquenessCheckResult exposureIdCheck = checkExposureIdUniqueness(exposures, errors);
        UniquenessCheckResult referenceNumberCheck = checkReferenceNumberUniqueness(exposures, errors);
        UniquenessCheckResult contentHashCheck = checkContentDuplicates(exposures, errors);
        
        // Calculate overall uniqueness score
        // Score = (exposures without errors / total exposures) * 100
        long exposuresWithErrors = errors.stream()
            .map(ValidationError::exposureId)
            .distinct()
            .count();
        
        double score = exposures.isEmpty() ? 100.0 : 
            ((exposures.size() - exposuresWithErrors) * 100.0) / exposures.size();
        
        return UniquenessValidationResult.builder()
            .exposureIdCheck(exposureIdCheck)
            .instrumentIdCheck(referenceNumberCheck)
            .contentHashCheck(contentHashCheck)
            .errors(errors)
            .totalExposures(exposures.size())
            .uniqueExposures(exposures.size() - (int)exposuresWithErrors)
            .score(score)
            .build();
    }
    
    /**
     * Check 1: ExposureId Univocità
     * Verifies that all exposure IDs are unique within the batch.
     * Adds ALL duplicate exposures to errors (not just subsequent occurrences).
     */
    private UniquenessCheckResult checkExposureIdUniqueness(
        List<ExposureRecord> exposures,
        List<ValidationError> errors
    ) {
        // Group exposures by exposureId
        Map<String, List<ExposureRecord>> exposuresById = exposures.stream()
            .filter(e -> e.exposureId() != null && !e.exposureId().isBlank())
            .collect(Collectors.groupingBy(ExposureRecord::exposureId));
        
        // Find duplicates (groups with more than 1 exposure)
        List<String> duplicateIds = new ArrayList<>();
        int duplicateCount = 0;
        
        for (Map.Entry<String, List<ExposureRecord>> entry : exposuresById.entrySet()) {
            List<ExposureRecord> group = entry.getValue();
            if (group.size() > 1) {
                String exposureId = entry.getKey();
                duplicateIds.add(exposureId);
                duplicateCount += group.size();
                
                // Add error for EACH exposure in the duplicate group
                for (ExposureRecord exposure : group) {
                    errors.add(new ValidationError(
                        "UNIQUENESS_EXPOSURE_ID_DUPLICATE",
                        String.format("Duplicate exposureId '%s' found (%d occurrences total)",
                            exposureId, group.size()),
                        "exposureId",
                        QualityDimension.UNIQUENESS,
                        exposure.exposureId(),
                        ValidationError.ErrorSeverity.CRITICAL
                    ));
                }
            }
        }
        
        boolean passed = duplicateIds.isEmpty();
        String summary = passed ? 
            String.format("All %d exposure IDs are unique - ✓ Univoco", exposuresById.size()) :
            String.format("Found %d duplicate exposure IDs (%d exposures affected) - ✗ FAIL", 
                duplicateIds.size(), duplicateCount);
        
        return UniquenessCheckResult.builder()
            .checkType(UniquenessCheckType.EXPOSURE_ID_UNIQUENESS)
            .passed(passed)
            .summary(summary)
            .duplicateIds(duplicateIds)
            .duplicateCount(duplicateCount)
            .build();
    }
    
    /**
     * Check 2: ReferenceNumber Univocità
     * Verifies that all reference numbers (instrument references) are unique within the batch.
     * Adds ALL duplicate exposures to errors.
     */
    private UniquenessCheckResult checkReferenceNumberUniqueness(
        List<ExposureRecord> exposures,
        List<ValidationError> errors
    ) {
        // Group exposures by referenceNumber (skip null/blank)
        Map<String, List<ExposureRecord>> exposuresByReference = exposures.stream()
            .filter(e -> e.referenceNumber() != null && !e.referenceNumber().isBlank())
            .collect(Collectors.groupingBy(ExposureRecord::referenceNumber));
        
        // Find duplicates
        List<String> duplicateIds = new ArrayList<>();
        int duplicateCount = 0;
        
        for (Map.Entry<String, List<ExposureRecord>> entry : exposuresByReference.entrySet()) {
            List<ExposureRecord> group = entry.getValue();
            if (group.size() > 1) {
                String referenceNumber = entry.getKey();
                duplicateIds.add(referenceNumber);
                duplicateCount += group.size();
                
                // Add error for EACH exposure with duplicate referenceNumber
                for (ExposureRecord exposure : group) {
                    errors.add(new ValidationError(
                        "UNIQUENESS_INSTRUMENT_ID_DUPLICATE",
                        String.format("Duplicate reference number '%s' found (%d occurrences total)",
                            referenceNumber, group.size()),
                        "referenceNumber",
                        QualityDimension.UNIQUENESS,
                        exposure.exposureId(),
                        ValidationError.ErrorSeverity.HIGH
                    ));
                }
            }
        }
        
        boolean passed = duplicateIds.isEmpty();
        String summary = passed ?
            String.format("All %d reference numbers are unique - ✓ Univoco", exposuresByReference.size()) :
            String.format("Found %d duplicate reference numbers (%d exposures affected) - ✗ FAIL",
                duplicateIds.size(), duplicateCount);
        
        return UniquenessCheckResult.builder()
            .checkType(UniquenessCheckType.INSTRUMENT_ID_UNIQUENESS)
            .passed(passed)
            .summary(summary)
            .duplicateIds(duplicateIds)
            .duplicateCount(duplicateCount)
            .build();
    }
    
    /**
     * Check 3: Esposizioni Duplicate (Content-Based)
     * Detects duplicate exposures based on content hash.
     * Ignores exposureId and referenceNumber fields when calculating hash.
     * Adds ALL duplicate exposures to errors.
     */
    private UniquenessCheckResult checkContentDuplicates(
        List<ExposureRecord> exposures,
        List<ValidationError> errors
    ) {
        // Create content hash for each exposure (excluding IDs)
        Map<String, List<ExposureRecord>> exposuresByHash = new HashMap<>();
        
        for (ExposureRecord exposure : exposures) {
            String contentHash = calculateContentHash(exposure);
            exposuresByHash.computeIfAbsent(contentHash, k -> new ArrayList<>())
                .add(exposure);
        }
        
        // Find duplicates
        List<String> duplicateHashes = new ArrayList<>();
        int duplicateCount = 0;
        
        for (Map.Entry<String, List<ExposureRecord>> entry : exposuresByHash.entrySet()) {
            List<ExposureRecord> group = entry.getValue();
            if (group.size() > 1) {
                String hash = entry.getKey();
                duplicateHashes.add(hash);
                duplicateCount += group.size();
                
                // Get first exposure for descriptive message
                ExposureRecord first = group.get(0);
                
                // Add error for EACH exposure with duplicate content
                for (ExposureRecord exposure : group) {
                    errors.add(new ValidationError(
                        "UNIQUENESS_CONTENT_DUPLICATE",
                        String.format("Duplicate exposure content found (hash: %s, %d identical exposures, amount: %s %s)",
                            hash.substring(0, Math.min(8, hash.length())), 
                            group.size(),
                            first.exposureAmount() != null ? first.exposureAmount().toPlainString() : "N/A",
                            first.currency() != null ? first.currency() : "N/A"),
                        null,  // Content-based, no single field
                        QualityDimension.UNIQUENESS,
                        exposure.exposureId(),
                        ValidationError.ErrorSeverity.HIGH
                    ));
                }
            }
        }
        
        boolean passed = duplicateHashes.isEmpty();
        String summary = passed ?
            String.format("All %d exposures have unique content - ✓ Univoco", exposures.size()) :
            String.format("Found %d content duplicates (%d exposures affected) - ✗ FAIL",
                duplicateHashes.size(), duplicateCount);
        
        return UniquenessCheckResult.builder()
            .checkType(UniquenessCheckType.CONTENT_DUPLICATE)
            .passed(passed)
            .summary(summary)
            .duplicateIds(duplicateHashes)
            .duplicateCount(duplicateCount)
            .build();
    }
    
    /**
     * Calculates a SHA-256 content hash for an exposure record.
     * Excludes exposureId and referenceNumber to detect semantic duplicates.
     * Includes all other significant fields in a deterministic order.
     */
    private String calculateContentHash(ExposureRecord exposure) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder content = new StringBuilder();
            
            // Include all significant fields except exposureId and referenceNumber
            // Order matters for consistent hashing
            content.append(exposure.counterpartyId() != null ? exposure.counterpartyId() : "");
            content.append("|");
            content.append(exposure.counterpartyLei() != null ? exposure.counterpartyLei() : "");
            content.append("|");
            content.append(exposure.sector() != null ? exposure.sector() : "");
            content.append("|");
            content.append(exposure.countryCode() != null ? exposure.countryCode() : "");
            content.append("|");
            content.append(exposure.exposureAmount() != null ? exposure.exposureAmount().toPlainString() : "");
            content.append("|");
            content.append(exposure.currency() != null ? exposure.currency() : "");
            content.append("|");
            content.append(exposure.reportingDate() != null ? exposure.reportingDate().toString() : "");
            content.append("|");
            content.append(exposure.valuationDate() != null ? exposure.valuationDate().toString() : "");
            content.append("|");
            content.append(exposure.maturityDate() != null ? exposure.maturityDate().toString() : "");
            content.append("|");
            content.append(exposure.riskWeight() != null ? exposure.riskWeight().toPlainString() : "");
            content.append("|");
            content.append(exposure.productType() != null ? exposure.productType() : "");
            content.append("|");
            content.append(exposure.counterpartyType() != null ? exposure.counterpartyType() : "");
            content.append("|");
            content.append(exposure.internalRating() != null ? exposure.internalRating() : "");
            content.append("|");
            content.append(exposure.riskCategory() != null ? exposure.riskCategory() : "");
            
            byte[] hash = digest.digest(content.toString().getBytes());
            return bytesToHex(hash);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Converts byte array to hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
