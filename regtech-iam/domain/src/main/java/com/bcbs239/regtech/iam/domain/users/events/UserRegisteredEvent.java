package com.bcbs239.regtech.iam.domain.users.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.context.CorrelationContext;
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
                               @JsonProperty("paymentMethodId") String paymentMethodId,
                               @JsonProperty("causationId") String causationId) {

        String correlationId = CorrelationContext.correlationId();
        Maybe<String> causationMaybe;
        if (causationId != null) {
            causationMaybe = Maybe.some(causationId);
        } else {
            causationMaybe = Maybe.none();
        }
        super(correlationId, causationMaybe, "UserRegisteredEvent");
        this.userId = userId;
        this.email = email;
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
    }

    public static UserRegisteredEvent create(String userId, String email, String bankId, String paymentMethodId, String causationId) {
        return new UserRegisteredEvent(userId, email, bankId, paymentMethodId, causationId);
    }

    @Override
    public String eventType() {
        return "UserRegisteredEvent";
    }
}


