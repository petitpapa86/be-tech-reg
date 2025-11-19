package com.bcbs239.regtech.reportgeneration.domain.generation;

/**
 * Exception thrown when XBRL report generation fails
 * 
 * This exception indicates a failure in the XBRL generation process,
 * such as XML construction errors, missing data, or namespace issues.
 */
public class XbrlGenerationException extends RuntimeException {
    
    /**
     * Create an XBRL generation exception with a message
     */
    public XbrlGenerationException(String message) {
        super(message);
    }
    
    /**
     * Create an XBRL generation exception with a message and cause
     */
    public XbrlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Create an XBRL generation exception with a cause
     */
    public XbrlGenerationException(Throwable cause) {
        super(cause);
    }
}
