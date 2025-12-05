package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Integration event published when a billing account is successfully activated.
 * This event is consumed by the IAM module to update user registration status.
 */
@Getter
public class BillingAccountActivatedEvent extends IntegrationEvent {

    private final String userId;

    @JsonCreator
    public BillingAccountActivatedEvent(
            @JsonProperty("userId") String userId,
            @JsonProperty("correlationId") String correlationId) {
        super(correlationId, Maybe.none(), "BillingAccountActivated");
        this.userId = userId;
    }

    @Override
    public String eventType() {
        return "BillingAccountActivated";
    }
}