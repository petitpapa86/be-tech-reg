package com.bcbs239.regtech.core.events;

import java.time.Instant;

/**
 * Cross-module event published when a user is registered and needs payment processing.
 * This event is consumed by the billing context to initiate payment processing.
 */
public class UserRegisteredEvent extends BaseEvent {
    private final String userId;
    private final String email;
    private final String name;
    private final String bankId;
    private final String paymentMethodId;
    private final String phone;
    private final AddressInfo address;

    public UserRegisteredEvent(
            String userId,
            String email,
            String name,
            String bankId,
            String paymentMethodId,
            String phone,
            AddressInfo address,
            String correlationId) {
        super(correlationId, "iam", Instant.now());
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
        this.phone = phone;
        this.address = address;
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getBankId() { return bankId; }
    public String getPaymentMethodId() { return paymentMethodId; }
    public String getPhone() { return phone; }
    public AddressInfo getAddress() { return address; }

    /**
     * Address information for Stripe integration
     */
    public record AddressInfo(
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country
    ) {}

    @Override
    public String toString() {
        return "UserRegisteredEvent{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", bankId='" + bankId + '\'' +
                ", paymentMethodId='" + paymentMethodId + '\'' +
                ", phone='" + phone + '\'' +
                ", address=" + address +
                ", correlationId='" + getCorrelationId() + '\'' +
                ", sourceModule='" + getSourceModule() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}