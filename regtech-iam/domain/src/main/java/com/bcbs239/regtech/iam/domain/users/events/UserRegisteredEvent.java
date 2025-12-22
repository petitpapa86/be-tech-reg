package com.bcbs239.regtech.iam.domain.users.events;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRegisteredEvent extends DomainEvent {

    private final String userId;
    private final String email;
    private final String bankId;
    private final String paymentMethodId;

    @JsonCreator
    public UserRegisteredEvent(@JsonProperty("userId") String userId,
                               @JsonProperty("email") String email,
                               @JsonProperty("bankId") String bankId,
                               @JsonProperty("paymentMethodId") String paymentMethodId) {

        String correlationId = CorrelationContext.correlationId();
        super(correlationId);
        this.userId = userId;
        this.email = email;
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
    }

    public static UserRegisteredEvent create(String userId, String email, String bankId, String paymentMethodId) {
        return new UserRegisteredEvent(userId, email, bankId, paymentMethodId);
    }
}


