package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.CorrelationContext;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UserRegisteredEvent extends DomainEvent {

    private final String userId;
    private final String email;
    private final String bankId;
    private final String paymentMethodId;

    public UserRegisteredEvent(String userId, String email, String bankId, String paymentMethodId) {
        super(CorrelationContext.correlationId(), CorrelationContext.causationId());
        this.userId = userId;
        this.email = email;
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
    }

    @Override
    public String eventType() {
        return "UserRegisteredEvent";
    }
}
}

