package com.bcbs239.regtech.core.domain.events;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Integration event for cross-module communication.
 * Separate from the domain event to avoid cycles and maintain clear boundaries.
 */
@Getter
@NoArgsConstructor
public class UserRegisteredIntegrationEvent extends IntegrationEvent {
    private String userId;
    private String email;
    private String name;
    private String bankId;
    private String paymentMethodId;
    private String phone;
    private UserRegisteredEvent.AddressInfo address;

    public UserRegisteredIntegrationEvent(
            String userId,
            String email,
            String name,
            String bankId,
            String paymentMethodId,
            String phone,
            UserRegisteredEvent.AddressInfo address) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
        this.phone = phone;
        this.address = address;
    }

}
