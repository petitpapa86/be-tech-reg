package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.domain.events.BaseEvent;
import lombok.Getter;

@Getter
public class UserRegisteredEvent extends BaseEvent {

    private final String userId;
    private final String email;
    private final String bankId;
    private final String paymentMethodId;

    public UserRegisteredEvent(String userId, String email, String bankId, String paymentMethodId) {
        this.userId = userId;
        this.email = email;
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
    }
}
