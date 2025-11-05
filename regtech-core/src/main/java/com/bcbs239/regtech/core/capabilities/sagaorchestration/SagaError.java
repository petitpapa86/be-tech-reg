package com.bcbs239.regtech.core.capabilities.sagaorchestration;

import com.bcbs239.regtech.core.capabilities.sharedinfrastructure.ErrorType;
import lombok.Value;

@Value
public class SagaError {
    String message;
    ErrorType errorType;
    boolean recoverable;
    boolean semiRecoverable;

    public SagaError withRetry() {
        return new SagaError(message + " (retry)", errorType, recoverable, semiRecoverable);
    }
}
