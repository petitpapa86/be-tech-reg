package com.bcbs239.regtech.billing.domain.billing;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentVerificationSagaData {
    private String correlationId;
    private UserId userId;
    private String userEmail;
    private String userName;
    private String paymentMethodId;
    
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripeInvoiceId;
    private String stripePaymentIntentId;
    
    private BillingAccountId billingAccountId;
    private SubscriptionId subscriptionId;
    
    private String failureReason;
}