package com.bcbs239.regtech.modules.dataquality.domain.report;

import com.bcbs239.regtech.modules.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.modules.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BatchId;

import java.util.List;
import java.util.Objects;

/**
 * Value object representing a summary of validation errors for a specific rule and dimension.
 * This is used to store aggregated error information in the database to avoid storing
 * individual error records for each exposure.
 */
public record QualityErrorSummary(
    BatchId batchId,
    String ruleCode,
    QualityDimension dimension,
    ValidationError.ErrorSeverity severity,
    String errorMessage,
    String fieldName,
    int errorCount,
    List<String> affectedExposureIds // Limited to 10 examples to prevent database bloat
) {
    
    public QualityErrorSummary {
        Objects.requireNonNull(batchId, "BatchId cannot be null");
        Objects.requireNonNull(ruleCode, "Rule code cannot be null");
        Objects.requireNonNull(dimension, "Quality dimension cannot be null");
        Objects.requireNonNull(severity, "Error severity cannot be null");
        Objects.requireNonNull(errorMessage, "Error message cannot be null");
        Objects.requireNonNull(affectedExposureIds, "Affected exposure IDs list cannot be null");
        
        if (ruleCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule code cannot be empty");
        }
        if (errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be empty");
        }
        if (errorCount < 0) {
            throw new IllegalArgumentException("Error count cannot be negative");
        }
        if (affectedExposureIds.size() > 10) {
            throw new IllegalArgumentException("Affected exposure IDs list cannot exceed 10 examples");
        }
    }
    
    /**
     * Create a QualityErrorSummary from a list of validation errors with the same rule code.
     */
    public static QualityErrorSummary fromValidationErrors(
        BatchId batchId,
        String ruleCode,
        List<ValidationError> errors
    ) {
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("Cannot create error summary from empty error list");
        }
        
        ValidationError firstError = errors.get(0);
        
        // Collect up to 10 exposure IDs as examples
        List<String> exposureIds = errors.stream()
            .map(ValidationError::exposureId)
            .filter(Objects::nonNull)
            .distinct()
            .limit(10)
            .toList();
        
        return new QualityErrorSummary(
            batchId,
            ruleCode,
            firstError.dimension(),
            firstError.severity(),
            firstError.message(),
            firstError.fieldName(),
            errors.size(),
            exposureIds
        );
    }
    
    /**
     * Get a display-friendly description of this error summary.
     */
    public String getDisplayDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(dimension.getDisplayName()).append("] ");
        sb.append("[").append(severity).append("] ");
        sb.append(errorMessage);
        sb.append(" (").append(errorCount).append(" occurrences)");
        
        if (fieldName != null) {
            sb.append(" - Field: ").append(fieldName);
        }
        
        return sb.toString();
    }
    
    /**
     * Check if this error summary represents batch-level errors.
     */
    public boolean isBatchLevelError() {
        return affectedExposureIds.isEmpty();
    }
    
    /**
     * Check if this error summary represents exposure-level errors.
     */
    public boolean isExposureLevelError() {
        return !affectedExposureIds.isEmpty();
    }
}