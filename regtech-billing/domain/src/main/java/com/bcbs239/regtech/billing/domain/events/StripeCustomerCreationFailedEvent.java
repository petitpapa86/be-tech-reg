package com.bcbs239.regtech.billing.domain.events;

import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaMessage;

import java.time.Instant;

public class StripeCustomerCreationFailedEvent extends SagaMessage {

    private final String errorMessage;

    public StripeCustomerCreationFailedEvent(SagaId sagaId, String errorMessage) {
        super("StripeCustomerCreationFailedEvent", Instant.now(), sagaId);
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}