package com.bcbs239.regtech.reportgeneration.domain.generation;

/**
 * Exception thrown when HTML report generation fails
 * 
 * This exception indicates a failure in the HTML generation process,
 * such as template rendering errors, missing data, or I/O issues.
 */
public class HtmlGenerationException extends RuntimeException {
    
    /**
     * Create an HTML generation exception with a message
     */
    public HtmlGenerationException(String message) {
        super(message);
    }
    
    /**
     * Create an HTML generation exception with a message and cause
     */
    public HtmlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Create an HTML generation exception with a cause
     */
    public HtmlGenerationException(Throwable cause) {
        super(cause);
    }
}
