package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.core.domain.shared.Result;
import org.w3c.dom.Document;

/**
 * Domain service interface for validating XBRL documents
 * 
 * Validates XBRL-XML documents against EBA XSD schema to ensure compliance
 * with regulatory technical requirements before submission.
 * 
 * This is a domain service interface that will be implemented in the
 * infrastructure layer using XML schema validation.
 * 
 * Requirements: 8.1
 */
public interface XbrlValidator {
    
    /**
     * Validate an XBRL document against EBA XSD schema
     * 
     * Performs validation and returns detailed results including:
     * - Validation success/failure status
     * - Detailed error messages with line numbers (if validation fails)
     * - Automatic correction suggestions (trim whitespace, round decimals)
     * 
     * @param xbrlDocument the XBRL document to validate
     * @return a Result containing the validation result (which itself contains validation status), or a system error
     */
    Result<ValidationResult> validate(Document xbrlDocument);
}
