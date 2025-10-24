package com.bcbs239.regtech.billing.application.policies.createstripecustomer;

import com.bcbs239.regtech.core.saga.SagaCommand;
import com.bcbs239.regtech.core.saga.SagaId;

import java.time.Instant;
import java.util.Map;

public class CreateStripeCustomerCommand extends SagaCommand {

    public CreateStripeCustomerCommand(SagaId sagaId, String userEmail, String userName, String paymentMethodId) {
        super(sagaId, "CreateStripeCustomerCommand", Map.of(
            "userEmail", userEmail,
            "userName", userName,
            "paymentMethodId", paymentMethodId
        ), Instant.now());
    }

    public String getUserEmail() {
        return (String) payload().get("userEmail");
    }

    public String getUserName() {
        return (String) payload().get("userName");
    }

    public String getPaymentMethodId() {
        return (String) payload().get("paymentMethodId");
    }
}