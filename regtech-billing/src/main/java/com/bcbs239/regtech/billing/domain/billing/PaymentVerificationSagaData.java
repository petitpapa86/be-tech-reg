package com.bcbs239.regtech.billing.domain.billing;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.iam.domain.users.UserId;
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