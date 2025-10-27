package com.bcbs239.regtech.core.saga;

import java.time.Instant;
import java.util.Map;

public class RequestNewPaymentMethodCommand extends SagaCommand {
    public RequestNewPaymentMethodCommand(SagaId sagaId) {
        super(sagaId, "REQUEST_NEW_PAYMENT_METHOD", Map.of(), Instant.now());
    }
}