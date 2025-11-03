package com.bcbs239.regtech.billing.application.policies.createstripecustomer;

import com.bcbs239.regtech.core.saga.SagaId;

/**
 * Command to create a Stripe customer
 */
public record CreateStripeCustomerCommand(
    SagaId sagaId,
    String userId,
    String email,
    String name,
    String paymentMethodId
) {
    
    public SagaId getSagaId() {
        return sagaId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getName() {
        return name;
    }
    
    public String getPaymentMethodId() {
        return paymentMethodId;
    }
    
    public static CreateStripeCustomerCommand create(SagaId sagaId, String userId, String email, String name) {
        return new CreateStripeCustomerCommand(sagaId, userId, email, name, null);
    }
}