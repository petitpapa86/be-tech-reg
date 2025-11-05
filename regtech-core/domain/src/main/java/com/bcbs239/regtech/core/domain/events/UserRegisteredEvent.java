package com.bcbs239.regtech.core.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Cross-module event published when a user is registered and needs payment processing.
 * This event is consumed by the billing context to initiate payment processing.
 */

@Getter
public class UserRegisteredEvent extends BaseEvent {
    private String userId;
    private String email;
    private String name;
    private String bankId;
    private String paymentMethodId;
    private String phone;
    private AddressInfo address;

    public UserRegisteredEvent() {
        super(null, "iam");
    }

    @JsonCreator
    public UserRegisteredEvent(
            @JsonProperty("userId") String userId,
            @JsonProperty("email") String email,
            @JsonProperty("name") String name,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("paymentMethodId") String paymentMethodId,
            @JsonProperty("phone") String phone,
            @JsonProperty("address") AddressInfo address,
            @JsonProperty("correlationId") String correlationId) {
        super(correlationId, "iam");
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.bankId = bankId;
        this.paymentMethodId = paymentMethodId;
        this.phone = phone;
        this.address = address;
    }

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
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
