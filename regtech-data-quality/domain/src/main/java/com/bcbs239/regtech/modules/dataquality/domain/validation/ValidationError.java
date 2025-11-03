package com.bcbs239.regtech.modules.dataquality.domain.validation;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.modules.dataquality.domain.quality.QualityDimension;

/**
 * Value object representing a validation error with quality dimension classification.
 * Extends the core ErrorDetail with data quality specific information.
 */
public record ValidationError(
    String code,
    String message,
    String fieldName,
    QualityDimension dimension,
    String exposureId,
    ErrorSeverity severity
) {
    
    /**
     * Severity levels for validation errors
     */
    public enum ErrorSeverity {
        CRITICAL,   // Must be fixed for compliance
        HIGH,       // Should be fixed for good quality
        MEDIUM,     // May impact quality scores
        LOW         // Minor quality issues
    }
    
    /**
     * Creates a ValidationError from an ErrorDetail and quality dimension
     */
    public static ValidationError fromErrorDetail(ErrorDetail errorDetail, QualityDimension dimension) {
        return new ValidationError(
            errorDetail.getCode(),
            errorDetail.getMessage(),
            extractFieldName(errorDetail),
            dimension,
            null, // exposureId will be set by the validation engine
            determineSeverity(errorDetail.getCode())
        );
    }
    
    /**
     * Creates a ValidationError with exposure ID context
     */
    public static ValidationError fromErrorDetail(ErrorDetail errorDetail, QualityDimension dimension, String exposureId) {
        return new ValidationError(
            errorDetail.getCode(),
            errorDetail.getMessage(),
            extractFieldName(errorDetail),
            dimension,
            exposureId,
            determineSeverity(errorDetail.getCode())
        );
    }
    
    /**
     * Creates a ValidationError for a specific exposure and field
     */
    public static ValidationError of(String code, String message, String fieldName, 
                                   QualityDimension dimension, String exposureId) {
        return new ValidationError(
            code,
            message,
            fieldName,
            dimension,
            exposureId,
            determineSeverity(code)
        );
    }
    
    /**
     * Creates a ValidationError for batch-level issues
     */
    public static ValidationError batchError(String code, String message, QualityDimension dimension) {
        return new ValidationError(
            code,
            message,
            null, // No specific field for batch errors
            dimension,
            null, // No specific exposure for batch errors
            determineSeverity(code)
        );
    }
    
    /**
     * Converts this ValidationError to a core ErrorDetail
     */
    public ErrorDetail toErrorDetail() {
        return ErrorDetail.of(code, message, fieldName);
    }
    
    /**
     * Checks if this is a batch-level error (no specific exposure)
     */
    public boolean isBatchError() {
        return exposureId == null;
    }
    
    /**
     * Checks if this is an exposure-level error
     */
    public boolean isExposureError() {
        return exposureId != null;
    }
    
    /**
     * Gets a display-friendly description including dimension and severity
     */
    public String getDisplayDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(dimension.getDisplayName()).append("] ");
        sb.append("[").append(severity).append("] ");
        sb.append(message);
        
        if (fieldName != null) {
            sb.append(" (Field: ").append(fieldName).append(")");
        }
        
        if (exposureId != null) {
            sb.append(" (Exposure: ").append(exposureId).append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Extracts field name from error detail message or code
     */
    private static String extractFieldName(ErrorDetail errorDetail) {
        // Try to extract field name from the message key or details
        if (errorDetail.getMessageKey() != null) {
            return errorDetail.getMessageKey();
        }
        
        // Extract from error code if it follows pattern like "COMPLETENESS_AMOUNT_MISSING"
        String code = errorDetail.getCode();
        if (code != null && code.contains("_")) {
            String[] parts = code.split("_");
            if (parts.length >= 3) {
                return parts[1].toLowerCase(); // Extract field name part
            }
        }
        
        return null;
    }
    
    /**
     * Determines error severity based on error code patterns
     */
    private static ErrorSeverity determineSeverity(String code) {
        if (code == null) {
            return ErrorSeverity.MEDIUM;
        }
        
        // Critical errors - missing required fields, invalid formats
        if (code.contains("_MISSING") || code.contains("_REQUIRED") || 
            code.contains("_INVALID_FORMAT") || code.contains("_DUPLICATE")) {
            return ErrorSeverity.CRITICAL;
        }
        
        // High severity - accuracy and consistency issues
        if (code.contains("ACCURACY_") || code.contains("CONSISTENCY_")) {
            return ErrorSeverity.HIGH;
        }
        
        // Medium severity - timeliness and validity issues
        if (code.contains("TIMELINESS_") || code.contains("VALIDITY_")) {
            return ErrorSeverity.MEDIUM;
        }
        
        // Low severity - minor completeness issues
        if (code.contains("COMPLETENESS_")) {
            return ErrorSeverity.LOW;
        }
        
        return ErrorSeverity.MEDIUM;
    }
}