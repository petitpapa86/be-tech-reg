package com.bcbs239.regtech.core.domain.saga;

import com.bcbs239.regtech.core.domain.shared.ErrorType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SagaError {
    String message;
    ErrorType errorType;
    boolean recoverable;
    boolean semiRecoverable;


    public SagaError withRetry() {
        return new SagaError(message + " (retry)", errorType, recoverable, semiRecoverable);
    }
}

