package com.bcbs239.regtech.core.events;

/**
 * Integration event for cross-module communication.
 * Separate from the domain event to avoid cycles and maintain clear boundaries.
 */
public class UserRegisteredIntegrationEvent extends BaseEvent {
    private final String userId;
    private final String email;
    private final String name;
    private final String bankId;
    private final String paymentMethodId;
    private final String phone;
    private final UserRegisteredEvent.AddressInfo address;

    public UserRegisteredIntegrationEvent(
            String userId,
            String email,
            String name,
            String bankId,
            String paymentMethodId,
            String phone,
            UserRegisteredEvent.AddressInfo address,
            String correlationId) {
        super(correlationId, "iam");
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
    public UserRegisteredEvent.AddressInfo getAddress() { return address; }
}