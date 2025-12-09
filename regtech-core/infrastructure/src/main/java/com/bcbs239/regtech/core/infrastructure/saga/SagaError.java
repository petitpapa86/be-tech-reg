package com.bcbs239.regtech.core.infrastructure.saga;


import com.bcbs239.regtech.core.domain.shared.ErrorType;

public record SagaError(String message, ErrorType errorType, boolean recoverable, boolean semiRecoverable) {
    public SagaError withRetry() {
        return new SagaError(message + " (retry)", errorType, recoverable, semiRecoverable);
    }

}

