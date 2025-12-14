package com.bcbs239.regtech.riskcalculation.domain.calculation;

/**
 * Exception thrown when batch data parsing fails.
 */
public class BatchDataParsingException extends RuntimeException {
    public BatchDataParsingException(String message) {
        super(message);
    }

    public BatchDataParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
