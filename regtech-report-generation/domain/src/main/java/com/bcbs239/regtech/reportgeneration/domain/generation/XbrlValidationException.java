package com.bcbs239.regtech.reportgeneration.domain.generation;

/**
 * Exception thrown when XBRL validation process fails
 * 
 * This exception indicates a failure in the validation process itself
 * (not validation errors in the XBRL document), such as schema loading
 * errors or XML parsing issues.
 * 
 * Note: This is different from validation errors in the XBRL document,
 * which are returned as part of ValidationResult.
 */
public class XbrlValidationException extends RuntimeException {
    
    /**
     * Create an XBRL validation exception with a message
     */
    public XbrlValidationException(String message) {
        super(message);
    }
    
    /**
     * Create an XBRL validation exception with a message and cause
     */
    public XbrlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Create an XBRL validation exception with a cause
     */
    public XbrlValidationException(Throwable cause) {
        super(cause);
    }
}
