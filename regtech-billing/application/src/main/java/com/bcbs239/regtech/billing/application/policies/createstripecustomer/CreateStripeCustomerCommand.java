package com.bcbs239.regtech.billing.application.policies.createstripecustomer;


import com.bcbs239.regtech.billing.domain.payments.PaymentMethodId;
import com.bcbs239.regtech.core.domain.saga.SagaCommand;
import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.core.domain.shared.valueobjects.UserId;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Command to create a Stripe customer with validated value objects
 */
@Getter
public class CreateStripeCustomerCommand extends SagaCommand {
    private final UserId userId;
    private final Email email;
    private final String name;
    private final PaymentMethodId paymentMethodId;

    public CreateStripeCustomerCommand(SagaId sagaId, UserId userId, Email email, String name, PaymentMethodId paymentMethodId) {
        super(sagaId, "CreateStripeCustomerCommand", 
              Map.of("userId", Objects.requireNonNull(userId, "UserId cannot be null").getValue(), 
                     "email", Objects.requireNonNull(email, "Email cannot be null").getValue(), 
                     "name", name != null ? name : "", 
                     "paymentMethodId", paymentMethodId != null ? paymentMethodId.getValue() : ""), 
              Instant.now());
        this.userId = Objects.requireNonNull(userId, "UserId cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.name = name;
        this.paymentMethodId = paymentMethodId;
    }

    public static CreateStripeCustomerCommand create(SagaId sagaId, UserId userId, Email email, String name) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        return new CreateStripeCustomerCommand(sagaId, userId, email, name, null);
    }
}

