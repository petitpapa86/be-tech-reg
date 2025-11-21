package com.bcbs239.regtech.reportgeneration.application.aggregation;

/**
 * Exception thrown when data aggregation fails
 */
public class DataAggregationException extends RuntimeException {
    
    public DataAggregationException(String message) {
        super(message);
    }
    
    public DataAggregationException(String message, Throwable cause) {
        super(message, cause);
    }
}
