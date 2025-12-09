package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

/**
 * XBRL Validation Status enumeration
 * Represents the validation state of an XBRL report against EBA schema
 */
public enum XbrlValidationStatus {
    /**
     * XBRL document passed validation against EBA XSD schema
     */
    VALID,
    
    /**
     * XBRL document failed validation with errors
     */
    INVALID,
    
    /**
     * XBRL validation was not performed or skipped
     */
    NOT_VALIDATED
}
