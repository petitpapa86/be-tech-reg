package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Integration event published when a user registers.
 * This is part of the Published Language - a shared contract between bounded contexts.
 * All modules can depend on this event definition from regtech-core.
 */
@Getter
public class UserRegisteredIntegrationEvent extends IntegrationEvent {

    private final String userId;
    private final String email;
    private final String bankId;
    private final String paymentMethodId;

    @JsonCreator
    public UserRegisteredIntegrationEvent(
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("causationId") Maybe<String> causationId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("userId") String userId,
            @JsonProperty("email") String email,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("paymentMethodId") String paymentMethodId) {
        super(correlationId, causationId, "UserRegisteredIntegrationEvent");
        this.userId = userId;
        this.email = email;
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
    }

    // Convenience constructor without eventId for creation
    public UserRegisteredIntegrationEvent(
            String correlationId,
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
