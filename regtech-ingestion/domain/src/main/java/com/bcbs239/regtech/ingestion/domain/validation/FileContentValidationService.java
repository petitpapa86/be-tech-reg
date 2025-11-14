package com.bcbs239.regtech.ingestion.domain.validation;

import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * Domain service interface for validating file content.
 * This represents the domain concern of ensuring uploaded files meet
 * structural and business rule requirements.
 * 
 * Note: This interface is generic over the parsed data type to avoid
 * coupling the domain to application layer types.
 */
public interface FileContentValidationService {
    
    /**
     * Validate the structure of parsed file data.
     * Checks that required fields are present and properly formatted.
     */
    <T> Result<ValidationResult> validateStructure(T parsedData);
    
    /**
     * Validate business rules for parsed file data.
     * Checks domain-specific constraints and data quality rules.
     */
    <T> Result<ValidationResult> validateBusinessRules(T parsedData);
    
    /**
     * Validation result containing metrics about the validated content.
     */
    record ValidationResult(int totalExposures) {}
}
