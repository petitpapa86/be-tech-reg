package com.bcbs239.regtech.core.application.saga;

public class SagaCreationException extends RuntimeException {
    public SagaCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
