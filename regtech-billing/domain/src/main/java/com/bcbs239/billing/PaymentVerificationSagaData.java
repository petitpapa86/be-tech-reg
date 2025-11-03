package com.bcbs239.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentVerificationSagaData {
    public static final Duration PAYMENT_TIMEOUT_SLA = Duration.ofMinutes(20);

    private String correlationId;
    private String userId;
    private String userEmail;
    private String userName;
    private String paymentMethodId;
    
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripeInvoiceId;
    private String stripePaymentIntentId;
    
    private String billingAccountId;
    private String subscriptionId;
    
    private String failureReason;
}