package com.bcbs239.regtech.core.saga;

public class SagaCreationException extends RuntimeException {
    public SagaCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
