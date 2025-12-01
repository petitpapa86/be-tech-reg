package com.bcbs239.regtech.riskcalculation.application.shared;

/**
 * Exception thrown when a risk report fails validation
 */
public class InvalidReportException extends RuntimeException {
    
    public InvalidReportException(String message) {
        super(message);
    }
    
    public InvalidReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
