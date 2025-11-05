package com.bcbs239.regtech.billing.application.policies.createstripecustomer;

import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaCommand;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Command to create a Stripe customer
 */
@Getter
public class CreateStripeCustomerCommand extends SagaCommand {
    private final String userId;
    private final String email;
    private final String name;
    private final String paymentMethodId;

    public CreateStripeCustomerCommand(SagaId sagaId, String userId, String email, String name, String paymentMethodId) {
        super(sagaId, "CreateStripeCustomerCommand", 
              Map.of("userId", userId, "email", email, "name", name, 
                     "paymentMethodId", paymentMethodId != null ? paymentMethodId : ""), 
              Instant.now());
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.paymentMethodId = paymentMethodId;
    }

    public static CreateStripeCustomerCommand create(SagaId sagaId, String userId, String email, String name) {
        return new CreateStripeCustomerCommand(sagaId, userId, email, name, null);
    }
}