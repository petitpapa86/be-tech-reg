package com.bcbs239.regtech.iam.application.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Getter;

/**
 * Integration event published by the IAM bounded context when a user registers.
 * This class adapts the IAM domain event into a core IntegrationEvent that
 * other bounded contexts can consume.
 */
@Getter
public class UserRegisteredIntegrationEvent extends IntegrationEvent {

    private final String userId;
    private final String email;
    private final String bankId;
    private final String paymentMethodId;

    public UserRegisteredIntegrationEvent(String correlationId,
                                          Maybe<String> causationId,
                                          String userId,
                                          String email,
                                          String bankId,
                                          String paymentMethodId) {
        super(correlationId, causationId, "UserRegisteredIntegrationEvent");
        this.userId = userId;
        this.email = email;
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
    }

    @Override
    public String toString() {
        return String.format("UserRegisteredIntegrationEvent{userId='%s', email='%s', bankId='%s', paymentMethodId='%s'}",
                userId, email, bankId, paymentMethodId);
    }

    @Override
    public String eventType() {
        return "UserRegisteredIntegrationEvent";
    }
}
